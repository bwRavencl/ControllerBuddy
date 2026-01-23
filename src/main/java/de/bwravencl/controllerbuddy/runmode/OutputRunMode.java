/* Copyright (C) 2022  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.runmode;

import de.bwravencl.controllerbuddy.ffi.Kernel32;
import de.bwravencl.controllerbuddy.ffi.User32;
import de.bwravencl.controllerbuddy.ffi.User32.INPUT;
import de.bwravencl.controllerbuddy.ffi.User32.INPUT.MOUSEINPUT;
import de.bwravencl.controllerbuddy.ffi.VjoyInterface;
import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.ScanCode;
import de.bwravencl.controllerbuddy.input.action.ToButtonAction;
import de.bwravencl.controllerbuddy.runmode.UinputDevice.DeviceType;
import de.bwravencl.controllerbuddy.runmode.UinputDevice.Event;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.lwjgl.sdl.SDLVideo;

public abstract class OutputRunMode extends RunMode {

	public static final int VJOY_DEFAULT_DEVICE = 1;

	private static final Logger LOGGER = Logger.getLogger(OutputRunMode.class.getName());

	private static final String SYSFS_BRIGHTNESS_FILENAME = "brightness";

	private static final String SYSFS_INPUT_DIR_REGEX_PREFIX = "input\\d+::";

	private static final String SYSFS_LEDS_DIR = File.separator + "sys" + File.separator + "class" + File.separator
			+ "leds";

	private static final int WHEEL_DELTA = 120;

	final Set<KeyStroke> downUpKeyStrokes = new HashSet<>();

	final Set<Integer> downUpMouseButtons = new HashSet<>();

	final Set<ScanCode> newDownModifiers = new HashSet<>();

	final Set<Integer> newDownMouseButtons = new HashSet<>();

	final Set<ScanCode> newDownNormalKeys = new HashSet<>();

	final Set<ScanCode> newUpModifiers = new HashSet<>();

	final Set<Integer> newUpMouseButtons = new HashSet<>();

	final Set<ScanCode> newUpNormalKeys = new HashSet<>();

	final Set<LockKey> offLockKeys = new HashSet<>();

	final Set<ScanCode> oldDownModifiers = new HashSet<>();

	final Set<Integer> oldDownMouseButtons = new HashSet<>();

	final Set<ScanCode> oldDownNormalKeys = new HashSet<>();

	final Set<LockKey> onLockKeys = new HashSet<>();

	DeviceValue axisRX;

	DeviceValue axisRY;

	DeviceValue axisRZ;

	DeviceValue axisS0;

	DeviceValue axisS1;

	DeviceValue axisX;

	DeviceValue axisY;

	DeviceValue axisZ;

	DeviceValue[] buttons;

	int cursorDeltaX;

	int cursorDeltaY;

	boolean forceStop;

	int scrollClicks;

	private ByteBuffer brightnessByteBuffer;

	private UinputDevice joystickUinputDevice;

	private UinputDevice keyboardUinputDevice;

	private Map<LockKey, FileChannel> lockKeyToBrightnessFileChannelMap;

	private UinputDevice mouseUinputDevice;

	private long prevKeyInputTime;

	private boolean restart;

	private int vJoyDevice;

	OutputRunMode(final Main main, final Input input) {
		super(main, input);

		if (Main.IS_WINDOWS) {
			vJoyDevice = main.getVJoyDevice();
		}
	}

	private static IllegalArgumentException buildInvalidMouseButtonException(final int button) {
		return new IllegalArgumentException("Parameter button has invalid value: " + button);
	}

	private static UnsupportedOperationException buildNotImplementedException() {
		return new UnsupportedOperationException("Not implemented");
	}

	private static void sendInputChecked(final MemorySegment input) throws IOException {
		if (User32.SendInput(1, MemorySegment.ofAddress(input.address()), (int) INPUT.LAYOUT.byteSize()) == 0) {
			throw new IOException("SendInput failed: " + Kernel32.GetLastError());
		}
	}

	static <T> void updateOutputSets(final Set<T> sourceSet, final Set<T> oldDownSet, final Set<T> newUpSet,
			final Set<T> newDownSet, final boolean keepStillDown) {
		newUpSet.clear();
		newDownSet.clear();

		final var oldDownSetIterator = oldDownSet.iterator();
		while (oldDownSetIterator.hasNext()) {
			final var oldDownElement = oldDownSetIterator.next();
			final var stillDown = sourceSet.contains(oldDownElement);

			if (stillDown) {
				if (keepStillDown) {
					newDownSet.add(oldDownElement);
				}
			} else {
				newUpSet.add(oldDownElement);
				oldDownSetIterator.remove();
			}
		}

		for (final var sourceElement : sourceSet) {
			final var alreadyDown = oldDownSet.contains(sourceElement);

			if (!alreadyDown) {
				newDownSet.add(sourceElement);
			}
		}

		oldDownSet.addAll(newDownSet);
	}

	final void deInit() {
		input.reset();
		input.deInit();

		if (main.isPreventPowerSaveMode()) {
			if (!SDLVideo.SDL_EnableScreenSaver()) {
				Main.logSdlError("Failed to enable screensaver");
			}
		}

		if (Main.IS_WINDOWS) {
			if (VjoyInterface.isInitialized()) {
				VjoyInterface.ResetButtons(vJoyDevice);
				VjoyInterface.ResetVJD(vJoyDevice);
				VjoyInterface.RelinquishVJD(vJoyDevice);
			}

			EventQueue.invokeLater(() -> main.setStatusBarText(
					MessageFormat.format(Main.STRINGS.getString("STATUS_DISCONNECTED_FROM_VJOY_DEVICE"), vJoyDevice)));
		} else if (Main.IS_LINUX) {
			EventQueue.invokeLater(
					() -> main.setStatusBarText(Main.STRINGS.getString("STATUS_DISCONNECTED_FROM_UINPUT_DEVICES")));
		}

		try {
			for (final var mouseButton : oldDownMouseButtons) {
				doMouseButtonInput(mouseButton, false);
			}
		} catch (final IOException e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}

		try {
			for (final var scanCode : oldDownModifiers) {
				doKeyboardInput(scanCode, false);
			}
		} catch (final IOException e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}

		if (lockKeyToBrightnessFileChannelMap != null) {
			lockKeyToBrightnessFileChannelMap.values().stream().filter(AbstractInterruptibleChannel::isOpen)
					.forEach(channel -> {
						try {
							channel.close();
						} catch (final IOException e) {
							LOGGER.log(Level.WARNING, e.getMessage(), e);
						}
					});
			lockKeyToBrightnessFileChannelMap = null;
		}
		brightnessByteBuffer = null;

		EventQueue.invokeLater(() -> {
			if (forceStop || restart) {
				main.stopAll(false, !restart, true);
			}

			main.updateTitleAndTooltip();

			if (restart) {
				main.restartLast();
			}
		});
	}

	private void doKeyboardInput(final ScanCode scanCode, final boolean down) throws IOException {
		if (Main.IS_WINDOWS) {
			try (final var arena = Arena.ofConfined()) {
				final var input = arena.allocate(INPUT.LAYOUT);
				INPUT.setType(input, INPUT.INPUT_KEYBOARD);
				final var ki = INPUT.getKi(input);
				INPUT.KEYBDINPUT.setWScan(ki, (short) scanCode.keyCode());
				var flags = (down ? 0 : INPUT.KEYBDINPUT.KEYEVENTF_KEYUP) | INPUT.KEYBDINPUT.KEYEVENTF_SCANCODE;
				if (ScanCode.EXTENDED_KEY_SCAN_CODES_SET.contains(scanCode.keyCode())) {
					flags |= INPUT.KEYBDINPUT.KEYEVENTF_EXTENDEDKEY;
				}
				INPUT.KEYBDINPUT.setDwFlags(ki, flags);

				sendInputChecked(input);
			}
		} else if (Main.IS_LINUX) {
			keyboardUinputDevice.emit(scanCode.event(), down ? 1 : 0, true);
		} else {
			throw buildNotImplementedException();
		}
	}

	private void doMouseButtonInput(final int button, final boolean down) throws IOException {
		if (Main.IS_WINDOWS) {
			try (final var arena = Arena.ofConfined()) {
				final var input = arena.allocate(INPUT.LAYOUT);
				INPUT.setType(input, INPUT.INPUT_MOUSE);
				final var mi = INPUT.getMi(input);

				switch (button) {
				case 1 -> INPUT.MOUSEINPUT.setDwFlags(mi,
						down ? INPUT.MOUSEINPUT.MOUSEEVENTF_LEFTDOWN : INPUT.MOUSEINPUT.MOUSEEVENTF_LEFTUP);
				case 2 -> INPUT.MOUSEINPUT.setDwFlags(mi,
						down ? INPUT.MOUSEINPUT.MOUSEEVENTF_RIGHTDOWN : INPUT.MOUSEINPUT.MOUSEEVENTF_RIGHTUP);
				case 3 -> INPUT.MOUSEINPUT.setDwFlags(mi,
						down ? INPUT.MOUSEINPUT.MOUSEEVENTF_MIDDLEDOWN : INPUT.MOUSEINPUT.MOUSEEVENTF_MIDDLEUP);
				case 4 -> {
					INPUT.MOUSEINPUT.setMouseData(mi, MOUSEINPUT.XBUTTON1);
					INPUT.MOUSEINPUT.setDwFlags(mi,
							down ? INPUT.MOUSEINPUT.MOUSEEVENTF_XDOWN : INPUT.MOUSEINPUT.MOUSEEVENTF_XUP);
				}
				case 5 -> {
					INPUT.MOUSEINPUT.setMouseData(mi, MOUSEINPUT.XBUTTON2);
					INPUT.MOUSEINPUT.setDwFlags(mi,
							down ? INPUT.MOUSEINPUT.MOUSEEVENTF_XDOWN : INPUT.MOUSEINPUT.MOUSEEVENTF_XUP);
				}
				default -> throw buildInvalidMouseButtonException(button);
				}

				sendInputChecked(input);
			}
		} else if (Main.IS_LINUX) {
			final var eventCode = switch (button) {
			case 1 -> Event.BTN_LEFT;
			case 2 -> Event.BTN_RIGHT;
			case 3 -> Event.BTN_MIDDLE;
			case 4 -> Event.BTN_SIDE;
			case 5 -> Event.BTN_EXTRA;
			default -> throw buildInvalidMouseButtonException(button);
			};

			mouseUinputDevice.emit(eventCode, down ? 1 : 0, true);
		} else {
			throw buildNotImplementedException();
		}
	}

	private boolean enoughButtons(final int numButtons) {
		var maxButtonId = -1;
		for (final var mode : input.getProfile().getModes()) {
			for (final var action : mode.getAllActions()) {
				if (action instanceof final ToButtonAction<?> toButtonAction) {
					final var buttonId = toButtonAction.getButtonId();
					if (buttonId > maxButtonId) {
						maxButtonId = buttonId;
					}
				}
			}
		}
		final var requiredButtons = maxButtonId + 1;

		if (numButtons < requiredButtons) {
			if (Main.IS_WINDOWS) {
				LOGGER.warning("vJoy device has not enough buttons");
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						MessageFormat.format(Main.STRINGS.getString("TOO_FEW_VJOY_BUTTONS_DIALOG_TEXT"), vJoyDevice,
								numButtons, requiredButtons),
						Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
			} else if (Main.IS_LINUX) {
				LOGGER.warning("uinput device has not enough buttons");
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						MessageFormat.format(Main.STRINGS.getString("TOO_FEW_UINPUT_BUTTONS_DIALOG_TEXT"), numButtons,
								requiredButtons),
						Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
			} else {
				throw buildNotImplementedException();
			}

			return false;
		}

		return true;
	}

	final void handleIOException(final IOException e) {
		forceStop = true;

		LOGGER.log(Level.SEVERE, e.getMessage(), e);
		EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
				Main.STRINGS.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
				Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
	}

	final boolean init() {
		final int numButtons;
		if (Main.IS_WINDOWS) {
			try {
				VjoyInterface.init(main);
			} catch (final Throwable t) {
				LOGGER.log(Level.SEVERE, t.getMessage(), t);
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						Main.STRINGS.getString("COULD_NOT_LOAD_VJOY_LIBRARY_DIALOG_TEXT"),
						Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));

				return false;
			}

			if (!VjoyInterface.vJoyEnabled()) {
				LOGGER.warning("vJoy driver is not enabled");
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						Main.STRINGS.getString("VJOY_DRIVER_NOT_ENABLED_DIALOG_TEXT"),
						Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				return false;
			}

			try (final var arena = Arena.ofConfined()) {
				final var dllVersion = arena.allocate(Short.BYTES);
				final var drvVersion = arena.allocate(Short.BYTES);
				if (!VjoyInterface.DriverMatch(dllVersion, drvVersion)) {
					LOGGER.warning("vJoy DLL version " + dllVersion + " does not match driver version " + drvVersion);
					EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
							MessageFormat.format(Main.STRINGS.getString("VJOY_VERSION_MISMATCH_DIALOG_TEXT"),
									dllVersion.get(ValueLayout.JAVA_SHORT, 0L),
									drvVersion.get(ValueLayout.JAVA_SHORT, 0L)),
							Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
					return false;
				}
			}

			LOGGER.info("Using vJoy device: " + vJoyDevice);

			if (VjoyInterface.GetVJDStatus(vJoyDevice) != VjoyInterface.VJD_STAT_FREE) {
				LOGGER.warning("vJoy device is not available");
				EventQueue
						.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
								MessageFormat.format(Main.STRINGS.getString("INVALID_VJOY_DEVICE_STATUS_DIALOG_TEXT"),
										vJoyDevice),
								Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				return false;
			}

			final var hasAxisX = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_X);
			final var hasAxisY = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_Y);
			final var hasAxisZ = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_Z);
			final var hasAxisRX = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_RX);
			final var hasAxisRY = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_RY);
			final var hasAxisRZ = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_RZ);
			final var hasAxisSL0 = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_SL0);
			final var hasAxisSL1 = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_SL1);
			if (!hasAxisX || !hasAxisY || !hasAxisZ || !hasAxisRX || !hasAxisRY || !hasAxisRZ || !hasAxisSL0
					|| !hasAxisSL1) {
				final var missingAxes = new ArrayList<String>();
				if (!hasAxisX) {
					missingAxes.add("X");
				}
				if (!hasAxisY) {
					missingAxes.add("Y");
				}
				if (!hasAxisZ) {
					missingAxes.add("Z");
				}
				if (!hasAxisRX) {
					missingAxes.add("Rx");
				}
				if (!hasAxisRY) {
					missingAxes.add("Ry");
				}
				if (!hasAxisRZ) {
					missingAxes.add("Rz");
				}
				if (!hasAxisSL0) {
					missingAxes.add("Slider");
				}
				if (!hasAxisSL1) {
					missingAxes.add("Dial/Slider2");
				}

				final var missingAxesString = String.join(", ", missingAxes);
				LOGGER.warning("vJoy device is missing the following axes: " + missingAxesString);
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						MessageFormat.format(Main.STRINGS.getString("MISSING_AXES_DIALOG_TEXT"), vJoyDevice,
								missingAxesString),
						Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				return false;
			}

			if (!VjoyInterface.AcquireVJD(vJoyDevice)) {
				LOGGER.warning("Could not acquire vJoy device");
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(), MessageFormat
						.format(Main.STRINGS.getString("COULD_NOT_ACQUIRE_VJOY_DEVICE_DIALOG_TEXT"), vJoyDevice),
						Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				return false;
			}

			if (!VjoyInterface.ResetVJD(vJoyDevice)) {
				LOGGER.warning("Could not reset vJoy device");
				EventQueue
						.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
								MessageFormat.format(Main.STRINGS.getString("COULD_NOT_RESET_VJOY_DEVICE_DIALOG_TEXT"),
										vJoyDevice),
								Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				return false;
			}

			try (final var arena = Arena.ofConfined()) {
				final var min = arena.allocate(Integer.SIZE);
				if (!VjoyInterface.GetVJDAxisMin(vJoyDevice, VjoyInterface.HID_USAGE_X, min)) {
					LOGGER.warning("Could not determine minimum axis value of vJoy device");
					EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
							MessageFormat.format(Main.STRINGS.getString("COULD_NOT_OBTAIN_VJOY_AXIS_RANGE_DIALOG_TEXT"),
									vJoyDevice),
							Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				}
				minAxisValue = min.get(ValueLayout.JAVA_INT, 0L);

				final var max = arena.allocate(Integer.SIZE);
				if (!VjoyInterface.GetVJDAxisMax(vJoyDevice, VjoyInterface.HID_USAGE_X, max)) {
					LOGGER.warning("Could not determine maximum axis value of vJoy device");
					EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
							MessageFormat.format(Main.STRINGS.getString("COULD_NOT_OBTAIN_VJOY_AXIS_RANGE_DIALOG_TEXT"),
									vJoyDevice),
							Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				}
				maxAxisValue = max.get(ValueLayout.JAVA_INT, 0L);
			}

			numButtons = VjoyInterface.GetVJDButtonNumber(vJoyDevice);
			if (!enoughButtons(numButtons)) {
				return false;
			}

			EventQueue.invokeLater(() -> main.setStatusBarText(
					MessageFormat.format(Main.STRINGS.getString("STATUS_CONNECTED_TO_VJOY_DEVICE"), vJoyDevice)));
		} else if (Main.IS_LINUX) {
			numButtons = Event.JOYSTICK_BUTTON_EVENTS.length;
			if (!enoughButtons(numButtons)) {
				return false;
			}

			try {
				joystickUinputDevice = UinputDevice.openUinputDevice(DeviceType.JOYSTICK);
				minAxisValue = Short.MIN_VALUE;
				maxAxisValue = Short.MAX_VALUE;
				mouseUinputDevice = UinputDevice.openUinputDevice(DeviceType.MOUSE);
				keyboardUinputDevice = UinputDevice.openUinputDevice(DeviceType.KEYBOARD);

				EventQueue.invokeLater(
						() -> main.setStatusBarText(Main.STRINGS.getString("STATUS_CONNECTED_TO_UINPUT_DEVICES")));
			} catch (final Throwable t) {
				LOGGER.log(Level.WARNING, t.getMessage(), t);

				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						Main.STRINGS.getString("COULD_NOT_OPEN_UINPUT_DEVICE_DIALOG_TEXT"),
						Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				return false;
			}

			try {
				lockKeyToBrightnessFileChannelMap = LockKey.LOCK_KEYS.stream()
						.collect(Collectors.toUnmodifiableMap(lockKey -> lockKey, lockKey -> {
							try (final var filesStream = Files.list(Path.of(SYSFS_LEDS_DIR))) {
								final var brightnessPath = filesStream.sorted().filter(p -> {
									final var fileName = p.getFileName();
									return fileName != null && fileName.toString()
											.matches(SYSFS_INPUT_DIR_REGEX_PREFIX + lockKey.sysfsLedName());
								}).findFirst()
										.orElseThrow(() -> new RuntimeException(
												"No brightness file for " + lockKey.sysfsLedName() + " LED"))
										.resolve(SYSFS_BRIGHTNESS_FILENAME);

								if (!Files.isRegularFile(brightnessPath) || !Files.isReadable(brightnessPath)) {
									throw new IOException("Unable to read: " + brightnessPath);
								}

								return FileChannel.open(brightnessPath, StandardOpenOption.READ);
							} catch (final IOException e) {
								throw new RuntimeException(e);
							}
						}));
			} catch (final Throwable t) {
				LOGGER.log(Level.WARNING, t.getMessage(), t);

				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						Main.STRINGS.getString("CANNOT_READ_LED_STATUS_DIALOG_TEXT"),
						Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				return false;
			}

			brightnessByteBuffer = ByteBuffer.allocateDirect(1);
		} else {
			throw buildNotImplementedException();
		}

		if (!input.init()) {
			controllerDisconnected();
			return false;
		}

		axisX = new DeviceValue();
		axisY = new DeviceValue();
		axisZ = new DeviceValue();
		axisRX = new DeviceValue();
		axisRY = new DeviceValue();
		axisRZ = new DeviceValue();
		axisS0 = new DeviceValue();
		axisS1 = new DeviceValue();

		setNumButtons(numButtons);

		if (main.isPreventPowerSaveMode() && !SDLVideo.SDL_DisableScreenSaver()) {
			Main.logSdlError("Failed to disable screensaver");
		}

		return true;
	}

	boolean readInput() throws IOException {
		process();

		return true;
	}

	private void setLockKeyState(final LockKey lockKey, final boolean on) throws IOException {
		if (Main.IS_WINDOWS) {
			final var virtualKeyCode = lockKey.virtualKeyCode();

			final var state = (User32.GetKeyState(virtualKeyCode) & 0x1) != 0;
			if (state != on) {
				final var toolkit = Toolkit.getDefaultToolkit();

				toolkit.setLockingKeyState(virtualKeyCode, true);
				toolkit.setLockingKeyState(virtualKeyCode, false);
			}
		} else if (Main.IS_LINUX) {
			final var brightnessFileChannel = lockKeyToBrightnessFileChannelMap.get(lockKey);
			if (brightnessFileChannel == null) {
				throw new IllegalStateException("No brightness file channel for " + lockKey.sysfsLedName() + " LED");
			}

			brightnessByteBuffer.clear();

			final var bytesRead = brightnessFileChannel.read(brightnessByteBuffer, 0);
			if (bytesRead == -1) {
				throw new IOException("Brightness file is empty");
			}

			final var ledState = brightnessByteBuffer.get(0);
			if (ledState != (on ? (byte) '1' : (byte) '0')) {
				keyboardUinputDevice.emit(lockKey.event(), 1, true);
				keyboardUinputDevice.emit(lockKey.event(), 0, true);
			}
		} else {
			throw buildNotImplementedException();
		}
	}

	@Override
	void setNumButtons(final int numButtons) {
		super.setNumButtons(numButtons);

		if (buttons == null || buttons.length != numButtons) {
			buttons = new DeviceValue[numButtons];
			for (var i = 0; i < buttons.length; i++) {
				buttons[i] = new DeviceValue();
			}
		}
	}

	final void writeOutput() {
		var writeSucessful = true;

		try {
			if (axisX.isChanged()) {
				if (Main.IS_WINDOWS) {
					writeSucessful &= VjoyInterface.SetAxis(axisX.get(), vJoyDevice, VjoyInterface.HID_USAGE_X);
				} else if (Main.IS_LINUX) {
					joystickUinputDevice.emit(Event.ABS_X, axisX.get(), true);
				} else {
					throw buildNotImplementedException();
				}

				axisX.setUnchanged();
			}

			if (axisY.isChanged()) {
				if (Main.IS_WINDOWS) {
					writeSucessful &= VjoyInterface.SetAxis(axisY.get(), vJoyDevice, VjoyInterface.HID_USAGE_Y);
				} else if (Main.IS_LINUX) {
					joystickUinputDevice.emit(Event.ABS_Y, axisY.get(), true);
				} else {
					throw buildNotImplementedException();
				}

				axisY.setUnchanged();
			}

			if (axisZ.isChanged()) {
				if (Main.IS_WINDOWS) {
					writeSucessful &= VjoyInterface.SetAxis(axisZ.get(), vJoyDevice, VjoyInterface.HID_USAGE_Z);
				} else if (Main.IS_LINUX) {
					joystickUinputDevice.emit(Event.ABS_Z, axisZ.get(), true);
				} else {
					throw buildNotImplementedException();
				}

				axisZ.setUnchanged();
			}

			if (axisRX.isChanged()) {
				if (Main.IS_WINDOWS) {
					writeSucessful &= VjoyInterface.SetAxis(axisRX.get(), vJoyDevice, VjoyInterface.HID_USAGE_RX);
				} else if (Main.IS_LINUX) {
					joystickUinputDevice.emit(Event.ABS_RX, axisRX.get(), true);
				} else {
					throw buildNotImplementedException();
				}

				axisRX.setUnchanged();
			}

			if (axisRY.isChanged()) {
				if (Main.IS_WINDOWS) {
					writeSucessful &= VjoyInterface.SetAxis(axisRY.get(), vJoyDevice, VjoyInterface.HID_USAGE_RY);
				} else if (Main.IS_LINUX) {
					joystickUinputDevice.emit(Event.ABS_RY, axisRY.get(), true);
				} else {
					throw buildNotImplementedException();
				}

				axisRY.setUnchanged();
			}

			if (axisRZ.isChanged()) {
				if (Main.IS_WINDOWS) {
					writeSucessful &= VjoyInterface.SetAxis(axisRZ.get(), vJoyDevice, VjoyInterface.HID_USAGE_RZ);
				} else if (Main.IS_LINUX) {
					joystickUinputDevice.emit(Event.ABS_RZ, axisRZ.get(), true);
				} else {
					throw buildNotImplementedException();
				}

				axisRZ.setUnchanged();
			}

			if (axisS0.isChanged()) {
				if (Main.IS_WINDOWS) {
					writeSucessful &= VjoyInterface.SetAxis(axisS0.get(), vJoyDevice, VjoyInterface.HID_USAGE_SL0);
				} else if (Main.IS_LINUX) {
					joystickUinputDevice.emit(Event.ABS_THROTTLE, axisS0.get(), true);
				} else {
					throw buildNotImplementedException();
				}

				axisS0.setUnchanged();
			}

			if (axisS1.isChanged()) {
				if (Main.IS_WINDOWS) {
					writeSucessful &= VjoyInterface.SetAxis(axisS1.get(), vJoyDevice, VjoyInterface.HID_USAGE_SL1);
				} else if (Main.IS_LINUX) {
					joystickUinputDevice.emit(Event.ABS_RUDDER, axisS1.get(), true);
				} else {
					throw buildNotImplementedException();
				}

				axisS1.setUnchanged();
			}

			for (var i = 0; i < buttons.length; i++) {
				if (buttons[i].isChanged()) {
					if (Main.IS_WINDOWS) {
						writeSucessful &= VjoyInterface.SetBtn(buttons[i].get() != 0, vJoyDevice, (byte) (i + 1));
					} else if (Main.IS_LINUX) {
						joystickUinputDevice.emit(Event.JOYSTICK_BUTTON_EVENTS[i], buttons[i].get(), true);
					} else {
						throw buildNotImplementedException();
					}

					buttons[i].setUnchanged();
				}
			}

			final var moveCursorOnXAxis = cursorDeltaX != 0;
			final var moveCursorOnYAxis = cursorDeltaY != 0;
			if (moveCursorOnXAxis || moveCursorOnYAxis) {
				if (Main.IS_WINDOWS) {
					try (final var arena = Arena.ofConfined()) {
						final var input = arena.allocate(INPUT.LAYOUT);
						INPUT.setType(input, INPUT.INPUT_MOUSE);
						final var mi = INPUT.getMi(input);
						INPUT.MOUSEINPUT.setDx(mi, cursorDeltaX);
						INPUT.MOUSEINPUT.setDy(mi, cursorDeltaY);
						INPUT.MOUSEINPUT.setDwFlags(mi, INPUT.MOUSEINPUT.MOUSEEVENTF_MOVE);

						sendInputChecked(input);
					}
				} else if (Main.IS_LINUX) {
					if (moveCursorOnXAxis) {
						mouseUinputDevice.emit(Event.REL_X, cursorDeltaX, false);
					}
					if (moveCursorOnYAxis) {
						mouseUinputDevice.emit(Event.REL_Y, cursorDeltaY, false);
					}
					mouseUinputDevice.syn();
				} else {
					throw buildNotImplementedException();
				}
			}

			for (final var mouseButton : newUpMouseButtons) {
				doMouseButtonInput(mouseButton, false);
			}

			for (final var mouseButton : newDownMouseButtons) {
				doMouseButtonInput(mouseButton, true);
			}

			for (final var mouseButton : downUpMouseButtons) {
				doMouseButtonInput(mouseButton, true);
				doMouseButtonInput(mouseButton, false);
			}

			for (final var scanCode : newUpNormalKeys) {
				doKeyboardInput(scanCode, false);
			}

			for (final var scanCode : newUpModifiers) {
				doKeyboardInput(scanCode, false);
			}

			for (final var scanCode : offLockKeys) {
				setLockKeyState(scanCode, false);
			}

			for (final var scanCode : onLockKeys) {
				setLockKeyState(scanCode, true);
			}

			for (final var scanCode : newDownModifiers) {
				doKeyboardInput(scanCode, true);
			}

			final var currentTime = System.currentTimeMillis();
			if (currentTime - prevKeyInputTime > input.getProfile().getKeyRepeatInterval()) {
				for (final var scanCode : newDownNormalKeys) {
					doKeyboardInput(scanCode, true);
				}

				prevKeyInputTime = currentTime;
			}

			for (final var keyStroke : downUpKeyStrokes) {
				for (final var scanCode : keyStroke.getModifierCodes()) {
					doKeyboardInput(scanCode, true);
				}

				for (final var scanCode : keyStroke.getKeyCodes()) {
					doKeyboardInput(scanCode, true);
					doKeyboardInput(scanCode, false);
				}

				for (final var scanCode : keyStroke.getModifierCodes()) {
					doKeyboardInput(scanCode, false);
				}
			}

			if (scrollClicks != 0) {
				if (Main.IS_WINDOWS) {
					try (final var arena = Arena.ofConfined()) {
						final var input = arena.allocate(INPUT.LAYOUT);
						INPUT.setType(input, INPUT.INPUT_MOUSE);
						final var mi = INPUT.getMi(input);
						INPUT.MOUSEINPUT.setMouseData(mi, scrollClicks * WHEEL_DELTA);
						INPUT.MOUSEINPUT.setDwFlags(mi, INPUT.MOUSEINPUT.MOUSEEVENTF_WHEEL);

						sendInputChecked(input);
					}
				} else if (Main.IS_LINUX) {
					mouseUinputDevice.emit(Event.REL_WHEEL, scrollClicks, true);
				} else {
					throw buildNotImplementedException();
				}
			}
		} catch (final IOException e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
			writeSucessful = false;
		}

		if (!writeSucessful) {
			final var confirmDialogTask = new FutureTask<>(() -> {
				final String message;
				if (Main.IS_WINDOWS) {
					message = "COULD_NOT_WRITE_TO_VJOY_DEVICE_DIALOG_TEXT";
				} else if (Main.IS_LINUX) {
					message = "COULD_NOT_WRITE_TO_UINPUT_DEVICE_DIALOG_TEXT";
				} else {
					throw buildNotImplementedException();
				}

				return main.executeWhileVisible(
						() -> JOptionPane.showConfirmDialog(main.getFrame(), Main.STRINGS.getString(message),
								Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.YES_NO_OPTION));
			});
			EventQueue.invokeLater(confirmDialogTask);
			try {
				for (;;) {
					process();
					if (confirmDialogTask.isDone() || confirmDialogTask.isCancelled()) {
						break;
					}
					// noinspection BusyWait
					Thread.sleep(10L);
				}

				if (confirmDialogTask.get() == JOptionPane.YES_OPTION) {
					restart = true;
				} else {
					forceStop = true;
				}
			} catch (final InterruptedException _) {
				// handled below
			} catch (final ExecutionException e) {
				throw new RuntimeException(e);
			}

			Thread.currentThread().interrupt();
		}
	}

	static final class DeviceValue {

		private int cachedValue;

		private boolean changed;

		private DeviceValue() {
			changed = true;
		}

		int get() {
			return cachedValue;
		}

		boolean isChanged() {
			return changed;
		}

		void set(final int value) {
			changed = cachedValue != value;
			cachedValue = value;
		}

		void setUnchanged() {
			changed = false;
		}
	}
}

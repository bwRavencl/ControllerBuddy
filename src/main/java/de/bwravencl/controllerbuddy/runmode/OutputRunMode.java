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

import com.sun.jna.IntegerType;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.LONGByReference;
import com.sun.jna.platform.win32.WinDef.UCHAR;
import com.sun.jna.platform.win32.WinDef.UINT;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinDef.WORDByReference;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.KEYBDINPUT;
import com.sun.jna.platform.win32.WinUser.MOUSEINPUT;
import de.bwravencl.controllerbuddy.constants.Constants;
import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.ScanCode;
import de.bwravencl.controllerbuddy.input.action.ToButtonAction;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;
import uk.co.bithatch.linuxio.InputDevice.Event;

public abstract class OutputRunMode extends RunMode {

	public static final int VJOY_DEFAULT_DEVICE = 1;

	public static final String VJOY_LIBRARY_FILENAME = VjoyInterface.VJOY_LIBRARY_NAME + ".dll";

	private static final long MOUSEEVENTF_LEFTDOWN = 0x0002L;

	private static final long MOUSEEVENTF_LEFTUP = 0x0004L;

	private static final long MOUSEEVENTF_MIDDLEDOWN = 0x0020L;

	private static final long MOUSEEVENTF_MIDDLEUP = 0x0040L;

	private static final long MOUSEEVENTF_MOVE = 0x0001L;

	private static final long MOUSEEVENTF_RIGHTDOWN = 0x0008L;

	private static final long MOUSEEVENTF_RIGHTUP = 0x0010L;

	private static final long MOUSEEVENTF_WHEEL = 0x0800L;

	private static final String SYSFS_BRIGHTNESS_FILENAME = "brightness";

	private static final String SYSFS_INPUT_DIR_REGEX_PREFIX = "input\\d+::";

	private static final String SYSFS_LEDS_DIR = File.separator + "sys" + File.separator + "class" + File.separator
			+ "leds";

	private static final EventCode[] UINPUT_JOYSTICK_BUTTON_EVENT_CODES = { EventCode.BTN_TRIGGER, EventCode.BTN_THUMB,
			EventCode.BTN_THUMB2, EventCode.BTN_TOP, EventCode.BTN_TOP2, EventCode.BTN_PINKIE, EventCode.BTN_BASE,
			EventCode.BTN_BASE2, EventCode.BTN_BASE3, EventCode.BTN_BASE4, EventCode.BTN_BASE5, EventCode.BTN_BASE6,
			EventCode.BTN_DEAD, EventCode.BTN_TRIGGER_HAPPY1, EventCode.BTN_TRIGGER_HAPPY2,
			EventCode.BTN_TRIGGER_HAPPY3, EventCode.BTN_TRIGGER_HAPPY4, EventCode.BTN_TRIGGER_HAPPY5,
			EventCode.BTN_TRIGGER_HAPPY6, EventCode.BTN_TRIGGER_HAPPY7, EventCode.BTN_TRIGGER_HAPPY8,
			EventCode.BTN_TRIGGER_HAPPY9, EventCode.BTN_TRIGGER_HAPPY10, EventCode.BTN_TRIGGER_HAPPY11,
			EventCode.BTN_TRIGGER_HAPPY12, EventCode.BTN_TRIGGER_HAPPY13, EventCode.BTN_TRIGGER_HAPPY14,
			EventCode.BTN_TRIGGER_HAPPY15, EventCode.BTN_TRIGGER_HAPPY16, EventCode.BTN_TRIGGER_HAPPY17,
			EventCode.BTN_TRIGGER_HAPPY18, EventCode.BTN_TRIGGER_HAPPY19, EventCode.BTN_TRIGGER_HAPPY20,
			EventCode.BTN_TRIGGER_HAPPY21, EventCode.BTN_TRIGGER_HAPPY22, EventCode.BTN_TRIGGER_HAPPY23,
			EventCode.BTN_TRIGGER_HAPPY24, EventCode.BTN_TRIGGER_HAPPY25, EventCode.BTN_TRIGGER_HAPPY26,
			EventCode.BTN_TRIGGER_HAPPY27, EventCode.BTN_TRIGGER_HAPPY28, EventCode.BTN_TRIGGER_HAPPY29,
			EventCode.BTN_TRIGGER_HAPPY30, EventCode.BTN_TRIGGER_HAPPY31, EventCode.BTN_TRIGGER_HAPPY32,
			EventCode.BTN_TRIGGER_HAPPY33, EventCode.BTN_TRIGGER_HAPPY34, EventCode.BTN_TRIGGER_HAPPY35,
			EventCode.BTN_TRIGGER_HAPPY36, EventCode.BTN_TRIGGER_HAPPY37, EventCode.BTN_TRIGGER_HAPPY38,
			EventCode.BTN_TRIGGER_HAPPY39, EventCode.BTN_TRIGGER_HAPPY40 };

	private static final int UINPUT_PRODUCT_CODE = 0x5678;

	private static final int UINPUT_VENDOR_CODE = 0x1234;

	private static final String VJOY_INSTALL_LOCATION_REGISTRY_VALUE = "InstallLocation";

	private static final String VJOY_UNINSTALL_REGISTRY_KEY = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{8E31F76F-74C3-47F1-9550-E041EEDC5FBB}_is1";

	private static final long WHEEL_DELTA = 120L;

	private static final Logger log = Logger.getLogger(OutputRunMode.class.getName());

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

	AxisValue axisRX;

	AxisValue axisRY;

	AxisValue axisRZ;

	AxisValue axisS0;

	AxisValue axisS1;

	AxisValue axisX;

	AxisValue axisY;

	AxisValue axisZ;

	ButtonValue[] buttons;

	int cursorDeltaX;

	int cursorDeltaY;

	boolean forceStop;

	int scrollClicks;

	private InputDevice joystickInputDevice;

	private InputDevice keyboardInputDevice;

	private Map<LockKey, File> lockKeyToBrightnessFileMap;

	private InputDevice mouseInputDevice;

	private long prevKeyInputTime;

	private boolean restart;

	private UINT vJoyDevice;

	OutputRunMode(final Main main, final Input input) {
		super(main, input);

		if (Main.isWindows) {
			vJoyDevice = new UINT(main.getVJoyDevice());
		}
	}

	private static IllegalArgumentException buildInvalidMouseButtonException(final int button) {
		return new IllegalArgumentException("Parameter button has invalid value: " + button);
	}

	private static UnsupportedOperationException buildNotImplementedException() {
		return new UnsupportedOperationException("Not implemented");
	}

	private static void closeInputDevice(final InputDevice inputDevice) {
		if (inputDevice != null && inputDevice.isOpen()) {
			try {
				inputDevice.close();
			} catch (final IOException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	public static String getDefaultVJoyPath() {
		try {
			return Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, VJOY_UNINSTALL_REGISTRY_KEY,
					VJOY_INSTALL_LOCATION_REGISTRY_VALUE);
		} catch (final Throwable t) {
			final var defaultPath = System.getenv("ProgramFiles") + File.separator + "vJoy";
			log.log(Level.WARNING, "Could not retrieve vJoy installation path from registry", t);

			return defaultPath;
		}
	}

	public static String getVJoyArchFolderName() {
		if (Platform.is64Bit()) {
			return "x64";
		} else {
			return "x86";
		}
	}

	static <T> void updateOutputSets(final Set<T> sourceSet, final Set<T> oldDownSet, final Set<T> newUpSet,
			final Set<T> newDownSet, final boolean keepStillDown) {
		newUpSet.clear();
		newDownSet.clear();

		final var oldDownSetIterator = oldDownSet.iterator();
		while (oldDownSetIterator.hasNext()) {
			final var oldDownElement = oldDownSetIterator.next();
			final var stillDown = sourceSet.stream().anyMatch(sourceElement -> sourceElement.equals(oldDownElement));

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
			final var alreadyDown = oldDownSet.stream().anyMatch(oldElement -> oldElement.equals(sourceElement));

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

		if (Main.isWindows) {
			if (VjoyInterface.isRegistered()) {
				VjoyInterface.ResetButtons(vJoyDevice);
				VjoyInterface.ResetVJD(vJoyDevice);
				VjoyInterface.RelinquishVJD(vJoyDevice);
			}

			EventQueue.invokeLater(() -> main.setStatusBarText(MessageFormat
					.format(Main.strings.getString("STATUS_DISCONNECTED_FROM_VJOY_DEVICE"), vJoyDevice.intValue())));
		} else if (Main.isLinux) {
			closeInputDevice(joystickInputDevice);
			closeInputDevice(mouseInputDevice);
			closeInputDevice(keyboardInputDevice);

			EventQueue.invokeLater(
					() -> main.setStatusBarText(Main.strings.getString("STATUS_DISCONNECTED_FROM_UINPUT_DEVICES")));
		}

		try {
			for (final var mouseButton : oldDownMouseButtons) {
				doMouseButtonInput(mouseButton, false);
			}
		} catch (final IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}

		try {
			for (final var scanCode : oldDownModifiers) {
				doKeyboardInput(scanCode, false);
			}
		} catch (final IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}

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
		if (Main.isWindows) {
			final var input = new INPUT();
			input.type = new DWORD(INPUT.INPUT_KEYBOARD);
			input.input.setType(KEYBDINPUT.class);
			input.input.ki.wScan = new WORD(scanCode.keyCode());
			var flags = (down ? 0 : KEYBDINPUT.KEYEVENTF_KEYUP) | KEYBDINPUT.KEYEVENTF_SCANCODE;
			if (ScanCode.extendedKeyScanCodesSet.contains(scanCode.keyCode())) {
				flags |= KEYBDINPUT.KEYEVENTF_EXTENDEDKEY;
			}
			input.input.ki.dwFlags = new DWORD(flags);

			User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
		} else if (Main.isLinux) {
			keyboardInputDevice.emit(new Event(scanCode.eventCode(), down ? 1 : 0));
		} else {
			throw buildNotImplementedException();
		}
	}

	private void doMouseButtonInput(final int button, final boolean down) throws IOException {
		if (Main.isWindows) {
			final var input = new INPUT();
			input.type = new DWORD(INPUT.INPUT_MOUSE);
			input.input.setType(MOUSEINPUT.class);

			switch (button) {
			case 1 -> input.input.mi.dwFlags = down ? new DWORD(MOUSEEVENTF_LEFTDOWN) : new DWORD(MOUSEEVENTF_LEFTUP);
			case 2 -> input.input.mi.dwFlags = down ? new DWORD(MOUSEEVENTF_RIGHTDOWN) : new DWORD(MOUSEEVENTF_RIGHTUP);
			case 3 -> input.input.mi.dwFlags = down ? new DWORD(MOUSEEVENTF_MIDDLEDOWN)
					: new DWORD(MOUSEEVENTF_MIDDLEUP);
			default -> throw buildInvalidMouseButtonException(button);
			}

			User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
		} else if (Main.isLinux) {
			final var eventCode = switch (button) {
			case 1 -> EventCode.BTN_LEFT;
			case 2 -> EventCode.BTN_RIGHT;
			case 3 -> EventCode.BTN_MIDDLE;
			default -> throw buildInvalidMouseButtonException(button);
			};

			mouseInputDevice.emit(new Event(eventCode, down ? 1 : 0));
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
			if (Main.isWindows) {
				log.log(Level.WARNING, "vJoy device has not enough buttons");
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						MessageFormat.format(Main.strings.getString("TOO_FEW_VJOY_BUTTONS_DIALOG_TEXT"),
								vJoyDevice.intValue(), numButtons, requiredButtons),
						Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
			} else if (Main.isLinux) {
				log.log(Level.WARNING, "uinput device has not enough buttons");
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						MessageFormat.format(Main.strings.getString("TOO_FEW_UINPUT_BUTTONS_DIALOG_TEXT"), numButtons,
								requiredButtons),
						Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
			} else {
				throw buildNotImplementedException();
			}

			return false;
		}

		return true;
	}

	final void handleIOException(final IOException e) {
		forceStop = true;

		log.log(Level.SEVERE, e.getMessage(), e);
		EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
				Main.strings.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
				Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
	}

	final boolean init() {
		final int numButtons;
		if (Main.isWindows) {
			try {
				final var vJoyPath = main.getVJoyDirectory();
				final var libraryPath = new File(vJoyPath, getVJoyArchFolderName()).getAbsolutePath();

				log.log(Level.INFO, "Using vJoy library path: " + libraryPath);
				System.setProperty("jna.library.path", libraryPath);

				VjoyInterface.register();

				if (!VjoyInterface.vJoyEnabled().booleanValue()) {
					log.log(Level.WARNING, "vJoy driver is not enabled");
					EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
							Main.strings.getString("VJOY_DRIVER_NOT_ENABLED_DIALOG_TEXT"),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
					return false;
				}

				final var dllVersion = new WORDByReference();
				final var drvVersion = new WORDByReference();
				if (!VjoyInterface.DriverMatch(dllVersion, drvVersion).booleanValue()) {
					log.log(Level.WARNING,
							"vJoy DLL version " + dllVersion + " does not match driver version " + drvVersion);
					EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
							MessageFormat.format(Main.strings.getString("VJOY_VERSION_MISMATCH_DIALOG_TEXT"),
									dllVersion.getValue().shortValue(), drvVersion.getValue().shortValue()),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
					return false;
				}

				log.log(Level.INFO, "Using vJoy device: " + vJoyDevice.toString());

				if (VjoyInterface.GetVJDStatus(vJoyDevice) != VjoyInterface.VJD_STAT_FREE) {
					log.log(Level.WARNING, "vJoy device is not available");
					EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
							MessageFormat.format(Main.strings.getString("INVALID_VJOY_DEVICE_STATUS_DIALOG_TEXT"),
									vJoyDevice.intValue()),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
					return false;
				}

				final var hasAxisX = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_X)
						.booleanValue();
				final var hasAxisY = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_Y)
						.booleanValue();
				final var hasAxisZ = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_Z)
						.booleanValue();
				final var hasAxisRX = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_RX)
						.booleanValue();
				final var hasAxisRY = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_RY)
						.booleanValue();
				final var hasAxisRZ = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_RZ)
						.booleanValue();
				final var hasAxisSL0 = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_SL0)
						.booleanValue();
				final var hasAxisSL1 = VjoyInterface.GetVJDAxisExist(vJoyDevice, VjoyInterface.HID_USAGE_SL1)
						.booleanValue();
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
					log.log(Level.WARNING, "vJoy device is missing the following axes: " + missingAxesString);
					EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
							MessageFormat.format(Main.strings.getString("MISSING_AXES_DIALOG_TEXT"),
									vJoyDevice.intValue(), missingAxesString),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
					return false;
				}

				if (!VjoyInterface.AcquireVJD(vJoyDevice).booleanValue()) {
					log.log(Level.WARNING, "Could not acquire vJoy device");
					EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
							MessageFormat.format(Main.strings.getString("COULD_NOT_ACQUIRE_VJOY_DEVICE_DIALOG_TEXT"),
									vJoyDevice.intValue()),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
					return false;
				}

				if (!VjoyInterface.ResetVJD(vJoyDevice).booleanValue()) {
					log.log(Level.WARNING, "Could not reset vJoy device");
					EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
							MessageFormat.format(Main.strings.getString("COULD_NOT_RESET_VJOY_DEVICE_DIALOG_TEXT"),
									vJoyDevice.intValue()),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
					return false;
				}

				final var Min = new LONGByReference();
				VjoyInterface.GetVJDAxisMin(vJoyDevice, VjoyInterface.HID_USAGE_X, Min);
				minAxisValue = Min.getValue().intValue();

				final var Max = new LONGByReference();
				VjoyInterface.GetVJDAxisMax(vJoyDevice, VjoyInterface.HID_USAGE_X, Max);
				maxAxisValue = Max.getValue().intValue();

				numButtons = VjoyInterface.GetVJDButtonNumber(vJoyDevice);
				if (!enoughButtons(numButtons)) {
					return false;
				}

				EventQueue.invokeLater(() -> main.setStatusBarText(MessageFormat
						.format(Main.strings.getString("STATUS_CONNECTED_TO_VJOY_DEVICE"), vJoyDevice.intValue())));
			} catch (final UnsatisfiedLinkError e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						Main.strings.getString("COULD_NOT_LOAD_VJOY_LIBRARY_DIALOG_TEXT"),
						Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));

				return false;
			}
		} else if (Main.isLinux) {
			numButtons = UINPUT_JOYSTICK_BUTTON_EVENT_CODES.length;
			if (!enoughButtons(numButtons)) {
				return false;
			}

			try {
				joystickInputDevice = new InputDevice(Constants.APPLICATION_NAME + " Joystick", UINPUT_VENDOR_CODE,
						UINPUT_PRODUCT_CODE);
				joystickInputDevice.addCapability(EventCode.ABS_X, EventCode.ABS_Y, EventCode.ABS_Z, EventCode.ABS_RX,
						EventCode.ABS_RY, EventCode.ABS_RZ, EventCode.ABS_THROTTLE, EventCode.ABS_RUDDER);
				joystickInputDevice.addCapability(UINPUT_JOYSTICK_BUTTON_EVENT_CODES);
				joystickInputDevice.open();
				log.log(Level.INFO, "Opened UINPUT joystick device: " + joystickInputDevice.toString());

				minAxisValue = Short.MIN_VALUE;
				maxAxisValue = Short.MAX_VALUE;

				mouseInputDevice = new InputDevice(Constants.APPLICATION_NAME + " Mouse", UINPUT_VENDOR_CODE,
						UINPUT_PRODUCT_CODE);
				mouseInputDevice.addCapability(EventCode.BTN_LEFT, EventCode.BTN_RIGHT, EventCode.BTN_MIDDLE,
						EventCode.REL_X, EventCode.REL_Y, EventCode.REL_WHEEL);
				mouseInputDevice.open();
				log.log(Level.INFO, "Opened UINPUT mouse device: " + mouseInputDevice.toString());

				keyboardInputDevice = new InputDevice(Constants.APPLICATION_NAME + " Keyboard", UINPUT_VENDOR_CODE,
						UINPUT_PRODUCT_CODE);
				for (final var eventCode : EventCode.values()) {
					if (eventCode.isKey()) {
						keyboardInputDevice.addCapability(eventCode);
					}
				}
				keyboardInputDevice.open();
				log.log(Level.INFO, "Opened UINPUT keyboard device: " + keyboardInputDevice.toString());

				EventQueue.invokeLater(
						() -> main.setStatusBarText(Main.strings.getString("STATUS_CONNECTED_TO_UINPUT_DEVICES")));
			} catch (final Throwable t) {
				log.log(Level.WARNING, t.getMessage(), t);

				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						Main.strings.getString("COULD_NOT_OPEN_UINPUT_DEVICE_DIALOG_TEXT"),
						Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				return false;
			}

			try {
				lockKeyToBrightnessFileMap = LockKey.LOCK_KEYS.stream()
						.collect(Collectors.toUnmodifiableMap(lockKey -> lockKey, lockKey -> {
							try (final var filesStream = Files.list(Path.of(SYSFS_LEDS_DIR))) {
								final var brightnessFile = filesStream.sorted().filter(p -> {
									final var fileName = p.getFileName();
									return fileName != null && fileName.toString()
											.matches(SYSFS_INPUT_DIR_REGEX_PREFIX + lockKey.sysfsLedName());
								}).findFirst()
										.orElseThrow(() -> new RuntimeException(
												"No brightness file for " + lockKey.sysfsLedName() + " LED"))
										.resolve(SYSFS_BRIGHTNESS_FILENAME).toFile();

								if (!brightnessFile.isFile() || !brightnessFile.canRead()) {
									throw new IOException("Unable to read: " + brightnessFile);
								}

								return brightnessFile;
							} catch (final IOException e) {
								throw new RuntimeException(e);
							}
						}));
			} catch (final Throwable t) {
				log.log(Level.WARNING, t.getMessage(), t);

				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						Main.strings.getString("CANNOT_READ_LED_STATUS_DIALOG_TEXT"),
						Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
				return false;
			}
		} else {
			throw buildNotImplementedException();
		}

		if (!input.init()) {
			controllerDisconnected();
			return false;
		}

		axisX = new AxisValue();
		axisY = new AxisValue();
		axisZ = new AxisValue();
		axisRX = new AxisValue();
		axisRY = new AxisValue();
		axisRZ = new AxisValue();
		axisS0 = new AxisValue();
		axisS1 = new AxisValue();

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
		if (Main.isWindows) {
			final var virtualKeyCode = lockKey.virtualKeyCode();

			final var state = (User32WithGetKeyState.INSTANCE.GetKeyState(virtualKeyCode) & 0x1) != 0;
			if (state != on) {
				final var toolkit = Toolkit.getDefaultToolkit();

				toolkit.setLockingKeyState(virtualKeyCode, true);
				toolkit.setLockingKeyState(virtualKeyCode, false);
			}
		} else if (Main.isLinux) {
			final var brightnessFile = lockKeyToBrightnessFileMap.get(lockKey);

			try (final var fileInputStream = new FileInputStream(brightnessFile)) {
				final var ledState = fileInputStream.read();

				if (ledState != (on ? '1' : '0')) {
					keyboardInputDevice.emit(new Event(lockKey.eventCode(), 1));
					keyboardInputDevice.emit(new Event(lockKey.eventCode(), 0));
				}
			}
		} else {
			throw buildNotImplementedException();
		}
	}

	@Override
	void setNumButtons(final int numButtons) {
		super.setNumButtons(numButtons);

		if (buttons == null || buttons.length != numButtons) {
			buttons = new ButtonValue[numButtons];
			for (var i = 0; i < buttons.length; i++) {
				buttons[i] = new ButtonValue();
			}
		}
	}

	final void writeOutput() {
		var writeSucessful = true;

		try {
			if (axisX.isChanged()) {
				if (Main.isWindows) {
					writeSucessful &= VjoyInterface.SetAxis(axisX.getvJoyValue(), vJoyDevice, VjoyInterface.HID_USAGE_X)
							.booleanValue();
				} else if (Main.isLinux) {
					joystickInputDevice.emit(new Event(EventCode.ABS_X, axisX.getUinputValue()));
				} else {
					throw buildNotImplementedException();
				}

				axisX.setUnchanged();
			}

			if (axisY.isChanged()) {
				if (Main.isWindows) {
					writeSucessful &= VjoyInterface.SetAxis(axisY.getvJoyValue(), vJoyDevice, VjoyInterface.HID_USAGE_Y)
							.booleanValue();
				} else if (Main.isLinux) {
					joystickInputDevice.emit(new Event(EventCode.ABS_Y, axisY.getUinputValue()));
				} else {
					throw buildNotImplementedException();
				}

				axisY.setUnchanged();
			}

			if (axisZ.isChanged()) {
				if (Main.isWindows) {
					writeSucessful &= VjoyInterface.SetAxis(axisZ.getvJoyValue(), vJoyDevice, VjoyInterface.HID_USAGE_Z)
							.booleanValue();
				} else if (Main.isLinux) {
					joystickInputDevice.emit(new Event(EventCode.ABS_Z, axisZ.getUinputValue()));
				} else {
					throw buildNotImplementedException();
				}

				axisZ.setUnchanged();
			}

			if (axisRX.isChanged()) {
				if (Main.isWindows) {
					writeSucessful &= VjoyInterface
							.SetAxis(axisRX.getvJoyValue(), vJoyDevice, VjoyInterface.HID_USAGE_RX).booleanValue();
				} else if (Main.isLinux) {
					joystickInputDevice.emit(new Event(EventCode.ABS_RX, axisRX.getUinputValue()));
				} else {
					throw buildNotImplementedException();
				}

				axisRX.setUnchanged();
			}

			if (axisRY.isChanged()) {
				if (Main.isWindows) {
					writeSucessful &= VjoyInterface
							.SetAxis(axisRY.getvJoyValue(), vJoyDevice, VjoyInterface.HID_USAGE_RY).booleanValue();
				} else if (Main.isLinux) {
					joystickInputDevice.emit(new Event(EventCode.ABS_RY, axisRY.getUinputValue()));
				} else {
					throw buildNotImplementedException();
				}

				axisRY.setUnchanged();
			}

			if (axisRZ.isChanged()) {
				if (Main.isWindows) {
					writeSucessful &= VjoyInterface
							.SetAxis(axisRZ.getvJoyValue(), vJoyDevice, VjoyInterface.HID_USAGE_RZ).booleanValue();
				} else if (Main.isLinux) {
					joystickInputDevice.emit(new Event(EventCode.ABS_RZ, axisRZ.getUinputValue()));
				} else {
					throw buildNotImplementedException();
				}

				axisRZ.setUnchanged();
			}

			if (axisS0.isChanged()) {
				if (Main.isWindows) {
					writeSucessful &= VjoyInterface
							.SetAxis(axisS0.getvJoyValue(), vJoyDevice, VjoyInterface.HID_USAGE_SL0).booleanValue();
				} else if (Main.isLinux) {
					joystickInputDevice.emit(new Event(EventCode.ABS_THROTTLE, axisS0.getUinputValue()));
				} else {
					throw buildNotImplementedException();
				}

				axisS0.setUnchanged();
			}

			if (axisS1.isChanged()) {
				if (Main.isWindows) {
					writeSucessful &= VjoyInterface
							.SetAxis(axisS1.getvJoyValue(), vJoyDevice, VjoyInterface.HID_USAGE_SL1).booleanValue();
				} else if (Main.isLinux) {
					joystickInputDevice.emit(new Event(EventCode.ABS_RUDDER, axisS1.getUinputValue()));
				} else {
					throw buildNotImplementedException();
				}

				axisS1.setUnchanged();
			}

			for (var i = 0; i < buttons.length; i++) {
				if (buttons[i].isChanged()) {
					if (Main.isWindows) {
						writeSucessful &= VjoyInterface.SetBtn(buttons[i].getvJoyValue(), vJoyDevice, new UCHAR(i + 1))
								.booleanValue();
					} else if (Main.isLinux) {
						joystickInputDevice
								.emit(new Event(UINPUT_JOYSTICK_BUTTON_EVENT_CODES[i], buttons[i].getUinputValue()));
					} else {
						throw buildNotImplementedException();
					}

					buttons[i].setUnchanged();
				}
			}

			final var moveCursorOnXAxis = cursorDeltaX != 0;
			final var moveCursorOnYAxis = cursorDeltaY != 0;
			if (moveCursorOnXAxis || moveCursorOnYAxis) {
				if (Main.isWindows) {
					final var input = new INPUT();
					input.type = new DWORD(INPUT.INPUT_MOUSE);
					input.input.setType(MOUSEINPUT.class);
					input.input.mi.dx = new LONG(cursorDeltaX);
					input.input.mi.dy = new LONG(cursorDeltaY);
					input.input.mi.dwFlags = new DWORD(MOUSEEVENTF_MOVE);

					User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
				} else if (Main.isLinux) {
					if (moveCursorOnXAxis) {
						mouseInputDevice.emit(new Event(EventCode.REL_X, cursorDeltaX), false);
					}
					if (moveCursorOnYAxis) {
						mouseInputDevice.emit(new Event(EventCode.REL_Y, cursorDeltaY), false);
					}
					mouseInputDevice.syn();
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

			for (final var code : newUpNormalKeys) {
				doKeyboardInput(code, false);
			}

			for (final var code : newUpModifiers) {
				doKeyboardInput(code, false);
			}

			for (final var code : offLockKeys) {
				setLockKeyState(code, false);
			}

			for (final var code : onLockKeys) {
				setLockKeyState(code, true);
			}

			for (final var code : newDownModifiers) {
				doKeyboardInput(code, true);
			}

			final var currentTime = System.currentTimeMillis();
			if (currentTime - prevKeyInputTime > input.getProfile().getKeyRepeatInterval()) {
				for (final var code : newDownNormalKeys) {
					doKeyboardInput(code, true);
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
				if (Main.isWindows) {
					final var input = new INPUT();
					input.type = new DWORD(INPUT.INPUT_MOUSE);
					input.input.setType(MOUSEINPUT.class);
					input.input.mi.mouseData = new DWORD(scrollClicks * WHEEL_DELTA);
					input.input.mi.dwFlags = new DWORD(MOUSEEVENTF_WHEEL);

					User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
				} else if (Main.isLinux) {
					mouseInputDevice.emit(new Event(EventCode.REL_WHEEL, scrollClicks));
				} else {
					throw buildNotImplementedException();
				}
			}
		} catch (final IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			writeSucessful = false;
		}

		if (!writeSucessful) {
			final var confirmDialogTask = new FutureTask<>(() -> {
				final String message;
				if (Main.isWindows) {
					message = "COULD_NOT_WRITE_TO_VJOY_DEVICE_DIALOG_TEXT";
				} else if (Main.isLinux) {
					message = "COULD_NOT_WRITE_TO_UINPUT_DEVICE_DIALOG_TEXT";
				} else {
					throw buildNotImplementedException();
				}

				return main.executeWhileVisible(
						() -> JOptionPane.showConfirmDialog(main.getFrame(), Main.strings.getString(message),
								Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.YES_NO_OPTION));
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

	static class AxisValue extends DeviceValue<LONG> {

		private AxisValue() {
			super(LONG.class);
		}
	}

	static class ButtonValue extends DeviceValue<BOOL> {

		private ButtonValue() {
			super(BOOL.class);
		}
	}

	private abstract static class DeviceValue<T extends IntegerType> {

		private boolean changed;

		private int uinputValue;

		private T vJoyValue;

		private DeviceValue(final Class<T> windowsClass) {
			if (Main.isWindows) {
				try {
					vJoyValue = windowsClass.getDeclaredConstructor().newInstance();
				} catch (final InstantiationException | IllegalAccessException | InvocationTargetException
						| NoSuchMethodException e) {
					throw new IllegalArgumentException(e);
				}
			}

			changed = true;
		}

		int getUinputValue() {
			return uinputValue;
		}

		T getvJoyValue() {
			return vJoyValue;
		}

		boolean isChanged() {
			return changed;
		}

		void setUnchanged() {
			changed = false;
		}

		void setValue(final int value) {
			if (Main.isWindows) {
				changed = vJoyValue.intValue() != value;
				vJoyValue.setValue(value);
			} else if (Main.isLinux) {
				changed = uinputValue != value;
				uinputValue = value;
			} else {
				throw buildNotImplementedException();
			}
		}
	}
}

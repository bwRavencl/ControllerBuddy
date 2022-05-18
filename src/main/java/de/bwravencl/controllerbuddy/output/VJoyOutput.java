/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.controllerbuddy.output;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.lwjgl.glfw.GLFW;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.UCHAR;
import com.sun.jna.platform.win32.WinDef.UINT;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.KEYBDINPUT;
import com.sun.jna.platform.win32.WinUser.MOUSEINPUT;

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.ScanCode;
import de.bwravencl.controllerbuddy.input.action.ToButtonAction;

public abstract class VJoyOutput extends Output {

	private static final Logger log = Logger.getLogger(VJoyOutput.class.getName());

	public static final int DEFAULT_VJOY_DEVICE = 1;
	private static final String LIBRARY_NAME = "vJoyInterface";
	public static final String LIBRARY_FILENAME = LIBRARY_NAME + ".dll";
	private static final String UNINSTALL_REGISTRY_KEY = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{8E31F76F-74C3-47F1-9550-E041EEDC5FBB}_is1";
	private static final String INSTALL_LOCATION_REGISTRY_VALUE = "InstallLocation";

	private static final long MOUSEEVENTF_MOVE = 0x0001L;
	private static final long MOUSEEVENTF_LEFTDOWN = 0x0002L;
	private static final long MOUSEEVENTF_LEFTUP = 0x0004L;
	private static final long MOUSEEVENTF_RIGHTDOWN = 0x0008L;
	private static final long MOUSEEVENTF_RIGHTUP = 0x0010L;
	private static final long MOUSEEVENTF_MIDDLEDOWN = 0x0020L;
	private static final long MOUSEEVENTF_MIDDLEUP = 0x0040L;
	private static final long MOUSEEVENTF_WHEEL = 0x0800L;
	private static final long WHEEL_DELTA = 120L;

	static {
		Native.register("User32");
	}

	private static IVjoyInterface vJoy;

	private static void doKeyboardInput(final int scanCode, final boolean down) {
		final var input = new INPUT();
		input.type = new DWORD(INPUT.INPUT_KEYBOARD);
		input.input.setType(KEYBDINPUT.class);
		input.input.ki.wScan = new WORD(scanCode);
		var flags = (down ? 0 : KEYBDINPUT.KEYEVENTF_KEYUP) | KEYBDINPUT.KEYEVENTF_SCANCODE;
		if (ScanCode.extendedKeyScanCodesSet.contains(scanCode))
			flags |= KEYBDINPUT.KEYEVENTF_EXTENDEDKEY;
		input.input.ki.dwFlags = new DWORD(flags);

		User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
	}

	private static void doMouseButtonInput(final int button, final boolean down) {
		final var input = new INPUT();
		input.type = new DWORD(INPUT.INPUT_MOUSE);
		input.input.setType(MOUSEINPUT.class);

		switch (button) {
		case 1 -> input.input.mi.dwFlags = down ? new DWORD(MOUSEEVENTF_LEFTDOWN) : new DWORD(MOUSEEVENTF_LEFTUP);
		case 2 -> input.input.mi.dwFlags = down ? new DWORD(MOUSEEVENTF_RIGHTDOWN) : new DWORD(MOUSEEVENTF_RIGHTUP);
		case 3 -> input.input.mi.dwFlags = down ? new DWORD(MOUSEEVENTF_MIDDLEDOWN) : new DWORD(MOUSEEVENTF_MIDDLEUP);
		default -> throw new IllegalArgumentException();
		}

		User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
	}

	public static String getArchFolderName() {
		return Platform.is64Bit() ? "x64" : "x86";
	}

	public static String getDefaultInstallationPath() {
		try {
			return Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, UNINSTALL_REGISTRY_KEY,
					INSTALL_LOCATION_REGISTRY_VALUE);
		} catch (final Throwable t) {
			final var defaultPath = System.getenv("ProgramFiles") + File.separator + "vJoy";
			log.log(Level.WARNING, "Could not retrieve vJoy installation path from registry", t);

			return defaultPath;
		}
	}

	private static native short GetKeyState(int KeyState);

	private static void setLockKeyState(final int virtualKeyCode, final boolean on) {
		final var state = (GetKeyState(virtualKeyCode) & 0x1) != 0;

		if (state != on) {
			final var toolkit = Toolkit.getDefaultToolkit();

			toolkit.setLockingKeyState(virtualKeyCode, true);
			toolkit.setLockingKeyState(virtualKeyCode, false);
		}
	}

	private static void setVJoy(final IVjoyInterface vJoy) {
		VJoyOutput.vJoy = vJoy;
	}

	static void updateOutputSets(final Set<Integer> sourceSet, final Set<Integer> oldDownSet,
			final Set<Integer> newUpSet, final Set<Integer> newDownSet, final boolean keepStillDown) {
		final var stillDownSet = new HashSet<Integer>();

		newUpSet.clear();

		for (final var oldCode : oldDownSet) {
			var stillDown = false;

			for (final var newCode : sourceSet)
				if (newCode.equals(oldCode)) {
					stillDown = true;
					break;
				}

			if (stillDown)
				stillDownSet.add(oldCode);
			else
				newUpSet.add(oldCode);
		}

		newDownSet.clear();

		if (keepStillDown)
			newDownSet.addAll(stillDownSet);

		for (final var newCode : sourceSet) {
			var alreadyDown = false;

			for (final var oldCode : oldDownSet)
				if (oldCode.equals(newCode)) {
					alreadyDown = true;
					break;
				}

			if (!alreadyDown)
				newDownSet.add(newCode);
		}

		oldDownSet.clear();
		oldDownSet.addAll(stillDownSet);
		oldDownSet.addAll(newDownSet);
	}

	private boolean restart;
	boolean forceStop;
	private UINT vJoyDevice = new UINT(DEFAULT_VJOY_DEVICE);
	LONG axisX;
	boolean axisXChanged;
	LONG axisY;
	boolean axisYChanged;
	LONG axisZ;
	boolean axisZChanged;
	LONG axisRX;
	boolean axisRXChanged;
	LONG axisRY;
	boolean axisRYChanged;
	LONG axisRZ;
	boolean axisRZChanged;
	LONG axisS0;
	boolean axisS0Changed;
	LONG axisS1;
	boolean axisS1Changed;
	BOOL[] buttons;
	boolean[] buttonsChanged;
	int cursorDeltaX;
	int cursorDeltaY;
	int scrollClicks;
	private long prevKeyInputTime = 0;
	final Set<Integer> oldDownMouseButtons = new HashSet<>();
	final Set<Integer> newUpMouseButtons = new HashSet<>();
	final Set<Integer> newDownMouseButtons = new HashSet<>();
	final Set<Integer> downUpMouseButtons = new HashSet<>();
	final Set<Integer> oldDownModifiers = new HashSet<>();
	final Set<Integer> newUpModifiers = new HashSet<>();
	final Set<Integer> newDownModifiers = new HashSet<>();
	final Set<Integer> oldDownNormalKeys = new HashSet<>();
	final Set<Integer> newUpNormalKeys = new HashSet<>();
	final Set<Integer> newDownNormalKeys = new HashSet<>();
	final Set<Integer> onLockKeys = new HashSet<>();
	final Set<Integer> offLockKeys = new HashSet<>();
	final Set<KeyStroke> downUpKeyStrokes = new HashSet<>();

	VJoyOutput(final Main main, final Input input) {
		super(main, input);
	}

	final void deInit() {
		input.reset();

		if (main.preventPowerSaveMode())
			Kernel32.INSTANCE.SetThreadExecutionState(WinBase.ES_CONTINUOUS);

		if (vJoy != null) {
			vJoy.ResetVJD(vJoyDevice);
			vJoy.RelinquishVJD(vJoyDevice);

			oldDownMouseButtons.forEach(mouseButton -> doMouseButtonInput(mouseButton, false));

			oldDownModifiers.forEach(code -> doKeyboardInput(code, false));

			EventQueue.invokeLater(() -> {
				main.setStatusBarText(MessageFormat
						.format(Main.strings.getString("STATUS_DISCONNECTED_FROM_VJOY_DEVICE"), vJoyDevice.intValue()));
			});
		}

		EventQueue.invokeLater(() -> {
			if (forceStop || restart)
				main.stopAll(false, false, true);
			if (restart)
				main.restartLast();
		});
	}

	final boolean init() {
		final var vJoyPath = main.getPreferences().get(Main.PREFERENCES_VJOY_DIRECTORY, getDefaultInstallationPath());
		final var libraryPath = new File(vJoyPath, getArchFolderName()).getAbsolutePath();

		log.log(Level.INFO, "Using vJoy library path: " + libraryPath);
		System.setProperty("jna.library.path", libraryPath);

		try {
			setVJoy(Native.load(LIBRARY_NAME, IVjoyInterface.class));

			if (!vJoy.vJoyEnabled().booleanValue()) {
				log.log(Level.WARNING, "vJoy driver is not enabled");
				EventQueue.invokeLater(() -> {
					GuiUtils.showMessageDialog(main.getFrame(),
							Main.strings.getString("VJOY_DRIVER_NOT_ENABLED_DIALOG_TEXT"),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				});
				return false;
			}

			final var dllVersion = new Memory(WinDef.WORD.SIZE);
			final var drvVersion = new Memory(WinDef.WORD.SIZE);
			if (!vJoy.DriverMatch(dllVersion, drvVersion).booleanValue()) {
				log.log(Level.WARNING, "vJoy DLL version " + dllVersion.toString() + " does not match driver version "
						+ drvVersion.toString());
				EventQueue.invokeLater(() -> {
					GuiUtils.showMessageDialog(main.getFrame(),
							MessageFormat.format(Main.strings.getString("VJOY_VERSION_MISMATCH_DIALOG_TEXT"),
									dllVersion.getShort(0L), drvVersion.getShort(0L)),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				});
				return false;
			}

			log.log(Level.INFO, "Using vJoy device: " + vJoyDevice.toString());

			if (vJoy.GetVJDStatus(vJoyDevice) != IVjoyInterface.VJD_STAT_FREE) {
				log.log(Level.WARNING, "vJoy device is not available");
				EventQueue.invokeLater(() -> {
					GuiUtils.showMessageDialog(main.getFrame(),
							MessageFormat.format(Main.strings.getString("INVALID_VJOY_DEVICE_STATUS_DIALOG_TEXT"),
									vJoyDevice.intValue()),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				});
				return false;
			}

			final var hasAxisX = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_X).booleanValue();
			final var hasAxisY = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_Y).booleanValue();
			final var hasAxisZ = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_Z).booleanValue();
			final var hasAxisRX = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_RX).booleanValue();
			final var hasAxisRY = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_RY).booleanValue();
			final var hasAxisRZ = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_RZ).booleanValue();
			final var hasAxisSL0 = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_SL0).booleanValue();
			final var hasAxisSL1 = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_SL1).booleanValue();
			if (!hasAxisX || !hasAxisY || !hasAxisZ || !hasAxisRX || !hasAxisRY || !hasAxisRZ || !hasAxisSL0
					|| !hasAxisSL1) {
				final var missingAxes = new ArrayList<String>();
				if (!hasAxisX)
					missingAxes.add("X");
				if (!hasAxisY)
					missingAxes.add("Y");
				if (!hasAxisZ)
					missingAxes.add("Z");
				if (!hasAxisRX)
					missingAxes.add("Rx");
				if (!hasAxisRY)
					missingAxes.add("Ry");
				if (!hasAxisRZ)
					missingAxes.add("Rz");
				if (!hasAxisSL0)
					missingAxes.add("Slider");
				if (!hasAxisSL1)
					missingAxes.add("Dial/Slider2");

				final var missingAxesString = String.join(", ", missingAxes);
				log.log(Level.WARNING, "vJoy device is missing the following axes: " + missingAxesString);
				EventQueue.invokeLater(() -> {
					GuiUtils.showMessageDialog(main.getFrame(),
							MessageFormat.format(Main.strings.getString("MISSING_AXES_DIALOG_TEXT"),
									vJoyDevice.intValue(), missingAxesString),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				});
				return false;
			}

			if (!vJoy.AcquireVJD(vJoyDevice).booleanValue()) {
				log.log(Level.WARNING, "Could not acquire vJoy device");
				EventQueue.invokeLater(() -> {
					GuiUtils.showMessageDialog(main.getFrame(),
							MessageFormat.format(Main.strings.getString("COULD_NOT_ACQUIRE_VJOY_DEVICE_DIALOG_TEXT"),
									vJoyDevice.intValue()),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				});
				return false;
			}

			if (!vJoy.ResetVJD(vJoyDevice).booleanValue()) {
				log.log(Level.WARNING, "Could not reset vJoy device");
				EventQueue.invokeLater(() -> {
					GuiUtils.showMessageDialog(main.getFrame(),
							MessageFormat.format(Main.strings.getString("COULD_NOT_RESET_VJOY_DEVICE_DIALOG_TEXT"),
									vJoyDevice.intValue()),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				});
				return false;
			}

			final var Min = new Memory(LONG.SIZE);
			vJoy.GetVJDAxisMin(vJoyDevice, IVjoyInterface.HID_USAGE_X, Min);
			minAxisValue = Min.getInt(0L);

			final var Max = new Memory(LONG.SIZE);
			vJoy.GetVJDAxisMax(vJoyDevice, IVjoyInterface.HID_USAGE_X, Max);
			maxAxisValue = Max.getInt(0L);

			final var nButtons = vJoy.GetVJDButtonNumber(vJoyDevice);
			var maxButtonId = -1;

			for (final var mode : input.getProfile().getModes())
				for (final var action : mode.getAllActions())
					if (action instanceof final ToButtonAction<?> toButtonAction) {
						final var buttonId = toButtonAction.getButtonId();
						if (buttonId > maxButtonId)
							maxButtonId = buttonId;
					}
			final var requiredButtons = maxButtonId + 1;

			if (nButtons < requiredButtons) {
				log.log(Level.WARNING, "vJoy device has not enough buttons");
				EventQueue.invokeLater(() -> {
					GuiUtils.showMessageDialog(main.getFrame(),
							MessageFormat.format(Main.strings.getString("TOO_FEW_BUTTONS_DIALOG_TEXT"),
									vJoyDevice.intValue(), nButtons, requiredButtons),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				});
				return false;
			}

			EventQueue.invokeLater(() -> {
				main.setStatusBarText(MessageFormat.format(Main.strings.getString("STATUS_CONNECTED_TO_VJOY_DEVICE"),
						vJoyDevice.intValue()));
			});

			if (main.preventPowerSaveMode())
				Kernel32.INSTANCE.SetThreadExecutionState(
						WinBase.ES_CONTINUOUS | WinBase.ES_SYSTEM_REQUIRED | WinBase.ES_DISPLAY_REQUIRED);

			input.init();

			axisX = new LONG();
			axisXChanged = true;
			axisY = new LONG();
			axisYChanged = true;
			axisZ = new LONG();
			axisZChanged = true;
			axisRX = new LONG();
			axisRXChanged = true;
			axisRY = new LONG();
			axisRYChanged = true;
			axisRZ = new LONG();
			axisRZChanged = true;
			axisS0 = new LONG();
			axisS0Changed = true;
			axisS1 = new LONG();
			axisS1Changed = true;

			setnButtons(nButtons);

			return true;
		} catch (final UnsatisfiedLinkError e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			EventQueue.invokeLater(() -> {
				GuiUtils.showMessageDialog(main.getFrame(),
						Main.strings.getString("COULD_NOT_LOAD_VJOY_LIBRARY_DIALOG_TEXT"),
						Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			});
			return false;
		}
	}

	boolean readInput() throws IOException {
		if (!Platform.isMac())
			GLFW.glfwPollEvents();

		return true;
	}

	@Override
	void setnButtons(final int nButtons) {
		super.setnButtons(nButtons);

		if (buttons == null || buttons.length != nButtons) {
			buttons = new BOOL[nButtons];
			buttonsChanged = new boolean[nButtons];
			for (var i = 0; i < buttons.length; i++) {
				buttons[i] = new BOOL();
				buttonsChanged[i] = true;
			}
		}
	}

	public final void setvJoyDevice(final UINT vJoyDevice) {
		this.vJoyDevice = vJoyDevice;
	}

	final void writeOutput() {
		var res = true;

		if (axisXChanged) {
			res &= vJoy.SetAxis(axisX, vJoyDevice, IVjoyInterface.HID_USAGE_X).booleanValue();
			axisXChanged = false;
		}

		if (axisYChanged) {
			res &= vJoy.SetAxis(axisY, vJoyDevice, IVjoyInterface.HID_USAGE_Y).booleanValue();
			axisYChanged = false;
		}

		if (axisZChanged) {
			res &= vJoy.SetAxis(axisZ, vJoyDevice, IVjoyInterface.HID_USAGE_Z).booleanValue();
			axisZChanged = false;
		}

		if (axisRXChanged) {
			res &= vJoy.SetAxis(axisRX, vJoyDevice, IVjoyInterface.HID_USAGE_RX).booleanValue();
			axisRXChanged = false;
		}

		if (axisRYChanged) {
			res &= vJoy.SetAxis(axisRY, vJoyDevice, IVjoyInterface.HID_USAGE_RY).booleanValue();
			axisRYChanged = false;
		}

		if (axisRZChanged) {
			res &= vJoy.SetAxis(axisRZ, vJoyDevice, IVjoyInterface.HID_USAGE_RZ).booleanValue();
			axisRZChanged = false;
		}

		if (axisS0Changed) {
			res &= vJoy.SetAxis(axisS0, vJoyDevice, IVjoyInterface.HID_USAGE_SL0).booleanValue();
			axisS0Changed = false;
		}

		if (axisS1Changed) {
			res &= vJoy.SetAxis(axisS1, vJoyDevice, IVjoyInterface.HID_USAGE_SL1).booleanValue();
			axisS1Changed = false;
		}

		for (var i = 0; i < buttons.length; i++)
			if (buttonsChanged[i]) {
				res &= vJoy.SetBtn(buttons[i], vJoyDevice, new UCHAR(i + 1)).booleanValue();
				buttonsChanged[i] = false;
			}

		if (!res) {
			final var confirmDialogTask = new FutureTask<>(() -> JOptionPane.showConfirmDialog(main.getFrame(),
					Main.strings.getString("COULD_NOT_WRITE_TO_VJOY_DEVICE_DIALOG_TEXT"),
					Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.YES_NO_OPTION));
			EventQueue.invokeLater(confirmDialogTask);
			try {
				if (confirmDialogTask.get() == JOptionPane.YES_OPTION)
					restart = true;
				else
					forceStop = true;
			} catch (final InterruptedException e) {
			} catch (final ExecutionException e) {
				throw new RuntimeException(e);
			}

			Thread.currentThread().interrupt();

			return;
		}

		if (cursorDeltaX != 0 || cursorDeltaY != 0) {
			final var input = new INPUT();
			input.type = new DWORD(INPUT.INPUT_MOUSE);
			input.input.setType(MOUSEINPUT.class);
			input.input.mi.dx = new LONG(cursorDeltaX);
			input.input.mi.dy = new LONG(cursorDeltaY);
			input.input.mi.dwFlags = new DWORD(MOUSEEVENTF_MOVE);

			User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
		}

		newUpMouseButtons.forEach(mouseButton -> doMouseButtonInput(mouseButton, false));
		newDownMouseButtons.forEach(mouseButton -> doMouseButtonInput(mouseButton, true));
		downUpMouseButtons.forEach(mouseButton -> {
			doMouseButtonInput(mouseButton, true);
			doMouseButtonInput(mouseButton, false);
		});

		newUpNormalKeys.forEach(code -> doKeyboardInput(code, false));
		newUpModifiers.forEach(code -> doKeyboardInput(code, false));

		offLockKeys.forEach(code -> setLockKeyState(code, false));
		onLockKeys.forEach(code -> setLockKeyState(code, true));

		newDownModifiers.forEach(code -> doKeyboardInput(code, true));

		final var currentTime = System.currentTimeMillis();
		if (currentTime - prevKeyInputTime > input.getProfile().getKeyRepeatInterval()) {
			newDownNormalKeys.forEach(code -> doKeyboardInput(code, true));

			prevKeyInputTime = currentTime;
		}

		downUpKeyStrokes.forEach(keyStroke -> {
			for (final var code : keyStroke.getModifierCodes())
				doKeyboardInput(code, true);

			for (final var code : keyStroke.getKeyCodes()) {
				doKeyboardInput(code, true);
				doKeyboardInput(code, false);
			}

			for (final var code : keyStroke.getModifierCodes())
				doKeyboardInput(code, false);
		});

		if (scrollClicks != 0) {
			final var input = new INPUT();
			input.type = new DWORD(INPUT.INPUT_MOUSE);
			input.input.setType(MOUSEINPUT.class);
			input.input.mi.mouseData = new DWORD(scrollClicks * WHEEL_DELTA);
			input.input.mi.dwFlags = new DWORD(MOUSEEVENTF_WHEEL);

			User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
		}
	}
}

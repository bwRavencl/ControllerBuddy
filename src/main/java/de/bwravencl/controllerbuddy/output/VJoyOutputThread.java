/* Copyright (C) 2016  Matteo Hausner
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

import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.UCHAR;
import com.sun.jna.platform.win32.WinDef.UINT;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.KEYBDINPUT;
import com.sun.jna.platform.win32.WinUser.MOUSEINPUT;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.ToButtonAction;

public abstract class VJoyOutputThread extends OutputThread {

	public static final int DEFAULT_VJOY_DEVICE = 1;
	public static final String LIBRARY_NAME = "vJoyInterface";
	public static final String LIBRARY_FILENAME = LIBRARY_NAME + ".dll";
	private static final long KEYEVENTF_KEYUP = 0x0002L;
	private static final long KEYEVENTF_SCANCODE = 0x0008L;
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

	private static void doKeyboardInput(int scanCode, boolean down) {
		final INPUT input = new INPUT();
		input.type = new DWORD(INPUT.INPUT_KEYBOARD);
		input.input.setType(KEYBDINPUT.class);
		input.input.ki.wScan = new WORD(scanCode);
		final long flags;
		if (down)
			flags = KEYEVENTF_SCANCODE;
		else
			flags = KEYEVENTF_KEYUP | KEYEVENTF_SCANCODE;
		input.input.ki.dwFlags = new DWORD(flags);

		User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
	}

	private static void doMouseButtonInput(int button, boolean down) {
		final INPUT input = new INPUT();
		input.type = new DWORD(INPUT.INPUT_MOUSE);
		input.input.setType(MOUSEINPUT.class);
		switch (button) {
		case 1:
			input.input.mi.dwFlags = (down ? new DWORD(MOUSEEVENTF_LEFTDOWN) : new DWORD(MOUSEEVENTF_LEFTUP));
			break;
		case 2:
			input.input.mi.dwFlags = (down ? new DWORD(MOUSEEVENTF_RIGHTDOWN) : new DWORD(MOUSEEVENTF_RIGHTUP));
			break;
		case 3:
			input.input.mi.dwFlags = (down ? new DWORD(MOUSEEVENTF_MIDDLEDOWN) : new DWORD(MOUSEEVENTF_MIDDLEUP));
			break;
		default:
			break;
		}

		User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
	}

	public static String getArchFolderName() {
		if (Main.is64Bit())
			return "x64";
		else
			return "x86";
	}

	public static String getDefaultInstallationPath() {
		return System.getenv("ProgramFiles") + File.separator + "vJoy";
	}

	public static String getDefaultLibraryFolderPath() {
		return getDefaultInstallationPath() + File.separator + getArchFolderName();
	}

	private static native short GetKeyState(int KeyState);

	public static String getLibraryFilePath(String vJoyDirectory) {
		return vJoyDirectory + File.separator + getArchFolderName() + File.separator + LIBRARY_FILENAME;
	}

	private static void setLockKeyState(int virtualKeyCode, boolean on) {
		final boolean state = (GetKeyState(virtualKeyCode) & 0x1) != 0;

		if (state != on) {
			final Toolkit toolkit = Toolkit.getDefaultToolkit();

			toolkit.setLockingKeyState(virtualKeyCode, true);
			toolkit.setLockingKeyState(virtualKeyCode, false);
		}
	}

	protected static void updateOutputSets(Set<Integer> sourceSet, Set<Integer> oldDownSet, Set<Integer> newUpSet,
			Set<Integer> newDownSet, boolean keepStillDown) {
		final Set<Integer> stillDownSet = new HashSet<>();

		newUpSet.clear();
		for (final int o : oldDownSet) {
			boolean stillDown = false;

			for (final int n : sourceSet) {
				if (n == o) {
					stillDown = true;
					break;
				}
			}

			if (stillDown)
				stillDownSet.add(o);
			else
				newUpSet.add(o);
		}

		newDownSet.clear();

		if (keepStillDown)
			newDownSet.addAll(stillDownSet);

		for (final int n : sourceSet) {
			boolean alreadyDown = false;

			for (final int o : oldDownSet) {
				if (o == n) {
					alreadyDown = true;
					break;
				}
			}

			if (!alreadyDown)
				newDownSet.add(n);
		}

		oldDownSet.clear();
		oldDownSet.addAll(stillDownSet);
		oldDownSet.addAll(newDownSet);
	}

	protected boolean restart = false;

	protected UINT vJoyDevice = new UINT(DEFAULT_VJOY_DEVICE);
	protected IVjoyInterface vJoy;
	protected LONG axisX;
	protected LONG axisY;
	protected LONG axisZ;
	protected LONG axisRX;
	protected LONG axisRY;
	protected LONG axisRZ;
	protected LONG axisS0;
	protected LONG axisS1;
	protected BOOL[] buttons;
	protected int cursorDeltaX;
	protected int cursorDeltaY;
	protected int scrollClicks;
	protected final Set<Integer> oldDownMouseButtons = new HashSet<>();
	protected final Set<Integer> newUpMouseButtons = new HashSet<>();
	protected final Set<Integer> newDownMouseButtons = new HashSet<>();
	protected final Set<Integer> downUpMouseButtons = new HashSet<>();
	protected final Set<Integer> oldDownModifiers = new HashSet<>();
	protected final Set<Integer> newUpModifiers = new HashSet<>();
	protected final Set<Integer> newDownModifiers = new HashSet<>();
	protected final Set<Integer> oldDownNormalKeys = new HashSet<>();
	protected final Set<Integer> newUpNormalKeys = new HashSet<>();
	protected final Set<Integer> newDownNormalKeys = new HashSet<>();
	protected final Set<Integer> onLockKeys = new HashSet<>();
	protected final Set<Integer> offLockKeys = new HashSet<>();
	protected final Set<KeyStroke> downUpKeyStrokes = new HashSet<>();

	public VJoyOutputThread(Main main, Input input) {
		super(main, input);
	}

	protected void deInit() {
		if (vJoy != null) {
			vJoy.ResetVJD(vJoyDevice);
			vJoy.RelinquishVJD(vJoyDevice);

			for (final int b : oldDownMouseButtons)
				doMouseButtonInput(b, false);

			for (final int c : oldDownModifiers)
				doKeyboardInput(c, false);

			main.setStatusBarText(rb.getString("STATUS_DISCONNECTED_FROM_VJOY_DEVICE") + vJoyDevice);
		}
	}

	public UINT getvJoyDevice() {
		return vJoyDevice;
	}

	protected boolean init() {
		System.setProperty("jna.library.path", main.getPreferences().get(
				Main.PREFERENCES_VJOY_DIRECTORY + File.separator + getArchFolderName(), getDefaultLibraryFolderPath()));

		try {
			vJoy = (IVjoyInterface) Native.loadLibrary(LIBRARY_NAME, IVjoyInterface.class);

			final Pointer dllVersion = new Memory(WinDef.WORD.SIZE);
			final Pointer drvVersion = new Memory(WinDef.WORD.SIZE);
			if (!vJoy.vJoyEnabled().booleanValue()) {
				JOptionPane.showMessageDialog(main.getFrame(), rb.getString("VJOY_DRIVER_NOT_ENABLED_DIALOG_TEXT"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if (!vJoy.DriverMatch(dllVersion, drvVersion).booleanValue()) {
				JOptionPane.showMessageDialog(main.getFrame(),
						rb.getString("VJOY_VERSION_MISMATCH_DIALOG_TEXT_PART_1") + dllVersion.getShort(0L)
								+ rb.getString("VJOY_VERSION_MISMATCH_DIALOG_TEXT_PART_2") + drvVersion.getShort(0L)
								+ rb.getString("VJOY_VERSION_MISMATCH_DIALOG_TEXT_PART_3"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				return false;
			}

			if (vJoy.GetVJDStatus(vJoyDevice) != IVjoyInterface.VJD_STAT_FREE) {
				JOptionPane.showMessageDialog(main.getFrame(),
						rb.getString("INVALID_VJOY_DEVICE_STATUS_DIALOG_TEXT_PREFIX") + vJoyDevice.toString()
								+ rb.getString("INVALID_VJOY_DEVICE_STATUS_DIALOG_TEXT_SUFFIX"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				return false;
			}

			final boolean hasAxisX = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_X).booleanValue();
			final boolean hasAxisY = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_Y).booleanValue();
			final boolean hasAxisZ = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_Z).booleanValue();
			final boolean hasAxisRX = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_RX).booleanValue();
			final boolean hasAxisRY = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_RY).booleanValue();
			final boolean hasAxisRZ = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_RZ).booleanValue();
			final boolean hasAxisSL0 = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_SL0).booleanValue();
			final boolean hasAxisSL1 = vJoy.GetVJDAxisExist(vJoyDevice, IVjoyInterface.HID_USAGE_SL1).booleanValue();
			if (!(hasAxisX && hasAxisY && hasAxisZ && hasAxisRX && hasAxisRY && hasAxisRZ && hasAxisSL0
					&& hasAxisSL1)) {
				final List<String> missingAxes = new ArrayList<>();
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

				final String missingAxesString = missingAxes.toString().replace("[", "").replace("]", "");

				JOptionPane.showMessageDialog(main.getFrame(),
						rb.getString("MISSING_AXES_DIALOG_TEXT_PART_1") + vJoyDevice.toString()
								+ rb.getString("MISSING_AXES_DIALOG_TEXT_PART_2") + missingAxesString
								+ rb.getString("MISSING_AXES_DIALOG_TEXT_PART_3"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				return false;
			}

			if (!vJoy.AcquireVJD(vJoyDevice).booleanValue()) {
				JOptionPane.showMessageDialog(main.getFrame(),
						rb.getString("COULD_NOT_ACQUIRE_VJOY_DEVICE_DIALOG_TEXT_PREFIX") + vJoyDevice.toString()
								+ rb.getString("COULD_NOT_ACQUIRE_VJOY_DEVICE_DIALOG_TEXT_SUFFIX"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				return false;
			}

			if (!vJoy.ResetVJD(vJoyDevice).booleanValue()) {
				JOptionPane.showMessageDialog(main.getFrame(),
						rb.getString("COULD_NOT_RESET_VJOY_DEVICE_DIALOG_TEXT_PREFIX") + vJoyDevice.toString()
								+ rb.getString("COULD_NOT_RESET_VJOY_DEVICE_DIALOG_TEXT_SUFFIX"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				return false;
			}

			final Pointer Min = new Memory(LONG.SIZE);
			vJoy.GetVJDAxisMin(vJoyDevice, IVjoyInterface.HID_USAGE_X, Min);
			minAxisValue = Min.getInt(0L);

			final Pointer Max = new Memory(LONG.SIZE);
			vJoy.GetVJDAxisMax(vJoyDevice, IVjoyInterface.HID_USAGE_X, Max);
			maxAxisValue = Max.getInt(0L);

			for (final VirtualAxis va : VirtualAxis.values())
				input.setAxis(va, 0.0f);

			final int nButtons = vJoy.GetVJDButtonNumber(vJoyDevice);
			int maxButtonId = -1;
			for (final Mode m : Input.getProfile().getModes()) {
				for (final List<IAction> actions : m.getComponentToActionsMap().values()) {
					for (final IAction a : actions) {
						if (a instanceof ToButtonAction) {
							final ToButtonAction toButtonAction = (ToButtonAction) a;
							final int buttonId = toButtonAction.getButtonId();
							if (buttonId > maxButtonId)
								maxButtonId = buttonId;
						}
					}
				}
			}
			final int requiredButtons = maxButtonId + 1;

			if (nButtons < requiredButtons) {
				JOptionPane.showMessageDialog(main.getFrame(),
						rb.getString("TOO_FEW_BUTTONS_DIALOG_TEXT_PART_1") + vJoyDevice.toString()
								+ rb.getString("TOO_FEW_BUTTONS_DIALOG_TEXT_PART_2") + nButtons
								+ rb.getString("TOO_FEW_BUTTONS_DIALOG_TEXT_PART_3") + requiredButtons
								+ rb.getString("TOO_FEW_BUTTONS_DIALOG_TEXT_PART_4"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				return false;
			}

			setnButtons(nButtons);

			main.setStatusBarText(rb.getString("STATUS_CONNECTED_TO_VJOY_DEVICE") + vJoyDevice.toString());
			return true;
		} catch (final UnsatisfiedLinkError e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(main.getFrame(), rb.getString("COULD_NOT_LOAD_VJOY_LIBRARY_DIALOG_TEXT"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	protected abstract boolean readInput() throws Exception;

	public void setvJoyDevice(UINT vJoyDevice) {
		this.vJoyDevice = vJoyDevice;
	}

	protected void writeOutput() {
		if (run) {
			boolean res = true;

			res &= vJoy.SetAxis(axisX, vJoyDevice, IVjoyInterface.HID_USAGE_X).booleanValue();
			res &= vJoy.SetAxis(axisY, vJoyDevice, IVjoyInterface.HID_USAGE_Y).booleanValue();
			res &= vJoy.SetAxis(axisZ, vJoyDevice, IVjoyInterface.HID_USAGE_Z).booleanValue();
			res &= vJoy.SetAxis(axisRX, vJoyDevice, IVjoyInterface.HID_USAGE_RX).booleanValue();
			res &= vJoy.SetAxis(axisRY, vJoyDevice, IVjoyInterface.HID_USAGE_RY).booleanValue();
			res &= vJoy.SetAxis(axisRZ, vJoyDevice, IVjoyInterface.HID_USAGE_RZ).booleanValue();
			res &= vJoy.SetAxis(axisS0, vJoyDevice, IVjoyInterface.HID_USAGE_SL0).booleanValue();
			res &= vJoy.SetAxis(axisS1, vJoyDevice, IVjoyInterface.HID_USAGE_SL1).booleanValue();

			for (int i = 0; i < buttons.length; i++)
				res &= vJoy.SetBtn(buttons[i], vJoyDevice, new UCHAR(i + 1)).booleanValue();

			if (res) {
				if (cursorDeltaX != 0 || cursorDeltaY != 0) {
					final INPUT input = new INPUT();
					input.type = new DWORD(INPUT.INPUT_MOUSE);
					input.input.setType(MOUSEINPUT.class);
					input.input.mi.dx = new LONG(cursorDeltaX);
					input.input.mi.dy = new LONG(cursorDeltaY);
					input.input.mi.dwFlags = new DWORD(MOUSEEVENTF_MOVE);

					User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
				}

				for (final int b : newUpMouseButtons)
					doMouseButtonInput(b, false);

				for (final int b : newDownMouseButtons)
					doMouseButtonInput(b, true);

				for (final int b : downUpMouseButtons) {
					doMouseButtonInput(b, true);
					doMouseButtonInput(b, false);
				}

				for (final int e : offLockKeys)
					setLockKeyState(e, false);

				for (final int c : newUpNormalKeys)
					doKeyboardInput(c, false);

				for (final int c : newUpModifiers)
					doKeyboardInput(c, false);

				for (final int c : newDownModifiers)
					doKeyboardInput(c, true);

				for (final int c : newDownNormalKeys)
					doKeyboardInput(c, true);

				for (final int e : onLockKeys)
					setLockKeyState(e, true);

				for (final KeyStroke ks : downUpKeyStrokes) {
					for (final int c : ks.getModifierCodes())
						doKeyboardInput(c, true);

					for (final int c : ks.getKeyCodes()) {
						doKeyboardInput(c, true);
						doKeyboardInput(c, false);
					}

					for (final int c : ks.getModifierCodes())
						doKeyboardInput(c, false);
				}

				if (scrollClicks != 0) {
					final INPUT input = new INPUT();
					input.type = new DWORD(INPUT.INPUT_MOUSE);
					input.input.setType(MOUSEINPUT.class);
					input.input.mi.mouseData = new DWORD(scrollClicks * WHEEL_DELTA);
					input.input.mi.dwFlags = new DWORD(MOUSEEVENTF_WHEEL);

					User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
				}
			} else {
				if (JOptionPane.showConfirmDialog(main.getFrame(),
						rb.getString("COULD_NOT_WRITE_TO_VJOY_DEVICE_DIALOG_TEXT"), rb.getString("ERROR_DIALOG_TITLE"),
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
					restart = true;

				stopOutput();
			}
		}
	}

}

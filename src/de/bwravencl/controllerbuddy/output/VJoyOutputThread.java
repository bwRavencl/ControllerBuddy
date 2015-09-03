/* Copyright (C) 2015  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.KeyStroke;

public abstract class VJoyOutputThread extends OutputThread {

	public static final int DEFAULT_VJOY_DEVICE = 1;

	public static final String LIBRARY_NAME = "vJoyInterface";
	public static final String LIBRARY_FILENAME = LIBRARY_NAME + ".dll";

	private static final DWORD MOUSEEVENTF_MOVE = new DWORD(0x0001L);
	private static final DWORD MOUSEEVENTF_LEFTDOWN = new DWORD(0x0002L);
	private static final DWORD MOUSEEVENTF_LEFTUP = new DWORD(0x0004L);
	private static final DWORD MOUSEEVENTF_RIGHTDOWN = new DWORD(0x0008L);
	private static final DWORD MOUSEEVENTF_RIGHTUP = new DWORD(0x0010L);
	private static final DWORD MOUSEEVENTF_MIDDLEDOWN = new DWORD(0x0020L);
	private static final DWORD MOUSEEVENTF_MIDDLEUP = new DWORD(0x0040L);
	private static final DWORD MOUSEEVENTF_WHEEL = new DWORD(0x0800L);
	private static final DWORD KEYEVENTF_KEYUP = new DWORD(0x0002L);
	private static final long WHEEL_DELTA = 120L;

	protected UINT vJoyDevice = new UINT(DEFAULT_VJOY_DEVICE);
	protected IVjoyInterface vJoy;
	protected boolean run = true;

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

	protected final Set<Integer> oldDownMouseButtons = new HashSet<Integer>();
	protected final Set<Integer> newUpMouseButtons = new HashSet<Integer>();
	protected final Set<Integer> newDownMouseButtons = new HashSet<Integer>();
	protected final Set<Integer> downUpMouseButtons = new HashSet<Integer>();

	protected final Set<Integer> oldDownModifiers = new HashSet<Integer>();
	protected final Set<Integer> newUpModifiers = new HashSet<Integer>();
	protected final Set<Integer> newDownModifiers = new HashSet<Integer>();
	protected final Set<Integer> downNormalKeys = new HashSet<Integer>();
	protected final Set<KeyStroke> downUpKeyStrokes = new HashSet<KeyStroke>();

	public static String getDefaultInstallationPath() {
		return System.getenv("ProgramFiles") + File.separator + "vJoy";
	}

	public static String getDefaultLibraryFolderPath() {
		return getDefaultInstallationPath() + File.separator + getArchFolderName();
	}

	public static String getLibraryFilePath(String vJoyDirectory) {
		return vJoyDirectory + File.separator + getArchFolderName() + File.separator + LIBRARY_FILENAME;
	}

	public static String getArchFolderName() {
		final String arch = System.getProperty("sun.arch.data.model");

		if ("64".equals(arch))
			return "x64";
		else
			return "x86";
	}

	protected static void updateOutputSets(Set<Integer> sourceSet, Set<Integer> oldDownSet, Set<Integer> newUpSet,
			Set<Integer> newDownSet) {
		final Set<Integer> stillDownSet = new HashSet<Integer>();

		newUpSet.clear();
		for (int o : oldDownSet) {
			boolean stillDown = false;

			for (int n : sourceSet) {
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
		for (int n : sourceSet) {
			boolean alreadyDown = false;

			for (int o : oldDownSet) {
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

	public VJoyOutputThread(Main main, Input input) {
		super(main, input);
	}

	public UINT getvJoyDevice() {
		return vJoyDevice;
	}

	public void setvJoyDevice(UINT vJoyDevice) {
		this.vJoyDevice = vJoyDevice;
	}

	protected boolean init() {
		System.setProperty("jna.library.path",
				main.getPreferences().get(Main.PREFERENCES_VJOY_DIRECTORY, getDefaultLibraryFolderPath()));

		try {
			vJoy = (IVjoyInterface) Native.loadLibrary(LIBRARY_NAME, IVjoyInterface.class);

			final Pointer dllVersion = new Memory(WinDef.WORD.SIZE);
			final Pointer drvVersion = new Memory(WinDef.WORD.SIZE);
			if (!vJoy.DriverMatch(dllVersion, drvVersion).booleanValue()) {
				JOptionPane.showMessageDialog(main.getFrame(),
						rb.getString("VJOY_VERSION_MISMATCH_DIALOG_TEXT_PART_1") + dllVersion.getShort(0L)
								+ rb.getString("VJOY_VERSION_MISMATCH_DIALOG_TEXT_PART_2") + drvVersion.getShort(0L)
								+ rb.getString("VJOY_VERSION_MISMATCH_DIALOG_TEXT_PART_3"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				return false;
			}

			if (!vJoy.vJoyEnabled().booleanValue()) {
				JOptionPane.showMessageDialog(main.getFrame(), rb.getString("VJOY_DRIVER_NOT_ENABLED_DIALOG_TEXT"),
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
				final List<String> missingAxis = new ArrayList<String>();
				if (!hasAxisX)
					missingAxis.add("X");
				if (!hasAxisY)
					missingAxis.add("Y");
				if (!hasAxisZ)
					missingAxis.add("Z");
				if (!hasAxisRX)
					missingAxis.add("Rx");
				if (!hasAxisRY)
					missingAxis.add("Ry");
				if (!hasAxisRZ)
					missingAxis.add("Rz");
				if (!hasAxisSL0)
					missingAxis.add("Slider");
				if (!hasAxisSL1)
					missingAxis.add("Dial/Slider2");

				final String missingAxisString = missingAxis.toString().replace("[", "").replace("]", "");

				JOptionPane.showMessageDialog(main.getFrame(),
						rb.getString("MISSING_AXIS_DIALOG_TEXT_PART_1") + vJoyDevice.toString()
								+ rb.getString("MISSING_AXIS_DIALOG_TEXT_PART_2") + missingAxisString
								+ rb.getString("MISSING_AXIS_DIALOG_TEXT_PART_3"),
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

			setnButtons(vJoy.GetVJDButtonNumber(vJoyDevice));

			main.setStatusbarText(rb.getString("STATUS_CONNECTED_TO_VJOY_DEVICE") + vJoyDevice.toString());
			return true;
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(main.getFrame(), rb.getString("COULD_NOT_LOAD_VJOY_LIBRARY_DIALOG_TEXT"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	protected abstract boolean readInput() throws Exception;

	private static void doMouseButtonInput(int button, boolean down) {
		final INPUT input = new INPUT();
		input.type = new DWORD(INPUT.INPUT_MOUSE);
		input.input.setType(MOUSEINPUT.class);
		switch (button) {
		case 1:
			input.input.mi.dwFlags = (down ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_LEFTUP);
			break;
		case 2:
			input.input.mi.dwFlags = (down ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_RIGHTUP);
			break;
		case 3:
			input.input.mi.dwFlags = (down ? MOUSEEVENTF_MIDDLEDOWN : MOUSEEVENTF_MIDDLEUP);
			break;
		default:
			break;
		}

		User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
	}

	private static void doKeyboardInput(int keyCode, boolean down) {
		final INPUT input = new INPUT();
		input.type = new DWORD(INPUT.INPUT_KEYBOARD);
		input.input.setType(KEYBDINPUT.class);
		input.input.ki.wVk = new WORD(keyCode);
		if (!down)
			input.input.ki.dwFlags = KEYEVENTF_KEYUP;

		User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
	}

	protected void writeOutput() {
		if (run) {
			vJoy.SetAxis(axisX, vJoyDevice, IVjoyInterface.HID_USAGE_X);
			vJoy.SetAxis(axisY, vJoyDevice, IVjoyInterface.HID_USAGE_Y);
			vJoy.SetAxis(axisZ, vJoyDevice, IVjoyInterface.HID_USAGE_Z);
			vJoy.SetAxis(axisRX, vJoyDevice, IVjoyInterface.HID_USAGE_RX);
			vJoy.SetAxis(axisRY, vJoyDevice, IVjoyInterface.HID_USAGE_RY);
			vJoy.SetAxis(axisRZ, vJoyDevice, IVjoyInterface.HID_USAGE_RZ);
			vJoy.SetAxis(axisS0, vJoyDevice, IVjoyInterface.HID_USAGE_SL0);
			vJoy.SetAxis(axisS1, vJoyDevice, IVjoyInterface.HID_USAGE_SL1);

			for (int i = 0; i < buttons.length; i++)
				vJoy.SetBtn(buttons[i], vJoyDevice, new UCHAR(i + 1));

			if (cursorDeltaX != 0 || cursorDeltaY != 0) {
				final INPUT input = new INPUT();
				input.type = new DWORD(INPUT.INPUT_MOUSE);
				input.input.setType(MOUSEINPUT.class);
				input.input.mi.dx = new LONG(cursorDeltaX);
				input.input.mi.dy = new LONG(cursorDeltaY);
				input.input.mi.dwFlags = MOUSEEVENTF_MOVE;

				User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
			}

			for (int b : newUpMouseButtons)
				doMouseButtonInput(b, false);

			for (int b : newDownMouseButtons)
				doMouseButtonInput(b, true);

			for (int b : downUpMouseButtons) {
				doMouseButtonInput(b, true);
				doMouseButtonInput(b, false);
			}

			for (int k : newUpModifiers)
				doKeyboardInput(k, false);

			for (int k : newDownModifiers)
				doKeyboardInput(k, true);

			for (int k : downNormalKeys)
				doKeyboardInput(k, true);

			for (KeyStroke ks : downUpKeyStrokes) {
				for (int k : ks.getModifierCodes())
					doKeyboardInput(k, true);

				for (int k : ks.getKeyCodes())
					doKeyboardInput(k, true);

				for (int k : ks.getModifierCodes())
					doKeyboardInput(k, false);
			}

			if (scrollClicks != 0) {
				final INPUT input = new INPUT();
				input.type = new DWORD(INPUT.INPUT_MOUSE);
				input.input.setType(MOUSEINPUT.class);
				input.input.mi.mouseData = new DWORD(scrollClicks * WHEEL_DELTA);
				input.input.mi.dwFlags = MOUSEEVENTF_WHEEL;

				User32.INSTANCE.SendInput(new DWORD(1L), new INPUT[] { input }, input.size());
			}
		}
	}

	protected void deInit() {
		if (vJoy != null) {
			vJoy.ResetVJD(vJoyDevice);
			vJoy.RelinquishVJD(vJoyDevice);

			for (int k : oldDownMouseButtons)
				doMouseButtonInput(k, false);

			for (int k : oldDownModifiers)
				doKeyboardInput(k, false);

			main.setStatusbarText(rb.getString("STATUS_DISCONNECTED_FROM_VJOY_DEVICE") + vJoyDevice);
		}
	}

	@Override
	public void stopOutput() {
		super.stopOutput();

		run = false;
	}
}

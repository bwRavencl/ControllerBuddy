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

package de.bwravencl.RemoteStick.output.vjoy;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.UCHAR;
import com.sun.jna.platform.win32.WinDef.UINT;

import de.bwravencl.RemoteStick.gui.Main;
import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.KeyStroke;
import de.bwravencl.RemoteStick.output.OutputThread;

public class VJoyThread extends OutputThread {

	public static final int DEFAULT_VJOY_DEVICE = 1;

	public static final String LIBRARY_NAME = "vJoyInterface";
	public static final String LIBRARY_FILENAME = LIBRARY_NAME + ".dll";

	protected UINT vJoyDevice = new UINT(DEFAULT_VJOY_DEVICE);
	protected IVjoyInterface vJoy;
	private boolean run = true;

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

	public VJoyThread(Main main, Input input) {
		super(main, input);
	}

	protected boolean connect() {
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
						rb.getString("COULD_NOT_ACQUIRE_VJOY_DEVICE_PREFIX") + vJoyDevice.toString()
								+ rb.getString("COULD_NOT_ACQUIRE_VJOY_DEVICE_SUFFIX"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				return false;
			}

			if (!vJoy.ResetVJD(vJoyDevice).booleanValue()) {
				JOptionPane.showMessageDialog(main.getFrame(),
						rb.getString("COULD_NOT_RESET_VJOY_DEVICE_PREFIX") + vJoyDevice.toString()
								+ rb.getString("COULD_NOT_RESET_VJOY_DEVICE_SUFFIX"),
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
			JOptionPane.showMessageDialog(main.getFrame(), rb.getString("COULD_NOT_LOAD_VJOY_LIBRARY"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	protected void disconnect() {
		if (vJoy != null) {
			vJoy.ResetVJD(vJoyDevice);
			vJoy.RelinquishVJD(vJoyDevice);
			main.setStatusbarText(rb.getString("STATUS_DISCONNECTED_FROM_VJOY_DEVICE") + vJoyDevice);
			main.stopFeeder();
		}
	}

	@Override
	public void run() {
		if (connect()) {
			try {
				final Robot robot = new Robot();
				final Set<Integer> oldDownMouseButtons = new HashSet<Integer>();
				final Set<Integer> oldDownKeyCodes = new HashSet<Integer>();

				while (run) {
					input.poll();

					vJoy.SetAxis(new LONG(input.getAxis().get(Input.VirtualAxis.X)), vJoyDevice,
							IVjoyInterface.HID_USAGE_X);
					vJoy.SetAxis(new LONG(input.getAxis().get(Input.VirtualAxis.Y)), vJoyDevice,
							IVjoyInterface.HID_USAGE_Y);
					vJoy.SetAxis(new LONG(input.getAxis().get(Input.VirtualAxis.Z)), vJoyDevice,
							IVjoyInterface.HID_USAGE_Z);
					vJoy.SetAxis(new LONG(input.getAxis().get(Input.VirtualAxis.RX)), vJoyDevice,
							IVjoyInterface.HID_USAGE_RX);
					vJoy.SetAxis(new LONG(input.getAxis().get(Input.VirtualAxis.RY)), vJoyDevice,
							IVjoyInterface.HID_USAGE_RY);
					vJoy.SetAxis(new LONG(input.getAxis().get(Input.VirtualAxis.RZ)), vJoyDevice,
							IVjoyInterface.HID_USAGE_RZ);
					vJoy.SetAxis(new LONG(input.getAxis().get(Input.VirtualAxis.S0)), vJoyDevice,
							IVjoyInterface.HID_USAGE_SL0);
					vJoy.SetAxis(new LONG(input.getAxis().get(Input.VirtualAxis.S1)), vJoyDevice,
							IVjoyInterface.HID_USAGE_SL1);

					for (int i = 0; i < input.getButtons().length; i++)
						vJoy.SetBtn(new BOOL(input.getButtons()[i] ? 1L : 0L), vJoyDevice, new UCHAR(i + 1));

					final Point currentPosition = MouseInfo.getPointerInfo().getLocation();
					robot.mouseMove(currentPosition.x + input.getCursorDeltaX(),
							currentPosition.y + input.getCursorDeltaY());
					input.setCursorDeltaX(0);
					input.setCursorDeltaY(0);

					oldDownMouseButtons.removeAll(input.getDownMouseButtons());
					for (int b : oldDownMouseButtons)
						robot.mouseRelease(InputEvent.getMaskForButton(b));

					for (int b : input.getDownMouseButtons())
						robot.mousePress(InputEvent.getMaskForButton(b));
					oldDownMouseButtons.addAll(input.getDownMouseButtons());

					for (int b : input.getDownUpMouseButtons())
						robot.mousePress(InputEvent.getMaskForButton(b));
					for (int b : input.getDownUpMouseButtons())
						robot.mouseRelease(InputEvent.getMaskForButton(b));
					input.getDownUpMouseButtons().clear();

					robot.mouseWheel(input.getScrollClicks());
					input.setScrollClicks(0);

					oldDownKeyCodes.removeAll(input.getDownKeyCodes());
					for (int k : oldDownKeyCodes)
						robot.keyRelease(k);

					for (int k : input.getDownKeyCodes())
						robot.keyPress(k);
					oldDownKeyCodes.addAll(input.getDownKeyCodes());

					for (KeyStroke ks : input.getDownUpKeyStrokes()) {
						for (int k : ks.getModifierCodes())
							robot.keyPress(k);

						for (int k : ks.getKeyCodes())
							robot.keyPress(k);

						for (int k : ks.getKeyCodes())
							robot.keyRelease(k);

						for (int k : ks.getModifierCodes())
							robot.keyRelease(k);
					}
					input.getDownUpKeyStrokes().clear();

					try {
						Thread.sleep(updateRate);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (AWTException e) {
				e.printStackTrace();
			}
		}

		disconnect();
	}

	public void stopFeeder() {
		run = false;
	}

	public UINT getvJoyDevice() {
		return vJoyDevice;
	}

	public void setvJoyDevice(UINT vJoyDevice) {
		this.vJoyDevice = vJoyDevice;
	}
}

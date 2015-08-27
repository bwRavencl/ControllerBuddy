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

package de.bwravencl.RemoteStick.vjoy;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.UCHAR;
import com.sun.jna.platform.win32.WinDef.UINT;

public class VJoyThread {

	public static void main(String[] args) {
		IVjoyInterface vJoy;
		try {
			vJoy = VJoy.loadLibrary();

			final UINT rID = new UINT(1L);

			System.out.println("Version: " + String.valueOf(vJoy.GetvJoyVersion().shortValue()));

			System.out.println("Enabled: " + vJoy.vJoyEnabled());

			System.out.println("Product String: " + vJoy.GetvJoyProductString().getPointer().getWideString(0L));

			System.out.println("Product String: " + vJoy.GetvJoyManufacturerString().getPointer().getWideString(0L));

			System.out.println("Product String: " + vJoy.GetvJoySerialNumberString().getPointer().getWideString(0L));

			Pointer DllVer = new Memory(WinDef.WORD.SIZE);
			Pointer DrvVer = new Memory(WinDef.WORD.SIZE);
			System.out.println("DriverMatch: " + vJoy.DriverMatch(DllVer, DrvVer));
			System.out.println("DllVer: " + DllVer.getShort(0L));
			System.out.println("DrvVer: " + DrvVer.getShort(0L));

			System.out.println("Button Number: " + vJoy.GetVJDButtonNumber(rID));

			System.out.println("Disc Pov Number: " + vJoy.GetVJDDiscPovNumber(rID));

			System.out.println("Cont Pov Number: " + vJoy.GetVJDContPovNumber(rID));

			System.out
					.println("Axis exists: " + vJoy.GetVJDAxisExist(rID, IVjoyInterface.HID_USAGE_SL0).booleanValue());

			System.out.println("Status: " + vJoy.GetVJDStatus(rID));
			System.out.println("Aquire: " + vJoy.AcquireVJD(rID));
			System.out.println("Status: " + vJoy.GetVJDStatus(rID));

			System.out.println("Reset VJD: " + vJoy.ResetVJD(rID));
			vJoy.ResetAll();
			System.out.println("Reset Buttons: " + vJoy.ResetButtons(rID));
			System.out.println("Reset Povs: " + vJoy.ResetPovs(rID));

			Pointer Max = new Memory(LONG.SIZE);
			System.out.println(vJoy.GetVJDAxisMax(rID, IVjoyInterface.HID_USAGE_X, Max));
			int maxAxisValue = Max.getInt(0L);
			System.out.println("Max Axis Value: " + maxAxisValue);

			Pointer Min = new Memory(LONG.SIZE);
			System.out.println(vJoy.GetVJDAxisMin(rID, IVjoyInterface.HID_USAGE_X, Min));
			int minAxisValue = Min.getInt(0L);
			System.out.println("Min Axis Value: " + minAxisValue);

			System.out.println("Set Axis: " + vJoy.SetAxis(new LONG(maxAxisValue), rID, IVjoyInterface.HID_USAGE_X));

			System.out.println("Set Btn: " + vJoy.SetBtn(new BOOL(1L), rID, new UCHAR(1)));

			/*
			 * System.out.println("Set Disc Pov: " + vJoy.SetDiscPov(int Value,
			 * rID, UCHAR nPov));
			 * 
			 * System.out.println("Set Pov: " + vJoy.SetContPov(DWORD Value,
			 * rID, UCHAR nPov));
			 */

			vJoy.RelinquishVJD(rID);
			System.out.println("Status: " + vJoy.GetVJDStatus(rID));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

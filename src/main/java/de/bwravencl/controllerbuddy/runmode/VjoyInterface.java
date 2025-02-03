/* Copyright (C) 2015  Matteo Hausner
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

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.LONGByReference;
import com.sun.jna.platform.win32.WinDef.PVOID;
import com.sun.jna.platform.win32.WinDef.SHORT;
import com.sun.jna.platform.win32.WinDef.UCHAR;
import com.sun.jna.platform.win32.WinDef.UINT;
import com.sun.jna.platform.win32.WinDef.WORDByReference;

@SuppressWarnings({ "UnusedReturnValue", "unused" })
public final class VjoyInterface {

	static final UINT HID_USAGE_X = new UINT(0x30L);
	static final UINT HID_USAGE_Y = new UINT(0x31L);
	static final UINT HID_USAGE_Z = new UINT(0x32L);
	static final UINT HID_USAGE_RX = new UINT(0x33L);
	static final UINT HID_USAGE_RY = new UINT(0x34L);
	static final UINT HID_USAGE_RZ = new UINT(0x35L);
	static final UINT HID_USAGE_SL0 = new UINT(0x36L);
	static final UINT HID_USAGE_SL1 = new UINT(0x37L);
	static final UINT HID_USAGE_WHL = new UINT(0x38L);
	static final UINT HID_USAGE_POV = new UINT(0x39L);
	static final int VJD_STAT_OWN = 0;
	static final int VJD_STAT_FREE = 1;
	static final int VJD_STAT_BUSY = 2;
	static final int VJD_STAT_MISS = 3;
	static final int VJD_STAT_UNKN = 4;
	static final String VJOY_LIBRARY_NAME = "vJoyInterface";

	static native BOOL AcquireVJD(UINT rID);

	static native BOOL DriverMatch(WORDByReference DllVer, WORDByReference DrvVer);

	static native BOOL GetVJDAxisExist(UINT rID, UINT Axis);

	static native BOOL GetVJDAxisMax(UINT rID, UINT Axis, LONGByReference Max);

	static native BOOL GetVJDAxisMin(UINT rID, UINT Axis, LONGByReference Min);

	static native int GetVJDButtonNumber(UINT rID);

	static native int GetVJDContPovNumber(UINT rID);

	static native int GetVJDDiscPovNumber(UINT rID);

	static native int GetVJDStatus(UINT rID);

	static native PVOID GetvJoyManufacturerString();

	static native PVOID GetvJoyProductString();

	static native PVOID GetvJoySerialNumberString();

	static native SHORT GetvJoyVersion();

	static native void RelinquishVJD(UINT rID);

	static native void ResetAll();

	static native BOOL ResetButtons(UINT rID);

	static native BOOL ResetPovs(UINT rID);

	static native BOOL ResetVJD(UINT rID);

	static native BOOL SetAxis(LONG Value, UINT rID, UINT Axis);

	static native BOOL SetBtn(BOOL Value, UINT rID, UCHAR nBtn);

	static native BOOL SetContPov(DWORD Value, UINT rID, UCHAR nPov);

	static native BOOL SetDiscPov(int Value, UINT rID, UCHAR nPov);

	static native BOOL UpdateVJD(UINT rID, PVOID pData);

	public static boolean isRegistered() {
		return Native.registered(VjoyInterface.class);
	}

	static void register() {
		if (!isRegistered()) {
			Native.register(VJOY_LIBRARY_NAME);
		}
	}

	static native BOOL vJoyEnabled();
}

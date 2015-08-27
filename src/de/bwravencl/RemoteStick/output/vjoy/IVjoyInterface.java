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

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.PVOID;
import com.sun.jna.platform.win32.WinDef.SHORT;
import com.sun.jna.platform.win32.WinDef.UCHAR;
import com.sun.jna.platform.win32.WinDef.UINT;

public interface IVjoyInterface extends Library {

	public static final UINT HID_USAGE_X = new UINT(0x30L);
	public static final UINT HID_USAGE_Y = new UINT(0x31L);
	public static final UINT HID_USAGE_Z = new UINT(0x32L);
	public static final UINT HID_USAGE_RX = new UINT(0x33L);
	public static final UINT HID_USAGE_RY = new UINT(0x34L);
	public static final UINT HID_USAGE_RZ = new UINT(0x35L);
	public static final UINT HID_USAGE_SL0 = new UINT(0x36L);
	public static final UINT HID_USAGE_SL1 = new UINT(0x37L);
	public static final UINT HID_USAGE_WHL = new UINT(0x38L);
	public static final UINT HID_USAGE_POV = new UINT(0x39L);

	public static final int VJD_STAT_OWN = 0;
	public static final int VJD_STAT_FREE = 1;
	public static final int VJD_STAT_BUSY = 2;
	public static final int VJD_STAT_MISS = 3;
	public static final int VJD_STAT_UNKN = 4;

	public SHORT GetvJoyVersion();

	public BOOL vJoyEnabled();

	public PVOID GetvJoyProductString();

	public PVOID GetvJoyManufacturerString();

	public PVOID GetvJoySerialNumberString();

	public BOOL DriverMatch(Pointer DllVer, Pointer DrvVer);

	public int GetVJDButtonNumber(UINT rID);

	public int GetVJDDiscPovNumber(UINT rID);

	public int GetVJDContPovNumber(UINT rID);

	public BOOL GetVJDAxisExist(UINT rID, UINT Axis);

	public BOOL GetVJDAxisMax(UINT rID, UINT Axis, Pointer Max);

	public BOOL GetVJDAxisMin(UINT rID, UINT Axis, Pointer Min);

	public BOOL AcquireVJD(UINT rID);

	public void RelinquishVJD(UINT rID);

	public BOOL UpdateVJD(UINT rID, PVOID pData);

	public int GetVJDStatus(UINT rID);

	public BOOL ResetVJD(UINT rID);

	public void ResetAll();

	public BOOL ResetButtons(UINT rID);

	public BOOL ResetPovs(UINT rID);

	public BOOL SetAxis(LONG Value, UINT rID, UINT Axis);

	public BOOL SetBtn(BOOL Value, UINT rID, UCHAR nBtn);

	public BOOL SetDiscPov(int Value, UINT rID, UCHAR nPov);

	public BOOL SetContPov(DWORD Value, UINT rID, UCHAR nPov);

}

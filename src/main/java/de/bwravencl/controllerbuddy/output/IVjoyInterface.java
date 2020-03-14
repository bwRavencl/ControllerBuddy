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

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.PVOID;
import com.sun.jna.platform.win32.WinDef.SHORT;
import com.sun.jna.platform.win32.WinDef.UCHAR;
import com.sun.jna.platform.win32.WinDef.UINT;

interface IVjoyInterface extends Library {

	UINT HID_USAGE_X = new UINT(0x30L);
	UINT HID_USAGE_Y = new UINT(0x31L);
	UINT HID_USAGE_Z = new UINT(0x32L);
	UINT HID_USAGE_RX = new UINT(0x33L);
	UINT HID_USAGE_RY = new UINT(0x34L);
	UINT HID_USAGE_RZ = new UINT(0x35L);
	UINT HID_USAGE_SL0 = new UINT(0x36L);
	UINT HID_USAGE_SL1 = new UINT(0x37L);
	UINT HID_USAGE_WHL = new UINT(0x38L);
	UINT HID_USAGE_POV = new UINT(0x39L);
	int VJD_STAT_OWN = 0;
	int VJD_STAT_FREE = 1;
	int VJD_STAT_BUSY = 2;
	int VJD_STAT_MISS = 3;
	int VJD_STAT_UNKN = 4;

	BOOL AcquireVJD(UINT rID);

	BOOL DriverMatch(Pointer DllVer, Pointer DrvVer);

	BOOL GetVJDAxisExist(UINT rID, UINT Axis);

	BOOL GetVJDAxisMax(UINT rID, UINT Axis, Pointer Max);

	BOOL GetVJDAxisMin(UINT rID, UINT Axis, Pointer Min);

	int GetVJDButtonNumber(UINT rID);

	int GetVJDContPovNumber(UINT rID);

	int GetVJDDiscPovNumber(UINT rID);

	int GetVJDStatus(UINT rID);

	PVOID GetvJoyManufacturerString();

	PVOID GetvJoyProductString();

	PVOID GetvJoySerialNumberString();

	SHORT GetvJoyVersion();

	void RelinquishVJD(UINT rID);

	void ResetAll();

	BOOL ResetButtons(UINT rID);

	BOOL ResetPovs(UINT rID);

	BOOL ResetVJD(UINT rID);

	BOOL SetAxis(LONG Value, UINT rID, UINT Axis);

	BOOL SetBtn(BOOL Value, UINT rID, UCHAR nBtn);

	BOOL SetContPov(DWORD Value, UINT rID, UCHAR nPov);

	BOOL SetDiscPov(int Value, UINT rID, UCHAR nPov);

	BOOL UpdateVJD(UINT rID, PVOID pData);

	BOOL vJoyEnabled();
}

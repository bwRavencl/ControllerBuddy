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

package de.bwravencl.controllerbuddy.gui.mumbleoverlay;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.MEMORY_BASIC_INFORMATION;

public class SharedMemory {

	private static final int FILE_MAP_ALL_ACCESS = Kernel32.STANDARD_RIGHTS_REQUIRED | Kernel32.SECTION_QUERY
			| Kernel32.SECTION_MAP_WRITE | Kernel32.SECTION_MAP_READ | Kernel32.SECTION_MAP_EXECUTE
			| Kernel32.SECTION_EXTEND_SIZE;
	private static final String NAME_PREFIX = "Local\\MumbleOverlayMemory";

	static {
		Native.register("Kernel32");
	}

	private static native long VirtualQuery(Pointer p, MEMORY_BASIC_INFORMATION infoOut, SIZE_T sz);

	private String name;
	private Pointer data;
	private int index;
	private int size;

	private HANDLE memory;

	public SharedMemory(final int minSize) throws Exception {
		for (int i = 0; i < 100; i++) {
			index++;
			name = NAME_PREFIX + index;
			memory = Kernel32.INSTANCE.CreateFileMapping(WinBase.INVALID_HANDLE_VALUE, null, WinNT.PAGE_READWRITE, 0,
					minSize, name);
			if (memory != null) {
				if (Kernel32.INSTANCE.GetLastError() != WinError.ERROR_ALREADY_EXISTS)
					break;

				Kernel32.INSTANCE.CloseHandle(memory);
				memory = null;
			}
		}

		if (memory == null) {
			deInit();
			throw new Exception(getClass().getName() + ": CreateFileMapping failed for: " + name);
		} else {
			data = Kernel32.INSTANCE.MapViewOfFile(memory, FILE_MAP_ALL_ACCESS, 0, 0, 0);

			if (data == null)
				throw new Exception(getClass().getName() + ": Failed to map memory for: " + name);
			else {
				final MEMORY_BASIC_INFORMATION mbi = new MEMORY_BASIC_INFORMATION();
				if (VirtualQuery(data, mbi, new SIZE_T(mbi.size())) == 0L || mbi.regionSize.intValue() < minSize)
					throw new Exception(getClass().getName() + ": Memory too small for: " + name);
				else {
					size = mbi.regionSize.intValue();
					return;
				}
			}
		}
	}

	public void deInit() {
		if (data != null) {
			Kernel32.INSTANCE.UnmapViewOfFile(data);
			data = null;
		}

		if (memory != null) {
			Kernel32.INSTANCE.CloseHandle(memory);
			memory = null;
		}
	}

	public void erase() {
		if (data != null)
			data.clear(size);
	}

	public Pointer getData() {
		return data;
	}

	public String getName() {
		return name;
	}

	public void systemRelease() {
		if (memory != null) {
			Kernel32.INSTANCE.CloseHandle(memory);
			memory = null;
		}
	}

}

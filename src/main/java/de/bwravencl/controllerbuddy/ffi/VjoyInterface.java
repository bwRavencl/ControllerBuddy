/* Copyright (C) 2025  Matteo Hausner
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

package de.bwravencl.controllerbuddy.ffi;

import de.bwravencl.controllerbuddy.gui.Main;
import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.Linker.Option;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("exports")
public final class VjoyInterface {

	public static final int HID_USAGE_RX = 0x33;

	public static final int HID_USAGE_RY = 0x34;

	public static final int HID_USAGE_RZ = 0x35;

	public static final int HID_USAGE_SL0 = 0x36;

	public static final int HID_USAGE_SL1 = 0x37;

	public static final int HID_USAGE_X = 0x30;

	public static final int HID_USAGE_Y = 0x31;

	public static final int HID_USAGE_Z = 0x32;

	public static final int VJD_STAT_FREE = 1;

	public static final String VJOY_LIBRARY_FILENAME = "vJoyInterface.dll";

	static final Linker LINKER = Linker.nativeLinker();

	private static final Logger LOGGER = Logger.getLogger(VjoyInterface.class.getName());

	private static MethodHandle AcquireVJDMethodHandle;

	private static MethodHandle DriverMatchMethodHandle;

	private static MethodHandle GetVJDAxisExistMethodHandle;

	private static MethodHandle GetVJDAxisMaxMethodHandle;

	private static MethodHandle GetVJDAxisMinMethodHandle;

	private static MethodHandle GetVJDButtonNumberMethodHandle;

	private static MethodHandle GetVJDStatusMethodHandle;

	private static MethodHandle RelinquishVJDMethodHandle;

	private static MethodHandle ResetButtonsMethodHandle;

	private static MethodHandle ResetVJDMethodHandle;

	private static MethodHandle SetAxisMethodHandle;

	private static MethodHandle SetBtnMethodHandle;

	private static boolean initialized = false;

	private static MethodHandle vJoyEnabled;

	private VjoyInterface() {
	}

	public static boolean AcquireVJD(final int rID) {
		try {
			return (int) AcquireVJDMethodHandle.invoke(rID) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static boolean DriverMatch(final MemorySegment dllVer, final MemorySegment drvVer) {
		try {
			return (int) DriverMatchMethodHandle.invoke(dllVer, drvVer) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static boolean GetVJDAxisExist(final int rID, final int axis) {
		try {
			return (int) GetVJDAxisExistMethodHandle.invoke(rID, axis) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static boolean GetVJDAxisMax(final int rID, @SuppressWarnings("SameParameterValue") final int axis,
			final MemorySegment max) {
		try {
			return (int) GetVJDAxisMaxMethodHandle.invoke(rID, axis, max) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static boolean GetVJDAxisMin(final int rID, @SuppressWarnings("SameParameterValue") final int axis,
			final MemorySegment min) {
		try {
			return (int) GetVJDAxisMinMethodHandle.invoke(rID, axis, min) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static int GetVJDButtonNumber(final int rID) {
		try {
			return (int) GetVJDButtonNumberMethodHandle.invoke(rID);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static int GetVJDStatus(final int rID) {
		try {
			return (int) GetVJDStatusMethodHandle.invoke(rID);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static String GetVJoyArchFolderName() {
		if ("amd64".equals(Main.OS_ARCH)) {
			return "x64";
		}

		return Main.OS_ARCH;
	}

	public static void RelinquishVJD(final int rID) {
		try {
			RelinquishVJDMethodHandle.invoke(rID);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	@SuppressWarnings("UnusedReturnValue")
	public static boolean ResetButtons(final int rID) {
		try {
			return (int) ResetButtonsMethodHandle.invoke(rID) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static boolean ResetVJD(final int rID) {
		try {
			return (int) ResetVJDMethodHandle.invoke(rID) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static boolean SetAxis(final int value, final int rID, final int axis) {
		try {
			return (int) SetAxisMethodHandle.invoke(value, rID, axis) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static boolean SetBtn(final boolean value, final int rID, final byte nBtn) {
		try {
			return (int) SetBtnMethodHandle.invoke(value ? 1 : 0, rID, nBtn) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static void init(final Main main) {
		final var vJoyPath = main.getVJoyDirectory();
		final var libraryPathFile = new File(vJoyPath, GetVJoyArchFolderName());

		LOGGER.log(Level.INFO, "Using vJoy library path: " + libraryPathFile.getAbsolutePath());

		final var symbolLookup = SymbolLookup.libraryLookup(libraryPathFile.toPath().resolve(VJOY_LIBRARY_FILENAME),
				Arena.global());

		AcquireVJDMethodHandle = LINKER.downcallHandle(symbolLookup.find("AcquireVJD").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

		DriverMatchMethodHandle = LINKER.downcallHandle(symbolLookup.find("DriverMatch").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

		GetVJDAxisExistMethodHandle = LINKER.downcallHandle(symbolLookup.find("GetVJDAxisExist").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

		GetVJDAxisMaxMethodHandle = LINKER.downcallHandle(symbolLookup.find("GetVJDAxisMax").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
						ValueLayout.ADDRESS));

		GetVJDAxisMinMethodHandle = LINKER.downcallHandle(symbolLookup.find("GetVJDAxisMin").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
						ValueLayout.ADDRESS));

		GetVJDButtonNumberMethodHandle = LINKER.downcallHandle(symbolLookup.find("GetVJDButtonNumber").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

		GetVJDStatusMethodHandle = LINKER.downcallHandle(symbolLookup.find("GetVJDStatus").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

		RelinquishVJDMethodHandle = LINKER.downcallHandle(symbolLookup.find("RelinquishVJD").orElseThrow(),
				FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));

		ResetButtonsMethodHandle = LINKER.downcallHandle(symbolLookup.find("ResetButtons").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

		ResetVJDMethodHandle = LINKER.downcallHandle(symbolLookup.find("ResetVJD").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

		SetAxisMethodHandle = LINKER
				.downcallHandle(
						symbolLookup.find("SetAxis").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT,
								ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT),
						Option.critical(false));

		SetBtnMethodHandle = LINKER
				.downcallHandle(
						symbolLookup.find("SetBtn").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT,
								ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_BYTE),
						Option.critical(false));

		vJoyEnabled = LINKER.downcallHandle(symbolLookup.find("vJoyEnabled").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT));

		initialized = true;
	}

	public static boolean isInitialized() {
		return initialized;
	}

	public static boolean vJoyEnabled() {
		try {
			return (int) vJoyEnabled.invoke() != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}
}

/*
 * Copyright (C) 2025 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
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
import java.util.logging.Logger;

/// Provides Java bindings to the vJoy virtual joystick driver library.
///
/// Uses the Foreign Function and Memory API to interface with the vJoy driver.
/// Functions are loaded dynamically at runtime via [init(Main)][#init(Main)].
@SuppressWarnings({ "exports", "restricted" })
public final class VjoyInterface {

	/// HID usage value for the RX (rotation X) axis.
	public static final int HID_USAGE_RX = 0x33;

	/// HID usage value for the RY (rotation Y) axis.
	public static final int HID_USAGE_RY = 0x34;

	/// HID usage value for the RZ (rotation Z) axis.
	public static final int HID_USAGE_RZ = 0x35;

	/// HID usage value for slider 0.
	public static final int HID_USAGE_SL0 = 0x36;

	/// HID usage value for slider 1.
	public static final int HID_USAGE_SL1 = 0x37;

	/// HID usage value for the X axis.
	public static final int HID_USAGE_X = 0x30;

	/// HID usage value for the Y axis.
	public static final int HID_USAGE_Y = 0x31;

	/// HID usage value for the Z axis.
	public static final int HID_USAGE_Z = 0x32;

	/// vJoy device status indicating the device is free and can be acquired.
	public static final int VJD_STAT_FREE = 1;

	/// Filename of the vJoy native library.
	public static final String VJOY_LIBRARY_FILENAME = "vJoyInterface.dll";

	/// Native linker used to bind to vJoy library functions.
	static final Linker LINKER = Linker.nativeLinker();

	private static final Logger LOGGER = Logger.getLogger(VjoyInterface.class.getName());

	/// Method handle for the `AcquireVJD` native function.
	private static MethodHandle AcquireVJDMethodHandle;

	/// Method handle for the `DriverMatch` native function.
	private static MethodHandle DriverMatchMethodHandle;

	/// Method handle for the `GetVJDAxisExist` native function.
	private static MethodHandle GetVJDAxisExistMethodHandle;

	/// Method handle for the `GetVJDAxisMax` native function.
	private static MethodHandle GetVJDAxisMaxMethodHandle;

	/// Method handle for the `GetVJDAxisMin` native function.
	private static MethodHandle GetVJDAxisMinMethodHandle;

	/// Method handle for the `GetVJDButtonNumber` native function.
	private static MethodHandle GetVJDButtonNumberMethodHandle;

	/// Method handle for the `GetVJDStatus` native function.
	private static MethodHandle GetVJDStatusMethodHandle;

	/// Method handle for the `RelinquishVJD` native function.
	private static MethodHandle RelinquishVJDMethodHandle;

	/// Method handle for the `ResetButtons` native function.
	private static MethodHandle ResetButtonsMethodHandle;

	/// Method handle for the `ResetVJD` native function.
	private static MethodHandle ResetVJDMethodHandle;

	/// Method handle for the `SetAxis` native function.
	private static MethodHandle SetAxisMethodHandle;

	/// Method handle for the `SetBtn` native function.
	private static MethodHandle SetBtnMethodHandle;

	/// Whether the vJoy library has been successfully initialized.
	private static boolean initialized = false;

	/// Method handle for the `vJoyEnabled` native function.
	private static MethodHandle vJoyEnabled;

	/// Prevents instantiation.
	private VjoyInterface() {
	}

	/// Acquires ownership of the specified vJoy device.
	///
	/// @param rID the vJoy device ID
	/// @return `true` if the device was successfully acquired
	public static boolean AcquireVJD(final int rID) {
		try {
			return (int) AcquireVJDMethodHandle.invoke(rID) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Checks whether the vJoy DLL version matches the vJoy driver version.
	///
	/// @param dllVer memory segment to receive the DLL version
	/// @param drvVer memory segment to receive the driver version
	/// @return `true` if the DLL and driver versions match
	public static boolean DriverMatch(final MemorySegment dllVer, final MemorySegment drvVer) {
		try {
			return (int) DriverMatchMethodHandle.invoke(dllVer, drvVer) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Checks whether the specified axis exists on the given vJoy device.
	///
	/// @param rID the vJoy device ID
	/// @param axis the axis HID usage value
	/// @return `true` if the axis exists
	public static boolean GetVJDAxisExist(final int rID, final int axis) {
		try {
			return (int) GetVJDAxisExistMethodHandle.invoke(rID, axis) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Retrieves the maximum value of the specified axis on the given vJoy device.
	///
	/// @param rID the vJoy device ID
	/// @param axis the axis identifier
	/// @param max the memory segment to store the maximum value
	/// @return `true` if the operation was successful
	public static boolean GetVJDAxisMax(final int rID, @SuppressWarnings("SameParameterValue") final int axis,
			final MemorySegment max) {
		try {
			return (int) GetVJDAxisMaxMethodHandle.invoke(rID, axis, max) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Retrieves the minimum value of the specified axis on the given vJoy device.
	///
	/// @param rID the vJoy device ID
	/// @param axis the axis identifier
	/// @param min the memory segment to store the minimum value
	/// @return `true` if the operation was successful
	public static boolean GetVJDAxisMin(final int rID, @SuppressWarnings("SameParameterValue") final int axis,
			final MemorySegment min) {
		try {
			return (int) GetVJDAxisMinMethodHandle.invoke(rID, axis, min) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Returns the number of buttons on the specified vJoy device.
	///
	/// @param rID the vJoy device ID
	/// @return the number of buttons
	public static int GetVJDButtonNumber(final int rID) {
		try {
			return (int) GetVJDButtonNumberMethodHandle.invoke(rID);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Returns the status of the specified vJoy device.
	///
	/// @param rID the vJoy device ID
	/// @return the device status code
	public static int GetVJDStatus(final int rID) {
		try {
			return (int) GetVJDStatusMethodHandle.invoke(rID);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Returns the architecture-specific folder name for the vJoy library.
	///
	/// @return the architecture-specific folder name
	public static String GetVJoyArchFolderName() {
		if ("amd64".equals(Main.OS_ARCH)) {
			return "x64";
		}

		return Main.OS_ARCH;
	}

	/// Releases ownership of the specified vJoy device.
	///
	/// @param rID the vJoy device ID
	public static void RelinquishVJD(final int rID) {
		try {
			RelinquishVJDMethodHandle.invoke(rID);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Resets all buttons on the specified vJoy device to their default state.
	///
	/// @param rID the vJoy device ID
	/// @return `true` if the operation was successful
	@SuppressWarnings("UnusedReturnValue")
	public static boolean ResetButtons(final int rID) {
		try {
			return (int) ResetButtonsMethodHandle.invoke(rID) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Resets the specified vJoy device to its default state.
	///
	/// @param rID the vJoy device ID
	/// @return `true` if the operation was successful
	public static boolean ResetVJD(final int rID) {
		try {
			return (int) ResetVJDMethodHandle.invoke(rID) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Sets the value of the specified axis on the given vJoy device.
	///
	/// @param value the axis value to set
	/// @param rID the vJoy device ID
	/// @param axis the axis identifier
	/// @return `true` if the operation was successful
	public static boolean SetAxis(final int value, final int rID, final int axis) {
		try {
			return (int) SetAxisMethodHandle.invoke(value, rID, axis) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Sets the state of a button on the specified vJoy device.
	///
	/// @param value the button state to set
	/// @param rID the vJoy device ID
	/// @param nBtn the button number
	/// @return `true` if the operation was successful
	public static boolean SetBtn(final boolean value, final int rID, final byte nBtn) {
		try {
			return (int) SetBtnMethodHandle.invoke(value ? 1 : 0, rID, nBtn) != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Initializes the vJoy library by loading native function handles from the
	/// vJoy installation directory.
	///
	/// @param main the application main instance providing the vJoy directory
	public static void init(final Main main) {
		final var vJoyPath = main.getVJoyDirectory();
		final var libraryPathFile = new File(vJoyPath, GetVJoyArchFolderName());

		LOGGER.info("Using vJoy library path: " + libraryPathFile.getAbsolutePath());

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

	/// Returns whether the vJoy library has been initialized.
	///
	/// @return `true` if the vJoy library has been initialized
	public static boolean isInitialized() {
		return initialized;
	}

	/// Returns whether the vJoy driver is enabled and running.
	///
	/// @return `true` if the vJoy driver is enabled
	public static boolean vJoyEnabled() {
		try {
			return (int) vJoyEnabled.invokeExact() != 0;
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}
}

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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/// Provides Java bindings to the Windows Kernel32 native library using the
/// Foreign Function and Memory API.
///
/// All native functions are loaded at class initialization time via a
/// global [Arena] and exposed as static methods. This class cannot
/// be instantiated.
@SuppressWarnings("restricted")
public final class Kernel32 {

	/// Method handle for the native `GetLastError` function.
	private static final MethodHandle GET_LAST_ERROR_METHOD_HANDLE;

	static {
		final var linker = Linker.nativeLinker();
		final var symbolLookup = SymbolLookup.libraryLookup(System.mapLibraryName("Kernel32"), Arena.global());

		GET_LAST_ERROR_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("GetLastError"),
				FunctionDescriptor.of(ValueLayout.JAVA_INT));
	}

	/// Prevents instantiation.
	private Kernel32() {
	}

	/// Retrieves the calling thread's last-error code value.
	///
	/// @return the calling thread's last-error code
	public static int GetLastError() {
		try {
			return (int) GET_LAST_ERROR_METHOD_HANDLE.invokeExact();
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}
}

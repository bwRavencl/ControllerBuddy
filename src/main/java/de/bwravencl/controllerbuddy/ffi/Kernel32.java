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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

@SuppressWarnings("exports")
public final class Kernel32 {

	private static final Linker LINKER = Linker.nativeLinker();

	private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.libraryLookup(System.mapLibraryName("Kernel32"),
			Arena.global());

	private static final MethodHandle GET_LAST_ERROR_METHOD_HANDLE = LINKER
			.downcallHandle(SYMBOL_LOOKUP.findOrThrow("GetLastError"), FunctionDescriptor.of(ValueLayout.JAVA_INT));

	private Kernel32() {
	}

	public static int GetLastError() {
		try {
			return (int) GET_LAST_ERROR_METHOD_HANDLE.invoke();
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}
}

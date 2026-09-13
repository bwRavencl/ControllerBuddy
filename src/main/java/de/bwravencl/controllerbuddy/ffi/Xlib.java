/*
 * Copyright (C) 2026 Matteo Hausner
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

/// Provides Java bindings to the Xlib native library.
///
/// Uses the Foreign Function and Memory API to expose functions for managing
/// X11 displays, inter-client communication, window properties, and event
/// synchronization.
@SuppressWarnings({ "restricted" })
public final class Xlib {

	/// Method handle for the native `XChangeProperty` function.
	public static final MethodHandle XChangeProperty;

	/// Method handle for the native `XCloseDisplay` function.
	public static final MethodHandle XCloseDisplay;

	/// Method handle for the native `XFlush` function.
	public static final MethodHandle XFlush;

	/// Method handle for the native `XInternAtom` function.
	public static final MethodHandle XInternAtom;

	/// Method handle for the native `XOpenDisplay` function.
	public static final MethodHandle XOpenDisplay;

	static {
		final var linker = Linker.nativeLinker();
		final var symbolLookup = SymbolLookup.libraryLookup("libX11.so.6", Arena.global());

		XChangeProperty = linker.downcallHandle(symbolLookup.find("XChangeProperty").orElseThrow(),
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
						ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
						ValueLayout.JAVA_INT));

		XCloseDisplay = linker.downcallHandle(symbolLookup.find("XCloseDisplay").orElseThrow(),
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

		XFlush = linker.downcallHandle(symbolLookup.find("XFlush").orElseThrow(),
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
		XInternAtom = linker.downcallHandle(symbolLookup.find("XInternAtom").orElseThrow(), FunctionDescriptor
				.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

		XOpenDisplay = linker.downcallHandle(symbolLookup.find("XOpenDisplay").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
	}
}

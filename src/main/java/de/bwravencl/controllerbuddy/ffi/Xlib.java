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
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/// Provides Java bindings to the Xlib native library using the Foreign Function
/// and Memory API.
///
/// This class exposes native functions for managing X11 displays, inter-client
/// communication, window properties, and event synchronization, as well as
/// memory layout definitions for the event structures used to send client
/// messages.
@SuppressWarnings("restricted")
public final class Xlib {

	/// `CWOverrideRedirect` window attribute value mask bit.
	public static final long CWOverrideRedirect = 1L << 9;

	/// Method handle for the native `XChangeProperty` function.
	private static final MethodHandle X_CHANGE_PROPERTY_METHOD_HANDLE;

	/// Method handle for the native `XChangeWindowAttributes` function.
	private static final MethodHandle X_CHANGE_WINDOW_ATTRIBUTES_METHOD_HANDLE;

	/// Method handle for the native `XCloseDisplay` function.
	private static final MethodHandle X_CLOSE_DISPLAY_METHOD_HANDLE;

	/// Method handle for the native `XFlush` function.
	private static final MethodHandle X_FLUSH_METHOD_HANDLE;

	/// Method handle for the native `XInternAtom` function.
	private static final MethodHandle X_INTERN_ATOM_METHOD_HANDLE;

	/// Method handle for the native `XOpenDisplay` function.
	private static final MethodHandle X_OPEN_DISPLAY_METHOD_HANDLE;

	static {
		final var linker = Linker.nativeLinker();
		final var symbolLookup = SymbolLookup.libraryLookup("libX11.so.6", Arena.global());

		X_CHANGE_PROPERTY_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XChangeProperty"),
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
						ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
						ValueLayout.JAVA_INT));

		X_CLOSE_DISPLAY_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XCloseDisplay"),
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

		X_FLUSH_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XFlush"),
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

		X_INTERN_ATOM_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XInternAtom"), FunctionDescriptor
				.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

		X_OPEN_DISPLAY_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XOpenDisplay"),
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

		X_CHANGE_WINDOW_ATTRIBUTES_METHOD_HANDLE = linker.downcallHandle(
				symbolLookup.findOrThrow("XChangeWindowAttributes"), FunctionDescriptor.of(ValueLayout.JAVA_INT,
						ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
	}

	/// Prevents instantiation.
	private Xlib() {
	}

	/// Changes the property of the specified window.
	///
	/// @param display the connection to the X server
	/// @param w the window whose property to change
	/// @param property the atom of the property
	/// @param type the atom of the property type
	/// @param format the format of the property data (8, 16 or 32)
	/// @param mode the mode of the operation (replace, prepend or append)
	/// @param data the property data
	/// @param nelements the number of elements in the property data
	public static void XChangeProperty(final MemorySegment display, final long w, final long property, final long type,
			final int format, final int mode, final MemorySegment data, final int nelements) {
		try {
			X_CHANGE_PROPERTY_METHOD_HANDLE.invokeExact(display, w, property, type, format, mode, data, nelements);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Changes the attributes of the specified window.
	///
	/// @param display the connection to the X server
	/// @param w the window whose attributes to change
	/// @param valuemask the bitmask of the attributes to change (e.g.,
	/// [#CWOverrideRedirect])
	/// @param attributes the attribute values as an [XSetWindowAttributes] memory
	/// segment
	/// @return the status of the request
	@SuppressWarnings("UnusedReturnValue")
	public static int XChangeWindowAttributes(final MemorySegment display, final long w, final long valuemask,
			final MemorySegment attributes) {
		try {
			return (int) X_CHANGE_WINDOW_ATTRIBUTES_METHOD_HANDLE.invokeExact(display, w, valuemask, attributes);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Closes the connection to the X server.
	///
	/// @param display the connection to the X server
	public static void XCloseDisplay(final MemorySegment display) {
		try {
			X_CLOSE_DISPLAY_METHOD_HANDLE.invokeExact(display);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Flushes the output buffer.
	///
	/// @param display the connection to the X server
	public static void XFlush(final MemorySegment display) {
		try {
			X_FLUSH_METHOD_HANDLE.invokeExact(display);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Returns the atom identifier for the given atom name.
	///
	/// @param display the connection to the X server
	/// @param atom_name the null-terminated name of the atom as a memory segment
	/// @param only_if_exists non-zero to return `None` instead of creating the atom
	/// if it does not exist
	/// @return the atom identifier, or `None`
	public static long XInternAtom(final MemorySegment display, final MemorySegment atom_name,
			final int only_if_exists) {
		try {
			return (long) X_INTERN_ATOM_METHOD_HANDLE.invokeExact(display, atom_name, only_if_exists);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Opens a connection to the X server.
	///
	/// @param display_name the null-terminated display name as a memory segment, or
	/// `NULL` to use the `DISPLAY` environment variable
	/// @return the connection to the X server, or `NULL` on failure
	public static MemorySegment XOpenDisplay(final MemorySegment display_name) {
		try {
			return (MemorySegment) X_OPEN_DISPLAY_METHOD_HANDLE.invokeExact(display_name);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Represents the `XSetWindowAttributes` structure used to change window
	/// attributes.
	///
	/// Provides a [GroupLayout] constant for LP64 platforms and static setter
	/// methods for populating the fields of an `XSetWindowAttributes` memory
	/// segment.
	public static final class XSetWindowAttributes {

		/// Field name for the background pixel value.
		private static final String BACKGROUND_PIXEL_NAME = "background_pixel";

		/// Field name for the background pixmap.
		private static final String BACKGROUND_PIXMAP_NAME = "background_pixmap";

		/// Field name for the backing pixel value.
		private static final String BACKING_PIXEL_NAME = "backing_pixel";

		/// Field name for the backing planes.
		private static final String BACKING_PLANES_NAME = "backing_planes";

		/// Field name for the backing store hint.
		private static final String BACKING_STORE_NAME = "backing_store";

		/// Field name for the bit gravity.
		private static final String BIT_GRAVITY_NAME = "bit_gravity";

		/// Field name for the border pixel value.
		private static final String BORDER_PIXEL_NAME = "border_pixel";

		/// Field name for the border pixmap.
		private static final String BORDER_PIXMAP_NAME = "border_pixmap";

		/// Field name for the colormap.
		private static final String COLORMAP_NAME = "colormap";

		/// Field name for the cursor.
		private static final String CURSOR_NAME = "cursor";

		/// Field name for the do-not-propagate event mask.
		private static final String DO_NOT_PROPAGATE_MASK_NAME = "do_not_propagate_mask";

		/// Field name for the event mask.
		private static final String EVENT_MASK_NAME = "event_mask";

		/// Field name for the override redirect flag.
		private static final String OVERRIDE_REDIRECT_NAME = "override_redirect";

		/// Field name for the save under flag.
		private static final String SAVE_UNDER_NAME = "save_under";

		/// Field name for the window gravity.
		private static final String WIN_GRAVITY_NAME = "win_gravity";

		/// Memory layout of the `XSetWindowAttributes` struct on LP64 platforms.
		public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
				ValueLayout.JAVA_LONG.withName(BACKGROUND_PIXMAP_NAME),
				ValueLayout.JAVA_LONG.withName(BACKGROUND_PIXEL_NAME),
				ValueLayout.JAVA_LONG.withName(BORDER_PIXMAP_NAME), ValueLayout.JAVA_LONG.withName(BORDER_PIXEL_NAME),
				ValueLayout.JAVA_INT.withName(BIT_GRAVITY_NAME), ValueLayout.JAVA_INT.withName(WIN_GRAVITY_NAME),
				ValueLayout.JAVA_INT.withName(BACKING_STORE_NAME), MemoryLayout.paddingLayout(4),
				ValueLayout.JAVA_LONG.withName(BACKING_PLANES_NAME), ValueLayout.JAVA_LONG.withName(BACKING_PIXEL_NAME),
				ValueLayout.JAVA_INT.withName(SAVE_UNDER_NAME), MemoryLayout.paddingLayout(4),
				ValueLayout.JAVA_LONG.withName(EVENT_MASK_NAME),
				ValueLayout.JAVA_LONG.withName(DO_NOT_PROPAGATE_MASK_NAME),
				ValueLayout.JAVA_INT.withName(OVERRIDE_REDIRECT_NAME), MemoryLayout.paddingLayout(4),
				ValueLayout.JAVA_LONG.withName(COLORMAP_NAME), ValueLayout.JAVA_LONG.withName(CURSOR_NAME));

		/// VarHandle for accessing the `override_redirect` field.
		public static final VarHandle OVERRIDE_REDIRECT_VAR_HANDLE = LAYOUT
				.varHandle(PathElement.groupElement(OVERRIDE_REDIRECT_NAME));

		/// Prevents instantiation.
		private XSetWindowAttributes() {
		}
	}
}

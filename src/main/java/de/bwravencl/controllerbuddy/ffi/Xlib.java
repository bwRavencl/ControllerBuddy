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
/// memory layout definitions for the Xlib event structures used to send client
/// messages.
@SuppressWarnings("restricted")
public final class Xlib {

	/// `ClientMessage` event type.
	public static final int ClientMessage = 33;

	/// `SubstructureNotifyMask` event mask.
	public static final long SubstructureNotifyMask = 1L << 19;

	/// `SubstructureRedirectMask` event mask.
	public static final long SubstructureRedirectMask = 1L << 20;

	/// Method handle for the native `XChangeProperty` function.
	private static final MethodHandle X_CHANGE_PROPERTY_METHOD_HANDLE;

	/// Method handle for the native `XCloseDisplay` function.
	private static final MethodHandle X_CLOSE_DISPLAY_METHOD_HANDLE;

	/// Method handle for the native `XDefaultRootWindow` function.
	private static final MethodHandle X_DEFAULT_ROOT_WINDOW_METHOD_HANDLE;

	/// Method handle for the native `XFlush` function.
	private static final MethodHandle X_FLUSH_METHOD_HANDLE;

	/// Method handle for the native `XInternAtom` function.
	private static final MethodHandle X_INTERN_ATOM_METHOD_HANDLE;

	/// Method handle for the native `XOpenDisplay` function.
	private static final MethodHandle X_OPEN_DISPLAY_METHOD_HANDLE;

	/// Method handle for the native `XSendEvent` function.
	private static final MethodHandle X_SEND_EVENT_METHOD_HANDLE;

	static {
		final var linker = Linker.nativeLinker();
		final var symbolLookup = SymbolLookup.libraryLookup("libX11.so.6", Arena.global());

		X_CHANGE_PROPERTY_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XChangeProperty"),
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
						ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
						ValueLayout.JAVA_INT));

		X_CLOSE_DISPLAY_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XCloseDisplay"),
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

		X_DEFAULT_ROOT_WINDOW_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XDefaultRootWindow"),
				FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

		X_FLUSH_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XFlush"),
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

		X_INTERN_ATOM_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XInternAtom"), FunctionDescriptor
				.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

		X_OPEN_DISPLAY_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XOpenDisplay"),
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

		X_SEND_EVENT_METHOD_HANDLE = linker.downcallHandle(symbolLookup.findOrThrow("XSendEvent"),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
						ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
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

	/// Returns the root window of the default screen.
	///
	/// @param display the connection to the X server
	/// @return the root window of the default screen
	public static long XDefaultRootWindow(final MemorySegment display) {
		try {
			return (long) X_DEFAULT_ROOT_WINDOW_METHOD_HANDLE.invokeExact(display);
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

	/// Sends an event to the specified window.
	///
	/// @param display the connection to the X server
	/// @param w the window the event is sent to
	/// @param propagate non-zero to propagate the event
	/// @param event_mask the event mask
	/// @param event_send the event to send as an [XEvent] memory segment
	/// @return zero if the conversion to wire protocol format failed, otherwise
	/// non-zero
	@SuppressWarnings("UnusedReturnValue")
	public static int XSendEvent(final MemorySegment display, final long w, final int propagate, final long event_mask,
			final MemorySegment event_send) {
		try {
			return (int) X_SEND_EVENT_METHOD_HANDLE.invokeExact(display, w, propagate, event_mask, event_send);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Represents the Xlib `XClientMessageEvent` structure used for inter-client
	/// communication.
	///
	/// Provides a [GroupLayout] constant for LP64 platforms and static setter
	/// methods for populating the fields of an `XClientMessageEvent` memory
	/// segment.
	public static final class XClientMessageEvent {

		/// Number of elements in the `data.l` array.
		private static final int DATA_L_LENGTH = 5;

		/// Field name for the message data.
		private static final String DATA_NAME = "data";

		/// Field name for the display connection.
		private static final String DISPLAY_NAME = "display";

		/// Field name for the data format.
		private static final String FORMAT_NAME = "format";

		/// Field name for the message type atom.
		private static final String MESSAGE_TYPE_NAME = "message_type";

		/// Field name for the send event flag.
		private static final String SEND_EVENT_NAME = "send_event";

		/// Field name for the request serial number.
		private static final String SERIAL_NAME = "serial";

		/// Field name for the event type.
		private static final String TYPE_NAME = "type";

		/// Field name for the target window.
		private static final String WINDOW_NAME = "window";

		/// Memory layout of the Xlib `XClientMessageEvent` struct on LP64 platforms.
		public static final GroupLayout LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName(TYPE_NAME),
				MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG.withName(SERIAL_NAME),
				ValueLayout.JAVA_INT.withName(SEND_EVENT_NAME), MemoryLayout.paddingLayout(4),
				ValueLayout.ADDRESS.withName(DISPLAY_NAME), ValueLayout.JAVA_LONG.withName(WINDOW_NAME),
				ValueLayout.JAVA_LONG.withName(MESSAGE_TYPE_NAME), ValueLayout.JAVA_INT.withName(FORMAT_NAME),
				MemoryLayout.paddingLayout(4),
				MemoryLayout.sequenceLayout(DATA_L_LENGTH, ValueLayout.JAVA_LONG).withName(DATA_NAME));

		/// Byte offset of the `data` field.
		private static final long DATA_OFFSET = LAYOUT.byteOffset(PathElement.groupElement(DATA_NAME));

		/// VarHandle for accessing the `display` field.
		private static final VarHandle DISPLAY_VAR_HANDLE = LAYOUT.varHandle(PathElement.groupElement(DISPLAY_NAME));

		/// VarHandle for accessing the `format` field.
		private static final VarHandle FORMAT_VAR_HANDLE = LAYOUT.varHandle(PathElement.groupElement(FORMAT_NAME));

		/// VarHandle for accessing the `message_type` field.
		private static final VarHandle MESSAGE_TYPE_VAR_HANDLE = LAYOUT
				.varHandle(PathElement.groupElement(MESSAGE_TYPE_NAME));

		/// VarHandle for accessing the `type` field.
		private static final VarHandle TYPE_VAR_HANDLE = LAYOUT.varHandle(PathElement.groupElement(TYPE_NAME));

		/// VarHandle for accessing the `window` field.
		private static final VarHandle WINDOW_VAR_HANDLE = LAYOUT.varHandle(PathElement.groupElement(WINDOW_NAME));

		/// Prevents instantiation.
		private XClientMessageEvent() {
		}

		/// Sets the `data.l` array in the given `XClientMessageEvent` memory segment.
		///
		/// @param seg the memory segment representing the `XClientMessageEvent` struct
		/// @param l the values to set, at most five
		/// @throws IllegalArgumentException if more than five values are given
		public static void setData_l(final MemorySegment seg, final long[] l) {
			if (l.length > DATA_L_LENGTH) {
				throw new IllegalArgumentException("At most " + DATA_L_LENGTH + " values are allowed");
			}

			MemorySegment.copy(l, 0, seg, ValueLayout.JAVA_LONG, DATA_OFFSET, l.length);
		}

		/// Sets the display field in the given `XClientMessageEvent` memory segment.
		///
		/// @param seg the memory segment representing the `XClientMessageEvent` struct
		/// @param display the connection to the X server
		public static void setDisplay(final MemorySegment seg, final MemorySegment display) {
			DISPLAY_VAR_HANDLE.set(seg, 0L, display);
		}

		/// Sets the format field in the given `XClientMessageEvent` memory segment.
		///
		/// @param seg the memory segment representing the `XClientMessageEvent` struct
		/// @param format the format of the message data (8, 16 or 32)
		public static void setFormat(final MemorySegment seg, final int format) {
			FORMAT_VAR_HANDLE.set(seg, 0L, format);
		}

		/// Sets the message type field in the given `XClientMessageEvent` memory
		/// segment.
		///
		/// @param seg the memory segment representing the `XClientMessageEvent` struct
		/// @param message_type the atom of the message type
		public static void setMessage_type(final MemorySegment seg, final long message_type) {
			MESSAGE_TYPE_VAR_HANDLE.set(seg, 0L, message_type);
		}

		/// Sets the type field in the given `XClientMessageEvent` memory segment.
		///
		/// @param seg the memory segment representing the `XClientMessageEvent` struct
		/// @param type the event type
		public static void setType(final MemorySegment seg, final int type) {
			TYPE_VAR_HANDLE.set(seg, 0L, type);
		}

		/// Sets the window field in the given `XClientMessageEvent` memory segment.
		///
		/// @param seg the memory segment representing the `XClientMessageEvent` struct
		/// @param window the window the message refers to
		public static void setWindow(final MemorySegment seg, final long window) {
			WINDOW_VAR_HANDLE.set(seg, 0L, window);
		}
	}

	/// Represents the Xlib `XEvent` union that holds any X event.
	///
	/// Provides a [GroupLayout] constant for LP64 platforms and static accessor
	/// methods for retrieving member slices from an `XEvent` memory segment.
	public static final class XEvent {

		/// Number of elements in the `pad` array.
		private static final int PAD_LENGTH = 24;

		/// Field name for the padding array.
		private static final String PAD_NAME = "pad";

		/// Field name for the client message event member.
		private static final String XCLIENT_NAME = "xclient";

		/// Memory layout of the Xlib `XEvent` union on LP64 platforms.
		public static final GroupLayout LAYOUT = MemoryLayout.unionLayout(
				XClientMessageEvent.LAYOUT.withName(XCLIENT_NAME),
				MemoryLayout.sequenceLayout(PAD_LENGTH, ValueLayout.JAVA_LONG).withName(PAD_NAME));

		/// Prevents instantiation.
		private XEvent() {
		}

		/// Returns the `xclient` member slice from the given `XEvent` memory segment.
		///
		/// @param seg the memory segment representing the `XEvent` union
		/// @return the `xclient` member slice
		public static MemorySegment getXclient(final MemorySegment seg) {
			return seg.asSlice(0L, XClientMessageEvent.LAYOUT);
		}
	}
}

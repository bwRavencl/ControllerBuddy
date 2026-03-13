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
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.Linker.Option;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/// Provides Java bindings to the Windows User32 native library.
///
/// Uses the Foreign Function and Memory API to expose functions for keyboard
/// and mouse input simulation, key state retrieval, and window positioning.
@SuppressWarnings({ "exports", "restricted", "UnusedReturnValue" })
public final class User32 {

	/// Windows `HWND_TOPMOST` constant for placing a window at the top of the Z
	/// order.
	public static final MemorySegment HWND_TOPMOST = MemorySegment.ofAddress(-1L);

	/// Windows `SWP_NOMOVE` flag to retain the current position when repositioning
	/// a window.
	public static final int SWP_NOMOVE = 2;

	/// Windows `SWP_NOSIZE` flag to retain the current size when repositioning a
	/// window.
	public static final int SWP_NOSIZE = 1;

	/// Native linker used to bind to User32 functions.
	private static final Linker LINKER = Linker.nativeLinker();

	/// Symbol lookup for the User32 native library.
	private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.libraryLookup(System.mapLibraryName("User32"),
			Arena.global());

	/// Method handle for the `GetKeyState` native function.
	private static final MethodHandle GET_KEY_STATE_METHOD_HANDLE = LINKER.downcallHandle(
			SYMBOL_LOOKUP.findOrThrow("GetKeyState"),
			FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.JAVA_INT), Option.critical(false));

	/// Method handle for the `SendInput` native function.
	private static final MethodHandle SEND_INPUT_METHOD_HANDLE = LINKER
			.downcallHandle(SYMBOL_LOOKUP.findOrThrow("SendInput"), FunctionDescriptor.of(ValueLayout.JAVA_INT,
					ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT), Option.critical(false));

	/// Method handle for the `SetWindowPos` native function.
	private static final MethodHandle SET_WINDOW_POS_METHOD_HANDLE = LINKER
			.downcallHandle(SYMBOL_LOOKUP.findOrThrow("SetWindowPos"),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
							ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
							ValueLayout.JAVA_INT));

	/// Prevents instantiation.
	private User32() {
	}

	/// Retrieves the status of the specified virtual key.
	///
	/// @param nVirtKey the virtual-key code
	/// @return the status of the specified virtual key
	public static short GetKeyState(final int nVirtKey) {
		try {
			return (short) GET_KEY_STATE_METHOD_HANDLE.invoke(nVirtKey);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Synthesizes keystrokes, mouse motions, and button clicks by inserting input
	/// events into the input stream.
	///
	/// @param cInputs the number of structures in the pInputs array
	/// @param pInputs a memory segment pointing to an array of INPUT structures
	/// @param cbSize the size, in bytes, of an INPUT structure
	/// @return the number of events that were successfully inserted into the input
	/// stream
	public static int SendInput(@SuppressWarnings("SameParameterValue") final int cInputs, final MemorySegment pInputs,
			final int cbSize) {
		try {
			return (int) SEND_INPUT_METHOD_HANDLE.invoke(cInputs, pInputs, cbSize);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Changes the size, position, and Z order of a window.
	///
	/// @param hWnd handle to the window
	/// @param hWndInsertAfter handle to the window to precede the positioned window
	/// in the Z order
	/// @param X the new position of the left side of the window
	/// @param Y the new position of the top of the window
	/// @param cx the new width of the window
	/// @param cy the new height of the window
	/// @param uFlags the window sizing and positioning flags
	/// @return nonzero if the function succeeds, zero otherwise
	public static int SetWindowPos(final MemorySegment hWnd, final MemorySegment hWndInsertAfter, final int X,
			final int Y, final int cx, final int cy, final int uFlags) {
		try {
			return (int) SET_WINDOW_POS_METHOD_HANDLE.invoke(hWnd, hWndInsertAfter, X, Y, cx, cy, uFlags);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Represents the Windows `INPUT` structure.
	///
	/// Used by `SendInput` to store information for synthesizing keyboard, mouse,
	/// and hardware input events via the corresponding union members.
	public static final class INPUT {

		/// Windows `INPUT_KEYBOARD` type constant indicating keyboard input.
		public static final int INPUT_KEYBOARD = 1;

		/// Windows `INPUT_MOUSE` type constant indicating mouse input.
		public static final int INPUT_MOUSE = 0;

		/// Field name constant for the `type` field in the `INPUT` struct.
		private static final String TYPE_NAME = "type";

		/// Memory layout of the Windows `INPUT` struct.
		public static final GroupLayout LAYOUT = MemoryLayout
				.structLayout(ValueLayout.JAVA_INT.withName(TYPE_NAME), MemoryLayout.paddingLayout(4),
						MemoryLayout
								.unionLayout(INPUT.MOUSEINPUT.LAYOUT.withName("mi"),
										INPUT.KEYBDINPUT.LAYOUT.withName("ki"), INPUT.HARDWAREINPUT.LAYOUT)
								.withName("hi"));

		/// Var handle for the `type` field in the `INPUT` struct.
		private static final VarHandle TYPE_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(TYPE_NAME));

		/// Prevents instantiation.
		private INPUT() {
		}

		/// Returns the KEYBDINPUT union member slice from the given INPUT memory
		/// segment.
		///
		/// @param seg the INPUT memory segment
		/// @return a memory segment slice for the keyboard input data
		public static MemorySegment getKi(final MemorySegment seg) {
			return seg.asSlice(8, 24);
		}

		/// Returns the MOUSEINPUT union member slice from the given INPUT memory
		/// segment.
		///
		/// @param seg the INPUT memory segment
		/// @return a memory segment slice for the mouse input data
		public static MemorySegment getMi(final MemorySegment seg) {
			return seg.asSlice(8, 32);
		}

		/// Sets the type field of the INPUT structure.
		///
		/// @param seg the INPUT memory segment
		/// @param type the input type (e.g., INPUT_MOUSE or INPUT_KEYBOARD)
		public static void setType(final MemorySegment seg, final int type) {
			TYPE_VAR_HANDLE.set(seg, 0L, type);
		}

		/// Represents the Windows `HARDWAREINPUT` structure used to describe simulated
		/// hardware input events.
		///
		/// Provides a [GroupLayout] constant describing the memory layout of the
		/// `HARDWAREINPUT` struct, which carries a message value and two word-sized
		/// parameters used by the raw input system.
		static final class HARDWAREINPUT {

			/// Memory layout of the Windows `HARDWAREINPUT` struct.
			private static final GroupLayout LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName("uMsg"),
					ValueLayout.JAVA_SHORT.withName("wParamL"), ValueLayout.JAVA_SHORT.withName("wParamH"));

			/// Prevents instantiation.
			private HARDWAREINPUT() {
			}
		}

		/// Represents the Windows `KEYBDINPUT` structure.
		///
		/// Contains information about a simulated keyboard event, including the scan
		/// code and key event flags such as extended key and key-up indicators.
		public static final class KEYBDINPUT {

			/// Windows `KEYEVENTF_EXTENDEDKEY` flag indicating an extended key.
			public static final int KEYEVENTF_EXTENDEDKEY = 1;

			/// Windows `KEYEVENTF_KEYUP` flag indicating a key-release event.
			public static final int KEYEVENTF_KEYUP = 2;

			/// Windows `KEYEVENTF_SCANCODE` flag indicating the scan code identifies the
			/// key.
			public static final int KEYEVENTF_SCANCODE = 8;

			/// Field name constant for the `dwFlags` field in the `KEYBDINPUT` struct.
			private static final String DW_FLAGS_NAME = "dwFlags";

			/// Field name constant for the `wScan` field in the `KEYBDINPUT` struct.
			private static final String W_SCAN_NAME = "wScan";

			/// Memory layout of the Windows `KEYBDINPUT` struct.
			private static final GroupLayout LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_SHORT.withName("wVk"),
					ValueLayout.JAVA_SHORT.withName(W_SCAN_NAME), ValueLayout.JAVA_INT.withName(DW_FLAGS_NAME),
					ValueLayout.JAVA_INT.withName("time"), MemoryLayout.paddingLayout(4),
					ValueLayout.JAVA_LONG.withName("dwExtraInfo"));

			/// Var handle for the `dwFlags` field in the `KEYBDINPUT` struct.
			private static final VarHandle DW_FLAGS_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(DW_FLAGS_NAME));

			/// Var handle for the `wScan` field in the `KEYBDINPUT` struct.
			private static final VarHandle W_SCAN_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(W_SCAN_NAME));

			/// Prevents instantiation.
			private KEYBDINPUT() {
			}

			/// Sets the dwFlags field of the KEYBDINPUT structure.
			///
			/// @param seg the KEYBDINPUT memory segment
			/// @param flags the keyboard event flags
			public static void setDwFlags(final MemorySegment seg, final int flags) {
				DW_FLAGS_VAR_HANDLE.set(seg, 0L, flags);
			}

			/// Sets the wScan field of the KEYBDINPUT structure.
			///
			/// @param seg the KEYBDINPUT memory segment
			/// @param wScan the hardware scan code for the key
			public static void setWScan(final MemorySegment seg, final short wScan) {
				W_SCAN_VAR_HANDLE.set(seg, 0L, wScan);
			}
		}

		/// Represents the Windows `MOUSEINPUT` structure.
		///
		/// Contains information about a simulated mouse event, including movement
		/// deltas, button presses, and wheel scrolling data.
		public static final class MOUSEINPUT {

			/// Windows `MOUSEEVENTF_LEFTDOWN` flag for a left button press event.
			public static final int MOUSEEVENTF_LEFTDOWN = 0x0002;

			/// Windows `MOUSEEVENTF_LEFTUP` flag for a left button release event.
			public static final int MOUSEEVENTF_LEFTUP = 0x0004;

			/// Windows `MOUSEEVENTF_MIDDLEDOWN` flag for a middle button press event.
			public static final int MOUSEEVENTF_MIDDLEDOWN = 0x0020;

			/// Windows `MOUSEEVENTF_MIDDLEUP` flag for a middle button release event.
			public static final int MOUSEEVENTF_MIDDLEUP = 0x0040;

			/// Windows `MOUSEEVENTF_MOVE` flag for a mouse movement event.
			public static final int MOUSEEVENTF_MOVE = 0x0001;

			/// Windows `MOUSEEVENTF_RIGHTDOWN` flag for a right button press event.
			public static final int MOUSEEVENTF_RIGHTDOWN = 0x0008;

			/// Windows `MOUSEEVENTF_RIGHTUP` flag for a right button release event.
			public static final int MOUSEEVENTF_RIGHTUP = 0x0010;

			/// Windows `MOUSEEVENTF_WHEEL` flag for a mouse wheel event.
			public static final int MOUSEEVENTF_WHEEL = 0x0800;

			/// Windows `MOUSEEVENTF_XDOWN` flag for an X button press event.
			public static final int MOUSEEVENTF_XDOWN = 0x0080;

			/// Windows `MOUSEEVENTF_XUP` flag for an X button release event.
			public static final int MOUSEEVENTF_XUP = 0x0100;

			/// Windows `XBUTTON1` constant identifying the first X button.
			public static final int XBUTTON1 = 0x0001;

			/// Windows `XBUTTON2` constant identifying the second X button.
			public static final int XBUTTON2 = 0x0002;

			/// Field name constant for the `dwFlags` field in the `MOUSEINPUT` struct.
			private static final String DW_FLAGS_NAME = "dwFlags";

			/// Field name constant for the `dx` field in the `MOUSEINPUT` struct.
			private static final String DX_NAME = "dx";

			/// Field name constant for the `dy` field in the `MOUSEINPUT` struct.
			private static final String DY_NAME = "dy";

			/// Field name constant for the `mouseData` field in the `MOUSEINPUT` struct.
			private static final String MOUSE_DATA_NAME = "mouseData";

			/// Memory layout of the Windows `MOUSEINPUT` struct.
			private static final GroupLayout LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName(DX_NAME),
					ValueLayout.JAVA_INT.withName(DY_NAME), ValueLayout.JAVA_INT.withName(MOUSE_DATA_NAME),
					ValueLayout.JAVA_INT.withName(DW_FLAGS_NAME), ValueLayout.JAVA_INT.withName("time"),
					MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG.withName("dwExtraInfo"));

			/// Var handle for the `dwFlags` field in the `MOUSEINPUT` struct.
			private static final VarHandle DW_FLAGS_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(DW_FLAGS_NAME));

			/// Var handle for the `dx` field in the `MOUSEINPUT` struct.
			private static final VarHandle DX_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(DX_NAME));

			/// Var handle for the `dy` field in the `MOUSEINPUT` struct.
			private static final VarHandle DY_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(DY_NAME));

			/// Var handle for the `mouseData` field in the `MOUSEINPUT` struct.
			private static final VarHandle MOUSE_DATA_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(MOUSE_DATA_NAME));

			/// Prevents instantiation.
			private MOUSEINPUT() {
			}

			/// Sets the dwFlags field of the MOUSEINPUT structure.
			///
			/// @param seg the MOUSEINPUT memory segment
			/// @param dwFlags the mouse event flags
			public static void setDwFlags(final MemorySegment seg, final int dwFlags) {
				DW_FLAGS_VAR_HANDLE.set(seg, 0L, dwFlags);
			}

			/// Sets the dx (absolute or relative X position) field of the MOUSEINPUT
			/// structure.
			///
			/// @param seg the MOUSEINPUT memory segment
			/// @param dx the X position or delta
			public static void setDx(final MemorySegment seg, final int dx) {
				DX_VAR_HANDLE.set(seg, 0L, dx);
			}

			/// Sets the dy (absolute or relative Y position) field of the MOUSEINPUT
			/// structure.
			///
			/// @param seg the MOUSEINPUT memory segment
			/// @param dy the Y position or delta
			public static void setDy(final MemorySegment seg, final int dy) {
				DY_VAR_HANDLE.set(seg, 0L, dy);
			}

			/// Sets the mouseData field of the MOUSEINPUT structure.
			///
			/// @param seg the MOUSEINPUT memory segment
			/// @param mouseData additional mouse data (e.g., wheel delta or X button
			/// identifier)
			public static void setMouseData(final MemorySegment seg, final int mouseData) {
				MOUSE_DATA_VAR_HANDLE.set(seg, 0L, mouseData);
			}
		}
	}
}

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
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.Linker.Option;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

@SuppressWarnings({ "exports", "restricted", "UnusedReturnValue" })
public final class User32 {

	public static final MemorySegment HWND_TOPMOST = MemorySegment.ofAddress(-1L);

	public static final int SWP_NOMOVE = 2;

	public static final int SWP_NOSIZE = 1;

	private static final Linker LINKER = Linker.nativeLinker();

	private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.libraryLookup(System.mapLibraryName("User32"),
			Arena.global());

	private static final MethodHandle GET_KEY_STATE_METHOD_HANDLE = LINKER.downcallHandle(
			SYMBOL_LOOKUP.findOrThrow("GetKeyState"),
			FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.JAVA_INT), Option.critical(false));

	private static final MethodHandle SEND_INPUT_METHOD_HANDLE = LINKER
			.downcallHandle(SYMBOL_LOOKUP.findOrThrow("SendInput"), FunctionDescriptor.of(ValueLayout.JAVA_INT,
					ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT), Option.critical(false));

	private static final MethodHandle SET_WINDOW_POS_METHOD_HANDLE = LINKER
			.downcallHandle(SYMBOL_LOOKUP.findOrThrow("SetWindowPos"),
					FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
							ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
							ValueLayout.JAVA_INT));

	private User32() {
	}

	public static short GetKeyState(final int nVirtKey) {
		try {
			return (short) GET_KEY_STATE_METHOD_HANDLE.invoke(nVirtKey);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static int SendInput(@SuppressWarnings("SameParameterValue") final int cInputs, final MemorySegment pInputs,
			final int cbSize) {
		try {
			return (int) SEND_INPUT_METHOD_HANDLE.invoke(cInputs, pInputs, cbSize);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static int SetWindowPos(final MemorySegment hWnd, final MemorySegment hWndInsertAfter, final int X,
			final int Y, final int cx, final int cy, final int uFlags) {
		try {
			return (int) SET_WINDOW_POS_METHOD_HANDLE.invoke(hWnd, hWndInsertAfter, X, Y, cx, cy, uFlags);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static final class INPUT {

		public static final int INPUT_KEYBOARD = 1;

		public static final int INPUT_MOUSE = 0;

		private static final String TYPE_NAME = "type";

		public static final GroupLayout LAYOUT = MemoryLayout
				.structLayout(ValueLayout.JAVA_INT.withName(TYPE_NAME), MemoryLayout.paddingLayout(4),
						MemoryLayout
								.unionLayout(INPUT.MOUSEINPUT.LAYOUT.withName("mi"),
										INPUT.KEYBDINPUT.LAYOUT.withName("ki"), INPUT.HARDWAREINPUT.LAYOUT)
								.withName("hi"));

		private static final VarHandle TYPE_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(TYPE_NAME));

		public static MemorySegment getKi(final MemorySegment seg) {
			return seg.asSlice(8, 24);
		}

		public static MemorySegment getMi(final MemorySegment seg) {
			return seg.asSlice(8, 32);
		}

		public static void setType(final MemorySegment seg, final int type) {
			TYPE_VAR_HANDLE.set(seg, 0L, type);
		}

		static final class HARDWAREINPUT {

			private static final GroupLayout LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName("uMsg"),
					ValueLayout.JAVA_SHORT.withName("wParamL"), ValueLayout.JAVA_SHORT.withName("wParamH"));
		}

		public static final class KEYBDINPUT {

			public static final int KEYEVENTF_EXTENDEDKEY = 1;

			public static final int KEYEVENTF_KEYUP = 2;

			public static final int KEYEVENTF_SCANCODE = 8;

			private static final String DW_FLAGS_NAME = "dwFlags";

			private static final String W_SCAN_NAME = "wScan";

			private static final GroupLayout LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_SHORT.withName("wVk"),
					ValueLayout.JAVA_SHORT.withName(W_SCAN_NAME), ValueLayout.JAVA_INT.withName(DW_FLAGS_NAME),
					ValueLayout.JAVA_INT.withName("time"), MemoryLayout.paddingLayout(4),
					ValueLayout.JAVA_LONG.withName("dwExtraInfo"));

			private static final VarHandle DW_FLAGS_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(DW_FLAGS_NAME));

			private static final VarHandle W_SCAN_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(W_SCAN_NAME));

			public static void setDwFlags(final MemorySegment seg, final int flags) {
				DW_FLAGS_VAR_HANDLE.set(seg, 0L, flags);
			}

			public static void setWScan(final MemorySegment seg, final short wScan) {
				W_SCAN_VAR_HANDLE.set(seg, 0L, wScan);
			}
		}

		public static final class MOUSEINPUT {

			public static final int MOUSEEVENTF_LEFTDOWN = 0x0002;

			public static final int MOUSEEVENTF_LEFTUP = 0x0004;

			public static final int MOUSEEVENTF_MIDDLEDOWN = 0x0020;

			public static final int MOUSEEVENTF_MIDDLEUP = 0x0040;

			public static final int MOUSEEVENTF_MOVE = 0x0001;

			public static final int MOUSEEVENTF_RIGHTDOWN = 0x0008;

			public static final int MOUSEEVENTF_RIGHTUP = 0x0010;

			public static final int MOUSEEVENTF_WHEEL = 0x0800;

			private static final String DW_FLAGS_NAME = "dwFlags";

			private static final String DX_NAME = "dx";

			private static final String DY_NAME = "dy";

			private static final String MOUSE_DATA_NAME = "mouseData";

			private static final GroupLayout LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName(DX_NAME),
					ValueLayout.JAVA_INT.withName(DY_NAME), ValueLayout.JAVA_INT.withName(MOUSE_DATA_NAME),
					ValueLayout.JAVA_INT.withName(DW_FLAGS_NAME), ValueLayout.JAVA_INT.withName("time"),
					MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG.withName("dwExtraInfo"));

			private static final VarHandle DW_FLAGS_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(DW_FLAGS_NAME));

			private static final VarHandle DX_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(DX_NAME));

			private static final VarHandle DY_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(DY_NAME));

			private static final VarHandle MOUSE_DATA_VAR_HANDLE = LAYOUT
					.varHandle(MemoryLayout.PathElement.groupElement(MOUSE_DATA_NAME));

			public static void setDwFlags(final MemorySegment seg, final int dwFlags) {
				DW_FLAGS_VAR_HANDLE.set(seg, 0L, dwFlags);
			}

			public static void setDx(final MemorySegment seg, final int dx) {
				DX_VAR_HANDLE.set(seg, 0L, dx);
			}

			public static void setDy(final MemorySegment seg, final int dy) {
				DY_VAR_HANDLE.set(seg, 0L, dy);
			}

			public static void setMouseData(final MemorySegment seg, final int mouseData) {
				MOUSE_DATA_VAR_HANDLE.set(seg, 0L, mouseData);
			}
		}
	}
}

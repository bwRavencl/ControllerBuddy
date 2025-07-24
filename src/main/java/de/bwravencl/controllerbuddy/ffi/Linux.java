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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

@SuppressWarnings({ "exports", "restricted", "SameParameterValue" })
public final class Linux {

	public static final int O_NONBLOCK = 4000;

	public static final int O_WRONLY = 1;

	public static final int UINPUT_MAX_NAME_SIZE = 80;

	private static final String ERRNO_NAME = "errno";

	public static final VarHandle ERRNO_VAR_HANDLE = Linker.Option.captureStateLayout()
			.varHandle(PathElement.groupElement(ERRNO_NAME));

	private static final Linker.Option ERRNO_CAPTURE_CALL_STATE = Linker.Option.captureCallState(ERRNO_NAME);

	private static final Linker LINKER = Linker.nativeLinker();

	private static final MethodHandle IOCTL_METHOD_HANDLE = LINKER.downcallHandle(
			LINKER.defaultLookup().findOrThrow("ioctl"), FunctionDescriptor.of(ValueLayout.JAVA_INT,
					ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
			ERRNO_CAPTURE_CALL_STATE, Linker.Option.firstVariadicArg(2));

	private static final MethodHandle CLOSE_METHOD_HANDLE = LINKER.downcallHandle(
			LINKER.defaultLookup().findOrThrow("close"),
			FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT), Linker.Option.critical(false));

	private static final MethodHandle OPEN_METHOD_HANDLE = LINKER.downcallHandle(
			LINKER.defaultLookup().findOrThrow("open"),
			FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
			Linker.Option.critical(false));

	private static final byte UINPUT_IOCTL_BASE = 'U';

	private static final MethodHandle WRITE_METHOD_HANDLE = LINKER
			.downcallHandle(
					LINKER.defaultLookup().findOrThrow("write"), FunctionDescriptor.of(ValueLayout.JAVA_LONG,
							ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
					Linker.Option.critical(false));

	private static final int _IOC_NONE = 0;

	private static final int _IOC_NRBITS = 8;

	private static final int _IOC_READ = 2;

	private static final int _IOC_SIZEBITS = 14;

	private static final int _IOC_TYPEBITS = 8;

	public static final int UI_DEV_DESTROY = _IO(UINPUT_IOCTL_BASE, 2);

	public static final int UI_DEV_CREATE = _IO(UINPUT_IOCTL_BASE, 1);

	public static final int UI_GET_VERSION = _IOR(UINPUT_IOCTL_BASE, 45, Integer.BYTES);

	private static final int _IOC_WRITE = 1;

	public static final int UI_SET_EVBIT = _IOW(UINPUT_IOCTL_BASE, 100, Integer.BYTES);

	public static final int UI_SET_KEYBIT = _IOW(UINPUT_IOCTL_BASE, 101, Integer.BYTES);

	public static final int UI_SET_RELBIT = _IOW(UINPUT_IOCTL_BASE, 102, Integer.BYTES);

	public static final int UI_SET_ABSBIT = _IOW(UINPUT_IOCTL_BASE, 103, Integer.BYTES);

	public static final int UI_DEV_SETUP = _IOW(UINPUT_IOCTL_BASE, 3, (int) uinput_setup.LAYOUT.byteSize());

	private Linux() {
	}

	private static int _IO(final int type, final int nr) {
		return _IOC(_IOC_NONE, type, nr, 0);
	}

	private static int _IOC(final int dir, final int type, final int nr, final int size) {
		final int _IOC_NRSHIFT = 0;
		final int _IOC_TYPESHIFT = _IOC_NRSHIFT + _IOC_NRBITS;
		final int _IOC_SIZESHIFT = _IOC_TYPESHIFT + _IOC_TYPEBITS;
		final int _IOC_DIRSHIFT = _IOC_SIZESHIFT + _IOC_SIZEBITS;

		return dir << _IOC_DIRSHIFT | type << _IOC_TYPESHIFT | nr << _IOC_NRSHIFT | size << _IOC_SIZESHIFT;
	}

	private static int _IOR(final int type, final int nr, final int size) {
		return _IOC(_IOC_READ, type, nr, size);
	}

	private static int _IOW(final int type, final int nr, final int size) {
		return _IOC(_IOC_WRITE, type, nr, size);
	}

	public static int close(final int fd) {
		try {
			return (int) CLOSE_METHOD_HANDLE.invokeExact(fd);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static int getErrno(final MemorySegment errorState) {
		return (int) ERRNO_VAR_HANDLE.get(errorState, 0);
	}

	public static int ioctl(final int fd, final long request, final MemorySegment argp, final MemorySegment errno) {
		try {
			return (int) IOCTL_METHOD_HANDLE.invoke(errno, fd, request, argp);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static int open(final MemorySegment pathname, final int flags) {
		try {
			return (int) OPEN_METHOD_HANDLE.invokeExact(pathname, flags);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static long write(final int fd, final MemorySegment buf, final long count) {
		try {
			return (long) WRITE_METHOD_HANDLE.invokeExact(fd, buf, count);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static final class input_event {

		private static final String CODE_NAME = "code";

		private static final String TIME_NAME = "time";

		private static final String TYPE_NAME = "type";

		private static final String VALUE_NAME = "value";

		public static final GroupLayout LAYOUT = MemoryLayout.structLayout(timeval.LAYOUT.withName(TIME_NAME),
				ValueLayout.JAVA_SHORT.withName(TYPE_NAME), ValueLayout.JAVA_SHORT.withName(CODE_NAME),
				ValueLayout.JAVA_INT.withName(VALUE_NAME));

		private static final VarHandle CODE_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(CODE_NAME));

		private static final VarHandle TYPE_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(TYPE_NAME));

		private static final VarHandle VALUE_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(VALUE_NAME));

		public static void setCode(final MemorySegment seg, final short code) {
			CODE_VAR_HANDLE.set(seg, 0L, code);
		}

		public static void setType(final MemorySegment seg, final short type) {
			TYPE_VAR_HANDLE.set(seg, 0L, type);
		}

		public static void setValue(final MemorySegment seg, final int value) {
			VALUE_VAR_HANDLE.set(seg, 0L, value);
		}
	}

	public static final class input_id {

		private static final String BUSTYPE_NAME = "bustype";

		private static final String PRODUCT_NAME = "product";

		private static final String VENDOR_NAME = "vendor";

		private static final String VERSION_NAME = "version";

		public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
				ValueLayout.JAVA_SHORT.withName(BUSTYPE_NAME), ValueLayout.JAVA_SHORT.withName(VENDOR_NAME),
				ValueLayout.JAVA_SHORT.withName(PRODUCT_NAME), ValueLayout.JAVA_SHORT.withName(VERSION_NAME));

		private static final VarHandle BUSTYPE_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(BUSTYPE_NAME));

		private static final VarHandle PRODUCT_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(PRODUCT_NAME));

		private static final VarHandle VENDOR_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(VENDOR_NAME));

		public static void setBustype(final MemorySegment seg, final short bustype) {
			BUSTYPE_VAR_HANDLE.set(seg, 0L, bustype);
		}

		public static void setProduct(final MemorySegment seg, final short product) {
			PRODUCT_VAR_HANDLE.set(seg, 0L, product);
		}

		public static void setVendor(final MemorySegment seg, final short vendor) {
			VENDOR_VAR_HANDLE.set(seg, 0L, vendor);
		}
	}

	private static final class timeval {

		private static final String TV_SEC_NAME = "tv_sec";

		private static final String TV_USEC_NAME = "tv_usec";

		private static final GroupLayout LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_LONG.withName(TV_SEC_NAME),
				ValueLayout.JAVA_LONG.withName(TV_USEC_NAME));
	}

	public static final class uinput_setup {

		private static final String FF_EFFECTS_MAX_NAME = "ff_effects_max";

		private static final String INPUT_ID_NAME = " input_id";

		private static final String NAME_NAME = "name";

		public static final GroupLayout LAYOUT = MemoryLayout.structLayout(input_id.LAYOUT.withName(INPUT_ID_NAME),
				MemoryLayout.sequenceLayout(UINPUT_MAX_NAME_SIZE, ValueLayout.JAVA_BYTE).withName(NAME_NAME)
						.withName(NAME_NAME),
				ValueLayout.JAVA_INT.withName(FF_EFFECTS_MAX_NAME));

		public static MemorySegment getInput_id(final MemorySegment seg) {
			return seg.asSlice(0L, input_id.LAYOUT);
		}

		public static MemorySegment getName(final MemorySegment seg) {
			return seg.asSlice(input_id.LAYOUT.byteSize(), UINPUT_MAX_NAME_SIZE);
		}
	}
}

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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/// Provides Java bindings to Linux system calls and uinput ioctl constants via
/// the Foreign Function & Memory API.
///
/// This class exposes native `open`, `close`, `write`, and `ioctl` operations,
/// as well as memory layout definitions for uinput data structures used to
/// create and manage virtual input devices.
@SuppressWarnings({ "exports", "restricted", "SameParameterValue" })
public final class Linux {

	/// Linux `O_NONBLOCK` flag for non-blocking I/O.
	public static final int O_NONBLOCK = 4000;

	/// Linux `O_WRONLY` flag for write-only access.
	public static final int O_WRONLY = 1;

	/// Maximum length of a uinput device name.
	public static final int UINPUT_MAX_NAME_SIZE = 80;

	/// Name of the `errno` field in the call state capture layout.
	private static final String ERRNO_NAME = "errno";

	/// VarHandle for reading the captured `errno` value from a call state segment.
	public static final VarHandle ERRNO_VAR_HANDLE = Linker.Option.captureStateLayout()
			.varHandle(PathElement.groupElement(ERRNO_NAME));

	/// Linker option that captures the `errno` value after each native call.
	private static final Linker.Option ERRNO_CAPTURE_CALL_STATE = Linker.Option.captureCallState(ERRNO_NAME);

	/// The native linker used to create method handles for native calls.
	private static final Linker LINKER = Linker.nativeLinker();

	/// Method handle for the native `ioctl` function with errno capture.
	private static final MethodHandle IOCTL_METHOD_HANDLE = LINKER.downcallHandle(
			LINKER.defaultLookup().findOrThrow("ioctl"), FunctionDescriptor.of(ValueLayout.JAVA_INT,
					ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
			ERRNO_CAPTURE_CALL_STATE, Linker.Option.firstVariadicArg(2));

	/// Method handle for the native `close` function.
	private static final MethodHandle CLOSE_METHOD_HANDLE = LINKER.downcallHandle(
			LINKER.defaultLookup().findOrThrow("close"),
			FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT), Linker.Option.critical(false));

	/// Method handle for the native `open` function.
	private static final MethodHandle OPEN_METHOD_HANDLE = LINKER.downcallHandle(
			LINKER.defaultLookup().findOrThrow("open"),
			FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
			Linker.Option.critical(false));

	/// The ioctl base character for uinput ioctl commands.
	private static final byte UINPUT_IOCTL_BASE = 'U';

	/// Method handle for the native `write` function.
	private static final MethodHandle WRITE_METHOD_HANDLE = LINKER
			.downcallHandle(
					LINKER.defaultLookup().findOrThrow("write"), FunctionDescriptor.of(ValueLayout.JAVA_LONG,
							ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
					Linker.Option.critical(false));

	/// Linux `_IOC_NONE` direction value (no data transfer).
	private static final int _IOC_NONE = 0;

	/// Number of bits in the ioctl number field.
	private static final int _IOC_NRBITS = 8;

	/// Linux `_IOC_READ` direction value (read from device).
	private static final int _IOC_READ = 2;

	/// Number of bits in the ioctl size field.
	private static final int _IOC_SIZEBITS = 14;

	/// Number of bits in the ioctl type field.
	private static final int _IOC_TYPEBITS = 8;

	/// Linux `UI_DEV_DESTROY` ioctl command to destroy a uinput device.
	public static final int UI_DEV_DESTROY = _IO(UINPUT_IOCTL_BASE, 2);

	/// Linux `UI_DEV_CREATE` ioctl command to create a uinput device.
	public static final int UI_DEV_CREATE = _IO(UINPUT_IOCTL_BASE, 1);

	/// Linux `UI_GET_VERSION` ioctl command to retrieve the uinput protocol
	/// version.
	public static final int UI_GET_VERSION = _IOR(UINPUT_IOCTL_BASE, 45, Integer.BYTES);

	/// Linux `_IOC_WRITE` direction value (write to device).
	private static final int _IOC_WRITE = 1;

	/// Linux `UI_SET_EVBIT` ioctl command to set an event type bit on the uinput
	/// device.
	public static final int UI_SET_EVBIT = _IOW(UINPUT_IOCTL_BASE, 100, Integer.BYTES);

	/// Linux `UI_SET_KEYBIT` ioctl command to set a key bit on the uinput device.
	public static final int UI_SET_KEYBIT = _IOW(UINPUT_IOCTL_BASE, 101, Integer.BYTES);

	/// Linux `UI_SET_RELBIT` ioctl command to set a relative axis bit on the uinput
	/// device.
	public static final int UI_SET_RELBIT = _IOW(UINPUT_IOCTL_BASE, 102, Integer.BYTES);

	/// Linux `UI_SET_ABSBIT` ioctl command to set an absolute axis bit on the
	/// uinput device.
	public static final int UI_SET_ABSBIT = _IOW(UINPUT_IOCTL_BASE, 103, Integer.BYTES);

	/// Linux `UI_DEV_SETUP` ioctl command to configure a uinput device before
	/// creation.
	public static final int UI_DEV_SETUP = _IOW(UINPUT_IOCTL_BASE, 3, (int) uinput_setup.LAYOUT.byteSize());

	/// Prevents instantiation.
	private Linux() {
	}

	/// Computes a no-data ioctl request code with no transfer direction.
	///
	/// @param type the ioctl type byte (device magic number)
	/// @param nr the ioctl sequence number
	/// @return the encoded ioctl request code
	private static int _IO(final int type, final int nr) {
		return _IOC(_IOC_NONE, type, nr, 0);
	}

	/// Computes an ioctl request code by encoding the direction, type, number, and
	/// size into a single integer.
	///
	/// @param dir the transfer direction bits
	/// @param type the ioctl type byte (device magic number)
	/// @param nr the ioctl sequence number
	/// @param size the size of the data argument in bytes
	/// @return the encoded ioctl request code
	private static int _IOC(final int dir, final int type, final int nr, final int size) {
		final var _IOC_NRSHIFT = 0;
		final var _IOC_TYPESHIFT = _IOC_NRSHIFT + _IOC_NRBITS;
		final var _IOC_SIZESHIFT = _IOC_TYPESHIFT + _IOC_TYPEBITS;
		final var _IOC_DIRSHIFT = _IOC_SIZESHIFT + _IOC_SIZEBITS;

		return dir << _IOC_DIRSHIFT | type << _IOC_TYPESHIFT | nr << _IOC_NRSHIFT | size << _IOC_SIZESHIFT;
	}

	/// Computes a read ioctl request code for transferring data from the device to
	/// userspace.
	///
	/// @param type the ioctl type byte (device magic number)
	/// @param nr the ioctl sequence number
	/// @param size the size of the data argument in bytes
	/// @return the encoded ioctl read request code
	private static int _IOR(final int type, final int nr, final int size) {
		return _IOC(_IOC_READ, type, nr, size);
	}

	/// Computes a write ioctl request code for transferring data from userspace to
	/// the device.
	///
	/// @param type the ioctl type byte (device magic number)
	/// @param nr the ioctl sequence number
	/// @param size the size of the data argument in bytes
	/// @return the encoded ioctl write request code
	private static int _IOW(final int type, final int nr, final int size) {
		return _IOC(_IOC_WRITE, type, nr, size);
	}

	/// Closes the file descriptor.
	///
	/// @param fd the file descriptor to close
	/// @return zero on success, or `-1` on error
	public static int close(final int fd) {
		try {
			return (int) CLOSE_METHOD_HANDLE.invokeExact(fd);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/// Extracts the `errno` value from a captured call state segment.
	///
	/// @param errorState the memory segment containing the captured errno state
	/// @return the errno value
	public static int getErrno(final MemorySegment errorState) {
		return (int) ERRNO_VAR_HANDLE.get(errorState, 0);
	}

	/// Invokes the `ioctl` system call on a file descriptor.
	///
	/// @param fd the file descriptor
	/// @param request the ioctl request code
	/// @param argp the argument pointer passed to the ioctl call
	/// @param errno the memory segment to capture the errno state
	/// @return the ioctl return value, typically zero on success or `-1` on error
	public static int ioctl(final int fd, final long request, final MemorySegment argp, final MemorySegment errno) {
		try {
			return (int) IOCTL_METHOD_HANDLE.invoke(errno, fd, request, argp);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/// Opens a file path with the specified flags.
	///
	/// @param pathname the null-terminated file path as a memory segment
	/// @param flags the open flags (e.g. [#O_WRONLY], [#O_NONBLOCK])
	/// @return a non-negative file descriptor on success, or `-1` on error
	public static int open(final MemorySegment pathname, final int flags) {
		try {
			return (int) OPEN_METHOD_HANDLE.invokeExact(pathname, flags);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/// Writes bytes to a file descriptor.
	///
	/// @param fd the file descriptor
	/// @param buf the memory segment containing the data to write
	/// @param count the number of bytes to write
	/// @return the number of bytes written, or `-1` on error
	public static long write(final int fd, final MemorySegment buf, final long count) {
		try {
			return (long) WRITE_METHOD_HANDLE.invokeExact(fd, buf, count);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/// Represents the Linux `input_event` structure used for writing input events
	/// to uinput devices.
	///
	/// Provides a [GroupLayout] constant and static setter methods for
	/// populating the type, code, and value fields of an `input_event`
	/// memory segment.
	public static final class input_event {

		/// Field name for the event code.
		private static final String CODE_NAME = "code";

		/// Field name for the timestamp.
		private static final String TIME_NAME = "time";

		/// Field name for the event type.
		private static final String TYPE_NAME = "type";

		/// Field name for the event value.
		private static final String VALUE_NAME = "value";

		/// Memory layout of the Linux `input_event` struct.
		public static final GroupLayout LAYOUT = MemoryLayout.structLayout(timeval.LAYOUT.withName(TIME_NAME),
				ValueLayout.JAVA_SHORT.withName(TYPE_NAME), ValueLayout.JAVA_SHORT.withName(CODE_NAME),
				ValueLayout.JAVA_INT.withName(VALUE_NAME));

		/// VarHandle for accessing the `code` field.
		private static final VarHandle CODE_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(CODE_NAME));

		/// VarHandle for accessing the `type` field.
		private static final VarHandle TYPE_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(TYPE_NAME));

		/// VarHandle for accessing the `value` field.
		private static final VarHandle VALUE_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(VALUE_NAME));

		/// Prevents instantiation.
		private input_event() {
		}

		/// Sets the event code field in the given `input_event` memory segment.
		///
		/// @param seg the memory segment representing an `input_event`
		/// @param code the event code value
		public static void setCode(final MemorySegment seg, final short code) {
			CODE_VAR_HANDLE.set(seg, 0L, code);
		}

		/// Sets the event type field in the given `input_event` memory segment.
		///
		/// @param seg the memory segment representing an `input_event`
		/// @param type the event type value
		public static void setType(final MemorySegment seg, final short type) {
			TYPE_VAR_HANDLE.set(seg, 0L, type);
		}

		/// Sets the event value field in the given `input_event` memory segment.
		///
		/// @param seg the memory segment representing an `input_event`
		/// @param value the event value
		public static void setValue(final MemorySegment seg, final int value) {
			VALUE_VAR_HANDLE.set(seg, 0L, value);
		}
	}

	/// Represents the Linux `input_id` structure describing the identity of an
	/// input device.
	///
	/// Provides a [GroupLayout] constant and static setter methods for
	/// populating the bustype, vendor, and product fields of an
	/// `input_id` memory segment.
	public static final class input_id {

		/// Field name for the bus type.
		private static final String BUSTYPE_NAME = "bustype";

		/// Field name for the product ID.
		private static final String PRODUCT_NAME = "product";

		/// Field name for the vendor ID.
		private static final String VENDOR_NAME = "vendor";

		/// Field name for the version.
		private static final String VERSION_NAME = "version";

		/// Memory layout of the Linux `input_id` struct.
		public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
				ValueLayout.JAVA_SHORT.withName(BUSTYPE_NAME), ValueLayout.JAVA_SHORT.withName(VENDOR_NAME),
				ValueLayout.JAVA_SHORT.withName(PRODUCT_NAME), ValueLayout.JAVA_SHORT.withName(VERSION_NAME));

		/// VarHandle for accessing the `bustype` field.
		private static final VarHandle BUSTYPE_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(BUSTYPE_NAME));

		/// VarHandle for accessing the `product` field.
		private static final VarHandle PRODUCT_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(PRODUCT_NAME));

		/// VarHandle for accessing the `vendor` field.
		private static final VarHandle VENDOR_VAR_HANDLE = LAYOUT
				.varHandle(MemoryLayout.PathElement.groupElement(VENDOR_NAME));

		/// Prevents instantiation.
		private input_id() {
		}

		/// Sets the bustype field in the given `input_id` memory segment.
		///
		/// @param seg the memory segment representing the `input_id` struct
		/// @param bustype the bus type value to set
		public static void setBustype(final MemorySegment seg, final short bustype) {
			BUSTYPE_VAR_HANDLE.set(seg, 0L, bustype);
		}

		/// Sets the product field in the given `input_id` memory segment.
		///
		/// @param seg the memory segment representing the `input_id` struct
		/// @param product the product ID value to set
		public static void setProduct(final MemorySegment seg, final short product) {
			PRODUCT_VAR_HANDLE.set(seg, 0L, product);
		}

		/// Sets the vendor field in the given `input_id` memory segment.
		///
		/// @param seg the memory segment representing the `input_id` struct
		/// @param vendor the vendor ID value to set
		public static void setVendor(final MemorySegment seg, final short vendor) {
			VENDOR_VAR_HANDLE.set(seg, 0L, vendor);
		}
	}

	/// Represents the Linux `timeval` structure used to express a time value as
	/// seconds and microseconds.
	///
	/// Provides a [GroupLayout] constant describing the memory layout of the
	/// `timeval` struct, with fields `tv_sec` (seconds) and `tv_usec`
	/// (microseconds).
	private static final class timeval {

		/// Field name for the seconds component.
		private static final String TV_SEC_NAME = "tv_sec";

		/// Field name for the microseconds component.
		private static final String TV_USEC_NAME = "tv_usec";

		/// Memory layout of the Linux `timeval` struct.
		private static final GroupLayout LAYOUT = MemoryLayout.structLayout(ValueLayout.JAVA_LONG.withName(TV_SEC_NAME),
				ValueLayout.JAVA_LONG.withName(TV_USEC_NAME));

		/// Prevents instantiation.
		private timeval() {
		}
	}

	/// Represents the Linux `uinput_setup` structure used to configure a uinput
	/// device.
	///
	/// Provides a [GroupLayout] constant and static accessor methods for
	/// retrieving the `input_id` and device name member slices from a
	/// `uinput_setup` memory segment.
	public static final class uinput_setup {

		/// Field name for the maximum number of force-feedback effects.
		private static final String FF_EFFECTS_MAX_NAME = "ff_effects_max";

		/// Field name for the embedded `input_id` struct.
		private static final String INPUT_ID_NAME = " input_id";

		/// Field name for the device name array.
		private static final String NAME_NAME = "name";

		/// Prevents instantiation.
		private uinput_setup() {
		}

		/// Memory layout of the Linux `uinput_setup` struct.
		public static final GroupLayout LAYOUT = MemoryLayout.structLayout(input_id.LAYOUT.withName(INPUT_ID_NAME),
				MemoryLayout.sequenceLayout(UINPUT_MAX_NAME_SIZE, ValueLayout.JAVA_BYTE).withName(NAME_NAME)
						.withName(NAME_NAME),
				ValueLayout.JAVA_INT.withName(FF_EFFECTS_MAX_NAME));

		/// Returns the `input_id` member slice from the given `uinput_setup` memory
		/// segment.
		///
		/// @param seg the memory segment representing the `uinput_setup` struct
		/// @return the `input_id` member slice
		public static MemorySegment getInput_id(final MemorySegment seg) {
			return seg.asSlice(0L, input_id.LAYOUT);
		}

		/// Returns the device name member slice from the given `uinput_setup` memory
		/// segment.
		///
		/// @param seg the memory segment representing the `uinput_setup` struct
		/// @return the device name member slice
		public static MemorySegment getName(final MemorySegment seg) {
			return seg.asSlice(input_id.LAYOUT.byteSize(), UINPUT_MAX_NAME_SIZE);
		}
	}
}

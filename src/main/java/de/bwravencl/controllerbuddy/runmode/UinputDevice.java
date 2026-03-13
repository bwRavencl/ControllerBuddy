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

package de.bwravencl.controllerbuddy.runmode;

import de.bwravencl.controllerbuddy.constants.Constants;
import de.bwravencl.controllerbuddy.ffi.Linux;
import de.bwravencl.controllerbuddy.ffi.Linux.input_event;
import de.bwravencl.controllerbuddy.ffi.Linux.input_id;
import de.bwravencl.controllerbuddy.ffi.Linux.uinput_setup;
import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/// Manages a Linux uinput virtual input device.
///
/// Supports creating joystick, keyboard, and mouse devices via the
/// uinput kernel interface, emitting input events, and tearing down
/// the device on close. Device instances are cached per [DeviceType]
/// and shared across callers.
public final class UinputDevice implements Closeable {

	/// USB bus type identifier used when registering the virtual device.
	private static final short BUS_USB = 0x3;

	/// Cache of open uinput devices keyed by device type.
	private static final Map<DeviceType, UinputDevice> DEVICE_MAP = new EnumMap<>(DeviceType.class);

	/// Path to the uinput kernel interface device node.
	private static final Path DEVICE_PATH = Paths.get("/dev", "uinput");

	/// Linux event type constant for absolute axis events.
	private static final short EV_ABS = 3;

	/// Linux event type constant for key/button events.
	private static final short EV_KEY = 1;

	/// Linux event type constant for relative axis events.
	private static final short EV_REL = 2;

	/// Linux event type constant for synchronization events.
	private static final short EV_SYN = 0;

	private static final Logger LOGGER = Logger.getLogger(UinputDevice.class.getName());

	/// USB product code reported by the virtual device.
	private static final short PRODUCT_CODE = 0x5678;

	/// USB vendor code reported by the virtual device.
	private static final short VENDOR_CODE = 0x1234;

	/// File descriptor for the open uinput device node.
	private final int fd;

	/// Human-readable name of this virtual device.
	private final String name;

	/// Opens the `/dev/uinput` device, configures the supported event codes, and
	/// creates the virtual input device for the given device type.
	///
	/// @param deviceType the type of virtual device to create
	/// @throws IOException if the uinput device cannot be opened or created
	private UinputDevice(final DeviceType deviceType) throws IOException {
		if (deviceType.name.getBytes(StandardCharsets.UTF_8).length > Linux.UINPUT_MAX_NAME_SIZE) {
			throw new IllegalArgumentException("Parameter name has more than " + Linux.UINPUT_MAX_NAME_SIZE + " bytes");
		}

		name = deviceType.name;

		try (final var arena = Arena.ofConfined()) {
			fd = Linux.open(arena.allocateFrom(DEVICE_PATH.toString()), Linux.O_WRONLY | Linux.O_NONBLOCK);
			if (fd == -1) {
				throw new IOException("Could not open: " + DEVICE_PATH);
			}

			final var driverVersion = arena.allocate(ValueLayout.JAVA_INT);
			ioctlChecked(fd, Linux.UI_GET_VERSION, driverVersion);
			final var driverVersionValue = driverVersion.get(AddressLayout.JAVA_INT, 0L);
			if (driverVersionValue < 5) {
				throw new UnsupportedOperationException("Unsupported uinput version: " + driverVersionValue);
			}

			for (final var eventCode : deviceType.supportedEvents) {
				final var type = eventCode.type;

				ioctlChecked(fd, Linux.UI_SET_EVBIT, MemorySegment.ofAddress(type));

				final var code = MemorySegment.ofAddress(eventCode.code);
				switch (type) {
				case EV_KEY -> ioctlChecked(fd, Linux.UI_SET_KEYBIT, code);
				case EV_REL -> ioctlChecked(fd, Linux.UI_SET_RELBIT, code);
				case EV_ABS -> ioctlChecked(fd, Linux.UI_SET_ABSBIT, code);
				default -> throw new UnsupportedOperationException("Unsupported event type: " + type);
				}
			}

			final var uinputSetup = arena.allocate(uinput_setup.LAYOUT);
			final var uinputSetupName = uinput_setup.getName(uinputSetup);
			uinputSetupName.setString(0L, name);

			final var inputId = uinput_setup.getInput_id(uinputSetup);
			input_id.setBustype(inputId, BUS_USB);
			input_id.setProduct(inputId, PRODUCT_CODE);
			input_id.setVendor(inputId, VENDOR_CODE);

			ioctlChecked(fd, Linux.UI_DEV_SETUP, uinputSetup);
		}

		ioctlChecked(fd, Linux.UI_DEV_CREATE, MemorySegment.NULL);
	}

	/// Returns an open [UinputDevice] for the given device type, creating it if
	/// it does not already exist in the device cache.
	///
	/// @param deviceType the type of virtual device to open or retrieve
	/// @return the open [UinputDevice] instance
	/// @throws IOException if the device needs to be created but creation fails
	static UinputDevice openUinputDevice(final DeviceType deviceType) throws IOException {
		var uinputDevice = DEVICE_MAP.get(deviceType);
		if (uinputDevice == null) {
			uinputDevice = new UinputDevice(deviceType);
			DEVICE_MAP.put(deviceType, uinputDevice);

			LOGGER.info("Opened uinput device: " + uinputDevice);
		}

		return uinputDevice;
	}

	/// Closes and removes all currently open uinput devices.
	public static void shutdown() {
		DEVICE_MAP.values().removeIf(uinputDevice -> {
			try {
				uinputDevice.close();
			} catch (final IOException e) {
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}

			return true;
		});
	}

	/// Destroys the uinput device and closes the underlying file descriptor.
	///
	/// @throws IOException if the device cannot be destroyed or closed
	@Override
	public void close() throws IOException {
		ioctlChecked(fd, Linux.UI_DEV_DESTROY, MemorySegment.NULL);

		if (Linux.close(fd) == -1) {
			throw new IOException("Could not close: " + DEVICE_PATH);
		}

		LOGGER.info("Closed uinput device: " + this);
	}

	/// Writes a single input event to the uinput device, optionally followed by a
	/// synchronization report.
	///
	/// @param event the event type and code to emit
	/// @param value the event value (e.g. 1 for key-down, 0 for key-up)
	/// @param syn `true` to follow the event with a `SYN_REPORT`
	/// @throws IOException if the write fails
	void emit(final Event event, final int value, final boolean syn) throws IOException {
		try (final var arena = Arena.ofConfined()) {
			final var inputEvent = arena.allocate(input_event.LAYOUT);
			input_event.setCode(inputEvent, event.code);
			input_event.setType(inputEvent, event.type);
			input_event.setValue(inputEvent, value);

			if (Linux.write(fd, inputEvent, inputEvent.byteSize()) == -1) {
				throw new IOException("write() failed");
			}
		}

		if (syn) {
			syn();
		}
	}

	/// Invokes the given `ioctl` request and throws an [IOException] if it fails.
	///
	/// @param fd the file descriptor to operate on
	/// @param request the `ioctl` request code
	/// @param argp the argument memory segment passed to the call
	/// @throws IOException if the `ioctl` call returns -1
	private void ioctlChecked(final int fd, final long request, final MemorySegment argp) throws IOException {
		try (final var arena = Arena.ofConfined()) {
			final var errno = arena.allocate(Linker.Option.captureStateLayout());

			if (Linux.ioctl(fd, request, argp, errno) == -1) {
				final var errnoValue = Linux.getErrno(errno);
				throw new IOException("ioctl() failed: " + errnoValue);
			}
		}
	}

	/// Emits a `SYN_REPORT` event to signal the end of an event batch to the
	/// kernel.
	///
	/// @throws IOException if the event cannot be written
	void syn() throws IOException {
		emit(Event.SYN_REPORT, 0, false);
	}

	/// Returns the human-readable name of this uinput device.
	///
	/// @return the device name
	@Override
	public String toString() {
		return name;
	}

	/// Classifies a uinput virtual device by its function.
	///
	/// Determines which set of [Event] values the device registers with the
	/// kernel when it is created, and how the application-level name is derived.
	@SuppressWarnings("ImmutableEnumChecker")
	enum DeviceType {

		/// A virtual joystick/gamepad device that supports absolute axes and joystick
		/// buttons
		JOYSTICK(Event.JOYSTICK_EVENTS),

		/// A virtual keyboard device that supports key events
		KEYBOARD(Event.KEYBOARD_EVENTS),

		/// A virtual mouse device that supports relative axes and mouse buttons
		MOUSE(Event.MOUSE_EVENTS);

		/// Human-readable name of the virtual device derived from the enum constant.
		private final String name;

		/// Set of events this device type registers with the uinput kernel interface.
		private final Set<Event> supportedEvents;

		/// Constructs a device type, deriving the human-readable device name from the
		/// application name and the enum constant name.
		///
		/// @param supportedEvents the events that the device registers with the kernel
		DeviceType(final Event[] supportedEvents) {
			final var enumName = name();
			name = Constants.APPLICATION_NAME + " " + enumName.charAt(0)
					+ enumName.substring(1).toLowerCase(Locale.ROOT);
			this.supportedEvents = Set.of(supportedEvents);
		}
	}

	/// Linux input event codes used with uinput.
	///
	/// Covers keyboard keys, mouse buttons, joystick buttons,
	/// absolute axes, relative axes, and the synchronization report.
	public enum Event {

		/// Escape key
		KEY_ESC((short) 1, EV_KEY),

		/// '1' key
		KEY_1((short) 2, EV_KEY),

		/// '2' key
		KEY_2((short) 3, EV_KEY),

		/// '3' key
		KEY_3((short) 4, EV_KEY),

		/// '4' key
		KEY_4((short) 5, EV_KEY),

		/// '5' key
		KEY_5((short) 6, EV_KEY),

		/// '6' key
		KEY_6((short) 7, EV_KEY),

		/// '7' key
		KEY_7((short) 8, EV_KEY),

		/// '8' key
		KEY_8((short) 9, EV_KEY),

		/// '9' key
		KEY_9((short) 10, EV_KEY),

		/// '0' key
		KEY_0((short) 11, EV_KEY),

		/// Minus ('-') key
		KEY_MINUS((short) 12, EV_KEY),

		/// Equal ('=') key
		KEY_EQUAL((short) 13, EV_KEY),

		/// Backspace key
		KEY_BACKSPACE((short) 14, EV_KEY),

		/// Tab key
		KEY_TAB((short) 15, EV_KEY),

		/// 'Q' key
		KEY_Q((short) 16, EV_KEY),

		/// 'W' key
		KEY_W((short) 17, EV_KEY),

		/// 'E' key
		KEY_E((short) 18, EV_KEY),

		/// 'R' key
		KEY_R((short) 19, EV_KEY),

		/// 'T' key
		KEY_T((short) 20, EV_KEY),

		/// 'Y' key
		KEY_Y((short) 21, EV_KEY),

		/// 'U' key
		KEY_U((short) 22, EV_KEY),

		/// 'I' key
		KEY_I((short) 23, EV_KEY),

		/// 'O' key
		KEY_O((short) 24, EV_KEY),

		/// 'P' key
		KEY_P((short) 25, EV_KEY),

		/// Left brace ('[') key
		KEY_LEFTBRACE((short) 26, EV_KEY),

		/// Right brace (']') key
		KEY_RIGHTBRACE((short) 27, EV_KEY),

		/// Enter key
		KEY_ENTER((short) 28, EV_KEY),

		/// Left control key
		KEY_LEFTCTRL((short) 29, EV_KEY),

		/// 'A' key
		KEY_A((short) 30, EV_KEY),

		/// 'S' key
		KEY_S((short) 31, EV_KEY),

		/// 'D' key
		KEY_D((short) 32, EV_KEY),

		/// 'F' key
		KEY_F((short) 33, EV_KEY),

		/// 'G' key
		KEY_G((short) 34, EV_KEY),

		/// 'H' key
		KEY_H((short) 35, EV_KEY),

		/// 'J' key
		KEY_J((short) 36, EV_KEY),

		/// 'K' key
		KEY_K((short) 37, EV_KEY),

		/// 'L' key
		KEY_L((short) 38, EV_KEY),

		/// Semicolon (';') key
		KEY_SEMICOLON((short) 39, EV_KEY),

		/// Apostrophe ('\'') key
		KEY_APOSTROPHE((short) 40, EV_KEY),

		/// Grave ('`') key
		KEY_GRAVE((short) 41, EV_KEY),

		/// Left shift key
		KEY_LEFTSHIFT((short) 42, EV_KEY),

		/// Backslash ('\') key
		KEY_BACKSLASH((short) 43, EV_KEY),

		/// 'Z' key
		KEY_Z((short) 44, EV_KEY),

		/// 'X' key
		KEY_X((short) 45, EV_KEY),

		/// 'C' key
		KEY_C((short) 46, EV_KEY),

		/// 'V' key
		KEY_V((short) 47, EV_KEY),

		/// 'B' key
		KEY_B((short) 48, EV_KEY),

		/// 'N' key
		KEY_N((short) 49, EV_KEY),

		/// 'M' key
		KEY_M((short) 50, EV_KEY),

		/// Comma (',') key
		KEY_COMMA((short) 51, EV_KEY),

		/// Dot ('.') key
		KEY_DOT((short) 52, EV_KEY),

		/// Slash ('/') key
		KEY_SLASH((short) 53, EV_KEY),

		/// Right shift key
		KEY_RIGHTSHIFT((short) 54, EV_KEY),

		/// Keypad asterisk ('*') key
		KEY_KPASTERISK((short) 55, EV_KEY),

		/// Left alt key
		KEY_LEFTALT((short) 56, EV_KEY),

		/// Space bar
		KEY_SPACE((short) 57, EV_KEY),

		/// Caps Lock key
		KEY_CAPSLOCK((short) 58, EV_KEY),

		/// Function key F1
		KEY_F1((short) 59, EV_KEY),

		/// Function key F2
		KEY_F2((short) 60, EV_KEY),

		/// Function key F3
		KEY_F3((short) 61, EV_KEY),

		/// Function key F4
		KEY_F4((short) 62, EV_KEY),

		/// Function key F5
		KEY_F5((short) 63, EV_KEY),

		/// Function key F6
		KEY_F6((short) 64, EV_KEY),

		/// Function key F7
		KEY_F7((short) 65, EV_KEY),

		/// Function key F8
		KEY_F8((short) 66, EV_KEY),

		/// Function key F9
		KEY_F9((short) 67, EV_KEY),

		/// Function key F10
		KEY_F10((short) 68, EV_KEY),

		/// Num Lock key
		KEY_NUMLOCK((short) 69, EV_KEY),

		/// Scroll Lock key
		KEY_SCROLLLOCK((short) 70, EV_KEY),

		/// Keypad 7
		KEY_KP7((short) 71, EV_KEY),

		/// Keypad 8
		KEY_KP8((short) 72, EV_KEY),

		/// Keypad 9
		KEY_KP9((short) 73, EV_KEY),

		/// Keypad minus ('-')
		KEY_KPMINUS((short) 74, EV_KEY),

		/// Keypad 4
		KEY_KP4((short) 75, EV_KEY),

		/// Keypad 5
		KEY_KP5((short) 76, EV_KEY),

		/// Keypad 6
		KEY_KP6((short) 77, EV_KEY),

		/// Keypad plus ('+')
		KEY_KPPLUS((short) 78, EV_KEY),

		/// Keypad 1
		KEY_KP1((short) 79, EV_KEY),

		/// Keypad 2
		KEY_KP2((short) 80, EV_KEY),

		/// Keypad 3
		KEY_KP3((short) 81, EV_KEY),

		/// Keypad 0
		KEY_KP0((short) 82, EV_KEY),

		/// Keypad dot ('.')
		KEY_KPDOT((short) 83, EV_KEY),

		/// 102nd key
		KEY_102ND((short) 86, EV_KEY),

		/// Function key F11
		KEY_F11((short) 87, EV_KEY),

		/// Function key F12
		KEY_F12((short) 88, EV_KEY),

		/// Keypad Enter
		KEY_KPENTER((short) 96, EV_KEY),

		/// Right control key
		KEY_RIGHTCTRL((short) 97, EV_KEY),

		/// Keypad slash ('/')
		KEY_KPSLASH((short) 98, EV_KEY),

		/// System Request / Print Screen
		KEY_SYSRQ((short) 99, EV_KEY),

		/// Right alt key
		KEY_RIGHTALT((short) 100, EV_KEY),

		/// Home key
		KEY_HOME((short) 102, EV_KEY),

		/// Up arrow key
		KEY_UP((short) 103, EV_KEY),

		/// Page Up key
		KEY_PAGEUP((short) 104, EV_KEY),

		/// Left arrow key
		KEY_LEFT((short) 105, EV_KEY),

		/// Right arrow key
		KEY_RIGHT((short) 106, EV_KEY),

		/// End key
		KEY_END((short) 107, EV_KEY),

		/// Down arrow key
		KEY_DOWN((short) 108, EV_KEY),

		/// Page Down key
		KEY_PAGEDOWN((short) 109, EV_KEY),

		/// Insert key
		KEY_INSERT((short) 110, EV_KEY),

		/// Delete key
		KEY_DELETE((short) 111, EV_KEY),

		/// Keypad equal ('=') key
		KEY_KPEQUAL((short) 117, EV_KEY),

		/// Pause key
		KEY_PAUSE((short) 119, EV_KEY),

		/// Left Meta (Windows) key
		KEY_LEFTMETA((short) 125, EV_KEY),

		/// Right Meta (Windows) key
		KEY_RIGHTMETA((short) 126, EV_KEY),

		/// Next track key
		KEY_NEXTSONG((short) 163, EV_KEY),

		/// Left mouse button
		BTN_LEFT((short) 0x110, EV_KEY),

		/// Right mouse button
		BTN_RIGHT((short) 0x111, EV_KEY),

		/// Middle mouse button
		BTN_MIDDLE((short) 0x112, EV_KEY),

		/// Side mouse button
		BTN_SIDE((short) 0x113, EV_KEY),

		/// Extra mouse button
		BTN_EXTRA((short) 0x114, EV_KEY),

		/// Joystick trigger
		BTN_TRIGGER((short) 0x120, EV_KEY),

		/// Joystick thumb button
		BTN_THUMB((short) 0x121, EV_KEY),

		/// Joystick second thumb button
		BTN_THUMB2((short) 0x122, EV_KEY),

		/// Joystick top button
		BTN_TOP((short) 0x123, EV_KEY),

		/// Joystick second top button
		BTN_TOP2((short) 0x124, EV_KEY),

		/// Joystick pinkie button
		BTN_PINKIE((short) 0x125, EV_KEY),

		/// Joystick base button 1
		BTN_BASE((short) 0x126, EV_KEY),

		/// Joystick base button 2
		BTN_BASE2((short) 0x127, EV_KEY),

		/// Joystick base button 3
		BTN_BASE3((short) 0x128, EV_KEY),

		/// Joystick base button 4
		BTN_BASE4((short) 0x129, EV_KEY),

		/// Joystick base button 5
		BTN_BASE5((short) 0x12A, EV_KEY),

		/// Joystick base button 6
		BTN_BASE6((short) 0x12B, EV_KEY),

		/// Dead button
		BTN_DEAD((short) 0x12F, EV_KEY),

		/// Trigger Happy button 1
		BTN_TRIGGER_HAPPY1((short) 0x2C0, EV_KEY),

		/// Trigger Happy button 2
		BTN_TRIGGER_HAPPY2((short) 0x2C1, EV_KEY),

		/// Trigger Happy button 3
		BTN_TRIGGER_HAPPY3((short) 0x2C2, EV_KEY),

		/// Trigger Happy button 4
		BTN_TRIGGER_HAPPY4((short) 0x2C3, EV_KEY),

		/// Trigger Happy button 5
		BTN_TRIGGER_HAPPY5((short) 0x2C4, EV_KEY),

		/// Trigger Happy button 6
		BTN_TRIGGER_HAPPY6((short) 0x2C5, EV_KEY),

		/// Trigger Happy button 7
		BTN_TRIGGER_HAPPY7((short) 0x2C6, EV_KEY),

		/// Trigger Happy button 8
		BTN_TRIGGER_HAPPY8((short) 0x2C7, EV_KEY),

		/// Trigger Happy button 9
		BTN_TRIGGER_HAPPY9((short) 0x2C8, EV_KEY),

		/// Trigger Happy button 10
		BTN_TRIGGER_HAPPY10((short) 0x2C9, EV_KEY),

		/// Trigger Happy button 11
		BTN_TRIGGER_HAPPY11((short) 0x2CA, EV_KEY),

		/// Trigger Happy button 12
		BTN_TRIGGER_HAPPY12((short) 0x2CB, EV_KEY),

		/// Trigger Happy button 13
		BTN_TRIGGER_HAPPY13((short) 0x2CC, EV_KEY),

		/// Trigger Happy button 14
		BTN_TRIGGER_HAPPY14((short) 0x2CD, EV_KEY),

		/// Trigger Happy button 15
		BTN_TRIGGER_HAPPY15((short) 0x2CE, EV_KEY),

		/// Trigger Happy button 16
		BTN_TRIGGER_HAPPY16((short) 0x2CF, EV_KEY),

		/// Trigger Happy button 17
		BTN_TRIGGER_HAPPY17((short) 0x2D0, EV_KEY),

		/// Trigger Happy button 18
		BTN_TRIGGER_HAPPY18((short) 0x2D1, EV_KEY),

		/// Trigger Happy button 19
		BTN_TRIGGER_HAPPY19((short) 0x2D2, EV_KEY),

		/// Trigger Happy button 20
		BTN_TRIGGER_HAPPY20((short) 0x2D3, EV_KEY),

		/// Trigger Happy button 21
		BTN_TRIGGER_HAPPY21((short) 0x2D4, EV_KEY),

		/// Trigger Happy button 22
		BTN_TRIGGER_HAPPY22((short) 0x2D5, EV_KEY),

		/// Trigger Happy button 23
		BTN_TRIGGER_HAPPY23((short) 0x2D6, EV_KEY),

		/// Trigger Happy button 24
		BTN_TRIGGER_HAPPY24((short) 0x2D7, EV_KEY),

		/// Trigger Happy button 25
		BTN_TRIGGER_HAPPY25((short) 0x2D8, EV_KEY),

		/// Trigger Happy button 26
		BTN_TRIGGER_HAPPY26((short) 0x2D9, EV_KEY),

		/// Trigger Happy button 27
		BTN_TRIGGER_HAPPY27((short) 0x2DA, EV_KEY),

		/// Trigger Happy button 28
		BTN_TRIGGER_HAPPY28((short) 0x2DB, EV_KEY),

		/// Trigger Happy button 29
		BTN_TRIGGER_HAPPY29((short) 0x2DC, EV_KEY),

		/// Trigger Happy button 30
		BTN_TRIGGER_HAPPY30((short) 0x2DD, EV_KEY),

		/// Trigger Happy button 31
		BTN_TRIGGER_HAPPY31((short) 0x2DE, EV_KEY),

		/// Trigger Happy button 32
		BTN_TRIGGER_HAPPY32((short) 0x2DF, EV_KEY),

		/// Trigger Happy button 33
		BTN_TRIGGER_HAPPY33((short) 0x2E0, EV_KEY),

		/// Trigger Happy button 34
		BTN_TRIGGER_HAPPY34((short) 0x2E1, EV_KEY),

		/// Trigger Happy button 35
		BTN_TRIGGER_HAPPY35((short) 0x2E2, EV_KEY),

		/// Trigger Happy button 36
		BTN_TRIGGER_HAPPY36((short) 0x2E3, EV_KEY),

		/// Trigger Happy button 37
		BTN_TRIGGER_HAPPY37((short) 0x2E4, EV_KEY),

		/// Trigger Happy button 38
		BTN_TRIGGER_HAPPY38((short) 0x2E5, EV_KEY),

		/// Trigger Happy button 39
		BTN_TRIGGER_HAPPY39((short) 0x2E6, EV_KEY),

		/// Trigger Happy button 40
		BTN_TRIGGER_HAPPY40((short) 0x2E7, EV_KEY),

		/// Synchronization report
		SYN_REPORT((short) 0, EV_SYN),

		/// Absolute X axis
		ABS_X((short) 0x0, EV_ABS),

		/// Absolute Y axis
		ABS_Y((short) 0x1, EV_ABS),

		/// Absolute Z axis
		ABS_Z((short) 0x2, EV_ABS),

		/// Absolute RX axis
		ABS_RX((short) 0x3, EV_ABS),

		/// Absolute RY axis
		ABS_RY((short) 0x4, EV_ABS),

		/// Absolute RZ axis
		ABS_RZ((short) 0x5, EV_ABS),

		/// Absolute throttle axis
		ABS_THROTTLE((short) 0x6, EV_ABS),

		/// Absolute rudder axis
		ABS_RUDDER((short) 0x7, EV_ABS),

		/// Relative X axis
		REL_X((short) 0x0, EV_REL),

		/// Relative Y axis
		REL_Y((short) 0x1, EV_REL),

		/// Relative wheel motion
		REL_WHEEL((short) 0x8, EV_REL);

		/// All joystick button events registered for a virtual joystick device.
		static final Event[] JOYSTICK_BUTTON_EVENTS = { BTN_TRIGGER, BTN_THUMB, BTN_THUMB2, BTN_TOP, BTN_TOP2,
				BTN_PINKIE, BTN_BASE, BTN_BASE2, BTN_BASE3, BTN_BASE4, BTN_BASE5, BTN_BASE6, BTN_DEAD,
				BTN_TRIGGER_HAPPY1, BTN_TRIGGER_HAPPY2, BTN_TRIGGER_HAPPY3, BTN_TRIGGER_HAPPY4, BTN_TRIGGER_HAPPY5,
				BTN_TRIGGER_HAPPY6, BTN_TRIGGER_HAPPY7, BTN_TRIGGER_HAPPY8, BTN_TRIGGER_HAPPY9, BTN_TRIGGER_HAPPY10,
				BTN_TRIGGER_HAPPY11, BTN_TRIGGER_HAPPY12, BTN_TRIGGER_HAPPY13, BTN_TRIGGER_HAPPY14, BTN_TRIGGER_HAPPY15,
				BTN_TRIGGER_HAPPY16, BTN_TRIGGER_HAPPY17, BTN_TRIGGER_HAPPY18, BTN_TRIGGER_HAPPY19, BTN_TRIGGER_HAPPY20,
				BTN_TRIGGER_HAPPY21, BTN_TRIGGER_HAPPY22, BTN_TRIGGER_HAPPY23, BTN_TRIGGER_HAPPY24, BTN_TRIGGER_HAPPY25,
				BTN_TRIGGER_HAPPY26, BTN_TRIGGER_HAPPY27, BTN_TRIGGER_HAPPY28, BTN_TRIGGER_HAPPY29, BTN_TRIGGER_HAPPY30,
				BTN_TRIGGER_HAPPY31, BTN_TRIGGER_HAPPY32, BTN_TRIGGER_HAPPY33, BTN_TRIGGER_HAPPY34, BTN_TRIGGER_HAPPY35,
				BTN_TRIGGER_HAPPY36, BTN_TRIGGER_HAPPY37, BTN_TRIGGER_HAPPY38, BTN_TRIGGER_HAPPY39,
				BTN_TRIGGER_HAPPY40 };

		/// All events registered for a virtual joystick device, combining buttons and
		/// absolute axes.
		static final Event[] JOYSTICK_EVENTS = Stream
				.concat(Arrays.stream(JOYSTICK_BUTTON_EVENTS),
						Stream.of(ABS_X, ABS_Y, ABS_Z, ABS_RX, ABS_RY, ABS_RZ, ABS_THROTTLE, ABS_RUDDER))
				.toArray(Event[]::new);

		/// All key events registered for a virtual keyboard device.
		static final Event[] KEYBOARD_EVENTS = Arrays.stream(values()).filter(event -> event.name().startsWith("KEY_"))
				.toArray(Event[]::new);

		/// All events registered for a virtual mouse device.
		static final Event[] MOUSE_EVENTS = { BTN_LEFT, BTN_RIGHT, BTN_MIDDLE, BTN_SIDE, BTN_EXTRA, REL_X, REL_Y,
				REL_WHEEL };

		/// Linux input event code identifying this event within its type.
		private final short code;

		/// Linux input event type (e.g. `EV_KEY`, `EV_ABS`, `EV_REL`, `EV_SYN`).
		private final short type;

		/// Constructs an event constant with its Linux input subsystem code and type.
		///
		/// @param code the event code (e.g. key code or axis index)
		/// @param type the event type (e.g. `EV_KEY`, `EV_ABS`, `EV_REL`, `EV_SYN`)
		Event(final short code, final short type) {
			this.code = code;
			this.type = type;
		}
	}
}

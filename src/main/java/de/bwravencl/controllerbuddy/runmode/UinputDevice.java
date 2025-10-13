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

public final class UinputDevice implements Closeable {

	private static final short BUS_USB = 0x3;

	private static final Map<DeviceType, UinputDevice> DEVICE_MAP = new EnumMap<>(DeviceType.class);

	private static final Path DEVICE_PATH = Paths.get("/dev", "uinput");

	private static final short EV_ABS = 3;

	private static final short EV_KEY = 1;

	private static final short EV_REL = 2;

	private static final short EV_SYN = 0;

	private static final Logger LOGGER = Logger.getLogger(UinputDevice.class.getName());

	private static final short PRODUCT_CODE = 0x5678;

	private static final short VENDOR_CODE = 0x1234;

	private final int fd;

	private final String name;

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
				default -> throw new UnsupportedOperationException(String.format("Unsupported event type " + type));
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

	static UinputDevice openUinputDevice(final DeviceType deviceType) throws IOException {
		var uinputDevice = DEVICE_MAP.get(deviceType);
		if (uinputDevice == null) {
			uinputDevice = new UinputDevice(deviceType);
			DEVICE_MAP.put(deviceType, uinputDevice);

			LOGGER.log(Level.INFO, "Opened uinput device: " + uinputDevice);
		}

		return uinputDevice;
	}

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

	@Override
	public void close() throws IOException {
		ioctlChecked(fd, Linux.UI_DEV_DESTROY, MemorySegment.NULL);

		if (Linux.close(fd) == -1) {
			throw new IOException("Could not close: " + DEVICE_PATH);
		}

		LOGGER.log(Level.INFO, "Closed uinput device: " + this);
	}

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

	private void ioctlChecked(final int fd, final long request, final MemorySegment argp) throws IOException {
		try (final var arena = Arena.ofConfined()) {
			final var errno = arena.allocate(Linker.Option.captureStateLayout());

			if (Linux.ioctl(fd, request, argp, errno) == -1) {
				final var errnoValue = Linux.getErrno(errno);
				throw new IOException("ioctl() failed: " + errnoValue);
			}
		}
	}

	void syn() throws IOException {
		emit(Event.SYN_REPORT, 0, false);
	}

	@Override
	public String toString() {
		return name;
	}

	@SuppressWarnings("ImmutableEnumChecker")
	enum DeviceType {

		JOYSTICK(Event.JOYSTICK_EVENTS), KEYBOARD(Event.KEYBOARD_EVENTS), MOUSE(Event.MOUSE_EVENTS);

		private final String name;

		private final Set<Event> supportedEvents;

		DeviceType(final Event[] supportedEvents) {
			final var enumName = name();
			name = Constants.APPLICATION_NAME + " " + enumName.charAt(0)
					+ enumName.substring(1).toLowerCase(Locale.ROOT);
			this.supportedEvents = Set.of(supportedEvents);
		}
	}

	public enum Event {

		KEY_ESC((short) 1, EV_KEY), KEY_1((short) 2, EV_KEY), KEY_2((short) 3, EV_KEY), KEY_3((short) 4, EV_KEY),
		KEY_4((short) 5, EV_KEY), KEY_5((short) 6, EV_KEY), KEY_6((short) 7, EV_KEY), KEY_7((short) 8, EV_KEY),
		KEY_8((short) 9, EV_KEY), KEY_9((short) 10, EV_KEY), KEY_0((short) 11, EV_KEY), KEY_MINUS((short) 12, EV_KEY),
		KEY_EQUAL((short) 13, EV_KEY), KEY_BACKSPACE((short) 14, EV_KEY), KEY_TAB((short) 15, EV_KEY),
		KEY_Q((short) 16, EV_KEY), KEY_W((short) 17, EV_KEY), KEY_E((short) 18, EV_KEY), KEY_R((short) 19, EV_KEY),
		KEY_T((short) 20, EV_KEY), KEY_Y((short) 21, EV_KEY), KEY_U((short) 22, EV_KEY), KEY_I((short) 23, EV_KEY),
		KEY_O((short) 24, EV_KEY), KEY_P((short) 25, EV_KEY), KEY_LEFTBRACE((short) 26, EV_KEY),
		KEY_RIGHTBRACE((short) 27, EV_KEY), KEY_ENTER((short) 28, EV_KEY), KEY_LEFTCTRL((short) 29, EV_KEY),
		KEY_A((short) 30, EV_KEY), KEY_S((short) 31, EV_KEY), KEY_D((short) 32, EV_KEY), KEY_F((short) 33, EV_KEY),
		KEY_G((short) 34, EV_KEY), KEY_H((short) 35, EV_KEY), KEY_J((short) 36, EV_KEY), KEY_K((short) 37, EV_KEY),
		KEY_L((short) 38, EV_KEY), KEY_SEMICOLON((short) 39, EV_KEY), KEY_APOSTROPHE((short) 40, EV_KEY),
		KEY_GRAVE((short) 41, EV_KEY), KEY_LEFTSHIFT((short) 42, EV_KEY), KEY_BACKSLASH((short) 43, EV_KEY),
		KEY_Z((short) 44, EV_KEY), KEY_X((short) 45, EV_KEY), KEY_C((short) 46, EV_KEY), KEY_V((short) 47, EV_KEY),
		KEY_B((short) 48, EV_KEY), KEY_N((short) 49, EV_KEY), KEY_M((short) 50, EV_KEY), KEY_COMMA((short) 51, EV_KEY),
		KEY_DOT((short) 52, EV_KEY), KEY_SLASH((short) 53, EV_KEY), KEY_RIGHTSHIFT((short) 54, EV_KEY),
		KEY_KPASTERISK((short) 55, EV_KEY), KEY_LEFTALT((short) 56, EV_KEY), KEY_SPACE((short) 57, EV_KEY),
		KEY_CAPSLOCK((short) 58, EV_KEY), KEY_F1((short) 59, EV_KEY), KEY_F2((short) 60, EV_KEY),
		KEY_F3((short) 61, EV_KEY), KEY_F4((short) 62, EV_KEY), KEY_F5((short) 63, EV_KEY), KEY_F6((short) 64, EV_KEY),
		KEY_F7((short) 65, EV_KEY), KEY_F8((short) 66, EV_KEY), KEY_F9((short) 67, EV_KEY), KEY_F10((short) 68, EV_KEY),
		KEY_NUMLOCK((short) 69, EV_KEY), KEY_SCROLLLOCK((short) 70, EV_KEY), KEY_KP7((short) 71, EV_KEY),
		KEY_KP8((short) 72, EV_KEY), KEY_KP9((short) 73, EV_KEY), KEY_KPMINUS((short) 74, EV_KEY),
		KEY_KP4((short) 75, EV_KEY), KEY_KP5((short) 76, EV_KEY), KEY_KP6((short) 77, EV_KEY),
		KEY_KPPLUS((short) 78, EV_KEY), KEY_KP1((short) 79, EV_KEY), KEY_KP2((short) 80, EV_KEY),
		KEY_KP3((short) 81, EV_KEY), KEY_KP0((short) 82, EV_KEY), KEY_KPDOT((short) 83, EV_KEY),
		KEY_102ND((short) 86, EV_KEY), KEY_F11((short) 87, EV_KEY), KEY_F12((short) 88, EV_KEY),
		KEY_KPENTER((short) 96, EV_KEY), KEY_RIGHTCTRL((short) 97, EV_KEY), KEY_KPSLASH((short) 98, EV_KEY),
		KEY_SYSRQ((short) 99, EV_KEY), KEY_RIGHTALT((short) 100, EV_KEY), KEY_HOME((short) 102, EV_KEY),
		KEY_UP((short) 103, EV_KEY), KEY_PAGEUP((short) 104, EV_KEY), KEY_LEFT((short) 105, EV_KEY),
		KEY_RIGHT((short) 106, EV_KEY), KEY_END((short) 107, EV_KEY), KEY_DOWN((short) 108, EV_KEY),
		KEY_PAGEDOWN((short) 109, EV_KEY), KEY_INSERT((short) 110, EV_KEY), KEY_DELETE((short) 111, EV_KEY),
		KEY_KPEQUAL((short) 117, EV_KEY), KEY_PAUSE((short) 119, EV_KEY), KEY_LEFTMETA((short) 125, EV_KEY),
		KEY_RIGHTMETA((short) 126, EV_KEY), KEY_NEXTSONG((short) 163, EV_KEY), BTN_LEFT((short) 0x110, EV_KEY),
		BTN_RIGHT((short) 0x111, EV_KEY), BTN_MIDDLE((short) 0x112, EV_KEY), BTN_TRIGGER((short) 0x120, EV_KEY),
		BTN_THUMB((short) 0x121, EV_KEY), BTN_THUMB2((short) 0x122, EV_KEY), BTN_TOP((short) 0x123, EV_KEY),
		BTN_TOP2((short) 0x124, EV_KEY), BTN_PINKIE((short) 0x125, EV_KEY), BTN_BASE((short) 0x126, EV_KEY),
		BTN_BASE2((short) 0x127, EV_KEY), BTN_BASE3((short) 0x128, EV_KEY), BTN_BASE4((short) 0x129, EV_KEY),
		BTN_BASE5((short) 0x12A, EV_KEY), BTN_BASE6((short) 0x12B, EV_KEY), BTN_DEAD((short) 0x12F, EV_KEY),
		BTN_TRIGGER_HAPPY1((short) 0x2C0, EV_KEY), BTN_TRIGGER_HAPPY2((short) 0x2C1, EV_KEY),
		BTN_TRIGGER_HAPPY3((short) 0x2C2, EV_KEY), BTN_TRIGGER_HAPPY4((short) 0x2C3, EV_KEY),
		BTN_TRIGGER_HAPPY5((short) 0x2C4, EV_KEY), BTN_TRIGGER_HAPPY6((short) 0x2C5, EV_KEY),
		BTN_TRIGGER_HAPPY7((short) 0x2C6, EV_KEY), BTN_TRIGGER_HAPPY8((short) 0x2C7, EV_KEY),
		BTN_TRIGGER_HAPPY9((short) 0x2C8, EV_KEY), BTN_TRIGGER_HAPPY10((short) 0x2C9, EV_KEY),
		BTN_TRIGGER_HAPPY11((short) 0x2CA, EV_KEY), BTN_TRIGGER_HAPPY12((short) 0x2CB, EV_KEY),
		BTN_TRIGGER_HAPPY13((short) 0x2CC, EV_KEY), BTN_TRIGGER_HAPPY14((short) 0x2CD, EV_KEY),
		BTN_TRIGGER_HAPPY15((short) 0x2CE, EV_KEY), BTN_TRIGGER_HAPPY16((short) 0x2CF, EV_KEY),
		BTN_TRIGGER_HAPPY17((short) 0x2D0, EV_KEY), BTN_TRIGGER_HAPPY18((short) 0x2D1, EV_KEY),
		BTN_TRIGGER_HAPPY19((short) 0x2D2, EV_KEY), BTN_TRIGGER_HAPPY20((short) 0x2D3, EV_KEY),
		BTN_TRIGGER_HAPPY21((short) 0x2D4, EV_KEY), BTN_TRIGGER_HAPPY22((short) 0x2D5, EV_KEY),
		BTN_TRIGGER_HAPPY23((short) 0x2D6, EV_KEY), BTN_TRIGGER_HAPPY24((short) 0x2D7, EV_KEY),
		BTN_TRIGGER_HAPPY25((short) 0x2D8, EV_KEY), BTN_TRIGGER_HAPPY26((short) 0x2D9, EV_KEY),
		BTN_TRIGGER_HAPPY27((short) 0x2DA, EV_KEY), BTN_TRIGGER_HAPPY28((short) 0x2DB, EV_KEY),
		BTN_TRIGGER_HAPPY29((short) 0x2DC, EV_KEY), BTN_TRIGGER_HAPPY30((short) 0x2DD, EV_KEY),
		BTN_TRIGGER_HAPPY31((short) 0x2DE, EV_KEY), BTN_TRIGGER_HAPPY32((short) 0x2DF, EV_KEY),
		BTN_TRIGGER_HAPPY33((short) 0x2E0, EV_KEY), BTN_TRIGGER_HAPPY34((short) 0x2E1, EV_KEY),
		BTN_TRIGGER_HAPPY35((short) 0x2E2, EV_KEY), BTN_TRIGGER_HAPPY36((short) 0x2E3, EV_KEY),
		BTN_TRIGGER_HAPPY37((short) 0x2E4, EV_KEY), BTN_TRIGGER_HAPPY38((short) 0x2E5, EV_KEY),
		BTN_TRIGGER_HAPPY39((short) 0x2E6, EV_KEY), BTN_TRIGGER_HAPPY40((short) 0x2E7, EV_KEY),
		SYN_REPORT((short) 0, EV_SYN), ABS_X((short) 0x0, EV_ABS), ABS_Y((short) 0x1, EV_ABS),
		ABS_Z((short) 0x2, EV_ABS), ABS_RX((short) 0x3, EV_ABS), ABS_RY((short) 0x4, EV_ABS),
		ABS_RZ((short) 0x5, EV_ABS), ABS_THROTTLE((short) 0x6, EV_ABS), ABS_RUDDER((short) 0x7, EV_ABS),
		REL_X((short) 0x0, EV_REL), REL_Y((short) 0x1, EV_REL), REL_WHEEL((short) 0x8, EV_REL);

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

		static final Event[] JOYSTICK_EVENTS = Stream
				.concat(Arrays.stream(JOYSTICK_BUTTON_EVENTS),
						Stream.of(ABS_X, ABS_Y, ABS_Z, ABS_RX, ABS_RY, ABS_RZ, ABS_THROTTLE, ABS_RUDDER))
				.toArray(Event[]::new);

		static final Event[] KEYBOARD_EVENTS = Arrays.stream(values()).filter(event -> event.name().startsWith("KEY_"))
				.toArray(Event[]::new);

		static final Event[] MOUSE_EVENTS = { BTN_LEFT, BTN_RIGHT, BTN_MIDDLE, REL_X, REL_Y, REL_WHEEL };

		private final short code;

		private final short type;

		Event(final short code, final short type) {
			this.code = code;
			this.type = type;
		}
	}
}

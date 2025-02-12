/* Copyright (C) 2022  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.driver.sony;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.driver.Driver;
import de.bwravencl.controllerbuddy.input.driver.IDriverBuilder;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.hid4java.HidDevice;
import org.lwjgl.glfw.GLFW;

public final class DualShock4Driver extends SonyDriver {

	private static final byte BLUETOOTH_INPUT_REPORT_ID = 0x11;

	private static final Connection BluetoothConnection = new Connection(2, BLUETOOTH_INPUT_REPORT_ID);

	private static final byte USB_INPUT_REPORT_ID = 0x1;

	private static final Connection DongleConnection = new Connection(0, USB_INPUT_REPORT_ID);

	private static final Connection UsbConnection = new Connection(0, USB_INPUT_REPORT_ID);

	private static final Logger log = Logger.getLogger(DualShock4Driver.class.getName());

	private DualShock4Driver(final Input input, final ControllerInfo controller, final HidDevice hidDevice,
			final Connection connection) {
		super(input, controller, hidDevice);

		this.connection = connection;
	}

	@Override
	int getButtonsOffset() {
		return 5;
	}

	@Override
	Optional<byte[]> getDefaultHidReport() {
		if (connection == null) {
			return Optional.empty();
		}

		final byte[] defaultHidReport;
		if (connection.isBluetooth()) {
			defaultHidReport = new byte[333];

			defaultHidReport[0] = (byte) 0xC0;
			defaultHidReport[2] = (byte) 0xF7;
		} else {
			defaultHidReport = new byte[31];

			defaultHidReport[0] = (byte) 0xF;
		}

		defaultHidReport[5 + connection.offset()] = (byte) 0xC;
		defaultHidReport[6 + connection.offset()] = (byte) 0x18;
		defaultHidReport[7 + connection.offset()] = (byte) 0x1C;

		return Optional.of(defaultHidReport);
	}

	@Override
	byte getDefaultHidReportId() {
		return (byte) (connection.isBluetooth() ? 0x15 : 0x5);
	}

	@Override
	int getL2Offset() {
		return 8;
	}

	@Override
	long getLightRumbleDuration() {
		return 20L;
	}

	@Override
	byte getLightRumbleStrength() {
		return Byte.MAX_VALUE;
	}

	@Override
	int getLightbarOffset() {
		return 5;
	}

	@Override
	Logger getLogger() {
		return log;
	}

	@Override
	int getRumbleOffset() {
		return 4;
	}

	@Override
	long getStrongRumbleDuration() {
		return 80L;
	}

	@Override
	byte getStrongRumbleStrength() {
		return Byte.MAX_VALUE;
	}

	@Override
	int getTouchpadOffset() {
		return 35;
	}

	@Override
	void handleBattery(final byte[] reportData, final int offset) {
		final var cableConnected = (reportData[30 + offset] >> 4 & 0x1) != 0;
		final var batteryData = reportData[30 + offset] & 0xF;
		final int batteryCapacity;
		final boolean charging;
		if (cableConnected) {
			if (batteryData < 10) {
				batteryCapacity = batteryData * 10 + 5;
				charging = true;
			} else if (batteryData == 10) {
				batteryCapacity = 100;
				charging = true;
			} else {
				if (batteryData == 11) {
					batteryCapacity = 100;
				} else {
					batteryCapacity = 0;
				}

				charging = false;
			}
		} else {
			if (batteryData < 10) {
				batteryCapacity = batteryData * 10 + 5;
			} else {
				batteryCapacity = 100;
			}

			charging = false;
		}

		setCharging(charging);
		setBatteryCapacity(batteryCapacity);
	}

	@Override
	void handleNewConnection(final int reportLength) {
		connection = isBluetoothConnection(reportLength) ? BluetoothConnection : UsbConnection;
	}

	public static class DualShock4DriverBuilder implements IDriverBuilder {

		@Override
		public Optional<Driver> getIfAvailable(final Input input, final List<ControllerInfo> presentControllers,
				final ControllerInfo selectedController) {
			final String guid;
			if (Main.isMac) {
				guid = GLFW.glfwGetJoystickGUID(selectedController.jid());
			} else {
				guid = selectedController.guid();
			}

			if (guid == null) {
				return Optional.empty();
			}

			final short productId;
			final Connection connection;
			if (guid.matches("^0[35]0000004c050000c405.*")) {
				productId = 0x5C4;
				connection = null;
			} else if (guid.matches("^0[35]0000004c050000cc09.*")) {
				productId = 0x9CC;
				connection = null;
			} else if (guid.matches("^0[35]0000004c050000a00b.*")) {
				productId = 0xBA0;
				connection = DongleConnection;
			} else {
				return Optional.empty();
			}

			return getHidDevice(presentControllers, selectedController, productId, "DualShock 4", log)
					.map(hidDevice -> new DualShock4Driver(input, selectedController, hidDevice, connection));
		}
	}
}

/* Copyright (C) 2020  Matteo Hausner
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

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.driver.Driver;
import de.bwravencl.controllerbuddy.input.driver.IDriverBuilder;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;

public final class DualShock4Driver extends SonyDriver {

	public static class DualShock4DriverBuilder implements IDriverBuilder {

		@Override
		public Driver getIfAvailable(final Input input, final List<ControllerInfo> presentControllers,
				final ControllerInfo selectedController) {
			final String guid;
			if (Main.isMac)
				guid = GLFW.glfwGetJoystickGUID(selectedController.jid());
			else
				guid = selectedController.guid();

			if (guid == null)
				return null;

			final short productId;
			Connection connection = null;
			if (guid.matches("^0[35]0000004c050000c405.*"))
				productId = 0x5C4;
			else if (guid.matches("^0[35]0000004c050000cc09.*"))
				productId = 0x9CC;
			else if (guid.matches("^0[35]0000004c050000a00b.*")) {
				productId = 0xBA0;
				connection = DongleConnection;
			} else
				return null;

			final var hidDeviceInfo = getHidDeviceInfo(presentControllers, selectedController, productId, "DualShock 4",
					log);
			if (hidDeviceInfo != null)
				try {
					return new DualShock4Driver(input, selectedController, hidDeviceInfo, connection);
				} catch (final IOException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}

			return null;
		}
	}

	private static final Logger log = Logger.getLogger(DualShock4Driver.class.getName());

	private static final byte USB_INPUT_REPORT_ID = 0x1;
	private static final byte BLUETOOTH_INPUT_REPORT_ID = 0x11;

	private static final Connection UsbConnection = new Connection(0, USB_INPUT_REPORT_ID);
	private static final Connection DongleConnection = new Connection(0, USB_INPUT_REPORT_ID);
	private static final Connection BluetoothConnection = new Connection(2, BLUETOOTH_INPUT_REPORT_ID);

	private DualShock4Driver(final Input input, final ControllerInfo controller, final HidDeviceInfo hidDeviceInfo,
			final Connection connection) throws IOException {
		super(input, controller);

		try {
			hidDevice = PureJavaHidApi.openDevice(hidDeviceInfo);

			hidDevice.setInputReportListener(new SonyInputReportListener() {

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
							if (batteryData == 11)
								batteryCapacity = 100;
							else
								batteryCapacity = 0;

							charging = false;
						}
					} else {
						if (batteryData < 10)
							batteryCapacity = batteryData * 10 + 5;
						else
							batteryCapacity = 100;

						charging = false;
					}

					setCharging(charging);
					setBatteryCapacity(batteryCapacity);
				}

				@Override
				void handleNewConnection(final int reportLength) {
					DualShock4Driver.this.connection = connection != null ? connection
							: isBluetoothConnection(reportLength) ? BluetoothConnection : UsbConnection;
				}
			});
		} catch (final Throwable t) {
			deInit(false);
			throw t;
		}
	}

	@Override
	int getButtonsOffset() {
		return 5;
	}

	@Override
	byte[] getDefaultHidReport() {
		if (connection == null)
			return null;

		final byte[] defaultHidReport;
		if (connection.isBluetooth()) {
			defaultHidReport = new byte[334];

			defaultHidReport[0] = 0x15;
			defaultHidReport[1] = (byte) 0xC0;
			defaultHidReport[3] = (byte) 0xF7;
		} else {
			defaultHidReport = new byte[32];

			defaultHidReport[0] = (byte) 0x5;
			defaultHidReport[1] = (byte) 0xF;
		}

		defaultHidReport[6 + connection.offset()] = (byte) 0xC;
		defaultHidReport[7 + connection.offset()] = (byte) 0x18;
		defaultHidReport[8 + connection.offset()] = (byte) 0x1C;

		return defaultHidReport;
	}

	@Override
	int getL2Offset() {
		return 8;
	}

	@Override
	int getLightbarOffset() {
		return 6;
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
	Logger getLogger() {
		return log;
	}

	@Override
	int getRumbleOffset() {
		return 5;
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
}

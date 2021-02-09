/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.controllerbuddy.input.sony;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;

import de.bwravencl.controllerbuddy.input.Input;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;

final class DualShock4Extension extends SonyExtension {

	private static final byte USB_INPUT_REPORT_ID = 0x1;
	private static final byte BLUETOOTH_INPUT_REPORT_ID = 0x11;

	private static final Connection UsbConnection = new Connection(0, USB_INPUT_REPORT_ID);
	private static final Connection DongleConnection = new Connection(0, USB_INPUT_REPORT_ID);
	private static final Connection BluetoothConnection = new Connection(2, BLUETOOTH_INPUT_REPORT_ID);

	private static final Logger log = Logger.getLogger(DualShock4Extension.class.getName());

	public static DualShock4Extension getIfAvailable(final Input input, final int jid) {
		final var guid = GLFW.glfwGetJoystickGUID(jid);
		if (guid == null)
			return null;

		final short productId;
		Connection connection = null;
		if (guid.startsWith("030000004c050000c405"))
			productId = 0x5C4;
		else if (guid.startsWith("030000004c050000cc09"))
			productId = 0x9CC;
		else if (guid.startsWith("030000004c050000a00b")) {
			productId = 0xBA0;
			connection = DongleConnection;
		} else
			return null;

		final var hidDeviceInfo = getHidDeviceInfo(jid, guid, productId, "DualShock 4", log);
		if (hidDeviceInfo != null)
			try {
				return new DualShock4Extension(jid, input, hidDeviceInfo, connection);
			} catch (final IOException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}

		return null;
	}

	private DualShock4Extension(final int jid, final Input input, final HidDeviceInfo hidDeviceInfo,
			final Connection connection) throws IOException {
		super(jid, input);

		try {
			hidDevice = PureJavaHidApi.openDevice(hidDeviceInfo);

			hidDevice.setInputReportListener(new SonyInputReportListener() {

				@Override
				void handleBattery(final byte[] reportData) {
					final var cableConnected = (reportData[30 + DualShock4Extension.this.connection.offset] >> 4
							& 0x1) != 0;
					var battery = reportData[30 + DualShock4Extension.this.connection.offset] & 0xF;

					setCharging(cableConnected);

					if (!cableConnected)
						battery++;

					battery = Math.min(battery, 10);
					battery *= 10;

					setBatteryState(battery);
				}

				@Override
				void handleNewConnection(final int reportLength) {
					DualShock4Extension.this.connection = connection != null ? connection
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

		defaultHidReport[6 + connection.offset] = (byte) 0xC;
		defaultHidReport[7 + connection.offset] = (byte) 0x18;
		defaultHidReport[8 + connection.offset] = (byte) 0x1C;

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

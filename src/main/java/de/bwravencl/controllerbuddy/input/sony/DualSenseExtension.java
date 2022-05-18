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

package de.bwravencl.controllerbuddy.input.sony;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;

import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;

final class DualSenseExtension extends SonyExtension {

	private static final Logger log = Logger.getLogger(DualSenseExtension.class.getName());

	private static final byte USB_INPUT_REPORT_ID = 0x1;
	private static final byte BLUETOOTH_INPUT_REPORT_ID = 0x31;

	private static final Connection UsbConnection = new Connection(0, USB_INPUT_REPORT_ID);
	private static final Connection BluetoothConnection = new Connection(1, BLUETOOTH_INPUT_REPORT_ID);

	public static DualSenseExtension getIfAvailable(final Input input, final ControllerInfo controller) {
		final var guid = GLFW.glfwGetJoystickGUID(controller.jid());
		if (guid == null || !guid.startsWith("030000004c050000e60c"))
			return null;

		final var hidDeviceInfo = getHidDeviceInfo(controller, guid, (short) 0xCE6, "DualSense", log);
		if (hidDeviceInfo != null)
			try {
				return new DualSenseExtension(input, controller.jid(), hidDeviceInfo);
			} catch (final IOException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}

		return null;
	}

	private DualSenseExtension(final Input input, final int jid, final HidDeviceInfo hidDeviceInfo) throws IOException {
		super(input, jid);

		try {
			hidDevice = PureJavaHidApi.openDevice(hidDeviceInfo);

			hidDevice.setInputReportListener(new SonyInputReportListener() {

				@Override
				void handleBattery(final byte[] reportData) {
					final var chargingStatus = (reportData[53 + connection.offset()] & 0xF0) >> 4;
					final var batteryData = reportData[53 + connection.offset()] & 0xF;

					final int batteryCapacity;
					final var charging = switch (chargingStatus) {
					case 0x0 -> {
						batteryCapacity = batteryData == 10 ? 100 : batteryData * 10 + 5;
						yield false;
					}
					case 0x1 -> {
						batteryCapacity = batteryData == 10 ? 100 : batteryData * 10 + 5;
						yield true;
					}
					case 0x2 -> {
						batteryCapacity = 100;
						yield false;
					}
					default -> {
						batteryCapacity = 0;
						yield false;
					}
					};
					setCharging(charging);
					setBatteryCapacity(batteryCapacity);
				}

				@Override
				void handleNewConnection(final int reportLength) {
					connection = isBluetoothConnection(reportLength) ? BluetoothConnection : UsbConnection;
				}
			});
		} catch (final Throwable t) {
			deInit(false);
			throw t;
		}
	}

	@Override
	int getButtonsOffset() {
		return 8;
	}

	@Override
	byte[] getDefaultHidReport() {
		if (connection == null)
			return null;

		final byte[] defaultHidReport;
		if (connection.isBluetooth()) {
			defaultHidReport = new byte[BLUETOOTH_REPORT_LENGTH];

			defaultHidReport[0] = 0x31;
			defaultHidReport[1] = 0x2;
		} else {
			defaultHidReport = new byte[48];

			defaultHidReport[0] = 0x2;
		}

		defaultHidReport[1 + connection.offset()] = 0x3;
		defaultHidReport[2 + connection.offset()] = 0x15;

		defaultHidReport[45 + connection.offset()] = (byte) 0x0;
		defaultHidReport[46 + connection.offset()] = (byte) 0x0;
		defaultHidReport[47 + connection.offset()] = (byte) 0xFF;

		return defaultHidReport;
	}

	@Override
	int getL2Offset() {
		return 5;
	}

	@Override
	int getLightbarOffset() {
		return 45;
	}

	@Override
	long getLightRumbleDuration() {
		return 38L;
	}

	@Override
	byte getLightRumbleStrength() {
		return 25;
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
		return 50L;
	}

	@Override
	byte getStrongRumbleStrength() {
		return 64;
	}

	@Override
	int getTouchpadOffset() {
		return 33;
	}

	@Override
	void reset() {
		if (connection == null)
			return;

		if (connection.isBluetooth()) {
			final var defaultHidReport = getDefaultHidReport();
			hidReport = Arrays.copyOf(defaultHidReport, defaultHidReport.length);
			hidReport[3] = 0x8;

			sendHidReport();
		}

		super.reset();
	}
}

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

import java.awt.EventQueue;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;

public abstract class SonyExtension {

	static record Connection(int offset, byte inputReportId) {

		boolean isBluetooth() {
			return offset != 0;
		}
	}

	abstract class SonyInputReportListener implements InputReportListener {

		private static final int TOUCHPAD_MAX_DELTA = 150;
		private static final float TOUCHPAD_CURSOR_SENSITIVITY = 1.25f;
		private static final float TOUCHPAD_SCROLL_SENSITIVITY = 0.25f;

		private boolean prevTouchpadButtonDown;
		private boolean prevDown1;
		private boolean prevDown2;
		private int prevX1;
		private int prevY1;

		abstract void handleBattery(byte[] reportData);

		abstract void handleNewConnection(int reportLength);

		@Override
		public void onInputReport(final HidDevice source, final byte reportID, final byte[] reportData,
				final int reportLength) {
			if (connection == null) {
				handleNewConnection(reportLength);
				reset();
			}

			if (!isInputReportValid(reportID, reportData, reportLength))
				return;

			lx = reportData[1 + connection.offset];
			ly = reportData[2 + connection.offset];
			rx = reportData[3 + connection.offset];
			ry = reportData[4 + connection.offset];

			l2 = reportData[getL2Offset() + connection.offset];
			r2 = reportData[getL2Offset() + 1 + connection.offset];

			final var buttonsOffset = getButtonsOffset();
			triangle = (reportData[buttonsOffset + connection.offset] & 1 << 7) != 0;
			circle = (reportData[buttonsOffset + connection.offset] & 1 << 6) != 0;
			cross = (reportData[buttonsOffset + connection.offset] & 1 << 5) != 0;
			square = (reportData[buttonsOffset + connection.offset] & 1 << 4) != 0;

			final var dpadRightData = (byte) (reportData[buttonsOffset + connection.offset] & 0xF);
			dpadRight = switch (dpadRightData) {
			case 0 -> {
				dpadUp = true;
				dpadDown = false;
				dpadLeft = false;
				yield false;
			}
			case 1 -> {
				dpadUp = true;
				dpadDown = false;
				dpadLeft = false;
				yield true;
			}
			case 2 -> {
				dpadUp = false;
				dpadDown = false;
				dpadLeft = false;
				yield true;
			}
			case 3 -> {
				dpadUp = false;
				dpadDown = true;
				dpadLeft = false;
				yield true;
			}
			case 4 -> {
				dpadUp = false;
				dpadDown = true;
				dpadLeft = false;
				yield false;
			}
			case 5 -> {
				dpadUp = false;
				dpadDown = true;
				dpadLeft = true;
				yield false;
			}
			case 6 -> {
				dpadUp = false;
				dpadDown = false;
				dpadLeft = true;
				yield false;
			}
			case 7 -> {
				dpadUp = true;
				dpadDown = false;
				dpadLeft = true;
				yield false;
			}
			case 8 -> {
				dpadUp = false;
				dpadDown = false;
				dpadLeft = false;
				yield false;
			}
			default -> throw new IllegalArgumentException("Unexpected value: " + dpadRightData);
			};

			r3 = (reportData[buttonsOffset + 1 + connection.offset] & 1 << 7) != 0;
			l3 = (reportData[buttonsOffset + 1 + connection.offset] & 1 << 6) != 0;
			options = (reportData[buttonsOffset + 1 + connection.offset] & 1 << 5) != 0;
			share = (reportData[buttonsOffset + 1 + connection.offset] & 1 << 4) != 0;
			r1 = (reportData[buttonsOffset + 1 + connection.offset] & 1 << 1) != 0;
			l1 = (reportData[buttonsOffset + 1 + connection.offset] & 1 << 0) != 0;

			ps = (reportData[buttonsOffset + 2 + connection.offset] & 1 << 0) != 0;

			if (jid != input.getController().jid())
				return;

			handleBattery(reportData);

			final var main = input.getMain();
			if (!main.isLocalRunning() && !main.isServerRunning())
				return;

			final var touchpadButtonDown = (reportData[buttonsOffset + 2 + connection.offset] & 1 << 2 - 1) != 0;

			final var touchpadOffset = getTouchpadOffset();
			final var down1 = reportData[touchpadOffset + connection.offset] >> 7 == 0;
			final var down2 = reportData[touchpadOffset + 4 + connection.offset] >> 7 == 0;
			final var x1 = reportData[touchpadOffset + 1 + connection.offset]
					+ (reportData[touchpadOffset + 2 + connection.offset] & 0xF) * 255;
			final var y1 = ((reportData[touchpadOffset + 2 + connection.offset] & 0xF0) >> 4)
					+ reportData[touchpadOffset + 3 + connection.offset] * 16;

			final var downMouseButtons = input.getDownMouseButtons();
			if (touchpadButtonDown)
				synchronized (downMouseButtons) {
					downMouseButtons.add(down2 ? 2 : 1);
				}
			else if (prevTouchpadButtonDown)
				synchronized (downMouseButtons) {
					downMouseButtons.clear();
				}

			if (down1 && prevDown1) {
				final var dX1 = x1 - prevX1;
				final var dY1 = y1 - prevY1;

				if (!prevDown2 || touchpadButtonDown) {
					if (prevX1 > 0 && Math.abs(dX1) < TOUCHPAD_MAX_DELTA)
						input.setCursorDeltaX((int) (dX1 * TOUCHPAD_CURSOR_SENSITIVITY));

					if (prevY1 > 0 && Math.abs(dY1) < TOUCHPAD_MAX_DELTA)
						input.setCursorDeltaY((int) (dY1 * TOUCHPAD_CURSOR_SENSITIVITY));
				} else if (prevY1 > 0 && Math.abs(dY1) < TOUCHPAD_MAX_DELTA)
					input.setScrollClicks((int) (-dY1 * TOUCHPAD_SCROLL_SENSITIVITY));
			}

			prevTouchpadButtonDown = touchpadButtonDown;
			prevDown1 = down1;
			prevDown2 = down2;
			prevX1 = x1;
			prevY1 = y1;
		}
	}

	static final int USB_REPORT_LENGTH = 64;

	static final int BLUETOOTH_REPORT_LENGTH = 78;

	private static final int LOW_BATTERY_WARNING = 20;
	private static final int hidReportPlatformOffset = Main.isWindows ? 1 : 0;

	static HidDeviceInfo getHidDeviceInfo(final ControllerInfo controller, final String guid, final short productId,
			final String humanReadableName, final Logger log) {
		final var devices = PureJavaHidApi.enumerateDevices().stream()
				.filter(hidDeviceInfo -> hidDeviceInfo.getVendorId() == (short) 0x54C
						&& hidDeviceInfo.getProductId() == productId)
				.toList();

		log.log(Level.INFO, "Found " + devices.size() + " " + humanReadableName + " controller(s): "
				+ devices.stream().map(SonyExtension::getPrintableDeviceId).collect(Collectors.joining(", ")));

		final var count = devices.size();
		if (count < 1)
			return null;

		var deviceIndex = 0;
		if (count > 1) {
			final var presentJidsWithSameGuid = new ArrayList<Integer>();
			for (var i = GLFW.GLFW_JOYSTICK_1; i <= GLFW.GLFW_JOYSTICK_LAST; i++)
				if (GLFW.glfwJoystickPresent(i) && guid.equals(GLFW.glfwGetJoystickGUID(i)))
					presentJidsWithSameGuid.add(i);
			deviceIndex = presentJidsWithSameGuid.indexOf(controller.jid());
		}

		final var hidDeviceInfo = devices.get(deviceIndex);

		log.log(Level.INFO, Main.assembleControllerLoggingMessage(
				"Using " + humanReadableName + " controller with ID " + getPrintableDeviceId(hidDeviceInfo) + " as",
				controller));

		return hidDeviceInfo;
	}

	public static SonyExtension getIfAvailable(final Input input, final ControllerInfo controller) {
		final var dualShock4Extension = DualShock4Extension.getIfAvailable(input, controller);
		if (dualShock4Extension != null)
			return dualShock4Extension;

		return DualSenseExtension.getIfAvailable(input, controller);
	}

	private static String getPrintableDeviceId(final HidDeviceInfo device) {
		var deviceId = device.getDeviceId();
		if (deviceId == null)
			return "?";

		if (deviceId.endsWith("\0"))
			deviceId = deviceId.substring(0, deviceId.indexOf('\0'));

		return deviceId;
	}

	static boolean isBluetoothConnection(final int reportLength) {
		return reportLength != USB_REPORT_LENGTH;
	}

	private static float mapRawAxisToFloat(final byte value) {
		return value < 0 ? Input.normalize(value, -128, -1, 0f, 1f) : Input.normalize(value, 0, 127, -1f, 0f);
	}

	final Input input;
	final int jid;

	HidDevice hidDevice;

	byte[] hidReport;

	Connection connection;
	volatile boolean charging = true;
	volatile int batteryState;

	byte lx = Byte.MAX_VALUE;
	byte ly = Byte.MAX_VALUE;

	byte rx = Byte.MAX_VALUE;
	byte ry = Byte.MAX_VALUE;
	byte l2 = Byte.MAX_VALUE;
	byte r2 = Byte.MAX_VALUE;
	boolean triangle;
	boolean circle;
	boolean cross;
	boolean square;
	boolean dpadUp;
	boolean dpadDown;
	boolean dpadLeft;
	boolean dpadRight;
	boolean r3;
	boolean l3;
	boolean options;
	boolean share;
	boolean r1;
	boolean l1;
	boolean ps;
	boolean disconnected;

	SonyExtension(final Input input, final int jid) {
		this.input = input;
		this.jid = jid;
	}

	public void deInit(final boolean disconnected) {
		this.disconnected = true;

		if (hidDevice != null) {
			if (!disconnected)
				reset();
			try {
				hidDevice.close();
			} catch (final IllegalStateException e) {
			}
			hidDevice = null;
		}
	}

	public int getBatteryState() {
		return batteryState;
	}

	abstract int getButtonsOffset();

	abstract byte[] getDefaultHidReport();

	public boolean getGamepadState(final GLFWGamepadState state) {
		if (disconnected)
			return false;

		state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X, mapRawAxisToFloat(lx));
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y, mapRawAxisToFloat(ly));
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X, mapRawAxisToFloat(rx));
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y, mapRawAxisToFloat(ry));
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER, mapRawAxisToFloat(l2));
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER, mapRawAxisToFloat(r2));

		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_CROSS, (byte) (cross ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_CIRCLE, (byte) (circle ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_SQUARE, (byte) (square ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_TRIANGLE, (byte) (triangle ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER, (byte) (l1 ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER, (byte) (r1 ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_BACK, (byte) (share ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_START, (byte) (options ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_GUIDE, (byte) (ps ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB, (byte) (l3 ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB, (byte) (r3 ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP, (byte) (dpadUp ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, (byte) (dpadRight ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT, (byte) (dpadLeft ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN, (byte) (dpadDown ? 0x1 : 0x0));

		return true;
	}

	abstract int getL2Offset();

	abstract int getLightbarOffset();

	abstract long getLightRumbleDuration();

	abstract byte getLightRumbleStrength();

	abstract Logger getLogger();

	abstract int getRumbleOffset();

	abstract long getStrongRumbleDuration();

	abstract byte getStrongRumbleStrength();

	abstract int getTouchpadOffset();

	public boolean isCharging() {
		return charging;
	}

	boolean isInputReportValid(final byte reportId, final byte[] reportData, final int reportLength) {
		if (connection == null)
			return false;

		final var bluetooth = connection.isBluetooth();

		if (reportId != connection.inputReportId
				|| (bluetooth ? reportLength < BLUETOOTH_REPORT_LENGTH : reportLength != USB_REPORT_LENGTH)) {
			getLogger().log(Level.WARNING,
					"Received unexpected HID input report with ID " + reportId + " and length " + reportLength);

			return false;
		}

		if (bluetooth) {
			final var byteBuffer = ByteBuffer.wrap(reportData, 0, BLUETOOTH_REPORT_LENGTH);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

			final var crc32 = new CRC32();
			crc32.update(0xA1);

			byteBuffer.limit(BLUETOOTH_REPORT_LENGTH - 4);
			crc32.update(byteBuffer);
			final var calculatedCrc32Value = crc32.getValue();

			byteBuffer.limit(BLUETOOTH_REPORT_LENGTH);
			final var receivedCrc32Value = byteBuffer.getInt() & 0xFFFFFFFFL;

			if (receivedCrc32Value != calculatedCrc32Value) {
				getLogger().log(Level.WARNING, "Received faulty HID input report");
				return false;
			}
		}

		return true;
	}

	void reset() {
		final var defaultHidReport = getDefaultHidReport();
		if (defaultHidReport == null)
			return;

		hidReport = Arrays.copyOf(defaultHidReport, defaultHidReport.length);
		sendHidReport();
	}

	private void rumble(final long duration, final byte strength) {
		if (hidDevice == null || hidReport == null || connection == null)
			return;

		final var actualRumbleOffset = getRumbleOffset() + connection.offset;

		new Thread(() -> {
			synchronized (hidReport) {
				hidReport[actualRumbleOffset] = strength;
				sendHidReport();
				try {
					Thread.sleep(duration);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				hidReport[actualRumbleOffset] = 0;
				sendHidReport();
			}
		}).start();
	}

	public void rumbleLight() {
		rumble(getLightRumbleDuration(), getLightRumbleStrength());
	}

	public void rumbleStrong() {
		rumble(getStrongRumbleDuration(), getStrongRumbleStrength());
	}

	void sendHidReport() {
		if (hidDevice == null || connection == null)
			return;

		if (connection.isBluetooth()) {
			final var crc32 = new CRC32();
			crc32.update(0xA2);
			crc32.update(hidReport, 0, hidReport.length - 4);
			final var crc32Value = crc32.getValue();

			hidReport[hidReport.length - 4] = (byte) crc32Value;
			hidReport[hidReport.length - 3] = (byte) (crc32Value >> 8);
			hidReport[hidReport.length - 2] = (byte) (crc32Value >> 16);
			hidReport[hidReport.length - 1] = (byte) (crc32Value >> 24);
		}

		final var dataLength = hidReport.length - hidReportPlatformOffset;
		try {
			hidDevice.setOutputReport(hidReport[0],
					Arrays.copyOfRange(hidReport, 0 + hidReportPlatformOffset, hidReport.length), dataLength);
		} catch (final IllegalStateException e) {
		}
	}

	void setBatteryState(final int batteryState) {
		if (this.batteryState != batteryState) {
			this.batteryState = batteryState;

			updateLightbarColor();

			final var main = input.getMain();
			if (main != null)
				EventQueue.invokeLater(() -> {
					main.updateTitleAndTooltip();

					if (batteryState == LOW_BATTERY_WARNING)
						main.displayLowBatteryWarning(batteryState / 100f);
				});
		}
	}

	void setCharging(final boolean charging) {
		if (this.charging != charging) {
			this.charging = charging;

			updateLightbarColor();

			final var main = input.getMain();
			EventQueue.invokeLater(() -> {
				main.updateTitleAndTooltip();
				main.displayChargingStateInfo(charging);
			});
		}
	}

	void updateLightbarColor() {
		if (hidDevice == null || connection == null || hidReport == null)
			return;

		synchronized (hidReport) {
			final var lightbarOffset = getLightbarOffset();

			if (charging) {
				hidReport[lightbarOffset + connection.offset] = (byte) (batteryState >= 100 ? 0x0 : 0x1C);
				hidReport[lightbarOffset + 1 + connection.offset] = (byte) 0x1C;
				hidReport[lightbarOffset + 2 + connection.offset] = 0x0;
			} else {
				hidReport[lightbarOffset
						+ connection.offset] = (byte) (batteryState <= LOW_BATTERY_WARNING ? 0x1C : 0x0);
				hidReport[lightbarOffset + 1 + connection.offset] = 0;
				hidReport[lightbarOffset + 2
						+ connection.offset] = (byte) (batteryState <= LOW_BATTERY_WARNING ? 0x0 : 0x1C);
			}

			sendHidReport();
		}
	}
}

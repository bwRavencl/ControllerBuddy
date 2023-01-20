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

import java.awt.EventQueue;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.driver.Driver;
import de.bwravencl.controllerbuddy.input.driver.IGamepadStateProvider;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;

public abstract class SonyDriver extends Driver implements IGamepadStateProvider {

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

		abstract void handleBattery(byte[] reportData, int offset);

		abstract void handleNewConnection(int reportLength);

		@Override
		public void onInputReport(final HidDevice source, final byte reportID, final byte[] reportData,
				final int reportLength) {
			try {
				if (connection == null) {
					handleNewConnection(reportLength);
					reset();
				}

				if (disconnected) {
					hidDevice.setInputReportListener(null);
					return;
				}

				if (!isInputReportValid(reportID, reportData, reportLength))
					return;

				final var offset = connection.offset + hidReadReportPlatformOffset;

				lx = reportData[1 + offset];
				ly = reportData[2 + offset];
				rx = reportData[3 + offset];
				ry = reportData[4 + offset];

				l2 = reportData[getL2Offset() + offset];
				r2 = reportData[getL2Offset() + 1 + offset];

				final var buttonsOffset = getButtonsOffset();
				triangle = (reportData[buttonsOffset + offset] & 1 << 7) != 0;
				circle = (reportData[buttonsOffset + offset] & 1 << 6) != 0;
				cross = (reportData[buttonsOffset + offset] & 1 << 5) != 0;
				square = (reportData[buttonsOffset + offset] & 1 << 4) != 0;

				final var dpadData = (byte) (reportData[buttonsOffset + offset] & 0xF);
				switch (dpadData) {
				case 0 -> {
					dpadUp = true;
					dpadDown = false;
					dpadLeft = false;
					dpadRight = false;
				}
				case 1 -> {
					dpadUp = true;
					dpadDown = false;
					dpadLeft = false;
					dpadRight = true;
				}
				case 2 -> {
					dpadUp = false;
					dpadDown = false;
					dpadLeft = false;
					dpadRight = true;

				}
				case 3 -> {
					dpadUp = false;
					dpadDown = true;
					dpadLeft = false;
					dpadRight = true;
				}
				case 4 -> {
					dpadUp = false;
					dpadDown = true;
					dpadLeft = false;
					dpadRight = false;
				}
				case 5 -> {
					dpadUp = false;
					dpadDown = true;
					dpadLeft = true;
					dpadRight = false;
				}
				case 6 -> {
					dpadUp = false;
					dpadDown = false;
					dpadLeft = true;
					dpadRight = false;
				}
				case 7 -> {
					dpadUp = true;
					dpadDown = false;
					dpadLeft = true;
					dpadRight = false;
				}
				case 8 -> {
					dpadUp = false;
					dpadDown = false;
					dpadLeft = false;
					dpadRight = false;
				}
				default -> throw new IllegalArgumentException("Unexpected value: " + dpadData);
				}

				r3 = (reportData[buttonsOffset + 1 + offset] & 1 << 7) != 0;
				l3 = (reportData[buttonsOffset + 1 + offset] & 1 << 6) != 0;
				options = (reportData[buttonsOffset + 1 + offset] & 1 << 5) != 0;
				share = (reportData[buttonsOffset + 1 + offset] & 1 << 4) != 0;
				r1 = (reportData[buttonsOffset + 1 + offset] & 1 << 1) != 0;
				l1 = (reportData[buttonsOffset + 1 + offset] & 1 << 0) != 0;

				ps = (reportData[buttonsOffset + 2 + offset] & 1 << 0) != 0;

				ready = true;
				timestampLastInputReport = System.currentTimeMillis();

				if (controller.jid() != input.getController().jid())
					return;

				handleBattery(reportData, offset);

				final var main = input.getMain();
				if (!main.isLocalRunning() && !main.isServerRunning())
					return;

				final var touchpadButtonDown = (reportData[buttonsOffset + 2 + offset] & 1 << 2 - 1) != 0;

				final var touchpadOffset = getTouchpadOffset();
				final var down1 = reportData[touchpadOffset + offset] >> 7 == 0;
				final var down2 = reportData[touchpadOffset + 4 + offset] >> 7 == 0;
				final var x1 = reportData[touchpadOffset + 1 + offset]
						+ (reportData[touchpadOffset + 2 + offset] & 0xF) * 255;
				final var y1 = ((reportData[touchpadOffset + 2 + offset] & 0xF0) >> 4)
						+ reportData[touchpadOffset + 3 + offset] * 16;

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
			} catch (final Throwable t) {
				getLogger().log(Level.SEVERE, t.getMessage(), t);

				hidDevice.setInputReportListener(null);
				disconnected = true;
			}
		}
	}

	static final int USB_REPORT_LENGTH = 64;
	static final int BLUETOOTH_REPORT_LENGTH = 78;
	private static final int LOW_BATTERY_WARNING = 20;
	private static final long INPUT_REPORT_TIMEOUT = 5000L;

	static final int hidReadReportPlatformOffset = Main.isMac ? 0 : -1;
	private static final int hidWriteReportPlatformOffset = Main.isMac ? 0 : 1;

	static HidDeviceInfo getHidDeviceInfo(final List<ControllerInfo> presentControllers,
			final ControllerInfo selectedController, final short productId, final String humanReadableName,
			final Logger log) {
		final var devices = PureJavaHidApi.enumerateDevices().stream()
				.filter(hidDeviceInfo -> hidDeviceInfo.getVendorId() == (short) 0x54C
						&& hidDeviceInfo.getProductId() == productId)
				.toList();

		log.log(Level.INFO, "Found " + devices.size() + " " + humanReadableName + " controller(s): "
				+ devices.stream().map(SonyDriver::getPrintableDeviceId).collect(Collectors.joining(", ")));

		final var count = devices.size();
		if (count < 1)
			return null;

		var deviceIndex = 0;
		if (count > 1) {
			final var presentJidsWithSameGuid = presentControllers.stream()
					.filter(controller -> selectedController.guid() != null
							&& selectedController.guid().equals(controller.guid()))
					.collect(Collectors.toUnmodifiableList());
			deviceIndex = presentJidsWithSameGuid.indexOf(selectedController);
		}

		final var hidDeviceInfo = devices.get(deviceIndex);

		log.log(Level.INFO, Main.assembleControllerLoggingMessage(
				"Using " + humanReadableName + " controller with ID " + getPrintableDeviceId(hidDeviceInfo) + " as",
				selectedController));

		return hidDeviceInfo;
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
		return reportLength != USB_REPORT_LENGTH + hidReadReportPlatformOffset;
	}

	private static float mapRawAxisToFloat(final byte value) {
		return value < 0 ? Input.normalize(value, -128, -1, 0f, 1f) : Input.normalize(value, 0, 127, -1f, 0f);
	}

	volatile HidDevice hidDevice;
	byte[] hidReport;
	Connection connection;
	volatile Boolean charging;
	volatile Integer batteryCapacity;
	volatile byte lx = Byte.MAX_VALUE;
	volatile byte ly = Byte.MAX_VALUE;
	volatile byte rx = Byte.MAX_VALUE;
	volatile byte ry = Byte.MAX_VALUE;
	volatile byte l2 = Byte.MAX_VALUE;
	volatile byte r2 = Byte.MAX_VALUE;
	volatile boolean triangle;
	volatile boolean circle;
	volatile boolean cross;
	volatile boolean square;
	volatile boolean dpadUp;
	volatile boolean dpadDown;
	volatile boolean dpadLeft;
	volatile boolean dpadRight;
	volatile boolean r3;
	volatile boolean l3;
	volatile boolean options;
	volatile boolean share;
	volatile boolean r1;
	volatile boolean l1;
	volatile boolean ps;
	volatile boolean disconnected;
	volatile long timestampLastInputReport = Long.MAX_VALUE;

	SonyDriver(final Input input, final ControllerInfo controller) {
		super(input, controller);
	}

	@Override
	public synchronized void deInit(final boolean disconnected) {
		super.deInit(disconnected);

		this.disconnected = true;

		if (hidDevice == null)
			return;

		if (!disconnected)
			reset();

		synchronized (hidDevice) {
			try {
				hidDevice.close();
			} catch (final IllegalStateException e) {
			}
		}

		hidDevice = null;
	}

	abstract int getButtonsOffset();

	abstract byte[] getDefaultHidReport();

	@Override
	public boolean getGamepadState(final GLFWGamepadState state) {
		if (disconnected || !ready)
			return false;

		if (System.currentTimeMillis() - timestampLastInputReport > INPUT_REPORT_TIMEOUT) {
			getLogger().log(Level.WARNING, Main.assembleControllerLoggingMessage(
					"No new input report for more than " + INPUT_REPORT_TIMEOUT + " ms from", controller));

			return false;
		}

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

	@Override
	public String getTooltip(final String title) {
		if (disconnected || !ready || charging == null || batteryCapacity == null)
			return super.getTooltip(title);

		return MessageFormat.format(
				Main.strings.getString(
						charging ? "BATTERY_TOOLTIP_PERCENT_CHARGING" : "BATTERY_TOOLTIP_PERCENT_DISCHARGING"),
				title, batteryCapacity / 100f);
	}

	abstract int getTouchpadOffset();

	public Boolean isCharging() {
		return charging;
	}

	boolean isInputReportValid(final byte reportId, final byte[] reportData, final int reportLength) {
		if (connection == null)
			return false;

		final var bluetooth = connection.isBluetooth();

		if (reportId != connection.inputReportId
				|| (bluetooth ? reportLength < BLUETOOTH_REPORT_LENGTH + hidReadReportPlatformOffset
						: reportLength != USB_REPORT_LENGTH + hidReadReportPlatformOffset)) {
			getLogger().log(Level.WARNING,
					Main.assembleControllerLoggingMessage("Received unexpected HID input report with ID " + reportId
							+ " and length " + reportLength + " from", controller));

			return false;
		}

		if (bluetooth) {
			final var byteBuffer = ByteBuffer.wrap(reportData, 0,
					BLUETOOTH_REPORT_LENGTH + hidReadReportPlatformOffset);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

			final var crc32 = new CRC32();
			crc32.update(0xA1);

			byteBuffer.limit(BLUETOOTH_REPORT_LENGTH + hidReadReportPlatformOffset - 4);
			if (!Main.isMac)
				crc32.update(reportId);
			crc32.update(byteBuffer);
			final var calculatedCrc32Value = crc32.getValue();

			byteBuffer.limit(BLUETOOTH_REPORT_LENGTH + hidReadReportPlatformOffset);
			final var receivedCrc32Value = byteBuffer.getInt() & 0xFFFFFFFFL;

			if (receivedCrc32Value != calculatedCrc32Value) {
				getLogger().log(Level.WARNING,
						Main.assembleControllerLoggingMessage("Received faulty HID input report from", controller));
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean isReady() {
		return ready;
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
			if (hidDevice != null)
				synchronized (hidDevice) {
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

	@Override
	public void rumbleLight() {
		rumble(getLightRumbleDuration(), getLightRumbleStrength());
	}

	@Override
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

		final var dataLength = hidReport.length - hidWriteReportPlatformOffset;
		try {
			hidDevice.setOutputReport(hidReport[0],
					Arrays.copyOfRange(hidReport, 0 + hidWriteReportPlatformOffset, hidReport.length), dataLength);
		} catch (final IllegalStateException e) {
		}
	}

	void setBatteryCapacity(final int batteryCapacity) {
		if (disconnected || !ready || this.batteryCapacity != null && this.batteryCapacity == batteryCapacity)
			return;

		this.batteryCapacity = batteryCapacity;

		updateLightbarColor();

		final var main = input.getMain();
		if (main != null)
			EventQueue.invokeLater(() -> {
				main.updateTitleAndTooltip();

				if (batteryCapacity == LOW_BATTERY_WARNING) {

					final var batteryLevelString = MessageFormat.format("{0,number,percent}", batteryCapacity / 100f);
					main.displayLowBatteryWarning(batteryLevelString);
				}
			});
	}

	void setCharging(final boolean charging) {
		if (disconnected || !ready)
			return;

		final var firstCall = this.charging == null;

		if (!firstCall && this.charging == charging)
			return;

		this.charging = charging;

		updateLightbarColor();

		if (!firstCall) {
			final var main = input.getMain();
			EventQueue.invokeLater(() -> {
				main.updateTitleAndTooltip();
				main.displayChargingStateInfo(charging, batteryCapacity);
			});
		}
	}

	void updateLightbarColor() {
		if (disconnected || hidDevice == null || connection == null || hidReport == null || charging == null
				|| batteryCapacity == null)
			return;

		synchronized (hidDevice) {
			final var lightbarOffset = getLightbarOffset();

			if (charging) {
				hidReport[lightbarOffset + connection.offset] = (byte) (batteryCapacity >= 100 ? 0x0 : 0x1C);
				hidReport[lightbarOffset + 1 + connection.offset] = (byte) 0x1C;
				hidReport[lightbarOffset + 2 + connection.offset] = 0x0;
			} else {
				hidReport[lightbarOffset
						+ connection.offset] = (byte) (batteryCapacity <= LOW_BATTERY_WARNING ? 0x1C : 0x0);
				hidReport[lightbarOffset + 1 + connection.offset] = 0;
				hidReport[lightbarOffset + 2
						+ connection.offset] = (byte) (batteryCapacity <= LOW_BATTERY_WARNING ? 0x0 : 0x1C);
			}

			sendHidReport();
		}
	}
}

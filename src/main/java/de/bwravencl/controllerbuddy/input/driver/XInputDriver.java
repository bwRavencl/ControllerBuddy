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

package de.bwravencl.controllerbuddy.input.driver;

import java.awt.EventQueue;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.XInputDevice14;
import com.github.strikerx3.jxinput.enums.XInputBatteryDeviceType;
import com.github.strikerx3.jxinput.enums.XInputBatteryLevel;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;
import com.sun.jna.Platform;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;

public class XInputDriver extends Driver {

	public static class XInputDriverBuilder implements IDriverBuilder {

		@Override
		public Driver getIfAvailable(final Input input, final List<ControllerInfo> presentControllers,
				final ControllerInfo controller) {
			if (Platform.isIntel() && Main.isWindows && isXInputController(controller) && XInputDevice.isAvailable()) {
				final var presentXInputControllers = presentControllers.stream()
						.filter(XInputDriver::isXInputController).collect(Collectors.toUnmodifiableList());
				if (presentXInputControllers.size() > 1) {
					log.log(Level.WARNING,
							"Found more than one XInput controller - extended XInput support will be unavailable");
					return null;
				}

				try {
					return new XInputDriver(input, controller);
				} catch (final Throwable t) {
					log.log(Level.SEVERE, t.getMessage(), t);
				}
			}

			return null;
		}
	}

	private static final Logger log = Logger.getLogger(XInputDriver.class.getName());
	private static final long BATTERY_LEVEL_POLL_INTERVAL = 60L;

	private static final int MAX_MOTOR_SPEED = 65535;

	private static final boolean isXInputController(final ControllerInfo controller) {
		return "78696e70757401000000000000000000".equals(controller.guid()) || controller.name().startsWith("Xbox")
				|| controller.name().startsWith("XInput");
	}

	private static float mapTriggerAxisValue(final float value) {
		return Input.normalize(value, 0f, 1f, -1f, 1f);
	}

	private volatile XInputDevice xinputDevice;
	private ScheduledExecutorService executorService;
	private volatile String batteryLevelString;

	private XInputDriver(final Input input, final ControllerInfo controller) throws XInputNotLoadedException {
		super(input, controller);

		if (XInputDevice14.isAvailable()) {
			xinputDevice = XInputDevice14.getDeviceFor(0);

			var batteryLevelAvailable = false;
			if (xinputDevice instanceof final XInputDevice14 xinputDevice14) {
				final var batteryInformation = xinputDevice14.getBatteryInformation(XInputBatteryDeviceType.GAMEPAD);

				batteryLevelAvailable = switch (batteryInformation.getType()) {
				case ALKALINE, NIMH -> true;
				default -> false;
				};
			}

			if (batteryLevelAvailable) {
				executorService = Executors.newSingleThreadScheduledExecutor();
				executorService.scheduleAtFixedRate(() -> {
					EventQueue.invokeLater(() -> {
						if (controller.jid() != input.getController().jid())
							return;

						if (xinputDevice instanceof final XInputDevice14 xinputDevice14) {
							final var batteryInformation = xinputDevice14
									.getBatteryInformation(XInputBatteryDeviceType.GAMEPAD);
							final var batteryLevel = batteryInformation.getLevel();

							batteryLevelString = Main.strings.getString(switch (batteryInformation.getLevel()) {
							case EMPTY -> "BATTERY_LEVEL_EMPTY";
							case LOW -> "BATTERY_LEVEL_LOW";
							case MEDIUM -> "BATTERY_LEVEL_MEDIUM";
							case FULL -> "BATTERY_LEVEL_FULL";
							});

							input.getMain().updateTitleAndTooltip();
							if (batteryLevel == XInputBatteryLevel.LOW)
								input.getMain().displayLowBatteryWarning(batteryLevelString);
						}
					});
				}, 0L, BATTERY_LEVEL_POLL_INTERVAL, TimeUnit.SECONDS);
			}
		} else
			xinputDevice = XInputDevice.getDeviceFor(0);

		ready = xinputDevice.poll();

		log.log(Level.INFO,
				Main.assembleControllerLoggingMessage("Using XInput "
						+ XInputDevice.getLibraryVersion().name().substring("XINPUT_".length()).replace('_', '.')
						+ " for", controller));
	}

	@Override
	public void deInit(final boolean disconnected) {
		super.deInit(disconnected);

		if (executorService != null)
			try {
				executorService.awaitTermination(2L, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		xinputDevice = null;
	}

	@Override
	public boolean getGamepadState(final GLFWGamepadState state) {
		if (xinputDevice == null || !xinputDevice.isConnected())
			return false;

		xinputDevice.poll();

		final var components = xinputDevice.getComponents();

		final var axes = components.getAxes();
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X, axes.lx);
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y, -axes.ly);
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X, axes.rx);
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y, -axes.ry);
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER, mapTriggerAxisValue(axes.lt));
		state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER, mapTriggerAxisValue(axes.rt));

		final var buttons = components.getButtons();
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_A, (byte) (buttons.a ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_B, (byte) (buttons.b ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_X, (byte) (buttons.x ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_Y, (byte) (buttons.y ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER, (byte) (buttons.lShoulder ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER, (byte) (buttons.rShoulder ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_BACK, (byte) (buttons.back ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_START, (byte) (buttons.start ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_GUIDE, (byte) (buttons.guide ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB, (byte) (buttons.lThumb ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB, (byte) (buttons.rThumb ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP, (byte) (buttons.up ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, (byte) (buttons.right ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT, (byte) (buttons.left ? 0x1 : 0x0));
		state.buttons(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN, (byte) (buttons.down ? 0x1 : 0x0));

		return true;
	}

	@Override
	public String getTooltip(final String title) {
		if (batteryLevelString == null)
			return title;

		return MessageFormat.format(Main.strings.getString("BATTERY_TOOLTIP_STRING"), title, batteryLevelString);
	}

	private void rumble(final long duration, final int leftMotor, final int rightMotor) {
		if (xinputDevice == null)
			return;

		new Thread(() -> {
			synchronized (xinputDevice) {
				xinputDevice.setVibration(leftMotor, rightMotor);
				try {
					Thread.sleep(duration);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				xinputDevice.setVibration(0, 0);
			}
		}).start();
	}

	@Override
	public void rumbleLight() {
		rumble(30L, MAX_MOTOR_SPEED, 0);
	}

	@Override
	public void rumbleStrong() {
		rumble(45L, 0, MAX_MOTOR_SPEED);
	}
}
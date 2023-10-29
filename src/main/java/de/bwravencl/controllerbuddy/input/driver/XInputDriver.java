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

import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.XInputDevice14;
import com.github.strikerx3.jxinput.enums.XInputBatteryDeviceType;
import com.github.strikerx3.jxinput.enums.XInputBatteryLevel;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;
import com.sun.jna.Platform;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.util.RunnableWithDefaultExceptionHandler;
import java.awt.EventQueue;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

public class XInputDriver extends Driver implements IGamepadStateProvider {

    private static final Logger log = Logger.getLogger(XInputDriver.class.getName());
    private static final long BATTERY_LEVEL_POLL_INTERVAL = 60L;
    private static final int MAX_MOTOR_SPEED = 65_535;
    private final Lock xinputDeviceLock = new ReentrantLock();
    private volatile XInputDevice xinputDevice;
    private ScheduledExecutorService executorService;
    private volatile String batteryLevelString;

    @SuppressWarnings("FutureReturnValueIgnored")
    private XInputDriver(final Input input, final ControllerInfo controller) throws XInputNotLoadedException {
        super(input, controller);

        final XInputDevice[] xinputDevices;
        if (XInputDevice14.isAvailable()) {
            xinputDevices = XInputDevice14.getAllDevices();
        } else {
            xinputDevices = XInputDevice.getAllDevices();
        }

        final var optionalXinputDevice =
                Arrays.stream(xinputDevices).filter(XInputDevice::poll).findFirst();

        if (optionalXinputDevice.isEmpty()) {
            throw new IllegalStateException("No XInput Device connected");
        }

        xinputDevice = optionalXinputDevice.get();
        ready = true;

        var batteryLevelAvailable = false;
        if (xinputDevice instanceof final XInputDevice14 xinputDevice14) {
            final var batteryInformation = xinputDevice14.getBatteryInformation(XInputBatteryDeviceType.GAMEPAD);
            if (batteryInformation != null) {
                batteryLevelAvailable = switch (batteryInformation.getType()) {
                    case ALKALINE, NIMH -> true;
                    default -> false;};
            }
        }

        if (batteryLevelAvailable) {
            executorService = Executors.newSingleThreadScheduledExecutor(
                    Thread.ofVirtual().factory());
            executorService.scheduleAtFixedRate(
                    new RunnableWithDefaultExceptionHandler(this::pollBatteryLevel),
                    0L,
                    BATTERY_LEVEL_POLL_INTERVAL,
                    TimeUnit.SECONDS);
        }

        log.log(
                Level.INFO,
                Main.assembleControllerLoggingMessage(
                        "Using XInput "
                                + XInputDevice.getLibraryVersion()
                                        .name()
                                        .substring("XINPUT_".length())
                                        .replace('_', '.')
                                + " controller with ID " + xinputDevice.getPlayerNum() + " as",
                        controller));
    }

    private static boolean isXInputController(final ControllerInfo controller) {
        return "78696e70757401000000000000000000".equals(controller.guid())
                || controller.name().startsWith("Xbox")
                || controller.name().startsWith("XInput");
    }

    private static float mapTriggerAxisValue(final float value) {
        return Input.normalize(value, 0f, 1f, -1f, 1f);
    }

    @Override
    public void deInit(final boolean disconnected) {
        super.deInit(disconnected);

        xinputDeviceLock.lock();
        try {
            if (executorService != null) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    executorService.awaitTermination(2L, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            xinputDevice = null;
        } finally {
            xinputDeviceLock.unlock();
        }
    }

    @Override
    public boolean getGamepadState(final GLFWGamepadState state) {
        if (xinputDevice == null || !xinputDevice.poll()) {
            return false;
        }

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
        if (batteryLevelString == null) {
            return super.getTooltip(title);
        }

        return MessageFormat.format(Main.strings.getString("BATTERY_TOOLTIP_STRING"), title, batteryLevelString);
    }

    private void pollBatteryLevel() {
        EventQueue.invokeLater(() -> {
            if (controller.jid() != input.getController().jid()) {
                return;
            }

            if (xinputDevice instanceof final XInputDevice14 xinputDevice14) {
                final var batteryInformation = xinputDevice14.getBatteryInformation(XInputBatteryDeviceType.GAMEPAD);
                final var batteryLevel = batteryInformation.getLevel();

                batteryLevelString = Main.strings.getString(
                        switch (batteryInformation.getLevel()) {
                            case EMPTY -> "BATTERY_LEVEL_EMPTY";
                            case LOW -> "BATTERY_LEVEL_LOW";
                            case MEDIUM -> "BATTERY_LEVEL_MEDIUM";
                            case FULL -> "BATTERY_LEVEL_FULL";
                        });

                input.getMain().updateTitleAndTooltip();
                if (batteryLevel == XInputBatteryLevel.LOW) {
                    input.getMain().displayLowBatteryWarning(batteryLevelString);
                }
            }
        });
    }

    private void rumble(final long duration, final int leftMotor, final int rightMotor) {
        if (xinputDevice == null) {
            return;
        }

        Thread.startVirtualThread(() -> {
            xinputDeviceLock.lock();
            try {
                if (xinputDevice == null) {
                    return;
                }

                xinputDevice.setVibration(leftMotor, rightMotor);
                try {
                    Thread.sleep(duration);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                xinputDevice.setVibration(0, 0);
            } finally {
                xinputDeviceLock.unlock();
            }
        });
    }

    @Override
    public void rumbleLight() {
        rumble(60L, MAX_MOTOR_SPEED, 0);
    }

    @Override
    public void rumbleStrong() {
        rumble(90L, 0, MAX_MOTOR_SPEED);
    }

    public static class XInputDriverBuilder implements IDriverBuilder {

        @Override
        public Driver getIfAvailable(
                final Input input,
                final List<ControllerInfo> presentControllers,
                final ControllerInfo selectedController) {
            if (Platform.isIntel()
                    && Main.isWindows
                    && isXInputController(selectedController)
                    && XInputDevice.isAvailable()) {
                final var presentXInputControllers = presentControllers.stream()
                        .filter(XInputDriver::isXInputController)
                        .toList();
                if (presentXInputControllers.size() > 1) {
                    log.log(Level.WARNING, "Found more than one XInput controller - XInput driver disabled");
                    return null;
                }

                try {
                    return new XInputDriver(input, selectedController);
                } catch (final XInputNotLoadedException e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }

            return null;
        }
    }
}

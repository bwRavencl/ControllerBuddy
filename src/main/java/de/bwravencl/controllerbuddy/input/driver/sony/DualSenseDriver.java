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

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.driver.Driver;
import de.bwravencl.controllerbuddy.input.driver.IDriverBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.hid4java.HidDevice;
import org.lwjgl.glfw.GLFW;

public final class DualSenseDriver extends SonyDriver {

    private static final Logger log = Logger.getLogger(DualSenseDriver.class.getName());
    private static final byte USB_INPUT_REPORT_ID = 0x1;
    private static final byte BLUETOOTH_INPUT_REPORT_ID = 0x31;
    private static final Connection UsbConnection = new Connection(0, USB_INPUT_REPORT_ID);
    private static final Connection BluetoothConnection = new Connection(1, BLUETOOTH_INPUT_REPORT_ID);

    private DualSenseDriver(final Input input, final ControllerInfo controller, final HidDevice hidDevice) {
        super(input, controller, hidDevice);
    }

    @Override
    int getButtonsOffset() {
        return 8;
    }

    @Override
    byte[] getDefaultHidReport() {
        if (connection == null) {
            return null;
        }

        final byte[] defaultHidReport;
        if (connection.isBluetooth()) {
            defaultHidReport = new byte[BLUETOOTH_REPORT_LENGTH - 1];

            defaultHidReport[0] = 0x2;
        } else {
            defaultHidReport = new byte[47];
        }

        defaultHidReport[connection.offset()] = 0x3;
        defaultHidReport[1 + connection.offset()] = 0x15;

        defaultHidReport[44 + connection.offset()] = (byte) 0x0;
        defaultHidReport[45 + connection.offset()] = (byte) 0x0;
        defaultHidReport[46 + connection.offset()] = (byte) 0xFF;

        return defaultHidReport;
    }

    @Override
    byte getDefaultHidReportId() {
        if (connection.isBluetooth()) {
            return (byte) 0x31;
        } else {
            return (byte) 0x2;
        }
    }

    @Override
    int getL2Offset() {
        return 5;
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
    int getLightbarOffset() {
        return 44;
    }

    @Override
    Logger getLogger() {
        return log;
    }

    @Override
    int getRumbleOffset() {
        return 3;
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
    void handleBattery(final byte[] reportData, final int offset) {
        final var chargingStatus = (reportData[53 + offset] & 0xF0) >> 4;
        final var batteryData = reportData[53 + offset] & 0xF;

        final int batteryCapacity;
        final var charging =
                switch (chargingStatus) {
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

    @Override
    boolean reset() {
        if (connection == null) {
            return false;
        }

        if (connection.isBluetooth()) {
            final var defaultHidReport = getDefaultHidReport();
            if (defaultHidReport == null) {
                return false;
            }

            hidReport = Arrays.copyOf(defaultHidReport, defaultHidReport.length);
            hidReport[2] = 0x8;

            if (!sendHidReport()) {
                return false;
            }
        }

        return super.reset();
    }

    public static class DualSenseDriverBuilder implements IDriverBuilder {

        @Override
        public Driver getIfAvailable(
                final Input input,
                final List<ControllerInfo> presentControllers,
                final ControllerInfo selectedController) {
            final String name;
            if (Main.isMac) {
                name = GLFW.glfwGetGamepadName(selectedController.jid());
            } else {
                name = selectedController.name();
            }

            if (!"PS5 Controller".equals(name)) {
                return null;
            }

            final var hidDevice = getHidDevice(presentControllers, selectedController, (short) 0xCE6, "DualSense", log);

            if (hidDevice != null) {
                return new DualSenseDriver(input, selectedController, hidDevice);
            }

            return null;
        }
    }
}

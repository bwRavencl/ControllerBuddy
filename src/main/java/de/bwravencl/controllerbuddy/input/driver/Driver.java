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

import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import io.github.classgraph.ClassGraph;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public abstract class Driver {

    private static final List<? extends IDriverBuilder> driverBuilders;

    static {
        try (final var scanResult = new ClassGraph()
                .acceptPackages(Driver.class.getPackageName())
                .enableClassInfo()
                .scan()) {
            final var classInfoList = scanResult.getClassesImplementing(IDriverBuilder.class);
            driverBuilders = classInfoList.stream()
                    .map(classInfo -> {
                        try {
                            return classInfo
                                    .loadClass(IDriverBuilder.class)
                                    .getDeclaredConstructor()
                                    .newInstance();
                        } catch (final InstantiationException
                                | IllegalAccessException
                                | IllegalArgumentException
                                | InvocationTargetException
                                | NoSuchMethodException
                                | SecurityException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .sorted()
                    .toList();
        }
    }

    protected final Input input;
    protected final ControllerInfo controller;
    protected volatile boolean ready;

    protected Driver(final Input input, final ControllerInfo controller) {
        this.input = input;
        this.controller = controller;
    }

    public static Driver getIfAvailable(
            final Input input, final List<ControllerInfo> presentControllers, final ControllerInfo selectedController) {

        for (final var driverBuilder : driverBuilders) {
            final var driver = driverBuilder.getIfAvailable(input, presentControllers, selectedController);
            if (driver != null) {
                return driver;
            }
        }

        return null;
    }

    public void deInit(final boolean disconnected) {
        ready = false;
    }

    public String getTooltip(final String title) {
        return title;
    }

    public boolean isReady() {
        return ready;
    }

    public abstract void rumbleLight();

    public abstract void rumbleStrong();
}

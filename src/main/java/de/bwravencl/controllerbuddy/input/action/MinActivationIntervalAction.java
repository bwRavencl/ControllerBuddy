/* Copyright (C) 2023  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.MinActivationIntervalEditorBuilder;

abstract class MinActivationIntervalAction<V extends Number> extends DescribableAction<V>
        implements IInitializationAction<V> {

    @ActionProperty(
            label = "MIN_ACTIVATION_INTERVAL",
            editorBuilder = MinActivationIntervalEditorBuilder.class,
            order = 500)
    private int minActivationInterval = 0;

    private transient boolean wasUp = true;

    private transient long minActivationTime = 0L;

    public int getMinActivationInterval() {
        return minActivationInterval;
    }

    boolean handleMinActivationInterval(final boolean hot) {
        if (minActivationInterval > 0L) {
            final var currentTime = System.currentTimeMillis();

            if (hot) {
                if (wasUp) {
                    wasUp = false;
                    minActivationTime = currentTime + minActivationInterval;
                }
            } else {
                wasUp = true;

                if (currentTime <= minActivationTime) {
                    return true;
                }

                minActivationTime = 0L;
            }
        }

        return hot;
    }

    @Override
    public void init(final Input input) {
        wasUp = true;
        minActivationTime = 0L;
    }

    public void setMinActivationInterval(final int minActivationInterval) {
        this.minActivationInterval = minActivationInterval;
    }
}

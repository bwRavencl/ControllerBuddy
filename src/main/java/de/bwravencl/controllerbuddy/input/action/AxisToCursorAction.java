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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.DeadZoneEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ExponentEditorBuilder;

@Action(label = "TO_CURSOR_ACTION", category = ActionCategory.AXIS, order = 25)
public final class AxisToCursorAction extends ToCursorAction<Float> implements IAxisToAction {

    private static final float DEFAULT_DEAD_ZONE = 0.1f;
    private static final float DEFAULT_EXPONENT = 2f;

    @ActionProperty(label = "DEAD_ZONE", editorBuilder = DeadZoneEditorBuilder.class, order = 13)
    private float deadZone = DEFAULT_DEAD_ZONE;

    @ActionProperty(label = "EXPONENT", editorBuilder = ExponentEditorBuilder.class, order = 12)
    private float exponent = DEFAULT_EXPONENT;

    @Override
    public void doAction(final Input input, final int component, final Float value) {
        final var absValue = Math.abs(value);

        if (!input.isAxisSuspended(component) && absValue > deadZone) {
            final var inMax = (float) Math.pow((1f - deadZone) * 100f, exponent);

            final var d = Input.normalize(
                            Math.signum(value) * (float) Math.pow((absValue - deadZone) * 100f, exponent),
                            -inMax,
                            inMax,
                            -cursorSensitivity,
                            cursorSensitivity)
                    * input.getRateMultiplier();
            moveCursor(input, d);
        } else {
            remainingD = 0f;
        }
    }

    public float getDeadZone() {
        return deadZone;
    }

    public float getExponent() {
        return exponent;
    }

    public void setDeadZone(final float deadZone) {
        this.deadZone = deadZone;
    }

    public void setExponent(final float exponent) {
        this.exponent = exponent;
    }
}

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
import de.bwravencl.controllerbuddy.input.action.gui.InitialValueEditorBuilder;

@Action(label = "TO_AXIS_ACTION", category = ActionCategory.AXIS, order = 10)
public class AxisToAxisAction extends ToAxisAction<Float> implements IAxisToAction, IInitializationAction<Float> {

    private static final float DEFAULT_INITIAL_VALUE = 0f;
    private static final float DEFAULT_DEAD_ZONE = 0f;
    private static final float DEFAULT_EXPONENT = 1f;

    @ActionProperty(label = "DEAD_ZONE", editorBuilder = DeadZoneEditorBuilder.class, order = 100)
    float deadZone = DEFAULT_DEAD_ZONE;

    @ActionProperty(label = "EXPONENT", editorBuilder = ExponentEditorBuilder.class, order = 101)
    float exponent = DEFAULT_EXPONENT;

    @ActionProperty(label = "INITIAL_VALUE", editorBuilder = InitialValueEditorBuilder.class, order = 202)
    float initialValue = DEFAULT_INITIAL_VALUE;

    @Override
    public void doAction(final Input input, final int component, Float value) {
        if (!input.isAxisSuspended(component)) {
            if (Math.abs(value) <= deadZone) value = 0f;
            else {
                final float inMax;
                if (exponent > 1f) {
                    inMax = (float) Math.pow((1f - deadZone) * 100f, exponent);

                    value = Math.signum(value) * (float) Math.pow((Math.abs(value) - deadZone) * 100f, exponent);
                } else inMax = 1f;

                if (value >= 0f) value = Input.normalize(value, deadZone, inMax, 0f, 1f);
                else value = Input.normalize(value, -inMax, -deadZone, -1f, 0f);
            }

            input.setAxis(virtualAxis, invert ? -value : value, false, null);
        }
    }

    public float getDeadZone() {
        return deadZone;
    }

    public float getExponent() {
        return exponent;
    }

    public float getInitialValue() {
        return initialValue;
    }

    @Override
    public void init(final Input input) {
        if (!input.isSkipAxisInitialization())
            input.setAxis(virtualAxis, invert ? -initialValue : initialValue, false, null);
    }

    public void setDeadZone(final float deadZone) {
        this.deadZone = deadZone;
    }

    public void setExponent(final float exponent) {
        this.exponent = exponent;
    }

    public void setInitialValue(final float initialValue) {
        this.initialValue = initialValue;
    }
}

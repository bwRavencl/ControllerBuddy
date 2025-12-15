/* Copyright (C) 2014  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.gui.AxisValueEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DeadZoneEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ExponentEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MaxAxisValueEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MinAxisValueEditorBuilder;

@Action(label = "TO_AXIS_ACTION", category = ActionCategory.AXIS_AND_TRIGGER, order = 10)
public class AxisToAxisAction extends ToAxisAction<Float> implements IAxisToAction, IInitializationAction<Float> {

	private static final float DEFAULT_DEAD_ZONE = 0f;

	private static final float DEFAULT_EXPONENT = 1f;

	private static final float DEFAULT_INITIAL_VALUE = 0f;

	private static final float DEFAULT_MAX_VALUE = 1f;

	private static final float DEFAULT_MIN_VALUE = -1f;

	@ActionProperty(label = "DEAD_ZONE", editorBuilder = DeadZoneEditorBuilder.class, order = 100)
	float deadZone = DEFAULT_DEAD_ZONE;

	@ActionProperty(label = "EXPONENT", editorBuilder = ExponentEditorBuilder.class, order = 103)
	float exponent = DEFAULT_EXPONENT;

	@ActionProperty(label = "INITIAL_VALUE", editorBuilder = AxisValueEditorBuilder.class, order = 202)
	float initialValue = DEFAULT_INITIAL_VALUE;

	@ActionProperty(label = "MAX_AXIS_VALUE", editorBuilder = MaxAxisValueEditorBuilder.class, order = 102)
	float maxValue = DEFAULT_MAX_VALUE;

	@ActionProperty(label = "MIN_AXIS_VALUE", editorBuilder = MinAxisValueEditorBuilder.class, order = 101)
	float minValue = DEFAULT_MIN_VALUE;

	@Override
	public void doAction(final Input input, final int component, Float value) {
		if (input.isAxisSuspended(component)) {
			return;
		}

		final var absValue = Math.abs(value);

		if (absValue <= deadZone) {
			value = 0f;
		} else {
			final float inMax;
			if (exponent != 0f) {
				inMax = (float) Math.pow((1f - deadZone) * 100f, exponent);

				value = Math.signum(value) * (float) Math.pow((absValue - deadZone) * 100f, exponent);
			} else {
				inMax = 1f;
			}

			if (value >= 0f) {
				value = Input.normalize(value, deadZone, inMax, 0f, maxValue);
			} else {
				value = Input.normalize(value, -inMax, -deadZone, minValue, 0f);
			}
		}

		input.setAxis(virtualAxis, invert ? -value : value, false, null, null, null);
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

	public float getMaxValue() {
		return maxValue;
	}

	public float getMinValue() {
		return minValue;
	}

	@Override
	public void init(final Input input) {
		if (!input.isSkipAxisInitialization()) {
			input.setAxis(virtualAxis, invert ? -initialValue : initialValue, false, null, null, null);
		}
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

	public void setMaxValue(final float maxValue) {
		this.maxValue = maxValue;
	}

	public void setMinValue(final float minValue) {
		this.minValue = minValue;
	}
}

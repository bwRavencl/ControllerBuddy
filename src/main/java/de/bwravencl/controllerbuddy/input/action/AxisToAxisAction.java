/*
 * Copyright (C) 2014 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
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

/// Maps a physical axis input to a virtual axis output.
///
/// Applies dead zone filtering, exponent-based response curves, and value
/// normalization to translate raw axis values into the configured virtual axis
/// range.
@Action(icon = "🕹", title = "TO_AXIS_ACTION_TITLE", description = "TO_AXIS_ACTION_DESCRIPTION", category = ActionCategory.AXIS_AND_TRIGGER, order = 10)
public class AxisToAxisAction extends ToAxisAction<Float> implements IAxisToAction, IInitializationAction<Float> {

	/// Default dead zone threshold below which axis values are treated as zero.
	private static final float DEFAULT_DEAD_ZONE = 0f;

	/// Default exponent used for the response curve (linear).
	private static final float DEFAULT_EXPONENT = 1f;

	/// Default initial value assigned to the virtual axis on initialization.
	private static final float DEFAULT_INITIAL_VALUE = 0f;

	/// Default maximum output value for the virtual axis.
	private static final float DEFAULT_MAX_VALUE = 1f;

	/// Default minimum output value for the virtual axis.
	private static final float DEFAULT_MIN_VALUE = -1f;

	/// The dead zone threshold below which axis input values are treated as zero.
	@ActionProperty(icon = "🚫", title = "DEAD_ZONE_TITLE", description = "DEAD_ZONE_DESCRIPTION", editorBuilder = DeadZoneEditorBuilder.class, order = 100)
	float deadZone = DEFAULT_DEAD_ZONE;

	/// The exponent applied to the input value for a non-linear response curve.
	@ActionProperty(icon = "📈", title = "EXPONENT_TITLE", description = "EXPONENT_DESCRIPTION", editorBuilder = ExponentEditorBuilder.class, order = 103)
	float exponent = DEFAULT_EXPONENT;

	/// The value assigned to the virtual axis when the action is initialized.
	@ActionProperty(icon = "🚩", title = "INITIAL_VALUE_TITLE", description = "INITIAL_VALUE_DESCRIPTION", editorBuilder = AxisValueEditorBuilder.class, order = 202)
	float initialValue = DEFAULT_INITIAL_VALUE;

	/// The maximum output value mapped to the virtual axis.
	@ActionProperty(icon = "≤", title = "MAX_AXIS_VALUE_TITLE", description = "MAX_AXIS_VALUE_DESCRIPTION", editorBuilder = MaxAxisValueEditorBuilder.class, order = 102)
	float maxValue = DEFAULT_MAX_VALUE;

	/// The minimum output value mapped to the virtual axis.
	@ActionProperty(icon = "≥", title = "MIN_AXIS_VALUE_TITLE", description = "MIN_AXIS_VALUE_DESCRIPTION", editorBuilder = MinAxisValueEditorBuilder.class, order = 101)
	float minValue = DEFAULT_MIN_VALUE;

	/// Processes the axis input value by applying dead zone, exponent curve, and
	/// normalization, then sets the virtual axis output.
	///
	/// @param input the input state
	/// @param component the component index
	/// @param value the raw axis value
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

	/// Returns the dead zone threshold below which axis values are treated as zero.
	///
	/// @return the dead zone value
	public float getDeadZone() {
		return deadZone;
	}

	/// Returns the exponent used for the response curve.
	///
	/// @return the exponent value
	public float getExponent() {
		return exponent;
	}

	/// Returns the initial value assigned to the virtual axis during
	/// initialization.
	///
	/// @return the initial axis value
	public float getInitialValue() {
		return initialValue;
	}

	/// Returns the maximum output axis value.
	///
	/// @return the maximum value
	public float getMaxValue() {
		return maxValue;
	}

	/// Returns the minimum output axis value.
	///
	/// @return the minimum value
	public float getMinValue() {
		return minValue;
	}

	/// Initializes the virtual axis to its configured initial value.
	///
	/// @param input the input state
	@Override
	public void init(final Input input) {
		if (!input.isSkipAxisInitialization()) {
			input.setAxis(virtualAxis, invert ? -initialValue : initialValue, false, null, null, null);
		}
	}

	/// Sets the dead zone threshold.
	///
	/// @param deadZone the dead zone value
	public void setDeadZone(final float deadZone) {
		this.deadZone = deadZone;
	}

	/// Sets the exponent for the response curve.
	///
	/// @param exponent the exponent value
	public void setExponent(final float exponent) {
		this.exponent = exponent;
	}

	/// Sets the initial value for the virtual axis.
	///
	/// @param initialValue the initial value
	public void setInitialValue(final float initialValue) {
		this.initialValue = initialValue;
	}

	/// Sets the maximum output axis value.
	///
	/// @param maxValue the maximum value
	public void setMaxValue(final float maxValue) {
		this.maxValue = maxValue;
	}

	/// Sets the minimum output value for the axis mapping.
	///
	/// @param minValue the minimum value
	public void setMinValue(final float minValue) {
		this.minValue = minValue;
	}
}

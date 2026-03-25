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

/// Triggers a keystroke when an axis value falls within a configurable range.
///
/// Extends [ToKeyAction] with axis-specific min/max threshold support and delay
/// handling.
@Action(icon = "⌨️", title = "TO_KEY_ACTION_TITLE", description = "TO_KEY_ACTION_DESCRIPTION", category = ActionCategory.AXIS_AND_TRIGGER, order = 40)
public final class AxisToKeyAction extends ToKeyAction<Float> implements IAxisToDelayableAction {

	/// Default upper bound of the axis activation range.
	private static final float DEFAULT_MAX_AXIS_VALUE = 1f;

	/// Default lower bound of the axis activation range.
	private static final float DEFAULT_MIN_AXIS_VALUE = 0.5f;

	/// Upper bound of the axis value range that triggers the key action.
	@ActionProperty(icon = "≤", title = "MAX_AXIS_VALUE_TITLE", description = "MAX_AXIS_VALUE_DESCRIPTION", editorBuilder = AxisValueEditorBuilder.class, order = 101)
	private float maxAxisValue = DEFAULT_MAX_AXIS_VALUE;

	/// Lower bound of the axis value range that triggers the key action.
	@ActionProperty(icon = "≥", title = "MIN_AXIS_VALUE_TITLE", description = "MIN_AXIS_VALUE_DESCRIPTION", editorBuilder = AxisValueEditorBuilder.class, order = 100)
	private float minAxisValue = DEFAULT_MIN_AXIS_VALUE;

	/// Executes the key action if the axis value is within the configured min/max
	/// range.
	///
	/// @param input the current input state
	/// @param component the axis component index
	/// @param value the current axis value
	@Override
	public void doAction(final Input input, final int component, Float value) {
		value = handleDelay(input, component, value);

		final var inZone = !input.isAxisSuspended(component) && value >= minAxisValue && value <= maxAxisValue;
		handleAction(inZone, input);
	}

	/// Returns the maximum axis value for the activation zone.
	///
	/// @return the upper bound of the axis activation range
	@Override
	public float getMaxAxisValue() {
		return maxAxisValue;
	}

	/// Returns the minimum axis value for the activation zone.
	///
	/// @return the lower bound of the axis activation range
	@Override
	public float getMinAxisValue() {
		return minAxisValue;
	}

	/// Sets the maximum axis value for the activation zone.
	///
	/// @param maxAxisValue the upper bound of the axis activation range
	@Override
	public void setMaxAxisValue(final float maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	/// Sets the minimum axis value for the activation zone.
	///
	/// @param minAxisValue the lower bound of the axis activation range
	@Override
	public void setMinAxisValue(final float minAxisValue) {
		this.minAxisValue = minAxisValue;
	}
}

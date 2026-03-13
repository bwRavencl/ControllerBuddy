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

/// Maps axis input to a virtual button output.
///
/// The button is considered pressed when the axis value falls within a
/// configurable range defined by [#minAxisValue] and [#maxAxisValue].
@Action(title = "TO_BUTTON_ACTION_TITLE", description = "TO_BUTTON_ACTION_DESCRIPTION", category = ActionCategory.AXIS_AND_TRIGGER, order = 20)
public final class AxisToButtonAction extends ToButtonAction<Float> implements IAxisToDelayableAction {

	/// Default upper bound of the activation axis range.
	private static final float DEFAULT_MAX_AXIS_VALUE = 1f;

	/// Default lower bound of the activation axis range.
	private static final float DEFAULT_MIN_AXIS_VALUE = 0.5f;

	/// The upper bound of the axis range that activates the virtual button.
	@ActionProperty(title = "MAX_AXIS_VALUE_TITLE", description = "MAX_AXIS_VALUE_DESCRIPTION", editorBuilder = AxisValueEditorBuilder.class, order = 101)
	private float maxAxisValue = DEFAULT_MAX_AXIS_VALUE;

	/// The lower bound of the axis range that activates the virtual button.
	@ActionProperty(title = "MIN_AXIS_VALUE_TITLE", description = "MIN_AXIS_VALUE_DESCRIPTION", editorBuilder = AxisValueEditorBuilder.class, order = 100)
	private float minAxisValue = DEFAULT_MIN_AXIS_VALUE;

	/// Processes the axis value and activates the virtual button if the value is
	/// within the configured axis range, respecting delay and axis suspension
	/// state.
	@Override
	public void doAction(final Input input, final int component, Float value) {
		value = handleDelay(input, component, value);

		final var inZone = !input.isAxisSuspended(component) && value >= minAxisValue && value <= maxAxisValue;
		handleAction(inZone, input);
	}

	/// Returns the upper bound of the activation axis range.
	@Override
	public float getMaxAxisValue() {
		return maxAxisValue;
	}

	/// Returns the lower bound of the activation axis range.
	@Override
	public float getMinAxisValue() {
		return minAxisValue;
	}

	/// Sets the upper bound of the activation axis range.
	@Override
	public void setMaxAxisValue(final float maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	/// Sets the lower bound of the activation axis range.
	@Override
	public void setMinAxisValue(final float minAxisValue) {
		this.minAxisValue = minAxisValue;
	}
}

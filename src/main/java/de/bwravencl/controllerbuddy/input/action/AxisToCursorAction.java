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
import de.bwravencl.controllerbuddy.input.action.gui.DeadZoneEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ExponentEditorBuilder;

/// Maps axis input to cursor movement.
///
/// The axis value is transformed using a configurable dead zone and exponent
/// curve, then applied as a cursor delta scaled by sensitivity and rate.
@Action(icon = "➚", title = "TO_CURSOR_ACTION_TITLE", description = "TO_CURSOR_ACTION_DESCRIPTION", category = ActionCategory.AXIS, order = 25)
public final class AxisToCursorAction extends ToCursorAction<Float> implements IAxisToAction {

	/// Default dead zone threshold below which axis input is ignored.
	private static final float DEFAULT_DEAD_ZONE = 0.1f;

	/// Default exponent applied to the input response curve.
	private static final float DEFAULT_EXPONENT = 2f;

	/// Dead zone threshold below which axis input is ignored.
	@ActionProperty(icon = "🚫", title = "DEAD_ZONE_TITLE", description = "DEAD_ZONE_DESCRIPTION", editorBuilder = DeadZoneEditorBuilder.class, order = 13)
	private float deadZone = DEFAULT_DEAD_ZONE;

	/// Exponent applied to the input response curve.
	@ActionProperty(icon = "📈", title = "EXPONENT_TITLE", description = "EXPONENT_DESCRIPTION", editorBuilder = ExponentEditorBuilder.class, order = 12)
	private float exponent = DEFAULT_EXPONENT;

	/// Processes the axis value and moves the cursor proportionally, applying dead
	/// zone filtering and exponential response curve. Resets the remaining delta
	/// when the axis is within the dead zone.
	@Override
	public void doAction(final Input input, final int component, final Float value) {
		final var absValue = Math.abs(value);

		if (!input.isAxisSuspended(component) && absValue > deadZone) {
			final var inMax = (float) Math.pow((1f - deadZone) * 100f, exponent);

			final var d = Input.normalize(Math.signum(value) * (float) Math.pow((absValue - deadZone) * 100f, exponent),
					-inMax, inMax, -cursorSensitivity, cursorSensitivity) * input.getRateMultiplier();
			moveCursor(input, d);
		} else {
			remainingD = 0f;
		}
	}

	/// Returns the dead zone threshold below which input is ignored.
	///
	/// @return the dead zone value
	public float getDeadZone() {
		return deadZone;
	}

	/// Returns the exponent applied to the input curve.
	///
	/// @return the exponent value
	public float getExponent() {
		return exponent;
	}

	/// Sets the dead zone threshold below which input is ignored.
	///
	/// @param deadZone the dead zone value
	public void setDeadZone(final float deadZone) {
		this.deadZone = deadZone;
	}

	/// Sets the exponent applied to the input curve.
	///
	/// @param exponent the exponent value
	public void setExponent(final float exponent) {
		this.exponent = exponent;
	}
}

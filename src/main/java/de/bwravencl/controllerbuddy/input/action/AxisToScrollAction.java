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

/// Maps a controller axis to mouse scroll wheel input.
///
/// Applies a dead zone and exponent curve to the axis value before converting
/// it to scroll clicks.
@Action(title = "TO_SCROLL_ACTION_TITLE", description = "TO_SCROLL_ACTION_DESCRIPTION", category = ActionCategory.AXIS_AND_TRIGGER, order = 35)
public final class AxisToScrollAction extends ToScrollAction<Float> implements IAxisToAction {

	/// Default dead zone threshold below which axis input is ignored.
	private static final float DEFAULT_DEAD_ZONE = 0.1f;

	/// Default exponent applied to the axis value curve.
	private static final float DEFAULT_EXPONENT = 1f;

	/// Dead zone threshold below which axis input is ignored.
	@ActionProperty(title = "DEAD_ZONE_TITLE", description = "DEAD_ZONE_DESCRIPTION", editorBuilder = DeadZoneEditorBuilder.class, order = 100)
	private float deadZone = DEFAULT_DEAD_ZONE;

	/// Exponent applied to the axis value curve.
	@ActionProperty(title = "EXPONENT_TITLE", description = "EXPONENT_DESCRIPTION", editorBuilder = ExponentEditorBuilder.class, order = 101)
	private float exponent = DEFAULT_EXPONENT;

	/// Converts the axis value to scroll clicks, applying dead zone filtering and
	/// an exponent curve. Resets the scroll remainder when the axis is in the dead
	/// zone or suspended.
	///
	/// @param input the current input state
	/// @param component the axis component index
	/// @param value the current axis value
	@Override
	public void doAction(final Input input, final int component, final Float value) {
		if (!input.isAxisSuspended(component) && Math.abs(value) > deadZone) {
			final var rateMultiplier = input.getRateMultiplier();

			final var d = -Input.normalize(
					Math.signum(value) * (float) Math.pow(Math.abs(value) * 100f, exponent) * rateMultiplier,
					(float) -Math.pow(99.9f, exponent) * rateMultiplier,
					(float) Math.pow(99.9f, exponent) * rateMultiplier, -clicks, clicks);

			scroll(input, d);
		} else {
			remainingD = 0f;
		}
	}

	/// Returns the dead zone threshold below which axis input is ignored.
	///
	/// @return the dead zone value
	public float getDeadZone() {
		return deadZone;
	}

	/// Returns the exponent applied to the axis value curve.
	///
	/// @return the exponent value
	public float getExponent() {
		return exponent;
	}

	/// Sets the dead zone threshold below which axis input is ignored.
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

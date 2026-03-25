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
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DetentValueEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MaxRelativeSpeedEditorBuilder;

/// Maps a physical axis to a virtual axis using relative movement.
///
/// The axis value controls the speed and direction of change rather than
/// setting an absolute position, with support for dead zones, exponent curves,
/// detent values, and haptic feedback.
@Action(icon = "🎚️", title = "AXIS_TO_RELATIVE_AXIS_ACTION_TITLE", description = "AXIS_TO_RELATIVE_AXIS_ACTION_DESCRIPTION", category = ActionCategory.AXIS, order = 15)
public final class AxisToRelativeAxisAction extends AxisToAxisAction {

	/// Default haptic feedback enabled state.
	private static final boolean DEFAULT_HAPTIC_FEEDBACK = false;

	/// Default maximum relative speed for axis movement.
	private static final float DEFAULT_MAX_RELATIVE_SPEED = 4f;

	/// Accumulated sub-unit movement remainder carried over between frames.
	transient float remainingD;

	/// Optional snap-to value for the virtual axis; `null` disables detent.
	@ActionProperty(icon = "⬹", title = "DETENT_VALUE_TITLE", description = "DETENT_VALUE_DESCRIPTION", editorBuilder = DetentValueEditorBuilder.class, order = 203)
	private Float detentValue = null;

	/// Whether haptic feedback is triggered on axis movement.
	@ActionProperty(icon = "📳", title = "HAPTIC_FEEDBACK_TITLE", description = "HAPTIC_FEEDBACK_DESCRIPTION", editorBuilder = BooleanEditorBuilder.class, order = 204)
	private boolean hapticFeedback = DEFAULT_HAPTIC_FEEDBACK;

	/// Maximum relative speed applied to axis movement per polling cycle.
	@ActionProperty(icon = "⚡", title = "MAX_RELATIVE_SPEED_TITLE", description = "MAX_RELATIVE_SPEED_DESCRIPTION", editorBuilder = MaxRelativeSpeedEditorBuilder.class, order = 201)
	private float maxRelativeSpeed = DEFAULT_MAX_RELATIVE_SPEED;

	/// Applies relative axis movement based on the current axis value, dead zone,
	/// exponent curve, and maximum relative speed. Accumulates subunit remainders
	/// across calls.
	///
	/// @param input the current input state
	/// @param component the axis component index
	/// @param value the current axis value
	@Override
	public void doAction(final Input input, final int component, final Float value) {
		final var absValue = Math.abs(value);

		if (input.isAxisSuspended(component) || absValue <= deadZone) {
			return;
		}

		final var inMax = (float) Math.pow((1f - deadZone) * 100f, exponent);

		var d = Input.normalize(Math.signum(value) * (float) Math.pow((absValue - deadZone) * 100f, exponent), -inMax,
				inMax, -maxRelativeSpeed, maxRelativeSpeed) * input.getRateMultiplier();
		d += remainingD;

		if (Math.abs(d) < input.getPlanckLength()) {
			remainingD = d;
			return;
		}

		final var runMode = input.getRunMode();
		final var oldValue = Input.normalize(input.getAxes().get(virtualAxis), runMode.getMinAxisValue(),
				runMode.getMaxAxisValue(), -1f, 1f);

		final var newValue = Math.clamp(oldValue + (invert ? -d : d), minValue, maxValue);
		input.setAxis(virtualAxis, newValue, hapticFeedback, minValue, maxValue, detentValue);

		remainingD = 0f;
	}

	/// Returns the optional detent (snap) value for the virtual axis.
	///
	/// @return the detent value, or `null` if no detent is configured
	public Float getDetentValue() {
		return detentValue;
	}

	/// Returns the maximum relative speed for axis movement.
	///
	/// @return the maximum relative speed
	public float getMaxRelativeSpeed() {
		return maxRelativeSpeed;
	}

	/// Returns whether haptic feedback is enabled for this action.
	///
	/// @return `true` if haptic feedback is enabled
	public boolean isHapticFeedback() {
		return hapticFeedback;
	}

	/// Sets the optional detent (snap) value for the virtual axis.
	///
	/// @param detentValue the detent value, or `null` to disable
	public void setDetentValue(final Float detentValue) {
		this.detentValue = detentValue;
	}

	/// Sets whether haptic feedback is enabled for this action.
	///
	/// @param hapticFeedback `true` to enable haptic feedback
	public void setHapticFeedback(final boolean hapticFeedback) {
		this.hapticFeedback = hapticFeedback;
	}

	/// Sets the maximum relative speed for axis movement.
	///
	/// @param maxRelativeSpeed the maximum relative speed
	public void setMaxRelativeSpeed(final float maxRelativeSpeed) {
		this.maxRelativeSpeed = maxRelativeSpeed;
	}
}

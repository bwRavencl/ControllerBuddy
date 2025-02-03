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
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DetentValueEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MaxRelativeSpeedEditorBuilder;

@Action(label = "AXIS_TO_RELATIVE_AXIS_ACTION", category = ActionCategory.AXIS, order = 15)
public final class AxisToRelativeAxisAction extends AxisToAxisAction {

	private static final float DEFAULT_MAX_RELATIVE_SPEED = 4f;
	private static final boolean DEFAULT_HAPTIC_FEEDBACK = false;
	transient float remainingD;

	@ActionProperty(label = "MAX_RELATIVE_SPEED", editorBuilder = MaxRelativeSpeedEditorBuilder.class, order = 201)
	private float maxRelativeSpeed = DEFAULT_MAX_RELATIVE_SPEED;

	@ActionProperty(label = "HAPTIC_FEEDBACK", editorBuilder = BooleanEditorBuilder.class, order = 204)
	private boolean hapticFeedback = DEFAULT_HAPTIC_FEEDBACK;

	@ActionProperty(label = "DETENT_VALUE", editorBuilder = DetentValueEditorBuilder.class, order = 203)
	private Float detentValue = null;

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

		final var newValue = Math.min(Math.max(oldValue + (invert ? -d : d), minValue), maxValue);
		input.setAxis(virtualAxis, newValue, hapticFeedback, detentValue);

		remainingD = 0f;
	}

	public Float getDetentValue() {
		return detentValue;
	}

	public float getMaxRelativeSpeed() {
		return maxRelativeSpeed;
	}

	public boolean isHapticFeedback() {
		return hapticFeedback;
	}

	public void setDetentValue(final Float detentValue) {
		this.detentValue = detentValue;
	}

	public void setHapticFeedback(final boolean hapticFeedback) {
		this.hapticFeedback = hapticFeedback;
	}

	public void setMaxRelativeSpeed(final float maxRelativeSpeed) {
		this.maxRelativeSpeed = maxRelativeSpeed;
	}
}

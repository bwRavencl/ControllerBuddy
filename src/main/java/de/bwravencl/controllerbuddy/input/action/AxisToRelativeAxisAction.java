/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DetentValueEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ExponentEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MaxRelativeSpeedEditorBuilder;

@Action(label = "AXIS_TO_RELATIVE_AXIS_ACTION", category = ActionCategory.AXIS, order = 15)
public final class AxisToRelativeAxisAction extends AxisToAxisAction implements IModeChangeListenerAction {

	private static final float DEFAULT_EXPONENT = 2f;
	private static final float DEFAULT_MAX_RELATIVE_SPEED = 4f;
	private static final boolean DEFAULT_HAPTIC_FEEDBACK = false;

	@ActionProperty(label = "EXPONENT", editorBuilder = ExponentEditorBuilder.class, order = 200)
	private float exponent = DEFAULT_EXPONENT;

	@ActionProperty(label = "MAX_RELATIVE_SPEED", editorBuilder = MaxRelativeSpeedEditorBuilder.class, order = 201)
	private float maxRelativeSpeed = DEFAULT_MAX_RELATIVE_SPEED;

	@ActionProperty(label = "HAPTIC_FEEDBACK", editorBuilder = BooleanEditorBuilder.class, order = 204)
	private boolean hapticFeedback = DEFAULT_HAPTIC_FEEDBACK;

	@ActionProperty(label = "DETENT_VALUE", editorBuilder = DetentValueEditorBuilder.class, order = 203)
	private Float detentValue = null;

	private transient long lastCallTime = 0L;

	@Override
	public void doAction(final Input input, final Float value) {
		final var currentTime = System.currentTimeMillis();
		var elapsedTime = input.getOutputThread().getPollInterval();

		if (lastCallTime > 0L)
			elapsedTime = currentTime - lastCallTime;
		lastCallTime = currentTime;

		if (!isSuspended() && Math.abs(value) > deadZone) {
			final var rateMultiplier = (float) elapsedTime / (float) 1000L;

			final var d = Input.normalize(Math.signum(value) * (float) Math.pow(Math.abs(value) * 100f, exponent),
					(float) -Math.pow(100f, exponent), (float) Math.pow(100f, exponent), -maxRelativeSpeed,
					maxRelativeSpeed) * rateMultiplier;

			final var oldValue = Input.normalize(input.getAxes().get(virtualAxis),
					input.getOutputThread().getMinAxisValue(), input.getOutputThread().getMaxAxisValue(), -1f, 1f);

			input.setAxis(virtualAxis, oldValue + (invert ? -d : d), hapticFeedback, detentValue);
		} else
			lastCallTime = 0L;
	}

	public Float getDetentValue() {
		return detentValue;
	}

	public float getExponent() {
		return exponent;
	}

	public float getMaxRelativeSpeed() {
		return maxRelativeSpeed;
	}

	public boolean isHapticFeedback() {
		return hapticFeedback;
	}

	@Override
	public void onModeChanged(final Mode newMode) {
		for (final var actions : newMode.getAxisToActionsMap().values())
			if (actions.contains(this)) {
				lastCallTime = 0L;
				break;
			}
	}

	public void setDetentValue(final Float detentValue) {
		this.detentValue = detentValue;
	}

	public void setExponent(final float exponent) {
		this.exponent = exponent;
	}

	public void setHapticFeedback(final boolean hapticFeedback) {
		this.hapticFeedback = hapticFeedback;
	}

	public void setMaxRelativeSpeed(final float maxRelativeSpeed) {
		this.maxRelativeSpeed = maxRelativeSpeed;
	}
}

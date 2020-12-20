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

import static de.bwravencl.controllerbuddy.input.Input.normalize;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.signum;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DetentValueEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ExponentEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MaxRelativeSpeedEditorBuilder;

@Action(label = "AXIS_TO_RELATIVE_AXIS_ACTION", category = ActionCategory.AXIS, order = 15)
public final class AxisToRelativeAxisAction extends AxisToAxisAction {

	private static final float DEFAULT_EXPONENT = 2f;
	private static final float DEFAULT_MAX_RELATIVE_SPEED = 4f;
	private static final boolean DEFAULT_HAPTIC_FEEDBACK = false;

	// private static final float MIN_STEP = 1e-4f;

	@ActionProperty(label = "EXPONENT", editorBuilder = ExponentEditorBuilder.class, order = 200)
	private float exponent = DEFAULT_EXPONENT;

	@ActionProperty(label = "MAX_RELATIVE_SPEED", editorBuilder = MaxRelativeSpeedEditorBuilder.class, order = 201)
	private float maxRelativeSpeed = DEFAULT_MAX_RELATIVE_SPEED;

	@ActionProperty(label = "HAPTIC_FEEDBACK", editorBuilder = BooleanEditorBuilder.class, order = 204)
	private boolean hapticFeedback = DEFAULT_HAPTIC_FEEDBACK;

	@ActionProperty(label = "DETENT_VALUE", editorBuilder = DetentValueEditorBuilder.class, order = 203)
	private Float detentValue = null;

	transient float remainingD;

	@Override
	public void doAction(final Input input, final int component, final Float value) {
		final var absValue = abs(value);

		if (!input.isAxisSuspended(component) && absValue > deadZone) {
			final var inMax = (float) pow((1f - deadZone) * 100f, exponent);

			var d = normalize(signum(value) * (float) pow((absValue - deadZone) * 100f, exponent), -inMax, inMax,
					-maxRelativeSpeed, maxRelativeSpeed) * input.getRateMultiplier();

			d += remainingD;

			if (abs(d) < input.getPlanckLength())
				remainingD = d;
			else {
				remainingD = 0f;

				final var output = input.getOutput();
				final var oldValue = normalize(input.getAxes().get(virtualAxis), output.getMinAxisValue(),
						output.getMaxAxisValue(), -1f, 1f);

				input.setAxis(virtualAxis, oldValue + (invert ? -d : d), hapticFeedback, detentValue);
			}
		}
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

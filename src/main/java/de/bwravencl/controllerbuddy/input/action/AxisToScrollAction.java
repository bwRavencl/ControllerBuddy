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
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.DeadZoneEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ExponentEditorBuilder;

@Action(label = "TO_SCROLL_ACTION", category = ActionCategory.AXIS, order = 35)
public final class AxisToScrollAction extends ToScrollAction<Float> implements ISuspendableAction {

	private static final float DEFAULT_DEAD_ZONE = 0.15f;
	private static final float DEFAULT_EXPONENT = 1f;

	@ActionProperty(label = "DEAD_ZONE", editorBuilder = DeadZoneEditorBuilder.class, order = 100)
	private float deadZone = DEFAULT_DEAD_ZONE;

	@ActionProperty(label = "EXPONENT", editorBuilder = ExponentEditorBuilder.class, order = 101)
	private float exponent = DEFAULT_EXPONENT;

	@Override
	public void doAction(final Input input, final Float value) {
		if (!isSuspended() && Math.abs(value) > deadZone) {
			final var rateMultiplier = input.getRateMultiplier();

			final var d = -Input.normalize(
					Math.signum(value) * (float) Math.pow(Math.abs(value) * 100f, exponent) * rateMultiplier,
					(float) -Math.pow(99.9f, exponent) * rateMultiplier,
					(float) Math.pow(99.9f, exponent) * rateMultiplier, -clicks, clicks);

			input.setScrollClicks((int) (input.getScrollClicks() + (invert ? -d : d)));
		}
	}

	public float getDeadZone() {
		return deadZone;
	}

	public float getExponent() {
		return exponent;
	}

	public void setDeadZone(final float deadZone) {
		this.deadZone = deadZone;
	}

	public void setExponent(final float exponent) {
		this.exponent = exponent;
	}
}

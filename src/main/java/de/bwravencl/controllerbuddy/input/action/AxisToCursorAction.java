/* Copyright (C) 2016  Matteo Hausner
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

public class AxisToCursorAction extends InvertableAction implements ISuspendableAction {

	public enum MouseAxis {
		X, Y
	}

	public static final float DEFAULT_DEAD_ZONE = 0.15f;
	public static final float DEFAULT_EXPONENT = 2.0f;
	public static final float DEFAULT_MAX_CURSOR_SPEED = 2000.0f;

	private float deadZone = DEFAULT_DEAD_ZONE;
	private float exponent = DEFAULT_EXPONENT;
	private float maxCursorSpeed = DEFAULT_MAX_CURSOR_SPEED;
	private MouseAxis axis = MouseAxis.X;
	private transient long lastCallTime;

	@Override
	public void doAction(final Input input, final float value) {
		final long currentTime = System.currentTimeMillis();
		long elapsedTime = input.getOutputThread().getPollInterval();
		if (lastCallTime > 0L)
			elapsedTime = currentTime - lastCallTime;
		lastCallTime = currentTime;

		if (!isSuspended() && Math.abs(value) > deadZone) {
			final float rateMultiplier = (float) elapsedTime / (float) 1000L;

			final float d = Input.normalize(Math.signum(value) * (float) Math.pow(Math.abs(value) * 100.0f, exponent),
					(float) -Math.pow(100.0f, exponent), (float) Math.pow(100.0f, exponent), -maxCursorSpeed,
					maxCursorSpeed) * rateMultiplier;

			if (axis.equals(MouseAxis.X))
				input.setCursorDeltaX((int) (input.getCursorDeltaX() + (invert ? -d : d)));
			else
				input.setCursorDeltaY((int) (input.getCursorDeltaY() + (invert ? -d : d)));
		}
	}

	public MouseAxis getAxis() {
		return axis;
	}

	public float getDeadZone() {
		return deadZone;
	}

	public float getExponent() {
		return exponent;
	}

	public float getMaxCursorSpeed() {
		return maxCursorSpeed;
	}

	public void setAxis(final MouseAxis axis) {
		this.axis = axis;
	}

	public void setDeadZone(final Float deadZone) {
		this.deadZone = deadZone;
	}

	public void setExponent(final Float exponent) {
		this.exponent = exponent;
	}

	public void setMaxCursorSpeed(final Float maxCursorSpeed) {
		this.maxCursorSpeed = maxCursorSpeed;
	}

	@Override
	public String toString() {
		return rb.getString("AXIS_TO_CURSOR_ACTION_STRING");
	}

}

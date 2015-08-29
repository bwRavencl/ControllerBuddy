/* Copyright (C) 2015  Matteo Hausner
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

package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToCursorAction extends InvertableAction {

	public final float DEFAULT_DEAD_ZONE = 0.25f;
	public final float DEFAULT_MAX_SPEED = 750.0f;

	private float deadZone = DEFAULT_DEAD_ZONE;
	private float maxSpeed = DEFAULT_MAX_SPEED;

	public enum MouseAxis {
		X, Y
	}

	private MouseAxis axis = MouseAxis.X;

	public float getDeadZone() {
		return deadZone;
	}

	public void setDeadZone(Float deadZone) {
		this.deadZone = deadZone;
	}

	public float getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(Float maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public MouseAxis getAxis() {
		return axis;
	}

	public void setAxis(MouseAxis axis) {
		this.axis = axis;
	}

	@Override
	public void doAction(Input input, float value) {
		if (Math.abs(value) > deadZone) {
			final float rateMultiplier = (float) input.getOutputThread().getUpdateRate() / (float) 1000L;

			float d = Input.normalize(value, -1.0f, 1.0f, -maxSpeed, maxSpeed) * rateMultiplier;

			if (axis.equals(MouseAxis.X))
				input.setCursorDeltaX((int) (input.getCursorDeltaX() + (invert ? -d : d)));
			else
				input.setCursorDeltaY((int) (input.getCursorDeltaY() + (invert ? -d : d)));
		}
	}

	@Override
	public String toString() {
		return rb.getString("AXIS_TO_CURSOR_ACTION_STRING");
	}

}

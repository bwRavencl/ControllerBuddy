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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;

public class AxisToRelativeAxisAction extends AxisToAxisAction {

	public static final float DEFAULT_DEAD_ZONE = 0.15f;
	public static final float DEFAULT_EXPONENT = 2.0f;
	public static final float DEFAULT_MAX_RELATIVE_SPEED = 6.0f;

	private float deadZone = DEFAULT_DEAD_ZONE;
	private float exponent = DEFAULT_EXPONENT;
	private float maxRelativeSpeed = DEFAULT_MAX_RELATIVE_SPEED;

	public float getDeadZone() {
		return deadZone;
	}

	public void setDeadZone(Float deadZone) {
		this.deadZone = deadZone;
	}

	public float getExponent() {
		return exponent;
	}

	public void setExponent(Float exponent) {
		this.exponent = exponent;
	}

	/*
	 * public float getSensitivity() { return sensitivity; }
	 * 
	 * public void setSensitivity(Float sensitivity) { this.sensitivity =
	 * sensitivity; }
	 */

	public float getMaxRelativeSpeed() {
		return maxRelativeSpeed;
	}

	public void setMaxRelativeSpeed(Float maxRelativeSpeed) {
		this.maxRelativeSpeed = maxRelativeSpeed;
	}

	@Override
	public void doAction(Input input, float value) {
		if (Math.abs(value) > deadZone) {
			/*
			 * final float d = (Math.signum(value) * (float)
			 * Math.pow(Math.abs(value * 100.0f), exponent) / 100.0f)
			 * sensitivity * (float) input.getOutputThread().getUpdateRate() /
			 * (float) 1000L;
			 */

			final float rateMultiplier = (float) input.getOutputThread().getUpdateRate() / (float) 1000L;

			final float d = Input.normalize(Math.signum(value) * (float) Math.pow(Math.abs(value) * 100.0f, exponent),
					(float) -Math.pow(100.0f, exponent), (float) Math.pow(100.0f, exponent), -maxRelativeSpeed,
					maxRelativeSpeed) * rateMultiplier;

			final float oldValue = Input.normalize(input.getAxis().get(virtualAxis),
					input.getOutputThread().getMinAxisValue(), input.getOutputThread().getMaxAxisValue(), -1.0f, 1.0f);

			input.setAxis(virtualAxis, oldValue + (invert ? -d : d));
		}
	}

	@Override
	public String toString() {
		return rb.getString("AXIS_TO_RELATIVE_AXIS_ACTION_STRING");
	}

}

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

public class AxisToRelativeAxisAction extends AxisToAxisAction {

	public final float DEFAULT_DEAD_ZONE = 0.25f;
	public final float DEFAULT_SENSITIVITY = 1.0f;

	private float deadZone = DEFAULT_DEAD_ZONE;
	private float sensitivity = DEFAULT_SENSITIVITY;

	public float getDeadZone() {
		return deadZone;
	}

	public void setDeadZone(Float deadZone) {
		this.deadZone = deadZone;
	}

	public float getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(Float sensitivity) {
		this.sensitivity = sensitivity;
	}

	@Override
	public void doAction(Input joystick, float value) {
		if (Math.abs(value) > deadZone) {
			final float d = value * sensitivity * (float) joystick.getOutputThread().getUpdateRate() / (float) 1000L;

			final float oldValue = Input.normalize(joystick.getAxis().get(virtualAxis),
					joystick.getOutputThread().getMinAxisValue(), joystick.getOutputThread().getMaxAxisValue(), -1.0f,
					1.0f);

			joystick.setAxis(virtualAxis, oldValue + (invert ? -d : d));
		}
	}

	@Override
	public String toString() {
		return rb.getString("AXIS_TO_RELATIVE_AXIS_ACTION_STRING");
	}

}

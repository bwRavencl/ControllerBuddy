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

public class AxisToButtonAction extends ToButtonAction implements ISuspendableAction {

	public static final float DEFAULT_MIN_AXIS_VALUE = 0.5f;
	public static final float DEFAULT_MAX_AXIS_VALUE = 1.0f;

	private float minAxisValue = 0.5f;
	private float maxAxisValue = 1.0f;

	@Override
	public void doAction(final Input input, final float value) {
		final boolean down = !isSuspended() && value >= minAxisValue && value <= maxAxisValue;

		input.setButtons(buttonId, down);
	}

	public float getMaxAxisValue() {
		return maxAxisValue;
	}

	public float getMinAxisValue() {
		return minAxisValue;
	}

	public void setMaxAxisValue(final Float maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	public void setMinAxisValue(final Float minAxisValue) {
		this.minAxisValue = minAxisValue;
	}

}

package de.bwravencl.controllerbuddy.input.action;
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

import de.bwravencl.controllerbuddy.input.Input;

public class ButtonToRelativeAxisReset extends ToAxisAction implements IButtonToAction {

	private static final float DEFAULT_RESET_VALUE = 0.0f;

	private float resetValue = DEFAULT_RESET_VALUE;
	private boolean longPress = DEFAULT_LONG_PRESS;
	private float activationValue = DEFAULT_ACTIVATION_VALUE;

	@Override
	public void doAction(Input input, float value) {
		value = handleLongPress(value);

		if (value == activationValue ^ invert)
			input.setAxis(virtualAxis, resetValue);
	}

	public float getActivationValue() {
		return activationValue;
	}

	public float getResetValue() {
		return resetValue;
	}

	public void setActivationValue(Float activationValue) {
		this.activationValue = activationValue;
	}

	public void setResetValue(Float resetValue) {
		this.resetValue = resetValue;
	}

	public boolean isLongPress() {
		return longPress;
	}

	public void setLongPress(Boolean longPress) {
		this.longPress = longPress;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_RELATIVE_AXIS_RESET_ACTION_STRING");
	}

}

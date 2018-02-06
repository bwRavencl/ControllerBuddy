/* Copyright (C) 2018  Matteo Hausner
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

public class ButtonToRelativeAxisReset extends ToAxisAction implements IButtonToAction {

	private static final float DEFAULT_RESET_VALUE = 0.0f;

	private float resetValue = DEFAULT_RESET_VALUE;
	private boolean longPress = DEFAULT_LONG_PRESS;
	private float activationValue = DEFAULT_ACTIVATION_VALUE;

	@Override
	public void doAction(final Input input, float value) {
		value = handleLongPress(input, value);

		if (IButtonToAction.floatEquals(value, activationValue) ^ invert)
			input.setAxis(virtualAxis, resetValue);
	}

	@Override
	public float getActivationValue() {
		return activationValue;
	}

	public float getResetValue() {
		return resetValue;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void setActivationValue(final Float activationValue) {
		this.activationValue = activationValue;
	}

	@Override
	public void setLongPress(final Boolean longPress) {
		this.longPress = longPress;
	}

	public void setResetValue(final Float resetValue) {
		this.resetValue = resetValue;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_RELATIVE_AXIS_RESET_ACTION_STRING");
	}

}

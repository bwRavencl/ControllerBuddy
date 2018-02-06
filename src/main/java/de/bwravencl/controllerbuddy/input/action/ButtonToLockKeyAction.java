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
import de.bwravencl.controllerbuddy.input.LockKey;

public class ButtonToLockKeyAction implements IButtonToAction {

	private float activationValue = DEFAULT_ACTIVATION_VALUE;
	private boolean longPress = DEFAULT_LONG_PRESS;
	private int virtualKeyCode = LockKey.LOCK_KEYS[0].virtualKeyCode;
	private boolean on = true;
	private transient boolean wasUp = true;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public void doAction(final Input input, float value) {
		value = handleLongPress(input, value);

		if (IButtonToAction.floatEquals(value, activationValue)) {
			if (wasUp) {
				wasUp = false;
				if (on)
					input.getOnLockKeys().add(virtualKeyCode);
				else
					input.getOffLockKeys().add(virtualKeyCode);
			}
		} else
			wasUp = true;
	}

	@Override
	public float getActivationValue() {
		return activationValue;
	}

	public LockKey getLockKey() {
		return LockKey.virtualKeyCodeToLockKeyMap.get(virtualKeyCode);
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	public boolean isOn() {
		return on;
	}

	@Override
	public void setActivationValue(final Float activationValue) {
		this.activationValue = activationValue;
	}

	public void setLockKey(final LockKey lockKey) {
		virtualKeyCode = LockKey.lockKeyToVirtualKeyCodeMap.get(lockKey);
	}

	@Override
	public void setLongPress(final Boolean longPress) {
		this.longPress = longPress;
	}

	public void setOn(final Boolean on) {
		this.on = on;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_LOCK_KEY_ACTION_STRING");
	}

}

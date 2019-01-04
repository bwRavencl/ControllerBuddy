/* Copyright (C) 2019  Matteo Hausner
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

public class ButtonToPressOnScreenKeyboardKeyAction implements IButtonToAction {

	private boolean lockKey = false;

	private boolean longPress = DEFAULT_LONG_PRESS;

	private transient boolean wasUp = true;

	private transient boolean wasDown = false;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public void doAction(final Input input, Byte value) {
		value = handleLongPress(input, value);

		final var onScreenKeyboard = input.getMain().getOnScreenKeyboard();

		if (value == 0) {
			if (lockKey)
				wasUp = true;
			else if (wasDown) {
				onScreenKeyboard.releaseSelected();
				wasDown = false;
			} else
				wasDown = false;
		} else if (lockKey) {
			if (wasUp) {
				onScreenKeyboard.toggleLock();
				wasUp = false;
			}
		} else {
			onScreenKeyboard.pressSelected();
			wasDown = true;
		}
	}

	public boolean isLockKey() {
		return lockKey;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	public void setLockKey(final boolean lockKey) {
		this.lockKey = lockKey;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_PRESS_ON_SCREEN_KEYBOARD_KEY_ACTION_STRING");
	}

}

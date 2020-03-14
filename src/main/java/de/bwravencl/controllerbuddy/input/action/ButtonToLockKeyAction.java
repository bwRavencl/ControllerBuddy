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
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LockKeyEditorBuilder;

@Action(label = "BUTTON_TO_LOCK_KEY_ACTION", category = ActionCategory.BUTTON, order = 116)
public final class ButtonToLockKeyAction implements IButtonToAction {

	private boolean longPress = DEFAULT_LONG_PRESS;

	@ActionProperty(label = "VIRTUAL_KEY_CODE", editorBuilder = LockKeyEditorBuilder.class, overrideFieldName = "lockKey", overrideFieldType = LockKey.class, order = 10)
	private int virtualKeyCode = LockKey.LOCK_KEYS[0].virtualKeyCode;

	@ActionProperty(label = "ON", editorBuilder = BooleanEditorBuilder.class, order = 11)
	private boolean on = true;

	private transient boolean wasUp = true;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public void doAction(final Input input, Byte value) {
		value = handleLongPress(input, value);

		if (value != 0) {
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

	public void setLockKey(final LockKey lockKey) {
		virtualKeyCode = LockKey.lockKeyToVirtualKeyCodeMap.get(lockKey);
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}

	public void setOn(final boolean on) {
		this.on = on;
	}
}

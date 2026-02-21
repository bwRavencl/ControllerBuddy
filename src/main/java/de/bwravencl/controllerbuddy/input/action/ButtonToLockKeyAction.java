/* Copyright (C) 2016  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LockKeyEditorBuilder;
import java.text.MessageFormat;

@Action(title = "BUTTON_TO_LOCK_KEY_ACTION_TITLE", description = "BUTTON_TO_LOCK_KEY_ACTION_DESCRIPTION", category = ActionCategory.BUTTON_AND_CYCLES, order = 116)
public final class ButtonToLockKeyAction extends DescribableAction<Boolean>
		implements IButtonToDelayableAction, IInitializationAction<Boolean> {

	private long delay = DEFAULT_DELAY;

	@ActionProperty(title = "ON_TITLE", description = "ON_DESCRIPTION", editorBuilder = BooleanEditorBuilder.class, order = 11)
	private boolean on = true;

	@ActionProperty(title = "KEY_TITLE", description = "KEY_DESCRIPTION", editorBuilder = LockKeyEditorBuilder.class, overrideFieldName = "lockKey", overrideFieldType = LockKey.class, order = 10)
	private LockKey virtualKeyCode = LockKey.CAPS_LOCK_LOCK_KEY;

	private transient boolean wasUp = true;

	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleDelay(input, component, value);

		if (value) {
			if (wasUp) {
				wasUp = false;
				if (on) {
					input.getOnLockKeys().add(virtualKeyCode);
				} else {
					input.getOffLockKeys().add(virtualKeyCode);
				}
			}
		} else {
			wasUp = true;
		}
	}

	@Override
	public long getDelay() {
		return delay;
	}

	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty()) {
			return super.getDescription(input);
		}

		return MessageFormat.format(Main.STRINGS.getString(on ? "LOCK_KEY_ON" : "LOCK_KEY_OFF"), getLockKey());
	}

	public LockKey getLockKey() {
		return virtualKeyCode;
	}

	@Override
	public void init(final Input input) {
		resetWasUp();
	}

	public boolean isOn() {
		return on;
	}

	void resetWasUp() {
		wasUp = true;
	}

	@Override
	public void setDelay(final long delay) {
		this.delay = delay;
	}

	public void setLockKey(final LockKey lockKey) {
		virtualKeyCode = lockKey;
	}

	public void setOn(final boolean on) {
		this.on = on;
	}
}

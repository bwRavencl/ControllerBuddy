/* Copyright (C) 2018  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DelayEditorBuilder;

@Action(title = "BUTTON_TO_PRESS_ON_SCREEN_KEYBOARD_KEY_ACTION_TITLE", description = "BUTTON_TO_PRESS_ON_SCREEN_KEYBOARD_KEY_ACTION_DESCRIPTION", category = ActionCategory.ON_SCREEN_KEYBOARD_MODE, order = 520)
public final class ButtonToPressOnScreenKeyboardKeyAction
		implements IButtonToDelayableAction, IInitializationAction<Boolean> {

	@ActionProperty(title = "DELAY_TITLE", description = "DELAY_DESCRIPTION", editorBuilder = DelayEditorBuilder.class, order = 400)
	private long delay = DEFAULT_DELAY;

	@ActionProperty(title = "LOCK_KEY_TITLE", description = "LOCK_KEY_DESCRIPTION", editorBuilder = BooleanEditorBuilder.class, order = 10)
	private boolean lockKey;

	private transient boolean wasDown;

	private transient boolean wasUp = true;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleDelay(input, component, value);

		final var onScreenKeyboard = input.getMain().getOnScreenKeyboard();

		if (!value) {
			if (lockKey) {
				wasUp = true;
			} else {
				if (wasDown) {
					onScreenKeyboard.releaseSelectedButton();
				}
				wasDown = false;
			}
		} else if (lockKey) {
			if (wasUp) {
				onScreenKeyboard.toggleLock();
				wasUp = false;
			}
		} else {
			onScreenKeyboard.pressSelectedButton();
			wasDown = true;
		}
	}

	@Override
	public long getDelay() {
		return delay;
	}

	@Override
	public String getDescription(final Input input) {
		return Main.STRINGS
				.getString(lockKey ? "LOCK_SELECTED_ON_SCREEN_KEYBOARD_KEY" : "PRESS_SELECTED_ON_SCREEN_KEYBOARD_KEY");
	}

	@Override
	public void init(final Input input) {
		wasUp = true;
		wasDown = false;
	}

	public boolean isLockKey() {
		return lockKey;
	}

	@Override
	public void setDelay(final long delay) {
		this.delay = delay;
	}

	public void setLockKey(final boolean lockKey) {
		this.lockKey = lockKey;
	}
}

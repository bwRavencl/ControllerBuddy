/* Copyright (C) 2022  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.gui.DelayEditorBuilder;

@Action(title = "BUTTON_TO_RELEASE_ALL_ON_SCREEN_KEYBOARD_KEYS_ACTION_TITLE", description = "BUTTON_TO_RELEASE_ALL_ON_SCREEN_KEYBOARD_KEYS_ACTION_DESCRIPTION", category = ActionCategory.ON_SCREEN_KEYBOARD_MODE, order = 530)
public final class ButtonToReleaseAllOnScreenKeyboardKeysAction implements IButtonToDelayableAction {

	@ActionProperty(title = "DELAY_TITLE", description = "DELAY_DESCRIPTION", editorBuilder = DelayEditorBuilder.class, order = 400)
	private long delay = DEFAULT_DELAY;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleDelay(input, component, value);

		if (value) {
			input.getMain().getOnScreenKeyboard().releaseAllButtons();
		}
	}

	@Override
	public long getDelay() {
		return delay;
	}

	@Override
	public String getDescription(final Input input) {
		return Main.STRINGS.getString("BUTTON_TO_RELEASE_ALL_ON_SCREEN_KEYBOARD_KEYS_ACTION_TITLE");
	}

	@Override
	public void setDelay(final long delay) {
		this.delay = delay;
	}
}

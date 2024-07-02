/* Copyright (C) 2021  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;

@Action(label = "BUTTON_TO_RELEASE_ALL_ON_SCREEN_KEYBOARD_KEYS_ACTION", category = ActionCategory.ON_SCREEN_KEYBOARD_MODE, order = 530)
public final class ButtonToReleaseAllOnScreenKeyboardKeysAction implements IButtonToAction {

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public void doAction(final Input input, final int component, Byte value) {
		value = handleLongPress(input, component, value);

		if (value != 0) {
			input.getMain().getOnScreenKeyboard().releaseAllButtons();
		}
	}

	@Override
	public String getDescription(final Input input) {
		return Main.strings.getString("BUTTON_TO_RELEASE_ALL_ON_SCREEN_KEYBOARD_KEYS_ACTION");
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}
}

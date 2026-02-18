/* Copyright (C) 2020  Matteo Hausner
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

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;

@Action(title = "TO_CURSOR_ACTION_TITLE", description = "TO_CURSOR_ACTION_DESCRIPTION", category = ActionCategory.BUTTON, order = 125)
public final class ButtonToCursorAction extends ToCursorAction<Boolean> implements IButtonToAction {

	@ActionProperty(title = "LONG_PRESS_TITLE", description = "LONG_PRESS_DESCRIPTION", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleLongPress(input, component, value);

		if (!value) {
			remainingD = 0f;

			return;
		}

		final var d = cursorSensitivity * input.getRateMultiplier();
		moveCursor(input, d);
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

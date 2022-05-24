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
import de.bwravencl.controllerbuddy.input.action.gui.DistanceEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;

@Action(label = "TO_CURSOR_ACTION", category = ActionCategory.BUTTON, order = 125)
public final class ButtonToCursorAction extends ToCursorAction<Byte> implements IButtonToAction {

	private static final int DEFAULT_DISTANCE = 1000;

	@ActionProperty(label = "DISTANCE", editorBuilder = DistanceEditorBuilder.class, order = 10)
	private int distance = DEFAULT_DISTANCE;

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@Override
	public void doAction(final Input input, final int component, Byte value) {
		value = handleLongPress(input, component, value);

		if (value == 0) {
			remainingD = 0f;

			return;
		}

		final var d = distance * input.getRateMultiplier();
		moveCursor(input, d);
	}

	public int getDistance() {
		return distance;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	public void setDistance(final int distance) {
		this.distance = distance;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}
}

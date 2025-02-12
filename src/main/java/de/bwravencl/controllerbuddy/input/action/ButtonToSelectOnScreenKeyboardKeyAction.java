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
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard.Direction;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.DirectionEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;
import java.text.MessageFormat;
import java.util.Locale;

@Action(label = "BUTTON_TO_SELECT_ON_SCREEN_KEYBOARD_KEY_ACTION", category = ActionCategory.ON_SCREEN_KEYBOARD_MODE, order = 510)
public final class ButtonToSelectOnScreenKeyboardKeyAction implements IButtonToAction, IResetableAction<Byte> {

	private static final long ACCELERATION_TIME = 300L;

	private static final long INITIAL_MIN_ELAPSE_TIME = 250L;

	private static final long PEAK_MIN_ELAPSE_TIME = 90L;

	private static final float peakElapseTimeReduction = (INITIAL_MIN_ELAPSE_TIME - PEAK_MIN_ELAPSE_TIME)
			/ (float) ACCELERATION_TIME;

	@ActionProperty(label = "DIRECTION", editorBuilder = DirectionEditorBuilder.class, order = 10)
	private Direction direction = Direction.UP;

	private transient long initialPressTime;

	private transient long lastPressTime;

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
			final var currentTime = System.currentTimeMillis();
			if (initialPressTime == 0) {
				initialPressTime = currentTime;
			}

			final var accelerationFactor = Math.min(currentTime - initialPressTime, ACCELERATION_TIME);
			final var minElapseTime = INITIAL_MIN_ELAPSE_TIME - (long) (accelerationFactor * peakElapseTimeReduction);

			if (currentTime - lastPressTime >= minElapseTime) {
				input.getMain().getOnScreenKeyboard().moveSelector(direction);
				lastPressTime = currentTime;
			}
		} else {
			initialPressTime = 0;
		}
	}

	@Override
	public String getDescription(final Input input) {
		return MessageFormat.format(Main.strings.getString("ON_SCREEN_KEYBOARD_KEY_SELECTOR"),
				direction.toString().toLowerCase(Locale.ROOT));
	}

	public Direction getDirection() {
		return direction;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void reset(final Input input) {
		initialPressTime = 0L;
		lastPressTime = 0L;
	}

	public void setDirection(final Direction direction) {
		this.direction = direction;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}
}

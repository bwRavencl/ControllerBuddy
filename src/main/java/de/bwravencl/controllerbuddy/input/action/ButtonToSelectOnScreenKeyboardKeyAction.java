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
import de.bwravencl.controllerbuddy.input.action.gui.DelayEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DirectionEditorBuilder;
import java.text.MessageFormat;
import java.util.Locale;

@Action(title = "BUTTON_TO_SELECT_ON_SCREEN_KEYBOARD_KEY_ACTION_TITLE", description = "BUTTON_TO_SELECT_ON_SCREEN_KEYBOARD_KEY_ACTION_DESCRIPTION", category = ActionCategory.ON_SCREEN_KEYBOARD_MODE, order = 510)
public final class ButtonToSelectOnScreenKeyboardKeyAction
		implements IButtonToDelayableAction, IResetableAction<Boolean> {

	private static final long ACCELERATION_TIME = 300L;

	private static final long INITIAL_MIN_ELAPSE_TIME = 250L;

	private static final long PEAK_MIN_ELAPSE_TIME = 90L;

	private static final float PEAK_ELAPSE_TIME_REDUCTION = (INITIAL_MIN_ELAPSE_TIME - PEAK_MIN_ELAPSE_TIME)
			/ (float) ACCELERATION_TIME;

	@ActionProperty(title = "DELAY_TITLE", description = "DELAY_DESCRIPTION", editorBuilder = DelayEditorBuilder.class, order = 400)
	private long delay = DEFAULT_DELAY;

	@ActionProperty(title = "DIRECTION_TITLE", description = "DIRECTION_DESCRIPTION", editorBuilder = DirectionEditorBuilder.class, order = 10)
	private Direction direction = Direction.UP;

	private transient long initialPressTime;

	private transient long lastPressTime;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleDelay(input, component, value);

		if (value) {
			final var currentTime = System.currentTimeMillis();
			if (initialPressTime == 0) {
				initialPressTime = currentTime;
			}

			final var accelerationFactor = Math.min(currentTime - initialPressTime, ACCELERATION_TIME);
			final var minElapseTime = INITIAL_MIN_ELAPSE_TIME
					- (long) (accelerationFactor * PEAK_ELAPSE_TIME_REDUCTION);

			if (currentTime - lastPressTime >= minElapseTime) {
				input.getMain().getOnScreenKeyboard().moveSelector(direction);
				lastPressTime = currentTime;
			}
		} else {
			initialPressTime = 0;
		}
	}

	@Override
	public long getDelay() {
		return delay;
	}

	@Override
	public String getDescription(final Input input) {
		return MessageFormat.format(Main.STRINGS.getString("ON_SCREEN_KEYBOARD_KEY_SELECTOR"),
				direction.toString().toLowerCase(Locale.ROOT));
	}

	public Direction getDirection() {
		return direction;
	}

	@Override
	public void reset(final Input input) {
		initialPressTime = 0L;
		lastPressTime = 0L;
	}

	@Override
	public void setDelay(final long delay) {
		this.delay = delay;
	}

	public void setDirection(final Direction direction) {
		this.direction = direction;
	}
}

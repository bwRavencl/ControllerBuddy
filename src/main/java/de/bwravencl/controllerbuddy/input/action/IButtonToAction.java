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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Profile;

public interface IButtonToAction extends IAction<Byte> {

	long MIN_LONG_PRESS_TIME = 1000L;
	boolean DEFAULT_LONG_PRESS = false;

	Set<IButtonToAction> wasDownActions = new HashSet<>();
	Map<IAction<?>, Boolean> actionToIsDownUpActionMap = new HashMap<>();
	Map<IButtonToAction, Long> actionToDownSinceMap = new HashMap<>();

	static boolean isDownUpAction(final IAction<?> action) {
		var res = actionToIsDownUpActionMap.get(action);

		if (res == null) {
			if (action instanceof ToKeyAction) {
				final var toKeyAction = (ToKeyAction<?>) action;
				res = toKeyAction.isDownUp();
			} else if (action instanceof ToMouseButtonAction) {
				final var toMouseButtonAction = (ToMouseButtonAction<?>) action;
				res = toMouseButtonAction.isDownUp();
			} else if (action instanceof ButtonToCycleAction)
				res = true;
			else
				res = false;

			actionToIsDownUpActionMap.put(action, res);
		}

		return res;
	}

	static void reset() {
		wasDownActions.clear();
		actionToIsDownUpActionMap.clear();
		actionToDownSinceMap.clear();
	}

	default byte handleLongPress(final Input input, final byte value) {
		if (isLongPress()) {
			final var currentTime = System.currentTimeMillis();

			if (value != 0) {
				if (!actionToDownSinceMap.containsKey(this))
					actionToDownSinceMap.put(this, currentTime);
				else if (currentTime - actionToDownSinceMap.get(this) >= MIN_LONG_PRESS_TIME)
					return value;
			} else if (actionToDownSinceMap.containsKey(this)) {
				if (currentTime - actionToDownSinceMap.get(this) >= MIN_LONG_PRESS_TIME) {
					for (final var actions : input.getProfile().getActiveMode().getButtonToActionsMap().values())
						if (actions.contains(this)) {
							wasDownActions.removeAll(actions);
							break;
						}

					if (!Profile.defaultMode.equals(input.getProfile().getActiveMode()))
						for (final var actions : input.getProfile().getModes().get(0).getButtonToActionsMap().values())
							if (actions.contains(this)) {
								wasDownActions.removeAll(actions);
								break;
							}
				}
				actionToDownSinceMap.remove(this);
			}

			return 0;
		} else if (isDownUpAction(this)) {
			if (value != 0)
				wasDownActions.add(this);
			else if (wasDownActions.contains(this)) {
				wasDownActions.remove(this);
				return 1;
			}

			return 0;
		} else
			return value;
	}

	boolean isLongPress();

	void setLongPress(boolean longPress);
}

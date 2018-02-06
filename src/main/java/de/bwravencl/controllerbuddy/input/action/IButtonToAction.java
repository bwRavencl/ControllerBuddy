/* Copyright (C) 2018  Matteo Hausner
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Profile;

public interface IButtonToAction extends IAction {

	static final long MIN_LONG_PRESS_TIME = 1000L;
	static final boolean DEFAULT_LONG_PRESS = false;
	static final float DEFAULT_ACTIVATION_VALUE = 1.0f;
	static final Set<IButtonToAction> actionToWasDown = new HashSet<>();
	static final Map<IButtonToAction, Long> actionToDownSinceMap = new HashMap<>();

	static boolean floatEquals(final float f1, final float f2) {
		return Math.abs(f1 - f2) < 0.001f;
	}

	static boolean isDownUpAction(final IAction action) {
		if (action instanceof ToKeyAction) {
			final ToKeyAction toKeyAction = (ToKeyAction) action;
			return toKeyAction.isDownUp();
		} else if (action instanceof ToMouseButtonAction) {
			final ToMouseButtonAction toMouseButtonAction = (ToMouseButtonAction) action;
			return toMouseButtonAction.isDownUp();
		} else if (action instanceof ButtonToCycleAction)
			return true;

		return false;
	}

	float getActivationValue();

	default float handleLongPress(final Input input, final float value) {
		final float activationValue = getActivationValue();

		if (isLongPress()) {
			final long currentTime = System.currentTimeMillis();

			if (IButtonToAction.floatEquals(value, activationValue)) {
				if (!actionToDownSinceMap.containsKey(this))
					actionToDownSinceMap.put(this, currentTime);
				else if (currentTime - actionToDownSinceMap.get(this) >= MIN_LONG_PRESS_TIME)
					return value;
			} else if (actionToDownSinceMap.containsKey(this)) {
				if (currentTime - actionToDownSinceMap.get(this) >= MIN_LONG_PRESS_TIME) {
					for (final List<IAction> actions : input.getProfile().getActiveMode().getComponentToActionsMap()
							.values())
						if (actions.contains(this)) {
							actionToWasDown.removeAll(actions);
							break;
						}

					if (!Profile.defaultMode.equals(input.getProfile().getActiveMode()))
						for (final List<IAction> actions : input.getProfile().getModes().get(0)
								.getComponentToActionsMap().values())
							if (actions.contains(this)) {
								actionToWasDown.removeAll(actions);
								break;
							}
				}
				actionToDownSinceMap.remove(this);
			}

			return activationValue - 1.0f;
		} else if (isDownUpAction(this)) {
			if (floatEquals(value, activationValue))
				actionToWasDown.add(this);
			else if (actionToWasDown.contains(this)) {
				actionToWasDown.remove(this);
				return activationValue;
			}

			return activationValue - 1.0f;
		} else
			return value;
	}

	boolean isLongPress();

	void setActivationValue(Float activationValue);

	void setLongPress(Boolean longPress);
}

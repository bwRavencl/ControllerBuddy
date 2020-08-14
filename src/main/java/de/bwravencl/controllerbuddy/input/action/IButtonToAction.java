/* Copyright (C) 2020  Matteo Hausner
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
import java.util.Map;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activatable;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;

public interface IButtonToAction extends IAction<Byte> {

	long MIN_LONG_PRESS_TIME = 1000L;

	boolean DEFAULT_LONG_PRESS = false;

	Map<IButtonToAction, Long> actionToDownSinceMap = new HashMap<>();
	Map<IAction<?>, Boolean> actionToMustDenyActivationMap = new HashMap<>();

	private static boolean isOnReleaseAction(final IActivatableAction<?> action) {
		return action.getActivation() == Activation.SINGLE_ON_RELEASE;
	}

	static void reset() {
		actionToDownSinceMap.clear();
		actionToMustDenyActivationMap.clear();
	}

	default byte handleLongPress(final Input input, final int component, final byte value) {
		if (!isLongPress())
			return value;

		final var currentTime = System.currentTimeMillis();

		if (value != 0) {
			if (!actionToDownSinceMap.containsKey(this))
				actionToDownSinceMap.put(this, currentTime);
			else if (currentTime - actionToDownSinceMap.get(this) >= MIN_LONG_PRESS_TIME) {
				for (final var mode : input.getProfile().getModes()) {
					final var actions = mode.getButtonToActionsMap().get(component);

					if (actions != null && actions.contains(this)) {
						for (final IAction<?> action : actions) {
							if (action == this)
								continue;

							var isUndelayedOnReleaseAction = actionToMustDenyActivationMap.get(action);

							if (isUndelayedOnReleaseAction == null) {
								isUndelayedOnReleaseAction = false;

								if (action instanceof ButtonToKeyAction) {
									final var buttonToKeyAction = (ButtonToKeyAction) action;

									if (!buttonToKeyAction.isLongPress())
										isUndelayedOnReleaseAction = isOnReleaseAction(buttonToKeyAction);
								} else if (action instanceof ButtonToMouseButtonAction) {
									final var buttonToMouseButtonAction = (ButtonToMouseButtonAction) action;

									if (!buttonToMouseButtonAction.isLongPress())
										isUndelayedOnReleaseAction = isOnReleaseAction(buttonToMouseButtonAction);
								} else if (action instanceof ButtonToCycleAction) {
									final var buttonToCycleAction = (ButtonToCycleAction) action;

									if (!buttonToCycleAction.isLongPress())
										isUndelayedOnReleaseAction = true;
								}

								actionToMustDenyActivationMap.put(action, isUndelayedOnReleaseAction);
							}

							if (isUndelayedOnReleaseAction)
								((IActivatableAction<?>) action).setActivatable(Activatable.DENIED_BY_OTHER_ACTION);
						}

						break;
					}
				}

				return value;
			}
		} else if (actionToDownSinceMap.containsKey(this))
			actionToDownSinceMap.remove(this);

		return 0;
	}

	boolean isLongPress();

	void setLongPress(final boolean longPress);
}

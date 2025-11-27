/* Copyright (C) 2015  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activatable;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import java.util.HashMap;
import java.util.Map;

public interface IButtonToAction extends ILongPressAction<Boolean> {

	Map<IButtonToAction, Long> actionToDownSinceMap = new HashMap<>();

	Map<IAction<?>, Boolean> actionToMustDenyActivationMap = new HashMap<>();

	private static boolean isOnReleaseAction(final IActivatableAction<?> action) {
		return action.getActivation() == Activation.SINGLE_ON_RELEASE;
	}

	static void onModeActivated(final Mode activeMode, final Mode newMode) {
		actionToDownSinceMap.keySet().removeIf(action -> {
			if (activeMode.getAllActions().contains(action)) {
				final var optionalButtonId = activeMode.getButtonToActionsMap().entrySet().stream()
						.filter(entry -> entry.getValue().contains(action)).map(Map.Entry::getKey).findFirst();

				return optionalButtonId.map(buttonId -> newMode.getButtonToActionsMap().containsKey(buttonId))
						.orElse(false);
			}

			return false;
		});
	}

	static void onModeDeactivated(final Mode activeMode) {
		actionToDownSinceMap.keySet().removeIf(action -> activeMode.getAllActions().contains(action));
	}

	static void reset() {
		actionToDownSinceMap.clear();
		actionToMustDenyActivationMap.clear();
	}

	default boolean handleLongPress(final Input input, final int component, final boolean value) {
		if (!isLongPress()) {
			return value;
		}

		final var currentTime = System.currentTimeMillis();

		if (value) {
			if (!actionToDownSinceMap.containsKey(this)) {
				actionToDownSinceMap.put(this, currentTime);
			} else if (currentTime - actionToDownSinceMap.get(this) >= MIN_LONG_PRESS_TIME) {
				for (final var mode : input.getProfile().getModes()) {
					final var actions = mode.getButtonToActionsMap().get(component);

					if (actions != null && actions.contains(this)) {
						for (final IAction<?> action : actions) {
							if (action == this) {
								continue;
							}

							var isUndelayedOnReleaseAction = actionToMustDenyActivationMap.get(action);

							if (isUndelayedOnReleaseAction == null) {
								isUndelayedOnReleaseAction = false;

								if (action instanceof final ButtonToButtonAction buttonToButtonAction) {
									if (!buttonToButtonAction.isLongPress()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(buttonToButtonAction);
									}
								} else if (action instanceof final ButtonToKeyAction buttonToKeyAction) {
									if (!buttonToKeyAction.isLongPress()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(buttonToKeyAction);
									}
								} else if (action instanceof final ButtonToMouseButtonAction buttonToMouseButtonAction) {
									if (!buttonToMouseButtonAction.isLongPress()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(buttonToMouseButtonAction);
									}
								} else if (action instanceof final ButtonToCycleAction buttonToCycleAction) {
									if (!buttonToCycleAction.isLongPress()) {
										isUndelayedOnReleaseAction = true;
									}
								}

								actionToMustDenyActivationMap.put(action, isUndelayedOnReleaseAction);
							}

							if (isUndelayedOnReleaseAction) {
								((IActivatableAction<?>) action).setActivatable(Activatable.DENIED_BY_OTHER_ACTION);
							}
						}

						break;
					}
				}

				return true;
			}
		} else {
			actionToDownSinceMap.remove(this);
		}

		return false;
	}
}

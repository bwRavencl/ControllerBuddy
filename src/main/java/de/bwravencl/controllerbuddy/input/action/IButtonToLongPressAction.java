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

public interface IButtonToLongPressAction extends ILongPressAction<Boolean> {

	Map<IButtonToLongPressAction, Long> ACTION_TO_DOWN_SINCE_MAP = new HashMap<>();

	Map<IAction<?>, Boolean> ACTION_TO_MUST_DENY_ACTIVATION_MAP = new HashMap<>();

	private static boolean isOnReleaseAction(final IActivatableAction<?> action) {
		return action.getActivation() == Activation.ON_RELEASE;
	}

	static void onModeActivated(final Mode activeMode, final Mode newMode) {
		ACTION_TO_DOWN_SINCE_MAP.keySet().removeIf(action -> {
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
		ACTION_TO_DOWN_SINCE_MAP.keySet().removeIf(action -> activeMode.getAllActions().contains(action));
	}

	static void reset() {
		ACTION_TO_DOWN_SINCE_MAP.clear();
		ACTION_TO_MUST_DENY_ACTIVATION_MAP.clear();
	}

	default boolean handleLongPress(final Input input, final int component, final boolean value) {
		if (!isLongPress()) {
			return value;
		}

		final var currentTime = System.currentTimeMillis();

		if (value) {
			if (!ACTION_TO_DOWN_SINCE_MAP.containsKey(this)) {
				ACTION_TO_DOWN_SINCE_MAP.put(this, currentTime);
			} else if (currentTime - ACTION_TO_DOWN_SINCE_MAP.get(this) >= MIN_LONG_PRESS_TIME) {
				for (final var mode : input.getProfile().getModes()) {
					final var actions = mode.getButtonToActionsMap().get(component);

					if (actions != null && actions.contains(this)) {
						for (final IAction<?> action : actions) {
							if (action == this) {
								continue;
							}

							var isUndelayedOnReleaseAction = ACTION_TO_MUST_DENY_ACTIVATION_MAP.get(action);

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

								ACTION_TO_MUST_DENY_ACTIVATION_MAP.put(action, isUndelayedOnReleaseAction);
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
			ACTION_TO_DOWN_SINCE_MAP.remove(this);
		}

		return false;
	}
}

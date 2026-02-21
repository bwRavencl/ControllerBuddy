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

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activatable;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import java.util.HashMap;
import java.util.Map;

public interface IAxisToLongPressAction extends IAxisToAction, ILongPressAction<Float> {

	Map<IAxisToLongPressAction, Long> ACTION_TO_DOWN_SINCE_MAP = new HashMap<>();

	Map<IAction<?>, Boolean> ACTION_TO_MUST_DENY_ACTIVATION_MAP = new HashMap<>();

	private static boolean isOnReleaseAction(final IActivatableAction<?> action) {
		return action.getActivation() == Activation.ON_RELEASE;
	}

	static void onModeActivated(final Mode activeMode, final Mode newMode) {
		ACTION_TO_DOWN_SINCE_MAP.keySet().removeIf(action -> {
			if (activeMode.getAllActions().contains(action)) {
				final var optionalAxisId = activeMode.getAxisToActionsMap().entrySet().stream()
						.filter(entry -> entry.getValue().contains(action)).map(Map.Entry::getKey).findFirst();

				return optionalAxisId.map(axisId -> newMode.getAxisToActionsMap().containsKey(axisId)).orElse(false);
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

	float getMaxAxisValue();

	float getMinAxisValue();

	default Float handleLongPress(final Input input, final int component, final Float value) {
		if (!isLongPress()) {
			return value;
		}

		final var currentTime = System.currentTimeMillis();

		if (value >= getMinAxisValue() && value <= getMaxAxisValue()) {
			if (!ACTION_TO_DOWN_SINCE_MAP.containsKey(this)) {
				ACTION_TO_DOWN_SINCE_MAP.put(this, currentTime);
			} else if (currentTime - ACTION_TO_DOWN_SINCE_MAP.get(this) >= MIN_LONG_PRESS_TIME) {
				for (final var mode : input.getProfile().getModes()) {
					final var actions = mode.getAxisToActionsMap().get(component);

					if (actions != null && actions.contains(this)) {
						for (final IAction<?> action : actions) {
							if (action == this) {
								continue;
							}

							var isUndelayedOnReleaseAction = ACTION_TO_MUST_DENY_ACTIVATION_MAP.get(action);

							if (isUndelayedOnReleaseAction == null) {
								isUndelayedOnReleaseAction = false;

								if (action instanceof final AxisToButtonAction axisToButtonAction) {
									if (!axisToButtonAction.isLongPress()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(axisToButtonAction);
									}
								} else if (action instanceof final AxisToKeyAction axisToKeyAction) {
									if (!axisToKeyAction.isLongPress()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(axisToKeyAction);
									}
								} else if (action instanceof final AxisToMouseButtonAction axisToMouseButtonAction) {
									if (!axisToMouseButtonAction.isLongPress()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(axisToMouseButtonAction);
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

				return value;
			}
		} else {
			ACTION_TO_DOWN_SINCE_MAP.remove(this);
		}

		return Float.MIN_VALUE;
	}

	void setMaxAxisValue(final float maxAxisValue);

	void setMinAxisValue(final float minAxisValue);
}

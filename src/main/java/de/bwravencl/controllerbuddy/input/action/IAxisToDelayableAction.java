/*
 * Copyright (C) 2021 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activatable;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import java.util.HashMap;
import java.util.Map;

/// Interface for axis-triggered actions that support an activation delay.
///
/// Combines [IAxisToAction] and [IDelayableAction] to provide delay handling
/// for actions driven by axis input. When a delay is configured, the action
/// only fires after the axis value remains within the configured range for the
/// specified duration. Delayed activation can also deny activation of
/// co-located undelayed on-release actions.
public interface IAxisToDelayableAction extends IAxisToAction, IDelayableAction<Float> {

	/// Maps each axis delayable action to the timestamp when it was first
	/// activated.
	Map<IAxisToDelayableAction, Long> ACTION_TO_DOWN_SINCE_MAP = new HashMap<>();

	/// Maps actions to whether their activation must be denied by a delayed action.
	Map<IAction<?>, Boolean> ACTION_TO_MUST_DENY_ACTIVATION_MAP = new HashMap<>();

	/// Returns whether the given action uses [Activation#ON_RELEASE] activation.
	///
	/// @param action the activatable action to check
	/// @return `true` if the action fires on release
	private static boolean isOnReleaseAction(final IActivatableAction<?> action) {
		return action.getActivation() == Activation.ON_RELEASE;
	}

	/// Handles mode activation by removing tracked delay entries for actions that
	/// exist in both the active and new modes on the same axis.
	///
	/// @param activeMode the currently active mode
	/// @param newMode the mode being activated
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

	/// Handles mode deactivation by removing tracked delay entries for actions
	/// belonging to the deactivated mode.
	///
	/// @param activeMode the mode being deactivated
	static void onModeDeactivated(final Mode activeMode) {
		ACTION_TO_DOWN_SINCE_MAP.keySet().removeIf(action -> activeMode.getAllActions().contains(action));
	}

	/// Clears all tracked delay state and denied activation state.
	static void reset() {
		ACTION_TO_DOWN_SINCE_MAP.clear();
		ACTION_TO_MUST_DENY_ACTIVATION_MAP.clear();
	}

	/// Returns the upper bound of the axis range that triggers this action.
	///
	/// @return the maximum axis value
	float getMaxAxisValue();

	/// Returns the lower bound of the axis range that triggers this action.
	///
	/// @return the minimum axis value
	float getMinAxisValue();

	/// Processes the delay logic for axis input. If no delay is configured, the
	/// value passes through unchanged. Otherwise, the value is suppressed until the
	/// axis stays within range for the configured delay duration.
	///
	/// @param input the current input state
	/// @param component the axis component index
	/// @param value the current axis value
	/// @return the original value if the delay has elapsed, or `Float.MIN_VALUE` to
	/// suppress
	default Float handleDelay(final Input input, final int component, final Float value) {
		if (!isDelayed()) {
			return value;
		}

		final var currentTime = System.currentTimeMillis();

		if (value >= getMinAxisValue() && value <= getMaxAxisValue()) {
			if (!ACTION_TO_DOWN_SINCE_MAP.containsKey(this)) {
				ACTION_TO_DOWN_SINCE_MAP.put(this, currentTime);
			} else if (currentTime - ACTION_TO_DOWN_SINCE_MAP.get(this) >= getDelay()) {
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
									if (!axisToButtonAction.isDelayed()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(axisToButtonAction);
									}
								} else if (action instanceof final AxisToKeyAction axisToKeyAction) {
									if (!axisToKeyAction.isDelayed()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(axisToKeyAction);
									}
								} else if (action instanceof final AxisToMouseButtonAction axisToMouseButtonAction) {
									if (!axisToMouseButtonAction.isDelayed()) {
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

	/// Sets the upper bound of the axis range that triggers this action.
	///
	/// @param maxAxisValue the maximum axis value
	void setMaxAxisValue(final float maxAxisValue);

	/// Sets the lower bound of the axis range that triggers this action.
	///
	/// @param minAxisValue the minimum axis value
	void setMinAxisValue(final float minAxisValue);
}

/*
 * Copyright (C) 2015 Matteo Hausner
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

/// Interface for button-triggered actions that support an activation delay.
///
/// Extends [IDelayableAction] with `Boolean` value type to provide delay
/// handling for actions driven by button input. When a delay is configured, the
/// action only fires after the button remains pressed for the specified
/// duration. Delayed activation can also deny activation of co-located
/// undelayed on-release actions.
public interface IButtonToDelayableAction extends IDelayableAction<Boolean> {

	/// Maps each delayable action to the timestamp when it was first activated.
	Map<IButtonToDelayableAction, Long> ACTION_TO_DOWN_SINCE_MAP = new HashMap<>();

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
	/// exist in both the active and new modes on the same button.
	///
	/// @param activeMode the currently active mode
	/// @param newMode the mode being activated
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

	/// Processes the delay logic for button input. If no delay is configured, the
	/// value passes through unchanged. Otherwise, the button press is suppressed
	/// until held for the configured delay duration.
	///
	/// @param input the current input state
	/// @param component the button component index
	/// @param value the current button state (true if pressed)
	/// @return the effective button state after delay processing
	default boolean handleDelay(final Input input, final int component, final boolean value) {
		if (!isDelayed()) {
			return value;
		}

		final var currentTime = System.currentTimeMillis();

		if (value) {
			if (!ACTION_TO_DOWN_SINCE_MAP.containsKey(this)) {
				ACTION_TO_DOWN_SINCE_MAP.put(this, currentTime);
			} else if (currentTime - ACTION_TO_DOWN_SINCE_MAP.get(this) >= getDelay()) {
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
									if (!buttonToButtonAction.isDelayed()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(buttonToButtonAction);
									}
								} else if (action instanceof final ButtonToKeyAction buttonToKeyAction) {
									if (!buttonToKeyAction.isDelayed()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(buttonToKeyAction);
									}
								} else if (action instanceof final ButtonToMouseButtonAction buttonToMouseButtonAction) {
									if (!buttonToMouseButtonAction.isDelayed()) {
										isUndelayedOnReleaseAction = isOnReleaseAction(buttonToMouseButtonAction);
									}
								} else if (action instanceof final ButtonToCycleAction buttonToCycleAction) {
									if (!buttonToCycleAction.isDelayed()) {
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

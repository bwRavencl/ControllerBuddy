/* Copyright (C) 2020  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activatable;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import java.util.HashMap;
import java.util.Map;

public interface IButtonToAction extends ILongPressAction<Byte> {

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
        if (!isLongPress()) {
            return value;
        }

        final var currentTime = System.currentTimeMillis();

        if (value != 0) {
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

                                if (action instanceof final ButtonToKeyAction buttonToKeyAction) {
                                    if (!buttonToKeyAction.isLongPress()) {
                                        isUndelayedOnReleaseAction = isOnReleaseAction(buttonToKeyAction);
                                    }
                                } else if (action
                                        instanceof final ButtonToMouseButtonAction buttonToMouseButtonAction) {
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

                return value;
            }
        } else {
            actionToDownSinceMap.remove(this);
        }

        return 0;
    }
}

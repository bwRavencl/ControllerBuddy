/*
 * Copyright (C) 2014 Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;

/// Maps a physical button press to a keyboard key press.
///
/// Delegates to [ToKeyAction] for the actual keystroke handling and supports
/// configurable activation delay via [IButtonToDelayableAction].
@Action(icon = "⌨️", title = "TO_KEY_ACTION_TITLE", description = "TO_KEY_ACTION_DESCRIPTION", category = ActionCategory.BUTTON_AND_CYCLES, order = 115)
public final class ButtonToKeyAction extends ToKeyAction<Boolean> implements IButtonToDelayableAction {

	/// Processes a button input value by applying delay handling, then delegating
	/// to the inherited key action logic.
	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleDelay(input, component, value);
		handleAction(value, input);
	}
}

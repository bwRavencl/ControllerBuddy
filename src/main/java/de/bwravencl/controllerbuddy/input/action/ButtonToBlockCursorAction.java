/*
 * Copyright (C) 2026 Matteo Hausner
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

import de.bwravencl.controllerbuddy.input.ControllerState;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import org.jspecify.annotations.Nullable;

/// Maps a button press to blocking cursor movement.
///
/// While the button is held, the cursor is blocked from moving.
@Action(icon = "↛", title = "BUTTON_TO_BLOCK_CURSOR_ACTION_TITLE", description = "BUTTON_TO_BLOCK_CURSOR_ACTION_DESCRIPTION", category = ActionCategory.BUTTON_AND_CYCLES, order = 126)
public final class ButtonToBlockCursorAction extends DescribableAction<Boolean> {

	@Override
	public void doAction(final Input input, final int component, final Boolean value,
			final @Nullable ControllerState controllerState) {
		input.setCursorBlocked(value);
	}
}

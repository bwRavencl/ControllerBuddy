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
import java.lang.constant.Constable;

/// A no-op action that performs no work when executed.
///
/// This is used as a placeholder action in configurations where an action slot
/// must be filled, but no actual behavior is desired.
@Action(icon = "∅", title = "NULL_ACTION_TITLE", description = "NULL_ACTION_DESCRIPTION", category = ActionCategory.ALL, order = 999)
public final class NullAction implements IAction<Constable> {

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/// Does nothing. This action intentionally has no effect.
	///
	/// @param input the current input state
	/// @param component the component index
	/// @param value the input value (ignored)
	@Override
	public void doAction(final Input input, final int component, final Constable value) {
	}

	/// Returns the default description for this no-op action.
	///
	/// @param input the current input state
	/// @return the action description
	@Override
	public String getDescription(final Input input) {
		return IAction.getDefaultDescription(this);
	}
}

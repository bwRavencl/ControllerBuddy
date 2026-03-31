/*
 * Copyright (C) 2019 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action.gui;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.input.action.IAction;

/// Editor builder for cursor sensitivity properties, providing a spinner with a
/// range of 1 to 100,000.
///
/// Higher values make the cursor move faster in response to axis input, while
/// lower values produce finer-grained movement.
public final class CursorSensitivityEditorBuilder extends NumberEditorBuilder<Integer> {

	/// Constructs a cursor sensitivity editor builder for the specified action
	/// property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose cursor sensitivity property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws ReflectiveOperationException if reflection operations fail
	public CursorSensitivityEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws ReflectiveOperationException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	Comparable<Integer> getMaximum() {
		return 100_000;
	}

	@Override
	Comparable<Integer> getMinimum() {
		return 1;
	}

	@Override
	Number getStepSize() {
		return 1;
	}
}

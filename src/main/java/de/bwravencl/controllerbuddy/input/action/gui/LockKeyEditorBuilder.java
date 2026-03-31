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
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.action.IAction;

/// Editor builder for lock key properties, presenting a combo box of available
/// [LockKey] values (e.g., Caps Lock, Num Lock, Scroll Lock).
///
/// Used to configure which lock key is toggled or synchronized when the
/// associated action fires.
public final class LockKeyEditorBuilder extends ArrayEditorBuilder<LockKey> {

	/// Constructs a lock key editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose lock key property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws ReflectiveOperationException if reflection operations fail
	public LockKeyEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws ReflectiveOperationException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	LockKey[] getValues() {
		return LockKey.LOCK_KEYS.toArray(LockKey[]::new);
	}
}

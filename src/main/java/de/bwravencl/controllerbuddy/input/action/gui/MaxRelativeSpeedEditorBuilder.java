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
import java.lang.reflect.InvocationTargetException;

/// Editor builder for maximum relative speed properties, providing a spinner
/// with a range of 0.1 to 100.0 and a step size of 0.01.
///
/// Controls the upper bound of the relative speed at which axis-driven actions
/// (such as cursor movement) are applied.
public final class MaxRelativeSpeedEditorBuilder extends NumberEditorBuilder<Float> {

	/// Constructs a maximum relative speed editor builder for the specified action
	/// property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose maximum relative speed property is being
	/// edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws IllegalAccessException if the property cannot be accessed
	/// @throws InvocationTargetException if the property getter throws an exception
	/// @throws NoSuchMethodException if the property getter method is not found
	public MaxRelativeSpeedEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	Comparable<Float> getMaximum() {
		return 100f;
	}

	@Override
	Comparable<Float> getMinimum() {
		return 0.1f;
	}

	@Override
	Number getStepSize() {
		return 0.01f;
	}
}

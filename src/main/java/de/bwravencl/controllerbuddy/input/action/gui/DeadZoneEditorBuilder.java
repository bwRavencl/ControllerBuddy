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

/// Editor builder for dead zone properties, providing a spinner with a range of
/// 0.0 to 1.0 and a step size of 0.01.
///
/// The dead zone defines the portion of the axis range near the center that
/// is treated as neutral, preventing small unintentional movements from
/// triggering an action.
public final class DeadZoneEditorBuilder extends NumberEditorBuilder<Float> {

	/// Constructs a dead zone editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose dead zone property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws IllegalAccessException if the property cannot be accessed
	/// @throws InvocationTargetException if the property getter throws an exception
	/// @throws NoSuchMethodException if the property getter method is not found
	public DeadZoneEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	Comparable<Float> getMaximum() {
		return 1f;
	}

	@Override
	Comparable<Float> getMinimum() {
		return 0f;
	}

	@Override
	Number getStepSize() {
		return 0.01f;
	}
}

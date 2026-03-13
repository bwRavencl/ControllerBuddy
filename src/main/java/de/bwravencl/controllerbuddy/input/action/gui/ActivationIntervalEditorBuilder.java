/*
 * Copyright (C) 2023 Matteo Hausner
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
import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.input.action.ActivationIntervalAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JPanel;

/// Editor builder for activation interval properties, rendering a millisecond
/// spinner with a range of 0 to 10,000 ms.
///
/// The spinner is disabled when the current activation mode does not support a
/// maximum interval or when editing inside a cycle editor.
public final class ActivationIntervalEditorBuilder extends NumberEditorBuilder<Integer> {

	/// Whether the spinner should be disabled because the current activation mode
	/// does not support an interval.
	private final boolean disable;

	/// Constructs an activation interval editor builder for the specified action
	/// property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose activation interval property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws IllegalAccessException if the property cannot be accessed
	/// @throws InvocationTargetException if the property getter throws an exception
	/// @throws NoSuchMethodException if the property getter method is not found
	public ActivationIntervalEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		super(editActionsDialog, action, fieldName, fieldType);

		if (editActionsDialog.isCycleEditor()) {
			disable = true;
		} else if (action instanceof final IActivatableAction<?> activatableAction
				&& !ActivationIntervalAction.activationSupportsMaxInterval(activatableAction.getActivation())) {
			final var fieldToActionPropertyMap = EditActionsDialog
					.getFieldToActionPropertiesMap(ActivationIntervalAction.class);
			disable = fieldToActionPropertyMap.entrySet().stream().filter(e -> fieldName.equals(e.getKey().getName()))
					.findFirst()
					.map(e -> ActivationIntervalAction.MAX_ACTIVATION_INTERVAL_TITLE.equals(e.getValue().title()))
					.orElse(false);
		} else {
			disable = false;
		}
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		if (disable) {
			initialValue = 0;
		}

		super.buildEditor(parentPanel);

		GuiUtils.makeMillisecondSpinner(spinner, 6);

		if (disable) {
			spinner.setEnabled(false);
		}
	}

	@Override
	Comparable<Integer> getMaximum() {
		return 10_000;
	}

	@Override
	Comparable<Integer> getMinimum() {
		return 0;
	}

	@Override
	Number getStepSize() {
		return 10;
	}
}

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
import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JPanel;

/// Editor builder for action activation mode properties, presenting a combo box
/// of [Activation] values.
///
/// When used inside a cycle editor, the activation is forced to
/// [Activation#ON_PRESS] and the combo box is disabled to prevent incompatible
/// configurations.
public final class ActivationEditorBuilder extends ArrayEditorBuilder<Activation> {

	/// Whether [#buildEditor(JPanel)] has completed, used to guard
	/// [#onNewValueSet()].
	private boolean initialized;

	/// Constructs an activation editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose activation property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws IllegalAccessException if the property cannot be accessed
	/// @throws InvocationTargetException if the property getter throws an exception
	/// @throws NoSuchMethodException if the property getter method is not found
	public ActivationEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var cycleEditor = editActionsDialog.isCycleEditor();

		if (cycleEditor) {
			initialValue = Activation.ON_PRESS;
		}

		super.buildEditor(parentPanel);

		if (cycleEditor) {
			comboBox.setEnabled(false);
		}

		initialized = true;
	}

	@Override
	Activation[] getValues() {
		if (action instanceof ButtonToCycleAction) {
			return new Activation[] { Activation.ON_PRESS, Activation.ON_RELEASE };
		}

		return Activation.values();
	}

	@Override
	void onNewValueSet() {
		super.onNewValueSet();

		if (!initialized) {
			return;
		}

		editActionsDialog.updateProperties();
		editActionsDialog.revalidate();
	}
}

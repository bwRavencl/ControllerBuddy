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
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/// Abstract editor builder for enum or array-valued properties.
///
/// Renders a [JComboBox] populated with the values returned by [#getValues()].
/// Subclasses provide the concrete set of selectable values by implementing the
/// abstract [#getValues()] method.
///
/// @param <T> the element type of the selectable values
abstract class ArrayEditorBuilder<T> extends EditorBuilder {

	private static final Logger LOGGER = Logger.getLogger(ArrayEditorBuilder.class.getName());

	/// The combo box component populated with selectable values.
	JComboBox<T> comboBox;

	/// Constructs an array editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws IllegalAccessException if the getter or setter method is not
	/// accessible
	/// @throws InvocationTargetException if the getter method throws an exception
	/// @throws NoSuchMethodException if the getter or setter method is not found
	ArrayEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action, final String fieldName,
			final Class<?> fieldType) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var values = getValues();

		comboBox = new JComboBox<>(values);
		comboBox.setAction(new JComboBoxSetPropertyAction(action, setterMethod));
		if (initialValue != null && Arrays.stream(values).noneMatch(initialValue::equals)) {
			comboBox.setSelectedItem(null);
		} else {
			comboBox.setSelectedItem(initialValue);
		}

		parentPanel.add(comboBox);
	}

	/// Returns the array of selectable values to populate the combo box.
	///
	/// @return the array of selectable values
	abstract T[] getValues();

	/// Called after a new value has been applied to the action property.
	///
	/// The default implementation is empty; subclasses may override to react
	/// to the change.
	void onNewValueSet() {
	}

	/// Action that applies the currently selected [JComboBox] item to the action
	/// property via the setter method.
	///
	/// Invoked automatically by the combo box whenever the user selects a
	/// different item, and also calls [ArrayEditorBuilder#onNewValueSet()] to
	/// allow subclasses to react to the change.
	private final class JComboBoxSetPropertyAction extends PropertySetterAction {

		@Serial
		private static final long serialVersionUID = 1938012378184518954L;

		/// Constructs the action with the target action and its property setter method.
		///
		/// @param action the action whose property is updated on selection
		/// @param setterMethod the setter method to invoke with the selected value
		JComboBoxSetPropertyAction(final IAction<?> action, final Method setterMethod) {
			super(action, setterMethod);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				setterMethod.invoke(action, ((JComboBox<?>) e.getSource()).getSelectedItem());
				onNewValueSet();
			} catch (final IllegalAccessException | InvocationTargetException e1) {
				LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}
}

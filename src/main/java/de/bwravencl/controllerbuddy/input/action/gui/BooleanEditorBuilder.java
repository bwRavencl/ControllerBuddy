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
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/// Editor builder for boolean properties, rendering a [JCheckBox] that toggles
/// the property value.
///
/// The checkbox state is initialized from the current property value and
/// updates the action property immediately when toggled.
public class BooleanEditorBuilder extends EditorBuilder {

	private static final Logger LOGGER = Logger.getLogger(BooleanEditorBuilder.class.getName());

	/// The checkbox rendered by this editor.
	JCheckBox checkBox;

	/// Constructs a boolean editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose boolean property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws ReflectiveOperationException if reflection operations fail
	public BooleanEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws ReflectiveOperationException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		checkBox = new JCheckBox(new JCheckBoxSetPropertyAction(action, setterMethod));
		checkBox.setSelected((boolean) initialValue);
		parentPanel.add(checkBox);
	}

	/// Action that applies the current [JCheckBox] selection state to the action
	/// property via the setter method.
	///
	/// Invoked automatically by the checkbox whenever the user toggles it,
	/// immediately updating the boolean property on the action.
	private final class JCheckBoxSetPropertyAction extends PropertySetterAction {

		@Serial
		private static final long serialVersionUID = -33052386834598414L;

		/// Constructs the action with the target action and its property setter method.
		///
		/// @param action the action whose boolean property is toggled
		/// @param setterMethod the setter method to invoke with the checkbox state
		private JCheckBoxSetPropertyAction(final IAction<?> action, final Method setterMethod) {
			super(action, setterMethod);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				final var selected = ((JCheckBox) e.getSource()).isSelected();
				setterMethod.invoke(action, selected);
				onNewValueSet();
			} catch (final ReflectiveOperationException e1) {
				LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}
}

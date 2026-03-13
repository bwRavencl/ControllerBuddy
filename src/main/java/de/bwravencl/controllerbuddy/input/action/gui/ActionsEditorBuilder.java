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
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;

/// Editor builder for sub-action list properties, providing a button that opens
/// a nested [EditActionsDialog] for editing the actions within a
/// [ButtonToCycleAction].
///
/// The button label shows the current number of sub-actions. Clicking it opens
/// the nested dialog, allowing the user to add, remove, and reorder the
/// sub-actions of a cycle action.
public final class ActionsEditorBuilder extends EditorBuilder {

	/// Constructs an actions editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose sub-actions property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws IllegalAccessException if the property cannot be accessed
	/// @throws InvocationTargetException if the property getter throws an exception
	/// @throws NoSuchMethodException if the property getter method is not found
	public ActionsEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var editActionsButton = new JButton(new EditActionsAction());
		editActionsButton.setPreferredSize(Main.BUTTON_DIMENSION);
		parentPanel.add(editActionsButton);
	}

	/// Action that opens a nested [EditActionsDialog] for editing the sub-actions
	/// of a [ButtonToCycleAction].
	///
	/// Displayed as a button in the property editor row; when invoked it opens
	/// the nested dialog and blocks until the user closes it.
	private final class EditActionsAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -6538021954760621595L;

		/// Constructs the action, setting its display name and tooltip description.
		private EditActionsAction() {
			putValue(NAME, Main.STRINGS.getString("EDIT_ACTIONS_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, MessageFormat.format(Main.STRINGS.getString("EDIT_ACTIONS_ACTION_DESCRIPTION"),
					IAction.getLabel(action.getClass())));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var editComponentDialog = new EditActionsDialog(editActionsDialog, (ButtonToCycleAction) action);
			editComponentDialog.setVisible(true);
		}
	}
}

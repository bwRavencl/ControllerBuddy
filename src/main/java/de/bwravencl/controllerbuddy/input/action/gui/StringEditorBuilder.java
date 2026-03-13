/*
 * Copyright (C) 2020 Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.IAction;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/// Editor builder for string properties, rendering a text field that updates
/// the action property on every document change, action event, and focus loss.
///
/// This ensures the property value stays in sync with the text field contents
/// regardless of how the user commits the input.
public final class StringEditorBuilder extends EditorBuilder {

	private static final Logger LOGGER = Logger.getLogger(StringEditorBuilder.class.getName());

	/// Constructs a string editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose string property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws IllegalAccessException if the property cannot be accessed
	/// @throws InvocationTargetException if the property getter throws an exception
	/// @throws NoSuchMethodException if the property getter method is not found
	public StringEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var textField = GuiUtils.createTextFieldWithMenu((String) initialValue, 17);
		textField.setCaretPosition(0);

		final var textFieldPropertySetter = new TextFieldPropertySetter(textField, action, setterMethod);
		textField.addActionListener(textFieldPropertySetter);
		textField.addFocusListener(textFieldPropertySetter);
		textField.getDocument().addDocumentListener(textFieldPropertySetter);

		parentPanel.add(textField);
	}

	/// Listener that writes the current text field content to the action property
	/// in response to document changes, action events, and focus-loss events.
	///
	/// When updating due to an action event or focus loss the text is also
	/// stripped of leading and trailing whitespace, and the field is updated
	/// asynchronously on the event dispatch thread if the stripped value differs
	/// from the original.
	private static final class TextFieldPropertySetter extends PropertySetter
			implements ActionListener, DocumentListener, FocusListener {

		/// Text field whose content is read and written on each change event.
		private final JTextField textField;

		/// Constructs the setter holding the text field, action, and setter method.
		///
		/// @param textField the text field whose content is read on each change
		/// @param action the action whose string property is updated
		/// @param setterMethod the setter method to invoke with the current text
		private TextFieldPropertySetter(final JTextField textField, final IAction<?> action,
				final Method setterMethod) {
			super(action, setterMethod);

			this.textField = textField;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			setString(true);
		}

		@Override
		public void changedUpdate(final DocumentEvent e) {
			setString(false);
		}

		@Override
		public void focusGained(final FocusEvent e) {
		}

		@Override
		public void focusLost(final FocusEvent e) {
			setString(true);
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			setString(false);
		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			setString(false);
		}

		/// Reads the current text from the field, strips surrounding whitespace when
		/// applicable, and invokes the setter with the result.
		///
		/// When `updateTextField` is `true` and the stripped text differs from the
		/// original, the field is updated asynchronously on the event dispatch thread.
		///
		/// @param updateTextField `true` to also update the text field when whitespace
		/// is stripped
		private void setString(final boolean updateTextField) {
			var text = textField.getText();

			if (text != null && !text.isEmpty()) {
				final var strippedText = text.strip();

				if (updateTextField && !strippedText.equals(text)) {
					EventQueue.invokeLater(() -> textField.setText(strippedText));
				}

				text = strippedText;
			}

			try {
				setterMethod.invoke(action, text);
			} catch (final IllegalAccessException | InvocationTargetException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
}

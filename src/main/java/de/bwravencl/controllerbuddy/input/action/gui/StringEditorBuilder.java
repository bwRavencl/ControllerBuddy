/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input.action.gui;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
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

public final class StringEditorBuilder extends EditorBuilder {

	private static final Logger log = Logger.getLogger(StringEditorBuilder.class.getName());

	public StringEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws SecurityException, NoSuchMethodException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var textField = new JTextField(17);
		textField.setText((String) initialValue);
		textField.setCaretPosition(0);

		final var textFieldPropertySetter = new TextFieldPropertySetter(textField, action, setterMethod);
		textField.addActionListener(textFieldPropertySetter);
		textField.addFocusListener(textFieldPropertySetter);
		textField.getDocument().addDocumentListener(textFieldPropertySetter);

		parentPanel.add(textField);
	}

	private static class TextFieldPropertySetter extends PropertySetter
			implements ActionListener, DocumentListener, FocusListener {

		private final JTextField textField;

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
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
}

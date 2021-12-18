/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.controllerbuddy.input.action.gui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.input.action.IAction;

public final class StringEditorBuilder extends EditorBuilder {

	private class MyDocumentListener extends PropertySetter implements DocumentListener {

		private MyDocumentListener(final IAction<?> action, final Method setterMethod) {
			super(action, setterMethod);
		}

		@Override
		public void changedUpdate(final DocumentEvent e) {
			update(e);
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			update(e);
		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			update(e);
		}

		private void update(final DocumentEvent e) {
			try {
				final var document = e.getDocument();
				final var text = document.getText(0, document.getLength());

				setterMethod.invoke(action, text);
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| BadLocationException e1) {
				log.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}

	private static final Logger log = Logger.getLogger(StringEditorBuilder.class.getName());

	public StringEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws NoSuchFieldException, SecurityException,
			NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var textField = new JTextField(23);
		textField.setText((String) initialValue);
		textField.setCaretPosition(0);
		textField.getDocument().addDocumentListener(new MyDocumentListener(action, setterMethod));

		parentPanel.add(textField);
	}
}

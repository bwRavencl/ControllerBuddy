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
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.IAction;
import java.io.Serial;
import java.text.ParseException;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JPanel;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

/// Editor builder for gamepad button index properties, rendering a spinner with
/// zero-based internal values displayed as one-based button numbers to the
/// user.
///
/// The display formatter adds one to the internal zero-based index so that
/// buttons are presented to the user starting from 1, up to the total number
/// of buttons reported by the connected controller.
public final class ButtonEditorBuilder extends NumberEditorBuilder<Integer> {

	/// Constructs a button editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose button index property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws ReflectiveOperationException if reflection operations fail
	public ButtonEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws ReflectiveOperationException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		super.buildEditor(parentPanel);

		final var formatterFactory = new ZeroBasedFormatterFactory();
		textField.setFormatterFactory(formatterFactory);

		final var formatter = (DefaultFormatter) textField.getFormatter();
		formatter.setCommitsOnValidEdit(true);
	}

	@Override
	Comparable<Integer> getMaximum() {
		return Input.MAX_N_BUTTONS - 1;
	}

	@Override
	Comparable<Integer> getMinimum() {
		return 0;
	}

	@Override
	Number getStepSize() {
		return 1;
	}

	/// Formatter that converts between zero-based integer values and one-based
	/// string representations for display to the user.
	///
	/// Parses the string entered by the user by subtracting one so that button
	/// numbers shown in the UI (starting from 1) map to the zero-based indices
	/// stored in the action.
	private static final class ZeroBasedFormatter extends DefaultFormatter {

		@Serial
		private static final long serialVersionUID = -7229427356426148291L;

		@Override
		public Object stringToValue(final String text) throws ParseException {
			if (text == null || text.isBlank()) {
				return null;
			}

			try {
				return Integer.parseInt(text) - 1;
			} catch (final NumberFormatException e) {
				throw new ParseException(text, 0);
			}
		}

		@Override
		public String valueToString(final Object value) {
			return Integer.toString((int) value + 1);
		}
	}

	/// Formatter factory that always supplies a [ZeroBasedFormatter] instance for
	/// the button index spinner field.
	///
	/// Overrides [DefaultFormatterFactory#getFormatter(JFormattedTextField)] to
	/// ensure the one-based display conversion is applied regardless of the field
	/// state.
	private static final class ZeroBasedFormatterFactory extends DefaultFormatterFactory {

		@Serial
		private static final long serialVersionUID = -7273246342105584827L;

		@Override
		public AbstractFormatter getFormatter(final JFormattedTextField tf) {
			return new ZeroBasedFormatter();
		}
	}
}

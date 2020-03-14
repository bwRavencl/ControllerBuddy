/* Copyright (C) 2019  Matteo Hausner
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
import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JPanel;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.IAction;

public final class ButtonEditorBuilder extends NumberEditorBuilder<Integer> {

	private static final class ZeroBasedFormatter extends DefaultFormatter {

		private static final long serialVersionUID = -7229427356426148291L;

		@Override
		public Object stringToValue(final String text) throws ParseException {
			return Integer.parseInt(text) - 1;
		}

		@Override
		public String valueToString(final Object value) throws ParseException {
			return Integer.toString((int) value + 1);
		}
	}

	private static final class ZeroBasedFormatterFactory extends DefaultFormatterFactory {

		private static final long serialVersionUID = -7273246342105584827L;

		@Override
		public AbstractFormatter getFormatter(final JFormattedTextField tf) {
			return new ZeroBasedFormatter();
		}
	}

	public ButtonEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws NoSuchFieldException, SecurityException,
			NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
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
}

/* Copyright (C) 2019  Matteo Hausner
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
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JPanel;

abstract class ArrayEditorBuilder<T> extends EditorBuilder {

	private static final Logger log = Logger.getLogger(ArrayEditorBuilder.class.getName());

	JComboBox<T> comboBox;

	ArrayEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action, final String fieldName,
			final Class<?> fieldType) throws SecurityException, NoSuchMethodException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
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

	abstract T[] getValues();

	private static final class JComboBoxSetPropertyAction extends PropertySetterAction {

		@Serial
		private static final long serialVersionUID = 1938012378184518954L;

		JComboBoxSetPropertyAction(final IAction<?> action, final Method setterMethod) {
			super(action, setterMethod);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				setterMethod.invoke(action, ((JComboBox<?>) e.getSource()).getSelectedItem());
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				log.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}
}

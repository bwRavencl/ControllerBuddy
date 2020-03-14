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

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.input.action.IAction;

abstract class ArrayEditorBuilder<T> extends EditorBuilder {

	private final class JComboBoxSetPropertyAction extends PropertySetterAction {

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

	private static final Logger log = Logger.getLogger(ArrayEditorBuilder.class.getName());

	ArrayEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action, final String fieldName,
			final Class<?> fieldType) throws NoSuchFieldException, SecurityException, NoSuchMethodException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var comboBox = new JComboBox<>(getValues());
		comboBox.setAction(new JComboBoxSetPropertyAction(action, setterMethod));
		comboBox.setSelectedItem(initialValue);
		parentPanel.add(comboBox);
	}

	abstract T[] getValues();
}

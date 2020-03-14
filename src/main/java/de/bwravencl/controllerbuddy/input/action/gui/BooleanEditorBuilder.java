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

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.input.action.IAction;

public class BooleanEditorBuilder extends EditorBuilder {

	private static final class JCheckBoxSetPropertyAction extends PropertySetterAction {

		private static final long serialVersionUID = -33052386834598414L;

		private JCheckBoxSetPropertyAction(final IAction<?> action, final Method setterMethod) {
			super(action, setterMethod);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				final var selected = ((JCheckBox) e.getSource()).isSelected();
				setterMethod.invoke(action, selected);
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				log.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}

	private static final Logger log = Logger.getLogger(BooleanEditorBuilder.class.getName());

	protected JCheckBox checkBox;

	public BooleanEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws NoSuchFieldException, SecurityException,
			NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		checkBox = new JCheckBox(new JCheckBoxSetPropertyAction(action, setterMethod));
		checkBox.setSelected((boolean) initialValue);
		parentPanel.add(checkBox);
	}
}

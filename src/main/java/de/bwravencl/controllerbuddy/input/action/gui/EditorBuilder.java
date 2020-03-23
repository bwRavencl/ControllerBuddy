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

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.action.IAction;

public abstract class EditorBuilder {

	static abstract class PropertySetterAction extends AbstractAction {

		private static final long serialVersionUID = 4141747329971720525L;

		final IAction<?> action;
		final Method setterMethod;

		PropertySetterAction(final IAction<?> action, final Method setterMethod) {
			this.action = action;
			this.setterMethod = setterMethod;
		}
	}

	static abstract class PropertySetterChangeListener implements ChangeListener {

		final IAction<?> action;
		final Method setterMethod;

		PropertySetterChangeListener(final IAction<?> action, final Method setterMethod) {
			this.action = action;
			this.setterMethod = setterMethod;
		}
	}

	protected final EditActionsDialog editActionsDialog;
	protected final IAction<?> action;
	protected final Class<?> fieldType;
	protected final Method setterMethod;
	protected Object initialValue;

	EditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action, final String fieldName,
			final Class<?> fieldType) throws NoSuchFieldException, SecurityException, NoSuchMethodException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		this.editActionsDialog = editActionsDialog;
		this.action = action;
		this.fieldType = fieldType;

		final var clazz = action.getClass();

		final var fieldNameChars = fieldName.toCharArray();
		fieldNameChars[0] = Character.toUpperCase(fieldNameChars[0]);
		final var capizalizedFieldName = String.valueOf(fieldNameChars);

		setterMethod = clazz.getMethod("set" + capizalizedFieldName, fieldType);

		final var getterMethodPrefix = fieldType == boolean.class || fieldType == Boolean.class ? "is" : "get";
		final var modeProperty = fieldType == Mode.class;

		final var getterParams = modeProperty ? new Class[] { Input.class } : null;
		final Method getterMethod = clazz.getMethod(getterMethodPrefix + capizalizedFieldName, getterParams);

		final var getterArgs = modeProperty ? new Object[] { editActionsDialog.getInput() } : null;
		initialValue = getterMethod.invoke(action, getterArgs);
	}

	public abstract void buildEditor(final JPanel parentPanel);
}

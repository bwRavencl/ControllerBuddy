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
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.action.IAction;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.AbstractAction;
import javax.swing.JPanel;

public abstract class EditorBuilder {

	protected final IAction<?> action;

	protected final EditActionsDialog editActionsDialog;

	protected final Method setterMethod;

	protected Object initialValue;

	EditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action, final String fieldName,
			final Class<?> fieldType) throws SecurityException, NoSuchMethodException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		this.editActionsDialog = editActionsDialog;
		this.action = action;

		final var clazz = action.getClass();

		final var fieldNameChars = fieldName.toCharArray();
		fieldNameChars[0] = Character.toUpperCase(fieldNameChars[0]);
		final var capitalizedFieldName = String.valueOf(fieldNameChars);

		setterMethod = clazz.getMethod("set" + capitalizedFieldName, fieldType);

		final var getterMethodPrefix = fieldType == boolean.class || fieldType == Boolean.class ? "is" : "get";
		final var modeProperty = fieldType == Mode.class;

		final var getterParams = modeProperty ? new Class<?>[] { Input.class } : null;
		final var getterMethod = clazz.getMethod(getterMethodPrefix + capitalizedFieldName, getterParams);

		final var getterArgs = modeProperty ? new Object[] { editActionsDialog.getInput() } : null;
		initialValue = getterMethod.invoke(action, getterArgs);
	}

	public abstract void buildEditor(final JPanel parentPanel);

	abstract static class PropertySetter {

		final IAction<?> action;

		final Method setterMethod;

		PropertySetter(final IAction<?> action, final Method setterMethod) {
			this.action = action;
			this.setterMethod = setterMethod;
		}
	}

	abstract static class PropertySetterAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 4141747329971720525L;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		final IAction<?> action;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		final Method setterMethod;

		PropertySetterAction(final IAction<?> action, final Method setterMethod) {
			this.action = action;
			this.setterMethod = setterMethod;
		}

		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(PropertySetterAction.class.getName());
		}

		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(PropertySetterAction.class.getName());
		}
	}
}

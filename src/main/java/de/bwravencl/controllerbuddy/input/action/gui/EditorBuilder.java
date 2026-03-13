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

/// Abstract base class for building property editors in the action editing
/// dialog.
///
/// Subclasses implement the builder pattern to create Swing editor components
/// for specific property types (boolean, number, string, array, keystroke,
/// etc.). Each builder uses reflection to discover getter and setter methods on
/// an [IAction] instance and wires up UI components to invoke the setter when
/// the user changes a value.
public abstract class EditorBuilder {

	/// The action whose property is being edited.
	protected final IAction<?> action;

	/// The parent dialog hosting the editor.
	protected final EditActionsDialog editActionsDialog;

	/// The setter method used to apply the new property value to the action.
	protected final Method setterMethod;

	/// The initial value of the property, read from the action at construction
	/// time.
	protected Object initialValue;

	/// Constructs an editor builder by resolving the getter and setter methods for
	/// the given field via reflection and reading the initial property value.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws IllegalAccessException if the getter or setter method is not
	/// accessible
	/// @throws InvocationTargetException if the getter method throws an exception
	/// @throws NoSuchMethodException if the getter or setter method is not found
	EditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action, final String fieldName,
			final Class<?> fieldType) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
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

	/// Builds the editor UI component and adds it to the given parent panel.
	///
	/// @param parentPanel the panel to which the editor component is added
	public abstract void buildEditor(final JPanel parentPanel);

	/// Base class that holds a reference to an action and its setter method for
	/// use by property-setting listeners.
	///
	/// Concrete subclasses wire up specific listener types (e.g., change
	/// listeners, selection listeners) to invoke the setter when the user
	/// modifies the editor component.
	abstract static class PropertySetter {

		/// The action whose property is being set.
		final IAction<?> action;

		/// The setter method to invoke when the property changes.
		final Method setterMethod;

		/// Constructs a property setter holding references to the action and its
		/// setter.
		///
		/// @param action the action whose property is being set
		/// @param setterMethod the setter method to invoke when the property changes
		PropertySetter(final IAction<?> action, final Method setterMethod) {
			this.action = action;
			this.setterMethod = setterMethod;
		}
	}

	/// Abstract Swing action that holds a reference to an action and its setter
	/// method for use by property-setting action listeners.
	///
	/// Not serializable by design; `readObject` and `writeObject` both throw
	/// `NotSerializableException` to prevent accidental serialization.
	abstract static class PropertySetterAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 4141747329971720525L;

		/// The action whose property is being set.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		final IAction<?> action;

		/// The setter method to invoke when the action is performed.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		final Method setterMethod;

		/// Constructs a property setter action holding references to the action and its
		/// setter.
		///
		/// @param action the action whose property is being set
		/// @param setterMethod the setter method to invoke when the action is performed
		PropertySetterAction(final IAction<?> action, final Method setterMethod) {
			this.action = action;
			this.setterMethod = setterMethod;
		}

		/// Prevents deserialization.
		///
		/// @param ignoredStream the object input stream (ignored)
		/// @throws NotSerializableException always
		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(PropertySetterAction.class.getName());
		}

		/// Prevents serialization.
		///
		/// @param ignoredStream the object output stream (ignored)
		/// @throws NotSerializableException always
		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(PropertySetterAction.class.getName());
		}
	}
}

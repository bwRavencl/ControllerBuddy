/*
 * Copyright (C) 2014 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import java.lang.constant.Constable;

/// Root interface for all controller input actions in the action type
/// hierarchy.
///
/// An action transforms raw controller input (axis values, button states) into
/// virtual output events. All actions are parameterized by their value type `V`
/// and support cloning.
///
/// @param <V> the type of input value this action processes
public interface IAction<V extends Constable> extends Cloneable {

	/// Returns a default human-readable description for the given action, derived
	/// from its
	/// [Action][de.bwravencl.controllerbuddy.input.action.annotation.Action]
	/// annotation title.
	///
	/// @param action the action to describe
	/// @return the localized default description string
	static String getDefaultDescription(final IAction<?> action) {
		return getLabel(action.getClass());
	}

	/// Returns the localized label for the given action class, based on its
	/// [Action][de.bwravencl.controllerbuddy.input.action.annotation.Action]
	/// annotation. The label is prefixed with the action's UTF-8 icon character.
	///
	/// @param actionClass the action class to retrieve the label for
	/// @return the localized label string, prefixed with the action's icon
	/// @throws RuntimeException if the class is missing, the Action annotation
	static String getLabel(final Class<?> actionClass) {
		final var annotation = actionClass.getAnnotation(Action.class);
		if (annotation == null) {
			throw new RuntimeException(
					actionClass.getName() + ": missing " + Action.class.getSimpleName() + " annotation");
		}

		return Main.STRINGS.getString(annotation.title());
	}

	/// Creates and returns a copy of this action.
	///
	/// @return a clone of this action
	/// @throws CloneNotSupportedException if cloning is not supported
	Object clone() throws CloneNotSupportedException;

	/// Executes this action, transforming the given input value into virtual output
	/// events.
	///
	/// @param input the current input state
	/// @param component the index of the input component (axis or button)
	/// triggering this action
	/// @param value the current input value
	void doAction(final Input input, int component, V value);

	/// Returns a human-readable description of this action for display in the UI.
	///
	/// @param input the current input state
	/// @return the description string
	String getDescription(final Input input);
}

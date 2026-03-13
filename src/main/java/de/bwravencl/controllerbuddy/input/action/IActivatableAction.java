/*
 * Copyright (C) 2020 Matteo Hausner
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
import java.lang.constant.Constable;

/// Interface for actions that support different activation modes (while
/// pressed, on press, on release).
///
/// Extends [IInitializationAction] to add configurable activation behavior and
/// an activatable state that controls whether the action is currently allowed
/// to fire. Other actions (such as delayed actions) may deny activation by
/// setting the activatable state.
///
/// @param <V> the type of input value this action processes
public interface IActivatableAction<V extends Constable> extends IInitializationAction<V> {

	/// Returns the current activatable state of this action.
	///
	/// @return the activatable state
	Activatable getActivatable();

	/// Returns the configured activation mode for this action.
	///
	/// @return the activation mode
	Activation getActivation();

	/// Initializes the activatable state based on the configured activation mode.
	///
	/// @param input the current input state
	@Override
	default void init(final Input input) {
		setActivatable(getActivation() == Activation.ON_RELEASE ? Activatable.NO : Activatable.YES);
	}

	/// Sets the activatable state of this action.
	///
	/// @param activatable the new activatable state
	void setActivatable(final Activatable activatable);

	/// Sets the activation mode for this action.
	///
	/// @param activation the new activation mode
	void setActivation(final Activation activation);

	/// Represents whether an action is currently allowed to activate.
	///
	/// The state transitions are managed by the input pipeline and by cooperating
	/// actions that may deny activation to enforce mutual exclusion.
	enum Activatable {

		/// The action is allowed to activate.
		YES,

		/// The action is not allowed to activate.
		NO,

		/// The action is denied because another action took priority.
		DENIED_BY_OTHER_ACTION,

		/// The action always activates regardless of other conditions.
		ALWAYS
	}

	/// Defines when an action should fire relative to the input event.
	///
	/// Each constant carries a localization key and a symbolic character used for
	/// compact display in the profile editor UI.
	enum Activation {

		/// Fires continuously while the input is held.
		WHILE_PRESSED("ACTIVATION_WHILE_PRESSED", "↦"),

		/// Fires once when the input is first pressed.
		ON_PRESS("ACTIVATION_ON_PRESS", "⤓"),

		/// Fires once when the input is released.
		ON_RELEASE("ACTIVATION_ON_RELEASE", "⤒");

		/// The localized label string for this activation mode.
		private final String label;

		/// The symbolic character representing this activation mode in the UI.
		private final String symbol;

		/// Creates an `Activation` constant with a localized label and a display
		/// symbol.
		///
		/// @param labelKey the resource bundle key used to look up the localized label
		/// @param symbol the symbolic character representing this activation mode
		Activation(final String labelKey, final String symbol) {
			label = Main.STRINGS.getString(labelKey);
			this.symbol = symbol;
		}

		/// Returns the symbol character representing this activation mode in the UI.
		///
		/// @return the activation mode symbol
		public String getSymbol() {
			return symbol;
		}

		/// Returns the localized label for this activation mode.
		///
		/// @return the localized label string
		@Override
		public String toString() {
			return label;
		}
	}
}

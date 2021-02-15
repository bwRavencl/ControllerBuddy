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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;

public interface IActivatableAction<V extends Number> extends IInitializationAction<V> {

	enum Activatable {
		YES, NO, DENIED_BY_OTHER_ACTION, ALWAYS
	}

	enum Activation {

		REPEAT("ACTIVATION_REPEAT"), SINGLE_IMMEDIATELY("ACTIVATION_SINGLE_IMMEDIATELY"),
		SINGLE_ON_RELEASE("ACTIVATION_SINGLE_ON_RELEASE");

		private final String label;

		Activation(final String labelKey) {
			label = Main.strings.getString(labelKey);
		}

		@Override
		public String toString() {
			return label;
		}
	}

	Activatable getActivatable();

	Activation getActivation();

	@Override
	default void init(final Input input) {
		setActivatable(getActivation() == Activation.SINGLE_ON_RELEASE ? Activatable.NO : Activatable.YES);
	}

	void setActivatable(final Activatable activatable);

	void setActivation(final Activation activation);
}

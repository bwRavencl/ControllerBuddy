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

import static de.bwravencl.controllerbuddy.gui.Main.strings;

import de.bwravencl.controllerbuddy.input.Input;

public interface IActivatableInputAction<V extends Number> extends IActivatableAction, IInitializationAction<V> {

	enum Activation {
		REPEAT {

			@Override
			public String toString() {
				return strings.getString("ACTIVATION_REPEAT");
			}
		},
		SINGLE_STROKE {

			@Override
			public String toString() {
				return strings.getString("ACTIVATION_SINGLE_STROKE");
			}
		},
		SINGLE_STROKE_ON_RELEASE {

			@Override
			public String toString() {
				return strings.getString("ACTIVATION_SINGLE_STROKE_ON_RELEASE");
			}
		}
	}

	Activation getActivation();

	@Override
	default void init(final Input input) {
		setActivatable(getActivation() == Activation.SINGLE_STROKE_ON_RELEASE ? Activatable.NO : Activatable.YES);
	}

	void setActivation(final Activation activation);
}

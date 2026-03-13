/*
 * Copyright (C) 2017 Matteo Hausner
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

import de.bwravencl.controllerbuddy.input.Input;
import java.lang.constant.Constable;

/// Interface for actions that require initialization before they can process
/// input.
///
/// Extends [IAction] to provide an initialization hook. Implementing actions
/// use [#init(Input)] to reset the transient state to well-defined initial
/// values.
///
/// @param <V> the type of input value this action processes
public interface IInitializationAction<V extends Constable> extends IAction<V> {

	/// Initializes or resets the transient state of this action.
	///
	/// @param input the current input state
	void init(final Input input);
}

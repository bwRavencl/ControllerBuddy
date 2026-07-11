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

import java.lang.constant.Constable;

/// Interface for actions whose activation can be delayed by a configurable
/// duration.
///
/// Extends [IAction] to add delay support. When a delay greater than zero is
/// configured, the action is considered "delayed" and will only fire after the
/// input has been held for at least the specified number of milliseconds.
///
/// @param <V> the type of input value this action processes
public interface IDelayableAction<V extends Constable> extends IAction<V> {

	/// Default delay duration in milliseconds (no delay).
	long DEFAULT_DELAY = 0L;

	/// Symbol indicating a delayed action.
	String DELAYED_SYMBOL = "⟿";

	/// Symbol indicating an instant (non-delayed) action.
	String INSTANT_SYMBOL = "⇝";

	/// Returns the delay duration in milliseconds.
	///
	/// @return the delay in milliseconds
	long getDelay();

	/// Returns whether this action has a delay configured (delay greater than
	/// zero).
	///
	/// @return `true` if the delay is greater than zero
	default boolean isDelayed() {
		return getDelay() > 0L;
	}

	/// Sets the delay duration in milliseconds.
	///
	/// @param delay the delay in milliseconds
	void setDelay(final long delay);
}

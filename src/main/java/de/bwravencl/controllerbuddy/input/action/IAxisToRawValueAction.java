/*
 * Copyright (C) 2026 Matteo Hausner
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

import de.bwravencl.controllerbuddy.input.ControllerState;

/// Interface for axis-triggered actions that support the usage of raw axis
/// values.
///
/// Extends [IAxisToAction] to provide configurable usage of raw axis input.
/// When enabled, the action receives the unprocessed axis value directly from
/// the controller state, bypassing any global transformations.
public interface IAxisToRawValueAction extends IAxisToAction {

	/// Default setting for whether to use the raw axis value.
	boolean DEFAULT_USE_RAW_VALUE = false;

	/// Handles the use of the raw axis value if configured to do so, returning
	/// either the raw value or the provided value.
	///
	/// @param component the axis component index
	/// @param value the current axis value
	/// @param controllerState the current gamepad state
	/// @return the axis value
	default float handleUseRawValue(final int component, final Float value, final ControllerState controllerState) {
		if (isUseRawValue()) {
			return controllerState.getRawAxes()[component];
		}

		return value;
	}

	/// Returns whether the raw axis value is used without transformations.
	///
	/// @return `true` if the raw value is used
	boolean isUseRawValue();

	/// Sets whether the raw axis value is used without transformations.
	///
	/// @param useRawValue `true` to use the raw value
	void setUseRawValue(boolean useRawValue);
}

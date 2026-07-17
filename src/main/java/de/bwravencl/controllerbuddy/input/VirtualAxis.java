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

package de.bwravencl.controllerbuddy.input;

import de.bwravencl.controllerbuddy.input.OverlayAxis.OverlayAxisOrientation;
import de.bwravencl.controllerbuddy.input.OverlayAxis.OverlayAxisStyle;

/// Represents the virtual axes available for input mapping.
///
/// Each constant corresponds to one axis channel on the virtual device and
/// carries default overlay orientation and rendering style metadata used
/// when displaying the axis value in the on-screen overlay.
public enum VirtualAxis {

	/// X axis (left stick horizontal).
	X(OverlayAxisOrientation.HORIZONTAL, OverlayAxisStyle.LINE),
	/// Y axis (left stick vertical).
	Y(OverlayAxisOrientation.VERTICAL, OverlayAxisStyle.SOLID),
	/// Z axis (additional axis).
	Z(OverlayAxisOrientation.VERTICAL, OverlayAxisStyle.SOLID),
	/// RX axis (right stick horizontal).
	RX(OverlayAxisOrientation.HORIZONTAL, OverlayAxisStyle.LINE),
	/// RY axis (right stick vertical).
	RY(OverlayAxisOrientation.VERTICAL, OverlayAxisStyle.SOLID),
	/// RZ axis (additional rotational axis).
	RZ(OverlayAxisOrientation.HORIZONTAL, OverlayAxisStyle.LINE),
	/// Slider 0 axis.
	S0(OverlayAxisOrientation.VERTICAL, OverlayAxisStyle.SOLID),
	/// Slider 1 axis.
	S1(OverlayAxisOrientation.VERTICAL, OverlayAxisStyle.SOLID);

	/// The default overlay orientation for this virtual axis.
	private final OverlayAxisOrientation defaultOverlayAxisOrientation;

	/// The default overlay rendering style for this virtual axis.
	private final OverlayAxisStyle defaultOverlayAxisStyle;

	/// Constructs a [VirtualAxis] constant with the given default overlay
	/// orientation and rendering style.
	///
	/// @param defaultOverlayAxisOrientation the default overlay orientation for
	/// this axis
	/// @param defaultOverlayAxisStyle the default overlay rendering style for this
	/// axis
	VirtualAxis(final OverlayAxisOrientation defaultOverlayAxisOrientation,
			final OverlayAxisStyle defaultOverlayAxisStyle) {
		this.defaultOverlayAxisOrientation = defaultOverlayAxisOrientation;
		this.defaultOverlayAxisStyle = defaultOverlayAxisStyle;
	}

	/// Returns the default overlay orientation for this virtual axis.
	///
	/// @return the default overlay axis orientation
	public OverlayAxisOrientation getDefaultOrientation() {
		return defaultOverlayAxisOrientation;
	}

	/// Returns the default overlay rendering style for this virtual axis.
	///
	/// @return the default overlay axis style
	public OverlayAxisStyle getDefaultStyle() {
		return defaultOverlayAxisStyle;
	}
}

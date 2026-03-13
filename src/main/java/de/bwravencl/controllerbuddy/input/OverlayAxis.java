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

package de.bwravencl.controllerbuddy.input;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import java.awt.Color;
import javax.swing.SwingConstants;

/// Represents the visual configuration for a virtual axis displayed on the
/// overlay.
///
/// Each overlay axis has a color, orientation, style, and an inversion flag
/// that control how the axis indicator is rendered on screen.
public final class OverlayAxis implements Cloneable {

	/// Color used to render the axis indicator on the overlay.
	private Color color;

	/// Whether the axis direction is inverted.
	private boolean inverted;

	/// Orientation of the axis indicator on the overlay.
	private OverlayAxisOrientation orientation;

	/// Rendering style of the axis indicator on the overlay.
	private OverlayAxisStyle style;

	/// Creates an overlay axis with default settings derived from the given virtual
	/// axis.
	///
	/// @param virtualAxis the virtual axis whose default orientation and style are
	/// used
	public OverlayAxis(final VirtualAxis virtualAxis) {
		this(Color.BLACK, virtualAxis.getDefaultOrientation(), virtualAxis.getDefaultStyle(), false);
	}

	/// Creates an overlay axis with the specified color, orientation, style, and
	/// inversion flag.
	///
	/// @param color the color used to render the axis indicator
	/// @param orientation the orientation of the axis indicator
	/// @param style the rendering style of the axis indicator
	/// @param inverted `true` if the axis direction should be inverted
	private OverlayAxis(final Color color, final OverlayAxisOrientation orientation, final OverlayAxisStyle style,
			final boolean inverted) {
		this.color = color;
		this.orientation = orientation;
		this.style = style;
		this.inverted = inverted;
	}

	/// Creates a deep copy of this overlay axis, including a copy of the color.
	///
	/// @return a new [OverlayAxis] with the same configuration
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public Object clone() {
		return new OverlayAxis(new Color(color.getRGB(), true), orientation, style, inverted);
	}

	/// Returns the color used to render this axis on the overlay.
	///
	/// @return the axis color
	public Color getColor() {
		return color;
	}

	/// Returns the orientation of this axis on the overlay.
	///
	/// @return the axis orientation
	public OverlayAxisOrientation getOrientation() {
		return orientation;
	}

	/// Returns the rendering style of this axis on the overlay.
	///
	/// @return the axis style
	public OverlayAxisStyle getStyle() {
		return style;
	}

	/// Returns whether this axis is inverted.
	///
	/// @return `true` if the axis direction is inverted
	public boolean isInverted() {
		return inverted;
	}

	/// Sets the color used to render this axis on the overlay.
	///
	/// @param color the axis color
	public void setColor(final Color color) {
		this.color = color;
	}

	/// Sets whether this axis is inverted.
	///
	/// @param inverted `true` to invert the axis direction
	public void setInverted(final boolean inverted) {
		this.inverted = inverted;
	}

	/// Sets the orientation of this axis on the overlay.
	///
	/// @param orientation the axis orientation
	public void setOrientation(final OverlayAxisOrientation orientation) {
		this.orientation = orientation;
	}

	/// Sets the rendering style of this axis on the overlay.
	///
	/// @param style the axis style
	public void setStyle(final OverlayAxisStyle style) {
		this.style = style;
	}

	/// Defines the orientation of an overlay axis indicator.
	///
	/// Each constant maps to the corresponding [SwingConstants] value used when
	/// rendering the overlay progress bar.
	public enum OverlayAxisOrientation {

		/// Horizontal axis orientation.
		HORIZONTAL("OVERLAY_AXIS_ORIENTATION_HORIZONTAL"),
		/// Vertical axis orientation.
		VERTICAL("OVERLAY_AXIS_ORIENTATION_VERTICAL");

		/// Localized display label for this orientation.
		private final String label;

		/// Creates an orientation constant with the localized label resolved from
		/// the given resource bundle key.
		///
		/// @param labelKey the resource bundle key used to look up the display label
		OverlayAxisOrientation(final String labelKey) {
			label = Main.STRINGS.getString(labelKey);
		}

		@Override
		public String toString() {
			return label;
		}

		/// Converts this orientation to the corresponding [SwingConstants] value.
		///
		/// @return [SwingConstants#HORIZONTAL] or [SwingConstants#VERTICAL]
		public int toSwingConstant() {
			return switch (this) {
			case HORIZONTAL -> SwingConstants.HORIZONTAL;
			case VERTICAL -> SwingConstants.VERTICAL;
			};
		}
	}

	/// Defines the rendering style of an overlay axis indicator.
	///
	/// The style controls how the axis value is visually represented in the
	/// on-screen overlay - either as a solid filled bar or as a thin line marker.
	public enum OverlayAxisStyle {

		/// Solid-fill rendering for the axis indicator.
		SOLID("OVERLAY_AXIS_STYLE_SOLID"),
		/// Line-style rendering for the axis indicator.
		LINE("OVERLAY_AXIS_STYLE_LINE");

		/// Localized display label for this style.
		private final String label;

		/// Creates a style constant with the localized label resolved from the given
		/// resource bundle key.
		///
		/// @param labelKey the resource bundle key used to look up the display label
		OverlayAxisStyle(final String labelKey) {
			label = Main.STRINGS.getString(labelKey);
		}

		@Override
		public String toString() {
			return label;
		}
	}
}

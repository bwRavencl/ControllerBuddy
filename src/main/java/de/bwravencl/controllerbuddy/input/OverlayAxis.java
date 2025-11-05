/* Copyright (C) 2019  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import java.awt.Color;
import javax.swing.SwingConstants;

public final class OverlayAxis implements Cloneable {

	private Color color;

	private boolean inverted;

	private OverlayAxisOrientation orientation;

	private OverlayAxisStyle style;

	public OverlayAxis(final VirtualAxis virtualAxis) {
		this(Color.BLACK, virtualAxis.getDefaultOrientation(), virtualAxis.getDefaultStyle(), false);
	}

	private OverlayAxis(final Color color, final OverlayAxisOrientation orientation, final OverlayAxisStyle style,
			final boolean inverted) {
		this.color = color;
		this.orientation = orientation;
		this.style = style;
		this.inverted = inverted;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public Object clone() {
		return new OverlayAxis(new Color(color.getRGB(), true), orientation, style, inverted);
	}

	public Color getColor() {
		return color;
	}

	public OverlayAxisOrientation getOrientation() {
		return orientation;
	}

	public OverlayAxisStyle getStyle() {
		return style;
	}

	public boolean isInverted() {
		return inverted;
	}

	public void setColor(final Color color) {
		this.color = color;
	}

	public void setInverted(final boolean inverted) {
		this.inverted = inverted;
	}

	public void setOrientation(final OverlayAxisOrientation orientation) {
		this.orientation = orientation;
	}

	public void setStyle(final OverlayAxisStyle style) {
		this.style = style;
	}

	public enum OverlayAxisOrientation {

		HORIZONTAL("OVERLAY_AXIS_ORIENTATION_HORIZONTAL"), VERTICAL("OVERLAY_AXIS_ORIENTATION_VERTICAL");

		private final String label;

		OverlayAxisOrientation(final String labelKey) {
			label = Main.STRINGS.getString(labelKey);
		}

		@Override
		public String toString() {
			return label;
		}

		public int toSwingConstant() {
			return switch (this) {
			case HORIZONTAL -> SwingConstants.HORIZONTAL;
			case VERTICAL -> SwingConstants.VERTICAL;
			};
		}
	}

	public enum OverlayAxisStyle {

		SOLID("OVERLAY_AXIS_STYLE_SOLID"), LINE("OVERLAY_AXIS_STYLE_LINE");

		private final String label;

		OverlayAxisStyle(final String labelKey) {
			label = Main.STRINGS.getString(labelKey);
		}

		@Override
		public String toString() {
			return label;
		}
	}
}

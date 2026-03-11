/* Copyright (C) 2026  Matteo Hausner
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

import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.OverlayAxis.OverlayAxisOrientation;
import java.awt.Color;
import javax.swing.SwingConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OverlayAxisTest {

	@Nested
	@DisplayName("clone()")
	class CloneTests {

		@Test
		@DisplayName("color in the clone is a distinct Color object with the same RGB value")
		void clonesColorAsDistinctObjectWithSameRgb() {
			final var original = new OverlayAxis(VirtualAxis.X);
			final var clone = (OverlayAxis) original.clone();

			Assertions.assertNotSame(original.getColor(), clone.getColor());
			Assertions.assertEquals(original.getColor().getRGB(), clone.getColor().getRGB());
		}

		@Test
		@DisplayName("orientation, style, and inverted are equal to those of the original")
		void preservesOrientationStyleAndInverted() {
			final var original = new OverlayAxis(VirtualAxis.Y);
			original.setInverted(true);
			final var clone = (OverlayAxis) original.clone();

			Assertions.assertEquals(original.getOrientation(), clone.getOrientation());
			Assertions.assertEquals(original.getStyle(), clone.getStyle());
			Assertions.assertEquals(original.isInverted(), clone.isInverted());
		}

		@Test
		@DisplayName("returns an OverlayAxis that is not the same instance as the original")
		void returnsDistinctInstance() {
			final var original = new OverlayAxis(VirtualAxis.X);
			Assertions.assertNotSame(original, original.clone());
		}
	}

	@Nested
	@DisplayName("OverlayAxis(VirtualAxis)")
	class ConstructorTests {

		@Test
		@DisplayName("sets color to Color.BLACK")
		void setsColorToBlack() {
			Assertions.assertEquals(Color.BLACK, new OverlayAxis(VirtualAxis.X).getColor());
		}

		@Test
		@DisplayName("sets inverted to false")
		void setsInvertedToFalse() {
			Assertions.assertFalse(new OverlayAxis(VirtualAxis.X).isInverted());
		}

		@Test
		@DisplayName("sets orientation from the VirtualAxis default orientation")
		void setsOrientationFromVirtualAxis() {
			// VirtualAxis.X has HORIZONTAL orientation; VirtualAxis.Y has VERTICAL
			Assertions.assertEquals(VirtualAxis.X.getDefaultOrientation(),
					new OverlayAxis(VirtualAxis.X).getOrientation());
			Assertions.assertEquals(VirtualAxis.Y.getDefaultOrientation(),
					new OverlayAxis(VirtualAxis.Y).getOrientation());
		}

		@Test
		@DisplayName("sets style from the VirtualAxis default style")
		void setsStyleFromVirtualAxis() {
			// VirtualAxis.X has LINE style; VirtualAxis.Y has SOLID style
			Assertions.assertEquals(VirtualAxis.X.getDefaultStyle(), new OverlayAxis(VirtualAxis.X).getStyle());
			Assertions.assertEquals(VirtualAxis.Y.getDefaultStyle(), new OverlayAxis(VirtualAxis.Y).getStyle());
		}
	}

	@Nested
	@DisplayName("OverlayAxisOrientation.toSwingConstant()")
	class OverlayAxisOrientationToSwingConstantTests {

		@Test
		@DisplayName("HORIZONTAL maps to SwingConstants.HORIZONTAL")
		void horizontalMapsToSwingConstantsHorizontal() {
			Assertions.assertEquals(SwingConstants.HORIZONTAL, OverlayAxisOrientation.HORIZONTAL.toSwingConstant());
		}

		@Test
		@DisplayName("VERTICAL maps to SwingConstants.VERTICAL")
		void verticalMapsToSwingConstantsVertical() {
			Assertions.assertEquals(SwingConstants.VERTICAL, OverlayAxisOrientation.VERTICAL.toSwingConstant());
		}
	}
}

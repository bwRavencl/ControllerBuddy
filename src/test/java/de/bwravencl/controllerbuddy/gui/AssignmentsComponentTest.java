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

package de.bwravencl.controllerbuddy.gui;

import com.formdev.flatlaf.ui.FlatButtonUI;
import de.bwravencl.controllerbuddy.input.Mode.Component;
import de.bwravencl.controllerbuddy.input.Mode.Component.ComponentType;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lwjgl.sdl.SDLGamepad;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class AssignmentsComponentTest {

	@Mock
	Main mockMain;

	/// Creates a `CompoundButton` (the only concrete sub-final class of the
	/// private-abstract `CustomButton`) via reflection, using the left-stick button
	/// component so the constructor follows the
	/// [ComponentType.BUTTON]/[SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK] branch.
	private Object createCompoundButton() throws ReflectiveOperationException {
		final var compoundButtonClass = Arrays.stream(AssignmentsComponent.class.getDeclaredClasses())
				.filter(c -> "CompoundButton".equals(c.getSimpleName())).findFirst().orElseThrow();
		final var constructor = compoundButtonClass.getDeclaredConstructor(Main.class, JPanel.class, Component.class);
		constructor.setAccessible(true);
		final var component = new Component(mockMain, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK);
		return constructor.newInstance(mockMain, new JPanel(), component);
	}

	/// Invokes `CustomButton.paintText` (declared on the super-final class of
	/// `CompoundButton`) via reflection.
	private void invokePaintText(final Object button, final Graphics g, final Rectangle textRect, final String text)
			throws ReflectiveOperationException {
		final var customButtonClass = button.getClass().getSuperclass();
		final var method = customButtonClass.getDeclaredMethod("paintText", Graphics.class, Rectangle.class,
				String.class);
		method.setAccessible(true);
		method.invoke(button, g, textRect, text);
	}

	@Nested
	@DisplayName("checkDimensionIsSquare()")
	final class CheckDimensionIsSquareTests {

		@Test
		@DisplayName("does not throw for a square dimension")
		void doesNotThrowForSquareDimension() throws ReflectiveOperationException {
			invokeCheckDimensionIsSquare(new Dimension(50, 50));
		}

		private void invokeCheckDimensionIsSquare(final Dimension dimension) throws ReflectiveOperationException {
			final var method = AssignmentsComponent.class.getDeclaredMethod("checkDimensionIsSquare", Dimension.class);
			method.setAccessible(true);
			method.invoke(null, dimension);
		}

		@Test
		@DisplayName("throws IllegalArgumentException when width and height differ")
		void throwsForNonSquareDimension() throws ReflectiveOperationException {
			final var method = AssignmentsComponent.class.getDeclaredMethod("checkDimensionIsSquare", Dimension.class);
			method.setAccessible(true);
			Assertions.assertThrows(java.lang.reflect.InvocationTargetException.class,
					() -> method.invoke(null, new Dimension(50, 60)));
		}
	}

	@Nested
	@DisplayName("CustomButton.paintText()")
	final class CustomButtonPaintTextTests {

		private Object button;

		private Graphics graphics;

		@Test
		@DisplayName("when multiple spaces exist, the one closest to the centre is chosen as the split point")
		void centermostSpaceIsChosen() throws ReflectiveOperationException {
			// "A BC DEF": spaces at indices 1 and 4, length=8, centre=4.
			// Space at 4 has distance 0; space at 1 has distance 3 → split at 4.
			// First line: "A BC", second line: "DEF".
			try (final var flatMock = Mockito.mockStatic(FlatButtonUI.class)) {
				final var textCaptor = ArgumentCaptor.forClass(String.class);
				invokePaintText(button, graphics, new Rectangle(0, 0, 200, 50), "A BC DEF");
				flatMock.verify(() -> FlatButtonUI.paintText(Mockito.any(), Mockito.any(), Mockito.any(),
						textCaptor.capture(), Mockito.any()), Mockito.times(2));
				Assertions.assertEquals(List.of("A BC", "DEF"), textCaptor.getAllValues());
			}
		}

		@Test
		@DisplayName("when two spaces are equidistant from the centre, the earlier one is chosen")
		void firstEquidistantSpaceIsChosen() throws ReflectiveOperationException {
			// "A B C D E": spaces at 1, 3, 5, 7; length=9, centre=4.
			// Space at 3 (dist 1) and space at 5 (dist 1) are equidistant.
			// The algorithm keeps the first one it finds with strictly less distance,
			// so index 3 wins → first line: "A B", second line: "C D E".
			try (final var flatMock = Mockito.mockStatic(FlatButtonUI.class)) {
				final var textCaptor = ArgumentCaptor.forClass(String.class);
				invokePaintText(button, graphics, new Rectangle(0, 0, 200, 50), "A B C D E");
				flatMock.verify(() -> FlatButtonUI.paintText(Mockito.any(), Mockito.any(), Mockito.any(),
						textCaptor.capture(), Mockito.any()), Mockito.times(2));
				Assertions.assertEquals(List.of("A B", "C D E"), textCaptor.getAllValues());
			}
		}

		@BeforeEach
		void setUp() throws ReflectiveOperationException {
			Mockito.when(mockMain.isSwapLeftAndRightSticks()).thenReturn(false);
			button = createCompoundButton();
			final var image = new BufferedImage(200, 50, BufferedImage.TYPE_INT_ARGB);
			graphics = image.createGraphics();
		}

		@Test
		@DisplayName("text with a space is split into two lines at that space")
		void textWithOneSpaceIsSplitIntoTwoLines() throws ReflectiveOperationException {
			try (final var flatMock = Mockito.mockStatic(FlatButtonUI.class)) {
				final var textCaptor = ArgumentCaptor.forClass(String.class);
				invokePaintText(button, graphics, new Rectangle(0, 0, 200, 50), "hello world");
				flatMock.verify(() -> FlatButtonUI.paintText(Mockito.any(), Mockito.any(), Mockito.any(),
						textCaptor.capture(), Mockito.any()), Mockito.times(2));
				Assertions.assertEquals(List.of("hello", "world"), textCaptor.getAllValues());
			}
		}

		@Test
		@DisplayName("text without spaces renders as a single line")
		void textWithoutSpaceTakesSingleLinePath() throws ReflectiveOperationException {
			try (final var flatMock = Mockito.mockStatic(FlatButtonUI.class)) {
				invokePaintText(button, graphics, new Rectangle(0, 0, 200, 50), "NoSpaces");
				flatMock.verify(() -> FlatButtonUI.paintText(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
						Mockito.any()), Mockito.times(1));
			}
		}
	}
}

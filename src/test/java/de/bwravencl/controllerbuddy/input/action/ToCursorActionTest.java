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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToCursorActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("moveCursor()")
	class MoveCursorTests {

		private ButtonToCursorAction action;

		@Test
		@DisplayName("accumulates fractional values until they round to a non-zero integer")
		void accumulatesFractionalValues() {
			// 0.3 rounds to 0, so it should be accumulated in remainingD
			action.moveCursor(mockInput, 0.3f);
			Mockito.verify(mockInput, Mockito.never()).setCursorDeltaX(Mockito.anyInt());
			Assertions.assertEquals(0.3f, action.remainingD, 0.001f);

			// 0.3 + 0.3 = 0.6, rounds to 1
			action.moveCursor(mockInput, 0.3f);
			Mockito.verify(mockInput).setCursorDeltaX(1);
			// remainder: 0.6 - 1 = -0.4
			Assertions.assertEquals(-0.4f, action.remainingD, 0.001f);
		}

		@Test
		@DisplayName("inverts direction when invert is true")
		void invertsDirection() {
			action.setInvert(true);
			action.moveCursor(mockInput, 5.0f);
			// Inverted: -5.0 rounds to -5
			Mockito.verify(mockInput).setCursorDeltaX(-5);
		}

		@Test
		@DisplayName("moves cursor on X axis by default")
		void movesCursorOnXAxis() {
			action.moveCursor(mockInput, 3.0f);
			Mockito.verify(mockInput).setCursorDeltaX(3);
		}

		@Test
		@DisplayName("moves cursor on Y axis when axis is set to Y")
		void movesCursorOnYAxis() {
			action.setAxis(ToCursorAction.MouseAxis.Y);
			action.moveCursor(mockInput, 3.0f);
			Mockito.verify(mockInput).setCursorDeltaY(3);
		}

		@Test
		@DisplayName("preserves fractional remainder across multiple calls")
		void preservesRemainderAcrossCalls() {
			// 1.7 rounds to 2, remainder = 1.7 - 2 = -0.3
			action.moveCursor(mockInput, 1.7f);
			Mockito.verify(mockInput).setCursorDeltaX(2);
			Assertions.assertEquals(-0.3f, action.remainingD, 0.001f);

			// Next call: 1.7 + (-0.3) = 1.4, rounds to 1, remainder = 1.4 - 1 = 0.4
			action.moveCursor(mockInput, 1.7f);
			Mockito.verify(mockInput).setCursorDeltaX(1); // cumulative via getCursorDeltaX returning 0
			Assertions.assertEquals(0.4f, action.remainingD, 0.001f);
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToCursorAction();
		}
	}
}

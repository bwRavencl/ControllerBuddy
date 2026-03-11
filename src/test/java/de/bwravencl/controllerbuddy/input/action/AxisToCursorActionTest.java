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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AxisToCursorActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("doAction()")
	class DoActionTests {

		private AxisToCursorAction action;

		@Test
		@DisplayName("higher exponent increases non-linearity of cursor movement")
		void higherExponentIncreasesNonLinearity() {
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(1.0f);

			// With exponent=2 (quadratic), half-range input
			action.setExponent(2f);
			action.doAction(mockInput, 0, 0.5f);

			final var captorQuadratic = ArgumentCaptor.forClass(Integer.class);
			Mockito.verify(mockInput).setCursorDeltaX(captorQuadratic.capture());
			final var quadraticDelta = Math.abs(captorQuadratic.getValue());

			Mockito.clearInvocations(mockInput);

			// With exponent=1 (linear), same input
			final var linearAction = new AxisToCursorAction();
			linearAction.setDeadZone(0.1f);
			linearAction.setExponent(1f);
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(1.0f);
			linearAction.doAction(mockInput, 0, 0.5f);

			final var captorLinear = ArgumentCaptor.forClass(Integer.class);
			Mockito.verify(mockInput).setCursorDeltaX(captorLinear.capture());
			final var linearDelta = Math.abs(captorLinear.getValue());

			// Linear should produce larger delta than quadratic for sub-max inputs
			Assertions.assertTrue(linearDelta > quadraticDelta, "linear delta (" + linearDelta
					+ ") should be larger than quadratic delta (" + quadraticDelta + ")");
		}

		@Test
		@DisplayName("moves cursor when value exceeds dead zone")
		void movesCursorAboveDeadZone() {
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(1.0f);
			action.doAction(mockInput, 0, 1.0f);
			Mockito.verify(mockInput).setCursorDeltaX(Mockito.anyInt());
		}

		@Test
		@DisplayName("negative input moves cursor in negative direction")
		void negativeInputMovesNegative() {
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(1.0f);
			action.doAction(mockInput, 0, -1.0f);
			// Should produce a negative cursor delta
			Mockito.verify(mockInput).setCursorDeltaX(Mockito.intThat(v -> v < 0));
		}

		@Test
		@DisplayName("resets remainingD when value is within the dead zone")
		void resetsRemainderInDeadZone() {
			action.remainingD = 0.7f;
			action.doAction(mockInput, 0, 0.05f);
			Assertions.assertEquals(0f, action.remainingD, 0.001f);
		}

		@Test
		@DisplayName("resets remainingD when axis is suspended")
		void resetsRemainderWhenSuspended() {
			Mockito.when(mockInput.isAxisSuspended(0)).thenReturn(true);
			action.remainingD = 0.7f;
			action.doAction(mockInput, 0, 0.5f);
			Assertions.assertEquals(0f, action.remainingD, 0.001f);
		}

		@BeforeEach
		void setUp() {
			action = new AxisToCursorAction();
			action.setDeadZone(0.1f);
			action.setExponent(2f);
		}
	}
}

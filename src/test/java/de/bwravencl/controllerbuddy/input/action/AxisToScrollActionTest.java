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
class AxisToScrollActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("doAction()")
	class DoActionTests {

		private AxisToScrollAction action;

		@Test
		@DisplayName("positive and negative inputs produce opposite scroll directions")
		void oppositeDirections() {
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(1.0f);
			action.doAction(mockInput, 0, 1.0f);

			final var captor = ArgumentCaptor.forClass(Integer.class);
			Mockito.verify(mockInput).setScrollClicks(captor.capture());
			final var positiveInputScroll = captor.getValue();

			Mockito.clearInvocations(mockInput);

			final var action2 = new AxisToScrollAction();
			action2.setDeadZone(0.1f);
			action2.setExponent(1f);
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(1.0f);
			action2.doAction(mockInput, 0, -1.0f);

			Mockito.verify(mockInput).setScrollClicks(captor.capture());
			final var negativeInputScroll = captor.getValue();

			Assertions.assertNotEquals(0, positiveInputScroll, "positive input should produce non-zero scroll");
			Assertions.assertNotEquals(0, negativeInputScroll, "negative input should produce non-zero scroll");
			Assertions.assertNotEquals(Integer.signum(positiveInputScroll), Integer.signum(negativeInputScroll),
					"scroll directions should be opposite: " + positiveInputScroll + " vs " + negativeInputScroll);
		}

		@Test
		@DisplayName("resets remainingD when value is within the dead zone")
		void resetsRemainderInDeadZone() {
			action.remainingD = 0.5f;
			action.doAction(mockInput, 0, 0.05f);
			Assertions.assertEquals(0f, action.remainingD, 0.001f);
		}

		@Test
		@DisplayName("resets remainingD when axis is suspended")
		void resetsRemainderWhenSuspended() {
			Mockito.when(mockInput.isAxisSuspended(0)).thenReturn(true);
			action.remainingD = 0.5f;
			action.doAction(mockInput, 0, 0.9f);
			Assertions.assertEquals(0f, action.remainingD, 0.001f);
		}

		@Test
		@DisplayName("scrolls when value exceeds dead zone")
		void scrollsAboveDeadZone() {
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(1.0f);
			action.doAction(mockInput, 0, 1.0f);
			Mockito.verify(mockInput).setScrollClicks(Mockito.anyInt());
		}

		@BeforeEach
		void setUp() {
			action = new AxisToScrollAction();
			action.setDeadZone(0.1f);
			action.setExponent(1f);
		}
	}
}

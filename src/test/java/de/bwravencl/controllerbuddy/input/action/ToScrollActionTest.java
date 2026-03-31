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
final class ToScrollActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("scroll()")
	final class ScrollTests {

		private ButtonToScrollAction action;

		@Test
		@DisplayName("accumulates fractional scroll values until they round to non-zero")
		void accumulatesFractionalValues() {
			action.scroll(mockInput, 0.4f);
			Mockito.verify(mockInput, Mockito.never()).setScrollClicks(Mockito.anyInt());
			Assertions.assertEquals(0.4f, action.remainingD, 0.001f);

			// 0.4 + 0.4 = 0.8, rounds to 1
			action.scroll(mockInput, 0.4f);
			Mockito.verify(mockInput).setScrollClicks(1);
		}

		@Test
		@DisplayName("inverts scroll direction when invert is true")
		void invertsDirection() {
			action.setInvert(true);
			action.scroll(mockInput, 5.0f);
			Mockito.verify(mockInput).setScrollClicks(-5);
		}

		@Test
		@DisplayName("sends rounded scroll clicks to input")
		void sendsRoundedScrollClicks() {
			action.scroll(mockInput, 3.0f);
			Mockito.verify(mockInput).setScrollClicks(3);
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToScrollAction();
		}
	}
}

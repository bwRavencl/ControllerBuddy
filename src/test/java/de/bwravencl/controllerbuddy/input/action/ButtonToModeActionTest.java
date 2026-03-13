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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ButtonToModeActionTest {

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("getSymbol()")
	class GetSymbolTests {

		@Test
		@DisplayName("returns momentary symbol when toggle is false")
		void returnsMomentarySymbol() {
			final var action = new ButtonToModeAction();
			Assertions.assertEquals(ButtonToModeAction.MOMENTARY_SYMBOL, action.getSymbol());
		}

		@Test
		@DisplayName("returns toggle symbol when toggle is true")
		void returnsToggleSymbol() {
			final var action = new ButtonToModeAction();
			action.setToggle(true);
			Assertions.assertEquals(ButtonToModeAction.TOGGLE_SYMBOL, action.getSymbol());
		}
	}
}

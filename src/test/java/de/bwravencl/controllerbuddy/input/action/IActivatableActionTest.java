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
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activatable;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IActivatableActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("Activation enum")
	class ActivationEnumTests {

		@Test
		@DisplayName("each activation has a distinct non-empty symbol")
		void eachActivationHasDistinctSymbol() {
			final var symbols = new java.util.HashSet<String>();
			for (final var activation : Activation.values()) {
				final var symbol = activation.getSymbol();
				Assertions.assertNotNull(symbol);
				Assertions.assertFalse(symbol.isEmpty());
				symbols.add(symbol);
			}
			Assertions.assertEquals(Activation.values().length, symbols.size());
		}
	}

	@Nested
	@DisplayName("init()")
	class InitTests {

		@Test
		@DisplayName("sets activatable to NO for ON_RELEASE activation")
		void setsNoForOnRelease() {
			final var action = new ButtonToButtonAction();
			action.setActivation(Activation.ON_RELEASE);
			action.init(mockInput);
			Assertions.assertEquals(Activatable.NO, action.getActivatable());
		}

		@Test
		@DisplayName("sets activatable to YES for ON_PRESS activation")
		void setsYesForOnPress() {
			final var action = new ButtonToButtonAction();
			action.setActivation(Activation.ON_PRESS);
			action.init(mockInput);
			Assertions.assertEquals(Activatable.YES, action.getActivatable());
		}

		@Test
		@DisplayName("sets activatable to YES for WHILE_PRESSED activation")
		void setsYesForWhilePressed() {
			final var action = new ButtonToButtonAction();
			action.setActivation(Activation.WHILE_PRESSED);
			action.init(mockInput);
			Assertions.assertEquals(Activatable.YES, action.getActivatable());
		}
	}
}

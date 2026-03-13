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
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activatable;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import java.util.HashSet;
import java.util.Set;
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
class ToMouseButtonActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("handleAction() with ALWAYS activatable")
	class AlwaysTests {

		@Test
		@DisplayName("always adds to downUpMouseButtons regardless of hot")
		void alwaysAdds() {
			final var action = new ButtonToMouseButtonAction();
			action.setActivatable(Activatable.ALWAYS);
			action.setMouseButton(1);
			final var downUpMouseButtons = new HashSet<Integer>();
			Mockito.when(mockInput.getDownUpMouseButtons()).thenReturn(downUpMouseButtons);

			action.handleAction(false, mockInput);
			Assertions.assertTrue(downUpMouseButtons.contains(1));
		}
	}

	@Nested
	@DisplayName("handleAction() with ON_PRESS activation (no min interval)")
	class OnPressTests {

		private ButtonToMouseButtonAction action;

		private Set<Integer> downUpMouseButtons;

		@Test
		@DisplayName("fires on first press, sustained press does not repeat, release re-arms")
		void firesOnTransitionOnly() {
			action.handleAction(true, mockInput);
			Assertions.assertTrue(downUpMouseButtons.contains(3));
			downUpMouseButtons.clear();

			// Sustained press: does not fire
			action.handleAction(true, mockInput);
			Assertions.assertTrue(downUpMouseButtons.isEmpty());

			// Release re-arms
			action.handleAction(false, mockInput);
			action.handleAction(true, mockInput);
			Assertions.assertTrue(downUpMouseButtons.contains(3));
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToMouseButtonAction();
			action.setActivation(Activation.ON_PRESS);
			action.setActivatable(Activatable.YES);
			action.setMouseButton(3);
			downUpMouseButtons = new HashSet<>();
			Mockito.when(mockInput.getDownUpMouseButtons()).thenReturn(downUpMouseButtons);
		}
	}

	@Nested
	@DisplayName("handleAction() with ON_RELEASE activation (no min interval)")
	class OnReleaseTests {

		private ButtonToMouseButtonAction action;

		private Set<Integer> downUpMouseButtons;

		@Test
		@DisplayName("DENIED_BY_OTHER_ACTION does not fire on release")
		void deniedDoesNotFire() {
			action.setActivatable(Activatable.DENIED_BY_OTHER_ACTION);
			action.handleAction(true, mockInput);
			action.handleAction(false, mockInput);
			Assertions.assertTrue(downUpMouseButtons.isEmpty());
		}

		@Test
		@DisplayName("fires on release after press")
		void firesOnRelease() {
			action.handleAction(true, mockInput);
			Assertions.assertTrue(downUpMouseButtons.isEmpty());

			action.handleAction(false, mockInput);
			Assertions.assertTrue(downUpMouseButtons.contains(2));
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToMouseButtonAction();
			action.setActivation(Activation.ON_RELEASE);
			action.setActivatable(Activatable.NO);
			action.setMouseButton(2);
			downUpMouseButtons = new HashSet<>();
			Mockito.lenient().when(mockInput.getDownUpMouseButtons()).thenReturn(downUpMouseButtons);
		}
	}

	@Nested
	@DisplayName("handleAction() with WHILE_PRESSED activation")
	class WhilePressedTests {

		private ButtonToMouseButtonAction action;

		private Set<Integer> downMouseButtons;

		@Test
		@DisplayName("adds mouse button while hot and removes on release")
		void addsAndRemovesMouseButton() {
			action.handleAction(true, mockInput);
			Assertions.assertTrue(downMouseButtons.contains(1));

			action.handleAction(false, mockInput);
			Assertions.assertFalse(downMouseButtons.contains(1));
		}

		@Test
		@DisplayName("does not remove mouse button if it was never pressed")
		void doesNotRemoveIfNeverPressed() {
			action.handleAction(false, mockInput);
			Assertions.assertTrue(downMouseButtons.isEmpty());
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToMouseButtonAction();
			action.setActivation(Activation.WHILE_PRESSED);
			action.setActivatable(Activatable.YES);
			action.setMouseButton(1);
			downMouseButtons = new HashSet<>();
			Mockito.lenient().when(mockInput.getDownMouseButtons()).thenReturn(downMouseButtons);
		}
	}
}

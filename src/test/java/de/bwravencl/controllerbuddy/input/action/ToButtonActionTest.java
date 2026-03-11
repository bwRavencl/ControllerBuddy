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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToButtonActionTest {

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
		@DisplayName("always sets button to true regardless of hot value")
		void alwaysSetsButton() {
			final var action = new ButtonToButtonAction();
			action.setButtonId(0);
			action.setActivatable(Activatable.ALWAYS);
			final var buttons = new boolean[128];
			Mockito.when(mockInput.getButtons()).thenReturn(buttons);

			action.handleAction(false, mockInput);
			Assertions.assertTrue(buttons[0]);
		}
	}

	@Nested
	@DisplayName("handleAction() with ON_PRESS activation")
	class OnPressTests {

		private ButtonToButtonAction action;

		private boolean[] buttons;

		@Test
		@DisplayName("does not fire when activatable is NO")
		void doesNotFireWhenNo() {
			action.setActivatable(Activatable.NO);
			action.handleAction(true, mockInput);
			Assertions.assertFalse(buttons[5]);
		}

		@Test
		@DisplayName("fires button only on the first hot=true after a hot=false")
		void firesOnlyOnTransition() {
			// First press: activatable=YES → fires, transitions to NO
			action.handleAction(true, mockInput);
			Assertions.assertTrue(buttons[5]);
			buttons[5] = false;

			// Sustained press: activatable=NO → does not fire again
			action.handleAction(true, mockInput);
			Assertions.assertFalse(buttons[5]);

			// Release: resets activatable to YES
			action.handleAction(false, mockInput);

			// Second press: fires again
			action.handleAction(true, mockInput);
			Assertions.assertTrue(buttons[5]);
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToButtonAction();
			action.setButtonId(5);
			action.setActivation(Activation.ON_PRESS);
			action.setActivatable(Activatable.YES);
			buttons = new boolean[128];
			Mockito.lenient().when(mockInput.getButtons()).thenReturn(buttons);
		}
	}

	@Nested
	@DisplayName("handleAction() with ON_RELEASE activation")
	class OnReleaseTests {

		private ButtonToButtonAction action;

		private boolean[] buttons;

		@Test
		@DisplayName("DENIED_BY_OTHER_ACTION transitions to NO on press, then does not fire on release")
		void deniedByOtherActionTransition() {
			action.setActivatable(Activatable.DENIED_BY_OTHER_ACTION);

			// Press: DENIED_BY_OTHER_ACTION → NO
			action.handleAction(true, mockInput);
			Assertions.assertFalse(buttons[7]);
			Assertions.assertEquals(Activatable.NO, action.getActivatable());

			// Release: activatable=NO → does not fire
			action.handleAction(false, mockInput);
			Assertions.assertFalse(buttons[7]);
		}

		@Test
		@DisplayName("fires button on transition from hot=true to hot=false")
		void firesOnRelease() {
			// Press: NO → YES
			action.handleAction(true, mockInput);
			Assertions.assertFalse(buttons[7]);

			// Release: activatable=YES → fires, transitions to NO
			action.handleAction(false, mockInput);
			Assertions.assertTrue(buttons[7]);
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToButtonAction();
			action.setButtonId(7);
			action.setActivation(Activation.ON_RELEASE);
			action.setActivatable(Activatable.NO);
			buttons = new boolean[128];
			Mockito.lenient().when(mockInput.getButtons()).thenReturn(buttons);
		}
	}

	@Nested
	@DisplayName("handleAction() with WHILE_PRESSED activation")
	class WhilePressedTests {

		private ButtonToButtonAction action;

		private boolean[] buttons;

		@Test
		@DisplayName("clears wasDown when hot becomes false")
		void clearsWasDownWhenNotHot() {
			action.handleAction(true, mockInput);
			Assertions.assertTrue(buttons[3]);
			buttons[3] = false;

			action.handleAction(false, mockInput);
			Assertions.assertFalse(buttons[3]);
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToButtonAction();
			action.setButtonId(3);
			action.setActivation(Activation.WHILE_PRESSED);
			action.setActivatable(Activatable.YES);
			buttons = new boolean[128];
			Mockito.when(mockInput.getButtons()).thenReturn(buttons);
		}

		@Test
		@DisplayName("sets button to true while hot is true")
		void setsButtonWhileHot() {
			action.handleAction(true, mockInput);
			Assertions.assertTrue(buttons[3]);
		}
	}
}

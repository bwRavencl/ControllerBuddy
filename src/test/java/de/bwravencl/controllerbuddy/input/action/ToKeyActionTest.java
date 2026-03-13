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
import de.bwravencl.controllerbuddy.input.KeyStroke;
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
class ToKeyActionTest {

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
		@DisplayName("always adds to downUpKeyStrokes regardless of hot value")
		void alwaysAddsKeystroke() {
			final var action = new ButtonToKeyAction();
			action.setActivatable(Activatable.ALWAYS);
			action.setKeystroke(new KeyStroke());
			final var downUpKeyStrokes = new HashSet<KeyStroke>();
			Mockito.when(mockInput.getDownUpKeyStrokes()).thenReturn(downUpKeyStrokes);

			action.handleAction(false, mockInput);
			Assertions.assertEquals(1, downUpKeyStrokes.size());
		}
	}

	@Nested
	@DisplayName("handleAction() with ON_PRESS activation (no min interval)")
	class OnPressTests {

		private ButtonToKeyAction action;

		private Set<KeyStroke> downUpKeyStrokes;

		@Test
		@DisplayName("fires downUp keystroke on first press transition")
		void firesOnFirstPress() {
			action.handleAction(true, mockInput);
			Assertions.assertEquals(1, downUpKeyStrokes.size());
		}

		@Test
		@DisplayName("release re-arms for the next press")
		void releaseRearms() {
			action.handleAction(true, mockInput);
			downUpKeyStrokes.clear();

			action.handleAction(false, mockInput);
			action.handleAction(true, mockInput);
			Assertions.assertEquals(1, downUpKeyStrokes.size());
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToKeyAction();
			action.setActivation(Activation.ON_PRESS);
			action.setActivatable(Activatable.YES);
			action.setKeystroke(new KeyStroke());
			downUpKeyStrokes = new HashSet<>();
			Mockito.when(mockInput.getDownUpKeyStrokes()).thenReturn(downUpKeyStrokes);
		}

		@Test
		@DisplayName("sustained press does not fire again")
		void sustainedPressDoesNotRepeat() {
			action.handleAction(true, mockInput);
			downUpKeyStrokes.clear();

			action.handleAction(true, mockInput);
			Assertions.assertTrue(downUpKeyStrokes.isEmpty());
		}
	}

	@Nested
	@DisplayName("handleAction() with ON_RELEASE activation (no min interval)")
	class OnReleaseTests {

		private ButtonToKeyAction action;

		private Set<KeyStroke> downUpKeyStrokes;

		@Test
		@DisplayName("DENIED_BY_OTHER_ACTION transitions to NO, does not fire on release")
		void deniedDoesNotFire() {
			action.setActivatable(Activatable.DENIED_BY_OTHER_ACTION);
			action.handleAction(true, mockInput);
			action.handleAction(false, mockInput);
			Assertions.assertTrue(downUpKeyStrokes.isEmpty());
		}

		@Test
		@DisplayName("fires downUp keystroke on release after press")
		void firesOnRelease() {
			action.handleAction(true, mockInput);
			action.handleAction(false, mockInput);
			Assertions.assertEquals(1, downUpKeyStrokes.size());
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToKeyAction();
			action.setActivation(Activation.ON_RELEASE);
			action.setActivatable(Activatable.NO);
			action.setKeystroke(new KeyStroke());
			downUpKeyStrokes = new HashSet<>();
			Mockito.lenient().when(mockInput.getDownUpKeyStrokes()).thenReturn(downUpKeyStrokes);
		}
	}

	@Nested
	@DisplayName("handleAction() with WHILE_PRESSED activation")
	class WhilePressedTests {

		private ButtonToKeyAction action;

		private Set<KeyStroke> downKeyStrokes;

		@Test
		@DisplayName("adds keystroke to downKeyStrokes while hot")
		void addsKeystrokeWhileHot() {
			action.handleAction(true, mockInput);
			Assertions.assertTrue(downKeyStrokes.contains(action.getKeystroke()));
		}

		@Test
		@DisplayName("does not remove keystroke if it was never pressed")
		void doesNotRemoveIfNeverPressed() {
			action.handleAction(false, mockInput);
			Assertions.assertTrue(downKeyStrokes.isEmpty());
		}

		@Test
		@DisplayName("removes keystroke from downKeyStrokes when hot becomes false after being down")
		void removesKeystrokeOnRelease() {
			action.handleAction(true, mockInput);
			Assertions.assertTrue(downKeyStrokes.contains(action.getKeystroke()));

			action.handleAction(false, mockInput);
			Assertions.assertFalse(downKeyStrokes.contains(action.getKeystroke()));
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToKeyAction();
			action.setActivation(Activation.WHILE_PRESSED);
			action.setActivatable(Activatable.YES);
			action.setKeystroke(new KeyStroke());
			downKeyStrokes = new HashSet<>();
			Mockito.lenient().when(mockInput.getDownKeyStrokes()).thenReturn(downKeyStrokes);
		}
	}
}

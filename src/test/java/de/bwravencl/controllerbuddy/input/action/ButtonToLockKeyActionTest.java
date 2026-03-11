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
import de.bwravencl.controllerbuddy.input.LockKey;
import java.util.HashSet;
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
class ButtonToLockKeyActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("doAction() with on=false")
	class DoActionOffTests {

		private ButtonToLockKeyAction action;

		private HashSet<LockKey> offLockKeys;

		@Test
		@DisplayName("adds lock key to offLockKeys on first press")
		void addsToOffLockKeys() {
			action.doAction(mockInput, 0, true);
			Assertions.assertTrue(offLockKeys.contains(LockKey.CAPS_LOCK_LOCK_KEY));
		}

		@Test
		@DisplayName("does not add to offLockKeys on sustained press (wasUp guard)")
		void doesNotAddOnSustainedPress() {
			action.doAction(mockInput, 0, true);
			offLockKeys.clear();

			action.doAction(mockInput, 0, true);
			Assertions.assertTrue(offLockKeys.isEmpty());
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToLockKeyAction();
			action.setLockKey(LockKey.CAPS_LOCK_LOCK_KEY);
			action.setOn(false);
			offLockKeys = new HashSet<>();
			Mockito.when(mockInput.getOffLockKeys()).thenReturn(offLockKeys);
		}
	}

	@Nested
	@DisplayName("doAction() with on=true")
	class DoActionOnTests {

		private ButtonToLockKeyAction action;

		private HashSet<LockKey> onLockKeys;

		@Test
		@DisplayName("adds lock key to onLockKeys on first press")
		void addsToOnLockKeys() {
			action.doAction(mockInput, 0, true);
			Assertions.assertTrue(onLockKeys.contains(LockKey.CAPS_LOCK_LOCK_KEY));
		}

		@Test
		@DisplayName("does not add again on sustained press (wasUp guard)")
		void doesNotAddOnSustainedPress() {
			action.doAction(mockInput, 0, true);
			onLockKeys.clear();

			action.doAction(mockInput, 0, true);
			Assertions.assertTrue(onLockKeys.isEmpty());
		}

		@Test
		@DisplayName("re-triggers after a release-press cycle")
		void reTriggersAfterReleasePressycle() {
			action.doAction(mockInput, 0, true);
			onLockKeys.clear();

			action.doAction(mockInput, 0, false);

			action.doAction(mockInput, 0, true);
			Assertions.assertTrue(onLockKeys.contains(LockKey.CAPS_LOCK_LOCK_KEY));
		}

		@Test
		@DisplayName("resetWasUp() allows re-triggering without an explicit release")
		void resetWasUpAllowsRetrigger() {
			action.doAction(mockInput, 0, true);
			onLockKeys.clear();

			action.resetWasUp();

			action.doAction(mockInput, 0, true);
			Assertions.assertTrue(onLockKeys.contains(LockKey.CAPS_LOCK_LOCK_KEY));
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToLockKeyAction();
			action.setLockKey(LockKey.CAPS_LOCK_LOCK_KEY);
			action.setOn(true);
			onLockKeys = new HashSet<>();
			Mockito.when(mockInput.getOnLockKeys()).thenReturn(onLockKeys);
		}
	}
}

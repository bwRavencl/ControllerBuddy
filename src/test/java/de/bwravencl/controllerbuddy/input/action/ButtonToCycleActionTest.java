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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
final class ButtonToCycleActionTest {

	@Mock
	Input mockInput;

	@SuppressWarnings("unchecked")
	private static IAction<Boolean> createCountingAction(final AtomicInteger counter) {
		final var action = (IAction<Boolean>) Mockito.mock(IAction.class);
		Mockito.lenient().doAnswer(_ -> {
			counter.incrementAndGet();
			return null;
		}).when(action).doAction(Mockito.any(), Mockito.anyInt(), Mockito.any());
		return action;
	}

	@SuppressWarnings("unchecked")
	private static IAction<Boolean> createLabeledAction(final List<String> log, final String label) {
		final var action = (IAction<Boolean>) Mockito.mock(IAction.class);
		Mockito.lenient().doAnswer(_ -> {
			log.add(label);
			return null;
		}).when(action).doAction(Mockito.any(), Mockito.anyInt(), Mockito.any());
		return action;
	}

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("doAction() with ON_PRESS activation")
	final class OnPressTests {

		private ButtonToCycleAction cycleAction;

		private AtomicInteger executionCount;

		@Test
		@DisplayName("cycles through actions and wraps around to the first")
		void cyclesThroughAndWraps() {
			// Press-release three times cycles through all 3 actions
			for (var i = 0; i < 3; i++) {
				cycleAction.doAction(mockInput, 0, true);
				cycleAction.doAction(mockInput, 0, false);
			}
			Assertions.assertEquals(3, executionCount.get());

			// 4th press wraps back to the first action
			cycleAction.doAction(mockInput, 0, true);
			Assertions.assertEquals(4, executionCount.get());
		}

		@Test
		@DisplayName("executes the current action on press transition and advances the index")
		void executesOnPressTransition() {
			cycleAction.doAction(mockInput, 0, true);
			Assertions.assertEquals(1, executionCount.get());
		}

		@BeforeEach
		void setUp() {
			cycleAction = new ButtonToCycleAction();
			cycleAction.setActivation(Activation.ON_PRESS);
			cycleAction.setActivatable(Activatable.YES);

			executionCount = new AtomicInteger();
			final var actions = new ArrayList<IAction<Boolean>>();
			for (var i = 0; i < 3; i++) {
				actions.add(createCountingAction(executionCount));
			}
			cycleAction.setActions(actions);
		}

		@Test
		@DisplayName("sustained press does not fire again")
		void sustainedPressDoesNotRepeat() {
			cycleAction.doAction(mockInput, 0, true);
			cycleAction.doAction(mockInput, 0, true);
			Assertions.assertEquals(1, executionCount.get());
		}
	}

	@Nested
	@DisplayName("doAction() with ON_RELEASE activation")
	final class OnReleaseTests {

		private ButtonToCycleAction cycleAction;

		private AtomicInteger executionCount;

		@Test
		@DisplayName("DENIED_BY_OTHER_ACTION transitions to NO on press, does not fire on release")
		void deniedByOtherActionDoesNotFire() {
			cycleAction.setActivatable(Activatable.DENIED_BY_OTHER_ACTION);

			// Press: DENIED_BY_OTHER_ACTION → NO
			cycleAction.doAction(mockInput, 0, true);

			// Release: NO → does not fire
			cycleAction.doAction(mockInput, 0, false);
			Assertions.assertEquals(0, executionCount.get());
		}

		@Test
		@DisplayName("executes the current action on release transition")
		void executesOnRelease() {
			// Press: NO → YES
			cycleAction.doAction(mockInput, 0, true);
			Assertions.assertEquals(0, executionCount.get());

			// Release: YES → fires
			cycleAction.doAction(mockInput, 0, false);
			Assertions.assertEquals(1, executionCount.get());
		}

		@BeforeEach
		void setUp() {
			cycleAction = new ButtonToCycleAction();
			cycleAction.setActivation(Activation.ON_RELEASE);
			cycleAction.setActivatable(Activatable.NO);

			executionCount = new AtomicInteger();
			final var actions = new ArrayList<IAction<Boolean>>();
			actions.add(createCountingAction(executionCount));
			cycleAction.setActions(actions);
		}
	}

	@Nested
	@DisplayName("reset()")
	final class ResetTests {

		@Test
		@DisplayName("resets the cycle index to 0")
		void resetsIndexToZero() {
			final var cycleAction = new ButtonToCycleAction();
			cycleAction.setActivation(Activation.ON_PRESS);
			cycleAction.setActivatable(Activatable.YES);

			final var executionOrder = new ArrayList<String>();
			final var actions = new ArrayList<IAction<Boolean>>();
			actions.add(createLabeledAction(executionOrder, "first"));
			actions.add(createLabeledAction(executionOrder, "second"));
			cycleAction.setActions(actions);

			// Advance to the second action
			cycleAction.doAction(mockInput, 0, true);
			cycleAction.doAction(mockInput, 0, false);

			// Reset
			cycleAction.reset(mockInput);

			// Next fire should execute the first action again
			cycleAction.doAction(mockInput, 0, true);
			Assertions.assertEquals(List.of("first", "first"), executionOrder);
		}
	}

	@Nested
	@DisplayName("doAction() with WHILE_PRESSED activation")
	final class WhilePressedTests {

		@SuppressWarnings("unchecked")
		@Test
		@DisplayName("throws IllegalStateException for WHILE_PRESSED activation")
		void throwsForWhilePressed() {
			final var cycleAction = new ButtonToCycleAction();
			cycleAction.setActivation(Activation.WHILE_PRESSED);
			cycleAction.setActions(List.of((IAction<Boolean>) Mockito.mock(IAction.class)));

			Assertions.assertThrows(IllegalStateException.class, () -> cycleAction.doAction(mockInput, 0, true));
		}
	}
}

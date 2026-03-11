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
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activatable;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
final class IButtonToDelayableActionTest {

	@Mock
	Input mockInput;

	@Mock
	Profile mockProfile;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("handleDelay()")
	class HandleDelayTests {

		@BeforeEach
		void clearStaticState() {
			IButtonToDelayableAction.ACTION_TO_DOWN_SINCE_MAP.clear();
			IButtonToDelayableAction.ACTION_TO_MUST_DENY_ACTIVATION_MAP.clear();
		}

		@Test
		@DisplayName("returns false on sustained press before delay elapses")
		void returnsFalseBeforeDelayElapses() {
			final var action = new ButtonToButtonAction();
			action.setDelay(10_000L);

			action.handleDelay(mockInput, 0, true);
			// Second press immediately - delay has not elapsed
			Assertions.assertFalse(action.handleDelay(mockInput, 0, true));
		}

		@Test
		@DisplayName("returns false on first press when delayed, recording the timestamp")
		void returnsFalseOnFirstPressWhenDelayed() {
			final var action = new ButtonToButtonAction();
			action.setDelay(1000L);
			Assertions.assertFalse(action.handleDelay(mockInput, 0, true));
			Assertions.assertTrue(IButtonToDelayableAction.ACTION_TO_DOWN_SINCE_MAP.containsKey(action));
		}

		@Test
		@DisplayName("returns false on release and removes from tracking map")
		void returnsFalseOnReleaseAndClearsMap() {
			final var action = new ButtonToButtonAction();
			action.setDelay(1000L);
			action.handleDelay(mockInput, 0, true);
			Assertions.assertTrue(IButtonToDelayableAction.ACTION_TO_DOWN_SINCE_MAP.containsKey(action));

			Assertions.assertFalse(action.handleDelay(mockInput, 0, false));
			Assertions.assertFalse(IButtonToDelayableAction.ACTION_TO_DOWN_SINCE_MAP.containsKey(action));
		}

		@Test
		@DisplayName("returns true after delay elapses during sustained press")
		void returnsTrueAfterDelayElapses() throws InterruptedException {
			final var action = new ButtonToButtonAction();
			action.setDelay(1L);

			final var mode = new Mode(UUID.randomUUID());
			final var actionList = new ArrayList<IAction<Boolean>>();
			actionList.add(action);
			mode.getButtonToActionsMap().put(0, actionList);
			Mockito.when(mockInput.getProfile()).thenReturn(mockProfile);
			Mockito.when(mockProfile.getModes()).thenReturn(List.of(mode));

			action.handleDelay(mockInput, 0, true);
			Thread.sleep(10);
			Assertions.assertTrue(action.handleDelay(mockInput, 0, true));
		}

		@Test
		@DisplayName("returns value unchanged when not delayed")
		void returnsValueWhenNotDelayed() {
			final var action = new ButtonToButtonAction();
			Assertions.assertTrue(action.handleDelay(mockInput, 0, true));
			Assertions.assertFalse(action.handleDelay(mockInput, 0, false));
		}

		@Test
		@DisplayName("sets DENIED_BY_OTHER_ACTION on sibling undelayed ON_RELEASE actions after delay")
		void setsDeniedOnSiblingOnReleaseActions() throws InterruptedException {
			final var delayedAction = new ButtonToButtonAction();
			delayedAction.setDelay(1L);

			final var siblingAction = new ButtonToButtonAction();
			siblingAction.setActivation(Activation.ON_RELEASE);
			// siblingAction is not delayed (default delay=0)

			final var mode = new Mode(UUID.randomUUID());
			final var actionList = new ArrayList<IAction<Boolean>>();
			actionList.add(delayedAction);
			actionList.add(siblingAction);
			mode.getButtonToActionsMap().put(0, actionList);
			Mockito.when(mockInput.getProfile()).thenReturn(mockProfile);
			Mockito.when(mockProfile.getModes()).thenReturn(List.of(mode));

			delayedAction.handleDelay(mockInput, 0, true);
			Thread.sleep(10);
			delayedAction.handleDelay(mockInput, 0, true);

			Assertions.assertEquals(Activatable.DENIED_BY_OTHER_ACTION, siblingAction.getActivatable());
		}
	}
}

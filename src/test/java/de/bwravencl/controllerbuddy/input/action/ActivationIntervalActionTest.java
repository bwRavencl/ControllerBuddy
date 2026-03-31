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

import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ActivationIntervalActionTest {

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("activationSupportsMaxInterval()")
	final class ActivationSupportsMaxIntervalTests {

		@Test
		@DisplayName("returns false for ON_PRESS")
		void returnsFalseForOnPress() {
			Assertions.assertFalse(ActivationIntervalAction.activationSupportsMaxInterval(Activation.ON_PRESS));
		}

		@Test
		@DisplayName("returns false for ON_RELEASE")
		void returnsFalseForOnRelease() {
			Assertions.assertFalse(ActivationIntervalAction.activationSupportsMaxInterval(Activation.ON_RELEASE));
		}

		@Test
		@DisplayName("returns true only for WHILE_PRESSED")
		void returnsTrueForWhilePressed() {
			Assertions.assertTrue(ActivationIntervalAction.activationSupportsMaxInterval(Activation.WHILE_PRESSED));
		}
	}

	@Nested
	@DisplayName("handleActivationInterval()")
	final class HandleActivationIntervalTests {

		@Test
		@DisplayName("holds activation for the minimum interval duration after release")
		void holdsActivationForMinInterval() {
			final var action = new ButtonToButtonAction();
			action.setActivation(Activation.WHILE_PRESSED);
			action.setMinActivationInterval(5000);

			// Press: starts the timer
			Assertions.assertTrue(action.handleActivationInterval(true));
			// Release immediately: minActivationTime is in the future, so returns true
			Assertions.assertTrue(action.handleActivationInterval(false));
		}

		@Test
		@DisplayName("max activation interval is only effective for WHILE_PRESSED activation")
		void maxIntervalOnlyEffectiveForWhilePressed() {
			final var action = new ButtonToButtonAction();
			action.setActivation(Activation.ON_PRESS);
			action.setMaxActivationInterval(1);

			// ON_PRESS does not support max interval, so setting it has no effect
			// The method should just return hot unchanged
			Assertions.assertTrue(action.handleActivationInterval(true));
			Assertions.assertFalse(action.handleActivationInterval(false));
		}

		@Test
		@DisplayName("returns hot unchanged when no intervals are configured")
		void returnsHotUnchangedWithoutIntervals() {
			final var action = new ButtonToButtonAction();
			Assertions.assertTrue(action.handleActivationInterval(true));
			Assertions.assertFalse(action.handleActivationInterval(false));
		}

		@Test
		@DisplayName("suppresses activation after max interval elapses during sustained press")
		void suppressesAfterMaxInterval() throws Exception {
			final var action = new ButtonToButtonAction();
			action.setActivation(Activation.WHILE_PRESSED);
			action.setMaxActivationInterval(500);

			// Press: starts the timer, sets maxActivationTime = now + 500
			Assertions.assertTrue(action.handleActivationInterval(true));

			// Set maxActivationTime to a time in the past via reflection
			final var maxActivationTimeField = ActivationIntervalAction.class.getDeclaredField("maxActivationTime");
			maxActivationTimeField.setAccessible(true);
			maxActivationTimeField.setLong(action, System.currentTimeMillis() - 1);

			// Now the max time has passed, so sustained press should return false
			Assertions.assertFalse(action.handleActivationInterval(true));
		}
	}

	@Nested
	@DisplayName("setActivation()")
	final class SetActivationTests {

		@Test
		@DisplayName("clears maxActivationInterval when switching away from WHILE_PRESSED")
		void clearsMaxIntervalWhenSwitchingAway() {
			final var action = new ButtonToButtonAction();
			action.setActivation(Activation.WHILE_PRESSED);
			action.setMaxActivationInterval(500);
			Assertions.assertEquals(500, action.getMaxActivationInterval());

			action.setActivation(Activation.ON_PRESS);
			Assertions.assertEquals(0, action.getMaxActivationInterval());
		}

		@Test
		@DisplayName("preserves maxActivationInterval when staying on WHILE_PRESSED")
		void preservesMaxIntervalForWhilePressed() {
			final var action = new ButtonToButtonAction();
			action.setActivation(Activation.WHILE_PRESSED);
			action.setMaxActivationInterval(500);

			action.setActivation(Activation.WHILE_PRESSED);
			Assertions.assertEquals(500, action.getMaxActivationInterval());
		}
	}
}

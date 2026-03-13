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
class AxisToButtonActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("doAction()")
	class DoActionTests {

		private AxisToButtonAction action;

		private boolean[] buttons;

		@Test
		@DisplayName("activates button at the exact minimum boundary")
		void activatesAtExactMinBoundary() {
			action.doAction(mockInput, 0, 0.5f);
			Assertions.assertTrue(buttons[0]);
		}

		@Test
		@DisplayName("activates button when axis value is within the min-max zone")
		void activatesButtonWhenInZone() {
			action.doAction(mockInput, 0, 0.75f);
			Assertions.assertTrue(buttons[0]);
		}

		@Test
		@DisplayName("does not activate button when axis value is above the maximum")
		void doesNotActivateAboveMax() {
			action.setMaxAxisValue(0.8f);
			action.doAction(mockInput, 0, 0.9f);
			Assertions.assertFalse(buttons[0]);
		}

		@Test
		@DisplayName("does not activate button when axis value is below the minimum")
		void doesNotActivateBelowMin() {
			action.doAction(mockInput, 0, 0.3f);
			Assertions.assertFalse(buttons[0]);
		}

		@Test
		@DisplayName("does not activate when axis is suspended")
		void doesNotActivateWhenSuspended() {
			Mockito.when(mockInput.isAxisSuspended(0)).thenReturn(true);
			action.doAction(mockInput, 0, 0.75f);
			Assertions.assertFalse(buttons[0]);
		}

		@BeforeEach
		void setUp() {
			action = new AxisToButtonAction();
			action.setButtonId(0);
			action.setMinAxisValue(0.5f);
			action.setMaxAxisValue(1.0f);
			action.setActivation(Activation.WHILE_PRESSED);
			action.setActivatable(Activatable.YES);
			buttons = new boolean[128];
			Mockito.lenient().when(mockInput.getButtons()).thenReturn(buttons);
		}
	}
}

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
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lwjgl.sdl.SDLGamepad;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@NullMarked
@ExtendWith(MockitoExtension.class)
final class AxisToButtonActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.strings;
	}

	@Nested
	@DisplayName("doAction()")
	final class DoActionTests {

		private AxisToButtonAction action;

		private boolean[] buttons;

		@Test
		@DisplayName("activates button at the exact minimum boundary")
		void activatesAtExactMinBoundary() {
			action.doAction(mockInput, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, 0.5f, null);
			Assertions.assertTrue(buttons[0]);
		}

		@Test
		@DisplayName("activates button when axis value is within the min-max zone")
		void activatesButtonWhenInZone() {
			action.doAction(mockInput, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, 0.75f, null);
			Assertions.assertTrue(buttons[0]);
		}

		@Test
		@DisplayName("does not activate button when axis value is above the maximum")
		void doesNotActivateAboveMax() {
			action.setMaxAxisValue(0.8f);
			action.doAction(mockInput, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, 0.9f, null);
			Assertions.assertFalse(buttons[0]);
		}

		@Test
		@DisplayName("does not activate button when axis value is below the minimum")
		void doesNotActivateBelowMin() {
			action.doAction(mockInput, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, 0.3f, null);
			Assertions.assertFalse(buttons[0]);
		}

		@Test
		@DisplayName("does not activate when axis is suspended")
		void doesNotActivateWhenSuspended() {
			Mockito.when(mockInput.isAxisSuspended(0)).thenReturn(true);
			action.doAction(mockInput, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, 0.75f, null);
			Assertions.assertFalse(buttons[0]);
		}

		@BeforeEach
		void setUp() {
			action = new AxisToButtonAction();
			action.setButtonId(0);
			action.setMinAxisValue(0.5f);
			action.setMaxAxisValue(1f);
			action.setActivation(Activation.WHILE_PRESSED);
			action.setActivatable(Activatable.YES);
			buttons = new boolean[128];
			Mockito.lenient().when(mockInput.getButtons()).thenReturn(buttons);
		}
	}
}

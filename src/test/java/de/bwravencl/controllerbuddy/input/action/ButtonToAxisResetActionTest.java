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
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activatable;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
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
class ButtonToAxisResetActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("resetAxis() fluid vs immediate")
	class FluidVsImmediateTests {

		private ButtonToAxisResetAction action;

		@Test
		@DisplayName("calls moveAxis when fluid is true")
		void callsMoveAxisWhenFluid() {
			action.setFluid(true);
			action.doAction(mockInput, 0, true);
			Mockito.verify(mockInput).moveAxis(VirtualAxis.X, 0f);
		}

		@Test
		@DisplayName("calls setAxis when fluid is false")
		void callsSetAxisWhenNotFluid() {
			action.doAction(mockInput, 0, true);
			Mockito.verify(mockInput).setAxis(VirtualAxis.X, 0f, false, null, null, null);
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToAxisResetAction();
			action.setActivation(Activation.WHILE_PRESSED);
			action.setVirtualAxis(VirtualAxis.X);
			action.setResetValue(0f);
		}
	}

	@Nested
	@DisplayName("doAction() with ON_PRESS")
	class OnPressTests {

		private ButtonToAxisResetAction action;

		@Test
		@DisplayName("resets axis only on the first press transition")
		void resetsOnlyOnFirstPress() {
			action.doAction(mockInput, 0, true);
			Mockito.verify(mockInput).setAxis(VirtualAxis.Y, 0.5f, false, null, null, null);

			// Sustained press: should not fire again
			action.doAction(mockInput, 0, true);
			Mockito.verify(mockInput, Mockito.times(1)).setAxis(Mockito.any(), Mockito.anyFloat(), Mockito.anyBoolean(),
					Mockito.any(), Mockito.any(), Mockito.any());

			// Release re-arms
			action.doAction(mockInput, 0, false);
			action.doAction(mockInput, 0, true);
			Mockito.verify(mockInput, Mockito.times(2)).setAxis(VirtualAxis.Y, 0.5f, false, null, null, null);
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToAxisResetAction();
			action.setActivation(Activation.ON_PRESS);
			action.setActivatable(Activatable.YES);
			action.setVirtualAxis(VirtualAxis.Y);
			action.setResetValue(0.5f);
		}
	}

	@Nested
	@DisplayName("doAction() with ON_RELEASE")
	class OnReleaseTests {

		private ButtonToAxisResetAction action;

		@Test
		@DisplayName("resets axis on release after a press")
		void resetsOnRelease() {
			// Press: NO → YES
			action.doAction(mockInput, 0, true);
			Mockito.verify(mockInput, Mockito.never()).setAxis(Mockito.any(), Mockito.anyFloat(), Mockito.anyBoolean(),
					Mockito.any(), Mockito.any(), Mockito.any());

			// Release: YES → fires
			action.doAction(mockInput, 0, false);
			Mockito.verify(mockInput).setAxis(VirtualAxis.Z, 0.25f, false, null, null, null);
		}

		@Test
		@DisplayName("DENIED_BY_OTHER_ACTION transitions to NO and does not fire on release")
		void deniedDoesNotFire() {
			action.setActivatable(Activatable.DENIED_BY_OTHER_ACTION);

			action.doAction(mockInput, 0, true);
			action.doAction(mockInput, 0, false);
			Mockito.verify(mockInput, Mockito.never()).setAxis(Mockito.any(), Mockito.anyFloat(), Mockito.anyBoolean(),
					Mockito.any(), Mockito.any(), Mockito.any());
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToAxisResetAction();
			action.setActivation(Activation.ON_RELEASE);
			action.setActivatable(Activatable.NO);
			action.setVirtualAxis(VirtualAxis.Z);
			action.setResetValue(0.25f);
		}
	}

	@Nested
	@DisplayName("doAction() with WHILE_PRESSED")
	class WhilePressedTests {

		private ButtonToAxisResetAction action;

		@Test
		@DisplayName("calls setAxis while button is pressed")
		void callsSetAxisWhilePressed() {
			action.doAction(mockInput, 0, true);
			Mockito.verify(mockInput).setAxis(VirtualAxis.X, 0f, false, null, null, null);
		}

		@Test
		@DisplayName("does not call setAxis when button is released")
		void doesNotCallSetAxisWhenReleased() {
			action.doAction(mockInput, 0, false);
			Mockito.verify(mockInput, Mockito.never()).setAxis(Mockito.any(), Mockito.anyFloat(), Mockito.anyBoolean(),
					Mockito.any(), Mockito.any(), Mockito.any());
		}

		@BeforeEach
		void setUp() {
			action = new ButtonToAxisResetAction();
			action.setActivation(Activation.WHILE_PRESSED);
			action.setVirtualAxis(VirtualAxis.X);
			action.setResetValue(0f);
		}
	}
}

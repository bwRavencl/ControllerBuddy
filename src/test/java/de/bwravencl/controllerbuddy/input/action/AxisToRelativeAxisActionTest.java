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
import de.bwravencl.controllerbuddy.runmode.RunMode;
import java.util.EnumMap;
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
class AxisToRelativeAxisActionTest {

	@Mock
	Input mockInput;

	@Mock
	RunMode mockRunMode;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("doAction()")
	class DoActionTests {

		private AxisToRelativeAxisAction action;

		private EnumMap<VirtualAxis, Integer> axes;

		@Test
		@DisplayName("accumulates fractional movement in remainingD below planck length")
		void accumulatesBelowPlanckLength() {
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(0.001f);
			Mockito.when(mockInput.getPlanckLength()).thenReturn(1.0f);

			action.doAction(mockInput, 0, 0.2f);
			Assertions.assertTrue(action.remainingD > 0f,
					"remainingD (" + action.remainingD + ") should be positive for positive input");
			Assertions.assertTrue(action.remainingD < 0.01f,
					"remainingD (" + action.remainingD + ") should be a small value below planck length");
			Mockito.verify(mockInput, Mockito.never()).setAxis(Mockito.any(), Mockito.anyFloat(), Mockito.anyBoolean(),
					Mockito.any(), Mockito.any(), Mockito.any());
		}

		@Test
		@DisplayName("applies axis movement when delta exceeds planck length")
		void appliesMovementAbovePlanckLength() {
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(1.0f);
			Mockito.when(mockInput.getPlanckLength()).thenReturn(0.001f);
			Mockito.when(mockInput.getRunMode()).thenReturn(mockRunMode);
			Mockito.when(mockRunMode.getMinAxisValue()).thenReturn(-32_767);
			Mockito.when(mockRunMode.getMaxAxisValue()).thenReturn(32_767);
			Mockito.when(mockInput.getAxes()).thenReturn(axes);

			action.doAction(mockInput, 0, 1.0f);
			Mockito.verify(mockInput).setAxis(Mockito.eq(VirtualAxis.X), Mockito.anyFloat(), Mockito.eq(false),
					Mockito.any(), Mockito.any(), Mockito.any());
			Assertions.assertEquals(0f, action.remainingD, 0.001f);
		}

		@Test
		@DisplayName("clamps output to maxValue")
		void clampsToMaxValue() {
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(1.0f);
			Mockito.when(mockInput.getPlanckLength()).thenReturn(0.001f);
			Mockito.when(mockInput.getRunMode()).thenReturn(mockRunMode);
			Mockito.when(mockRunMode.getMinAxisValue()).thenReturn(-32_767);
			Mockito.when(mockRunMode.getMaxAxisValue()).thenReturn(32_767);
			// Axis already at near-maximum
			axes.put(VirtualAxis.X, 32_000);
			Mockito.when(mockInput.getAxes()).thenReturn(axes);

			action.doAction(mockInput, 0, 1.0f);

			final var valueCaptor = org.mockito.ArgumentCaptor.forClass(Float.class);
			Mockito.verify(mockInput).setAxis(Mockito.eq(VirtualAxis.X), valueCaptor.capture(), Mockito.eq(false),
					Mockito.any(), Mockito.any(), Mockito.any());
			Assertions.assertTrue(valueCaptor.getValue() <= 1.0f);
		}

		@Test
		@DisplayName("does nothing when value is within dead zone")
		void doesNothingInDeadZone() {
			action.doAction(mockInput, 0, 0.05f);
			Mockito.verify(mockInput, Mockito.never()).setAxis(Mockito.any(), Mockito.anyFloat(), Mockito.anyBoolean(),
					Mockito.any(), Mockito.any(), Mockito.any());
		}

		@Test
		@DisplayName("does nothing when axis is suspended")
		void doesNothingWhenSuspended() {
			Mockito.when(mockInput.isAxisSuspended(0)).thenReturn(true);
			action.doAction(mockInput, 0, 1.0f);
			Mockito.verify(mockInput, Mockito.never()).setAxis(Mockito.any(), Mockito.anyFloat(), Mockito.anyBoolean(),
					Mockito.any(), Mockito.any(), Mockito.any());
		}

		@Test
		@DisplayName("inverts movement direction when invert is true")
		void invertsMovement() {
			action.setInvert(true);
			Mockito.when(mockInput.getRateMultiplier()).thenReturn(1.0f);
			Mockito.when(mockInput.getPlanckLength()).thenReturn(0.001f);
			Mockito.when(mockInput.getRunMode()).thenReturn(mockRunMode);
			Mockito.when(mockRunMode.getMinAxisValue()).thenReturn(-32_767);
			Mockito.when(mockRunMode.getMaxAxisValue()).thenReturn(32_767);
			axes.put(VirtualAxis.X, 0);
			Mockito.when(mockInput.getAxes()).thenReturn(axes);

			action.doAction(mockInput, 0, 1.0f);

			final var valueCaptor = org.mockito.ArgumentCaptor.forClass(Float.class);
			Mockito.verify(mockInput).setAxis(Mockito.eq(VirtualAxis.X), valueCaptor.capture(), Mockito.eq(false),
					Mockito.any(), Mockito.any(), Mockito.any());
			// Positive input with invert=true should result in negative movement
			Assertions.assertTrue(valueCaptor.getValue() < 0f);
		}

		@BeforeEach
		void setUp() {
			action = new AxisToRelativeAxisAction();
			action.setVirtualAxis(VirtualAxis.X);
			action.setDeadZone(0.1f);
			axes = new EnumMap<>(VirtualAxis.class);
			axes.put(VirtualAxis.X, 0);
		}
	}
}

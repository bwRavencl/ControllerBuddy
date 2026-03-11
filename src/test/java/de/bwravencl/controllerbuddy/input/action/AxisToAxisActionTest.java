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
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AxisToAxisActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("doAction()")
	class DoActionTests {

		private AxisToAxisAction action;

		@Test
		@DisplayName("applies dead zone: values within dead zone produce 0")
		void deadZoneProducesZero() {
			action.setDeadZone(0.2f);
			action.doAction(mockInput, 0, 0.1f);

			final var valueCaptor = ArgumentCaptor.forClass(Float.class);
			Mockito.verify(mockInput).setAxis(Mockito.eq(VirtualAxis.X), valueCaptor.capture(), Mockito.eq(false),
					Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
			Assertions.assertEquals(0f, valueCaptor.getValue(), 0.001f);
		}

		@Test
		@DisplayName("does nothing when axis is suspended")
		void doesNothingWhenAxisSuspended() {
			Mockito.when(mockInput.isAxisSuspended(0)).thenReturn(true);
			action.doAction(mockInput, 0, 0.5f);
			Mockito.verify(mockInput, Mockito.never()).setAxis(Mockito.any(), Mockito.anyFloat(), Mockito.anyBoolean(),
					Mockito.any(), Mockito.any(), Mockito.any());
		}

		@Test
		@DisplayName("full negative input with defaults produces minValue (-1.0)")
		void fullNegativeInputProducesMinValue() {
			action.doAction(mockInput, 0, -1.0f);

			final var valueCaptor = ArgumentCaptor.forClass(Float.class);
			Mockito.verify(mockInput).setAxis(Mockito.eq(VirtualAxis.X), valueCaptor.capture(), Mockito.eq(false),
					Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
			Assertions.assertEquals(-1.0f, valueCaptor.getValue(), 0.01f);
		}

		@Test
		@DisplayName("full positive input with defaults produces maxValue (1.0)")
		void fullPositiveInputProducesMaxValue() {
			action.doAction(mockInput, 0, 1.0f);

			final var valueCaptor = ArgumentCaptor.forClass(Float.class);
			Mockito.verify(mockInput).setAxis(Mockito.eq(VirtualAxis.X), valueCaptor.capture(), Mockito.eq(false),
					Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
			Assertions.assertEquals(1.0f, valueCaptor.getValue(), 0.01f);
		}

		@Test
		@DisplayName("inverts the output value when invert is true")
		void invertsOutput() {
			action.setInvert(true);
			action.doAction(mockInput, 0, 1.0f);

			final var valueCaptor = ArgumentCaptor.forClass(Float.class);
			Mockito.verify(mockInput).setAxis(Mockito.eq(VirtualAxis.X), valueCaptor.capture(), Mockito.eq(false),
					Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
			Assertions.assertTrue(valueCaptor.getValue() < 0f);
		}

		@BeforeEach
		void setUp() {
			action = new AxisToAxisAction();
			action.setVirtualAxis(VirtualAxis.X);
		}

		@Test
		@DisplayName("exponent=0 bypasses the power curve and uses linear normalization")
		void zeroExponentUsesLinearNormalization() {
			action.setExponent(0f);
			action.doAction(mockInput, 0, 0.5f);

			final var valueCaptor = ArgumentCaptor.forClass(Float.class);
			Mockito.verify(mockInput).setAxis(Mockito.eq(VirtualAxis.X), valueCaptor.capture(), Mockito.eq(false),
					Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
			// With exponent=0: value passes through normalize(0.5, 0, 1, 0, 1) = 0.5
			Assertions.assertEquals(0.5f, valueCaptor.getValue(), 0.01f);
		}
	}

	@Nested
	@DisplayName("init()")
	class InitTests {

		@Test
		@DisplayName("initialValue is inverted when invert is true")
		void invertsInitialValue() {
			final var action = new AxisToAxisAction();
			action.setVirtualAxis(VirtualAxis.X);
			action.setInitialValue(0.5f);
			action.setInvert(true);
			Mockito.when(mockInput.isSkipAxisInitialization()).thenReturn(false);

			action.init(mockInput);

			Mockito.verify(mockInput).setAxis(VirtualAxis.X, -0.5f, false, null, null, null);
		}

		@Test
		@DisplayName("sets axis to initialValue when not skipping initialization")
		void setsAxisToInitialValue() {
			final var action = new AxisToAxisAction();
			action.setVirtualAxis(VirtualAxis.Y);
			action.setInitialValue(0.5f);
			Mockito.when(mockInput.isSkipAxisInitialization()).thenReturn(false);

			action.init(mockInput);

			Mockito.verify(mockInput).setAxis(VirtualAxis.Y, 0.5f, false, null, null, null);
		}

		@Test
		@DisplayName("skips initialization when input says so")
		void skipsWhenFlagSet() {
			final var action = new AxisToAxisAction();
			Mockito.when(mockInput.isSkipAxisInitialization()).thenReturn(true);

			action.init(mockInput);

			Mockito.verify(mockInput, Mockito.never()).setAxis(Mockito.any(), Mockito.anyFloat(), Mockito.anyBoolean(),
					Mockito.any(), Mockito.any(), Mockito.any());
		}
	}
}

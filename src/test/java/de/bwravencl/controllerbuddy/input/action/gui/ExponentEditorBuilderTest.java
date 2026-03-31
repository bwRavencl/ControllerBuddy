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

package de.bwravencl.controllerbuddy.input.action.gui;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ExponentEditorBuilderTest {

	@Nested
	@DisplayName("PowerFunctionPlotter.calculateY()")
	final class CalculateYTests {

		private Method calculateYMethod;

		private Object plotter;

		private int calculateY(final int x, final int plotWidth, final int plotHeight) throws Exception {
			return (int) calculateYMethod.invoke(plotter, x, plotWidth, plotHeight);
		}

		@Test
		@DisplayName("higher power produces larger Y (more curved) for midpoint x")
		void higherPowerProducesLargerYAtMidpoint() throws Exception {
			// With power=1 (linear), midpoint should be around plotHeight/2
			final var yLinear = calculateY(50, 100, 100);

			// Create plotter with higher power
			final var plotterClass = Arrays.stream(ExponentEditorBuilder.class.getDeclaredClasses())
					.filter(c -> "PowerFunctionPlotter".equals(c.getSimpleName())).findFirst().orElseThrow();
			final var constructor = plotterClass.getDeclaredConstructor(float.class);
			constructor.setAccessible(true);
			final var plotterPow3 = constructor.newInstance(3f);

			final var method = plotterClass.getDeclaredMethod("calculateY", int.class, int.class, int.class);
			method.setAccessible(true);
			final var yCubic = (int) method.invoke(plotterPow3, 50, 100, 100);

			// With higher exponent, the curve bows down → Y is larger (further from top)
			Assertions.assertTrue(yCubic > yLinear,
					"cubic power (" + yCubic + ") should produce larger Y than linear (" + yLinear + ")");
		}

		@Test
		@DisplayName("calculateY values are monotonically non-decreasing from x=max to x=0")
		void monotonicallyDecreasing() throws Exception {
			// As x increases from 0 to plotWidth-1, Y should decrease (move toward top)
			var previousY = calculateY(0, 50, 50);
			for (var x = 1; x < 50; x++) {
				final var y = calculateY(x, 50, 50);
				Assertions.assertTrue(y <= previousY,
						"Y at x=" + x + " (" + y + ") should be <= Y at x=" + (x - 1) + " (" + previousY + ")");
				previousY = y;
			}
		}

		@Test
		@DisplayName("returns plotHeight-1 when x is 0 (bottom of plot)")
		void returnsBottomForZeroX() throws Exception {
			Assertions.assertEquals(99, calculateY(0, 100, 100));
		}

		@Test
		@DisplayName("returns 0 when x equals plotWidth-1 (top of plot) for power=1")
		void returnsTopForMaxX() throws Exception {
			final var y = calculateY(99, 100, 100);
			Assertions.assertTrue(y <= 0);
		}

		@BeforeEach
		void setUp() throws Exception {
			final var plotterClass = Arrays.stream(ExponentEditorBuilder.class.getDeclaredClasses())
					.filter(c -> "PowerFunctionPlotter".equals(c.getSimpleName())).findFirst().orElseThrow();
			final var constructor = plotterClass.getDeclaredConstructor(float.class);
			constructor.setAccessible(true);
			plotter = constructor.newInstance(1f);

			calculateYMethod = plotterClass.getDeclaredMethod("calculateY", int.class, int.class, int.class);
			calculateYMethod.setAccessible(true);
		}
	}
}

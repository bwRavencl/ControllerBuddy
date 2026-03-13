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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NumberEditorBuilderTest {

	@Nested
	@DisplayName("roundFloat()")
	class RoundFloatTests {

		@Test
		@DisplayName("handles trailing floating point noise like 0.1 + 0.2")
		void handlesFloatingPointNoise() {
			// 0.1 + 0.2 = 0.30000000000000004 in floating point
			final var noisy = (float) (0.1 + 0.2);
			Assertions.assertEquals(0.3f, NumberEditorBuilder.roundFloat(noisy));
		}

		@Test
		@DisplayName("handles zero")
		void handlesZero() {
			Assertions.assertEquals(0f, NumberEditorBuilder.roundFloat(0f));
		}

		@Test
		@DisplayName("preserves value with fewer than 3 decimals")
		void preservesShortValues() {
			Assertions.assertEquals(0.5f, NumberEditorBuilder.roundFloat(0.5f));
		}

		@Test
		@DisplayName("rounds 0.5 up at the third decimal")
		void roundsHalfUp() {
			Assertions.assertEquals(0.556f, NumberEditorBuilder.roundFloat(0.5555f));
		}

		@Test
		@DisplayName("rounds negative values correctly")
		void roundsNegativeValues() {
			Assertions.assertEquals(-1.235f, NumberEditorBuilder.roundFloat(-1.2345f));
		}

		@Test
		@DisplayName("rounds to 3 decimal places using HALF_UP")
		void roundsToThreeDecimals() {
			Assertions.assertEquals(1.235f, NumberEditorBuilder.roundFloat(1.2345f));
		}
	}
}

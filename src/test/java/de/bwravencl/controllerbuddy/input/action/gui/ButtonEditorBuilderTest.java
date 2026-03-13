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

import java.text.ParseException;
import java.util.Arrays;
import javax.swing.text.DefaultFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ButtonEditorBuilderTest {

	@Nested
	@DisplayName("ZeroBasedFormatter")
	class ZeroBasedFormatterTests {

		private DefaultFormatter formatter;

		@Test
		@DisplayName("round-trip: stringToValue(valueToString(n)) == n")
		void roundTrip() throws Exception {
			for (final var value : new int[] { 0, 1, 63, 127 }) {
				final var display = formatter.valueToString(value);
				final var parsed = formatter.stringToValue(display);
				Assertions.assertEquals(value, parsed);
			}
		}

		@BeforeEach
		void setUp() throws Exception {
			final var clazz = Arrays.stream(ButtonEditorBuilder.class.getDeclaredClasses())
					.filter(c -> "ZeroBasedFormatter".equals(c.getSimpleName())).findFirst().orElseThrow();
			final var constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			formatter = (DefaultFormatter) constructor.newInstance();
		}

		@Test
		@DisplayName("stringToValue converts 1-based display string to 0-based internal value")
		void stringToValueConvertsToZeroBased() throws Exception {
			Assertions.assertEquals(0, formatter.stringToValue("1"));
			Assertions.assertEquals(4, formatter.stringToValue("5"));
			Assertions.assertEquals(127, formatter.stringToValue("128"));
		}

		@Test
		@DisplayName("stringToValue returns null for blank input")
		void stringToValueReturnsNullForBlank() throws Exception {
			Assertions.assertNull(formatter.stringToValue("  "));
		}

		@Test
		@DisplayName("stringToValue returns null for null input")
		void stringToValueReturnsNullForNull() throws Exception {
			Assertions.assertNull(formatter.stringToValue(null));
		}

		@Test
		@DisplayName("stringToValue throws ParseException for non-numeric input")
		void stringToValueThrowsForNonNumeric() {
			Assertions.assertThrows(ParseException.class, () -> formatter.stringToValue("abc"));
		}

		@Test
		@DisplayName("valueToString converts 0-based internal value to 1-based display string")
		void valueToStringConvertsToOneBased() throws Exception {
			Assertions.assertEquals("1", formatter.valueToString(0));
			Assertions.assertEquals("5", formatter.valueToString(4));
			Assertions.assertEquals("128", formatter.valueToString(127));
		}
	}
}

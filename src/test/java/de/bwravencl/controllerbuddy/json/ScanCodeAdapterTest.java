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

package de.bwravencl.controllerbuddy.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import de.bwravencl.controllerbuddy.input.ScanCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScanCodeAdapterTest {

	private static ScanCodeAdapter createAdapter() {
		return new ScanCodeAdapter();
	}

	@Nested
	@DisplayName("deserialize()")
	class DeserializeTests {

		@Test
		@DisplayName("deserializes ESCAPE from its name string")
		void deserializesEscapeFromName() {
			final var adapter = createAdapter();
			final var expected = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_ESCAPE);
			final var result = adapter.deserialize(new JsonPrimitive(ScanCode.DIK_ESCAPE), ScanCode.class, null);
			Assertions.assertSame(expected, result);
		}

		@Test
		@DisplayName("deserializes a JSON number by looking up the key code in KEY_CODE_TO_SCAN_CODE_MAP")
		void deserializesFromKeyCode() {
			final var adapter = createAdapter();
			final var expected = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A);
			final var result = adapter.deserialize(new JsonPrimitive(expected.keyCode()), ScanCode.class, null);
			Assertions.assertSame(expected, result);
		}

		@Test
		@DisplayName("deserializes a JSON string by looking up the name in NAME_TO_SCAN_CODE_MAP")
		void deserializesFromStringName() {
			final var adapter = createAdapter();
			final var expected = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A);
			final var result = adapter.deserialize(new JsonPrimitive(ScanCode.DIK_A), ScanCode.class, null);
			Assertions.assertSame(expected, result);
		}

		@Test
		@DisplayName("throws JsonParseException when the JSON element is not a primitive")
		void throwsForNonPrimitiveJson() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonObject(), ScanCode.class, null));
		}

		@Test
		@DisplayName("throws JsonParseException when the number does not match any key code")
		void throwsForUnknownKeyCode() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonPrimitive(-1), ScanCode.class, null));
		}

		@Test
		@DisplayName("throws JsonParseException when the string does not match any known scan code name")
		void throwsForUnknownStringName() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonPrimitive("DIK_UNKNOWN_KEY"), ScanCode.class, null));
		}
	}

	@Nested
	@DisplayName("serialize()")
	class SerializeTests {

		@Test
		@DisplayName("returns a JsonPrimitive containing the scan code's name")
		void serializesAsName() {
			final var adapter = createAdapter();
			final var scanCode = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A);
			final var result = adapter.serialize(scanCode, ScanCode.class, null);
			Assertions.assertInstanceOf(JsonPrimitive.class, result);
			Assertions.assertEquals(ScanCode.DIK_A, result.getAsString());
		}

		@Test
		@DisplayName("serializes ESCAPE scan code as its name string")
		void serializesEscapeAsName() {
			final var adapter = createAdapter();
			final var scanCode = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_ESCAPE);
			final var result = adapter.serialize(scanCode, ScanCode.class, null);
			Assertions.assertEquals(ScanCode.DIK_ESCAPE, result.getAsString());
		}
	}
}

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

package de.bwravencl.controllerbuddy.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import de.bwravencl.controllerbuddy.input.Scancode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScancodeAdapterTest {

	private static ScancodeAdapter createAdapter() {
		return new ScancodeAdapter();
	}

	@Nested
	@DisplayName("deserialize()")
	class DeserializeTests {

		@Test
		@DisplayName("deserializes ESCAPE from its name string")
		void deserializesEscapeFromName() {
			final var adapter = createAdapter();
			final var expected = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_ESCAPE);
			final var result = adapter.deserialize(new JsonPrimitive(Scancode.DIK_ESCAPE), Scancode.class, null);
			Assertions.assertSame(expected, result);
		}

		@Test
		@DisplayName("deserializes a JSON number by looking up the key code in KEY_CODE_TO_SCAN_CODE_MAP")
		void deserializesFromKeyCode() {
			final var adapter = createAdapter();
			final var expected = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			final var result = adapter.deserialize(new JsonPrimitive(expected.keyCode()), Scancode.class, null);
			Assertions.assertSame(expected, result);
		}

		@Test
		@DisplayName("deserializes a JSON string by looking up the name in NAME_TO_SCAN_CODE_MAP")
		void deserializesFromStringName() {
			final var adapter = createAdapter();
			final var expected = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			final var result = adapter.deserialize(new JsonPrimitive(Scancode.DIK_A), Scancode.class, null);
			Assertions.assertSame(expected, result);
		}

		@Test
		@DisplayName("throws JsonParseException when the JSON element is not a primitive")
		void throwsForNonPrimitiveJson() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonObject(), Scancode.class, null));
		}

		@Test
		@DisplayName("throws JsonParseException when the number does not match any key code")
		void throwsForUnknownKeyCode() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonPrimitive(-1), Scancode.class, null));
		}

		@Test
		@DisplayName("throws JsonParseException when the string does not match any known scancode name")
		void throwsForUnknownStringName() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonPrimitive("DIK_UNKNOWN_KEY"), Scancode.class, null));
		}
	}

	@Nested
	@DisplayName("serialize()")
	class SerializeTests {

		@Test
		@DisplayName("returns a JsonPrimitive containing the scancode's name")
		void serializesAsName() {
			final var adapter = createAdapter();
			final var scancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			final var result = adapter.serialize(scancode, Scancode.class, null);
			Assertions.assertInstanceOf(JsonPrimitive.class, result);
			Assertions.assertEquals(Scancode.DIK_A, result.getAsString());
		}

		@Test
		@DisplayName("serializes ESCAPE scancode as its name string")
		void serializesEscapeAsName() {
			final var adapter = createAdapter();
			final var scancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_ESCAPE);
			final var result = adapter.serialize(scancode, Scancode.class, null);
			Assertions.assertEquals(Scancode.DIK_ESCAPE, result.getAsString());
		}
	}
}

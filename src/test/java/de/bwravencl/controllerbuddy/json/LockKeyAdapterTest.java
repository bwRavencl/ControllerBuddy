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
import de.bwravencl.controllerbuddy.input.LockKey;
import java.awt.event.KeyEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LockKeyAdapterTest {

	private static LockKeyAdapter createAdapter() {
		return new LockKeyAdapter();
	}

	@Nested
	@DisplayName("deserialize()")
	class DeserializeTests {

		@Test
		@DisplayName("deserializes a JSON string by looking up the name in NAME_TO_LOCK_KEY_MAP")
		void deserializesFromStringName() {
			final var adapter = createAdapter();
			final var result = adapter.deserialize(new JsonPrimitive(LockKey.CAPS_LOCK), LockKey.class, null);
			Assertions.assertSame(LockKey.CAPS_LOCK_LOCK_KEY, result);
		}

		@Test
		@DisplayName("deserializes a JSON number by looking up the virtual key code in VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP")
		void deserializesFromVirtualKeyCode() {
			final var adapter = createAdapter();
			final var result = adapter.deserialize(new JsonPrimitive(KeyEvent.VK_CAPS_LOCK), LockKey.class, null);
			Assertions.assertSame(LockKey.CAPS_LOCK_LOCK_KEY, result);
		}

		@Test
		@DisplayName("deserializes NUM_LOCK from its virtual key code")
		void deserializesNumLockFromVirtualKeyCode() {
			final var adapter = createAdapter();
			final var result = adapter.deserialize(new JsonPrimitive(KeyEvent.VK_NUM_LOCK), LockKey.class, null);
			Assertions.assertSame(LockKey.NUM_LOCK_LOCK_KEY, result);
		}

		@Test
		@DisplayName("throws JsonParseException when the JSON element is not a primitive")
		void throwsForNonPrimitiveJson() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonObject(), LockKey.class, null));
		}

		@Test
		@DisplayName("throws JsonParseException when the string does not match any known lock key name")
		void throwsForUnknownStringName() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonPrimitive("UNKNOWN_KEY"), LockKey.class, null));
		}

		@Test
		@DisplayName("throws JsonParseException when the number does not match any virtual key code")
		void throwsForUnknownVirtualKeyCode() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonPrimitive(-1), LockKey.class, null));
		}
	}

	@Nested
	@DisplayName("serialize()")
	class SerializeTests {

		@Test
		@DisplayName("returns a JsonPrimitive containing the lock key's name")
		void serializesAsName() {
			final var adapter = createAdapter();
			final var result = adapter.serialize(LockKey.CAPS_LOCK_LOCK_KEY, LockKey.class, null);
			Assertions.assertInstanceOf(JsonPrimitive.class, result);
			Assertions.assertEquals(LockKey.CAPS_LOCK, result.getAsString());
		}

		@Test
		@DisplayName("serializes NUM_LOCK_LOCK_KEY as its name string")
		void serializesNumLockAsName() {
			final var adapter = createAdapter();
			final var result = adapter.serialize(LockKey.NUM_LOCK_LOCK_KEY, LockKey.class, null);
			Assertions.assertEquals(LockKey.NUM_LOCK, result.getAsString());
		}
	}
}

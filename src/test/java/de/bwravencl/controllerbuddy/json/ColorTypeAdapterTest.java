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
import java.awt.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ColorTypeAdapterTest {

	private static ColorTypeAdapter createAdapter() {
		return new ColorTypeAdapter();
	}

	@Nested
	@DisplayName("deserialize()")
	final class DeserializeTests {

		@Test
		@DisplayName("deserializes a JSON number as an RGBA integer")
		void deserializesFromNumber() {
			final var adapter = createAdapter();
			final var color = Color.RED;
			final var result = adapter.deserialize(new JsonPrimitive(color.getRGB()), Color.class, null);
			Assertions.assertEquals(color.getRGB(), result.getRGB());
		}

		@Test
		@DisplayName("deserializes a JSON object with a numeric 'value' field")
		void deserializesFromObjectWithValueField() {
			final var adapter = createAdapter();
			final var color = Color.BLUE;
			final var jsonObject = new JsonObject();
			jsonObject.addProperty("value", color.getRGB());
			final var result = adapter.deserialize(jsonObject, Color.class, null);
			Assertions.assertEquals(color.getRGB(), result.getRGB());
		}

		@Test
		@DisplayName("preserves the alpha channel when deserializing from a number")
		void preservesAlphaChannel() {
			final var adapter = createAdapter();
			final var color = new Color(100, 150, 200, 128);
			final var result = adapter.deserialize(new JsonPrimitive(color.getRGB()), Color.class, null);
			Assertions.assertEquals(color.getRGB(), result.getRGB());
			Assertions.assertEquals(color.getAlpha(), result.getAlpha());
		}

		@Test
		@DisplayName("throws JsonParseException when the JSON object's 'value' field is not a number")
		void throwsForObjectWithNonNumericValueField() {
			final var adapter = createAdapter();
			final var jsonObject = new JsonObject();
			jsonObject.addProperty("value", "red");
			Assertions.assertThrows(JsonParseException.class, () -> adapter.deserialize(jsonObject, Color.class, null));
		}

		@Test
		@DisplayName("throws JsonParseException when the JSON object has no 'value' field")
		void throwsForObjectWithoutValueField() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonObject(), Color.class, null));
		}

		@Test
		@DisplayName("throws JsonParseException when the JSON element is a string")
		void throwsForStringJson() {
			final var adapter = createAdapter();
			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(new JsonPrimitive("red"), Color.class, null));
		}
	}

	@Nested
	@DisplayName("serialize()")
	final class SerializeTests {

		@Test
		@DisplayName("round-trips a color with alpha through serialize then deserialize")
		void roundTripsColorWithAlpha() {
			final var adapter = createAdapter();
			final var original = new Color(10, 20, 30, 200);
			final var serialized = adapter.serialize(original, Color.class, null);
			final var deserialized = adapter.deserialize(serialized, Color.class, null);
			Assertions.assertEquals(original.getRGB(), deserialized.getRGB());
		}

		@Test
		@DisplayName("returns a JsonPrimitive containing the color's RGB value")
		void serializesAsRgbInteger() {
			final var adapter = createAdapter();
			final var color = Color.GREEN;
			final var result = adapter.serialize(color, Color.class, null);
			Assertions.assertInstanceOf(JsonPrimitive.class, result);
			Assertions.assertEquals(color.getRGB(), result.getAsInt());
		}
	}
}

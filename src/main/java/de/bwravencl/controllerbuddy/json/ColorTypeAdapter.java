/*
 * Copyright (C) 2021 Matteo Hausner
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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.awt.Color;
import java.lang.reflect.Type;

/// Gson type adapter for serializing and deserializing [Color] instances.
///
/// Supports deserializing from either a JSON number (interpreted as an RGBA
/// integer) or a JSON object with a `value` member. Serializes a [Color] as its
/// RGB integer representation.
public final class ColorTypeAdapter implements JsonSerializer<Color>, JsonDeserializer<Color> {

	/// Deserializes a [Color] from a JSON element.
	///
	/// Accepts either a JSON number representing an RGBA value or a JSON object
	/// containing a numeric `value` member. Throws [JsonParseException] if the
	/// element cannot be interpreted as a color.
	///
	/// @param json the JSON element to deserialize
	/// @param typeOfT the target type
	/// @param context the deserialization context
	/// @return the deserialized [Color]
	@Override
	public Color deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
			throws JsonParseException {
		try {
			Integer rgba = null;
			if (json.isJsonPrimitive() && ((JsonPrimitive) json).isNumber()) {
				rgba = json.getAsInt();
			} else if (json.isJsonObject()) {
				final var jsonObject = json.getAsJsonObject();

				final var valueMember = jsonObject.get("value");
				if (valueMember != null && valueMember.isJsonPrimitive() && ((JsonPrimitive) valueMember).isNumber()) {
					rgba = valueMember.getAsInt();
				}
			}

			if (rgba == null) {
				throw new JsonParseException("Could not deserialize as " + Color.class.getSimpleName() + ": " + json);
			}

			return new Color(rgba, true);
		} catch (final JsonParseException e) {
			throw e;
		} catch (final Throwable t) {
			throw new JsonParseException(t);
		}
	}

	/// Serializes a [Color] as a JSON primitive containing its RGB integer value.
	///
	/// @param src the color to serialize
	/// @param typeOfSrc the source type
	/// @param context the serialization context
	/// @return a [JsonPrimitive] containing the RGB integer
	@Override
	public JsonElement serialize(final Color src, final Type typeOfSrc, final JsonSerializationContext context) {
		return new JsonPrimitive(src.getRGB());
	}
}

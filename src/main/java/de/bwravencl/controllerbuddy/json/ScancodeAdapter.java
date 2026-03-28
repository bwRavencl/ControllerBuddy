/*
 * Copyright (C) 2022 Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.Scancode;
import java.lang.reflect.Type;

/// Gson type adapter for serializing and deserializing [Scancode] instances.
///
/// Supports deserializing from either a JSON number (key code) or a JSON string
/// (scancode name). Serializes a [Scancode] using its name.
public final class ScancodeAdapter implements JsonSerializer<Scancode>, JsonDeserializer<Scancode> {

	/// Deserializes a [Scancode] from a JSON element.
	///
	/// Accepts either a numeric key code or a string name to look up the
	/// corresponding [Scancode]. Throws [JsonParseException] if no matching scan
	/// code is found.
	///
	/// @param json the JSON element to deserialize
	/// @param typeOfT the target type
	/// @param context the deserialization context
	/// @return the deserialized [Scancode]
	@Override
	public Scancode deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
			throws JsonParseException {
		try {
			Scancode scancode = null;
			if (json.isJsonPrimitive()) {
				final var jsonPrimitive = (JsonPrimitive) json;

				if (jsonPrimitive.isNumber()) {
					scancode = Scancode.KEY_CODE_TO_SCAN_CODE_MAP.get(jsonPrimitive.getAsInt());
				} else if (jsonPrimitive.isString()) {
					scancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(jsonPrimitive.getAsString());
				}
			}

			if (scancode == null) {
				throw new JsonParseException(
						"Could not deserialize as " + Scancode.class.getSimpleName() + ": " + json);
			}

			return scancode;
		} catch (final JsonParseException e) {
			throw e;
		} catch (final Throwable t) {
			throw new JsonParseException(t);
		}
	}

	/// Serializes a [Scancode] as a JSON primitive containing its name.
	///
	/// @param src the scancode to serialize
	/// @param typeOfSrc the source type
	/// @param context the serialization context
	/// @return a [JsonPrimitive] containing the scancode name
	@Override
	public JsonElement serialize(final Scancode src, final Type typeOfSrc, final JsonSerializationContext context) {
		return new JsonPrimitive(src.name());
	}
}

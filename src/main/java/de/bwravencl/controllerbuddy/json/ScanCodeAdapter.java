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
import de.bwravencl.controllerbuddy.input.ScanCode;
import java.lang.reflect.Type;

/// Gson type adapter for serializing and deserializing [ScanCode] instances.
///
/// Supports deserializing from either a JSON number (key code) or a JSON string
/// (scan code name). Serializes a [ScanCode] using its name.
public final class ScanCodeAdapter implements JsonSerializer<ScanCode>, JsonDeserializer<ScanCode> {

	/// Deserializes a [ScanCode] from a JSON element.
	///
	/// Accepts either a numeric key code or a string name to look up the
	/// corresponding [ScanCode]. Throws [JsonParseException] if no matching scan
	/// code is found.
	///
	/// @param json the JSON element to deserialize
	/// @param typeOfT the target type
	/// @param context the deserialization context
	/// @return the deserialized [ScanCode]
	@Override
	public ScanCode deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
			throws JsonParseException {
		try {
			ScanCode scanCode = null;
			if (json.isJsonPrimitive()) {
				final var jsonPrimitive = (JsonPrimitive) json;

				if (jsonPrimitive.isNumber()) {
					scanCode = ScanCode.KEY_CODE_TO_SCAN_CODE_MAP.get(jsonPrimitive.getAsInt());
				} else if (jsonPrimitive.isString()) {
					scanCode = ScanCode.NAME_TO_SCAN_CODE_MAP.get(jsonPrimitive.getAsString());
				}
			}

			if (scanCode == null) {
				throw new JsonParseException(
						"Could not deserialize as " + ScanCode.class.getSimpleName() + ": " + json);
			}

			return scanCode;
		} catch (final JsonParseException e) {
			throw e;
		} catch (final Throwable t) {
			throw new JsonParseException(t);
		}
	}

	/// Serializes a [ScanCode] as a JSON primitive containing its name.
	///
	/// @param src the scan code to serialize
	/// @param typeOfSrc the source type
	/// @param context the serialization context
	/// @return a [JsonPrimitive] containing the scan code name
	@Override
	public JsonElement serialize(final ScanCode src, final Type typeOfSrc, final JsonSerializationContext context) {
		return new JsonPrimitive(src.name());
	}
}

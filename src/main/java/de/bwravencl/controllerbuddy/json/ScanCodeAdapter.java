/* Copyright (C) 2022  Matteo Hausner
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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.bwravencl.controllerbuddy.input.ScanCode;
import java.lang.reflect.Type;

public final class ScanCodeAdapter implements JsonSerializer<ScanCode>, JsonDeserializer<ScanCode> {

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

	@Override
	public JsonElement serialize(final ScanCode src, final Type typeOfSrc, final JsonSerializationContext context) {
		return new JsonPrimitive(src.name());
	}
}

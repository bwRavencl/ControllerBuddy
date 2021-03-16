/* Copyright (C) 2021  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.controllerbuddy.json;

import java.awt.Color;
import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class ColorTypeAdapter implements JsonSerializer<Color>, JsonDeserializer<Color> {

	@Override
	public Color deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
			throws JsonParseException {
		try {
			Integer rgba = null;
			if (json.isJsonPrimitive() && ((JsonPrimitive) json).isNumber())
				rgba = json.getAsInt();
			else if (json.isJsonObject()) {
				final var jsonObject = json.getAsJsonObject();

				final var valueMember = jsonObject.get("value");
				if (valueMember != null && valueMember.isJsonPrimitive() && ((JsonPrimitive) valueMember).isNumber())
					rgba = valueMember.getAsInt();
			}

			if (rgba == null)
				throw new JsonParseException("Could not deserialize as color: " + json.toString());

			return new Color(rgba, true);
		} catch (final JsonParseException e) {
			throw e;
		} catch (final Throwable t) {
			throw new JsonParseException(t);
		}
	}

	@Override
	public JsonElement serialize(final Color src, final Type typeOfSrc, final JsonSerializationContext context) {
		return new JsonPrimitive(src.getRGB());
	}
}

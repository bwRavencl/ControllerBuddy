/* Copyright (C) 2016  Matteo Hausner
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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

	private static final String PROPERTY_TYPE = "type";
	private static final String PROPERTY_DATA = "data";

	@Override
	public T deserialize(JsonElement elem, java.lang.reflect.Type interfaceType, JsonDeserializationContext context)
			throws JsonParseException {
		final JsonObject wrapper = (JsonObject) elem;
		final JsonElement typeName = get(wrapper, PROPERTY_TYPE);
		final JsonElement data = get(wrapper, PROPERTY_DATA);
		final java.lang.reflect.Type actualType = typeForName(typeName);

		return context.deserialize(data, actualType);
	}

	private JsonElement get(final JsonObject wrapper, String memberName) {
		final JsonElement elem = wrapper.get(memberName);
		if (elem == null)
			throw new JsonParseException(getClass().getName() + ": No member '" + memberName
					+ "' found in what was expected to be an interface wrapper");
		return elem;
	}

	@Override
	public JsonElement serialize(T object, java.lang.reflect.Type interfaceType, JsonSerializationContext context) {
		final JsonObject wrapper = new JsonObject();
		wrapper.addProperty(PROPERTY_TYPE, object.getClass().getName());
		wrapper.add(PROPERTY_DATA, context.serialize(object));

		return wrapper;
	}

	private java.lang.reflect.Type typeForName(final JsonElement typeElem) {
		try {
			return Class.forName(typeElem.getAsString());
		} catch (final ClassNotFoundException e) {
			throw new JsonParseException(getClass().getName() + ": " + e);
		}
	}

}
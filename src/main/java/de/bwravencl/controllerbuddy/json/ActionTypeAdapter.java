/* Copyright (C) 2019  Matteo Hausner
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

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.NullAction;

public final class ActionTypeAdapter implements JsonSerializer<IAction<?>>, JsonDeserializer<IAction<?>> {

	private static final Logger log = Logger.getLogger(ActionTypeAdapter.class.getName());

	private static final String PROPERTY_TYPE = "type";
	private static final String PROPERTY_DATA = "data";

	private static JsonElement get(final JsonObject wrapper, final String memberName) {
		final var jsonElement = wrapper.get(memberName);
		if (jsonElement == null)
			throw new JsonParseException(
					"No member '" + memberName + "' found in what was expected to be an interface wrapper");

		return jsonElement;
	}

	private final Set<String> unknownActionClasses = new HashSet<>();

	@Override
	public IAction<?> deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
			throws JsonParseException {
		final var wrapper = json.getAsJsonObject();
		final var typeName = get(wrapper, PROPERTY_TYPE);
		final var typeNameString = typeName.getAsString();
		final var data = get(wrapper, PROPERTY_DATA);

		try {
			final var actualType = Class.forName(typeNameString);
			return context.deserialize(data, actualType);
		} catch (final ClassNotFoundException e) {
			if (typeOfT == IAction.class) {
				log.log(Level.WARNING, "Action class '" + typeNameString + "' not found, substituting with '"
						+ NullAction.class.getSimpleName() + "'");
				unknownActionClasses.add(typeNameString);

				return new NullAction();
			}

			throw new JsonParseException(e);
		}
	}

	public Set<String> getUnknownActionClasses() {
		return unknownActionClasses;
	}

	@Override
	public JsonElement serialize(final IAction<?> src, final Type typeOfSrc, final JsonSerializationContext context) {
		final var wrapper = new JsonObject();
		wrapper.addProperty(PROPERTY_TYPE, src.getClass().getName());
		wrapper.add(PROPERTY_DATA, context.serialize(src));

		return wrapper;
	}
}

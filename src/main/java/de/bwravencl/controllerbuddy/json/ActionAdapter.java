/* Copyright (C) 2018  Matteo Hausner
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

import java.lang.System.Logger;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.NullAction;

public class ActionAdapter implements JsonSerializer<IAction>, JsonDeserializer<IAction> {

	private static final System.Logger log = System.getLogger(ActionAdapter.class.getName());

	private static final String PROPERTY_TYPE = "type";
	private static final String PROPERTY_DATA = "data";

	private static JsonElement get(final JsonObject wrapper, final String memberName) {
		final JsonElement elem = wrapper.get(memberName);
		if (elem == null)
			throw new JsonParseException(
					"No member '" + memberName + "' found in what was expected to be an interface wrapper");

		return elem;
	}

	private final Set<String> unknownActionClasses = new HashSet<>();

	@Override
	public IAction deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
			throws JsonParseException {
		final JsonObject wrapper = json.getAsJsonObject();
		final JsonElement typeName = get(wrapper, PROPERTY_TYPE);
		final String typeNameString = typeName.getAsString();
		final JsonElement data = get(wrapper, PROPERTY_DATA);

		try {
			final Type actualType = Class.forName(typeNameString);
			return context.deserialize(data, actualType);
		} catch (final ClassNotFoundException e) {
			if (typeOfT == IAction.class) {
				log.log(Logger.Level.WARNING, "Action class '" + typeNameString + "' not found, substituting with '"
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
	public JsonElement serialize(final IAction src, final Type typeOfSrc, final JsonSerializationContext context) {
		final JsonObject wrapper = new JsonObject();
		wrapper.addProperty(PROPERTY_TYPE, src.getClass().getName());
		wrapper.add(PROPERTY_DATA, context.serialize(src));

		return wrapper;
	}

}
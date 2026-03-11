/* Copyright (C) 2026  Matteo Hausner
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.NullAction;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActionTypeAdapterTest {

	@Mock
	IAction<?> mockAction;

	@Mock
	JsonDeserializationContext mockDeserializationContext;

	@Mock
	JsonSerializationContext mockSerializationContext;

	private ActionTypeAdapter createAdapter() {
		return new ActionTypeAdapter();
	}

	@Nested
	@DisplayName("deserialize()")
	class DeserializeTests {

		@Test
		@DisplayName("delegates to context and returns the deserialized action when the class is found")
		void deserializesKnownClass() {
			final var adapter = createAdapter();
			final var data = new JsonObject();
			final var wrapper = new JsonObject();
			wrapper.addProperty("type", NullAction.class.getName());
			wrapper.add("data", data);

			Mockito.when(mockDeserializationContext.deserialize(data, NullAction.class)).thenReturn(new NullAction());

			final var result = adapter.deserialize(wrapper, IAction.class, mockDeserializationContext);

			Assertions.assertInstanceOf(NullAction.class, result);
			Mockito.verify(mockDeserializationContext).deserialize(data, NullAction.class);
		}

		@Test
		@DisplayName("wraps ClassNotFoundException in a JsonParseException when the class is not found and typeOfT is not IAction")
		void rethrowsAsJsonParseExceptionForUnknownClassWhenTypeIsNotIAction() {
			final var adapter = createAdapter();
			final var wrapper = new JsonObject();
			wrapper.addProperty("type", "com.example.NonExistentAction");
			wrapper.add("data", new JsonObject());

			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(wrapper, String.class, mockDeserializationContext));
		}

		@Test
		@DisplayName("returns a NullAction and records the class name when the class is not found and typeOfT is IAction")
		void substitutesNullActionForUnknownClassWhenTypeIsIAction() {
			final var adapter = createAdapter();
			final var wrapper = new JsonObject();
			wrapper.addProperty("type", "com.example.NonExistentAction");
			wrapper.add("data", new JsonObject());

			final var result = adapter.deserialize(wrapper, IAction.class, mockDeserializationContext);

			Assertions.assertInstanceOf(NullAction.class, result);
			Assertions.assertTrue(adapter.getUnknownActionClasses().contains("com.example.NonExistentAction"));
		}

		@Test
		@DisplayName("records the unknown class name when typeOfT is a ParameterizedType whose raw type is IAction")
		void substitutesNullActionForUnknownClassWhenTypeIsParameterizedIAction() {
			final var adapter = createAdapter();
			final var wrapper = new JsonObject();
			wrapper.addProperty("type", "com.example.NonExistentAction");
			wrapper.add("data", new JsonObject());

			// Construct a ParameterizedType whose raw type resolves to IAction.class
			final var parameterizedType = new ParameterizedType() {

				@Override
				public Type[] getActualTypeArguments() {
					return new Type[] { Object.class };
				}

				@Override
				public Type getOwnerType() {
					return null;
				}

				@Override
				public Type getRawType() {
					return IAction.class;
				}
			};

			final var result = adapter.deserialize(wrapper, parameterizedType, mockDeserializationContext);

			Assertions.assertInstanceOf(NullAction.class, result);
			Assertions.assertTrue(adapter.getUnknownActionClasses().contains("com.example.NonExistentAction"));
		}

		@Test
		@DisplayName("throws JsonParseException when the 'data' property is missing from the wrapper")
		void throwsWhenDataMissing() {
			final var adapter = createAdapter();
			final var wrapper = new JsonObject();
			wrapper.addProperty("type", NullAction.class.getName());

			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(wrapper, IAction.class, mockDeserializationContext));
		}

		@Test
		@DisplayName("throws JsonParseException when the 'type' property is missing from the wrapper")
		void throwsWhenTypeMissing() {
			final var adapter = createAdapter();
			final var wrapper = new JsonObject();
			wrapper.add("data", new JsonObject());

			Assertions.assertThrows(JsonParseException.class,
					() -> adapter.deserialize(wrapper, IAction.class, mockDeserializationContext));
		}
	}

	@Nested
	@DisplayName("serialize()")
	class SerializeTests {

		@Test
		@DisplayName("uses the serialized data element returned by the context as the 'data' property")
		void delegatesSerializationToContext() {
			final var adapter = createAdapter();
			final var serializedData = new JsonPrimitive("encoded");

			Mockito.when(mockSerializationContext.serialize(mockAction)).thenReturn(serializedData);

			final var result = adapter.serialize(mockAction, IAction.class, mockSerializationContext);

			Assertions.assertEquals(serializedData, result.getAsJsonObject().get("data"));
			Mockito.verify(mockSerializationContext).serialize(mockAction);
		}

		@Test
		@DisplayName("produces a JsonObject with 'type' set to the action's class name and 'data' set to the serialized action")
		void producesWrapperWithTypeAndData() {
			final var adapter = createAdapter();
			final var serializedData = new JsonObject();
			serializedData.addProperty("someField", "someValue");

			Mockito.when(mockSerializationContext.serialize(mockAction)).thenReturn(serializedData);

			final var result = adapter.serialize(mockAction, IAction.class, mockSerializationContext);

			Assertions.assertInstanceOf(JsonObject.class, result);
			final var resultObject = result.getAsJsonObject();
			// mockAction.getClass() returns the Mockito proxy class at runtime;
			// the adapter calls src.getClass().getName() on the same object, so
			// comparing against mockAction.getClass().getName() is always correct.
			Assertions.assertEquals(mockAction.getClass().getName(), resultObject.get("type").getAsString());
			Assertions.assertEquals(serializedData, resultObject.get("data"));
		}
	}
}

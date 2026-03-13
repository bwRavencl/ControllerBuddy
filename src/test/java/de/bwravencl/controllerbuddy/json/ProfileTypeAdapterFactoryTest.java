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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.OverlayAxis.OverlayAxisOrientation;
import de.bwravencl.controllerbuddy.input.OverlayAxis.OverlayAxisStyle;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.ButtonToButtonAction;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import java.awt.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDLGamepad;

class ProfileTypeAdapterFactoryTest {

	private static com.google.gson.Gson createGson() {
		return new GsonBuilder().registerTypeAdapterFactory(new ProfileTypeAdapterFactory())
				.registerTypeAdapter(Color.class, new ColorTypeAdapter()).create();
	}

	@Nested
	@DisplayName("create() - enum adapter for Activation")
	class ActivationEnumAdapterTests {

		@Test
		@DisplayName("reads a current Activation value by its enum name")
		void readsCurrentActivationValue() {
			final var gson = createGson();
			Assertions.assertEquals(Activation.WHILE_PRESSED, gson.fromJson("\"WHILE_PRESSED\"", Activation.class));
			Assertions.assertEquals(Activation.ON_PRESS, gson.fromJson("\"ON_PRESS\"", Activation.class));
			Assertions.assertEquals(Activation.ON_RELEASE, gson.fromJson("\"ON_RELEASE\"", Activation.class));
		}

		@Test
		@DisplayName("reads JSON null as null")
		void readsNullAsNull() {
			final var gson = createGson();
			Assertions.assertNull(gson.fromJson("null", Activation.class));
		}

		@Test
		@DisplayName("remaps legacy 'REPEAT' to WHILE_PRESSED")
		void remapsLegacyRepeatToWhilePressed() {
			final var gson = createGson();
			Assertions.assertEquals(Activation.WHILE_PRESSED, gson.fromJson("\"REPEAT\"", Activation.class));
		}

		@Test
		@DisplayName("remaps legacy 'SINGLE_IMMEDIATELY' to ON_PRESS")
		void remapsLegacySingleImmediatelyToOnPress() {
			final var gson = createGson();
			Assertions.assertEquals(Activation.ON_PRESS, gson.fromJson("\"SINGLE_IMMEDIATELY\"", Activation.class));
		}

		@Test
		@DisplayName("remaps legacy 'SINGLE_ON_RELEASE' to ON_RELEASE")
		void remapsLegacySingleOnReleaseToOnRelease() {
			final var gson = createGson();
			Assertions.assertEquals(Activation.ON_RELEASE, gson.fromJson("\"SINGLE_ON_RELEASE\"", Activation.class));
		}

		@Test
		@DisplayName("throws JsonParseException for an unrecognised enum value")
		void throwsForUnrecognisedValue() {
			final var gson = createGson();
			Assertions.assertThrows(JsonParseException.class, () -> gson.fromJson("\"BOGUS_VALUE\"", Activation.class));
		}

		@Test
		@DisplayName("writes an Activation value using the standard enum serialization")
		void writesActivationByName() {
			final var gson = createGson();
			Assertions.assertEquals("\"WHILE_PRESSED\"", gson.toJson(Activation.WHILE_PRESSED, Activation.class));
		}
	}

	@Nested
	@DisplayName("create() - IDelayableAction adapter")
	class DelayableActionAdapterTests {

		@Test
		@DisplayName("does not set delay when 'longPress' is absent from the JSON")
		void doesNotSetDelayWhenLongPressIsAbsent() {
			final var gson = createGson();
			final var action = gson.fromJson("{}", ButtonToButtonAction.class);
			Assertions.assertEquals(0L, action.getDelay());
		}

		@Test
		@DisplayName("does not set delay when 'longPress' is false in the JSON")
		void doesNotSetDelayWhenLongPressIsFalse() {
			final var gson = createGson();
			final var action = gson.fromJson("{\"longPress\": false}", ButtonToButtonAction.class);
			Assertions.assertEquals(0L, action.getDelay());
		}

		@Test
		@DisplayName("sets delay to 1000 ms when 'longPress' is true in the JSON")
		void setsDelayWhenLongPressIsTrue() {
			final var gson = createGson();
			final var action = gson.fromJson("{\"longPress\": true}", ButtonToButtonAction.class);
			Assertions.assertEquals(1000L, action.getDelay());
		}
	}

	@Nested
	@DisplayName("create() - Mode post-processing")
	class ModeAdapterTests {

		@Test
		@DisplayName("clears the description from the JSON output when serializing DEFAULT_MODE")
		void clearsDescriptionInJsonWhenSerializingDefaultMode() {
			final var gson = createGson();
			final var json = gson.toJson(Profile.DEFAULT_MODE, Mode.class);
			final var jsonObject = JsonParser.parseString(json).getAsJsonObject();
			Assertions.assertFalse(jsonObject.has("description"));
		}

		@Test
		@DisplayName("does not overwrite description for a regular mode")
		void doesNotOverwriteRegularModeDescription() {
			final var gson = createGson();
			final var mode = gson.fromJson("{\"description\": \"My Custom Mode\"}", Mode.class);
			Assertions.assertEquals("My Custom Mode", mode.getDescription());
		}

		@Test
		@DisplayName("restores the description on DEFAULT_MODE after serialization")
		void restoresDescriptionAfterSerializingDefaultMode() {
			final var gson = createGson();
			var _ = gson.toJson(Profile.DEFAULT_MODE, Mode.class);
			Assertions.assertEquals(Main.STRINGS.getString("DEFAULT_MODE_DESCRIPTION"),
					Profile.DEFAULT_MODE.getDescription());
		}

		@Test
		@DisplayName("sets the DEFAULT_MODE_DESCRIPTION on a Mode whose UUID matches DEFAULT_MODE")
		void setsDefaultModeDescription() {
			final var gson = createGson();
			final var uuid = Profile.DEFAULT_MODE.getUuid();
			final var mode = gson.fromJson("{\"uuid\": \"" + uuid + "\"}", Mode.class);
			Assertions.assertEquals(Main.STRINGS.getString("DEFAULT_MODE_DESCRIPTION"), mode.getDescription());
		}

		@Test
		@DisplayName("sets the ON_SCREEN_KEYBOARD_MODE_DESCRIPTION on a Mode matching ON_SCREEN_KEYBOARD_MODE")
		void setsOnScreenKeyboardModeDescription() {
			final var gson = createGson();
			final var uuid = OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.getUuid();
			final var mode = gson.fromJson("{\"uuid\": \"" + uuid + "\"}", Mode.class);
			Assertions.assertEquals(Main.STRINGS.getString("ON_SCREEN_KEYBOARD_MODE_DESCRIPTION"),
					mode.getDescription());
		}
	}

	@Nested
	@DisplayName("create() - OverlayAxis post-processing")
	class OverlayAxisAdapterTests {

		@Test
		@DisplayName("does not override orientation when it is already set")
		void doesNotOverrideExistingOrientation() {
			final var gson = createGson();
			final var overlayAxis = gson.fromJson("{\"orientation\": \"HORIZONTAL\"}",
					de.bwravencl.controllerbuddy.input.OverlayAxis.class);
			Assertions.assertEquals(OverlayAxisOrientation.HORIZONTAL, overlayAxis.getOrientation());
		}

		@Test
		@DisplayName("does not override style when it is already set")
		void doesNotOverrideExistingStyle() {
			final var gson = createGson();
			final var overlayAxis = gson.fromJson("{\"style\": \"LINE\"}",
					de.bwravencl.controllerbuddy.input.OverlayAxis.class);
			Assertions.assertEquals(OverlayAxisStyle.LINE, overlayAxis.getStyle());
		}

		@Test
		@DisplayName("sets orientation to VERTICAL when it is null after deserialization")
		void setsOrientationToVerticalWhenNull() {
			final var gson = createGson();
			final var overlayAxis = gson.fromJson("{}", de.bwravencl.controllerbuddy.input.OverlayAxis.class);
			Assertions.assertEquals(OverlayAxisOrientation.VERTICAL, overlayAxis.getOrientation());
		}

		@Test
		@DisplayName("sets style to SOLID when it is null after deserialization")
		void setsStyleToSolidWhenNull() {
			final var gson = createGson();
			final var overlayAxis = gson.fromJson("{}", de.bwravencl.controllerbuddy.input.OverlayAxis.class);
			Assertions.assertEquals(OverlayAxisStyle.SOLID, overlayAxis.getStyle());
		}
	}

	@Nested
	@DisplayName("create() - Profile version migration")
	class ProfileVersionMigrationTests {

		@Test
		@DisplayName("does not migrate button indices for profiles with version >= 1.4")
		void doesNotMigrateButtonIndicesForCurrentVersionProfile() {
			final var gson = createGson();
			final var json = "{\"version\": \"1.4.0\", \"buttonToModeActionsMap\": {\"4\": []}, \"modes\": []}";
			final var profile = gson.fromJson(json, Profile.class);
			Assertions.assertTrue(profile.getButtonToModeActionsMap().containsKey(4));
			Assertions.assertFalse(
					profile.getButtonToModeActionsMap().containsKey(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER));
		}

		@Test
		@DisplayName("does not migrate button indices when the profile has no version")
		void doesNotMigrateWhenVersionIsNull() {
			final var gson = createGson();
			final var json = "{\"buttonToModeActionsMap\": {\"4\": []}, \"modes\": []}";
			final var profile = gson.fromJson(json, Profile.class);
			Assertions.assertTrue(profile.getButtonToModeActionsMap().containsKey(4));
		}

		@Test
		@DisplayName("migrates GLFW button indices to SDL button indices for profiles with version < 1.4")
		void migratesGlfwButtonIndicesForOldVersionProfile() {
			final var gson = createGson();
			// GLFW_GAMEPAD_BUTTON_LEFT_BUMPER = 4, maps to SDL_GAMEPAD_BUTTON_LEFT_SHOULDER
			final var json = "{\"version\": \"1.3.0\", \"buttonToModeActionsMap\": {\"4\": []}, \"modes\": []}";
			final var profile = gson.fromJson(json, Profile.class);
			Assertions.assertTrue(
					profile.getButtonToModeActionsMap().containsKey(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER));
			Assertions.assertFalse(profile.getButtonToModeActionsMap().containsKey(4));
		}
	}
}

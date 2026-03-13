/*
 * Copyright (C) 2018 Matteo Hausner
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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.OverlayAxis;
import de.bwravencl.controllerbuddy.input.OverlayAxis.OverlayAxisOrientation;
import de.bwravencl.controllerbuddy.input.OverlayAxis.OverlayAxisStyle;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import de.bwravencl.controllerbuddy.input.action.IDelayableAction;
import de.bwravencl.controllerbuddy.util.VersionUtils;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.lwjgl.sdl.SDLGamepad;

/// A Gson [TypeAdapterFactory] that provides custom type adapters for profile
/// deserialization.
///
/// Handles migration of legacy GLFW gamepad button indices to SDL equivalents,
/// applies default values for missing fields such as overlay axis orientation
/// and style, resolves legacy enum constant names for [Activation], and
/// migrates the deprecated `longPress` field to the delay mechanism on
/// [IDelayableAction] instances.
public final class ProfileTypeAdapterFactory implements TypeAdapterFactory {

	/// Legacy GLFW button index for the Back button.
	private static final int GLFW_GAMEPAD_BUTTON_BACK = 6;

	/// Legacy GLFW button index for the D-pad Down button.
	private static final int GLFW_GAMEPAD_BUTTON_DPAD_DOWN = 13;

	/// Legacy GLFW button index for the D-pad Left button.
	private static final int GLFW_GAMEPAD_BUTTON_DPAD_LEFT = 14;

	/// Legacy GLFW button index for the D-pad Right button.
	private static final int GLFW_GAMEPAD_BUTTON_DPAD_RIGHT = 12;

	/// Legacy GLFW button index for the Guide button.
	private static final int GLFW_GAMEPAD_BUTTON_GUIDE = 8;

	/// Legacy GLFW button index for the Left Bumper button.
	private static final int GLFW_GAMEPAD_BUTTON_LEFT_BUMPER = 4;

	/// Legacy GLFW button index for the Left Thumb (stick click) button.
	private static final int GLFW_GAMEPAD_BUTTON_LEFT_THUMB = 9;

	/// Legacy GLFW button index for the Right Bumper button.
	private static final int GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER = 5;

	/// Legacy GLFW button index for the Right Thumb (stick click) button.
	private static final int GLFW_GAMEPAD_BUTTON_RIGHT_THUMB = 10;

	/// Legacy GLFW button index for the Start button.
	private static final int GLFW_GAMEPAD_BUTTON_START = 7;

	/// Converts a legacy GLFW gamepad button index to its SDL equivalent.
	///
	/// GLFW and SDL use different numeric indices for the same physical buttons.
	/// This method maps the GLFW-era indices stored in old profiles to the
	/// corresponding SDL constants. Indices that have no special mapping are
	/// returned unchanged.
	///
	/// @param glfwButton the GLFW gamepad button index to convert
	/// @return the corresponding SDL gamepad button constant, or the original
	/// value if no mapping exists
	private static int convertGlfwToSdlButton(final int glfwButton) {
		return switch (glfwButton) {
		case GLFW_GAMEPAD_BUTTON_LEFT_BUMPER -> SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER;
		case GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER -> SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER;
		case GLFW_GAMEPAD_BUTTON_BACK -> SDLGamepad.SDL_GAMEPAD_BUTTON_BACK;
		case GLFW_GAMEPAD_BUTTON_START -> SDLGamepad.SDL_GAMEPAD_BUTTON_START;
		case GLFW_GAMEPAD_BUTTON_GUIDE -> SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE;
		case GLFW_GAMEPAD_BUTTON_LEFT_THUMB -> SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK;
		case GLFW_GAMEPAD_BUTTON_RIGHT_THUMB -> SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK;
		case GLFW_GAMEPAD_BUTTON_DPAD_RIGHT -> SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
		case GLFW_GAMEPAD_BUTTON_DPAD_DOWN -> SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN;
		case GLFW_GAMEPAD_BUTTON_DPAD_LEFT -> SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT;
		default -> glfwButton;
		};
	}

	/// Creates a type adapter for the given type, applying profile migration logic
	/// as needed.
	///
	/// Returns specialized adapters for enum types (with legacy name mapping),
	/// [IDelayableAction]
	/// subtypes (with `longPress` migration), and general types (with profile
	/// version migration, default mode descriptions, and overlay axis defaults).
	///
	/// @param gson the Gson instance
	/// @param type the type token for the target type
	/// @param <T> the type being adapted
	/// @return a [TypeAdapter] for the given type
	@Override
	public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		final var rawType = (Class<?>) type.getRawType();
		if (rawType.isEnum()) {
			final var delegate = gson.getDelegateAdapter(this, type);

			return new TypeAdapter<>() {

				@Override
				public T read(final JsonReader in) throws IOException {
					if (in.peek() == JsonToken.NULL) {
						in.nextNull();
						return null;
					}

					var value = in.nextString();

					if (rawType == Activation.class) {
						value = switch (value) {
						case "REPEAT" -> Activation.WHILE_PRESSED.name();
						case "SINGLE_IMMEDIATELY" -> Activation.ON_PRESS.name();
						case "SINGLE_ON_RELEASE" -> Activation.ON_RELEASE.name();
						default -> value;
						};
					}

					final var result = delegate.fromJson("\"" + value + "\"");
					if (result == null) {
						throw new JsonParseException("Invalid enum value '" + value + "' for " + rawType.getName());
					}

					return result;
				}

				@Override
				public void write(final JsonWriter out, final T value) throws IOException {
					delegate.write(out, value);
				}
			};
		} else if (IDelayableAction.class.isAssignableFrom(rawType)) {
			final var delegate = gson.getDelegateAdapter(this, type);

			return new TypeAdapter<>() {

				@Override
				public T read(final JsonReader in) throws IOException {
					final var jsonElement = gson.getAdapter(JsonElement.class).read(in);
					final var object = delegate.fromJsonTree(jsonElement);

					if (object instanceof final IDelayableAction<?> delayableAction && jsonElement.isJsonObject()) {
						final var jsonObject = jsonElement.getAsJsonObject();
						if (jsonObject.has("longPress") && jsonObject.get("longPress").getAsBoolean()) {
							delayableAction.setDelay(1000L);
						}
					}

					return object;
				}

				@Override
				public void write(final JsonWriter out, final T value) throws IOException {
					delegate.write(out, value);
				}
			};
		}

		final var delegate = gson.getDelegateAdapter(this, type);

		return new TypeAdapter<>() {

			@Override
			public T read(final JsonReader in) throws IOException {
				final var obj = delegate.read(in);

				switch (obj) {
				case final Profile profile -> {
					final var version = profile.getVersion();
					if (version == null) {
						break;
					}

					final var versionParts = VersionUtils.getVersionIntegerParts(version);
					if (versionParts.length < 2) {
						break;
					}

					if (versionParts[0] <= 1 && versionParts[1] < 4) {
						profile.setButtonToModeActionsMap(
								profile.getButtonToModeActionsMap().entrySet().stream().collect(Collectors
										.toMap((entry) -> convertGlfwToSdlButton(entry.getKey()), Entry::getValue)));

						profile.getModes().forEach(mode -> mode.setButtonToActionsMap(
								mode.getButtonToActionsMap().entrySet().stream().collect(Collectors
										.toMap((entry) -> convertGlfwToSdlButton(entry.getKey()), Entry::getValue))));
					}
				}
				case final Mode mode -> {
					if (Profile.DEFAULT_MODE.equals(mode)) {
						mode.setDescription(Main.STRINGS.getString("DEFAULT_MODE_DESCRIPTION"));
					} else if (OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.equals(mode)) {
						mode.setDescription(Main.STRINGS.getString("ON_SCREEN_KEYBOARD_MODE_DESCRIPTION"));
					}
				}
				case final OverlayAxis overlayAxis -> {
					if (overlayAxis.getOrientation() == null) {
						overlayAxis.setOrientation(OverlayAxisOrientation.VERTICAL);
					}
					if (overlayAxis.getStyle() == null) {
						overlayAxis.setStyle(OverlayAxisStyle.SOLID);
					}
				}
				case null, default -> {
				}
				}

				return obj;
			}

			@Override
			public void write(final JsonWriter out, final T value) throws IOException {
				final var delegate = gson.getDelegateAdapter(ProfileTypeAdapterFactory.this, type);

				String prevDescription = null;
				try {
					if (value instanceof final Mode mode && (Profile.DEFAULT_MODE.equals(value)
							|| OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.equals(value))) {
						prevDescription = mode.getDescription();
						mode.setDescription(null);
					}

					delegate.write(out, value);
				} finally {
					if (prevDescription != null) {
						((Mode) value).setDescription(prevDescription);
					}
				}
			}
		};
	}
}

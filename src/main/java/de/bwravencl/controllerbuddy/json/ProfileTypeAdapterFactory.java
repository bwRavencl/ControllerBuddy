/* Copyright (C) 2018  Matteo Hausner
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

import com.google.gson.Gson;
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
import de.bwravencl.controllerbuddy.util.VersionUtils;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.lwjgl.sdl.SDLGamepad;

public final class ProfileTypeAdapterFactory implements TypeAdapterFactory {

	private static final int GLFW_GAMEPAD_BUTTON_BACK = 6;

	private static final int GLFW_GAMEPAD_BUTTON_DPAD_DOWN = 13;

	private static final int GLFW_GAMEPAD_BUTTON_DPAD_LEFT = 14;

	private static final int GLFW_GAMEPAD_BUTTON_DPAD_RIGHT = 12;

	private static final int GLFW_GAMEPAD_BUTTON_GUIDE = 8;

	private static final int GLFW_GAMEPAD_BUTTON_LEFT_BUMPER = 4;

	private static final int GLFW_GAMEPAD_BUTTON_LEFT_THUMB = 9;

	private static final int GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER = 5;

	private static final int GLFW_GAMEPAD_BUTTON_RIGHT_THUMB = 10;

	private static final int GLFW_GAMEPAD_BUTTON_START = 7;

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

					final var value = in.nextString();
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

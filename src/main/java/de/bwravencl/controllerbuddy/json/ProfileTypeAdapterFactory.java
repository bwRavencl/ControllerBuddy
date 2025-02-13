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
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.Mode;
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
					if (Profile.defaultMode.equals(mode)) {
						mode.setDescription(Main.strings.getString("DEFAULT_MODE_DESCRIPTION"));
					} else if (OnScreenKeyboard.onScreenKeyboardMode.equals(mode)) {
						mode.setDescription(Main.strings.getString("ON_SCREEN_KEYBOARD_MODE_DESCRIPTION"));
					}
				}
				default -> {
				}
				}

				return obj;
			}

			@Override
			public void write(final JsonWriter out, final T value) throws IOException {
				final var delegate = gson.getDelegateAdapter(ProfileTypeAdapterFactory.this, type);

				String prevDescription = null;
				try {
					if (value instanceof final Mode mode && (Profile.defaultMode.equals(value)
							|| OnScreenKeyboard.onScreenKeyboardMode.equals(value))) {
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

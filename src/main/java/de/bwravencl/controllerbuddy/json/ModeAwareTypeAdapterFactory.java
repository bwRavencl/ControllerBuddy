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

import static de.bwravencl.controllerbuddy.gui.Main.strings;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;

public final class ModeAwareTypeAdapterFactory implements TypeAdapterFactory {

	@Override
	public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
		final var delegate = gson.getDelegateAdapter(this, type);

		return new TypeAdapter<>() {

			@Override
			public T read(final JsonReader in) throws IOException {
				final var obj = delegate.read(in);

				if (obj instanceof Mode) {
					final var mode = (Mode) obj;

					if (Profile.defaultMode.equals(mode))
						mode.setDescription(strings.getString("DEFAULT_MODE_DESCRIPTION"));
					else if (OnScreenKeyboard.onScreenKeyboardMode.equals(mode))
						mode.setDescription(strings.getString("ON_SCREEN_KEYBOARD_MODE_DESCRIPTION"));
				}

				return obj;
			}

			@Override
			public void write(final JsonWriter out, final T value) throws IOException {
				final var delegate = gson.getDelegateAdapter(ModeAwareTypeAdapterFactory.this, type);

				String prevDescription = null;
				try {
					if (value instanceof Mode)
						if (Profile.defaultMode.equals(value) || OnScreenKeyboard.onScreenKeyboardMode.equals(value)) {
							final var mode = (Mode) value;
							prevDescription = mode.getDescription();
							mode.setDescription(null);
						}

					delegate.write(out, value);
				} finally {
					if (prevDescription != null)
						((Mode) value).setDescription(prevDescription);
				}
			}
		};
	}
}

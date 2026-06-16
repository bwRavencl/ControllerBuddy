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

package de.bwravencl.controllerbuddy.input;

import de.bwravencl.controllerbuddy.gui.Main;
import java.util.EnumSet;
import org.lwjgl.sdl.SDLGamepad;

/// Enumeration of gamepad buttons that can be assigned as the hot-swapping
/// trigger.
///
/// Each constant maps a human-readable label to its corresponding SDL button
/// ID. [#NONE] represents the unassigned state with an ID of `-1`.
public enum HotSwappingButton {

	/// No button assigned for hot-swapping.
	NONE(-1, "NONE"),

	/// The A (south) gamepad button.
	A(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, "A_BUTTON"),

	/// The B (east) gamepad button.
	B(SDLGamepad.SDL_GAMEPAD_BUTTON_EAST, "B_BUTTON"),

	/// The X (west) gamepad button.
	X(SDLGamepad.SDL_GAMEPAD_BUTTON_WEST, "X_BUTTON"),

	/// The Y (north) gamepad button.
	Y(SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH, "Y_BUTTON"),

	/// The left shoulder gamepad button.
	LEFT_SHOULDER(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER, "LEFT_SHOULDER"),

	/// The right shoulder gamepad button.
	RIGHT_SHOULDER(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER, "RIGHT_SHOULDER"),

	/// The back gamepad button.
	BACK(SDLGamepad.SDL_GAMEPAD_BUTTON_BACK, "BACK_BUTTON"),

	/// The start gamepad button.
	START(SDLGamepad.SDL_GAMEPAD_BUTTON_START, "START_BUTTON"),

	/// The guide gamepad button.
	GUIDE(SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE, "GUIDE_BUTTON"),

	/// The left stick press gamepad button.
	LEFT_STICK(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK, "LEFT_STICK"),

	/// The right stick press gamepad button.
	RIGHT_STICK(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK, "RIGHT_STICK"),

	/// The D-pad up gamepad button.
	DPAD_UP(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP, "DPAD_UP"),

	/// The D-pad right gamepad button.
	DPAD_RIGHT(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT, "DPAD_RIGHT"),

	/// The D-pad down gamepad button.
	DPAD_DOWN(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN, "DPAD_DOWN"),

	/// The D-pad left gamepad button.
	DPAD_LEFT(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT, "DPAD_LEFT");

	/// The SDL button ID, or `-1` for [#NONE].
	public final int id;

	/// Localized display label for this button constant.
	private final String label;

	/// Constructs a [HotSwappingButton] constant with the given SDL button ID and
	/// localization key.
	///
	/// @param id the SDL button ID, or `-1` for [#NONE]
	/// @param labelKey the resource-bundle key for the localized display label
	HotSwappingButton(final int id, final String labelKey) {
		this.id = id;
		label = Main.STRINGS.getString(labelKey);
	}

	/// Returns the [HotSwappingButton] constant whose id matches the given
	/// SDL button ID, or [#NONE] if no match is found.
	///
	/// @param id the SDL button ID to look up
	/// @return the matching [HotSwappingButton] constant, or [#NONE] as default
	public static HotSwappingButton fromId(final int id) {
		return EnumSet.allOf(HotSwappingButton.class).stream().filter(hotSwappingButton -> hotSwappingButton.id == id)
				.findFirst().orElse(NONE);
	}

	@Override
	public String toString() {
		return label;
	}
}

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
import org.lwjgl.sdl.SDLGamepad;

/// Represents a controller component (axis or button) with stick-swapping
/// support.
///
/// When stick swapping is enabled in the application, the [#index] accessor
/// transparently remaps left-stick and right-stick indices so that actions
/// follow the swap.
///
/// @param main the main application instance used to check stick-swapping state
/// @param type the type of component (axis or button)
/// @param index the SDL gamepad component index
public record ControllerComponent(Main main, ControllerComponentType type, int index) {

	/// Returns the component index, swapping left and right sticks if enabled.
	///
	/// @return the component index, potentially swapped
	@Override
	public int index() {
		if (main.isSwapLeftAndRightSticks()) {
			return switch (type) {
			case AXIS -> switch (index) {
			case SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX -> SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX;
			case SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY -> SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY;
			case SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX -> SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX;
			case SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY -> SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY;
			default -> index;
			};
			case BUTTON -> switch (index) {
			case SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK -> SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK -> SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK;
			default -> index;
			};
			};
		}

		return index;
	}

	/// Enumerates the types of controller components.
	///
	/// Used by [ControllerComponent] to distinguish between axis and button
	/// mappings when resolving stick-swap indices.
	public enum ControllerComponentType {
		/// An axis component.
		AXIS,
		/// A button component.
		BUTTON
	}
}

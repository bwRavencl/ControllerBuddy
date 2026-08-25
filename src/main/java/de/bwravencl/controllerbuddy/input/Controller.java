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

import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLGUID;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDL_GUID;
import org.lwjgl.system.MemoryStack;

/// Record representing a connected gamepad controller, identified by SDL
/// instance ID, name, and GUID.
///
/// Instances are created when SDL reports a gamepad connection event. The GUID
/// is derived from the SDL gamepad GUID and uniquely identifies the controller
/// model for database lookups.
///
/// @param instanceId the SDL instance ID of the controller
/// @param name the human-readable name of the controller
/// @param guid the globally unique ID string for the controller
public record Controller(int instanceId, @Nullable String name, String guid) {

	/// Constructs a [Controller] by resolving the name and GUID from the given SDL
	/// instance ID.
	///
	/// @param instanceId the SDL instance ID of the controller
	public Controller(final int instanceId) {
		this(instanceId, SDLGamepad.SDL_GetGamepadNameForID(instanceId), prepareGuid(instanceId));
	}

	/// Converts the SDL GUID for the given instance ID to a hex string.
	///
	/// Allocates a stack buffer, calls `SDL_GUIDToString`, and strips any
	/// trailing null characters before returning the result.
	///
	/// @param instanceId the SDL instance ID whose GUID is requested
	/// @return the GUID as a trimmed hex string
	private static String prepareGuid(final int instanceId) {
		String guid;
		try (final var stack = MemoryStack.stackPush()) {
			final var sdlGuid = SDL_GUID.malloc(stack);
			SDLGamepad.SDL_GetGamepadGUIDForID(instanceId, sdlGuid);
			final var guidByteBuffer = stack.calloc(33);
			SDLGUID.SDL_GUIDToString(sdlGuid, guidByteBuffer);
			guid = StandardCharsets.UTF_8.decode(guidByteBuffer).toString();
			final var nullPos = guid.indexOf('\u0000');
			if (nullPos != -1) {
				guid = guid.substring(0, nullPos);
			}
		}

		return guid;
	}
}

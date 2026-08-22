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

import org.lwjgl.sdl.SDLGamepad;

/// Holds the most recently polled axis and button state for a single SDL
/// gamepad.
///
/// Wraps a native SDL gamepad handle and provides an [#update] method that
/// reads all axis and button values via SDL, applies optional transformations
/// such as stick-swap and circular-to-square axis mapping, and stores the
/// normalized results for use by the input pipeline on each polling cycle.
public final class GamepadState {

	/// Normalized axis values for all SDL gamepad axes.
	private final float[] axes = new float[SDLGamepad.SDL_GAMEPAD_AXIS_COUNT];

	/// Button pressed states for all SDL gamepad buttons.
	private final boolean[] buttons = new boolean[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT + 1];

	/// Whether circular axis values should be remapped to square axes.
	private final boolean mapCircularAxesToSquareAxes;

	/// Normalized axis values for the left and right stick, before any
	/// circular-to-square mapping is applied.
	private final float[] rawAxes;

	/// The SDL gamepad handle associated with this state.
	private final long sdlGamepad;

	/// Whether the left and right sticks should be swapped during input processing.
	private final boolean swapLeftAndRightSticks;

	/// Constructs a [GamepadState] for the given SDL gamepad handle.
	///
	/// @param sdlGamepad the native SDL gamepad handle
	/// @param swapLeftAndRightSticks whether to swap the left and right stick axes
	/// @param mapCircularAxesToSquareAxes whether to apply circular-to-square axis
	/// mapping
	GamepadState(final long sdlGamepad, final boolean swapLeftAndRightSticks,
			final boolean mapCircularAxesToSquareAxes) {
		this.sdlGamepad = sdlGamepad;
		this.swapLeftAndRightSticks = swapLeftAndRightSticks;
		this.mapCircularAxesToSquareAxes = mapCircularAxesToSquareAxes;

		if (mapCircularAxesToSquareAxes) {
			rawAxes = new float[SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY + 1];
		} else {
			rawAxes = axes;
		}
	}

	/// Clamps a float value to the range [-1, 1].
	///
	/// @param v the value to clamp
	/// @return the clamped value in the range [-1, 1]
	private static float clamp(final float v) {
		return Math.clamp(v, -1f, 1f);
	}

	/// Corrects near-zero numerical imprecision by snapping values smaller than
	/// a small epsilon to exactly zero.
	///
	/// @param d the value to correct
	/// @return `0` if `d` is smaller than the epsilon threshold, otherwise `d`
	private static double correctNumericalImprecision(final double d) {
		if (d < 0.000_000_1) {
			return 0d;
		}
		return d;
	}

	/// Returns the normalized axis values for all SDL gamepad axes.
	///
	/// @return the array of axis values
	float[] getAxes() {
		return axes;
	}

	/// Returns the button pressed states for all SDL gamepad buttons.
	///
	/// @return the array of button pressed states
	boolean[] getButtons() {
		return buttons;
	}

	/// Returns the raw, normalized axis values for the left and right stick, before
	/// any circular-to-square remapping is applied.
	///
	/// @return the array of raw axis values
	public float[] getRawAxes() {
		return rawAxes;
	}

	/// Remaps the circular range of a pair of axes to a square range using disc to
	/// square mapping as described in ["Analytical Methods for Squaring the Disc"
	/// by Chamberlain Fong](https://arxiv.org/abs/1509.06344), updating the axis
	/// values in place.
	///
	/// @param xAxisIndex the index of the horizontal axis in the axes array
	/// @param yAxisIndex the index of the vertical axis in the axes array
	private void mapCircularAxesToSquareAxes(final int xAxisIndex, final int yAxisIndex) {
		final var u = clamp(axes[xAxisIndex]);
		final var v = clamp(axes[yAxisIndex]);

		final var u2 = u * u;
		final var v2 = v * v;

		final var subtermX = 2d + u2 - v2;
		final var subtermY = 2d - u2 + v2;

		final var twoSqrt2 = 2d * Math.sqrt(2d);

		var termX1 = subtermX + u * twoSqrt2;
		var termX2 = subtermX - u * twoSqrt2;
		var termY1 = subtermY + v * twoSqrt2;
		var termY2 = subtermY - v * twoSqrt2;

		termX1 = correctNumericalImprecision(termX1);
		termY1 = correctNumericalImprecision(termY1);
		termX2 = correctNumericalImprecision(termX2);
		termY2 = correctNumericalImprecision(termY2);

		final var x = 0.5 * Math.sqrt(termX1) - 0.5 * Math.sqrt(termX2);
		final var y = 0.5 * Math.sqrt(termY1) - 0.5 * Math.sqrt(termY2);

		axes[xAxisIndex] = clamp((float) x);
		axes[yAxisIndex] = clamp((float) y);
	}

	/// Polls the SDL gamepad and refreshes all axis and button state arrays.
	///
	/// Reads normalized axis values for sticks and triggers, optionally swapping
	/// left and right sticks and remapping circular to square axes, then reads
	/// all button states.
	///
	/// @return `true` if the gamepad is still connected and state was updated,
	/// `false` if the gamepad has been disconnected
	boolean update() {
		if (!SDLGamepad.SDL_GamepadConnected(sdlGamepad)) {
			return false;
		}

		axes[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX : SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = Input
				.normalize(SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX),
						Short.MIN_VALUE, Short.MAX_VALUE, -1f, 1f);
		axes[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY : SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY] = Input
				.normalize(SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY),
						Short.MIN_VALUE, Short.MAX_VALUE, -1f, 1f);
		axes[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX : SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX] = Input
				.normalize(SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX),
						Short.MIN_VALUE, Short.MAX_VALUE, -1f, 1f);
		axes[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY : SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY] = Input
				.normalize(SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY),
						Short.MIN_VALUE, Short.MAX_VALUE, -1f, 1f);

		System.arraycopy(axes, 0, rawAxes, 0, rawAxes.length);
		if (mapCircularAxesToSquareAxes) {
			mapCircularAxesToSquareAxes(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY);
			mapCircularAxesToSquareAxes(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY);
		}

		axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER] = Input.normalize(
				SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER), 0, Short.MAX_VALUE,
				-1f, 1f);

		axes[SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER] = Input.normalize(
				SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER), 0,
				Short.MAX_VALUE, -1f, 1f);

		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_EAST] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_EAST);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_WEST] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_WEST);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_BACK] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_BACK);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_START] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_START);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK);

		buttons[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK
				: SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
						SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK);
		buttons[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK
				: SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
						SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK);

		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT);
		buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
				SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT);

		return true;
	}
}

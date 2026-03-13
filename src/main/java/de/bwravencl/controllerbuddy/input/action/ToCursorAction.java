/*
 * Copyright (C) 2020 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.CursorSensitivityEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MouseAxisEditorBuilder;
import java.lang.constant.Constable;
import java.text.MessageFormat;

/// Abstract base class for actions that map controller input to mouse cursor
/// movement.
///
/// Supports configurable cursor sensitivity and mouse axis (X or Y). Subpixel
/// movement is accumulated across frames via [#remainingD].
///
/// @param <V> the type of input value this action processes
public abstract class ToCursorAction<V extends Constable> extends InvertableAction<V> {

	/// Default cursor sensitivity applied when none is explicitly configured.
	private static final int DEFAULT_CURSOR_SENSITIVITY = 2000;

	/// Cursor sensitivity scaling factor applied to raw input deltas.
	@ActionProperty(title = "CURSOR_SENSITIVITY_TITLE", description = "CURSOR_SENSITIVITY_DESCRIPTION", editorBuilder = CursorSensitivityEditorBuilder.class, order = 11)
	int cursorSensitivity = DEFAULT_CURSOR_SENSITIVITY;

	/// Accumulated subpixel movement remainder carried over between frames.
	transient float remainingD;

	/// Target mouse axis for cursor movement.
	@ActionProperty(title = "MOUSE_AXIS_TITLE", description = "MOUSE_AXIS_DESCRIPTION", editorBuilder = MouseAxisEditorBuilder.class, order = 10)
	private MouseAxis axis = MouseAxis.X;

	/// Returns the mouse axis targeted by this action.
	///
	/// @return the mouse axis
	public MouseAxis getAxis() {
		return axis;
	}

	/// Returns the cursor sensitivity value.
	///
	/// @return the cursor sensitivity
	public int getCursorSensitivity() {
		return cursorSensitivity;
	}

	/// Returns a description including the target mouse axis direction.
	///
	/// @param input the current input state
	/// @return the action description
	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty()) {
			return super.getDescription(input);
		}

		return MessageFormat.format(Main.STRINGS.getString("MOUSE_AXIS_DIR"), axis.toString());
	}

	/// Moves the mouse cursor by the given delta along the configured axis.
	///
	/// Applies inversion if enabled, accumulates subpixel remainders across
	/// calls, and updates the corresponding cursor delta on the input state
	/// only when the rounded movement is non-zero.
	///
	/// @param input the current input state
	/// @param d the raw movement delta in pixels
	void moveCursor(final Input input, float d) {
		d = invert ? -d : d;
		d += remainingD;

		final var roundedD = Math.round(d);
		if (roundedD == 0) {
			remainingD = d;
			return;
		}

		if (axis == MouseAxis.X) {
			input.setCursorDeltaX(input.getCursorDeltaX() + roundedD);
		} else {
			input.setCursorDeltaY(input.getCursorDeltaY() + roundedD);
		}

		remainingD = d - roundedD;
	}

	/// Sets the mouse axis targeted by this action.
	///
	/// @param axis the mouse axis to target
	public void setAxis(final MouseAxis axis) {
		this.axis = axis;
	}

	/// Sets the cursor sensitivity value.
	///
	/// @param cursorSensitivity the sensitivity to set
	public void setCursorSensitivity(final int cursorSensitivity) {
		this.cursorSensitivity = cursorSensitivity;
	}

	/// Represents a mouse movement axis.
	///
	/// Each constant maps to a localization key used
	/// for display in the profile editor UI.
	public enum MouseAxis {

		/// Horizontal mouse axis.
		X("MOUSE_AXIS_X"),

		/// Vertical mouse axis.
		Y("MOUSE_AXIS_Y");

		/// Localized display label for this mouse axis.
		private final String label;

		/// Creates a `MouseAxis` constant with a localized label.
		///
		/// @param labelKey the resource bundle key used to look up the localized label
		MouseAxis(final String labelKey) {
			label = Main.STRINGS.getString(labelKey);
		}

		/// Returns the localized label for this mouse axis.
		///
		/// @return the localized label string
		@Override
		public String toString() {
			return label;
		}
	}
}

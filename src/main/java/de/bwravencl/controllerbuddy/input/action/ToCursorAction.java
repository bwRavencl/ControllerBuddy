/* Copyright (C) 2020  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.CursorSensitivityEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MouseAxisEditorBuilder;
import java.lang.constant.Constable;
import java.text.MessageFormat;

public abstract class ToCursorAction<V extends Constable> extends InvertableAction<V> {

	private static final int DEFAULT_CURSOR_SENSITIVITY = 2000;

	@ActionProperty(label = "CURSOR_SENSITIVITY", editorBuilder = CursorSensitivityEditorBuilder.class, order = 11)
	int cursorSensitivity = DEFAULT_CURSOR_SENSITIVITY;

	transient float remainingD;

	@ActionProperty(label = "MOUSE_AXIS", editorBuilder = MouseAxisEditorBuilder.class, order = 10)
	private MouseAxis axis = MouseAxis.X;

	public MouseAxis getAxis() {
		return axis;
	}

	public int getCursorSensitivity() {
		return cursorSensitivity;
	}

	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty()) {
			return super.getDescription(input);
		}

		return MessageFormat.format(Main.strings.getString("MOUSE_AXIS_DIR"), axis.toString());
	}

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

	public void setAxis(final MouseAxis axis) {
		this.axis = axis;
	}

	public void setCursorSensitivity(final int cursorSensitivity) {
		this.cursorSensitivity = cursorSensitivity;
	}

	public enum MouseAxis {

		X("MOUSE_AXIS_X"), Y("MOUSE_AXIS_Y");

		private final String label;

		MouseAxis(final String labelKey) {
			label = Main.strings.getString(labelKey);
		}

		@Override
		public String toString() {
			return label;
		}
	}
}

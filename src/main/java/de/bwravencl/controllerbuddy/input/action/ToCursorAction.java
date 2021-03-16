/* Copyright (C) 2020  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.MouseAxisEditorBuilder;

public abstract class ToCursorAction<V extends Number> extends InvertableAction<V> {

	public enum MouseAxis {
		X, Y
	}

	@ActionProperty(label = "MOUSE_AXIS", editorBuilder = MouseAxisEditorBuilder.class, order = 10)
	MouseAxis axis = MouseAxis.X;

	transient float remainingD;

	public MouseAxis getAxis() {
		return axis;
	}

	void moveCursor(final Input input, float d) {
		d = invert ? -d : d;

		d += remainingD;

		if (d >= -1f && d <= 1f)
			remainingD = d;
		else {
			remainingD = 0f;

			final var intD = Math.round(d);

			if (MouseAxis.X.equals(axis))
				input.setCursorDeltaX(input.getCursorDeltaX() + intD);
			else
				input.setCursorDeltaY(input.getCursorDeltaY() + intD);
		}
	}

	public void setAxis(final MouseAxis axis) {
		this.axis = axis;
	}
}

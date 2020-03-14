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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.AxisValueEditorBuilder;

@Action(label = "TO_KEY_ACTION", category = ActionCategory.AXIS, order = 40)
public final class AxisToKeyAction extends ToKeyAction<Float> implements ISuspendableAction {

	private static final float DEFAULT_MIN_AXIS_VALUE = 0.75f;
	private static final float DEFAULT_MAX_AXIS_VALUE = 1f;

	@ActionProperty(label = "MIN_AXIS_VALUE", editorBuilder = AxisValueEditorBuilder.class, order = 100)
	private float minAxisValue = DEFAULT_MIN_AXIS_VALUE;

	@ActionProperty(label = "MAX_AXIS_VALUE", editorBuilder = AxisValueEditorBuilder.class, order = 101)
	private float maxAxisValue = DEFAULT_MAX_AXIS_VALUE;

	@Override
	public void doAction(final Input input, final Float value) {
		if (!isSuspended() && value >= minAxisValue && value <= maxAxisValue) {
			if (downUp) {
				if (wasUp) {
					input.getDownUpKeyStrokes().add(keystroke);
					wasUp = false;
				}
			} else
				input.getDownKeyStrokes().add(keystroke);
		} else if (downUp)
			wasUp = true;
		else
			input.getDownKeyStrokes().remove(keystroke);
	}

	public float getMaxAxisValue() {
		return maxAxisValue;
	}

	public float getMinAxisValue() {
		return minAxisValue;
	}

	public void setMaxAxisValue(final float maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	public void setMinAxisValue(final float minAxisValue) {
		this.minAxisValue = minAxisValue;
	}
}

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

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.AxisValueEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;

@Action(label = "TO_BUTTON_ACTION", category = ActionCategory.AXIS, order = 20)
public final class AxisToButtonAction extends ToButtonAction<Float> implements IAxisToLongPressAction {

	private static final float DEFAULT_MIN_AXIS_VALUE = 0.5f;
	private static final float DEFAULT_MAX_AXIS_VALUE = 1f;

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@ActionProperty(label = "MIN_AXIS_VALUE", editorBuilder = AxisValueEditorBuilder.class, order = 100)
	private float minAxisValue = DEFAULT_MIN_AXIS_VALUE;

	@ActionProperty(label = "MAX_AXIS_VALUE", editorBuilder = AxisValueEditorBuilder.class, order = 101)
	private float maxAxisValue = DEFAULT_MAX_AXIS_VALUE;

	@Override
	public void doAction(final Input input, final int component, Float value) {
		value = handleLongPress(input, component, value);

		var hot = !input.isAxisSuspended(component) && value >= minAxisValue && value <= maxAxisValue;
		hot = handleActivationInterval(hot);

		if (isAlreadyPressed(input)) {
			return;
		}

		input.setButton(buttonId, hot);
	}

	@Override
	public float getMaxAxisValue() {
		return maxAxisValue;
	}

	@Override
	public float getMinAxisValue() {
		return minAxisValue;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}

	@Override
	public void setMaxAxisValue(final float maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	@Override
	public void setMinAxisValue(final float minAxisValue) {
		this.minAxisValue = minAxisValue;
	}
}

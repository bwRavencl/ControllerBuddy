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
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.DeadZoneEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.InitialValueEditorBuilder;

@Action(label = "TO_AXIS_ACTION", category = ActionCategory.AXIS, order = 10)
public class AxisToAxisAction extends ToAxisAction<Float> implements ISuspendableAction, IInitializationAction {

	private static final float DEFAULT_DEAD_ZONE = 0f;
	private static final float DEFAULT_INITIAL_VALUE = 0f;

	@ActionProperty(label = "DEAD_ZONE", editorBuilder = DeadZoneEditorBuilder.class, order = 100)
	float deadZone = DEFAULT_DEAD_ZONE;

	@ActionProperty(label = "INITIAL_VALUE", editorBuilder = InitialValueEditorBuilder.class, order = 202)
	float initialValue = DEFAULT_INITIAL_VALUE;

	@Override
	public void doAction(final Input input, Float value) {
		if (!isSuspended()) {
			if (Math.abs(value) <= deadZone)
				value = 0f;
			else if (value >= 0f)
				value = Input.normalize(value, deadZone, 1f, 0f, 1f);
			else
				value = Input.normalize(value, -1f, -deadZone, -1f, 0f);

			input.setAxis(virtualAxis, invert ? -value : value, false, null);
		}
	}

	public float getDeadZone() {
		return deadZone;
	}

	public float getInitialValue() {
		return initialValue;
	}

	@Override
	public void init(final Input input) {
		input.setAxis(virtualAxis, invert ? -initialValue : initialValue, false, null);
	}

	public void setDeadZone(final float deadZone) {
		this.deadZone = deadZone;
	}

	public void setInitialValue(final float initialValue) {
		this.initialValue = initialValue;
	}
}

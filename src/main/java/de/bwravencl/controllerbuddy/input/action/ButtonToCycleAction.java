/* Copyright (C) 2018  Matteo Hausner
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

import java.util.ArrayList;
import java.util.List;

import de.bwravencl.controllerbuddy.input.Input;

public class ButtonToCycleAction implements IButtonToAction, IResetableAction {

	private transient boolean wasUp = true;
	private transient int index = 0;
	private boolean longPress = DEFAULT_LONG_PRESS;
	private float activationValue = DEFAULT_ACTIVATION_VALUE;
	private List<IAction> actions = new ArrayList<>();

	@Override
	public Object clone() throws CloneNotSupportedException {
		final ButtonToCycleAction cycleAction = (ButtonToCycleAction) super.clone();

		final List<IAction> clonedActions = new ArrayList<>();
		for (final IAction a : actions)
			clonedActions.add((IAction) a.clone());
		cycleAction.setActions(clonedActions);

		return cycleAction;
	}

	@Override
	public void doAction(final Input input, float value) {
		value = handleLongPress(input, value);

		if (!IButtonToAction.floatEquals(value, activationValue)) {
			actions.get(index).doAction(input, value);
			if (!wasUp) {
				if (index == actions.size() - 1)
					index = 0;
				else
					index++;

				wasUp = true;
			}
		} else if (wasUp) {
			actions.get(index).doAction(input, activationValue);
			wasUp = false;
		}
	}

	public List<IAction> getActions() {
		return actions;
	}

	@Override
	public float getActivationValue() {
		return activationValue;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void reset() {
		index = 0;
	}

	public void setActions(final List<IAction> actions) {
		this.actions = actions;
	}

	@Override
	public void setActivationValue(final Float activationValue) {
		this.activationValue = activationValue;
	}

	@Override
	public void setLongPress(final Boolean longPress) {
		this.longPress = longPress;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_CYCLE_ACTION_STRING");
	}

}

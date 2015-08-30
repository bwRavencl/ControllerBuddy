/* Copyright (C) 2015  Matteo Hausner
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

public class ButtonToCycleAction implements IAction {

	public final float DEFAULT_ACTIVATION_VALUE = 1.0f;

	private boolean wasUp = true;
	private int index = 0;
	private float activationValue = DEFAULT_ACTIVATION_VALUE;
	private List<IAction> actions = new ArrayList<IAction>();

	public float getActivationValue() {
		return activationValue;
	}

	public void setActivationValue(Float activationValue) {
		this.activationValue = activationValue;
	}

	public List<IAction> getActions() {
		return actions;
	}

	public void setActions(List<IAction> actions) {
		this.actions = actions;
	}

	@Override
	public void doAction(Input input, float value) {
		if (value != activationValue)
			wasUp = true;
		else if (wasUp) {
			actions.get(index).doAction(input, value);

			if (index == actions.size() - 1)
				index = 0;
			else
				index++;

			wasUp = false;
		}
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_CYCLE_ACTION_STRING");
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final ButtonToCycleAction cycleAction = (ButtonToCycleAction) super.clone();

		final List<IAction> clonedActions = new ArrayList<IAction>();
		for (IAction a : actions)
			clonedActions.add((IAction) a.clone());
		cycleAction.setActions(clonedActions);

		return cycleAction;
	}

}

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

import java.util.ArrayList;
import java.util.List;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActionsEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;

@Action(label = "BUTTON_TO_CYCLE_ACTION", category = ActionCategory.BUTTON, order = 135)
public final class ButtonToCycleAction implements IButtonToAction, IResetableAction {

	private transient boolean wasUp = true;

	private transient int index = 0;

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@ActionProperty(label = "ACTIONS", editorBuilder = ActionsEditorBuilder.class, order = 10)
	private List<IAction<Byte>> actions = new ArrayList<>();

	@SuppressWarnings("unchecked")
	@Override
	public Object clone() throws CloneNotSupportedException {
		final var cycleAction = (ButtonToCycleAction) super.clone();

		final var clonedActions = new ArrayList<IAction<Byte>>();
		for (final var action : actions)
			clonedActions.add((IAction<Byte>) action.clone());
		cycleAction.setActions(clonedActions);

		return cycleAction;
	}

	@Override
	public void doAction(final Input input, Byte value) {
		value = handleLongPress(input, value);

		if (value == 0) {
			actions.get(index).doAction(input, value);
			if (!wasUp) {
				if (index == actions.size() - 1)
					index = 0;
				else
					index++;

				wasUp = true;
			}
		} else if (wasUp) {
			actions.get(index).doAction(input, Byte.MAX_VALUE);
			wasUp = false;
		}
	}

	public List<IAction<Byte>> getActions() {
		return actions;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void reset() {
		index = 0;
	}

	public void setActions(final List<IAction<Byte>> actions) {
		this.actions = actions;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}
}

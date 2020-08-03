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

import static de.bwravencl.controllerbuddy.gui.Main.strings;
import static java.util.stream.Collectors.joining;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActionsEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;

@Action(label = "BUTTON_TO_CYCLE_ACTION", category = ActionCategory.BUTTON, order = 140)
public final class ButtonToCycleAction extends DescribableAction<Byte>
		implements IButtonToAction, IResetableAction, IActivatableAction {

	private transient Activatable activatable = Activatable.YES;

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
	public void doAction(final Input input, final int component, Byte value) {
		value = handleLongPress(input, component, value);

		if (value == 0) {
			actions.get(index).doAction(input, component, value);
			if (activatable != Activatable.YES) {
				if (index == actions.size() - 1)
					index = 0;
				else
					index++;

				activatable = Activatable.YES;
			}
		} else if (activatable == Activatable.YES) {
			actions.get(index).doAction(input, component, Byte.MAX_VALUE);
			activatable = Activatable.NO;
		}
	}

	public List<IAction<Byte>> getActions() {
		return actions;
	}

	@Override
	public Activatable getActivatable() {
		return activatable;
	}

	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty())
			return super.getDescription(input);

		return MessageFormat.format(strings.getString("CYCLE"),
				actions.stream().map(action -> action.getDescription(input)).collect(joining(" \u2192 ")));
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
	public void setActivatable(final Activatable activatable) {
		this.activatable = activatable;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}
}

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

import java.text.MessageFormat;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MouseButtonEditorBuilder;

abstract class ToMouseButtonAction<V extends Number> extends DescribableAction<V>
		implements IActivatableAction<V>, ILongPressAction<V> {

	private static final int DEFAULT_MOUSE_BUTTON = 1;

	@ActionProperty(label = "ACTIVATION", editorBuilder = ActivationEditorBuilder.class, order = 11)
	private Activation activation = Activation.REPEAT;

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@ActionProperty(label = "MOUSE_BUTTON", editorBuilder = MouseButtonEditorBuilder.class, order = 10)
	private int mouseButton = DEFAULT_MOUSE_BUTTON;

	private transient boolean initiator = false;

	private transient Activatable activatable;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public Activatable getActivatable() {
		return activatable;
	}

	@Override
	public Activation getActivation() {
		return activation;
	}

	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty())
			return super.getDescription(input);

		return MessageFormat.format(Main.strings.getString("MOUSE_BUTTON_NO"), mouseButton);
	}

	public int getMouseButton() {
		return mouseButton;
	}

	void handleAction(final boolean hot, final Input input) {
		if (activatable == Activatable.ALWAYS) {
			input.getDownUpMouseButtons().add(mouseButton);
			return;
		}

		switch (activation) {
		case REPEAT:
			final var downMouseButtons = input.getDownMouseButtons();
			if (!hot) {
				if (initiator) {
					initiator = false;
					downMouseButtons.remove(mouseButton);
				}
			} else {
				initiator = true;
				downMouseButtons.add(mouseButton);
			}
			break;
		case SINGLE_IMMEDIATELY:
			if (!hot)
				activatable = Activatable.YES;
			else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				input.getDownUpMouseButtons().add(mouseButton);
			}
			break;
		case SINGLE_ON_RELEASE:
			if (hot) {
				if (activatable == Activatable.NO)
					activatable = Activatable.YES;
				else if (activatable == Activatable.DENIED_BY_OTHER_ACTION)
					activatable = Activatable.NO;
			} else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				input.getDownUpMouseButtons().add(mouseButton);
			}
			break;
		}
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void setActivatable(final Activatable activatable) {
		this.activatable = activatable;
	}

	@Override
	public void setActivation(final Activation activation) {
		this.activation = activation;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}

	public void setMouseButton(final int mouseButton) {
		this.mouseButton = mouseButton;
	}
}

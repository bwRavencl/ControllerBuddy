/* Copyright (C) 2015  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MouseButtonEditorBuilder;
import java.lang.constant.Constable;
import java.text.MessageFormat;

abstract class ToMouseButtonAction<V extends Constable> extends ActivationIntervalAction<V>
		implements IActivatableAction<V>, ILongPressAction<V> {

	private static final int DEFAULT_MOUSE_BUTTON = 1;

	private transient Activatable activatable;

	@ActionProperty(label = "ACTIVATION", editorBuilder = ActivationEditorBuilder.class, order = 11)
	private Activation activation = Activation.REPEAT;

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@ActionProperty(label = "MOUSE_BUTTON", editorBuilder = MouseButtonEditorBuilder.class, order = 10)
	private int mouseButton = DEFAULT_MOUSE_BUTTON;

	private transient boolean wasDown;

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
		if (!isDescriptionEmpty()) {
			return super.getDescription(input);
		}

		return MessageFormat.format(Main.STRINGS.getString("MOUSE_BUTTON_NO"), mouseButton);
	}

	public int getMouseButton() {
		return mouseButton;
	}

	void handleAction(boolean hot, final Input input) {
		hot = handleActivationInterval(hot);

		if (activatable == Activatable.ALWAYS) {
			input.getDownUpMouseButtons().add(mouseButton);
			return;
		}

		switch (activation) {
		case REPEAT -> {
			final var downMouseButtons = input.getDownMouseButtons();
			if (!hot) {
				if (wasDown) {
					downMouseButtons.remove(mouseButton);
					wasDown = false;
				}
			} else {
				downMouseButtons.add(mouseButton);
				wasDown = true;
			}
		}
		case SINGLE_IMMEDIATELY -> {
			if (!hot) {
				activatable = Activatable.YES;
			} else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				input.getDownUpMouseButtons().add(mouseButton);
			}
		}
		case SINGLE_ON_RELEASE -> {
			if (hot) {
				if (activatable == Activatable.NO) {
					activatable = Activatable.YES;
				} else if (activatable == Activatable.DENIED_BY_OTHER_ACTION) {
					activatable = Activatable.NO;
				}
			} else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				input.getDownUpMouseButtons().add(mouseButton);
			}
		}
		}
	}

	@Override
	public void init(final Input input) {
		super.init(input);
		IActivatableAction.super.init(input);
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

/* Copyright (C) 2014  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.gui.ButtonEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;
import java.lang.constant.Constable;
import java.text.MessageFormat;

public abstract class ToButtonAction<V extends Constable> extends ActivationIntervalAction<V>
		implements ILongPressAction<V>, IResetableAction<V> {

	@ActionProperty(label = "BUTTON_ID", editorBuilder = ButtonEditorBuilder.class, order = 10)
	int buttonId;

	private transient Activatable activatable;

	@ActionProperty(label = "ACTIVATION", editorBuilder = ActivationEditorBuilder.class, order = 11)
	private Activation activation = Activation.WHILE_PRESSED;

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	private transient boolean wasDown;

	@Override
	public Activatable getActivatable() {
		return activatable;
	}

	@Override
	public Activation getActivation() {
		return activation;
	}

	public int getButtonId() {
		return buttonId;
	}

	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty()) {
			return super.getDescription(input);
		}

		return MessageFormat.format(Main.STRINGS.getString("JOYSTICK_BUTTON_NO"), buttonId + 1);
	}

	void handleAction(boolean hot, final Input input) {
		if (activatable == Activatable.ALWAYS) {
			input.getButtons()[buttonId] = true;
			return;
		}

		switch (activation) {
		case WHILE_PRESSED -> {
			hot = handleActivationInterval(hot);
			if (!hot) {
				wasDown = false;
			} else {
				input.getButtons()[buttonId] = true;
				wasDown = true;
			}
		}
		case ON_PRESS -> {
			if (minActivationInterval != 0) {
				final var hold = handleActivationInterval(!wasDown && hot);
				if (!hot && !wasDown) {
					activatable = Activatable.YES;
				} else if (activatable == Activatable.YES) {
					activatable = Activatable.NO;
					input.getButtons()[buttonId] = true;
					wasDown = true;
				}
				if (hold) {
					if (wasDown) {
						input.getButtons()[buttonId] = true;
					}
				} else {
					wasDown = false;
				}
			} else {
				hot = handleActivationInterval(hot);
				if (!hot) {
					activatable = Activatable.YES;
				} else if (activatable == Activatable.YES) {
					activatable = Activatable.NO;
					input.getButtons()[buttonId] = true;
				}
			}
		}
		case ON_RELEASE -> {
			if (minActivationInterval != 0) {
				final var hold = handleActivationInterval(!wasDown && !hot);
				if (hot && !wasDown) {
					if (activatable == Activatable.NO) {
						activatable = Activatable.YES;
					} else if (activatable == Activatable.DENIED_BY_OTHER_ACTION) {
						activatable = Activatable.NO;
					}
				} else if (activatable == Activatable.YES) {
					activatable = Activatable.NO;
					input.getButtons()[buttonId] = true;
					wasDown = true;
				} else if (hold) {
					if (wasDown) {
						input.getButtons()[buttonId] = true;
					}
				} else {
					wasDown = false;
				}
			} else {
				hot = handleActivationInterval(hot);
				if (hot) {
					if (activatable == Activatable.NO) {
						activatable = Activatable.YES;
					} else if (activatable == Activatable.DENIED_BY_OTHER_ACTION) {
						activatable = Activatable.NO;
					}
				} else if (activatable == Activatable.YES) {
					activatable = Activatable.NO;
					input.getButtons()[buttonId] = true;
				}
			}
		}
		}
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void reset(final Input input) {
		wasDown = false;
	}

	@Override
	public void setActivatable(final Activatable activatable) {
		this.activatable = activatable;
	}

	@Override
	public void setActivation(final Activation activation) {
		this.activation = activation;
	}

	public void setButtonId(final int buttonId) {
		this.buttonId = buttonId;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}
}

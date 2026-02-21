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
import de.bwravencl.controllerbuddy.input.action.gui.DelayEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MouseButtonEditorBuilder;
import java.lang.constant.Constable;
import java.text.MessageFormat;

abstract class ToMouseButtonAction<V extends Constable> extends ActivationIntervalAction<V>
		implements IDelayableAction<V>, IResetableAction<V> {

	private static final int DEFAULT_MOUSE_BUTTON = 1;

	private transient Activatable activatable;

	@ActionProperty(title = "DELAY_TITLE", description = "DELAY_DESCRIPTION", editorBuilder = DelayEditorBuilder.class, order = 400)
	private long delay = DEFAULT_DELAY;

	@ActionProperty(title = "MOUSE_BUTTON_TITLE", description = "MOUSE_BUTTON_DESCRIPTION", editorBuilder = MouseButtonEditorBuilder.class, order = 10)
	private int mouseButton = DEFAULT_MOUSE_BUTTON;

	private transient boolean wasDown;

	@Override
	public Activatable getActivatable() {
		return activatable;
	}

	@Override
	public long getDelay() {
		return delay;
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
		if (activatable == Activatable.ALWAYS) {
			input.getDownUpMouseButtons().add(mouseButton);
			return;
		}

		switch (activation) {
		case WHILE_PRESSED -> {
			hot = handleActivationInterval(hot);
			if (!hot) {
				if (wasDown) {
					input.getDownMouseButtons().remove(mouseButton);
					wasDown = false;
				}
			} else {
				input.getDownMouseButtons().add(mouseButton);
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
					input.getDownMouseButtons().add(mouseButton);
					wasDown = true;
				}
				if (wasDown && !hold) {
					input.getDownMouseButtons().remove(mouseButton);
					wasDown = false;
				}
			} else {
				hot = handleActivationInterval(hot);
				if (!hot) {
					activatable = Activatable.YES;
				} else if (activatable == Activatable.YES) {
					activatable = Activatable.NO;
					input.getDownUpMouseButtons().add(mouseButton);
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
					input.getDownMouseButtons().add(mouseButton);
					wasDown = true;
				} else if (!hold) {
					if (wasDown) {
						input.getDownMouseButtons().remove(mouseButton);
						wasDown = false;
					}
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
					input.getDownUpMouseButtons().add(mouseButton);
				}
			}
		}
		}
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
	public void setDelay(final long delay) {
		this.delay = delay;
	}

	public void setMouseButton(final int mouseButton) {
		this.mouseButton = mouseButton;
	}
}

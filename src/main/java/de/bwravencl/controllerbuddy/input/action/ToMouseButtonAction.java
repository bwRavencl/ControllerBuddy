/*
 * Copyright (C) 2015 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.DelayEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MouseButtonEditorBuilder;
import java.lang.constant.Constable;
import java.text.MessageFormat;

/// Abstract base class for actions that produce mouse button output.
///
/// Manages mouse button press/release state based on the configured activation
/// mode and activation interval.
///
/// @param <V> the input value type
abstract class ToMouseButtonAction<V extends Constable> extends ActivationIntervalAction<V>
		implements IDelayableAction<V>, IResetableAction<V> {

	/// Default mouse button number used when none is explicitly configured.
	private static final int DEFAULT_MOUSE_BUTTON = 1;

	/// Current activatable state tracking whether the action may fire.
	private transient Activatable activatable;

	/// Delay in milliseconds before repeated activation triggers.
	@ActionProperty(icon = "⏱️", title = "DELAY_TITLE", description = "DELAY_DESCRIPTION", editorBuilder = DelayEditorBuilder.class, order = 400)
	private long delay = DEFAULT_DELAY;

	/// Mouse button number produced by this action.
	@ActionProperty(icon = "🖱️", title = "MOUSE_BUTTON_TITLE", description = "MOUSE_BUTTON_DESCRIPTION", editorBuilder = MouseButtonEditorBuilder.class, order = 10)
	private int mouseButton = DEFAULT_MOUSE_BUTTON;

	/// Whether the mouse button is currently held down.
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

	/// Returns the mouse button number produced by this action.
	///
	/// @return the mouse button number
	public int getMouseButton() {
		return mouseButton;
	}

	/// Handles a mouse button press or release by updating the target mouse
	/// button state according to the configured activation mode and interval.
	///
	/// When the activatable state is [Activatable#ALWAYS], a down-up mouse button
	/// event is enqueued unconditionally. Otherwise, the activation mode determines
	/// whether the button is held while pressed, fired on press, or fired on
	/// release, with optional minimum interval hold behavior.
	///
	/// @param hot `true` if the input is currently active
	/// @param input the current input state
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

	/// Sets the mouse button number produced by this action.
	///
	/// @param mouseButton the mouse button number to set
	public void setMouseButton(final int mouseButton) {
		this.mouseButton = mouseButton;
	}
}

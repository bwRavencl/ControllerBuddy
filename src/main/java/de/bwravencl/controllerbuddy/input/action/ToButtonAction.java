/*
 * Copyright (C) 2014 Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.gui.ButtonEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DelayEditorBuilder;
import java.lang.constant.Constable;
import java.text.MessageFormat;

/// Abstract base class for actions that map controller input to a virtual
/// joystick button.
///
/// Supports configurable activation modes, activation intervals, and delay. The
/// target button is identified by a zero-based button ID.
///
/// @param <V> the type of input value this action processes
public abstract class ToButtonAction<V extends Constable> extends ActivationIntervalAction<V>
		implements IDelayableAction<V>, IResetableAction<V> {

	/// The zero-based ID of the target virtual button.
	@ActionProperty(icon = "◉", title = "BUTTON_ID_TITLE", description = "BUTTON_ID_DESCRIPTION", editorBuilder = ButtonEditorBuilder.class, order = 10)
	int buttonId;

	/// Current activatable state tracking whether the action may fire.
	private transient Activatable activatable;

	/// Delay in milliseconds before repeated activation triggers.
	@ActionProperty(icon = "⏱️", title = "DELAY_TITLE", description = "DELAY_DESCRIPTION", editorBuilder = DelayEditorBuilder.class, order = 400)
	private long delay = DEFAULT_DELAY;

	/// Whether the virtual button is currently held down.
	private transient boolean wasDown;

	@Override
	public Activatable getActivatable() {
		return activatable;
	}

	/// Returns the zero-based ID of the target virtual button.
	///
	/// @return the button ID
	public int getButtonId() {
		return buttonId;
	}

	@Override
	public long getDelay() {
		return delay;
	}

	/// Returns a description including the one-based target button number.
	///
	/// @param input the current input state
	/// @return the action description
	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty()) {
			return super.getDescription(input);
		}

		return MessageFormat.format(Main.STRINGS.getString("JOYSTICK_BUTTON_NO"), buttonId + 1);
	}

	/// Handles a button press or release by updating the target virtual button
	/// state according to the configured activation mode and interval.
	///
	/// When the activatable state is [Activatable#ALWAYS], the button is pressed
	/// unconditionally. Otherwise, the activation mode determines whether the
	/// button is held while pressed, fired on press, or fired on release, with
	/// optional minimum interval hold behavior.
	///
	/// @param hot `true` if the input is currently active
	/// @param input the current input state
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
	public void reset(final Input input) {
		wasDown = false;
	}

	@Override
	public void setActivatable(final Activatable activatable) {
		this.activatable = activatable;
	}

	/// Sets the zero-based ID of the target virtual button.
	///
	/// @param buttonId the button ID to set
	public void setButtonId(final int buttonId) {
		this.buttonId = buttonId;
	}

	@Override
	public void setDelay(final long delay) {
		this.delay = delay;
	}
}

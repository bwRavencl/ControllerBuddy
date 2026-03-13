/*
 * Copyright (C) 2016 Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LockKeyEditorBuilder;
import java.text.MessageFormat;

/// Maps a button press to toggling a lock key (e.g., Caps Lock, Num Lock,
/// Scroll Lock).
///
/// On each button press, the configured [LockKey] is added to either the on or
/// off lock key set depending on the [#on] property. Uses edge detection so
/// that holding the button does not repeatedly toggle the key.
@Action(title = "BUTTON_TO_LOCK_KEY_ACTION_TITLE", description = "BUTTON_TO_LOCK_KEY_ACTION_DESCRIPTION", category = ActionCategory.BUTTON_AND_CYCLES, order = 116)
public final class ButtonToLockKeyAction extends DescribableAction<Boolean>
		implements IButtonToDelayableAction, IInitializationAction<Boolean> {

	/// Delay in milliseconds before this action becomes active.
	private long delay = DEFAULT_DELAY;

	/// Whether the lock key is toggled to the on state by this action.
	@ActionProperty(title = "ON_TITLE", description = "ON_DESCRIPTION", editorBuilder = BooleanEditorBuilder.class, order = 11)
	private boolean on = true;

	/// Lock key targeted by this action.
	@ActionProperty(title = "KEY_TITLE", description = "KEY_DESCRIPTION", editorBuilder = LockKeyEditorBuilder.class, overrideFieldName = "lockKey", overrideFieldType = LockKey.class, order = 10)
	private LockKey virtualKeyCode = LockKey.CAPS_LOCK_LOCK_KEY;

	/// Edge-detection flag; `true` when the button was last observed as released.
	private transient boolean wasUp = true;

	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleDelay(input, component, value);

		if (value) {
			if (wasUp) {
				wasUp = false;
				if (on) {
					input.getOnLockKeys().add(virtualKeyCode);
				} else {
					input.getOffLockKeys().add(virtualKeyCode);
				}
			}
		} else {
			wasUp = true;
		}
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

		return MessageFormat.format(Main.STRINGS.getString(on ? "LOCK_KEY_ON" : "LOCK_KEY_OFF"), getLockKey());
	}

	/// Returns the lock key targeted by this action.
	///
	/// @return the lock key
	public LockKey getLockKey() {
		return virtualKeyCode;
	}

	@Override
	public void init(final Input input) {
		resetWasUp();
	}

	/// Returns whether the lock key is initially set to the on state.
	///
	/// @return `true` if the lock key starts in the on state
	public boolean isOn() {
		return on;
	}

	/// Resets the edge-detection flag so the next button press is treated as a
	/// fresh press.
	void resetWasUp() {
		wasUp = true;
	}

	@Override
	public void setDelay(final long delay) {
		this.delay = delay;
	}

	/// Sets the lock key targeted by this action.
	///
	/// @param lockKey the lock key
	public void setLockKey(final LockKey lockKey) {
		virtualKeyCode = lockKey;
	}

	/// Sets whether the lock key should start in the on state.
	///
	/// @param on `true` to start in the on state
	public void setOn(final boolean on) {
		this.on = on;
	}
}

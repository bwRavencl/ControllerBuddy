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
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DelayEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ModeEditorBuilder;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.lwjgl.sdl.SDLGamepad;

/// Maps a gamepad button press to a mode switch.
///
/// Supports both momentary (held) and toggle activation. Manages a stack of
/// active mode actions to allow nested mode switching with proper deactivation
/// order.
@Action(title = "BUTTON_TO_MODE_ACTION_TITLE", description = "BUTTON_TO_MODE_ACTION_DESCRIPTION", category = ActionCategory.BUTTON, order = 145)
public final class ButtonToModeAction implements IButtonToDelayableAction, IResetableAction<Boolean> {

	/// Symbol used to represent momentary mode activation.
	public static final String MOMENTARY_SYMBOL = "⇧";

	/// Symbol used to represent toggle mode activation.
	public static final String TOGGLE_SYMBOL = "⇪";

	/// Global stack tracking the currently active mode actions in activation order.
	@SuppressWarnings("JdkObsolete")
	private static final LinkedList<ButtonToModeAction> BUTTON_TO_MODE_ACTION_STACK = new LinkedList<>();

	/// Delay in milliseconds before this action becomes active.
	@ActionProperty(title = "DELAY_TITLE", description = "DELAY_DESCRIPTION", editorBuilder = DelayEditorBuilder.class, order = 400)
	private long delay = DEFAULT_DELAY;

	/// UUID of the target mode to activate.
	@ActionProperty(title = "MODE_UUID_TITLE", description = "MODE_UUID_DESCRIPTION", editorBuilder = ModeEditorBuilder.class, overrideFieldName = "mode", overrideFieldType = Mode.class, order = 10)
	private UUID modeUuid;

	/// Whether this action uses toggle mode instead of momentary activation.
	@ActionProperty(title = "TOGGLE_TITLE", description = "TOGGLE_DESCRIPTION", editorBuilder = BooleanEditorBuilder.class, order = 11)
	private boolean toggle;

	/// Edge-detection flag; `true` when the button was last observed as released.
	private transient boolean up = true;

	/// Returns the global stack of currently active mode actions.
	///
	/// @return the mode action stack
	public static List<ButtonToModeAction> getButtonToModeActionStack() {
		return BUTTON_TO_MODE_ACTION_STACK;
	}

	/// Pushes this action onto the mode stack and switches the profile to the
	/// target mode.
	///
	/// Has no effect if this action is already on the stack. Notifies delayed
	/// action handlers of the mode transition and shows the on-screen keyboard
	/// if the target mode is the on-screen keyboard mode.
	///
	/// @param input the current input state
	/// @param profile the active profile
	private void activateMode(final Input input, final Profile profile) {
		if (!BUTTON_TO_MODE_ACTION_STACK.contains(this)) {
			BUTTON_TO_MODE_ACTION_STACK.push(this);
			final var activeMode = profile.getActiveMode();

			profile.getModeByUuid(modeUuid).ifPresent(newMode -> {
				IAxisToDelayableAction.onModeActivated(activeMode, newMode);
				IButtonToDelayableAction.onModeActivated(activeMode, newMode);

				profile.setActiveMode(input, newMode);
			});

			if (targetsOnScreenKeyboardMode()) {
				input.getMain().setOnScreenKeyboardVisible(true);
			}
		}
	}

	/// Returns whether the button that triggers this action is not mapped in any
	/// currently active mode.
	///
	/// Searches all buttons for the one assigned to this action, then checks
	/// every mode on the active mode stack to see if that button is also mapped
	/// in the mode. Returns `true` only when no active mode intercepts the button,
	/// indicating it is safe to activate this action's mode.
	///
	/// @param input the current input state
	/// @return `true` if no active mode uses the same button as this action
	private boolean buttonNotUsedByActiveModes(final Input input) {
		final var profile = input.getProfile();

		Integer myButton = null;
		buttonLoop: for (var button = 0; button <= SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT; button++) {
			final var buttonToModeActions = profile.getButtonToModeActionsMap().get(button);
			if (buttonToModeActions != null) {
				for (final var action : buttonToModeActions) {
					if (equals(action)) {
						myButton = button;
						break buttonLoop;
					}
				}
			}
		}

		if (myButton != null) {
			for (final var action : BUTTON_TO_MODE_ACTION_STACK) {
				final var buttonToActionMap = action.getMode(input).getButtonToActionsMap();
				if (buttonToActionMap.containsKey(myButton)) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/// Pops this action from the mode stack and restores the previous mode.
	///
	/// If other mode actions are stacked above this one they are recursively
	/// deactivated first. After popping, all activatable actions in the
	/// previously active mode are re-initialized, overlapping axes are suspended,
	/// and the on-screen keyboard is hidden if it was shown by this action.
	///
	/// @param input the current input state
	/// @param profile the active profile
	private void deactivateMode(final Input input, final Profile profile) {
		for (var topmostModeAction = BUTTON_TO_MODE_ACTION_STACK
				.peek(); topmostModeAction != this; topmostModeAction = BUTTON_TO_MODE_ACTION_STACK.peek()) {
			if (topmostModeAction != null) {
				topmostModeAction.deactivateMode(input, profile);
			}

			input.repeatModeActionWalk();
		}

		BUTTON_TO_MODE_ACTION_STACK.pop();

		final Mode previousMode;
		final var previousButtonToModeAction = BUTTON_TO_MODE_ACTION_STACK.peek();
		if (previousButtonToModeAction != null) {
			previousMode = previousButtonToModeAction.getMode(input);
		} else {
			previousMode = profile.getModes().getFirst();
		}

		final var activeMode = profile.getActiveMode();

		activeMode.getAllActions().forEach(action -> {
			if (action instanceof final IActivatableAction<?> activatableAction) {
				activatableAction.init(input);
			}
		});

		final var axes = activeMode.getAxisToActionsMap().keySet();
		final var previousModeAxisToActionsMap = previousMode.getAxisToActionsMap();
		final var main = input.getMain();
		if (previousModeAxisToActionsMap != null) {
			axes.stream().filter(previousModeAxisToActionsMap::containsKey)
					.forEach(axis -> previousModeAxisToActionsMap.get(axis).stream()
							.filter(action -> action instanceof IAxisToAction).forEach(_ -> input.suspendAxis(axis)));
		}

		IAxisToDelayableAction.onModeDeactivated(activeMode);
		IButtonToDelayableAction.onModeDeactivated(activeMode);

		profile.setActiveMode(input, previousMode);

		if (targetsOnScreenKeyboardMode()) {
			main.setOnScreenKeyboardVisible(false);
		}
	}

	/// Activates or deactivates the target mode based on button state and toggle
	/// configuration.
	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleDelay(input, component, value);

		final var profile = input.getProfile();

		if (!value) {
			if (toggle) {
				up = true;
			} else if (BUTTON_TO_MODE_ACTION_STACK.contains(this)) {
				deactivateMode(input, profile);
			}
		} else if (toggle) {
			if (up) {
				if (BUTTON_TO_MODE_ACTION_STACK.peek() == this) {
					deactivateMode(input, profile);
				} else if (Profile.DEFAULT_MODE.equals(profile.getActiveMode()) || buttonNotUsedByActiveModes(input)) {
					activateMode(input, profile);
				}

				up = false;
			}
		} else if (Profile.DEFAULT_MODE.equals(profile.getActiveMode()) || buttonNotUsedByActiveModes(input)) {
			activateMode(input, profile);
		}
	}

	@Override
	public long getDelay() {
		return delay;
	}

	/// Returns a localized description including the target mode name.
	@Override
	public String getDescription(final Input input) {
		final var mode = getMode(input);
		if (mode == null) {
			return null;
		}

		return MessageFormat.format(Main.STRINGS.getString("MODE_NAME"), mode.getDescription());
	}

	/// Returns the target [Mode], falling back to the on-screen keyboard mode or
	/// the default mode.
	///
	/// @param input the current input context
	/// @return the resolved target mode
	public Mode getMode(final Input input) {
		for (final var mode : input.getProfile().getModes()) {
			if (mode.getUuid().equals(modeUuid)) {
				return mode;
			}
		}

		if (OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.getUuid().equals(modeUuid)) {
			return OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE;
		}

		return Profile.DEFAULT_MODE;
	}

	/// Returns the UI symbol representing the activation type (momentary or
	/// toggle).
	///
	/// @return the symbol string
	public String getSymbol() {
		return toggle ? TOGGLE_SYMBOL : MOMENTARY_SYMBOL;
	}

	/// Returns whether this action uses toggle mode instead of momentary mode.
	///
	/// @return `true` if toggle mode is enabled
	public boolean isToggle() {
		return toggle;
	}

	/// Clears the mode action stack and hides the on-screen keyboard if targeted.
	@Override
	public void reset(final Input input) {
		BUTTON_TO_MODE_ACTION_STACK.clear();

		if (targetsOnScreenKeyboardMode()) {
			input.getMain().setOnScreenKeyboardVisible(false);
		}
	}

	@Override
	public void setDelay(final long delay) {
		this.delay = delay;
	}

	/// Sets the target mode by storing its UUID, or clears it if the mode is
	/// `null`.
	///
	/// @param mode the target mode, or `null` to clear
	public void setMode(final Mode mode) {
		if (mode != null) {
			modeUuid = mode.getUuid();
		} else {
			modeUuid = null;
		}
	}

	/// Sets whether this action uses toggle mode instead of momentary mode.
	///
	/// @param toggle `true` to enable toggle mode
	public void setToggle(final boolean toggle) {
		this.toggle = toggle;
	}

	/// Returns whether this action targets the on-screen keyboard mode.
	///
	/// @return `true` if targeting the on-screen keyboard mode
	public boolean targetsOnScreenKeyboardMode() {
		return OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.getUuid().equals(modeUuid);
	}
}

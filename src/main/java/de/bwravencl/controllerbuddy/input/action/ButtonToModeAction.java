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
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ModeEditorBuilder;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.lwjgl.sdl.SDLGamepad;

@Action(title = "BUTTON_TO_MODE_ACTION_TITLE", description = "BUTTON_TO_MODE_ACTION_DESCRIPTION", category = ActionCategory.BUTTON, order = 145)
public final class ButtonToModeAction implements IButtonToLongPressAction, IResetableAction<Boolean> {

	public static final String MOMENTARY_SYMBOL = "⇧";

	public static final String TOGGLE_SYMBOL = "⇪";

	@SuppressWarnings("JdkObsolete")
	private static final LinkedList<ButtonToModeAction> BUTTON_TO_MODE_ACTION_STACK = new LinkedList<>();

	@ActionProperty(title = "LONG_PRESS_TITLE", description = "LONG_PRESS_DESCRIPTION", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@ActionProperty(title = "MODE_UUID_TITLE", description = "MODE_UUID_DESCRIPTION", editorBuilder = ModeEditorBuilder.class, overrideFieldName = "mode", overrideFieldType = Mode.class, order = 10)
	private UUID modeUuid;

	@ActionProperty(title = "TOGGLE_TITLE", description = "TOGGLE_DESCRIPTION", editorBuilder = BooleanEditorBuilder.class, order = 11)
	private boolean toggle;

	private transient boolean up = true;

	public static List<ButtonToModeAction> getButtonToModeActionStack() {
		return BUTTON_TO_MODE_ACTION_STACK;
	}

	private void activateMode(final Input input, final Profile profile) {
		if (!BUTTON_TO_MODE_ACTION_STACK.contains(this)) {
			BUTTON_TO_MODE_ACTION_STACK.push(this);
			final var activeMode = profile.getActiveMode();

			profile.getModeByUuid(modeUuid).ifPresent(newMode -> {
				IAxisToLongPressAction.onModeActivated(activeMode, newMode);
				IButtonToLongPressAction.onModeActivated(activeMode, newMode);

				profile.setActiveMode(input, newMode);
			});

			if (targetsOnScreenKeyboardMode()) {
				input.getMain().setOnScreenKeyboardVisible(true);
			}
		}
	}

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

		IAxisToLongPressAction.onModeDeactivated(activeMode);
		IButtonToLongPressAction.onModeDeactivated(activeMode);

		profile.setActiveMode(input, previousMode);

		if (targetsOnScreenKeyboardMode()) {
			main.setOnScreenKeyboardVisible(false);
		}
	}

	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleLongPress(input, component, value);

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
	public String getDescription(final Input input) {
		final var mode = getMode(input);
		if (mode == null) {
			return null;
		}

		return MessageFormat.format(Main.STRINGS.getString("MODE_NAME"), mode.getDescription());
	}

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

	public String getSymbol() {
		return toggle ? TOGGLE_SYMBOL : MOMENTARY_SYMBOL;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	public boolean isToggle() {
		return toggle;
	}

	@Override
	public void reset(final Input input) {
		BUTTON_TO_MODE_ACTION_STACK.clear();

		if (targetsOnScreenKeyboardMode()) {
			input.getMain().setOnScreenKeyboardVisible(false);
		}
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}

	public void setMode(final Mode mode) {
		if (mode != null) {
			modeUuid = mode.getUuid();
		} else {
			modeUuid = null;
		}
	}

	public void setToggle(final boolean toggle) {
		this.toggle = toggle;
	}

	public boolean targetsOnScreenKeyboardMode() {
		return OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.getUuid().equals(modeUuid);
	}
}

/* Copyright (C) 2019  Matteo Hausner
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

import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_LAST;

import java.util.LinkedList;
import java.util.UUID;

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

@Action(label = "BUTTON_TO_MODE_ACTION", category = ActionCategory.BUTTON, order = 140)
public final class ButtonToModeAction implements IButtonToAction {

	private static final LinkedList<ButtonToModeAction> buttonToModeActionStack = new LinkedList<>();

	public static LinkedList<ButtonToModeAction> getButtonToModeActionStack() {
		return buttonToModeActionStack;
	}

	public static void reset() {
		buttonToModeActionStack.clear();
	}

	@ActionProperty(label = "TOGGLE", editorBuilder = BooleanEditorBuilder.class, order = 11)
	private boolean toggle = false;

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@ActionProperty(label = "MODE_UUID", editorBuilder = ModeEditorBuilder.class, overrideFieldName = "mode", overrideFieldType = Mode.class, order = 10)
	private UUID modeUuid;

	private transient boolean up = true;

	public ButtonToModeAction(final Input input) {
		final var modes = input.getProfile().getModes();
		setMode(modes.size() > 1 ? modes.get(1) : OnScreenKeyboard.onScreenKeyboardMode);
	}

	private void activateMode(final Input input, final Profile profile) {
		if (!buttonToModeActionStack.contains(this)) {
			buttonToModeActionStack.push(this);
			profile.setActiveMode(input, modeUuid);

			if (targetsOnScreenKeyboardMode())
				input.getMain().toggleOnScreenKeyboard();
		}
	}

	private boolean buttonNotUsedByActiveModes(final Input input) {
		final var profile = input.getProfile();

		Integer myButton = null;
		buttonLoop: for (var button = 0; button <= GLFW_GAMEPAD_BUTTON_LAST; button++) {
			final var buttonToModeActions = profile.getButtonToModeActionsMap().get(button);
			if (buttonToModeActions != null)
				for (final var action : buttonToModeActions)
					if (action.equals(this)) {
						myButton = button;
						break buttonLoop;
					}
		}

		if (myButton != null)
			for (final var action : buttonToModeActionStack) {
				final var buttonToActionMap = action.getMode(input).getButtonToActionsMap();
				if (buttonToActionMap.containsKey(myButton))
					return false;
			}

		return true;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	private void deactivateMode(final Input input, final Profile profile) {
		for (var topmostModeAction = buttonToModeActionStack
				.peek(); topmostModeAction != this; topmostModeAction = buttonToModeActionStack.peek()) {
			topmostModeAction.deactivateMode(input, profile);
			input.repeatModeActionWalk();
		}

		buttonToModeActionStack.pop();

		final Mode previousMode;
		final var previousButtonToModeAction = buttonToModeActionStack.peek();
		if (previousButtonToModeAction != null)
			previousMode = previousButtonToModeAction.getMode(input);
		else
			previousMode = profile.getModes().get(0);

		final var activeMode = profile.getActiveMode();

		for (final var action : activeMode.getAllActions())
			if (action instanceof ToKeyAction)
				((ToKeyAction<?>) action).resetWasUp();

		final var axes = activeMode.getAxisToActionsMap().keySet();
		final var defaultAxisToActionsMap = previousMode.getAxisToActionsMap();
		final var main = input.getMain();
		final var timer = main.getTimer();
		if (defaultAxisToActionsMap != null)
			for (final int axis : axes)
				if (defaultAxisToActionsMap.containsKey(axis))
					for (final var action : defaultAxisToActionsMap.get(axis))
						if (action instanceof ISuspendableAction)
							((ISuspendableAction) action).suspendAxis(timer, axis);

		profile.setActiveMode(input, previousMode.getUuid());

		if (targetsOnScreenKeyboardMode())
			main.toggleOnScreenKeyboard();
	}

	@Override
	public void doAction(final Input input, Byte value) {
		value = handleLongPress(input, value);
		final var profile = input.getProfile();

		if (value == 0) {
			if (toggle)
				up = true;
			else if (buttonToModeActionStack.contains(this))
				deactivateMode(input, profile);
		} else if (toggle) {
			if (up) {
				if (buttonToModeActionStack.peek() == this)
					deactivateMode(input, profile);
				else if (Profile.defaultMode.equals(profile.getActiveMode()) || buttonNotUsedByActiveModes(input))
					activateMode(input, profile);

				up = false;
			}
		} else if (Profile.defaultMode.equals(profile.getActiveMode()) || buttonNotUsedByActiveModes(input))
			activateMode(input, profile);
	}

	public Mode getMode(final Input input) {
		for (final var mode : input.getProfile().getModes())
			if (mode.getUuid().equals(modeUuid))
				return mode;

		if (OnScreenKeyboard.onScreenKeyboardMode.getUuid().equals(modeUuid))
			return OnScreenKeyboard.onScreenKeyboardMode;

		return null;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	public boolean isToggle() {
		return toggle;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}

	public void setMode(final Mode mode) {
		modeUuid = mode.getUuid();
	}

	public void setToggle(final boolean toggle) {
		this.toggle = toggle;
	}

	public boolean targetsOnScreenKeyboardMode() {
		return OnScreenKeyboard.onScreenKeyboardMode.getUuid().equals(modeUuid);
	}
}

/* Copyright (C) 2016  Matteo Hausner
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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import net.java.games.input.Component;

public class ButtonToModeAction implements IButtonToAction {

	private static final Deque<ButtonToModeAction> buttonToModeActionStack = new LinkedList<ButtonToModeAction>();

	public static Deque<ButtonToModeAction> getButtonToModeActionStack() {
		return buttonToModeActionStack;
	}

	private boolean toggle = false;
	private transient boolean up = true;
	private boolean longPress = DEFAULT_LONG_PRESS;

	private float activationValue = DEFAULT_ACTIVATION_VALUE;

	private UUID modeUuid;

	public ButtonToModeAction() {
		final List<Mode> modes = Input.getProfile().getModes();

		if (modes.size() > 1)
			modeUuid = modes.get(1).getUuid();
	}

	private void activateMode(Input input, Profile profile) {
		if (!buttonToModeActionStack.contains(this)) {
			buttonToModeActionStack.push(this);
			profile.setActiveMode(input, modeUuid);
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	private boolean componentNotUsedByActiveModes(Input input) {
		final Profile profile = Input.getProfile();

		Component component = null;
		componentLoop: for (final Component c : Input.getComponents(input.getController())) {
			final List<ButtonToModeAction> buttonToModeActions = profile.getComponentToModeActionMap().get(c.getName());
			if (buttonToModeActions != null) {
				for (final ButtonToModeAction a : buttonToModeActions) {
					if (a.equals(this)) {
						component = c;
						break componentLoop;
					}
				}
			}
		}

		if (component != null) {
			for (final ButtonToModeAction a : buttonToModeActionStack) {
				final Map<String, List<IAction>> componentToActionMap = a.getMode().getComponentToActionsMap();
				final List<IAction> actions = componentToActionMap.get(component.getName());

				if (actions != null)
					return false;
			}
		}

		return true;
	}

	private void deactivateMode(Input input, Profile profile) {
		if (buttonToModeActionStack.contains(this)) {
			while (!buttonToModeActionStack.isEmpty() && !buttonToModeActionStack.peek().equals(this))
				buttonToModeActionStack.poll().deactivateMode(input, profile);

			Mode previousMode = profile.getModes().get(0);
			if (!buttonToModeActionStack.isEmpty()) {
				buttonToModeActionStack.pop();
				if (!buttonToModeActionStack.isEmpty())
					previousMode = buttonToModeActionStack.peek().getMode();
			}

			final Map<String, List<IAction>> componentToActionsMap = profile.getActiveMode().getComponentToActionsMap();
			for (final List<IAction> actions : componentToActionsMap.values()) {
				for (final IAction a : actions) {
					if (a instanceof ToKeyAction)
						((ToKeyAction) a).resetWasUp();
				}
			}

			final Set<String> components = componentToActionsMap.keySet();
			final Map<String, List<IAction>> defaultComponentToActionsMap = previousMode.getComponentToActionsMap();
			if (defaultComponentToActionsMap != null) {
				for (final String c : components) {
					if (defaultComponentToActionsMap.containsKey(c)) {
						for (final IAction a : defaultComponentToActionsMap.get(c)) {
							if (a instanceof ISuspendableAction)
								((ISuspendableAction) a).suspend(c);
						}
					}
				}
			}

			profile.setActiveMode(input, previousMode.getUuid());
			input.getDownKeyStrokes().clear();
		}
	}

	@Override
	public void doAction(Input input, float value) {
		value = handleLongPress(value);
		final Profile profile = Input.getProfile();

		if (value != activationValue) {
			if (toggle)
				up = true;
			else
				deactivateMode(input, profile);
		} else {
			if (toggle) {
				if (up) {
					if (profile.getActiveMode().getUuid().equals(modeUuid))
						deactivateMode(input, profile);
					else if (Profile.isDefaultMode(profile.getActiveMode()) || componentNotUsedByActiveModes(input))
						activateMode(input, profile);

					up = false;
				}
			} else if (Profile.isDefaultMode(profile.getActiveMode()) || componentNotUsedByActiveModes(input))
				activateMode(input, profile);
		}
	}

	@Override
	public float getActivationValue() {
		return activationValue;
	}

	public Mode getMode() {
		for (final Mode m : Input.getProfile().getModes())
			if (modeUuid.equals(m.getUuid()))
				return m;

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
	public void setActivationValue(Float activationValue) {
		this.activationValue = activationValue;
	}

	@Override
	public void setLongPress(Boolean longPress) {
		this.longPress = longPress;
	}

	public void setMode(Mode mode) {
		modeUuid = mode.getUuid();
	}

	public void setToggle(Boolean toggle) {
		this.toggle = toggle;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_MODE_ACTION_STRING");
	}

}

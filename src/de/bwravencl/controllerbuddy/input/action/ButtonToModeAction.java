/* Copyright (C) 2015  Matteo Hausner
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;

public class ButtonToModeAction implements IButtonToAction {

	private boolean toggle = false;
	private boolean up = true;
	private boolean longPress = DEFAULT_LONG_PRESS;
	private float activationValue = DEFAULT_ACTIVATION_VALUE;

	private UUID modeUuid;

	public ButtonToModeAction() {
		final List<Mode> modes = Input.getProfile().getModes();

		if (modes.size() > 1)
			modeUuid = modes.get(1).getUuid();
	}

	private void activateMode(Profile profile) {
		profile.setActiveMode(modeUuid);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	private void deactivateMode(Input input, Profile profile) {
		final Set<String> components = profile.getActiveMode().getComponentToActionsMap().keySet();
		final Map<String, List<IAction>> defaultComponentToActionsMap = profile.getModes().get(0)
				.getComponentToActionsMap();

		for (String c : components) {
			if (defaultComponentToActionsMap.containsKey(c)) {
				for (IAction a : defaultComponentToActionsMap.get(c)) {
					if (a instanceof ISuspendableAction)
						((ISuspendableAction) a).suspend();
				}
			}
		}

		profile.setActiveMode(0);
		input.getDownKeyStrokes().clear();
	}

	@Override
	public void doAction(Input input, float value) {
		value = handleLongPress(value);
		final Profile profile = Input.getProfile();

		if (value != activationValue) {
			if (toggle)
				up = true;
			else {
				if (profile.getActiveMode().getUuid().equals(modeUuid))
					deactivateMode(input, profile);
			}
		} else {
			if (toggle) {
				if (up) {
					if (Profile.isDefaultMode(profile.getActiveMode()))
						activateMode(profile);
					else if (profile.getActiveMode().getUuid().equals(modeUuid))
						deactivateMode(input, profile);

					up = false;
				}
			} else if (Profile.isDefaultMode(profile.getActiveMode()))
				activateMode(profile);
		}
	}

	public float getActivationValue() {
		return activationValue;
	}

	public Mode getMode() {
		for (Mode m : Input.getProfile().getModes())
			if (modeUuid.equals(m.getUuid()))
				return m;

		return null;
	}

	public boolean isToggle() {
		return toggle;
	}

	public void setActivationValue(Float activationValue) {
		this.activationValue = activationValue;
	}

	public void setMode(Mode mode) {
		modeUuid = mode.getUuid();
	}

	public void setToggle(Boolean toggle) {
		this.toggle = toggle;
	}

	public boolean isLongPress() {
		return longPress;
	}

	public void setLongPress(Boolean longPress) {
		this.longPress = longPress;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_MODE_ACTION_STRING");
	}

}

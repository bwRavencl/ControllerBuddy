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
import java.util.UUID;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;

public class ButtonToModeAction implements IAction {

	public final float DEFAULT_ACTIVATION_VALUE = 1.0f;

	private boolean toggle = false;
	private boolean up = true;
	private float activationValue = DEFAULT_ACTIVATION_VALUE;

	private UUID modeUuid;

	public ButtonToModeAction() {
		final List<Mode> modes = Input.getProfile().getModes();

		if (modes.size() > 1)
			modeUuid = modes.get(1).getUuid();
	}

	public boolean isToggle() {
		return toggle;
	}

	public void setToggle(Boolean toggle) {
		this.toggle = toggle;
	}

	public float getActivationValue() {
		return activationValue;
	}

	public void setActivationValue(Float activationValue) {
		this.activationValue = activationValue;
	}

	public Mode getMode() {
		for (Mode m : Input.getProfile().getModes())
			if (modeUuid.equals(m.getUuid()))
				return m;

		return null;
	}

	public void setMode(Mode mode) {
		modeUuid = mode.getUuid();
	}

	@Override
	public void doAction(Input input, float value) {
		final Profile profile = Input.getProfile();

		if (value != activationValue) {
			if (toggle)
				up = true;
			else {
				if (profile.getActiveMode().getUuid().equals(modeUuid)) {
					profile.setActiveMode(0);
					input.getDownKeyStrokes().clear();
				}
			}
		} else {
			if (toggle) {
				if (up) {
					if (Profile.isDefaultMode(profile.getActiveMode()))
						profile.setActiveMode(modeUuid);
					else if (profile.getActiveMode().getUuid().equals(modeUuid)) {
						profile.setActiveMode(0);
						input.getDownKeyStrokes().clear();
					}
					up = false;
				}
			} else if (Profile.isDefaultMode(profile.getActiveMode()))
				profile.setActiveMode(modeUuid);
		}
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_MODE_ACTION_STRING");
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}

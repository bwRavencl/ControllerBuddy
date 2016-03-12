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

package de.bwravencl.controllerbuddy.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import net.brockmatt.util.ResourceBundleUtil;

public class Profile implements Cloneable {

	public static final String DEFAULT_MODE_UUID_STRING = "067e6162-3b6f-4ae2-a171-2470b63dff00";

	public static boolean isDefaultMode(Mode mode) {
		return (mode.getUuid().equals(UUID.fromString(DEFAULT_MODE_UUID_STRING)));
	}

	private Map<String, List<ButtonToModeAction>> componentToModeActionMap = new HashMap<String, List<ButtonToModeAction>>();
	private List<Mode> modes = new ArrayList<Mode>();
	private int activeModeIndex = 0;

	public Profile() {
		final Mode defaultMode = new Mode(DEFAULT_MODE_UUID_STRING);
		final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(Main.STRING_RESOURCE_BUNDLE_BASENAME,
				Locale.getDefault());
		defaultMode.setDescription(rb.getString("DEFAULT_MODE_DESCRIPTION"));
		modes.add(defaultMode);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final Profile profile = new Profile();

		final Map<String, List<ButtonToModeAction>> clonedComponentToModeActionMap = new HashMap<String, List<ButtonToModeAction>>();
		for (Map.Entry<String, List<ButtonToModeAction>> e : componentToModeActionMap.entrySet()) {
			final List<ButtonToModeAction> buttonToModeActions = new ArrayList<ButtonToModeAction>();
			for (ButtonToModeAction a : e.getValue()) {
				try {
					buttonToModeActions.add((ButtonToModeAction) a.clone());
				} catch (CloneNotSupportedException e1) {
					e1.printStackTrace();
				}
			}
			clonedComponentToModeActionMap.put(new String(e.getKey()), buttonToModeActions);
		}
		profile.setComponentToModeActionMap(clonedComponentToModeActionMap);

		final List<Mode> clonedModes = new ArrayList<Mode>();
		for (Mode p : modes)
			try {
				clonedModes.add((Mode) p.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		profile.setModes(clonedModes);

		return profile;
	}

	public Mode getActiveMode() {
		return modes.get(activeModeIndex);
	}

	public Map<String, List<ButtonToModeAction>> getComponentToModeActionMap() {
		return componentToModeActionMap;
	}

	public List<Mode> getModes() {
		return modes;
	}

	public void removeMode(Mode mode) {
		final List<String> actionsToRemove = new ArrayList<String>();

		for (Map.Entry<String, List<ButtonToModeAction>> e : componentToModeActionMap.entrySet()) {
			for (ButtonToModeAction a : e.getValue()) {
				if (a.getMode().equals(mode))
					actionsToRemove.add(e.getKey());
			}
		}

		for (String s : actionsToRemove)
			componentToModeActionMap.remove(s);

		modes.remove(mode);
	}

	public void setActiveMode(int index) {
		if (modes.size() > index)
			this.activeModeIndex = index;
	}

	public void setActiveMode(UUID modeUuid) {
		for (Mode m : modes) {
			if (m.getUuid().equals(modeUuid)) {
				setActiveMode(modes.indexOf(m));
				break;
			}
		}

	}

	public void setComponentToModeActionMap(Map<String, List<ButtonToModeAction>> componentToModeActionMap) {
		this.componentToModeActionMap = componentToModeActionMap;
	}

	public void setModes(List<Mode> modes) {
		this.modes = modes;
	}

}

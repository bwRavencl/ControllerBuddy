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

package de.bwravencl.RemoteStick.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import net.brockmatt.util.ResourceBundleUtil;
import de.bwravencl.RemoteStick.gui.Main;
import de.bwravencl.RemoteStick.input.action.ButtonToModeAction;

public class Profile implements Cloneable {

	public static final String DEFAULT_MODE_UUID_STRING = "067e6162-3b6f-4ae2-a171-2470b63dff00";

	private Map<String, ButtonToModeAction> componentToModeActionMap = new HashMap<String, ButtonToModeAction>();
	private List<Mode> modes = new ArrayList<Mode>();
	private int activeModeIndex = 0;

	public Profile() {
		final Mode defaultMode = new Mode(DEFAULT_MODE_UUID_STRING);
		final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(
				Main.STRING_RESOURCE_BUNDLE_BASENAME, Locale.getDefault());
		defaultMode.setDescription(rb.getString("DEFAULT_MODE_DESCRIPTION"));
		modes.add(defaultMode);
	}

	public Map<String, ButtonToModeAction> getComponentToModeActionMap() {
		return componentToModeActionMap;
	}

	public void setComponentToModeActionMap(
			Map<String, ButtonToModeAction> componentToModeActionMap) {
		this.componentToModeActionMap = componentToModeActionMap;
	}

	public List<Mode> getModes() {
		return modes;
	}

	public void setModes(List<Mode> modes) {
		this.modes = modes;
	}

	public static boolean isDefaultMode(Mode mode) {
		return (mode.getUuid()
				.equals(UUID.fromString(DEFAULT_MODE_UUID_STRING)));
	}

	public Mode getActiveMode() {
		return modes.get(activeModeIndex);
	}

	public void setActiveMode(int index) {
		if (modes.size() > index)
			this.activeModeIndex = index;
	}

	public void setActiveMode(UUID modeUuid) {
		for (Mode p : modes) {
			if (p.getUuid().equals(modeUuid)) {
				setActiveMode(modes.indexOf(p));
				return;
			}
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final Profile profile = new Profile();

		final Map<String, ButtonToModeAction> clonedComponentToModeActionMap = new HashMap<String, ButtonToModeAction>();
		for (Map.Entry<String, ButtonToModeAction> e : componentToModeActionMap
				.entrySet())
			try {
				clonedComponentToModeActionMap.put(new String(e.getKey()),
						(ButtonToModeAction) e.getValue().clone());
			} catch (CloneNotSupportedException e1) {
				e1.printStackTrace();
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

}

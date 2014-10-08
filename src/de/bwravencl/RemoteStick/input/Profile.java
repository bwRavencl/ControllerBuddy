package de.bwravencl.RemoteStick.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.bwravencl.RemoteStick.input.action.ButtonToModeAction;

public class Profile {

	public static final String DEFAULT_MODE_UUID_STRING = "067e6162-3b6f-4ae2-a171-2470b63dff00";
	public static final String DEFAULT_MODE_DESCRIPTION = "Default Mode";

	private Map<String, ButtonToModeAction> componentToModeActionMap = new HashMap<String, ButtonToModeAction>();
	private List<Mode> modes = new ArrayList<Mode>();
	private int activeModeIndex = 0;

	public Profile() {
		final Mode defaultMode = new Mode(DEFAULT_MODE_UUID_STRING);
		defaultMode.setDescription(DEFAULT_MODE_DESCRIPTION);
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

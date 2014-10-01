package de.bwravencl.RemoteStick.input;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import de.bwravencl.RemoteStick.input.action.IAction;

public class Profile {

	public static final String DEFAULT_PROFILE_UUID_STRING = "067e6162-3b6f-4ae2-a171-2470b63dff00";
	public static final String DEFAULT_PROFILE_DESCRIPTION = "Default Profile";
	
	private UUID uuid;
	private String description = new String();
	private Map<String, HashSet<IAction>> componentToActionMap = new HashMap<String, HashSet<IAction>>();

	public Profile() {
		uuid = UUID.randomUUID();
	}

	public Profile(String uuid) {
		this.uuid = UUID.fromString(uuid);
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, HashSet<IAction>> getComponentToActionMap() {
		return componentToActionMap;
	}

	public void setComponentToActionMap(
			Map<String, HashSet<IAction>> componentToActionMap) {
		this.componentToActionMap = componentToActionMap;
	}

}

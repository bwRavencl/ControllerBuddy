package de.bwravencl.RemoteStick.input;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import de.bwravencl.RemoteStick.input.action.IAction;

public class Profile {

	private String description = new String();
	private Map<String, HashSet<IAction>> componentToActionMap = new HashMap<String, HashSet<IAction>>();

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

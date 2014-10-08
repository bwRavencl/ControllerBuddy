package de.bwravencl.RemoteStick.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.bwravencl.RemoteStick.input.action.IAction;

public class Mode {

	private UUID uuid;
	private String description = new String("New Mode");
	private Map<String, List<IAction>> componentToActionMap = new HashMap<String, List<IAction>>();

	public Mode() {
		uuid = UUID.randomUUID();
	}

	public Mode(String uuid) {
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

	public Map<String, List<IAction>> getComponentToActionMap() {
		return componentToActionMap;
	}

	public void setComponentToActionMap(
			Map<String, List<IAction>> componentToActionMap) {
		this.componentToActionMap = componentToActionMap;
	}

	@Override
	public String toString() {
		return description;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final Mode mode = new Mode(uuid.toString());
		mode.setDescription(new String(description));

		final Map<String, List<IAction>> clonedComponentToActionMap = new HashMap<String, List<IAction>>();
		for (Map.Entry<String, List<IAction>> e : componentToActionMap
				.entrySet()) {
			for (IAction a : e.getValue()) {
				final String key = new String(e.getKey());

				List<IAction> actions = clonedComponentToActionMap.get(key);
				if (actions == null) {
					actions = new ArrayList<IAction>();
					clonedComponentToActionMap.put(key, actions);
				}

				actions.add((IAction) a.clone());
			}
		}
		mode.setComponentToActionMap(clonedComponentToActionMap);

		return mode;
	}

}

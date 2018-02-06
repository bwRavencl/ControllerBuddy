/* Copyright (C) 2018  Matteo Hausner
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
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.action.IAction;
import net.brockmatt.util.ResourceBundleUtil;

public class Mode implements Cloneable {

	private UUID uuid;
	private String description;
	private Map<String, List<IAction>> componentToActionsMap = new HashMap<>();

	public Mode() {
		uuid = UUID.randomUUID();
		final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(Main.STRING_RESOURCE_BUNDLE_BASENAME,
				Locale.getDefault());
		description = rb.getString("NEW_MODE_DESCRIPTION");
	}

	public Mode(final UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final Mode mode = (Mode) super.clone();
		mode.setUuid(uuid);
		mode.setDescription(description);

		final Map<String, List<IAction>> clonedComponentToActionMap = new HashMap<>();
		for (final Map.Entry<String, List<IAction>> e : componentToActionsMap.entrySet())
			for (final IAction a : e.getValue()) {
				final String key = e.getKey();

				List<IAction> actions = clonedComponentToActionMap.get(key);
				if (actions == null) {
					actions = new ArrayList<>();
					clonedComponentToActionMap.put(key, actions);
				}

				actions.add((IAction) a.clone());
			}
		mode.setComponentToActionMap(clonedComponentToActionMap);

		return mode;
	}

	@Override
	public boolean equals(final Object arg0) {
		if (this == arg0)
			return true;
		else if (arg0 instanceof Mode)
			return getUuid().equals(((Mode) arg0).getUuid());
		else
			return false;
	}

	public Map<String, List<IAction>> getComponentToActionsMap() {
		return componentToActionsMap;
	}

	public String getDescription() {
		return description;
	}

	public UUID getUuid() {
		return uuid;
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid);
	}

	public void setComponentToActionMap(final Map<String, List<IAction>> componentToActionsMap) {
		this.componentToActionsMap = componentToActionsMap;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setUuid(final UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		return description;
	}

}

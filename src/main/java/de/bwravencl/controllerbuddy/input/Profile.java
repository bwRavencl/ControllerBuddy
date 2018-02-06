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

import java.awt.Color;
import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.action.AxisToAxisAction;
import de.bwravencl.controllerbuddy.input.action.AxisToRelativeAxisAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.IModeChangeListenerAction;
import net.brockmatt.util.ResourceBundleUtil;

public class Profile implements Cloneable {

	private static final System.Logger log = System.getLogger(Profile.class.getName());

	private static final UUID DEFAULT_MODE_UUID = UUID.fromString("067e6162-3b6f-4ae2-a171-2470b63dff00");

	public static final Mode defaultMode;

	static {
		defaultMode = new Mode(DEFAULT_MODE_UUID);
		final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(Main.STRING_RESOURCE_BUNDLE_BASENAME,
				Locale.getDefault());
		defaultMode.setDescription(rb.getString("DEFAULT_MODE_DESCRIPTION"));
	}

	private Map<String, List<ButtonToModeAction>> componentToModeActionMap = new HashMap<>();
	private List<Mode> modes = new ArrayList<>();
	private transient int activeModeIndex = 0;
	private Map<VirtualAxis, Color> virtualAxisToColorMap = new HashMap<>();

	public Profile() {
		modes.add(defaultMode);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final Profile profile = (Profile) super.clone();

		final Map<String, List<ButtonToModeAction>> clonedComponentToModeActionMap = new HashMap<>();
		for (final Map.Entry<String, List<ButtonToModeAction>> e : componentToModeActionMap.entrySet()) {
			final List<ButtonToModeAction> buttonToModeActions = new ArrayList<>();
			for (final ButtonToModeAction a : e.getValue())
				try {
					buttonToModeActions.add((ButtonToModeAction) a.clone());
				} catch (final CloneNotSupportedException e1) {
					log.log(Logger.Level.ERROR, e1.getMessage(), e1);
				}
			clonedComponentToModeActionMap.put(e.getKey(), buttonToModeActions);
		}
		profile.setComponentToModeActionMap(clonedComponentToModeActionMap);

		final List<Mode> clonedModes = new ArrayList<>();
		for (final Mode p : modes)
			try {
				clonedModes.add((Mode) p.clone());
			} catch (final CloneNotSupportedException e) {
				log.log(Logger.Level.ERROR, e.getMessage(), e);
			}
		profile.setModes(clonedModes);

		final Map<VirtualAxis, Color> clonedVirtualAxisToColorMap = new HashMap<>();
		for (final Map.Entry<VirtualAxis, Color> e : virtualAxisToColorMap.entrySet()) {
			final Color color = e.getValue();
			clonedVirtualAxisToColorMap.put(e.getKey(),
					new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));
		}
		profile.setVirtualAxisToColorMap(clonedVirtualAxisToColorMap);

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

	public Map<VirtualAxis, Color> getVirtualAxisToColorMap() {
		return virtualAxisToColorMap;
	}

	public void removeMode(final Input input, final Mode mode) {
		final List<String> actionsToRemove = new ArrayList<>();

		for (final Map.Entry<String, List<ButtonToModeAction>> e : componentToModeActionMap.entrySet())
			for (final ButtonToModeAction a : e.getValue())
				if (a.getMode(input).equals(mode))
					actionsToRemove.add(e.getKey());

		for (final String s : actionsToRemove)
			componentToModeActionMap.remove(s);

		modes.remove(mode);
	}

	public void setActiveMode(final Input input, final int index) {
		if (modes.size() > index) {
			input.getDownKeyStrokes().clear();
			input.getDownMouseButtons().clear();
			final boolean[] buttons = input.getButtons();
			if (buttons != null)
				for (int i = 0; i < buttons.length; i++)
					buttons[i] = false;

			final Mode newMode = modes.get(index);

			if (input.getOutputThread() != null)
				for (final String c : newMode.getComponentToActionsMap().keySet()) {
					final Map<String, List<IAction>> currentComponentToActionsMap = getActiveMode()
							.getComponentToActionsMap();
					if (currentComponentToActionsMap.containsKey(c))
						for (final IAction a : currentComponentToActionsMap.get(c)) {
							if (a instanceof AxisToAxisAction && !(a instanceof AxisToRelativeAxisAction)) {
								final AxisToAxisAction axisToAxisAction = (AxisToAxisAction) a;
								input.setAxis(axisToAxisAction.getVirtualAxis(), 0.0f);
							}

							if (a instanceof IModeChangeListenerAction) {
								final IModeChangeListenerAction modeChangeListenerAction = (IModeChangeListenerAction) a;
								modeChangeListenerAction.onModeChanged(newMode);
							}
						}
				}

			activeModeIndex = index;
			input.getMain().setOverlayText(newMode.getDescription());
		}
	}

	public void setActiveMode(final Input input, final UUID modeUuid) {
		for (final Mode m : modes)
			if (m.getUuid().equals(modeUuid)) {
				setActiveMode(input, modes.indexOf(m));
				break;
			}

	}

	public void setComponentToModeActionMap(final Map<String, List<ButtonToModeAction>> componentToModeActionMap) {
		this.componentToModeActionMap = componentToModeActionMap;
	}

	public void setModes(final List<Mode> modes) {
		this.modes = modes;
	}

	public void setVirtualAxisToColorMap(final Map<VirtualAxis, Color> virtualAxisToColorMap) {
		this.virtualAxisToColorMap = virtualAxisToColorMap;
	}

}

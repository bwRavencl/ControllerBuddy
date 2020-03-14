/* Copyright (C) 2020  Matteo Hausner
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

import static de.bwravencl.controllerbuddy.gui.Main.strings;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.action.AxisToAxisAction;
import de.bwravencl.controllerbuddy.input.action.AxisToRelativeAxisAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IModeChangeListenerAction;

public final class Profile implements Cloneable {

	private static final Logger log = Logger.getLogger(Profile.class.getName());

	private static final UUID DEFAULT_MODE_UUID = UUID.fromString("067e6162-3b6f-4ae2-a171-2470b63dff00");

	private static final boolean DEFAULT_SHOW_OVERLAY = true;

	private static final boolean DEFAULT_SHOW_VR_OVERLAY = true;

	private static final long DEFAULT_KEY_REPEAT_INTERVAL = 30L;

	public static final Mode defaultMode;

	static {
		defaultMode = new Mode(DEFAULT_MODE_UUID);
		defaultMode.setDescription(strings.getString("DEFAULT_MODE_DESCRIPTION"));
	}

	private String version;
	private boolean showOverlay = DEFAULT_SHOW_OVERLAY;
	private boolean showVrOverlay = DEFAULT_SHOW_VR_OVERLAY;
	private long keyRepeatInterval = DEFAULT_KEY_REPEAT_INTERVAL;
	private Map<Integer, List<ButtonToModeAction>> buttonToModeActionsMap = new HashMap<>();
	private List<Mode> modes = new ArrayList<>();
	private transient int activeModeIndex = 0;
	private Map<VirtualAxis, OverlayAxis> virtualAxisToOverlayAxisMap = new HashMap<>();

	Profile() {
		modes.add(defaultMode);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final var profile = (Profile) super.clone();

		profile.setVersion(version);
		profile.setShowOverlay(showOverlay);
		profile.setShowVrOverlay(showVrOverlay);
		profile.setKeyRepeatInterval(keyRepeatInterval);

		final var clonedButtonToModeActionsMap = new HashMap<Integer, List<ButtonToModeAction>>();
		for (final var e : buttonToModeActionsMap.entrySet()) {
			final var buttonToModeActions = new ArrayList<ButtonToModeAction>();
			for (final var action : e.getValue())
				try {
					buttonToModeActions.add((ButtonToModeAction) action.clone());
				} catch (final CloneNotSupportedException e1) {
					log.log(Level.SEVERE, e1.getMessage(), e1);
				}
			clonedButtonToModeActionsMap.put(e.getKey(), buttonToModeActions);
		}
		profile.setButtonToModeActionsMap(clonedButtonToModeActionsMap);

		final var clonedModes = new ArrayList<Mode>();
		for (final var mode : modes)
			try {
				clonedModes.add((Mode) mode.clone());
			} catch (final CloneNotSupportedException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		profile.setModes(clonedModes);

		final var clonedVirtualAxisToOverlayAxisMap = new HashMap<VirtualAxis, OverlayAxis>();
		for (final var e : virtualAxisToOverlayAxisMap.entrySet())
			clonedVirtualAxisToOverlayAxisMap.put(e.getKey(), (OverlayAxis) e.getValue().clone());
		profile.virtualAxisToOverlayAxisMap = clonedVirtualAxisToOverlayAxisMap;

		return profile;
	}

	public Mode getActiveMode() {
		return modes.get(activeModeIndex);
	}

	public Map<Integer, List<ButtonToModeAction>> getButtonToModeActionsMap() {
		return buttonToModeActionsMap;
	}

	public long getKeyRepeatInterval() {
		return keyRepeatInterval;
	}

	public List<Mode> getModes() {
		return modes;
	}

	public String getVersion() {
		return version;
	}

	public Map<VirtualAxis, OverlayAxis> getVirtualAxisToOverlayAxisMap() {
		return virtualAxisToOverlayAxisMap;
	}

	public boolean isShowOverlay() {
		return showOverlay;
	}

	public boolean isShowVrOverlay() {
		return showVrOverlay;
	}

	public void removeMode(final Input input, final Mode mode) {
		buttonToModeActionsMap.entrySet().removeIf(e -> {
			for (final var action : e.getValue())
				if (action.getMode(input).equals(mode))
					return true;

			return false;
		});

		modes.remove(mode);
	}

	void setActiveMode(final Input input, final int index) {
		if (modes.size() > index) {
			input.scheduleClearOnNextPoll();

			final var newMode = modes.get(index);

			if (input.getOutputThread() != null)
				for (final var axis : newMode.getAxisToActionsMap().keySet()) {
					final var currentAxisToActionsMap = getActiveMode().getAxisToActionsMap();
					if (currentAxisToActionsMap.containsKey(axis))
						for (final var action : currentAxisToActionsMap.get(axis)) {
							if (action instanceof AxisToAxisAction && !(action instanceof AxisToRelativeAxisAction)) {
								final AxisToAxisAction axisToAxisAction = (AxisToAxisAction) action;
								final var value = axis == GLFW_GAMEPAD_AXIS_LEFT_TRIGGER
										|| axis == GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER ? -1f : 0f;
								input.setAxis(axisToAxisAction.getVirtualAxis(), value, false, null);
							}

							if (action instanceof IModeChangeListenerAction) {
								final IModeChangeListenerAction modeChangeListenerAction = (IModeChangeListenerAction) action;
								modeChangeListenerAction.onModeChanged(newMode);
							}
						}
				}

			activeModeIndex = index;
			input.getMain().setOverlayText(newMode.getDescription());
		}
	}

	public void setActiveMode(final Input input, final UUID modeUuid) {
		for (final var mode : modes)
			if (mode.getUuid().equals(modeUuid)) {
				setActiveMode(input, modes.indexOf(mode));
				break;
			}

	}

	private void setButtonToModeActionsMap(final Map<Integer, List<ButtonToModeAction>> buttonToModeActionMap) {
		buttonToModeActionsMap = buttonToModeActionMap;
	}

	public void setKeyRepeatInterval(final long minKeyRepeatInterval) {
		keyRepeatInterval = minKeyRepeatInterval;
	}

	private void setModes(final List<Mode> modes) {
		this.modes = modes;
	}

	public void setShowOverlay(final boolean showOverlay) {
		this.showOverlay = showOverlay;
	}

	public void setShowVrOverlay(final boolean showVrOverlay) {
		this.showVrOverlay = showVrOverlay;
	}

	public void setVersion(final String version) {
		this.version = version;
	}
}

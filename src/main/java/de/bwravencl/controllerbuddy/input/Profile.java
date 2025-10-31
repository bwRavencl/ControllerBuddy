/* Copyright (C) 2014  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.action.AxisToAxisAction;
import de.bwravencl.controllerbuddy.input.action.AxisToRelativeAxisAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.lwjgl.sdl.SDLGamepad;

public final class Profile implements Cloneable {

	public static final Mode DEFAULT_MODE;

	private static final long DEFAULT_KEY_REPEAT_INTERVAL = 30L;

	private static final UUID DEFAULT_MODE_UUID = UUID.fromString("067e6162-3b6f-4ae2-a171-2470b63dff00");

	private static final boolean DEFAULT_SHOW_OVERLAY = true;

	static {
		DEFAULT_MODE = new Mode(DEFAULT_MODE_UUID);
		DEFAULT_MODE.setDescription(Main.STRINGS.getString("DEFAULT_MODE_DESCRIPTION"));
	}

	private transient int activeModeIndex = 0;

	private Map<Integer, List<ButtonToModeAction>> buttonToModeActionsMap = new HashMap<>();

	private long keyRepeatInterval = DEFAULT_KEY_REPEAT_INTERVAL;

	private List<Mode> modes = new ArrayList<>();

	private boolean showOverlay = DEFAULT_SHOW_OVERLAY;

	private String version;

	private Map<VirtualAxis, OverlayAxis> virtualAxisToOverlayAxisMap = new EnumMap<>(VirtualAxis.class);

	Profile() {
		modes.add(DEFAULT_MODE);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final var profile = (Profile) super.clone();

		final var clonedButtonToModeActionsMap = new HashMap<Integer, List<ButtonToModeAction>>();
		for (final var entry : buttonToModeActionsMap.entrySet()) {
			final var buttonToModeActions = new ArrayList<ButtonToModeAction>();
			for (final var action : entry.getValue()) {
				buttonToModeActions.add((ButtonToModeAction) action.clone());
			}
			clonedButtonToModeActionsMap.put(entry.getKey(), buttonToModeActions);
		}
		profile.setButtonToModeActionsMap(clonedButtonToModeActionsMap);

		final var clonedModes = new ArrayList<Mode>();
		for (final var mode : modes) {
			clonedModes.add((Mode) mode.clone());
		}
		profile.setModes(clonedModes);

		final var clonedVirtualAxisToOverlayAxisMap = new EnumMap<VirtualAxis, OverlayAxis>(VirtualAxis.class);
		for (final var entry : virtualAxisToOverlayAxisMap.entrySet()) {
			clonedVirtualAxisToOverlayAxisMap.put(entry.getKey(), (OverlayAxis) entry.getValue().clone());
		}
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

	public Optional<Mode> getModeByUuid(final UUID modeUuid) {
		return modes.stream().filter(mode -> mode.getUuid().equals(modeUuid)).findFirst();
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

	public void removeMode(final Input input, final Mode mode) {
		buttonToModeActionsMap.entrySet().removeIf(entry -> {
			for (final var action : entry.getValue()) {
				if (action.getMode(input).equals(mode)) {
					return true;
				}
			}

			return false;
		});

		modes.remove(mode);
	}

	void setActiveMode(final Input input, final int index) {
		if (modes.size() > index) {
			input.scheduleClearOnNextPoll();

			final var newMode = modes.get(index);

			if (input.getRunMode() != null) {
				newMode.getAxisToActionsMap().keySet().forEach(axis -> {
					final var currentAxisToActionsMap = getActiveMode().getAxisToActionsMap();
					if (currentAxisToActionsMap.containsKey(axis)) {
						currentAxisToActionsMap.get(axis).forEach(action -> {
							if (action instanceof final AxisToAxisAction axisToAxisAction
									&& !(action instanceof AxisToRelativeAxisAction)) {
								final var value = axis == SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER
										|| axis == SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER ? -1f : 0f;
								input.setAxis(axisToAxisAction.getVirtualAxis(), value, false, null);
							}
						});
					}
				});
			}

			activeModeIndex = index;
			input.getMain().setOverlayMode(newMode);
		}
	}

	public void setActiveMode(final Input input, final Mode mode) {
		setActiveMode(input, modes.indexOf(mode));
	}

	public void setButtonToModeActionsMap(final Map<Integer, List<ButtonToModeAction>> buttonToModeActionMap) {
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

	public void setVersion(final String version) {
		this.version = version;
	}
}

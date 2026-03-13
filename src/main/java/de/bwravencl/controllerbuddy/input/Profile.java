/*
 * Copyright (C) 2014 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
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

/// Represents an input mapping profile that defines modes, button-to-mode
/// mappings, overlay axis configurations, and key repeat settings.
///
/// A profile contains one or more [Mode] instances. The first mode is always
/// the [default mode][#DEFAULT_MODE]. Profiles are cloneable for use in the
/// profile editor.
public final class Profile implements Cloneable {

	/// The default mode that is always present as the first mode in a profile.
	public static final Mode DEFAULT_MODE;

	/// Default key repeat interval in milliseconds.
	private static final long DEFAULT_KEY_REPEAT_INTERVAL = 30L;

	/// UUID used to identify the default mode across serialization.
	private static final UUID DEFAULT_MODE_UUID = UUID.fromString("067e6162-3b6f-4ae2-a171-2470b63dff00");

	/// Default value for whether the overlay is shown.
	private static final boolean DEFAULT_SHOW_OVERLAY = true;

	static {
		DEFAULT_MODE = new Mode(DEFAULT_MODE_UUID);
		DEFAULT_MODE.setDescription(Main.STRINGS.getString("DEFAULT_MODE_DESCRIPTION"));
	}

	/// Index of the currently active mode within [#modes].
	private transient int activeModeIndex = 0;

	/// Maps button indices to their associated button-to-mode action lists.
	private Map<Integer, List<ButtonToModeAction>> buttonToModeActionsMap = new HashMap<>();

	/// Key repeat interval in milliseconds.
	private long keyRepeatInterval = DEFAULT_KEY_REPEAT_INTERVAL;

	/// Ordered list of modes contained in this profile.
	private List<Mode> modes = new ArrayList<>();

	/// Whether the overlay is shown during output.
	private boolean showOverlay = DEFAULT_SHOW_OVERLAY;

	/// Version string of the serialized profile.
	private String version;

	/// Maps virtual axes to their overlay axis display configurations.
	private Map<VirtualAxis, OverlayAxis> virtualAxisToOverlayAxisMap = new EnumMap<>(VirtualAxis.class);

	/// Creates a new profile pre-populated with the default mode.
	Profile() {
		modes.add(DEFAULT_MODE);
	}

	/// Creates a deep copy of this profile, including all modes, button-to-mode
	/// actions, and overlay axis mappings.
	///
	/// @return a new [Profile] with cloned contents
	/// @throws CloneNotSupportedException if cloning of a contained object fails
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

	/// Returns the currently active mode.
	///
	/// @return the active mode
	public Mode getActiveMode() {
		return modes.get(activeModeIndex);
	}

	/// Returns the mapping from button indices to button-to-mode actions.
	///
	/// @return the button-to-mode-actions map
	public Map<Integer, List<ButtonToModeAction>> getButtonToModeActionsMap() {
		return buttonToModeActionsMap;
	}

	/// Returns the key repeat interval in milliseconds.
	///
	/// @return the key repeat interval
	public long getKeyRepeatInterval() {
		return keyRepeatInterval;
	}

	/// Returns the mode with the specified UUID, if present.
	///
	/// @param modeUuid the UUID of the mode to find
	/// @return an optional containing the matching mode, or empty if not found
	public Optional<Mode> getModeByUuid(final UUID modeUuid) {
		return modes.stream().filter(mode -> mode.getUuid().equals(modeUuid)).findFirst();
	}

	/// Returns the list of all modes in this profile.
	///
	/// @return the list of modes
	public List<Mode> getModes() {
		return modes;
	}

	/// Returns the version string of this profile.
	///
	/// @return the version string
	public String getVersion() {
		return version;
	}

	/// Returns the mapping from virtual axes to their overlay axis configurations.
	///
	/// @return the virtual-axis-to-overlay-axis map
	public Map<VirtualAxis, OverlayAxis> getVirtualAxisToOverlayAxisMap() {
		return virtualAxisToOverlayAxisMap;
	}

	/// Returns whether the overlay should be displayed.
	///
	/// @return `true` if the overlay should be shown
	public boolean isShowOverlay() {
		return showOverlay;
	}

	/// Removes the specified mode and its associated button-to-mode actions.
	///
	/// @param input the input instance
	/// @param mode the mode to remove
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

	/// Sets the active mode to the mode at the specified index, resetting any
	/// axis-to-axis action values from the outgoing mode and updating the overlay.
	///
	/// @param input the input instance used for scheduling and overlay updates
	/// @param index the index of the mode to activate
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
								input.setAxis(axisToAxisAction.getVirtualAxis(), value, false, null, null, null);
							}
						});
					}
				});
			}

			activeModeIndex = index;
			input.getMain().setOverlayMode(newMode);
		}
	}

	/// Sets the active mode to the specified mode instance.
	///
	/// @param input the input instance
	/// @param mode the mode to activate
	public void setActiveMode(final Input input, final Mode mode) {
		setActiveMode(input, modes.indexOf(mode));
	}

	/// Sets the mapping from button indices to button-to-mode actions.
	///
	/// @param buttonToModeActionMap the new button-to-mode actions mapping
	public void setButtonToModeActionsMap(final Map<Integer, List<ButtonToModeAction>> buttonToModeActionMap) {
		buttonToModeActionsMap = buttonToModeActionMap;
	}

	/// Sets the key repeat interval in milliseconds.
	///
	/// @param minKeyRepeatInterval the key repeat interval in milliseconds
	public void setKeyRepeatInterval(final long minKeyRepeatInterval) {
		keyRepeatInterval = minKeyRepeatInterval;
	}

	/// Sets the list of modes for this profile, replacing the existing list.
	///
	/// @param modes the new list of modes
	private void setModes(final List<Mode> modes) {
		this.modes = modes;
	}

	/// Sets whether the overlay should be displayed.
	///
	/// @param showOverlay `true` to show the overlay
	public void setShowOverlay(final boolean showOverlay) {
		this.showOverlay = showOverlay;
	}

	/// Sets the version string of this profile.
	///
	/// @param version the version string
	public void setVersion(final String version) {
		this.version = version;
	}
}

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
import de.bwravencl.controllerbuddy.input.ControllerComponent.ControllerComponentType;
import de.bwravencl.controllerbuddy.input.action.IAction;
import java.lang.constant.Constable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/// Represents an input mode that maps gamepad axes and buttons to actions.
///
/// Each mode has a unique UUID, a description, and contains mappings from
/// axis/button indices to action lists. A [Profile] holds one or more modes
/// that can be switched at runtime.
public final class Mode implements Cloneable {

	/// Maps axis indices to their associated action lists.
	private Map<Integer, List<IAction<Float>>> axisToActionsMap = new HashMap<>();

	/// Maps button indices to their associated action lists.
	private Map<Integer, List<IAction<Boolean>>> buttonToActionsMap = new HashMap<>();

	/// Human-readable description of this mode.
	private @Nullable String description;

	/// Unique identifier of this mode.
	/// This field is not final to allow modification by Gson.
	@SuppressWarnings({ "CanBeFinal", "FieldMayBeFinal" })
	private UUID uuid;

	/// Constructs a [Mode] with a random UUID and the default description.
	public Mode() {
		uuid = UUID.randomUUID();
		description = Main.strings.getString("NEW_MODE_DESCRIPTION");
	}

	/// Constructs a [Mode] with the specified UUID and description.
	///
	/// @param uuid the unique identifier for this mode
	/// @param description the human-readable description of this mode
	public Mode(final UUID uuid, final @Nullable String description) {
		this.uuid = uuid;
		this.description = description;
	}

	/// Creates a deep copy of an action map by cloning every action in each list.
	///
	/// @param <V> the input value type of the actions
	/// @param actionMap the action map to clone
	/// @return a new map with the same keys and deeply cloned action lists
	/// @throws CloneNotSupportedException if any action does not support cloning
	@SuppressWarnings("unchecked")
	private static <V extends Constable> Map<Integer, List<IAction<V>>> cloneActionMap(
			final Map<Integer, List<IAction<V>>> actionMap) throws CloneNotSupportedException {
		final var clonedActionMap = new HashMap<Integer, List<IAction<V>>>();
		for (final var entry : actionMap.entrySet()) {
			for (final var action : entry.getValue()) {
				final var key = entry.getKey();

				final var clonedActions = clonedActionMap.computeIfAbsent(key, _ -> new ArrayList<>());
				clonedActions.add((IAction<V>) action.clone());
			}
		}

		return clonedActionMap;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final var mode = (Mode) super.clone();

		mode.axisToActionsMap = cloneActionMap(axisToActionsMap);
		mode.buttonToActionsMap = cloneActionMap(buttonToActionsMap);

		return mode;
	}

	@Override
	public boolean equals(final @Nullable Object obj) {
		return obj instanceof final Mode mode && Objects.equals(uuid, mode.uuid);
	}

	/// Returns all actions from both axis and button mappings.
	///
	/// @return the set of all actions across axis and button mappings
	public Set<IAction<?>> getAllActions() {
		return Stream.concat(axisToActionsMap.values().stream(), buttonToActionsMap.values().stream())
				.flatMap(List::stream).collect(Collectors.toUnmodifiableSet());
	}

	/// Returns the mapping from axis indices to their associated actions.
	///
	/// @return the axis-to-actions map
	public Map<Integer, List<IAction<Float>>> getAxisToActionsMap() {
		return axisToActionsMap;
	}

	/// Returns the mapping from button indices to their associated actions.
	///
	/// @return the button-to-actions map
	public Map<Integer, List<IAction<Boolean>>> getButtonToActionsMap() {
		return buttonToActionsMap;
	}

	/// Returns the action map for the specified component type (axis or button).
	///
	/// @param type the component type to retrieve actions for
	/// @return the action map for the specified component type
	public Map<Integer, ?> getComponentToActionsMap(final ControllerComponentType type) {
		if (type == ControllerComponentType.AXIS) {
			return axisToActionsMap;
		}
		return buttonToActionsMap;
	}

	/// Returns the human-readable description of this mode.
	///
	/// @return the description of this mode
	public @Nullable String getDescription() {
		return description;
	}

	/// Returns the unique identifier of this mode.
	///
	/// @return the UUID of this mode
	public UUID getUuid() {
		return uuid;
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid);
	}

	/// Sets the mapping from button indices to their associated actions.
	///
	/// @param buttonToActionsMap the new button-to-actions map
	public void setButtonToActionsMap(final Map<Integer, List<IAction<Boolean>>> buttonToActionsMap) {
		this.buttonToActionsMap = buttonToActionsMap;
	}

	/// Sets the human-readable description of this mode.
	///
	/// @param description the new description for this mode
	public void setDescription(final @Nullable String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return description != null ? description : uuid.toString();
	}
}

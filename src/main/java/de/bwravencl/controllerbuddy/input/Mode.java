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
import de.bwravencl.controllerbuddy.input.Mode.Component.ComponentType;
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
import org.lwjgl.sdl.SDLGamepad;

public final class Mode implements Cloneable {

	private final UUID uuid;

	private Map<Integer, List<IAction<Float>>> axisToActionsMap = new HashMap<>();

	private Map<Integer, List<IAction<Boolean>>> buttonToActionsMap = new HashMap<>();

	private String description;

	public Mode() {
		uuid = UUID.randomUUID();
		description = Main.strings.getString("NEW_MODE_DESCRIPTION");
	}

	public Mode(final UUID uuid) {
		this.uuid = uuid;
	}

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
	public boolean equals(final Object obj) {
		return obj instanceof final Mode mode && Objects.equals(uuid, mode.uuid);
	}

	public Set<IAction<?>> getAllActions() {
		return Stream.concat(axisToActionsMap.values().stream(), buttonToActionsMap.values().stream())
				.flatMap(List::stream).collect(Collectors.toUnmodifiableSet());
	}

	public Map<Integer, List<IAction<Float>>> getAxisToActionsMap() {
		return axisToActionsMap;
	}

	public Map<Integer, List<IAction<Boolean>>> getButtonToActionsMap() {
		return buttonToActionsMap;
	}

	public Map<Integer, ?> getComponentToActionsMap(final ComponentType type) {
		if (type == ComponentType.AXIS) {
			return axisToActionsMap;
		}
		return buttonToActionsMap;
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

	public void setButtonToActionsMap(final Map<Integer, List<IAction<Boolean>>> buttonToActionsMap) {
		this.buttonToActionsMap = buttonToActionsMap;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return description;
	}

	public static final class Component {

		private final int index;

		private final Main main;

		private final ComponentType type;

		public Component(final Main main, final ComponentType type, final int index) {
			this.main = main;
			this.type = type;
			this.index = index;
		}

		public int getIndex() {
			if (main.isSwapLeftAndRightSticks()) {
				return switch (type) {
				case AXIS -> switch (index) {
				case SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX -> SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX;
				case SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY -> SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY;
				case SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX -> SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX;
				case SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY -> SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY;
				default -> index;
				};
				case BUTTON -> switch (index) {
				case SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK -> SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK;
				case SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK -> SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK;
				default -> index;
				};
				};
			}

			return index;
		}

		public ComponentType getType() {
			return type;
		}

		public enum ComponentType {
			AXIS, BUTTON
		}
	}
}

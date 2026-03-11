/* Copyright (C) 2026  Matteo Hausner
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lwjgl.sdl.SDLGamepad;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModeTest {

	@Mock
	IAction<Float> mockAxisAction;

	@Mock
	IAction<Boolean> mockButtonAction;

	@Mock
	Main mockMain;

	private static Mode createMode() {
		return new Mode(UUID.randomUUID());
	}

	@Nested
	@DisplayName("clone()")
	class CloneTests {

		private Mode mode;

		@Test
		@DisplayName("deep clones the axisToActionsMap so that mutation of the clone does not affect the original")
		void deepClonesAxisToActionsMap() throws CloneNotSupportedException {
			final var actions = new ArrayList<IAction<Float>>();
			actions.add(mockAxisAction);
			mode.getAxisToActionsMap().put(0, actions);
			Mockito.when(mockAxisAction.clone()).thenReturn(mockAxisAction);

			final var clone = (Mode) mode.clone();

			Assertions.assertNotSame(mode.getAxisToActionsMap(), clone.getAxisToActionsMap());
			Mockito.verify(mockAxisAction).clone();
		}

		@Test
		@DisplayName("deep clones the buttonToActionsMap so that mutation of the clone does not affect the original")
		void deepClonesButtonToActionsMap() throws CloneNotSupportedException {
			final var actions = new ArrayList<IAction<Boolean>>();
			actions.add(mockButtonAction);
			mode.getButtonToActionsMap().put(0, actions);
			Mockito.when(mockButtonAction.clone()).thenReturn(mockButtonAction);

			final var clone = (Mode) mode.clone();

			Assertions.assertNotSame(mode.getButtonToActionsMap(), clone.getButtonToActionsMap());
			Mockito.verify(mockButtonAction).clone();
		}

		@Test
		@DisplayName("returns a Mode instance that is not the same object as the original")
		void returnsDistinctModeInstance() throws CloneNotSupportedException {
			Assertions.assertNotSame(mode, mode.clone());
		}

		@BeforeEach
		void setUp() {
			mode = createMode();
		}
	}

	@Nested
	@DisplayName("Component.index() - swap disabled")
	class ComponentIndexNoSwapTests {

		@Test
		@DisplayName("returns the original index unchanged for both AXIS and BUTTON types")
		void returnsOriginalIndexWhenSwapDisabled() {
			Mockito.when(mockMain.isSwapLeftAndRightSticks()).thenReturn(false);

			Assertions.assertEquals(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new Mode.Component(mockMain, ComponentType.AXIS, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX).index());
			Assertions.assertEquals(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK,
					new Mode.Component(mockMain, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK)
							.index());
		}
	}

	@Nested
	@DisplayName("Component.index() - swap enabled")
	class ComponentIndexSwapTests {

		@Test
		@DisplayName("returns a non-stick axis index unchanged")
		void returnsNonStickAxisIndexUnchanged() {
			Assertions.assertEquals(SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER,
					new Mode.Component(mockMain, ComponentType.AXIS, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER).index());
		}

		@Test
		@DisplayName("returns a non-stick button index unchanged")
		void returnsNonStickButtonIndexUnchanged() {
			Assertions.assertEquals(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new Mode.Component(mockMain, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH).index());
		}

		@BeforeEach
		void setUp() {
			Mockito.when(mockMain.isSwapLeftAndRightSticks()).thenReturn(true);
		}

		@Test
		@DisplayName("maps SDL_GAMEPAD_BUTTON_LEFT_STICK to SDL_GAMEPAD_BUTTON_RIGHT_STICK")
		void swapsLeftStickButtonToRightStick() {
			Assertions.assertEquals(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK,
					new Mode.Component(mockMain, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK)
							.index());
		}

		@Test
		@DisplayName("maps SDL_GAMEPAD_AXIS_LEFTX to SDL_GAMEPAD_AXIS_RIGHTX")
		void swapsLeftXToRightX() {
			Assertions.assertEquals(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX,
					new Mode.Component(mockMain, ComponentType.AXIS, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX).index());
		}

		@Test
		@DisplayName("maps SDL_GAMEPAD_AXIS_LEFTY to SDL_GAMEPAD_AXIS_RIGHTY")
		void swapsLeftYToRightY() {
			Assertions.assertEquals(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY,
					new Mode.Component(mockMain, ComponentType.AXIS, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY).index());
		}

		@Test
		@DisplayName("maps SDL_GAMEPAD_BUTTON_RIGHT_STICK to SDL_GAMEPAD_BUTTON_LEFT_STICK")
		void swapsRightStickButtonToLeftStick() {
			Assertions.assertEquals(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK,
					new Mode.Component(mockMain, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK)
							.index());
		}

		@Test
		@DisplayName("maps SDL_GAMEPAD_AXIS_RIGHTX to SDL_GAMEPAD_AXIS_LEFTX")
		void swapsRightXToLeftX() {
			Assertions.assertEquals(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new Mode.Component(mockMain, ComponentType.AXIS, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX).index());
		}

		@Test
		@DisplayName("maps SDL_GAMEPAD_AXIS_RIGHTY to SDL_GAMEPAD_AXIS_LEFTY")
		void swapsRightYToLeftY() {
			Assertions.assertEquals(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY,
					new Mode.Component(mockMain, ComponentType.AXIS, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY).index());
		}
	}

	@Nested
	@DisplayName("Mode()")
	class DefaultConstructorTests {

		@Test
		@DisplayName("sets description to the NEW_MODE_DESCRIPTION resource string")
		void setsDescriptionFromResourceBundle() {
			final var mode = new Mode();
			Assertions.assertEquals(Main.STRINGS.getString("NEW_MODE_DESCRIPTION"), mode.getDescription());
		}

		@Test
		@DisplayName("sets a non-null UUID")
		void setsNonNullUuid() {
			Assertions.assertNotNull(new Mode().getUuid());
		}
	}

	@Nested
	@DisplayName("equals() / hashCode()")
	class EqualsHashCodeTests {

		@Test
		@DisplayName("two modes with the same UUID are equal and have the same hash code")
		void equalModesHaveSameUuidAndHashCode() {
			final var uuid = UUID.randomUUID();
			final var a = new Mode(uuid);
			final var b = new Mode(uuid);

			Assertions.assertEquals(a, b);
			Assertions.assertEquals(a.hashCode(), b.hashCode());
		}

		@Test
		@DisplayName("a Mode is not equal to a non-Mode object")
		void modeIsNotEqualToNonModeObject() {
			// noinspection AssertBetweenInconvertibleTypes
			Assertions.assertNotEquals(new Mode(UUID.randomUUID()), "not a mode");
		}

		@Test
		@DisplayName("two modes with different UUIDs are not equal")
		void modesWithDifferentUuidsAreNotEqual() {
			Assertions.assertNotEquals(new Mode(UUID.randomUUID()), new Mode(UUID.randomUUID()));
		}
	}

	@Nested
	@DisplayName("getAllActions()")
	class GetAllActionsTests {

		@Test
		@DisplayName("returns actions from both the axis and button maps in a single set")
		void returnsActionsFromBothMaps() {
			final var mode = createMode();
			mode.getAxisToActionsMap().put(0, List.of(mockAxisAction));
			mode.getButtonToActionsMap().put(0, List.of(mockButtonAction));

			final var actions = mode.getAllActions();

			Assertions.assertTrue(actions.contains(mockAxisAction));
			Assertions.assertTrue(actions.contains(mockButtonAction));
		}

		@Test
		@DisplayName("returns an empty set when both maps contain no actions")
		void returnsEmptySetWhenNoActionsRegistered() {
			Assertions.assertTrue(createMode().getAllActions().isEmpty());
		}
	}

	@Nested
	@DisplayName("getComponentToActionsMap()")
	class GetComponentToActionsMapTests {

		@Test
		@DisplayName("returns the axisToActionsMap when type is AXIS")
		void returnsAxisMapForAxisType() {
			final var mode = createMode();
			Assertions.assertSame(mode.getAxisToActionsMap(), mode.getComponentToActionsMap(ComponentType.AXIS));
		}

		@Test
		@DisplayName("returns the buttonToActionsMap when type is BUTTON")
		void returnsButtonMapForButtonType() {
			final var mode = createMode();
			Assertions.assertSame(mode.getButtonToActionsMap(), mode.getComponentToActionsMap(ComponentType.BUTTON));
		}
	}

	@Nested
	@DisplayName("toString()")
	class ToStringTests {

		@Test
		@DisplayName("returns the description field")
		void returnsDescriptionField() {
			final var mode = createMode();
			mode.setDescription("My Mode");
			Assertions.assertEquals("My Mode", mode.toString());
		}
	}
}

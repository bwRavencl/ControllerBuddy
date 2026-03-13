/*
 * Copyright (C) 2026 Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.runmode.RunMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileTest {

	@Mock
	ButtonToModeAction mockButtonToModeAction;

	@Mock
	Input mockInput;

	@Mock
	Main mockMain;

	private Profile createProfile() {
		return new Profile();
	}

	@Nested
	@DisplayName("clone()")
	class CloneTests {

		@Test
		@DisplayName("deeply clones the buttonToModeActionsMap so mutation of the clone does not affect the original")
		void deeplyClonesButtonToModeActionsMap() throws CloneNotSupportedException {
			final var profile = createProfile();
			final var actions = new ArrayList<ButtonToModeAction>();
			Mockito.when(mockButtonToModeAction.clone()).thenReturn(mockButtonToModeAction);
			actions.add(mockButtonToModeAction);
			profile.setButtonToModeActionsMap(Map.of(1, actions));

			final var clone = (Profile) profile.clone();

			Assertions.assertNotSame(profile.getButtonToModeActionsMap(), clone.getButtonToModeActionsMap());
			Assertions.assertNotSame(profile.getButtonToModeActionsMap().get(1),
					clone.getButtonToModeActionsMap().get(1));
		}

		@Test
		@DisplayName("deeply clones the modes list so mutation of the clone does not affect the original")
		void deeplyClonesModesList() throws CloneNotSupportedException {
			final var profile = createProfile();
			final var extraMode = new Mode(UUID.randomUUID());
			profile.getModes().add(extraMode);

			final var clone = (Profile) profile.clone();
			clone.getModes().clear();

			Assertions.assertEquals(2, profile.getModes().size());
		}

		@Test
		@DisplayName("deeply clones the virtualAxisToOverlayAxisMap so mutation of the clone does not affect the original")
		void deeplyClonesVirtualAxisToOverlayAxisMap() throws CloneNotSupportedException {
			final var profile = createProfile();
			final var overlayAxis = new OverlayAxis(VirtualAxis.X);
			profile.getVirtualAxisToOverlayAxisMap().put(VirtualAxis.X, overlayAxis);

			final var clone = (Profile) profile.clone();

			Assertions.assertNotSame(profile.getVirtualAxisToOverlayAxisMap(), clone.getVirtualAxisToOverlayAxisMap());
			Assertions.assertNotSame(profile.getVirtualAxisToOverlayAxisMap().get(VirtualAxis.X),
					clone.getVirtualAxisToOverlayAxisMap().get(VirtualAxis.X));
		}

		@Test
		@DisplayName("returns a profile that is equal in content but not the same instance")
		void returnsDistinctClone() throws CloneNotSupportedException {
			final var profile = createProfile();

			final var clone = (Profile) profile.clone();

			Assertions.assertNotSame(profile, clone);
		}
	}

	@Nested
	@DisplayName("Profile()")
	class ConstructorTests {

		@Test
		@DisplayName("adds the default mode to the modes list on construction")
		void addsDefaultModeOnConstruction() {
			final var profile = createProfile();

			Assertions.assertEquals(1, profile.getModes().size());
			Assertions.assertSame(Profile.DEFAULT_MODE, profile.getModes().getFirst());
		}

		@Test
		@DisplayName("sets the active mode to the default mode on construction")
		void setsActiveModeToDefaultModeOnConstruction() {
			final var profile = createProfile();

			Assertions.assertSame(Profile.DEFAULT_MODE, profile.getActiveMode());
		}
	}

	@Nested
	@DisplayName("getModeByUuid()")
	class GetModeByUuidTests {

		@Test
		@DisplayName("returns an empty Optional when no mode matches the given UUID")
		void returnsEmptyWhenUuidDoesNotMatch() {
			final var profile = createProfile();

			final var result = profile.getModeByUuid(UUID.randomUUID());

			Assertions.assertTrue(result.isEmpty());
		}

		@Test
		@DisplayName("returns the mode matching the given UUID when it exists")
		void returnsModeWhenUuidMatches() {
			final var profile = createProfile();

			final var result = profile.getModeByUuid(Profile.DEFAULT_MODE.getUuid());

			Assertions.assertTrue(result.isPresent());
			Assertions.assertSame(Profile.DEFAULT_MODE, result.get());
		}
	}

	@Nested
	@DisplayName("removeMode()")
	class RemoveModeTests {

		@Test
		@DisplayName("removes the specified mode from the modes list")
		void removesModeFromList() {
			final var profile = createProfile();
			final var extraMode = new Mode(UUID.randomUUID());
			profile.getModes().add(extraMode);

			profile.removeMode(mockInput, extraMode);

			Assertions.assertFalse(profile.getModes().contains(extraMode));
		}

		@Test
		@DisplayName("removes buttonToModeActions entries that reference the removed mode")
		void removesRelatedButtonToModeActions() {
			final var profile = createProfile();
			final var modeToRemove = new Mode(UUID.randomUUID());
			profile.getModes().add(modeToRemove);

			Mockito.when(mockButtonToModeAction.getMode(mockInput)).thenReturn(modeToRemove);
			final var actions = new ArrayList<ButtonToModeAction>();
			actions.add(mockButtonToModeAction);
			final var map = new HashMap<Integer, List<ButtonToModeAction>>();
			map.put(1, actions);
			profile.setButtonToModeActionsMap(map);

			profile.removeMode(mockInput, modeToRemove);

			Assertions.assertFalse(profile.getButtonToModeActionsMap().containsKey(1));
		}

		@Test
		@DisplayName("retains buttonToModeActions entries that do not reference the removed mode")
		void retainsUnrelatedButtonToModeActions() {
			final var profile = createProfile();
			final var modeToRemove = new Mode(UUID.randomUUID());
			final var otherMode = new Mode(UUID.randomUUID());
			profile.getModes().add(modeToRemove);
			profile.getModes().add(otherMode);

			Mockito.when(mockButtonToModeAction.getMode(mockInput)).thenReturn(otherMode);
			final var actions = new ArrayList<ButtonToModeAction>();
			actions.add(mockButtonToModeAction);
			final var map = new HashMap<Integer, List<ButtonToModeAction>>();
			map.put(2, actions);
			profile.setButtonToModeActionsMap(map);

			profile.removeMode(mockInput, modeToRemove);

			Assertions.assertTrue(profile.getButtonToModeActionsMap().containsKey(2));
		}
	}

	@Nested
	@DisplayName("setActiveMode() - axis reset with runMode")
	class SetActiveModeAxisResetTests {

		@Mock
		AxisToAxisAction mockAxisToAxisAction;

		@Mock
		RunMode mockRunMode;

		@Test
		@DisplayName("resets AxisToAxisAction virtual axes when switching modes with runMode active")
		void resetsAxisToAxisActionsOnModeSwitch() {
			final var profile = createProfile();
			final var secondMode = new Mode(UUID.randomUUID());
			profile.getModes().add(secondMode);

			// The second mode has an axis action on axis 0
			final var axisActions = new ArrayList<IAction<Float>>();
			axisActions.add(mockAxisToAxisAction);
			secondMode.getAxisToActionsMap().put(0, axisActions);

			// The current (default) mode also handles the same axis
			final var currentAxisActions = new ArrayList<IAction<Float>>();
			currentAxisActions.add(mockAxisToAxisAction);
			Profile.DEFAULT_MODE.getAxisToActionsMap().put(0, currentAxisActions);

			Mockito.when(mockInput.getRunMode()).thenReturn(mockRunMode);
			Mockito.when(mockInput.getMain()).thenReturn(mockMain);
			// mockAxisToAxisAction is an AxisToAxisAction but NOT an
			// AxisToRelativeAxisAction
			Mockito.when(mockAxisToAxisAction.getVirtualAxis()).thenReturn(VirtualAxis.X);

			profile.setActiveMode(mockInput, 1);

			Mockito.verify(mockInput).setAxis(Mockito.eq(VirtualAxis.X), Mockito.eq(0f), Mockito.eq(false),
					Mockito.isNull(), Mockito.isNull(), Mockito.isNull());

			// Clean up shared DEFAULT_MODE state modified by this test
			Profile.DEFAULT_MODE.getAxisToActionsMap().remove(0);
		}
	}

	@Nested
	@DisplayName("setActiveMode(Input, int)")
	class SetActiveModeByIndexTests {

		@Test
		@DisplayName("does not change the active mode when the index is out of bounds")
		void doesNotChangeModeWhenIndexOutOfBounds() {
			final var profile = createProfile();

			profile.setActiveMode(mockInput, 99);

			Assertions.assertSame(Profile.DEFAULT_MODE, profile.getActiveMode());
		}

		@Test
		@DisplayName("switches to the mode at the given index when the index is valid and runMode is null")
		void switchesToModeAtValidIndexWhenRunModeIsNull() {
			final var profile = createProfile();
			final var secondMode = new Mode(UUID.randomUUID());
			profile.getModes().add(secondMode);

			Mockito.when(mockInput.getRunMode()).thenReturn(null);
			Mockito.when(mockInput.getMain()).thenReturn(mockMain);

			profile.setActiveMode(mockInput, 1);

			Assertions.assertSame(secondMode, profile.getActiveMode());
		}
	}

	@Nested
	@DisplayName("setActiveMode(Input, Mode)")
	class SetActiveModeByModeTests {

		@Test
		@DisplayName("switches the active mode to the specified mode object when it is in the list")
		void switchesToSpecifiedMode() {
			final var profile = createProfile();
			final var secondMode = new Mode(UUID.randomUUID());
			profile.getModes().add(secondMode);

			Mockito.when(mockInput.getRunMode()).thenReturn(null);
			Mockito.when(mockInput.getMain()).thenReturn(mockMain);

			profile.setActiveMode(mockInput, secondMode);

			Assertions.assertSame(secondMode, profile.getActiveMode());
		}
	}
}

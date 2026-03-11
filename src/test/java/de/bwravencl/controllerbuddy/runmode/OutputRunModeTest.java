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

package de.bwravencl.controllerbuddy.runmode;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.ButtonToButtonAction;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutputRunModeTest {

	@Mock
	Main mockMain;

	@Mock
	Input mockInput;

	@Mock
	Profile mockProfile;

	private static OutputRunMode.DeviceValue createDeviceValue() throws ReflectiveOperationException {
		final var constructor = OutputRunMode.DeviceValue.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		return constructor.newInstance();
	}

	@Nested
	@DisplayName("DeviceValue")
	class DeviceValueTests {

		private OutputRunMode.DeviceValue deviceValue;

		@BeforeEach
		void setUp() throws ReflectiveOperationException {
			deviceValue = createDeviceValue();
		}

		@Test
		@DisplayName("is marked as changed immediately after construction")
		void isChangedAfterConstruction() {
			Assertions.assertTrue(deviceValue.isChanged());
		}

		@Test
		@DisplayName("set() with a different value marks the value as changed")
		void setWithDifferentValueMarksChanged() {
			deviceValue.setUnchanged();
			deviceValue.set(42);
			Assertions.assertTrue(deviceValue.isChanged());
		}

		@Test
		@DisplayName("set() with the same value clears the changed flag")
		void setWithSameValueClearsChanged() {
			deviceValue.set(42);
			deviceValue.set(42);
			Assertions.assertFalse(deviceValue.isChanged());
		}

		@Test
		@DisplayName("setUnchanged() clears the changed flag")
		void setUnchangedClearsChangedFlag() {
			deviceValue.setUnchanged();
			Assertions.assertFalse(deviceValue.isChanged());
		}
	}

	@Nested
	@DisplayName("updateOutputSets()")
	class UpdateOutputSetsTests {

		@Test
		@DisplayName("elements no longer in sourceSet appear in newUpSet and are removed from oldDownSet")
		void removedElementsAppearInNewUpSet() {
			final var sourceSet = new HashSet<Integer>();
			final var oldDownSet = new HashSet<>(Set.of(1, 2));
			final var newUpSet = new HashSet<Integer>();
			final var newDownSet = new HashSet<Integer>();

			sourceSet.add(1);
			OutputRunMode.updateOutputSets(sourceSet, oldDownSet, newUpSet, newDownSet, false);

			Assertions.assertTrue(newUpSet.contains(2));
			Assertions.assertFalse(oldDownSet.contains(2));
		}

		@Test
		@DisplayName("elements newly in sourceSet appear in newDownSet and are added to oldDownSet")
		void newElementsAppearInNewDownSet() {
			final var sourceSet = new HashSet<>(Set.of(1, 2));
			final var oldDownSet = new HashSet<Integer>();
			final var newUpSet = new HashSet<Integer>();
			final var newDownSet = new HashSet<Integer>();

			OutputRunMode.updateOutputSets(sourceSet, oldDownSet, newUpSet, newDownSet, false);

			Assertions.assertTrue(newDownSet.contains(1));
			Assertions.assertTrue(newDownSet.contains(2));
			Assertions.assertTrue(oldDownSet.contains(1));
			Assertions.assertTrue(oldDownSet.contains(2));
		}

		@Test
		@DisplayName("still-down elements appear in newDownSet when keepStillDown is true")
		void stillDownElementsIncludedInNewDownSetWhenKeepStillDown() {
			final var sourceSet = new HashSet<>(Set.of(1));
			final var oldDownSet = new HashSet<>(Set.of(1));
			final var newUpSet = new HashSet<Integer>();
			final var newDownSet = new HashSet<Integer>();

			OutputRunMode.updateOutputSets(sourceSet, oldDownSet, newUpSet, newDownSet, true);

			Assertions.assertTrue(newDownSet.contains(1));
			Assertions.assertTrue(newUpSet.isEmpty());
		}

		@Test
		@DisplayName("still-down elements do not appear in newDownSet when keepStillDown is false")
		void stillDownElementsExcludedFromNewDownSetWhenNotKeepStillDown() {
			final var sourceSet = new HashSet<>(Set.of(1));
			final var oldDownSet = new HashSet<>(Set.of(1));
			final var newUpSet = new HashSet<Integer>();
			final var newDownSet = new HashSet<Integer>();

			OutputRunMode.updateOutputSets(sourceSet, oldDownSet, newUpSet, newDownSet, false);

			Assertions.assertFalse(newDownSet.contains(1));
			Assertions.assertTrue(newUpSet.isEmpty());
		}

		@Test
		@DisplayName("all previously-down elements are released when sourceSet is empty")
		void allElementsReleasedWhenSourceSetIsEmpty() {
			final var sourceSet = new HashSet<Integer>();
			final var oldDownSet = new HashSet<>(Set.of(1, 2, 3));
			final var newUpSet = new HashSet<Integer>();
			final var newDownSet = new HashSet<Integer>();

			OutputRunMode.updateOutputSets(sourceSet, oldDownSet, newUpSet, newDownSet, false);

			Assertions.assertEquals(Set.of(1, 2, 3), newUpSet);
			Assertions.assertTrue(oldDownSet.isEmpty());
			Assertions.assertTrue(newDownSet.isEmpty());
		}

		@Test
		@DisplayName("newUpSet and newDownSet are cleared before each update")
		void setsAreClearedBeforeUpdate() {
			final var sourceSet = new HashSet<Integer>();
			final var oldDownSet = new HashSet<Integer>();
			final var newUpSet = new HashSet<>(Set.of(99));
			final var newDownSet = new HashSet<>(Set.of(88));

			OutputRunMode.updateOutputSets(sourceSet, oldDownSet, newUpSet, newDownSet, false);

			Assertions.assertFalse(newUpSet.contains(99));
			Assertions.assertFalse(newDownSet.contains(88));
		}
	}

	@Nested
	@DisplayName("enoughButtons()")
	class EnoughButtonsTests {

		private LocalRunMode runMode;

		@BeforeEach
		void setUp() {
			Mockito.when(mockInput.getProfile()).thenReturn(mockProfile);
			runMode = new LocalRunMode(mockMain, mockInput);
		}

		private boolean invokeEnoughButtons(final int numButtons) throws ReflectiveOperationException {
			final var method = OutputRunMode.class.getDeclaredMethod("enoughButtons", int.class);
			method.setAccessible(true);
			return (boolean) method.invoke(runMode, numButtons);
		}

		@Test
		@DisplayName("returns true when the profile has no modes")
		void returnsTrueWhenNoModes() throws ReflectiveOperationException {
			Mockito.when(mockProfile.getModes()).thenReturn(List.of());
			Assertions.assertTrue(invokeEnoughButtons(0));
		}

		@Test
		@DisplayName("returns true when no mode contains a ToButtonAction")
		void returnsTrueWhenNoToButtonAction() throws ReflectiveOperationException {
			Mockito.when(mockProfile.getModes()).thenReturn(List.of(new Mode()));
			Assertions.assertTrue(invokeEnoughButtons(0));
		}

		@Test
		@DisplayName("returns true when numButtons exactly meets the requirement")
		void returnsTrueWhenNumButtonsMeetsRequirement() throws ReflectiveOperationException {
			final var action = new ButtonToButtonAction();
			action.setButtonId(5);
			final var mode = new Mode();
			mode.getButtonToActionsMap().put(0, List.of(action));
			Mockito.when(mockProfile.getModes()).thenReturn(List.of(mode));

			// buttonId=5 means requiredButtons=6; exactly 6 should pass
			Assertions.assertTrue(invokeEnoughButtons(6));
		}

		@Test
		@DisplayName("returns false when numButtons is less than required")
		void returnsFalseWhenNumButtonsInsufficient() throws ReflectiveOperationException {
			Assumptions.assumeFalse(Main.IS_MAC,
					"Skipping: enoughButtons() throws UnsupportedOperationException on macOS");
			final var skipDialogsField = Main.class.getDeclaredField("skipMessageDialogs");
			skipDialogsField.setAccessible(true);
			skipDialogsField.setBoolean(null, true);
			try {
				final var action = new ButtonToButtonAction();
				action.setButtonId(5);
				final var mode = new Mode();
				mode.getButtonToActionsMap().put(0, List.of(action));
				Mockito.when(mockProfile.getModes()).thenReturn(List.of(mode));

				// buttonId=5 means requiredButtons=6; only 5 available
				Assertions.assertFalse(invokeEnoughButtons(5));
			} finally {
				skipDialogsField.setBoolean(null, false);
			}
		}

		@Test
		@DisplayName("selects the maximum buttonId across actions in multiple modes")
		void selectsMaxButtonIdAcrossMultipleModes() throws ReflectiveOperationException {
			final var action1 = new ButtonToButtonAction();
			action1.setButtonId(3);
			final var mode1 = new Mode();
			mode1.getButtonToActionsMap().put(0, List.of(action1));

			final var action2 = new ButtonToButtonAction();
			action2.setButtonId(7);
			final var mode2 = new Mode();
			mode2.getButtonToActionsMap().put(0, List.of(action2));

			Mockito.when(mockProfile.getModes()).thenReturn(List.of(mode1, mode2));

			// max buttonId=7 across both modes, so requiredButtons=8
			Assertions.assertTrue(invokeEnoughButtons(8));
		}
	}
}

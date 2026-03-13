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

package de.bwravencl.controllerbuddy.input.action.gui;

import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class ActivationEditorBuilderTest {

	@BeforeAll
	static void ensureMainInitialized() {
		final var ignored = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("getValues()")
	class GetValuesTests {

		private ActivationEditorBuilder createBuilderWithAction(
				final de.bwravencl.controllerbuddy.input.action.IAction<?> action) throws Exception {
			// Use Mockito to create a mock that bypasses the constructor
			final var builder = Mockito.mock(ActivationEditorBuilder.class, Mockito.CALLS_REAL_METHODS);

			// Set the action field via reflection
			final var actionField = EditorBuilder.class.getDeclaredField("action");
			actionField.setAccessible(true);
			actionField.set(builder, action);

			return builder;
		}

		@Test
		@DisplayName("returns all Activation values for non-cycle actions")
		void returnsAllValuesForNonCycleAction() throws Exception {
			final var action = new de.bwravencl.controllerbuddy.input.action.ButtonToButtonAction();

			final var builder = createBuilderWithAction(action);

			final var getValuesMethod = ActivationEditorBuilder.class.getDeclaredMethod("getValues");
			getValuesMethod.setAccessible(true);
			final var values = (Activation[]) getValuesMethod.invoke(builder);

			Assertions.assertEquals(Activation.values().length, values.length);
		}

		@Test
		@DisplayName("returns only ON_PRESS and ON_RELEASE for ButtonToCycleAction")
		void returnsLimitedValuesForCycleAction() throws Exception {
			final var action = new de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction();

			// Create a minimal ActivationEditorBuilder via reflection, bypassing the
			// constructor
			final var builder = createBuilderWithAction(action);

			final var getValuesMethod = ActivationEditorBuilder.class.getDeclaredMethod("getValues");
			getValuesMethod.setAccessible(true);
			final var values = (Activation[]) getValuesMethod.invoke(builder);

			Assertions.assertEquals(2, values.length);
			Assertions.assertEquals(Activation.ON_PRESS, values[0]);
			Assertions.assertEquals(Activation.ON_RELEASE, values[1]);
		}
	}
}

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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class DescribableActionTest {

	@Mock
	Input mockInput;

	@BeforeAll
	static void ensureMainInitialized() {
		final var _ = de.bwravencl.controllerbuddy.gui.Main.STRINGS;
	}

	@Nested
	@DisplayName("getDescription()")
	final class GetDescriptionTests {

		@Test
		@DisplayName("returns custom description when set")
		void returnsCustomDescription() {
			final var action = new ButtonToAxisResetAction();
			action.setDescription("Custom label");
			Assertions.assertEquals("Custom label", action.getDescription(mockInput));
		}

		@Test
		@DisplayName("returns default description from @Action annotation when no custom description is set")
		void returnsDefaultDescriptionWhenEmpty() {
			final var action = new ButtonToAxisResetAction();
			final var description = action.getDescription(mockInput);
			Assertions.assertNotNull(description);
			Assertions.assertFalse(description.isEmpty());
		}

		@Test
		@DisplayName("returns default description when custom description is empty string")
		void returnsDefaultForEmptyString() {
			final var action = new ButtonToAxisResetAction();
			action.setDescription("");
			final var description = action.getDescription(mockInput);
			// Should fall through to default since isEmpty() is true
			Assertions.assertNotNull(description);
			Assertions.assertFalse(description.isEmpty());
		}
	}

	@Nested
	@DisplayName("isDescriptionEmpty()")
	final class IsDescriptionEmptyTests {

		@Test
		@DisplayName("returns false when description is non-empty")
		void returnsFalseForNonEmpty() {
			final var action = new ButtonToAxisResetAction();
			action.setDescription("something");
			Assertions.assertFalse(action.isDescriptionEmpty());
		}

		@Test
		@DisplayName("returns true when description is empty string")
		void returnsTrueForEmpty() {
			final var action = new ButtonToAxisResetAction();
			action.setDescription("");
			Assertions.assertTrue(action.isDescriptionEmpty());
		}

		@Test
		@DisplayName("returns true when description is null")
		void returnsTrueForNull() {
			final var action = new ButtonToAxisResetAction();
			Assertions.assertTrue(action.isDescriptionEmpty());
		}
	}
}

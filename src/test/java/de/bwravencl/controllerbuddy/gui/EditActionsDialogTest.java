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

package de.bwravencl.controllerbuddy.gui;

import de.bwravencl.controllerbuddy.input.action.DescribableAction;
import de.bwravencl.controllerbuddy.input.action.NullAction;
import de.bwravencl.controllerbuddy.input.action.ToAxisAction;
import java.awt.Toolkit;
import java.lang.reflect.Field;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

final class EditActionsDialogTest {

	/// [EditActionsDialog]'s static initializer calls [Toolkit#createCustomCursor],
	/// which throws [java.awt.HeadlessException] in headless mode. We mock
	/// [Toolkit#getDefaultToolkit] in @[BeforeAll] so the mock is active when the
	/// final class initializer first runs (triggered by the first reference to
	/// [EditActionsDialog] inside a test method body).
	private static MockedStatic<Toolkit> toolkitMock;

	@BeforeAll
	static void setUpToolkitMock() {
		final var mockToolkit = Mockito.mock(Toolkit.class);
		toolkitMock = Mockito.mockStatic(Toolkit.class);
		toolkitMock.when(Toolkit::getDefaultToolkit).thenReturn(mockToolkit);
	}

	@AfterAll
	static void tearDownToolkitMock() {
		toolkitMock.close();
	}

	@Nested
	@DisplayName("findFirstMissingOrNext()")
	final class FindFirstMissingOrNextTests {

		@Test
		@DisplayName("clamps the result to maxValue when the full range is occupied")
		void clampsToMaxValueWhenFull() {
			// {0, 1, 2} with maxValue=2: next would be 3 but is clamped to 2
			Assertions.assertEquals(2, EditActionsDialog.findFirstMissingOrNext(IntStream.of(0, 1, 2), 2));
		}

		@Test
		@DisplayName("deduplicates values before searching")
		void deduplicatesValues() {
			// {0, 0, 1}: after dedup → {0, 1} → next = 2
			Assertions.assertEquals(2, EditActionsDialog.findFirstMissingOrNext(IntStream.of(0, 0, 1), 5));
		}

		@Test
		@DisplayName("filters out values above maxValue before searching")
		void filtersValuesAboveMaxValue() {
			// {0, 1, 10} with maxValue=5: 10 is excluded, leaving {0, 1} → next = 2
			Assertions.assertEquals(2, EditActionsDialog.findFirstMissingOrNext(IntStream.of(0, 1, 10), 5));
		}

		@Test
		@DisplayName("returns the position of the first gap in an otherwise consecutive sequence")
		void returnsFirstGapPosition() {
			// {0, 2, 3}: gap at 1
			Assertions.assertEquals(1, EditActionsDialog.findFirstMissingOrNext(IntStream.of(0, 2, 3), 5));
		}

		@Test
		@DisplayName("returns the next consecutive value after a dense sequence")
		void returnsNextAfterDenseSequence() {
			Assertions.assertEquals(3, EditActionsDialog.findFirstMissingOrNext(IntStream.of(0, 1, 2), 5));
		}

		@Test
		@DisplayName("returns 0 for an empty stream")
		void returnsZeroForEmptyStream() {
			Assertions.assertEquals(0, EditActionsDialog.findFirstMissingOrNext(IntStream.empty(), 5));
		}

		@Test
		@DisplayName("sorts unsorted input before searching")
		void sortsInputInternally() {
			// {2, 0, 1}: sorted → {0, 1, 2} → next = 3
			Assertions.assertEquals(3, EditActionsDialog.findFirstMissingOrNext(IntStream.of(2, 0, 1), 5));
		}
	}

	@Nested
	@DisplayName("getFieldToActionPropertiesMap()")
	final class GetFieldToActionPropertiesMapTests {

		@Test
		@DisplayName("returns the same cached map instance on repeated calls")
		void cachesSameMapInstance() {
			final var map1 = EditActionsDialog.getFieldToActionPropertiesMap(NullAction.class);
			final var map2 = EditActionsDialog.getFieldToActionPropertiesMap(NullAction.class);
			Assertions.assertSame(map1, map2);
		}

		@Test
		@DisplayName("includes @ActionProperty fields inherited from parent IAction classes")
		void includesInheritedAnnotatedFields() {
			// ToAxisAction extends InvertableAction extends DescribableAction;
			// expects own 'virtualAxis' + inherited 'invert' + inherited 'description'
			final var map = EditActionsDialog.getFieldToActionPropertiesMap(ToAxisAction.class);
			final var fieldNames = map.keySet().stream().map(Field::getName).toList();
			Assertions.assertEquals(3, map.size());
			Assertions.assertTrue(fieldNames.contains("virtualAxis"));
			Assertions.assertTrue(fieldNames.contains("invert"));
			Assertions.assertTrue(fieldNames.contains("description"));
		}

		@Test
		@DisplayName("returns an empty map for an action final class with no @ActionProperty fields")
		void returnsEmptyMapForActionWithNoProperties() {
			Assertions.assertTrue(EditActionsDialog.getFieldToActionPropertiesMap(NullAction.class).isEmpty());
		}

		@Test
		@DisplayName("returns own @ActionProperty fields for a direct IAction implementor")
		void returnsOwnAnnotatedFields() {
			final var map = EditActionsDialog.getFieldToActionPropertiesMap(DescribableAction.class);
			final var fieldNames = map.keySet().stream().map(Field::getName).toList();
			Assertions.assertEquals(1, map.size());
			Assertions.assertTrue(fieldNames.contains("description"));
		}

		@Test
		@DisplayName("throws IllegalArgumentException for a final class that does not implement IAction")
		void throwsForNonIActionClass() {
			Assertions.assertThrows(IllegalArgumentException.class,
					() -> EditActionsDialog.getFieldToActionPropertiesMap(String.class));
		}
	}
}

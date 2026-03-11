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

package de.bwravencl.controllerbuddy.gui;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MainTest {

	private static PlainDocument createLimitedLengthPlainDocument(final int limit) throws ReflectiveOperationException {
		final var clazz = Arrays.stream(Main.class.getDeclaredClasses())
				.filter(c -> "LimitedLengthPlainDocument".equals(c.getSimpleName())).findFirst().orElseThrow();
		final var constructor = clazz.getDeclaredConstructor(int.class);
		constructor.setAccessible(true);
		return (PlainDocument) constructor.newInstance(limit);
	}

	private static String invokeCreateVisualizationLegendHtml(final Set<String> usedSymbols)
			throws ReflectiveOperationException {
		final var method = Main.class.getDeclaredMethod("createVisualizationLegendHtml", Set.class);
		method.setAccessible(true);
		return (String) method.invoke(null, usedSymbols);
	}

	private static int invokeGetExtendedKeyCodeForMenu(final JButton button, final Set<Integer> alreadyAssigned)
			throws ReflectiveOperationException {
		final var method = Main.class.getDeclaredMethod("getExtendedKeyCodeForMenu", javax.swing.AbstractButton.class,
				Set.class);
		method.setAccessible(true);
		return (int) method.invoke(null, button, alreadyAssigned);
	}

	private static boolean invokeIsValidHost(final String host) throws ReflectiveOperationException {
		final var method = Main.class.getDeclaredMethod("isValidHost", String.class);
		method.setAccessible(true);
		return (boolean) method.invoke(null, host);
	}

	private static boolean invokeIsValidPassword(final String password) throws ReflectiveOperationException {
		final var method = Main.class.getDeclaredMethod("isValidPassword", String.class);
		method.setAccessible(true);
		return (boolean) method.invoke(null, password);
	}

	@Nested
	@DisplayName("createVisualizationLegendHtml()")
	class CreateVisualizationLegendHtmlTests {

		@Test
		@DisplayName("ignores symbols not present in the known symbol-to-description map")
		void ignoresUnknownSymbols() throws ReflectiveOperationException {
			final var result = invokeCreateVisualizationLegendHtml(Set.of("NO_SUCH_SYMBOL_999"));
			// The unknown symbol is filtered out, leaving an empty map - but the
			// !usedSymbols.isEmpty() guard has already passed, so we get a table header
			// with no data rows.
			Assertions.assertTrue(result.contains("<table>"));
			Assertions.assertFalse(result.contains("NO_SUCH_SYMBOL_999"));
		}

		@Test
		@DisplayName("includes a known symbol's description in the output")
		void includesKnownSymbolDescription() throws ReflectiveOperationException {
			// SWAPPED_SYMBOL ("⇆") is always in SYMBOL_TO_DESCRIPTION_MAP.
			final var result = invokeCreateVisualizationLegendHtml(Set.of(Main.SWAPPED_SYMBOL));
			Assertions.assertTrue(result.contains(Main.SWAPPED_SYMBOL));
			Assertions.assertTrue(result.contains("<td"));
		}

		@Test
		@DisplayName("inserts <tr> row breaks every VISUALIZATION_LEGEND_ITEMS_PER_ROW items")
		void insertsRowBreaksAtCorrectIntervals() throws ReflectiveOperationException {
			// Provide enough known symbols to force at least two rows.
			// SYMBOL_TO_DESCRIPTION_MAP has 9 entries total. Use all known symbols.
			final var mapField = Main.class.getDeclaredField("SYMBOL_TO_DESCRIPTION_MAP");
			mapField.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var map = (java.util.Map<String, String>) mapField.get(null);
			final var allSymbols = new LinkedHashSet<>(map.keySet());

			final var result = invokeCreateVisualizationLegendHtml(allSymbols);

			// Count <tr> tags. With N items and ITEMS_PER_ROW = 6, we expect
			// ceil(N / 6) rows.
			final var trCount = result.split("<tr>", -1).length - 1;
			final var expectedRows = (int) Math
					.ceil((double) allSymbols.size() / Main.VISUALIZATION_LEGEND_ITEMS_PER_ROW);
			Assertions.assertEquals(expectedRows, trCount);
		}

		@Test
		@DisplayName("returns bare HTML wrapper for an empty symbol set")
		void returnsBareHtmlForEmptySet() throws ReflectiveOperationException {
			Assertions.assertEquals("<html></html>", invokeCreateVisualizationLegendHtml(Set.of()));
		}
	}

	@Nested
	@DisplayName("getExtendedKeyCodeForMenu()")
	class GetExtendedKeyCodeForMenuTests {

		@Test
		@DisplayName("adds the chosen key code to the already-assigned set as a side effect")
		void addsChosenKeyCodeToSet() throws ReflectiveOperationException {
			final var alreadyAssigned = new HashSet<Integer>();
			final var button = new JButton("Edit");
			invokeGetExtendedKeyCodeForMenu(button, alreadyAssigned);
			Assertions.assertTrue(alreadyAssigned.contains(KeyEvent.getExtendedKeyCodeForChar('E')));
		}

		@Test
		@DisplayName("returns the key code of the first character when no conflicts exist")
		void returnsFirstCharKeyCode() throws ReflectiveOperationException {
			final var button = new JButton("File");
			final var keyCode = invokeGetExtendedKeyCodeForMenu(button, new HashSet<>());
			Assertions.assertEquals(KeyEvent.getExtendedKeyCodeForChar('F'), keyCode);
		}

		@Test
		@DisplayName("returns VK_UNDEFINED for a button with empty text")
		void returnsUndefinedForEmptyText() throws ReflectiveOperationException {
			final var button = new JButton("");
			Assertions.assertEquals(KeyEvent.VK_UNDEFINED, invokeGetExtendedKeyCodeForMenu(button, new HashSet<>()));
		}

		@Test
		@DisplayName("returns VK_UNDEFINED for a button with null text")
		void returnsUndefinedForNullText() throws ReflectiveOperationException {
			final var button = new JButton((String) null);
			Assertions.assertEquals(KeyEvent.VK_UNDEFINED, invokeGetExtendedKeyCodeForMenu(button, new HashSet<>()));
		}

		@Test
		@DisplayName("skips to the next character when the first character's key code is already assigned")
		void skipsAlreadyAssignedFirstChar() throws ReflectiveOperationException {
			final var alreadyAssigned = new HashSet<>(Set.of(KeyEvent.getExtendedKeyCodeForChar('F')));
			final var button = new JButton("File");
			final var keyCode = invokeGetExtendedKeyCodeForMenu(button, alreadyAssigned);
			// 'F' is taken, so 'i' should be chosen
			Assertions.assertEquals(KeyEvent.getExtendedKeyCodeForChar('i'), keyCode);
		}

		@Test
		@DisplayName("two menus with the same first letter get different key codes")
		void twoMenusWithSameFirstLetterGetDifferentCodes() throws ReflectiveOperationException {
			final var alreadyAssigned = new HashSet<Integer>();
			final var code1 = invokeGetExtendedKeyCodeForMenu(new JButton("File"), alreadyAssigned);
			final var code2 = invokeGetExtendedKeyCodeForMenu(new JButton("Format"), alreadyAssigned);
			Assertions.assertNotEquals(code1, code2);
			// "File" claims 'F', so "Format" must fall back to 'o'
			Assertions.assertEquals(KeyEvent.getExtendedKeyCodeForChar('o'), code2);
		}
	}

	@Nested
	@DisplayName("HotSwappingButton.fromId()")
	class HotSwappingButtonFromIdTests {

		@Test
		@DisplayName("every enum constant round-trips through fromId")
		void allConstantsRoundTrip() throws ReflectiveOperationException {
			for (final var button : EnumSet.allOf(Main.HotSwappingButton.class)) {
				Assertions.assertEquals(button, invokeFromId(button.id));
			}
		}

		private Main.HotSwappingButton invokeFromId(final int id) throws ReflectiveOperationException {
			final var method = Main.HotSwappingButton.class.getDeclaredMethod("fromId", int.class);
			method.setAccessible(true);
			return (Main.HotSwappingButton) method.invoke(null, id);
		}

		@Test
		@DisplayName("returns the correct enum constant for a known button id")
		void returnsCorrectConstantForKnownId() throws ReflectiveOperationException {
			Assertions.assertEquals(Main.HotSwappingButton.A, invokeFromId(Main.HotSwappingButton.A.id));
		}

		@Test
		@DisplayName("returns NONE for its own id (-1)")
		void returnsNoneForNoneId() throws ReflectiveOperationException {
			Assertions.assertEquals(Main.HotSwappingButton.NONE, invokeFromId(-1));
		}

		@Test
		@DisplayName("returns NONE for an id that does not match any enum constant")
		void returnsNoneForUnknownId() throws ReflectiveOperationException {
			Assertions.assertEquals(Main.HotSwappingButton.NONE, invokeFromId(Integer.MIN_VALUE));
		}
	}

	@Nested
	@DisplayName("isValidHost()")
	class IsValidHostTests {

		@Test
		@DisplayName("returns false for a blank string")
		void returnsFalseForBlank() throws ReflectiveOperationException {
			Assertions.assertFalse(invokeIsValidHost("   "));
		}

		@Test
		@DisplayName("returns false for null")
		void returnsFalseForNull() throws ReflectiveOperationException {
			Assertions.assertFalse(invokeIsValidHost(null));
		}

		@Test
		@DisplayName("returns true for a hostname string")
		void returnsTrueForHostname() throws ReflectiveOperationException {
			Assertions.assertTrue(invokeIsValidHost("my-controller.local"));
		}

		@Test
		@DisplayName("returns true for a non-blank host string")
		void returnsTrueForValidHost() throws ReflectiveOperationException {
			Assertions.assertTrue(invokeIsValidHost("192.168.1.1"));
		}
	}

	@Nested
	@DisplayName("isValidPassword()")
	class IsValidPasswordTests {

		@Test
		@DisplayName("returns false for a blank string")
		void returnsFalseForBlank() throws ReflectiveOperationException {
			Assertions.assertFalse(invokeIsValidPassword("   "));
		}

		@Test
		@DisplayName("returns false for null")
		void returnsFalseForNull() throws ReflectiveOperationException {
			Assertions.assertFalse(invokeIsValidPassword(null));
		}

		@Test
		@DisplayName("returns false when length exceeds the maximum")
		void returnsFalseWhenTooLong() throws ReflectiveOperationException {
			Assertions.assertFalse(invokeIsValidPassword("a".repeat(Main.PASSWORD_MAX_LENGTH + 1)));
		}

		@Test
		@DisplayName("returns false when length is below the minimum")
		void returnsFalseWhenTooShort() throws ReflectiveOperationException {
			Assertions.assertFalse(invokeIsValidPassword("a".repeat(Main.PASSWORD_MIN_LENGTH - 1)));
		}

		@Test
		@DisplayName("returns true at the maximum length boundary")
		void returnsTrueAtMaximumLength() throws ReflectiveOperationException {
			Assertions.assertTrue(invokeIsValidPassword("a".repeat(Main.PASSWORD_MAX_LENGTH)));
		}

		@Test
		@DisplayName("returns true at the minimum length boundary")
		void returnsTrueAtMinimumLength() throws ReflectiveOperationException {
			Assertions.assertTrue(invokeIsValidPassword("a".repeat(Main.PASSWORD_MIN_LENGTH)));
		}
	}

	@Nested
	@DisplayName("LimitedLengthPlainDocument")
	class LimitedLengthPlainDocumentTests {

		@Test
		@DisplayName("accepts an insertion that brings the document exactly to the limit")
		void acceptsInsertionAtExactLimit() throws ReflectiveOperationException, BadLocationException {
			final var doc = createLimitedLengthPlainDocument(5);
			doc.insertString(0, "hello", null);
			Assertions.assertEquals(5, doc.getLength());
		}

		@Test
		@DisplayName("accepts an insertion that fits within the limit")
		void acceptsInsertionWithinLimit() throws ReflectiveOperationException, BadLocationException {
			final var doc = createLimitedLengthPlainDocument(10);
			doc.insertString(0, "hello", null);
			Assertions.assertEquals("hello", doc.getText(0, doc.getLength()));
		}

		@Test
		@DisplayName("silently ignores a null string insertion")
		void ignoresNullInsertion() throws ReflectiveOperationException, BadLocationException {
			final var doc = createLimitedLengthPlainDocument(5);
			doc.insertString(0, null, null);
			Assertions.assertEquals(0, doc.getLength());
		}

		@Test
		@DisplayName("rejects an insertion entirely when it would exceed the limit")
		void rejectsInsertionExceedingLimit() throws ReflectiveOperationException, BadLocationException {
			final var doc = createLimitedLengthPlainDocument(5);
			doc.insertString(0, "hello", null);
			// This would bring the total to 10, exceeding the limit of 5
			doc.insertString(5, "world", null);
			Assertions.assertEquals("hello", doc.getText(0, doc.getLength()));
		}

		@Test
		@DisplayName("rejects a single-character insertion when the document is already at the limit")
		void rejectsSingleCharWhenFull() throws ReflectiveOperationException, BadLocationException {
			final var doc = createLimitedLengthPlainDocument(3);
			doc.insertString(0, "abc", null);
			doc.insertString(3, "d", null);
			Assertions.assertEquals("abc", doc.getText(0, doc.getLength()));
		}
	}

	@Nested
	@DisplayName("Theme.fromId()")
	class ThemeFromIdTests {

		@Test
		@DisplayName("falls back to SYSTEM for an unknown id")
		void fallsBackToSystemForUnknownId() throws ReflectiveOperationException {
			Assertions.assertEquals(getThemeConstant("SYSTEM"), invokeFromId(999));
		}

		private Object getThemeConstant(final String name) throws ReflectiveOperationException {
			final var themeClass = Arrays.stream(Main.class.getDeclaredClasses())
					.filter(c -> "Theme".equals(c.getSimpleName())).findFirst().orElseThrow();
			final var field = themeClass.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(null);
		}

		private Object invokeFromId(final int id) throws ReflectiveOperationException {
			final var themeClass = Arrays.stream(Main.class.getDeclaredClasses())
					.filter(c -> "Theme".equals(c.getSimpleName())).findFirst().orElseThrow();
			final var method = themeClass.getDeclaredMethod("fromId", int.class);
			method.setAccessible(true);
			return method.invoke(null, id);
		}

		@Test
		@DisplayName("returns DARK for id 2")
		void returnsDarkForId2() throws ReflectiveOperationException {
			Assertions.assertEquals(getThemeConstant("DARK"), invokeFromId(2));
		}

		@Test
		@DisplayName("returns LIGHT for id 1")
		void returnsLightForId1() throws ReflectiveOperationException {
			Assertions.assertEquals(getThemeConstant("LIGHT"), invokeFromId(1));
		}

		@Test
		@DisplayName("returns SYSTEM for id 0")
		void returnsSystemForId0() throws ReflectiveOperationException {
			Assertions.assertEquals(getThemeConstant("SYSTEM"), invokeFromId(0));
		}
	}
}

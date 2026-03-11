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

import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.ScanCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OnScreenKeyboardTest {

	private static String invokeGetDefaultKeyDisplayName(final String name) throws ReflectiveOperationException {
		final var method = OnScreenKeyboard.class.getDeclaredMethod("getDefaultKeyDisplayName", String.class);
		method.setAccessible(true);
		return (String) method.invoke(null, name);
	}

	private static String invokeGetLockKeyDisplayName(final String name) throws ReflectiveOperationException {
		final var method = OnScreenKeyboard.class.getDeclaredMethod("getLockKeyDisplayName", String.class);
		method.setAccessible(true);
		return (String) method.invoke(null, name);
	}

	private static String invokeMultilineDisplayName(final String name) throws ReflectiveOperationException {
		final var method = OnScreenKeyboard.class.getDeclaredMethod("multilineDisplayName", String.class);
		method.setAccessible(true);
		return (String) method.invoke(null, name);
	}

	@Nested
	@DisplayName("getDefaultKeyDisplayName()")
	class GetDefaultKeyDisplayNameTests {

		@Test
		@DisplayName("DIK_SYSRQ returns a multiline HTML display name")
		void dikSysrqReturnsMultilineDisplayName() throws ReflectiveOperationException {
			Assertions.assertEquals("<html><center>Sys<br>Rq</center></html>",
					invokeGetDefaultKeyDisplayName(ScanCode.DIK_SYSRQ));
		}

		@Test
		@DisplayName("Down Arrow key name is converted to the ↓ character")
		void downArrowKeyConvertsToUnicodeArrow() throws ReflectiveOperationException {
			Assertions.assertEquals("↓", invokeGetDefaultKeyDisplayName(ScanCode.DIK_DOWN));
		}

		@Test
		@DisplayName("'L ' prefix is stripped from key name")
		void lPrefixIsStripped() throws ReflectiveOperationException {
			// DIK_LCONTROL = "L Ctrl" → "Ctrl"
			Assertions.assertEquals("Ctrl", invokeGetDefaultKeyDisplayName(ScanCode.DIK_LCONTROL));
		}

		@Test
		@DisplayName("Left Arrow key name is converted to the ← character")
		void leftArrowKeyConvertsToUnicodeArrow() throws ReflectiveOperationException {
			Assertions.assertEquals("←", invokeGetDefaultKeyDisplayName(ScanCode.DIK_LEFT));
		}

		@Test
		@DisplayName("'Num' prefix is stripped from numpad key name")
		void numPrefixIsStripped() throws ReflectiveOperationException {
			// DIK_NUMPAD7 = "Num7" → "7"
			Assertions.assertEquals("7", invokeGetDefaultKeyDisplayName(ScanCode.DIK_NUMPAD7));
		}

		@Test
		@DisplayName("'R ' prefix is stripped from key name")
		void rPrefixIsStripped() throws ReflectiveOperationException {
			// DIK_RCONTROL = "R Ctrl" → "Ctrl"
			Assertions.assertEquals("Ctrl", invokeGetDefaultKeyDisplayName(ScanCode.DIK_RCONTROL));
		}

		@Test
		@DisplayName("Right Arrow key name is converted to the → character")
		void rightArrowKeyConvertsToUnicodeArrow() throws ReflectiveOperationException {
			Assertions.assertEquals("→", invokeGetDefaultKeyDisplayName(ScanCode.DIK_RIGHT));
		}

		@Test
		@DisplayName("a single uppercase letter is lowercased")
		void singleUppercaseLetterIsLowercased() throws ReflectiveOperationException {
			Assertions.assertEquals("a", invokeGetDefaultKeyDisplayName("A"));
		}

		@Test
		@DisplayName("a name with no special prefix or suffix is returned unchanged")
		void unremarkableNameReturnedUnchanged() throws ReflectiveOperationException {
			// "Escape" has no L/R/Num prefix, is not a single uppercase letter, and
			// does not end with "Arrow"
			Assertions.assertEquals("Escape", invokeGetDefaultKeyDisplayName("Escape"));
		}

		@Test
		@DisplayName("Up Arrow key name is converted to the ↑ character")
		void upArrowKeyConvertsToUnicodeArrow() throws ReflectiveOperationException {
			Assertions.assertEquals("↑", invokeGetDefaultKeyDisplayName(ScanCode.DIK_UP));
		}
	}

	@Nested
	@DisplayName("getLockKeyDisplayName()")
	class GetLockKeyDisplayNameTests {

		@Test
		@DisplayName("CAPS_LOCK name is returned as-is without HTML wrapping")
		void capsLockReturnedAsIs() throws ReflectiveOperationException {
			Assertions.assertEquals(LockKey.CAPS_LOCK, invokeGetLockKeyDisplayName(LockKey.CAPS_LOCK));
		}

		@Test
		@DisplayName("other lock key names are wrapped in multiline HTML")
		void otherLockKeyWrappedInHtml() throws ReflectiveOperationException {
			// NUM_LOCK = "Num Lock" → "<html><center>Num<br>Lock</center></html>"
			Assertions.assertEquals("<html><center>Num<br>Lock</center></html>",
					invokeGetLockKeyDisplayName(LockKey.NUM_LOCK));
		}
	}

	@Nested
	@DisplayName("multilineDisplayName()")
	class MultilineDisplayNameTests {

		@Test
		@DisplayName("only the first space is replaced with <br>")
		void onlyFirstSpaceIsReplaced() throws ReflectiveOperationException {
			Assertions.assertEquals("<html><center>a<br>b c</center></html>", invokeMultilineDisplayName("a b c"));
		}

		@Test
		@DisplayName("a string without spaces is wrapped in HTML without a <br>")
		void stringWithoutSpaceHasNoLineBreak() throws ReflectiveOperationException {
			Assertions.assertEquals("<html><center>NoSpace</center></html>", invokeMultilineDisplayName("NoSpace"));
		}

		@Test
		@DisplayName("wraps text in HTML center tags and replaces first space with <br>")
		void wrapsWithHtmlAndReplacesFirstSpace() throws ReflectiveOperationException {
			Assertions.assertEquals("<html><center>foo<br>bar</center></html>", invokeMultilineDisplayName("foo bar"));
		}
	}
}

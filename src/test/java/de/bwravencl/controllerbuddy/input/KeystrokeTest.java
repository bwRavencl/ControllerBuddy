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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KeystrokeTest {

	private static Keystroke createEmptyKeystroke() {
		return new Keystroke();
	}

	private static Keystroke createKeystroke(final Scancode[] keyCodes, final Scancode[] modifierCodes) {
		return new Keystroke(keyCodes, modifierCodes);
	}

	@Nested
	@DisplayName("clone()")
	class CloneTests {

		@Test
		@DisplayName("keyCodes array in the clone is a distinct copy")
		void clonesKeyCodesArrayAsDistinctCopy() throws CloneNotSupportedException {
			final var keyCode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			final var original = createKeystroke(new Scancode[] { keyCode }, new Scancode[0]);

			final var clone = (Keystroke) original.clone();

			Assertions.assertNotSame(original.getKeyCodes(), clone.getKeyCodes());
			Assertions.assertArrayEquals(original.getKeyCodes(), clone.getKeyCodes());
		}

		@Test
		@DisplayName("modifierCodes array in the clone is a distinct copy")
		void clonesModifierCodesArrayAsDistinctCopy() throws CloneNotSupportedException {
			final var modifier = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_LSHIFT);
			final var original = createKeystroke(new Scancode[0], new Scancode[] { modifier });

			final var clone = (Keystroke) original.clone();

			Assertions.assertNotSame(original.getModifierCodes(), clone.getModifierCodes());
			Assertions.assertArrayEquals(original.getModifierCodes(), clone.getModifierCodes());
		}

		@Test
		@DisplayName("returns a Keystroke that is not the same instance as the original")
		void returnsDistinctInstance() throws CloneNotSupportedException {
			final var original = createEmptyKeystroke();
			Assertions.assertNotSame(original, original.clone());
		}
	}

	@Nested
	@DisplayName("equals() / hashCode()")
	class EqualsHashCodeTests {

		@Test
		@DisplayName("two Keystrokes with equal arrays are equal and share the same hash code")
		void equalKeystrokesHaveSameHashCode() {
			final var keyCode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			final var a = createKeystroke(new Scancode[] { keyCode }, new Scancode[0]);
			final var b = createKeystroke(new Scancode[] { keyCode }, new Scancode[0]);

			Assertions.assertEquals(a, b);
			Assertions.assertEquals(a.hashCode(), b.hashCode());
		}

		@Test
		@DisplayName("a Keystroke is not equal to a non-Keystroke object")
		void keystrokeIsNotEqualToNonKeystrokeObject() {
			// noinspection AssertBetweenInconvertibleTypes
			Assertions.assertNotEquals("not a keystroke", createEmptyKeystroke());
		}

		@Test
		@DisplayName("Keystrokes with different keyCodes are not equal")
		void keystrokesWithDifferentKeyCodesAreNotEqual() {
			final var a = createKeystroke(new Scancode[] { Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A) },
					new Scancode[0]);
			final var b = createKeystroke(new Scancode[] { Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_B) },
					new Scancode[0]);

			Assertions.assertNotEquals(a, b);
		}

		@Test
		@DisplayName("Keystrokes with different modifierCodes are not equal")
		void keystrokesWithDifferentModifierCodesAreNotEqual() {
			final var keyCode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			final var a = createKeystroke(new Scancode[] { keyCode },
					new Scancode[] { Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_LSHIFT) });
			final var b = createKeystroke(new Scancode[] { keyCode }, new Scancode[0]);

			Assertions.assertNotEquals(a, b);
		}
	}

	@Nested
	@DisplayName("toString()")
	class ToStringTests {

		@Test
		@DisplayName("joins modifier and key scancode names with ' + ' when both are present")
		void joinsModifierAndKeyNamesWithPlus() {
			final var modifier = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_LSHIFT);
			final var key = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			final var keystroke = createKeystroke(new Scancode[] { key }, new Scancode[] { modifier });

			Assertions.assertEquals(Scancode.DIK_LSHIFT + " + " + Scancode.DIK_A, keystroke.toString());
		}

		@Test
		@DisplayName("returns the key name alone when no modifier codes are present")
		void returnsKeyNameAloneWithNoModifiers() {
			final var key = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_RETURN);
			final var keystroke = createKeystroke(new Scancode[] { key }, new Scancode[0]);

			Assertions.assertEquals(Scancode.DIK_RETURN, keystroke.toString());
		}

		@Test
		@DisplayName("returns the NOTHING resource string when both keyCodes and modifierCodes are empty")
		void returnsNothingStringWhenBothArraysAreEmpty() {
			Assertions.assertEquals(Main.STRINGS.getString("NOTHING"), createEmptyKeystroke().toString());
		}
	}
}

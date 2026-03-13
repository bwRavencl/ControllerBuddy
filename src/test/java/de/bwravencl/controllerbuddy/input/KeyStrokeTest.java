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

class KeyStrokeTest {

	private static KeyStroke createEmptyKeyStroke() {
		return new KeyStroke();
	}

	private static KeyStroke createKeyStroke(final ScanCode[] keyCodes, final ScanCode[] modifierCodes) {
		return new KeyStroke(keyCodes, modifierCodes);
	}

	@Nested
	@DisplayName("clone()")
	class CloneTests {

		@Test
		@DisplayName("keyCodes array in the clone is a distinct copy")
		void clonesKeyCodesArrayAsDistinctCopy() throws CloneNotSupportedException {
			final var keyCode = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A);
			final var original = createKeyStroke(new ScanCode[] { keyCode }, new ScanCode[0]);

			final var clone = (KeyStroke) original.clone();

			Assertions.assertNotSame(original.getKeyCodes(), clone.getKeyCodes());
			Assertions.assertArrayEquals(original.getKeyCodes(), clone.getKeyCodes());
		}

		@Test
		@DisplayName("modifierCodes array in the clone is a distinct copy")
		void clonesModifierCodesArrayAsDistinctCopy() throws CloneNotSupportedException {
			final var modifier = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_LSHIFT);
			final var original = createKeyStroke(new ScanCode[0], new ScanCode[] { modifier });

			final var clone = (KeyStroke) original.clone();

			Assertions.assertNotSame(original.getModifierCodes(), clone.getModifierCodes());
			Assertions.assertArrayEquals(original.getModifierCodes(), clone.getModifierCodes());
		}

		@Test
		@DisplayName("returns a KeyStroke that is not the same instance as the original")
		void returnsDistinctInstance() throws CloneNotSupportedException {
			final var original = createEmptyKeyStroke();
			Assertions.assertNotSame(original, original.clone());
		}
	}

	@Nested
	@DisplayName("equals() / hashCode()")
	class EqualsHashCodeTests {

		@Test
		@DisplayName("two KeyStrokes with equal arrays are equal and share the same hash code")
		void equalKeyStrokesHaveSameHashCode() {
			final var keyCode = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A);
			final var a = createKeyStroke(new ScanCode[] { keyCode }, new ScanCode[0]);
			final var b = createKeyStroke(new ScanCode[] { keyCode }, new ScanCode[0]);

			Assertions.assertEquals(a, b);
			Assertions.assertEquals(a.hashCode(), b.hashCode());
		}

		@Test
		@DisplayName("a KeyStroke is not equal to a non-KeyStroke object")
		void keyStrokeIsNotEqualToNonKeyStrokeObject() {
			// noinspection AssertBetweenInconvertibleTypes
			Assertions.assertNotEquals("not a keystroke", createEmptyKeyStroke());
		}

		@Test
		@DisplayName("KeyStrokes with different keyCodes are not equal")
		void keyStrokesWithDifferentKeyCodesAreNotEqual() {
			final var a = createKeyStroke(new ScanCode[] { ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A) },
					new ScanCode[0]);
			final var b = createKeyStroke(new ScanCode[] { ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_B) },
					new ScanCode[0]);

			Assertions.assertNotEquals(a, b);
		}

		@Test
		@DisplayName("KeyStrokes with different modifierCodes are not equal")
		void keyStrokesWithDifferentModifierCodesAreNotEqual() {
			final var keyCode = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A);
			final var a = createKeyStroke(new ScanCode[] { keyCode },
					new ScanCode[] { ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_LSHIFT) });
			final var b = createKeyStroke(new ScanCode[] { keyCode }, new ScanCode[0]);

			Assertions.assertNotEquals(a, b);
		}
	}

	@Nested
	@DisplayName("toString()")
	class ToStringTests {

		@Test
		@DisplayName("joins modifier and key scan code names with ' + ' when both are present")
		void joinsModifierAndKeyNamesWithPlus() {
			final var modifier = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_LSHIFT);
			final var key = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A);
			final var keyStroke = createKeyStroke(new ScanCode[] { key }, new ScanCode[] { modifier });

			Assertions.assertEquals(ScanCode.DIK_LSHIFT + " + " + ScanCode.DIK_A, keyStroke.toString());
		}

		@Test
		@DisplayName("returns the key name alone when no modifier codes are present")
		void returnsKeyNameAloneWithNoModifiers() {
			final var key = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_RETURN);
			final var keyStroke = createKeyStroke(new ScanCode[] { key }, new ScanCode[0]);

			Assertions.assertEquals(ScanCode.DIK_RETURN, keyStroke.toString());
		}

		@Test
		@DisplayName("returns the NOTHING resource string when both keyCodes and modifierCodes are empty")
		void returnsNothingStringWhenBothArraysAreEmpty() {
			Assertions.assertEquals(Main.STRINGS.getString("NOTHING"), createEmptyKeyStroke().toString());
		}
	}
}

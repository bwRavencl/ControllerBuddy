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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScancodeTest {

	@Nested
	@DisplayName("EXTENDED_KEY_SCAN_CODES_SET")
	class ExtendedKeyScancodesSetTests {

		@Test
		@DisplayName("contains the key codes of all extended keys")
		void containsKeyCodesOfExtendedKeys() {
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_RCONTROL).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_RMENU).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_INSERT).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_DELETE).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_HOME).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_END).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_PRIOR).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_NEXT).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_UP).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_DOWN).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_LEFT).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_RIGHT).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_SYSRQ).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_DIVIDE).keyCode()));
			Assertions.assertTrue(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_NUMPADENTER).keyCode()));
		}

		@Test
		@DisplayName("does not contain the key codes of non-extended keys")
		void doesNotContainKeyCodesOfNonExtendedKeys() {
			Assertions.assertFalse(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A).keyCode()));
			Assertions.assertFalse(Scancode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_ESCAPE).keyCode()));
		}

		@Test
		@DisplayName("is unmodifiable")
		void isUnmodifiable() {
			Assertions.assertThrows(UnsupportedOperationException.class,
					() -> Scancode.EXTENDED_KEY_SCAN_CODES_SET.add(0));
		}
	}

	@Nested
	@DisplayName("KEY_CODE_TO_SCAN_CODE_MAP")
	class KeyCodeToScancodeMapTests {

		@Test
		@DisplayName("is unmodifiable")
		void isUnmodifiable() {
			Assertions.assertThrows(UnsupportedOperationException.class, () -> Scancode.KEY_CODE_TO_SCAN_CODE_MAP.put(0,
					Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A)));
		}

		@Test
		@DisplayName("maps key codes to the correct Scancode instances")
		void mapsKeyCodesToCorrectScancodeInstances() {
			final var escScancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_ESCAPE);
			Assertions.assertSame(escScancode, Scancode.KEY_CODE_TO_SCAN_CODE_MAP.get(escScancode.keyCode()));

			final var aScancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			Assertions.assertSame(aScancode, Scancode.KEY_CODE_TO_SCAN_CODE_MAP.get(aScancode.keyCode()));
		}
	}

	@Nested
	@DisplayName("NAME_TO_SCAN_CODE_MAP")
	class NameToScancodeMapTests {

		@Test
		@DisplayName("is unmodifiable")
		void isUnmodifiable() {
			Assertions.assertThrows(UnsupportedOperationException.class,
					() -> Scancode.NAME_TO_SCAN_CODE_MAP.put("X", Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A)));
		}

		@Test
		@DisplayName("maps each DIK name constant to a Scancode with that name")
		void mapsDikNamesToCorrectScancodes() {
			Assertions.assertEquals(Scancode.DIK_A, Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A).name());
			Assertions.assertEquals(Scancode.DIK_ESCAPE,
					Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_ESCAPE).name());
			Assertions.assertEquals(Scancode.DIK_RETURN,
					Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_RETURN).name());
		}
	}

	@Nested
	@DisplayName("toString()")
	class ToStringTests {

		@Test
		@DisplayName("returns the name field of the Scancode")
		void returnsNameField() {
			final var scancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			Assertions.assertEquals(Scancode.DIK_A, scancode.toString());
		}
	}
}

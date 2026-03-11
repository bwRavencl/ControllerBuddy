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

package de.bwravencl.controllerbuddy.input;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScanCodeTest {

	@Nested
	@DisplayName("EXTENDED_KEY_SCAN_CODES_SET")
	class ExtendedKeyScanCodesSetTests {

		@Test
		@DisplayName("contains the key codes of all extended keys")
		void containsKeyCodesOfExtendedKeys() {
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_RCONTROL).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_RMENU).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_INSERT).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_DELETE).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_HOME).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_END).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_PRIOR).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_NEXT).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_UP).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_DOWN).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_LEFT).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_RIGHT).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_SYSRQ).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_DIVIDE).keyCode()));
			Assertions.assertTrue(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_NUMPADENTER).keyCode()));
		}

		@Test
		@DisplayName("does not contain the key codes of non-extended keys")
		void doesNotContainKeyCodesOfNonExtendedKeys() {
			Assertions.assertFalse(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A).keyCode()));
			Assertions.assertFalse(ScanCode.EXTENDED_KEY_SCAN_CODES_SET
					.contains(ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_ESCAPE).keyCode()));
		}

		@Test
		@DisplayName("is unmodifiable")
		void isUnmodifiable() {
			Assertions.assertThrows(UnsupportedOperationException.class,
					() -> ScanCode.EXTENDED_KEY_SCAN_CODES_SET.add(0));
		}
	}

	@Nested
	@DisplayName("KEY_CODE_TO_SCAN_CODE_MAP")
	class KeyCodeToScanCodeMapTests {

		@Test
		@DisplayName("is unmodifiable")
		void isUnmodifiable() {
			Assertions.assertThrows(UnsupportedOperationException.class, () -> ScanCode.KEY_CODE_TO_SCAN_CODE_MAP.put(0,
					ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A)));
		}

		@Test
		@DisplayName("maps key codes to the correct ScanCode instances")
		void mapsKeyCodesToCorrectScanCodeInstances() {
			final var escScanCode = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_ESCAPE);
			Assertions.assertSame(escScanCode, ScanCode.KEY_CODE_TO_SCAN_CODE_MAP.get(escScanCode.keyCode()));

			final var aScanCode = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A);
			Assertions.assertSame(aScanCode, ScanCode.KEY_CODE_TO_SCAN_CODE_MAP.get(aScanCode.keyCode()));
		}
	}

	@Nested
	@DisplayName("NAME_TO_SCAN_CODE_MAP")
	class NameToScanCodeMapTests {

		@Test
		@DisplayName("is unmodifiable")
		void isUnmodifiable() {
			Assertions.assertThrows(UnsupportedOperationException.class,
					() -> ScanCode.NAME_TO_SCAN_CODE_MAP.put("X", ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A)));
		}

		@Test
		@DisplayName("maps each DIK name constant to a ScanCode with that name")
		void mapsDikNamesToCorrectScanCodes() {
			Assertions.assertEquals(ScanCode.DIK_A, ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A).name());
			Assertions.assertEquals(ScanCode.DIK_ESCAPE,
					ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_ESCAPE).name());
			Assertions.assertEquals(ScanCode.DIK_RETURN,
					ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_RETURN).name());
		}
	}

	@Nested
	@DisplayName("toString()")
	class ToStringTests {

		@Test
		@DisplayName("returns the name field of the ScanCode")
		void returnsNameField() {
			final var scanCode = ScanCode.NAME_TO_SCAN_CODE_MAP.get(ScanCode.DIK_A);
			Assertions.assertEquals(ScanCode.DIK_A, scanCode.toString());
		}
	}
}

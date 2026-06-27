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

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NullMarked
final class ScancodeTest {

	@Nested
	@DisplayName("EXTENDED_KEY_SCAN_CODES_SET")
	final class ExtendedKeyScancodesSetTests {

		private static void assertIsExtended(final String scancodeName) {
			Assertions.assertTrue(isExtended(scancodeName));
		}

		private static void assertIsNotExtended(final String scancodeName) {
			Assertions.assertFalse(isExtended(scancodeName));
		}

		private static boolean isExtended(final String scancodeName) {
			final var scanCode = Scancode.NAME_TO_SCAN_CODE_MAP.get(scancodeName);

			Assertions.assertNotNull(scanCode);
			return Scancode.EXTENDED_KEY_SCAN_CODES_SET.contains(scanCode.keyCode());
		}

		@Test
		@DisplayName("contains the key codes of all extended keys")
		void containsKeyCodesOfExtendedKeys() {
			assertIsExtended(Scancode.DIK_RCONTROL);
			assertIsExtended(Scancode.DIK_RMENU);
			assertIsExtended(Scancode.DIK_INSERT);
			assertIsExtended(Scancode.DIK_DELETE);
			assertIsExtended(Scancode.DIK_HOME);
			assertIsExtended(Scancode.DIK_END);
			assertIsExtended(Scancode.DIK_PRIOR);
			assertIsExtended(Scancode.DIK_NEXT);
			assertIsExtended(Scancode.DIK_UP);
			assertIsExtended(Scancode.DIK_DOWN);
			assertIsExtended(Scancode.DIK_LEFT);
			assertIsExtended(Scancode.DIK_RIGHT);
			assertIsExtended(Scancode.DIK_SYSRQ);
			assertIsExtended(Scancode.DIK_DIVIDE);
			assertIsExtended(Scancode.DIK_NUMPADENTER);
		}

		@Test
		@DisplayName("does not contain the key codes of non-extended keys")
		void doesNotContainKeyCodesOfNonExtendedKeys() {
			assertIsNotExtended(Scancode.DIK_A);
			assertIsNotExtended(Scancode.DIK_ESCAPE);
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
	final class KeyCodeToScancodeMapTests {

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
			Assertions.assertNotNull(escScancode);
			Assertions.assertSame(escScancode, Scancode.KEY_CODE_TO_SCAN_CODE_MAP.get(escScancode.keyCode()));

			final var aScancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			Assertions.assertNotNull(aScancode);
			Assertions.assertSame(aScancode, Scancode.KEY_CODE_TO_SCAN_CODE_MAP.get(aScancode.keyCode()));
		}
	}

	@Nested
	@DisplayName("NAME_TO_SCAN_CODE_MAP")
	final class NameToScancodeMapTests {

		@Test
		@DisplayName("is unmodifiable")
		void isUnmodifiable() {
			Assertions.assertThrows(UnsupportedOperationException.class,
					() -> Scancode.NAME_TO_SCAN_CODE_MAP.put("X", Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A)));
		}

		@Test
		@DisplayName("maps each DIK name constant to a Scancode with that name")
		void mapsDikNamesToCorrectScancodes() {
			final var aScancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			Assertions.assertNotNull(aScancode);
			Assertions.assertEquals(Scancode.DIK_A, aScancode.name());

			final var escapeScancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_ESCAPE);
			Assertions.assertNotNull(escapeScancode);
			Assertions.assertEquals(Scancode.DIK_ESCAPE, escapeScancode.name());

			final var returnScancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_RETURN);
			Assertions.assertNotNull(returnScancode);
			Assertions.assertEquals(Scancode.DIK_RETURN, returnScancode.name());
		}
	}

	@Nested
	@DisplayName("toString()")
	final class ToStringTests {

		@Test
		@DisplayName("returns the name field of the Scancode")
		void returnsNameField() {
			final var scancode = Scancode.NAME_TO_SCAN_CODE_MAP.get(Scancode.DIK_A);
			Assertions.assertNotNull(scancode);
			Assertions.assertEquals(Scancode.DIK_A, scancode.toString());
		}
	}
}

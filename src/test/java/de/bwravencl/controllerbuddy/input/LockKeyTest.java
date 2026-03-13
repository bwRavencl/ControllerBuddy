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

import java.awt.event.KeyEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LockKeyTest {

	@Nested
	@DisplayName("LOCK_KEYS")
	class LockKeysTests {

		@Test
		@DisplayName("contains CAPS_LOCK_LOCK_KEY, NUM_LOCK_LOCK_KEY, and SCROLL_LOCK_LOCK_KEY")
		void containsAllThreeLockKeyInstances() {
			Assertions.assertTrue(LockKey.LOCK_KEYS.contains(LockKey.CAPS_LOCK_LOCK_KEY));
			Assertions.assertTrue(LockKey.LOCK_KEYS.contains(LockKey.NUM_LOCK_LOCK_KEY));
			Assertions.assertTrue(LockKey.LOCK_KEYS.contains(LockKey.SCROLL_LOCK_LOCK_KEY));
		}

		@Test
		@DisplayName("contains exactly three entries")
		void containsExactlyThreeEntries() {
			Assertions.assertEquals(3, LockKey.LOCK_KEYS.size());
		}
	}

	@Nested
	@DisplayName("NAME_TO_LOCK_KEY_MAP")
	class NameToLockKeyMapTests {

		@Test
		@DisplayName("is unmodifiable")
		void isUnmodifiable() {
			Assertions.assertThrows(UnsupportedOperationException.class,
					() -> LockKey.NAME_TO_LOCK_KEY_MAP.put("X", LockKey.CAPS_LOCK_LOCK_KEY));
		}

		@Test
		@DisplayName("maps each lock key name to the correct LockKey instance")
		void mapsEachNameToCorrectLockKeyInstance() {
			Assertions.assertSame(LockKey.CAPS_LOCK_LOCK_KEY, LockKey.NAME_TO_LOCK_KEY_MAP.get(LockKey.CAPS_LOCK));
			Assertions.assertSame(LockKey.NUM_LOCK_LOCK_KEY, LockKey.NAME_TO_LOCK_KEY_MAP.get(LockKey.NUM_LOCK));
		}
	}

	@Nested
	@DisplayName("toString()")
	class ToStringTests {

		@Test
		@DisplayName("returns the name field of the LockKey")
		void returnsNameField() {
			Assertions.assertEquals(LockKey.CAPS_LOCK, LockKey.CAPS_LOCK_LOCK_KEY.toString());
			Assertions.assertEquals(LockKey.NUM_LOCK, LockKey.NUM_LOCK_LOCK_KEY.toString());
		}
	}

	@Nested
	@DisplayName("VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP")
	class VirtualKeyCodeToLockKeyMapTests {

		@Test
		@DisplayName("is unmodifiable")
		void isUnmodifiable() {
			Assertions.assertThrows(UnsupportedOperationException.class,
					() -> LockKey.VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP.put(0, LockKey.CAPS_LOCK_LOCK_KEY));
		}

		@Test
		@DisplayName("maps each virtual key code to the correct LockKey instance")
		void mapsEachVirtualKeyCodeToCorrectLockKeyInstance() {
			Assertions.assertSame(LockKey.CAPS_LOCK_LOCK_KEY,
					LockKey.VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP.get(KeyEvent.VK_CAPS_LOCK));
			Assertions.assertSame(LockKey.NUM_LOCK_LOCK_KEY,
					LockKey.VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP.get(KeyEvent.VK_NUM_LOCK));
			Assertions.assertSame(LockKey.SCROLL_LOCK_LOCK_KEY,
					LockKey.VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP.get(KeyEvent.VK_SCROLL_LOCK));
		}
	}
}

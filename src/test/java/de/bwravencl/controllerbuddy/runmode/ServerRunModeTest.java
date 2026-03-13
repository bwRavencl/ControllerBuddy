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

package de.bwravencl.controllerbuddy.runmode;

import de.bwravencl.controllerbuddy.gui.Main;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerRunModeTest {

	@Mock
	Main mockMain;

	@Nested
	@DisplayName("MessageType.fromId()")
	class MessageTypeFromIdTests {

		@Test
		@DisplayName("returns the correct enum constant for each known message type ID")
		void returnsCorrectEnumForKnownIds() {
			Assertions.assertEquals(ServerRunMode.MessageType.CLIENT_HELLO,
					ServerRunMode.MessageType.fromId(ServerRunMode.MessageType.CLIENT_HELLO.getId()));
			Assertions.assertEquals(ServerRunMode.MessageType.SERVER_HELLO,
					ServerRunMode.MessageType.fromId(ServerRunMode.MessageType.SERVER_HELLO.getId()));
			Assertions.assertEquals(ServerRunMode.MessageType.UPDATE,
					ServerRunMode.MessageType.fromId(ServerRunMode.MessageType.UPDATE.getId()));
			Assertions.assertEquals(ServerRunMode.MessageType.REQUEST_ALIVE,
					ServerRunMode.MessageType.fromId(ServerRunMode.MessageType.REQUEST_ALIVE.getId()));
			Assertions.assertEquals(ServerRunMode.MessageType.CLIENT_ALIVE,
					ServerRunMode.MessageType.fromId(ServerRunMode.MessageType.CLIENT_ALIVE.getId()));
		}

		@Test
		@DisplayName("returns null for an unknown message type ID")
		void returnsNullForUnknownId() {
			Assertions.assertNull(ServerRunMode.MessageType.fromId(-1));
		}
	}

	@Nested
	@DisplayName("deriveKey()")
	class DeriveKeyTests {

		@Test
		@DisplayName("returns a non-null AES key for valid inputs")
		void returnsNonNullAesKey() {
			Mockito.when(mockMain.getPassword()).thenReturn("test-password");
			final var salt = new byte[ServerRunMode.SALT_LENGTH];
			final var key = ServerRunMode.deriveKey(mockMain, salt);
			Assertions.assertNotNull(key);
			Assertions.assertEquals("AES", key.getAlgorithm());
		}

		@Test
		@DisplayName("produces different keys for different salts")
		void producesDifferentKeysForDifferentSalts() {
			Mockito.when(mockMain.getPassword()).thenReturn("test-password");
			final var salt1 = new byte[ServerRunMode.SALT_LENGTH];
			final var salt2 = new byte[ServerRunMode.SALT_LENGTH];
			salt2[0] = 1;
			final var key1 = ServerRunMode.deriveKey(mockMain, salt1);
			final var key2 = ServerRunMode.deriveKey(mockMain, salt2);
			Assertions.assertFalse(java.util.Arrays.equals(key1.getEncoded(), key2.getEncoded()));
		}

		@Test
		@DisplayName("produces different keys for different passwords")
		void producesDifferentKeysForDifferentPasswords() {
			final var salt = new byte[ServerRunMode.SALT_LENGTH];
			Mockito.when(mockMain.getPassword()).thenReturn("password-one");
			final var key1 = ServerRunMode.deriveKey(mockMain, salt);
			Mockito.when(mockMain.getPassword()).thenReturn("password-two");
			final var key2 = ServerRunMode.deriveKey(mockMain, salt);
			Assertions.assertFalse(java.util.Arrays.equals(key1.getEncoded(), key2.getEncoded()));
		}

		@Test
		@DisplayName("produces the same key for identical password and salt")
		void producesDeterministicKey() {
			Mockito.when(mockMain.getPassword()).thenReturn("test-password");
			final var salt = new byte[ServerRunMode.SALT_LENGTH];
			final var key1 = ServerRunMode.deriveKey(mockMain, salt);
			final var key2 = ServerRunMode.deriveKey(mockMain, salt);
			Assertions.assertArrayEquals(key1.getEncoded(), key2.getEncoded());
		}
	}
}

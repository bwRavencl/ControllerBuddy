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

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.MainFrame;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.ButtonToButtonAction;
import de.bwravencl.controllerbuddy.util.MainLoop;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@NullMarked
@ExtendWith(MockitoExtension.class)
final class ServerRunModeTest {

	@Mock
	MainFrame mockMainFrame;

	@Nested
	@DisplayName("deriveKey()")
	final class DeriveKeyTests {

		@Test
		@DisplayName("produces the same key for identical password and salt")
		void producesDeterministicKey() {
			Mockito.when(mockMainFrame.getPassword()).thenReturn("test-password");
			final var salt = new byte[ServerRunMode.SALT_LENGTH];
			final var key1 = ServerRunMode.deriveKey(mockMainFrame, salt);
			final var key2 = ServerRunMode.deriveKey(mockMainFrame, salt);
			Assertions.assertArrayEquals(key1.getEncoded(), key2.getEncoded());
		}

		@Test
		@DisplayName("produces different keys for different passwords")
		void producesDifferentKeysForDifferentPasswords() {
			final var salt = new byte[ServerRunMode.SALT_LENGTH];
			Mockito.when(mockMainFrame.getPassword()).thenReturn("password-one");
			final var key1 = ServerRunMode.deriveKey(mockMainFrame, salt);
			Mockito.when(mockMainFrame.getPassword()).thenReturn("password-two");
			final var key2 = ServerRunMode.deriveKey(mockMainFrame, salt);
			Assertions.assertFalse(Arrays.equals(key1.getEncoded(), key2.getEncoded()));
		}

		@Test
		@DisplayName("produces different keys for different salts")
		void producesDifferentKeysForDifferentSalts() {
			Mockito.when(mockMainFrame.getPassword()).thenReturn("test-password");
			final var salt1 = new byte[ServerRunMode.SALT_LENGTH];
			final var salt2 = new byte[ServerRunMode.SALT_LENGTH];
			salt2[0] = 1;
			final var key1 = ServerRunMode.deriveKey(mockMainFrame, salt1);
			final var key2 = ServerRunMode.deriveKey(mockMainFrame, salt2);
			Assertions.assertFalse(Arrays.equals(key1.getEncoded(), key2.getEncoded()));
		}

		@Test
		@DisplayName("returns a non-null AES key for valid inputs")
		void returnsNonNullAesKey() {
			Mockito.when(mockMainFrame.getPassword()).thenReturn("test-password");
			final var salt = new byte[ServerRunMode.SALT_LENGTH];
			final var key = ServerRunMode.deriveKey(mockMainFrame, salt);
			Assertions.assertNotNull(key);
			Assertions.assertEquals("AES", key.getAlgorithm());
		}
	}

	@Nested
	@DisplayName("client hello handling")
	final class HelloHandlingTests {

		private static final long TIMEOUT_MILLIS = 10_000L;

		@Mock
		Input mockInput;

		@Mock
		MainLoop mockMainLoop;

		@Mock
		Profile mockProfile;

		private boolean previousSkipMessageDialogs;

		private static int findFreePort() throws IOException {
			try (final var socket = new DatagramSocket(0)) {
				return socket.getLocalPort();
			}
		}

		private static byte[] createHello(final int numButtons) throws IOException {
			try (final var byteArrayOutputStream = new ByteArrayOutputStream();
					final var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
				dataOutputStream.writeInt(ServerRunMode.MessageType.CLIENT_HELLO.getId());
				dataOutputStream.write(new byte[ServerRunMode.SALT_LENGTH]);
				dataOutputStream.writeInt(Short.MIN_VALUE);
				dataOutputStream.writeInt(Short.MAX_VALUE);
				dataOutputStream.writeInt(numButtons);
				dataOutputStream.flush();

				return byteArrayOutputStream.toByteArray();
			}
		}

		private void runServerAndSendHello(final int numButtons) throws Exception {
			final var port = findFreePort();
			Mockito.when(mockMainFrame.getPort()).thenReturn(port);

			final var serverRunMode = new ServerRunMode(mockMainFrame);
			final var serverThread = new Thread(serverRunMode);
			serverThread.start();

			final var hello = createHello(numButtons);
			try (final var clientSocket = new DatagramSocket()) {
				final var deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
				while (serverThread.isAlive() && System.currentTimeMillis() < deadline) {
					clientSocket.send(new DatagramPacket(hello, hello.length, InetAddress.getLoopbackAddress(), port));
					serverThread.join(200L);
				}
			} finally {
				serverRunMode.requestStop();
				serverThread.interrupt();
				serverThread.join(TIMEOUT_MILLIS);
			}

			Assertions.assertFalse(serverThread.isAlive());
		}

		@BeforeEach
		void setUp() {
			previousSkipMessageDialogs = GuiUtils.skipMessageDialogs;
			GuiUtils.skipMessageDialogs = true;

			final var action = new ButtonToButtonAction();
			action.setButtonId(40);
			final var mode = new Mode();
			mode.getButtonToActionsMap().put(0, List.of(action));

			Mockito.when(mockMainFrame.getInput()).thenReturn(mockInput);
			Mockito.when(mockMainFrame.getMinPollingRate()).thenReturn(RunMode.DEFAULT_MIN_POLLING_RATE_HZ);
			Mockito.when(mockMainFrame.getMaxPollingRate()).thenReturn(RunMode.DEFAULT_MAX_POLLING_RATE_HZ);
			Mockito.when(mockMainFrame.getTimeout()).thenReturn(ServerRunMode.DEFAULT_TIMEOUT);
			Mockito.when(mockMainFrame.getRandom()).thenReturn(new Random(0L));
			Mockito.when(mockMainFrame.getMainLoop()).thenReturn(mockMainLoop);
			Mockito.when(mockInput.getProfile()).thenReturn(mockProfile);
			Mockito.when(mockProfile.getModes()).thenReturn(List.of(mode));
		}

		@Test
		@DisplayName("proceeds to initialize the input when the client has enough buttons for the profile")
		void proceedsWhenClientHasEnoughButtons() throws Exception {
			Mockito.when(mockMainFrame.getPassword()).thenReturn("test-password");

			runServerAndSendHello(41);

			Mockito.verify(mockInput).init();
		}

		@Test
		@DisplayName("stops without initializing the input when the client has fewer buttons than the profile requires")
		void stopsWhenClientHasTooFewButtons() throws Exception {
			runServerAndSendHello(40);

			Mockito.verify(mockInput, Mockito.never()).init();
			Mockito.verify(mockInput, Mockito.never()).initButtons();
		}

		@AfterEach
		void tearDown() {
			GuiUtils.skipMessageDialogs = previousSkipMessageDialogs;
		}
	}

	@Nested
	@DisplayName("MessageType.fromId()")
	final class MessageTypeFromIdTests {

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
}

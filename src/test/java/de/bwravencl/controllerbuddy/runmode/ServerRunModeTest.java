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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
	@DisplayName("client connection")
	final class ConnectionTests {

		private static final int REQUIRED_NUM_BUTTONS = 41;

		private static final long TIMEOUT_MILLIS = 10_000L;

		@Mock
		Input mockInput;

		@Mock
		MainLoop mockMainLoop;

		@Mock
		Profile mockProfile;

		private int port;

		private boolean previousSkipMessageDialogs;

		private @Nullable ServerRunMode serverRunMode;

		private @Nullable Thread serverThread;

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

		private static int findFreePort() throws IOException {
			try (final var socket = new DatagramSocket(0)) {
				return socket.getLocalPort();
			}
		}

		private byte[] decrypt(final DatagramPacket packet) throws GeneralSecurityException {
			final var packetByteBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
			final var iv = new byte[ServerRunMode.IV_LENGTH];
			packetByteBuffer.get(iv);

			final var cipher = Cipher.getInstance(ServerRunMode.CIPHER_TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE,
					ServerRunMode.deriveKey(mockMainFrame, new byte[ServerRunMode.SALT_LENGTH]),
					new GCMParameterSpec(ServerRunMode.TAG_LENGTH, iv));

			final var encryptedBytes = new byte[packetByteBuffer.remaining()];
			packetByteBuffer.get(encryptedBytes);

			return cipher.doFinal(encryptedBytes);
		}

		@Test
		@DisplayName("proceeds to initialize the input when the client has enough buttons for the profile")
		void proceedsWhenClientHasEnoughButtons() throws Exception {
			sendHelloUntilServerStops(REQUIRED_NUM_BUTTONS);

			Mockito.verify(mockInput).init();
		}

		@SuppressWarnings("unchecked")
		private List<Update> receiveUpdates(final int numUpdates) throws Exception {
			final var hello = createHello(REQUIRED_NUM_BUTTONS);
			final var updates = new ArrayList<Update>();
			try (final var clientSocket = new DatagramSocket()) {
				clientSocket.setSoTimeout(200);
				final var deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
				var connected = false;
				while (updates.size() < numUpdates && System.currentTimeMillis() < deadline) {
					if (!connected) {
						clientSocket
								.send(new DatagramPacket(hello, hello.length, InetAddress.getLoopbackAddress(), port));
					}

					final var receiveBuf = new byte[65_507];
					final var packet = new DatagramPacket(receiveBuf, receiveBuf.length);
					try {
						clientSocket.receive(packet);
					} catch (SocketTimeoutException _) {
						continue;
					}

					try (final var dataInputStream = new DataInputStream(new ByteArrayInputStream(decrypt(packet)))) {
						final var messageType = dataInputStream.readInt();
						if (messageType == ServerRunMode.MessageType.SERVER_HELLO.getId()) {
							connected = true;
						} else if (messageType == ServerRunMode.MessageType.UPDATE.getId()) {
							dataInputStream.readLong();
							try (final var objectInputStream = new ObjectInputStream(dataInputStream)) {
								objectInputStream.readObject();
								final var buttons = (boolean[]) objectInputStream.readObject();
								objectInputStream.readInt();
								objectInputStream.readInt();
								final var downMouseButtons = (Set<Integer>) objectInputStream.readObject();
								updates.add(new Update(buttons[REQUIRED_NUM_BUTTONS - 1], downMouseButtons));
							}
						}
					}
				}
			}

			return updates;
		}

		@Test
		@DisplayName("rejects the client and keeps listening when the client has fewer buttons than the profile requires")
		void rejectsClientWithTooFewButtons() throws Exception {
			Objects.requireNonNull(serverThread, "Field serverThread must not be null");

			final var hello = createHello(REQUIRED_NUM_BUTTONS - 1);
			final var receiveBuf = new byte[1024];
			final var replyPacket = new DatagramPacket(receiveBuf, receiveBuf.length);
			try (final var clientSocket = new DatagramSocket()) {
				clientSocket.setSoTimeout(200);
				final var deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
				var replied = false;
				while (!replied && System.currentTimeMillis() < deadline) {
					clientSocket.send(new DatagramPacket(hello, hello.length, InetAddress.getLoopbackAddress(), port));
					try {
						clientSocket.receive(replyPacket);
						replied = true;
					} catch (SocketTimeoutException _) {
						// the server might not be listening yet
					}
				}
				Assertions.assertTrue(replied);
			}

			try (final var dataInputStream = new DataInputStream(new ByteArrayInputStream(decrypt(replyPacket)))) {
				Assertions.assertEquals(ServerRunMode.MessageType.TOO_FEW_BUTTONS.getId(), dataInputStream.readInt());
				Assertions.assertEquals(REQUIRED_NUM_BUTTONS, dataInputStream.readInt());
			}

			Assertions.assertTrue(serverThread.isAlive());
			Mockito.verify(mockInput, Mockito.never()).init();
			Mockito.verify(mockInput, Mockito.never()).initButtons();
		}

		@Test
		@DisplayName("resends the unchanged input state so that the client recovers from a lost packet")
		void resendsUnchangedState() throws Exception {
			final var buttons = new boolean[REQUIRED_NUM_BUTTONS];
			final var pollCount = new AtomicInteger();
			Mockito.when(mockInput.init()).thenReturn(true);
			Mockito.when(mockInput.getButtons()).thenReturn(buttons);
			Mockito.when(mockInput.poll()).thenAnswer(_ -> {
				if (pollCount.getAndIncrement() == 0) {
					buttons[REQUIRED_NUM_BUTTONS - 1] = true;
				}
				return true;
			});

			final var updates = receiveUpdates(3);

			Assertions.assertEquals(List.of(true, false, false), updates.stream().map(Update::buttonPressed).toList());
		}

		private void sendHelloUntilServerStops(final int numButtons) throws Exception {
			Objects.requireNonNull(serverThread, "Field serverThread must not be null");

			final var hello = createHello(numButtons);
			try (final var clientSocket = new DatagramSocket()) {
				final var deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
				while (serverThread.isAlive() && System.currentTimeMillis() < deadline) {
					clientSocket.send(new DatagramPacket(hello, hello.length, InetAddress.getLoopbackAddress(), port));
					serverThread.join(200L);
				}
			}
		}

		@Test
		@DisplayName("sends a change of the held mouse buttons that keeps their hash code unchanged")
		void sendsChangeOfDownMouseButtonsWithEqualHashCode() throws Exception {
			final var downMouseButtons = new HashSet<Integer>();
			final var pollCount = new AtomicInteger();
			Mockito.when(mockInput.init()).thenReturn(true);
			Mockito.when(mockInput.getButtons()).thenReturn(new boolean[REQUIRED_NUM_BUTTONS]);
			Mockito.when(mockInput.getDownMouseButtons()).thenReturn(downMouseButtons);
			Mockito.when(mockInput.poll()).thenAnswer(_ -> {
				downMouseButtons.clear();
				switch (pollCount.getAndIncrement()) {
				case 0 -> downMouseButtons.addAll(Set.of(1, 4));
				case 1 -> downMouseButtons.addAll(Set.of(2, 3));
				default -> {
				}
				}
				return true;
			});

			final var updates = receiveUpdates(2);

			Assertions.assertEquals(List.of(Set.of(1, 4), Set.of(2, 3)),
					updates.stream().map(Update::downMouseButtons).toList());
		}

		@Test
		@DisplayName("sends the release of a virtual button that was pressed during a single poll")
		void sendsReleaseOfButtonPressedDuringSinglePoll() throws Exception {
			final var buttons = new boolean[REQUIRED_NUM_BUTTONS];
			final var pollCount = new AtomicInteger();
			Mockito.when(mockInput.init()).thenReturn(true);
			Mockito.when(mockInput.getButtons()).thenReturn(buttons);
			Mockito.when(mockInput.poll()).thenAnswer(_ -> {
				if (pollCount.getAndIncrement() == 0) {
					buttons[REQUIRED_NUM_BUTTONS - 1] = true;
				}
				return true;
			});

			final var updates = receiveUpdates(2);

			Assertions.assertEquals(List.of(true, false), updates.stream().map(Update::buttonPressed).toList());
		}

		@BeforeEach
		void setUp() throws IOException {
			previousSkipMessageDialogs = GuiUtils.skipMessageDialogs;
			GuiUtils.skipMessageDialogs = true;

			final var action = new ButtonToButtonAction();
			action.setButtonId(REQUIRED_NUM_BUTTONS - 1);
			final var mode = new Mode();
			mode.getButtonToActionsMap().put(0, List.of(action));

			port = findFreePort();

			Mockito.when(mockMainFrame.getInput()).thenReturn(mockInput);
			Mockito.when(mockMainFrame.getMinPollingRate()).thenReturn(RunMode.DEFAULT_MIN_POLLING_RATE_HZ);
			Mockito.when(mockMainFrame.getMaxPollingRate()).thenReturn(RunMode.DEFAULT_MAX_POLLING_RATE_HZ);
			Mockito.when(mockMainFrame.getPort()).thenReturn(port);
			Mockito.when(mockMainFrame.getTimeout()).thenReturn(ServerRunMode.DEFAULT_TIMEOUT);
			Mockito.when(mockMainFrame.getRandom()).thenReturn(new Random(0L));
			Mockito.when(mockMainFrame.getMainLoop()).thenReturn(mockMainLoop);
			Mockito.when(mockMainFrame.getPassword()).thenReturn("test-password");
			Mockito.when(mockInput.getProfile()).thenReturn(mockProfile);
			Mockito.when(mockProfile.getModes()).thenReturn(List.of(mode));

			final var serverRunMode = new ServerRunMode(mockMainFrame);
			this.serverRunMode = serverRunMode;
			final var serverThread = new Thread(serverRunMode);
			this.serverThread = serverThread;
			serverThread.start();
		}

		@AfterEach
		void tearDown() throws InterruptedException {
			Objects.requireNonNull(serverRunMode, "Field serverRunMode must not be null");
			Objects.requireNonNull(serverThread, "Field serverThread must not be null");

			serverRunMode.requestStop();
			serverThread.interrupt();
			serverThread.join(TIMEOUT_MILLIS);
			Assertions.assertFalse(serverThread.isAlive());

			GuiUtils.skipMessageDialogs = previousSkipMessageDialogs;
		}

		private record Update(boolean buttonPressed, Set<Integer> downMouseButtons) {
		}
	}

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

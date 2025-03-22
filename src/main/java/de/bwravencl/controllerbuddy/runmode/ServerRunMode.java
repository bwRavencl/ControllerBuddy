/* Copyright (C) 2015  Matteo Hausner
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

package de.bwravencl.controllerbuddy.runmode;

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.LockKey;
import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;

public final class ServerRunMode extends RunMode implements Closeable {

	public static final int DEFAULT_PORT = 28789;

	public static final int DEFAULT_TIMEOUT = 100;

	static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

	static final int IV_LENGTH = 12;

	static final byte PROTOCOL_VERSION = 3;

	static final int SALT_LENGTH = 100;

	static final int TAG_LENGTH = 128;

	private static final Logger LOGGER = Logger.getLogger(ServerRunMode.class.getName());

	private static final int NUM_REQUEST_ALIVE_RETRIES = 10;

	private static final int REQUEST_ALIVE_INTERVAL = 100;

	private final Cipher cipher;

	private final byte[] iv = new byte[IV_LENGTH];

	private final int port;

	private final Random random;

	private final int timeout;

	private InetAddress clientAddress;

	private Key key;

	private DatagramSocket serverSocket;

	public ServerRunMode(final Main main, final Input input) {
		super(main, input);

		port = main.getPort();
		timeout = main.getTimeout();
		random = main.getRandom();

		try {
			cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	static Key deriveKey(final Main main, final byte[] saltBytes) {
		final var pbeKeySpec = new PBEKeySpec(main.getPassword().toCharArray(), saltBytes, 1000, 256);
		try {
			final var secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			final var secretKey = secretKeyFactory.generateSecret(pbeKeySpec);

			return new SecretKeySpec(secretKey.getEncoded(), "AES");
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		if (serverSocket != null) {
			serverSocket.close();
		}
	}

	@Override
	Logger getLogger() {
		return LOGGER;
	}

	@Override
	public void run() {
		logStart();

		final var clientPort = port + 1;
		var serverState = ServerState.LISTENING;
		DatagramPacket receivePacket;
		var counter = 0L;

		try {
			serverSocket = new DatagramSocket(port);
			final var receiveBuf = new byte[1024];

			EventQueue.invokeLater(() -> main
					.setStatusBarText(MessageFormat.format(Main.STRINGS.getString("STATUS_LISTENING"), port)));

			for (;;) {
				process();

				switch (serverState) {
				case LISTENING -> {
					counter = 0;
					receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
					serverSocket.setSoTimeout(100);
					for (;;) {
						try {
							serverSocket.receive(receivePacket);
							break;
						} catch (final SocketTimeoutException _) {
							// expected when waiting for a client
						}

						process();
					}
					clientAddress = receivePacket.getAddress();

					try (final var dataInputStream = new DataInputStream(
							new ByteArrayInputStream(receivePacket.getData()))) {
						final var messageType = dataInputStream.readInt();

						if (messageType == MessageType.CLIENT_HELLO.getId()) {
							final var salt = dataInputStream.readNBytes(SALT_LENGTH);
							key = deriveKey(main, salt);

							minAxisValue = dataInputStream.readInt();
							maxAxisValue = dataInputStream.readInt();
							setNumButtons(dataInputStream.readInt());

							try (final var byteArrayOutputStream = new ByteArrayOutputStream();
									final var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
								dataOutputStream.writeInt(MessageType.SERVER_HELLO.getId());
								dataOutputStream.writeByte(PROTOCOL_VERSION);
								dataOutputStream.writeLong(pollInterval);

								sendEncrypted(byteArrayOutputStream, clientPort);
							}

							serverState = ServerState.CONNECTED;
							if (!input.init()) {
								controllerDisconnected();
								return;
							}
							EventQueue.invokeLater(() -> main.setStatusBarText(
									MessageFormat.format(Main.STRINGS.getString("STATUS_CONNECTED_TO"),
											clientAddress.getCanonicalHostName(), clientPort)));
						}
					}
				}
				case CONNECTED -> {
					// noinspection BusyWait
					Thread.sleep(pollInterval);

					try (final var byteArrayOutputStream = new ByteArrayOutputStream();
							final var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
						dataOutputStream.writeInt(MessageType.UPDATE.getId());
						dataOutputStream.writeLong(counter);

						if (!input.poll()) {
							controllerDisconnected();
							return;
						}
						try (final var objectOutputStream = new ObjectOutputStream(dataOutputStream)) {
							objectOutputStream.writeObject(input.getAxes());
							objectOutputStream.writeObject(input.getButtons());
							objectOutputStream.writeInt(input.getCursorDeltaX());
							objectOutputStream.writeInt(input.getCursorDeltaY());
							objectOutputStream.writeObject(new HashSet<>(input.getDownMouseButtons()));
							objectOutputStream.writeObject(input.getDownUpMouseButtons());
							objectOutputStream.writeObject(input.getDownKeyStrokes());
							objectOutputStream.writeObject(input.getDownUpKeyStrokes());

							objectOutputStream.writeInt(input.getScrollClicks());

							objectOutputStream.writeObject(input.getOnLockKeys().stream().map(LockKey::virtualKeyCode)
									.collect(Collectors.toSet()));
							objectOutputStream.writeObject(input.getOffLockKeys().stream().map(LockKey::virtualKeyCode)
									.collect(Collectors.toSet()));

							input.setCursorDeltaX(0);
							input.setCursorDeltaY(0);

							input.getDownUpMouseButtons().clear();
							input.getDownUpKeyStrokes().clear();

							input.setScrollClicks(0);

							input.getOnLockKeys().clear();
							input.getOffLockKeys().clear();

							sendEncrypted(byteArrayOutputStream, clientPort);
							counter++;
						}
					}

					if (counter % REQUEST_ALIVE_INTERVAL == 0) {
						var gotClientAlive = false;
						for (int i = 0; i < NUM_REQUEST_ALIVE_RETRIES; i++) {
							try (final var byteArrayOutputStream = new ByteArrayOutputStream();
									final var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
								dataOutputStream.writeInt(MessageType.REQUEST_ALIVE.getId());

								sendEncrypted(byteArrayOutputStream, clientPort);
							}

							receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
							serverSocket.setSoTimeout(timeout);
							try {
								serverSocket.receive(receivePacket);

								if (clientAddress.equals(receivePacket.getAddress())) {
									try (final var byteArrayInputStream = new ByteArrayInputStream(
											receivePacket.getData());
											final var dataInputStream = new DataInputStream(byteArrayInputStream)) {
										final var messageType = dataInputStream.readInt();
										if (messageType == MessageType.CLIENT_ALIVE.getId()) {
											counter++;
											gotClientAlive = true;
											break;
										}
									}
								}
							} catch (final SocketTimeoutException _) {
								// handled below
							}
						}

						if (! gotClientAlive) {
							input.reset();
							input.deInit();

							main.setStatusBarText(Main.STRINGS.getString("STATUS_TIMEOUT"));
							main.scheduleStatusBarText(
									MessageFormat.format(Main.STRINGS.getString("STATUS_LISTENING"), port));

							serverState = ServerState.LISTENING;
						}
					}
				}
				}
			}
		} catch (final BindException e) {
			LOGGER.log(Level.WARNING, "Could not bind socket on port " + port);
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
					MessageFormat.format(Main.STRINGS.getString("COULD_NOT_OPEN_SOCKET_DIALOG_TEXT"), port),
					Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		} catch (final SocketException e) {
			LOGGER.log(Level.FINE, e.getMessage(), e);
		} catch (final IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
					Main.STRINGS.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
					Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		} catch (final InterruptedException _) {
			// expected whenever the run mode gets stopped
		} finally {
			input.reset();

			if (serverSocket != null) {
				serverSocket.close();
			}

			EventQueue.invokeLater(() -> {
				main.setStatusBarText(Main.STRINGS.getString("STATUS_SOCKET_CLOSED"));
				main.stopAll(false, false, true);
			});
		}

		logStop();
	}

	private void sendEncrypted(final ByteArrayOutputStream byteArrayOutputStream, final int clientPort)
			throws IOException {
		Objects.requireNonNull(cipher, "Field cipher must not be null");

		final var messageBytes = byteArrayOutputStream.toByteArray();
		try {
			random.nextBytes(iv);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));

			final var encryptedBytes = cipher.doFinal(messageBytes);
			final var packetByteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
			packetByteBuffer.put(iv);
			packetByteBuffer.put(encryptedBytes);
			final var packetBytes = packetByteBuffer.array();

			final var datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, clientAddress, clientPort);
			serverSocket.send(datagramPacket);
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	enum MessageType {

		CLIENT_HELLO(0), SERVER_HELLO(1), UPDATE(2), REQUEST_ALIVE(3), CLIENT_ALIVE(4);

		private static final Map<Integer, MessageType> ID_TO_MESSAGE_TYPE_MAP = Arrays.stream(values())
				.collect(Collectors.toUnmodifiableMap(MessageType::getId, Function.identity()));

		private final int id;

		MessageType(final int id) {
			this.id = id;
		}

		static MessageType fromId(final int id) {
			return ID_TO_MESSAGE_TYPE_MAP.get(id);
		}

		int getId() {
			return id;
		}
	}

	public enum ServerState {
		LISTENING, CONNECTED
	}
}

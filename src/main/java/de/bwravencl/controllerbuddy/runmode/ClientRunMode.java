/*
 * Copyright (C) 2015 Matteo Hausner
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
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.Keystroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.runmode.ServerRunMode.MessageType;
import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.Status;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.swing.JOptionPane;

/// Client-side run mode that receives controller input state over the
/// network.
///
/// Connects to a remote [ServerRunMode] via an encrypted UDP channel,
/// receives serialized input state updates, and writes them to the
/// local virtual output device.
public final class ClientRunMode extends OutputRunMode {

	private static final Logger LOGGER = Logger.getLogger(ClientRunMode.class.getName());

	/// Number of retries when attempting to establish the initial connection.
	private static final int NUM_CONNECTION_RETRIES = 10;

	/// Number of retries when waiting to receive a packet from the server.
	private static final int NUM_RECEIVE_PACKET_RETRIES = 10;

	/// Cipher instance used for AES-GCM encryption and decryption.
	private final Cipher cipher;

	/// Hostname or IP address of the remote server to connect to.
	private final String host;

	/// Buffer holding the initialization vector for AES-GCM decryption.
	private final byte[] iv = new byte[ServerRunMode.IV_LENGTH];

	/// Secret key derived from the salt used for encrypting and decrypting packets.
	private final Key key;

	/// UDP port number used to communicate with the server.
	private final int port;

	/// Reusable byte buffer for incoming UDP packets.
	private final byte[] receiveBuf = new byte[1024];

	/// Random salt sent to the server during the handshake for key derivation.
	private final byte[] salt = new byte[ServerRunMode.SALT_LENGTH];

	/// Socket timeout in milliseconds when waiting for incoming packets.
	private final int timeout;

	/// UDP socket used to send and receive packets to and from the server.
	private DatagramSocket clientSocket;

	/// Current connection state of the client.
	private ClientState clientState = ClientState.CONNECTING;

	/// Monotonically increasing counter used to discard out-of-order packets.
	private long counter = -1;

	/// Resolved network address of the remote server host.
	private InetAddress hostAddress;

	/// Creates a new client run mode that will connect to the configured remote
	/// server.
	///
	/// @param main the main application instance providing host, port, timeout,
	/// and encryption settings
	/// @param input the input instance for controller state
	public ClientRunMode(final Main main, final Input input) {
		super(main, input);

		host = main.getHost();
		port = main.getPort();
		timeout = main.getTimeout();

		try {
			cipher = Cipher.getInstance(ServerRunMode.CIPHER_TRANSFORMATION);
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException(e);
		}

		main.getRandom().nextBytes(salt);
		key = ServerRunMode.deriveKey(main, salt);
	}

	/// Decrypts the payload of the given datagram packet using AES-GCM.
	///
	/// Reads the initialization vector from the beginning of the packet data,
	/// initializes the cipher in decrypt mode, and returns the decrypted bytes.
	///
	/// @param packet the datagram packet whose payload is to be decrypted
	/// @return the decrypted byte array
	/// @throws GeneralSecurityException if decryption fails
	private byte[] decrypt(final DatagramPacket packet) throws GeneralSecurityException {
		final var packetByteBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());

		packetByteBuffer.get(iv);
		cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(ServerRunMode.TAG_LENGTH, iv));

		final var encryptedBytes = new byte[packetByteBuffer.remaining()];
		packetByteBuffer.get(encryptedBytes);

		return cipher.doFinal(encryptedBytes);
	}

	@Override
	Logger getLogger() {
		return LOGGER;
	}

	/// Handles a [GeneralSecurityException] that occurred during decryption by
	/// logging the error, showing an error dialog, and requesting a stop.
	///
	/// @param e the security exception that was caught
	private void handleGeneralSecurityException(final GeneralSecurityException e) {
		LOGGER.log(Level.WARNING, e.getMessage(), e);

		EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
				Main.STRINGS.getString("DECRYPTION_ERROR_DIALOG_TEXT"), Main.STRINGS.getString("ERROR_DIALOG_TITLE"),
				JOptionPane.ERROR_MESSAGE));

		forceStop = true;
		run = false;
	}

	@SuppressWarnings("unchecked")
	@Override
	boolean readInput() throws IOException {
		super.readInput();

		var retVal = false;

		switch (clientState) {
		case CONNECTING -> {
			LOGGER.info("Connecting to " + host + ":" + port);
			EventQueue.invokeLater(() -> main.setStatusBarText(
					MessageFormat.format(Main.STRINGS.getString("STATUS_CONNECTING_TO_HOST"), host, port)));

			try (final var byteArrayOutputStream = new ByteArrayOutputStream();
					final var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
				dataOutputStream.writeInt(MessageType.CLIENT_HELLO.getId());

				dataOutputStream.write(salt, 0, ServerRunMode.SALT_LENGTH);

				dataOutputStream.writeInt(minAxisValue);
				dataOutputStream.writeInt(maxAxisValue);
				dataOutputStream.writeInt(numButtons);

				final var helloBuf = byteArrayOutputStream.toByteArray();
				final var helloPacket = new DatagramPacket(helloBuf, helloBuf.length, hostAddress, port);

				var success = false;
				var retry = NUM_CONNECTION_RETRIES;
				do {
					clientSocket.send(helloPacket);

					final var receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
					try {
						clientSocket.receive(receivePacket);

						final var plaintextBuf = decrypt(receivePacket);
						try (final var byteArrayInputStream = new ByteArrayInputStream(plaintextBuf);
								final var dataInputStream = new DataInputStream(byteArrayInputStream)) {
							final var messageType = dataInputStream.readInt();
							if (messageType == MessageType.SERVER_HELLO.getId()) {
								final var serverProtocolVersion = dataInputStream.readByte();
								if (serverProtocolVersion != ServerRunMode.PROTOCOL_VERSION) {
									LOGGER.warning("Protocol version mismatch: client " + ServerRunMode.PROTOCOL_VERSION
											+ " vs server " + serverProtocolVersion);
									EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
											MessageFormat.format(
													Main.STRINGS.getString("PROTOCOL_VERSION_MISMATCH_DIALOG_TEXT"),
													ServerRunMode.PROTOCOL_VERSION, serverProtocolVersion),
											Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
									retry = -1;
								} else {
									pollInterval = dataInputStream.readLong();
									success = true;
								}
							} else {
								retry--;
								final var finalRetry = retry;
								EventQueue.invokeLater(() -> main.setStatusBarText(
										MessageFormat.format(Main.STRINGS.getString("STATUS_INVALID_MESSAGE_RETRYING"),
												NUM_CONNECTION_RETRIES - finalRetry, NUM_CONNECTION_RETRIES)));
							}
						}
					} catch (final GeneralSecurityException e) {
						handleGeneralSecurityException(e);
					} catch (final SocketTimeoutException e) {
						LOGGER.log(Level.INFO, e.getMessage(), e);
						retry--;
						final var finalRetry = retry;
						EventQueue.invokeLater(() -> main.setStatusBarText(
								MessageFormat.format(Main.STRINGS.getString("STATUS_TIMEOUT_RETRYING"),
										NUM_CONNECTION_RETRIES - finalRetry, NUM_CONNECTION_RETRIES)));
					}
				} while (!success && retry > 0 && run);

				if (success) {
					clientState = ClientState.CONNECTED;
					LOGGER.info("Successfully connected");
					EventQueue.invokeLater(() -> main.setStatusBarText(
							MessageFormat.format(Main.STRINGS.getString("STATUS_CONNECTED_TO"), host, port)));
				} else {
					if (retry != -1 && run) {
						LOGGER.info("Could not connect after " + NUM_CONNECTION_RETRIES + " retries");
						EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
								MessageFormat.format(Main.STRINGS.getString("COULD_NOT_CONNECT_DIALOG_TEXT"),
										NUM_CONNECTION_RETRIES),
								Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
					}

					forceStop = true;
					run = false;
				}
			}

		}
		case CONNECTED -> {
			try {
				final var receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
				SocketTimeoutException socketTimeoutException = null;
				for (var i = 0; i < NUM_RECEIVE_PACKET_RETRIES; i++) {
					try {
						clientSocket.receive(receivePacket);
						socketTimeoutException = null;
						break;
					} catch (final SocketTimeoutException e) {
						socketTimeoutException = e;
					}
				}

				if (socketTimeoutException != null) {
					throw socketTimeoutException;
				}

				final var plaintextBuf = decrypt(receivePacket);
				try (final var byteArrayInputStream = new ByteArrayInputStream(plaintextBuf);
						final var dataInputStream = new DataInputStream(byteArrayInputStream)) {
					final var messageType = MessageType.fromId(dataInputStream.readInt());
					if (messageType == null) {
						throw new IOException("Invalid message type");
					}

					switch (messageType) {
					case UPDATE -> {
						final var newCounter = dataInputStream.readLong();

						try (final var objectInputStream = new ObjectInputStream(dataInputStream)) {
							objectInputStream
									.setObjectInputFilter(ObjectInputFilter.allowFilter(
											clazz -> clazz == null || clazz.isArray() || clazz == Number.class
													|| clazz == Integer.class || clazz == Enum.class
													|| clazz == HashSet.class || clazz == EnumMap.class
													|| clazz == VirtualAxis.class || clazz == Keystroke.class,
											Status.REJECTED));

							if (newCounter > counter) {
								final var inputAxes = (EnumMap<VirtualAxis, Integer>) objectInputStream.readObject();
								axisX.set(inputAxes.get(VirtualAxis.X));
								axisY.set(inputAxes.get(VirtualAxis.Y));
								axisZ.set(inputAxes.get(VirtualAxis.Z));
								axisRX.set(inputAxes.get(VirtualAxis.RX));
								axisRY.set(inputAxes.get(VirtualAxis.RY));
								axisRZ.set(inputAxes.get(VirtualAxis.RZ));
								axisS0.set(inputAxes.get(VirtualAxis.S0));
								axisS1.set(inputAxes.get(VirtualAxis.S1));

								final var inputButtons = (boolean[]) objectInputStream.readObject();
								for (var i = 0; i < numButtons; i++) {
									buttons[i].set(inputButtons[i] ? 1 : 0);
									inputButtons[i] = false;
								}

								cursorDeltaX = objectInputStream.readInt();
								cursorDeltaY = objectInputStream.readInt();

								updateOutputSets((Set<Integer>) objectInputStream.readObject(), oldDownMouseButtons,
										newUpMouseButtons, newDownMouseButtons, false);

								downUpMouseButtons.clear();
								downUpMouseButtons.addAll((Set<Integer>) objectInputStream.readObject());

								final var downKeystrokes = (Set<Keystroke>) objectInputStream.readObject();
								final var inputDownModifiers = downKeystrokes.stream().map(Keystroke::getModifierCodes)
										.flatMap(Stream::of).collect(Collectors.toSet());
								updateOutputSets(inputDownModifiers, oldDownModifiers, newUpModifiers, newDownModifiers,
										false);

								final var inputDownNormalKeys = downKeystrokes.stream().map(Keystroke::getKeyCodes)
										.flatMap(Stream::of).collect(Collectors.toSet());
								updateOutputSets(inputDownNormalKeys, oldDownNormalKeys, newUpNormalKeys,
										newDownNormalKeys, true);

								downUpKeystrokes.clear();
								downUpKeystrokes.addAll((Set<Keystroke>) objectInputStream.readObject());

								scrollClicks = objectInputStream.readInt();

								onLockKeys.clear();
								((Set<Integer>) objectInputStream.readObject()).stream()
										.map(LockKey.VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP::get)
										.forEachOrdered(onLockKeys::add);

								offLockKeys.clear();
								((Set<Integer>) objectInputStream.readObject()).stream()
										.map(LockKey.VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP::get)
										.forEachOrdered(offLockKeys::add);

								counter = newCounter;
								retVal = true;
							}
						}
					}
					case REQUEST_ALIVE -> {
						try (final var byteArrayOutputStream = new ByteArrayOutputStream();
								final var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
							dataOutputStream.writeInt(MessageType.CLIENT_ALIVE.getId());

							final var keepAliveBuf = byteArrayOutputStream.toByteArray();
							final var keepAlivePacket = new DatagramPacket(keepAliveBuf, keepAliveBuf.length,
									hostAddress, port);
							clientSocket.send(keepAlivePacket);
						}
					}
					default -> {
					}
					}
				} catch (final ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			} catch (final GeneralSecurityException e) {
				handleGeneralSecurityException(e);
			} catch (final SocketTimeoutException e) {
				LOGGER.log(Level.FINE, e.getMessage(), e);
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
						Main.STRINGS.getString("CONNECTION_LOST_DIALOG_TEXT"),
						Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));

				forceStop = true;
				run = false;
			}
		}
		}

		return retVal;
	}

	/// Requests this client run mode to stop by closing the UDP socket and
	/// signaling a forced stop.
	@Override
	public void requestStop() {
		super.requestStop();

		if (clientSocket != null) {
			clientSocket.close();
			forceStop = true;
		}
	}

	/// Runs the client loop: initializes the UDP socket, connects to the remote
	/// server, and repeatedly receives input state and writes it to the local
	/// output device until stopped.
	@Override
	public void run() {
		logStart();

		try {
			if (init()) {
				clientSocket = new DatagramSocket(port + 1);

				IOException ioException = null;
				for (final var hostAddress : InetAddress.getAllByName(host)) {
					try {
						clientSocket.connect(hostAddress, port);
						this.hostAddress = hostAddress;
						break;
					} catch (final UncheckedIOException e) {
						final var causingIoException = e.getCause();

						if (ioException == null) {
							ioException = causingIoException;
						} else {
							ioException.addSuppressed(causingIoException);
						}
					}
				}

				if (ioException != null) {
					throw ioException;
				}

				clientSocket.setSoTimeout(timeout);

				while (run) {
					if (readInput()) {
						writeOutput();
					}
				}
			} else {
				forceStop = true;
			}
		} catch (final UnknownHostException e) {
			forceStop = true;

			LOGGER.info("Could not resolve host: " + host);
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
					MessageFormat.format(Main.STRINGS.getString("INVALID_HOST_ADDRESS_DIALOG_TEXT"), host),
					Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		} catch (final SocketException e) {
			if (forceStop) {
				return;
			}

			forceStop = true;

			LOGGER.log(Level.INFO, e.getMessage(), e);
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
					MessageFormat.format(Main.STRINGS.getString("SOCKET_ERROR_DIALOG_TEXT"), host),
					Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		} catch (final IOException e) {
			handleIOException(e);
		} finally {
			if (clientSocket != null) {
				clientSocket.close();
			}

			deInit();
		}

		logStop();
	}

	/// Represents the connection state of the client run mode.
	///
	/// Tracks whether the client is in the process of establishing a connection
	/// to the server or has successfully connected and is receiving input data.
	private enum ClientState {
		/// The client is attempting to establish a connection to the server
		CONNECTING,

		/// The client has successfully connected to the server
		CONNECTED
	}
}

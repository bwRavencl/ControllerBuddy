/* Copyright (C) 2020  Matteo Hausner
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

import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.Status;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.runmode.ServerRunMode.MessageType;

public final class ClientRunMode extends OutputRunMode {

	private enum ClientState {
		Connecting, Connected
	}

	private static final Logger log = Logger.getLogger(ClientRunMode.class.getName());

	public static final String DEFAULT_HOST = "127.0.0.1";
	private static final int N_CONNECTION_RETRIES = 10;

	private String host = DEFAULT_HOST;
	private int port = ServerRunMode.DEFAULT_PORT;
	private int timeout = ServerRunMode.DEFAULT_TIMEOUT;
	private ClientState clientState = ClientState.Connecting;
	private InetAddress hostAddress;
	private DatagramSocket clientSocket;
	private final byte[] receiveBuf = new byte[1024];
	private long counter = -1;

	public ClientRunMode(final Main main, final Input input) {
		super(main, input);
	}

	public void close() {
		if (clientSocket != null)
			clientSocket.close();
	}

	@Override
	Logger getLogger() {
		return log;
	}

	@SuppressWarnings("unchecked")
	@Override
	boolean readInput() throws IOException {
		super.readInput();

		var retVal = false;

		switch (clientState) {
		case Connecting -> {
			log.log(Level.INFO, "Connecting to " + host + ":" + port);
			EventQueue.invokeLater(() -> main.setStatusBarText(
					MessageFormat.format(Main.strings.getString("STATUS_CONNECTING_TO_HOST"), host, port)));

			try (final var byteArrayOutputStream = new ByteArrayOutputStream();
					var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
				dataOutputStream.writeInt(MessageType.ClientHello.ordinal());
				dataOutputStream.writeInt(minAxisValue);
				dataOutputStream.writeInt(maxAxisValue);
				dataOutputStream.writeInt(nButtons);

				final var helloBuf = byteArrayOutputStream.toByteArray();
				final var helloPacket = new DatagramPacket(helloBuf, helloBuf.length, hostAddress, port);

				var success = false;
				var retry = N_CONNECTION_RETRIES;
				do {
					clientSocket.send(helloPacket);

					final var receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
					try {
						clientSocket.receive(receivePacket);

						try (final var byteArrayInputStream = new ByteArrayInputStream(receivePacket.getData());
								var dataInputStream = new DataInputStream(byteArrayInputStream)) {
							final var messageType = dataInputStream.readInt();
							if (messageType == MessageType.ServerHello.ordinal()) {
								final var serverProtocolVersion = dataInputStream.readByte();
								if (serverProtocolVersion != ServerRunMode.PROTOCOL_VERSION) {
									log.log(Level.WARNING, "Protocol version mismatch: client "
											+ ServerRunMode.PROTOCOL_VERSION + " vs server " + serverProtocolVersion);
									EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main.getFrame(),
											MessageFormat.format(
													Main.strings.getString("PROTOCOL_VERSION_MISMATCH_DIALOG_TEXT"),
													ServerRunMode.PROTOCOL_VERSION, serverProtocolVersion),
											Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
									retry = -1;
								} else {
									pollInterval = dataInputStream.readLong();
									success = true;
								}
							} else {
								retry--;
								final var finalRetry = retry;
								EventQueue.invokeLater(() -> main.setStatusBarText(
										MessageFormat.format(Main.strings.getString("STATUS_INVALID_MESSAGE_RETRYING"),
												N_CONNECTION_RETRIES - finalRetry, N_CONNECTION_RETRIES)));
							}
						}
					} catch (final SocketTimeoutException e) {
						log.log(Level.INFO, e.getMessage(), e);
						retry--;
						final var finalRetry = retry;
						EventQueue.invokeLater(() -> main.setStatusBarText(
								MessageFormat.format(Main.strings.getString("STATUS_TIMEOUT_RETRYING"),
										N_CONNECTION_RETRIES - finalRetry, N_CONNECTION_RETRIES)));
					}
				} while (!success && retry > 0 && !Thread.currentThread().isInterrupted());

				if (success) {
					clientState = ClientState.Connected;
					log.log(Level.INFO, "Successfully connected");
					EventQueue.invokeLater(() -> main.setStatusBarText(MessageFormat
							.format(Main.strings.getString("STATUS_CONNECTED_TO"), host, port, pollInterval)));
				} else {
					if (retry != -1 && !Thread.currentThread().isInterrupted()) {
						log.log(Level.INFO, "Could not connect after " + N_CONNECTION_RETRIES + " retries");
						EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main.getFrame(),
								MessageFormat.format(Main.strings.getString("COULD_NOT_CONNECT_DIALOG_TEXT"),
										N_CONNECTION_RETRIES),
								Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
					}

					forceStop = true;
					Thread.currentThread().interrupt();
				}
			}
		}
		case Connected -> {
			try {
				final var receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
				clientSocket.receive(receivePacket);

				try (final var byteArrayInputStream = new ByteArrayInputStream(receivePacket.getData());
						var dataInputStream = new DataInputStream(byteArrayInputStream);
						var objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
					objectInputStream.setObjectInputFilter(ObjectInputFilter.allowFilter(
							clazz -> clazz == null || clazz.isArray() || clazz == Number.class || clazz == Integer.class
									|| clazz == Enum.class || clazz == HashSet.class || clazz == EnumMap.class
									|| clazz == VirtualAxis.class || clazz == KeyStroke.class,
							Status.REJECTED));

					final var messageType = MessageType.values()[dataInputStream.readInt()];

					if (messageType == MessageType.Update || messageType == MessageType.UpdateRequestAlive) {

						final var newCounter = dataInputStream.readLong();
						if (newCounter > counter) {

							final var inputAxes = (EnumMap<VirtualAxis, Integer>) objectInputStream.readObject();
							axisX.setValue(inputAxes.get(VirtualAxis.X));
							axisY.setValue(inputAxes.get(VirtualAxis.Y));
							axisZ.setValue(inputAxes.get(VirtualAxis.Z));
							axisRX.setValue(inputAxes.get(VirtualAxis.RX));
							axisRY.setValue(inputAxes.get(VirtualAxis.RY));
							axisRZ.setValue(inputAxes.get(VirtualAxis.RZ));
							axisS0.setValue(inputAxes.get(VirtualAxis.S0));
							axisS1.setValue(inputAxes.get(VirtualAxis.S1));

							final var inputButtons = (boolean[]) objectInputStream.readObject();
							for (var i = 0; i < nButtons; i++)
								buttons[i].setValue(inputButtons[i] ? 1 : 0);

							cursorDeltaX = objectInputStream.readInt();
							cursorDeltaY = objectInputStream.readInt();

							updateOutputSets((Set<Integer>) objectInputStream.readObject(), oldDownMouseButtons,
									newUpMouseButtons, newDownMouseButtons, false);

							downUpMouseButtons.clear();
							downUpMouseButtons.addAll((Set<Integer>) objectInputStream.readObject());

							final var downKeyStrokes = (Set<KeyStroke>) objectInputStream.readObject();
							final var inputDownModifiers = downKeyStrokes.stream().map(KeyStroke::getModifierCodes)
									.flatMap(Stream::of).collect(Collectors.toSet());
							updateOutputSets(inputDownModifiers, oldDownModifiers, newUpModifiers, newDownModifiers,
									false);

							final var inputDownNormalKeys = downKeyStrokes.stream().map(KeyStroke::getKeyCodes)
									.flatMap(Stream::of).collect(Collectors.toSet());
							updateOutputSets(inputDownNormalKeys, oldDownNormalKeys, newUpNormalKeys, newDownNormalKeys,
									true);

							downUpKeyStrokes.clear();
							downUpKeyStrokes.addAll((Set<KeyStroke>) objectInputStream.readObject());

							scrollClicks = objectInputStream.readInt();

							onLockKeys.clear();
							((Set<Integer>) objectInputStream.readObject()).stream()
									.map(LockKey.virtualKeyCodeToLockKeyMap::get).forEachOrdered(onLockKeys::add);

							offLockKeys.clear();
							((Set<Integer>) objectInputStream.readObject()).stream()
									.map(LockKey.virtualKeyCodeToLockKeyMap::get).forEachOrdered(offLockKeys::add);

							counter = newCounter;
							retVal = true;
						}

					}

					if (messageType == MessageType.UpdateRequestAlive)
						try (final var byteArrayOutputStream = new ByteArrayOutputStream();
								var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
							dataOutputStream.writeInt(MessageType.ClientAlive.ordinal());

							final var keepAliveBuf = byteArrayOutputStream.toByteArray();
							final var keepAlivePacket = new DatagramPacket(keepAliveBuf, keepAliveBuf.length,
									hostAddress, port);
							clientSocket.send(keepAlivePacket);
						}
				} catch (final ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			} catch (final SocketTimeoutException e) {
				log.log(Level.FINE, e.getMessage(), e);
				EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main.getFrame(),
						Main.strings.getString("CONNECTION_LOST_DIALOG_TEXT"),
						Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));

				forceStop = true;
				Thread.currentThread().interrupt();
			}
		}
		}

		return retVal;
	}

	@Override
	public void run() {
		logStart();

		try {
			if (init()) {
				hostAddress = InetAddress.getByName(host);
				clientSocket = new DatagramSocket(port + 1);
				clientSocket.connect(hostAddress, port);
				clientSocket.setSoTimeout(timeout);

				while (!Thread.interrupted())
					if (readInput())
						writeOutput();
			} else
				forceStop = true;
		} catch (final UnknownHostException e) {
			forceStop = true;

			log.log(Level.INFO, "Could not resolve host: " + host);
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main.getFrame(),
					MessageFormat.format(Main.strings.getString("INVALID_HOST_ADDRESS_DIALOG_TEXT"), host),
					Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		} catch (final SocketException e) {
			log.log(Level.FINE, e.getMessage(), e);
		} catch (final IOException e) {
			forceStop = true;

			log.log(Level.SEVERE, e.getMessage(), e);
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main.getFrame(),
					Main.strings.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
					Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		} finally {
			if (clientSocket != null)
				clientSocket.close();
			deInit();
		}

		logStop();
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}
}

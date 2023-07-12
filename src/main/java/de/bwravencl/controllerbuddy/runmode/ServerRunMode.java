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
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.lwjgl.glfw.GLFW;

import com.sun.jna.Platform;

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.LockKey;

public final class ServerRunMode extends RunMode {

	public enum MessageType {
		ClientHello, ServerHello, Update, UpdateRequestAlive, ClientAlive
	}

	public enum ServerState {
		Listening, Connected
	}

	private static final Logger log = Logger.getLogger(ServerRunMode.class.getName());

	static final byte PROTOCOL_VERSION = 1;

	public static final int DEFAULT_PORT = 28789;
	public static final int DEFAULT_TIMEOUT = 2000;

	private static final int REQUEST_ALIVE_INTERVAL = 100;

	private int port = DEFAULT_PORT;
	private int timeout = DEFAULT_TIMEOUT;
	private DatagramSocket serverSocket;
	private InetAddress clientIPAddress;

	public ServerRunMode(final Main main, final Input input) {
		super(main, input);
	}

	public void close() {
		if (serverSocket != null)
			serverSocket.close();
	}

	@Override
	Logger getLogger() {
		return log;
	}

	@Override
	public void run() {
		logStart();

		final var clientPort = port + 1;
		var serverState = ServerState.Listening;
		DatagramPacket receivePacket;
		var counter = 0L;

		try {
			serverSocket = new DatagramSocket(port);
			final var receiveBuf = new byte[1024];

			EventQueue.invokeLater(() -> main
					.setStatusBarText(MessageFormat.format(Main.strings.getString("STATUS_LISTENING"), port)));

			for (;;) {
				if (!Platform.isMac())
					GLFW.glfwPollEvents();

				switch (serverState) {
				case Listening -> {
					counter = 0;
					receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
					serverSocket.setSoTimeout(0);
					serverSocket.receive(receivePacket);
					clientIPAddress = receivePacket.getAddress();

					try (var dataInputStream = new DataInputStream(new ByteArrayInputStream(receivePacket.getData()))) {
						final var messageType = dataInputStream.readInt();

						if (messageType == MessageType.ClientHello.ordinal()) {
							minAxisValue = dataInputStream.readInt();
							maxAxisValue = dataInputStream.readInt();
							setnButtons(dataInputStream.readInt());

							try (final var byteArrayOutputStream = new ByteArrayOutputStream();
									var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
								dataOutputStream.writeInt(MessageType.ServerHello.ordinal());
								dataOutputStream.writeByte(PROTOCOL_VERSION);
								dataOutputStream.writeLong(pollInterval);

								final var sendBuf = byteArrayOutputStream.toByteArray();
								final var sendPacket = new DatagramPacket(sendBuf, sendBuf.length, clientIPAddress,
										clientPort);
								serverSocket.send(sendPacket);
							}

							serverState = ServerState.Connected;
							input.init();
							EventQueue.invokeLater(() -> main.setStatusBarText(
									MessageFormat.format(Main.strings.getString("STATUS_CONNECTED_TO"),
											clientIPAddress.getCanonicalHostName(), clientPort, pollInterval)));
						}
					}
				}
				case Connected -> {
					Thread.sleep(pollInterval);

					final var doAliveCheck = counter % REQUEST_ALIVE_INTERVAL == 0;
					try (final var byteArrayOutputStream = new ByteArrayOutputStream();
							var dataOutputStream = new DataOutputStream(byteArrayOutputStream);
							var objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
						dataOutputStream.writeInt(
								(doAliveCheck ? MessageType.UpdateRequestAlive : MessageType.Update).ordinal());
						dataOutputStream.writeLong(counter);

						if (!input.poll()) {
							controllerDisconnected();
							return;
						}

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

						final var sendBuf = byteArrayOutputStream.toByteArray();
						final var sendPacket = new DatagramPacket(sendBuf, sendBuf.length, clientIPAddress, clientPort);
						serverSocket.send(sendPacket);
					}

					if (doAliveCheck) {
						receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
						serverSocket.setSoTimeout(timeout);
						try {
							serverSocket.receive(receivePacket);

							if (clientIPAddress.equals(receivePacket.getAddress()))
								try (final var byteArrayInputStream = new ByteArrayInputStream(
										receivePacket.getData())) {
									try (var dataInputStream = new DataInputStream(byteArrayInputStream)) {
										final var messageType = dataInputStream.readInt();
										if (messageType == MessageType.ClientAlive.ordinal())
											counter++;
									}
								}
						} catch (final SocketTimeoutException e) {
							input.reset();
							input.deInit(false);

							main.setStatusBarText(Main.strings.getString("STATUS_TIMEOUT"));
							main.scheduleStatusBarText(
									MessageFormat.format(Main.strings.getString("STATUS_LISTENING"), port));

							serverState = ServerState.Listening;
						}
					} else
						counter++;
				}
				}
			}
		} catch (final BindException e) {
			log.log(Level.WARNING, "Could not bind socket on port " + port);
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main.getFrame(),
					MessageFormat.format(Main.strings.getString("COULD_NOT_OPEN_SOCKET_DIALOG_TEXT"), port),
					Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		} catch (final SocketException e) {
			log.log(Level.FINE, e.getMessage(), e);
		} catch (final IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main.getFrame(),
					Main.strings.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
					Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		} catch (final InterruptedException ignored) {
		} finally {
			input.reset();

			if (serverSocket != null)
				serverSocket.close();

			EventQueue.invokeLater(() -> {
				main.setStatusBarText(Main.strings.getString("STATUS_SOCKET_CLOSED"));
				main.stopAll(false, false, true);
			});
		}

		logStop();
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}
}

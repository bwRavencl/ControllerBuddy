/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.controllerbuddy.output;

import static com.sun.jna.Platform.isMac;
import static de.bwravencl.controllerbuddy.gui.GuiUtils.showMessageDialog;
import static de.bwravencl.controllerbuddy.gui.Main.strings;
import static java.awt.EventQueue.invokeLater;
import static java.text.MessageFormat.format;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.version.VersionUtils;

public final class ServerOutput extends Output {

	public enum ServerState {
		Listening, Connected
	}

	private static final Logger log = Logger.getLogger(ServerOutput.class.getName());

	public static final int DEFAULT_PORT = 28789;
	public static final int DEFAULT_TIMEOUT = 2000;
	static final String PROTOCOL_MESSAGE_DELIMITER = ":";
	static final String PROTOCOL_MESSAGE_CLIENT_HELLO = "CLIENT_HELLO";
	static final String PROTOCOL_MESSAGE_SERVER_HELLO = "SERVER_HELLO";
	static final String PROTOCOL_MESSAGE_UPDATE = "UPDATE";
	static final String PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE = PROTOCOL_MESSAGE_UPDATE + "_ALIVE";
	static final String PROTOCOL_MESSAGE_CLIENT_ALIVE = "CLIENT_ALIVE";
	private static final int REQUEST_ALIVE_INTERVAL = 100;

	private int port = DEFAULT_PORT;
	private int timeout = DEFAULT_TIMEOUT;
	private DatagramSocket serverSocket;
	private InetAddress clientIPAddress;
	private ServerState serverState;

	public ServerOutput(final Main main, final Input input) {
		super(main, input);
	}

	public void close() {
		serverSocket.close();
	}

	@Override
	Logger getLogger() {
		return log;
	}

	public ServerState getServerState() {
		return serverState;
	}

	@Override
	public void run() {
		logStart();

		final var clientPort = port + 1;
		serverState = ServerState.Listening;
		DatagramPacket receivePacket;
		String message;
		var counter = 0L;

		try {
			serverSocket = new DatagramSocket(port);
			final var receiveBuf = new byte[1024];

			invokeLater(() -> {
				main.setStatusBarText(format(strings.getString("STATUS_LISTENING"), port));
			});

			for (;;) {
				if (!isMac())
					glfwPollEvents();

				switch (serverState) {
				case Listening -> {
					counter = 0;
					receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
					serverSocket.setSoTimeout(0);
					serverSocket.receive(receivePacket);
					clientIPAddress = receivePacket.getAddress();
					message = new String(receivePacket.getData(), 0, receivePacket.getLength(),
							StandardCharsets.US_ASCII);

					if (message.startsWith(PROTOCOL_MESSAGE_CLIENT_HELLO)) {
						final var messageParts = message.split(PROTOCOL_MESSAGE_DELIMITER);

						if (messageParts.length == 4) {
							minAxisValue = Integer.parseInt(messageParts[1]);
							maxAxisValue = Integer.parseInt(messageParts[2]);
							setnButtons(Integer.parseInt(messageParts[3]));

							final var sb = new StringBuilder();
							sb.append(PROTOCOL_MESSAGE_SERVER_HELLO);
							sb.append(PROTOCOL_MESSAGE_DELIMITER);
							sb.append(VersionUtils.getMajorAndMinorVersion());
							sb.append(PROTOCOL_MESSAGE_DELIMITER);
							sb.append(String.valueOf(pollInterval));

							final var sendBuf = sb.toString().getBytes("ASCII");
							final var sendPacket = new DatagramPacket(sendBuf, sendBuf.length, clientIPAddress,
									clientPort);
							serverSocket.send(sendPacket);

							serverState = ServerState.Connected;
							input.init();
							invokeLater(() -> {
								main.setStatusBarText(format(strings.getString("STATUS_CONNECTED_TO"),
										clientIPAddress.getCanonicalHostName(), clientPort, pollInterval));
							});
						}
					}
				}
				case Connected -> {
					Thread.sleep(pollInterval);

					final var sb = new StringBuilder();
					var doAliveCheck = false;
					if (counter % REQUEST_ALIVE_INTERVAL == 0) {
						sb.append(PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE);
						doAliveCheck = true;
					} else
						sb.append(PROTOCOL_MESSAGE_UPDATE);
					sb.append(PROTOCOL_MESSAGE_DELIMITER + counter);

					if (!input.poll())
						controllerDisconnected();
					else {
						for (final var v : input.getAxes().values())
							sb.append(PROTOCOL_MESSAGE_DELIMITER + v);

						for (var i = 0; i < nButtons; i++) {
							sb.append(PROTOCOL_MESSAGE_DELIMITER + input.getButtons()[i]);
							input.getButtons()[i] = false;
						}

						sb.append(PROTOCOL_MESSAGE_DELIMITER + input.getCursorDeltaX() + PROTOCOL_MESSAGE_DELIMITER
								+ input.getCursorDeltaY());
						input.setCursorDeltaX(0);
						input.setCursorDeltaY(0);

						final var downMouseButtons = input.getDownMouseButtons();
						synchronized (downMouseButtons) {
							sb.append(PROTOCOL_MESSAGE_DELIMITER + downMouseButtons.size());
							for (final var b : downMouseButtons)
								sb.append(PROTOCOL_MESSAGE_DELIMITER + b);
						}

						sb.append(PROTOCOL_MESSAGE_DELIMITER + input.getDownUpMouseButtons().size());
						for (final var b : input.getDownUpMouseButtons())
							sb.append(PROTOCOL_MESSAGE_DELIMITER + b);
						input.getDownUpMouseButtons().clear();

						sb.append(PROTOCOL_MESSAGE_DELIMITER + input.getDownKeyStrokes().size());
						for (final var keyStroke : input.getDownKeyStrokes()) {
							sb.append(PROTOCOL_MESSAGE_DELIMITER + keyStroke.getModifierCodes().length);
							for (final var c : keyStroke.getModifierCodes())
								sb.append(PROTOCOL_MESSAGE_DELIMITER + c);

							sb.append(PROTOCOL_MESSAGE_DELIMITER + keyStroke.getKeyCodes().length);
							for (final var c : keyStroke.getKeyCodes())
								sb.append(PROTOCOL_MESSAGE_DELIMITER + c);
						}

						sb.append(PROTOCOL_MESSAGE_DELIMITER + input.getDownUpKeyStrokes().size());
						for (final var keyStroke : input.getDownUpKeyStrokes()) {
							sb.append(PROTOCOL_MESSAGE_DELIMITER + keyStroke.getModifierCodes().length);
							for (final var c : keyStroke.getModifierCodes())
								sb.append(PROTOCOL_MESSAGE_DELIMITER + c);

							sb.append(PROTOCOL_MESSAGE_DELIMITER + keyStroke.getKeyCodes().length);
							for (final var c : keyStroke.getKeyCodes())
								sb.append(PROTOCOL_MESSAGE_DELIMITER + c);
						}
						input.getDownUpKeyStrokes().clear();

						sb.append(PROTOCOL_MESSAGE_DELIMITER + input.getScrollClicks());
						input.setScrollClicks(0);

						sb.append(PROTOCOL_MESSAGE_DELIMITER + input.getOnLockKeys().size());
						for (final var c : input.getOnLockKeys())
							sb.append(PROTOCOL_MESSAGE_DELIMITER + c);
						input.getOnLockKeys().clear();

						sb.append(PROTOCOL_MESSAGE_DELIMITER + input.getOffLockKeys().size());
						for (final var c : input.getOffLockKeys())
							sb.append(PROTOCOL_MESSAGE_DELIMITER + c);
						input.getOffLockKeys().clear();

						final var sendBuf = sb.toString().getBytes("ASCII");

						final var sendPacket = new DatagramPacket(sendBuf, sendBuf.length, clientIPAddress, clientPort);
						serverSocket.send(sendPacket);

						if (doAliveCheck) {
							receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
							serverSocket.setSoTimeout(timeout);
							try {
								serverSocket.receive(receivePacket);

								if (clientIPAddress.equals(receivePacket.getAddress())) {
									message = new String(receivePacket.getData(), 0, receivePacket.getLength(),
											StandardCharsets.US_ASCII);

									if (PROTOCOL_MESSAGE_CLIENT_ALIVE.equals(message))
										counter++;
								}
							} catch (final SocketTimeoutException e) {
								serverState = ServerState.Listening;
								main.scheduleStatusBarText(format(strings.getString("STATUS_LISTENING"), port));
							}
						} else
							counter++;
					}

				}
				}
			}
		} catch (final BindException e) {
			log.log(WARNING, "Could not bind socket on port " + port);
			invokeLater(() -> {
				showMessageDialog(main.getFrame(), format(strings.getString("COULD_NOT_OPEN_SOCKET_DIALOG_TEXT"), port),
						strings.getString("ERROR_DIALOG_TITLE"), ERROR_MESSAGE);
			});
		} catch (final SocketException e) {
			log.log(FINE, e.getMessage(), e);
		} catch (final IOException e) {
			log.log(SEVERE, e.getMessage(), e);
			invokeLater(() -> {
				showMessageDialog(main.getFrame(), strings.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
						strings.getString("ERROR_DIALOG_TITLE"), ERROR_MESSAGE);
			});
		} catch (final InterruptedException e) {
		} finally {
			input.reset();

			if (serverSocket != null)
				serverSocket.close();

			invokeLater(() -> {
				main.setStatusBarText(strings.getString("STATUS_SOCKET_CLOSED"));
				main.stopAll(true);
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

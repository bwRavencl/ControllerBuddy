/* Copyright (C) 2016  Matteo Hausner
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

import java.io.IOException;
import java.io.StringWriter;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;

public class ServerOutputThread extends OutputThread {

	private enum ServerState {
		Listening, Connected
	}

	public static final int DEFAULT_PORT = 28789;
	public static final int DEFAULT_TIMEOUT = 2000;
	public static final int PROTOCOL_VERSION = 3;
	public static final String PROTOCOL_MESSAGE_DELIMITER = ":";
	public static final String PROTOCOL_MESSAGE_CLIENT_HELLO = "CLIENT_HELLO";
	public static final String PROTOCOL_MESSAGE_SERVER_HELLO = "SERVER_HELLO";
	public static final String PROTOCOL_MESSAGE_UPDATE = "UPDATE";
	public static final String PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE = PROTOCOL_MESSAGE_UPDATE + "_ALIVE";
	public static final String PROTOCOL_MESSAGE_CLIENT_ALIVE = "CLIENT_ALIVE";
	private static final int REQUEST_ALIVE_INTERVAL = 100;

	private int port = DEFAULT_PORT;
	private int timeout = DEFAULT_TIMEOUT;
	private DatagramSocket serverSocket;
	private InetAddress clientIPAddress;

	public ServerOutputThread(final Main main, final Input input) {
		super(main, input);
	}

	private void deInit() {
		if (serverSocket != null)
			serverSocket.close();

		main.setStatusBarText(rb.getString("STATUS_SOCKET_CLOSED"));
		main.stopServer(false);
	}

	public int getPort() {
		return port;
	}

	public int getTimeout() {
		return timeout;
	}

	@Override
	public void run() {
		final int clientPort = port + 1;
		ServerState serverState = ServerState.Listening;
		DatagramPacket receivePacket;
		String message;
		long counter = 0;

		try {
			serverSocket = new DatagramSocket(port);
			final byte[] receiveBuf = new byte[1024];

			main.setStatusBarText(rb.getString("STATUS_LISTENING") + port);

			while (run) {
				switch (serverState) {
				case Listening:
					counter = 0;
					receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
					serverSocket.setSoTimeout(0);
					serverSocket.receive(receivePacket);
					clientIPAddress = receivePacket.getAddress();
					message = new String(receivePacket.getData(), 0, receivePacket.getLength(),
							StandardCharsets.US_ASCII);

					if (message.startsWith(PROTOCOL_MESSAGE_CLIENT_HELLO)) {
						final String[] messageParts = message.split(PROTOCOL_MESSAGE_DELIMITER);

						if (messageParts.length == 4) {
							minAxisValue = Integer.parseInt(messageParts[1]);
							maxAxisValue = Integer.parseInt(messageParts[2]);
							setnButtons(Integer.parseInt(messageParts[3]));

							final StringWriter sw = new StringWriter();
							sw.append(PROTOCOL_MESSAGE_SERVER_HELLO);
							sw.append(PROTOCOL_MESSAGE_DELIMITER);
							sw.append(String.valueOf(PROTOCOL_VERSION));
							sw.append(PROTOCOL_MESSAGE_DELIMITER);
							sw.append(String.valueOf(pollInterval));

							final byte[] sendBuf = sw.toString().getBytes("ASCII");
							final DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length,
									clientIPAddress, clientPort);
							serverSocket.send(sendPacket);

							serverState = ServerState.Connected;
							main.setStatusBarText(
									rb.getString("STATUS_CONNECTED_TO_PART_1") + clientIPAddress.getCanonicalHostName()
											+ rb.getString("STATUS_CONNECTED_TO_PART_2") + clientPort
											+ rb.getString("STATUS_CONNECTED_TO_PART_3") + pollInterval
											+ rb.getString("STATUS_CONNECTED_TO_PART_4"));
						}
					}
					break;
				case Connected:
					try {
						Thread.sleep(pollInterval);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}

					final StringWriter sw = new StringWriter();
					boolean doAliveCheck = false;
					if (counter % REQUEST_ALIVE_INTERVAL == 0) {
						sw.append(PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE);
						doAliveCheck = true;
					} else
						sw.append(PROTOCOL_MESSAGE_UPDATE);
					sw.append(PROTOCOL_MESSAGE_DELIMITER + counter);

					if (!input.poll())
						controllerDisconnected();
					else {
						for (final int v : Input.getAxis().values())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + v);

						for (int i = 0; i < nButtons; i++) {
							sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getButtons()[i]);
							input.getButtons()[i] = false;
						}

						sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getCursorDeltaX() + PROTOCOL_MESSAGE_DELIMITER
								+ input.getCursorDeltaY());
						input.setCursorDeltaX(0);
						input.setCursorDeltaY(0);

						sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getDownMouseButtons().size());
						for (final int b : input.getDownMouseButtons())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + b);

						sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getDownUpMouseButtons().size());
						for (final int b : input.getDownUpMouseButtons())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + b);
						input.getDownUpMouseButtons().clear();

						sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getDownKeyStrokes().size());
						for (final KeyStroke ks : input.getDownKeyStrokes()) {
							sw.append(PROTOCOL_MESSAGE_DELIMITER + ks.getModifierCodes().length);
							for (final int c : ks.getModifierCodes())
								sw.append(PROTOCOL_MESSAGE_DELIMITER + c);

							sw.append(PROTOCOL_MESSAGE_DELIMITER + ks.getKeyCodes().length);
							for (final int c : ks.getKeyCodes())
								sw.append(PROTOCOL_MESSAGE_DELIMITER + c);
						}

						sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getDownUpKeyStrokes().size());
						for (final KeyStroke ks : input.getDownUpKeyStrokes()) {
							sw.append(PROTOCOL_MESSAGE_DELIMITER + ks.getModifierCodes().length);
							for (final int c : ks.getModifierCodes())
								sw.append(PROTOCOL_MESSAGE_DELIMITER + c);

							sw.append(PROTOCOL_MESSAGE_DELIMITER + ks.getKeyCodes().length);
							for (final int c : ks.getKeyCodes())
								sw.append(PROTOCOL_MESSAGE_DELIMITER + c);
						}
						input.getDownUpKeyStrokes().clear();

						sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getScrollClicks());
						input.setScrollClicks(0);

						sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getOnLockKeys().size());
						for (final int c : input.getOnLockKeys())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + c);
						input.getOnLockKeys().clear();

						sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getOffLockKeys().size());
						for (final int c : input.getOffLockKeys())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + c);
						input.getOffLockKeys().clear();

						final byte[] sendBuf = sw.toString().getBytes("ASCII");

						final DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, clientIPAddress,
								clientPort);
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
								main.scheduleStatusBarText(rb.getString("STATUS_LISTENING") + port);
							}
						} else
							counter++;
					}

					break;
				}
			}
		} catch (final BindException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(main.getFrame(),
					rb.getString("COULD_NOT_OPEN_SOCKET_DIALOG_TEXT_PREFIX") + port
							+ rb.getString("COULD_NOT_OPEN_SOCKET_DIALOG_TEXT_SUFFIX"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		} catch (final SocketException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(main.getFrame(), rb.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		} finally {
			deInit();
		}
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	@Override
	public void stopOutput() {
		super.stopOutput();
		serverSocket.close();
	}

}

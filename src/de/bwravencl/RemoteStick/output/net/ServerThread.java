/* Copyright (C) 2015  Matteo Hausner
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

package de.bwravencl.RemoteStick.output.net;

import java.io.IOException;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

import de.bwravencl.RemoteStick.gui.Main;
import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.KeyStroke;
import de.bwravencl.RemoteStick.output.OutputThread;

public class ServerThread extends OutputThread {

	public static final int DEFAULT_PORT = 28789;
	public static final int DEFAULT_CLIENT_TIMEOUT = 1000;

	private static final int PROTOCOL_VERSION = 1;
	private static final String PROTOCOL_MESSAGE_DELIMITER = ":";
	private static final String PROTOCOL_MESSAGE_CLIENT_HELLO = "CLIENT_HELLO";
	private static final String PROTOCOL_MESSAGE_SERVER_HELLO = "SERVER_HELLO";
	private static final String PROTOCOL_MESSAGE_UPDATE = "UPDATE";
	private static final String PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE = PROTOCOL_MESSAGE_UPDATE + "_ALIVE";
	private static final String PROTOCOL_MESSAGE_CLIENT_ALIVE = "CLIENT_ALIVE";

	private static final int REQUEST_ALIVE_INTERVAL = 100;

	private enum ServerState {
		Listening, Connected
	}

	private int port = DEFAULT_PORT;
	private int clientTimeout = DEFAULT_CLIENT_TIMEOUT;
	private ServerState serverState = ServerState.Listening;
	private DatagramSocket serverSocket = null;
	private InetAddress clientIPAddress = null;

	public ServerThread(Main main, Input input) {
		super(main, input);
	}

	@Override
	public void run() {
		DatagramPacket receivePacket = null;
		String message = null;
		long counter = 0;

		try {
			serverSocket = new DatagramSocket(port);
			final byte[] receiveBuf = new byte[1024];

			setListeningStatusbarText();

			while (true) {
				switch (serverState) {
				case Listening:
					counter = 0;
					receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
					serverSocket.setSoTimeout(0);
					serverSocket.receive(receivePacket);
					clientIPAddress = receivePacket.getAddress();
					message = new String(receivePacket.getData(), 0, receivePacket.getLength());

					if (message.startsWith(PROTOCOL_MESSAGE_CLIENT_HELLO)) {
						final String[] messageParts = message.split(PROTOCOL_MESSAGE_DELIMITER);

						if (messageParts.length == 4) {
							minAxisValue = Integer.parseInt(messageParts[1]);
							maxAxisValue = Integer.parseInt(messageParts[2]);
							setnButtons(Integer.parseInt(messageParts[3]));

							StringWriter sw = new StringWriter();
							sw.append(PROTOCOL_MESSAGE_SERVER_HELLO);
							sw.append(PROTOCOL_MESSAGE_DELIMITER);
							sw.append(String.valueOf(PROTOCOL_VERSION));
							sw.append(PROTOCOL_MESSAGE_DELIMITER);
							sw.append(String.valueOf(updateRate));

							final byte[] sendBuf = sw.toString().getBytes("ASCII");
							final DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length,
									clientIPAddress, port);
							serverSocket.send(sendPacket);

							serverState = ServerState.Connected;
							main.setStatusbarText(
									rb.getString("STATUS_CONNECTED_WITH") + clientIPAddress.getCanonicalHostName());
						}
					}
					break;
				case Connected:
					Thread.sleep(updateRate);

					StringWriter sw = new StringWriter();
					boolean doAliveCheck = false;
					if (counter % REQUEST_ALIVE_INTERVAL == 0) {
						sw.append(PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE);
						doAliveCheck = true;
					} else
						sw.append(PROTOCOL_MESSAGE_UPDATE);
					sw.append(PROTOCOL_MESSAGE_DELIMITER + counter);

					input.poll();

					for (int v : input.getAxis().values())
						sw.append(PROTOCOL_MESSAGE_DELIMITER + v);

					for (boolean v : input.getButtons())
						sw.append(PROTOCOL_MESSAGE_DELIMITER + v);

					sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getCursorDeltaX() + PROTOCOL_MESSAGE_DELIMITER
							+ input.getCursorDeltaY());

					input.setCursorDeltaX(0);
					input.setCursorDeltaY(0);

					sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getScrollClicks());

					input.setScrollClicks(0);

					sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getDownKeyCodes().size());
					for (int k : input.getDownKeyCodes())
						sw.append(PROTOCOL_MESSAGE_DELIMITER + k);

					sw.append(PROTOCOL_MESSAGE_DELIMITER + input.getDownUpKeyStrokes().size());

					for (KeyStroke ks : input.getDownUpKeyStrokes()) {
						sw.append(PROTOCOL_MESSAGE_DELIMITER + ks.getModifierCodes().length);
						for (int k : ks.getModifierCodes())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + k);

						sw.append(PROTOCOL_MESSAGE_DELIMITER + ks.getKeyCodes().length);
						for (int k : ks.getKeyCodes())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + k);
					}

					input.getDownUpKeyStrokes().clear();

					final byte[] sendBuf = sw.toString().getBytes("ASCII");
					final DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, clientIPAddress,
							port);
					serverSocket.send(sendPacket);

					if (doAliveCheck) {
						receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
						serverSocket.setSoTimeout(clientTimeout);
						try {
							serverSocket.receive(receivePacket);

							if (clientIPAddress.equals(receivePacket.getAddress())) {
								message = new String(receivePacket.getData(), 0, receivePacket.getLength());

								if (PROTOCOL_MESSAGE_CLIENT_ALIVE.equals(message))
									counter++;
							}
						} catch (SocketTimeoutException e) {
							serverState = ServerState.Listening;

							main.setStatusbarText(rb.getString("STATUS_TIMEOUT"));
							new Timer().schedule(new TimerTask() {
								@Override
								public void run() {
									setListeningStatusbarText();
								}
							}, 5000L);
						}
					} else
						counter++;

					break;
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			closeSocket();
		}
	}

	private void setListeningStatusbarText() {
		main.setStatusbarText(rb.getString("STATUS_LISTENING") + port);
	}

	public void closeSocket() {
		if (serverSocket != null)
			serverSocket.close();

		main.setStatusbarText(rb.getString("STATUS_SOCKET_CLOSED"));
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getClientTimeout() {
		return clientTimeout;
	}

	public void setClientTimeout(int clientTimeout) {
		this.clientTimeout = clientTimeout;
	}

}

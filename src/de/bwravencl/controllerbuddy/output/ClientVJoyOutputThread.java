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

package de.bwravencl.controllerbuddy.output;

import java.awt.MouseInfo;
import java.awt.Point;
import java.io.IOException;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashSet;

import javax.swing.JOptionPane;

import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.LONG;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;

public class ClientVJoyOutputThread extends VJoyOutputThread {

	public static final String DEFAULT_HOST = "127.0.0.1";

	private static final int N_CONNECTION_RETRIES = 10;

	private enum ClientState {
		Connecting, Connected
	}

	private String host = DEFAULT_HOST;
	private int port = ServerOutputThread.DEFAULT_PORT;
	private int timeout = ServerOutputThread.DEFAULT_TIMEOUT;

	private ClientState clientState = ClientState.Connecting;
	private InetAddress hostAddress;
	private DatagramSocket clientSocket;
	private final byte[] receiveBuf = new byte[1024];
	private long counter = -1;

	public ClientVJoyOutputThread(Main main, Input input) {
		super(main, input);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	public void run() {
		try {
			if (init()) {
				hostAddress = InetAddress.getByName(host);
				clientSocket = new DatagramSocket(port + 1);
				clientSocket.setSoTimeout(timeout);

				while (run) {
					if (readInput())
						writeOutput();
				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(main.getFrame(),
					rb.getString("INVALID_HOST_ADDRESS_DIALOG_TEXT_PREFIX") + host
							+ rb.getString("INVALID_HOST_ADDRESS_DIALOG_TEXT_SUFFIX"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(main.getFrame(), rb.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		} finally {
			if (clientSocket != null)
				clientSocket.close();
			deInit();
		}
	}

	@Override
	protected boolean readInput() throws IOException {
		boolean retVal = false;

		switch (clientState) {
		case Connecting:
			main.setStatusbarText(rb.getString("STATUS_CONNECTING_TO_HOST_PART_1") + host
					+ rb.getString("STATUS_CONNECTING_TO_HOST_PART_2") + port
					+ rb.getString("STATUS_CONNECTING_TO_HOST_PART_3"));

			final StringWriter sw = new StringWriter();
			sw.append(ServerOutputThread.PROTOCOL_MESSAGE_CLIENT_HELLO);
			sw.append(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);
			sw.append(String.valueOf(minAxisValue));
			sw.append(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);
			sw.append(String.valueOf(maxAxisValue));
			sw.append(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);
			sw.append(String.valueOf(nButtons));

			final byte[] sendBuf = sw.toString().getBytes("ASCII");
			final DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, hostAddress, port);

			boolean success = false;
			int retry = N_CONNECTION_RETRIES;
			do {
				clientSocket.send(sendPacket);

				final DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
				try {
					clientSocket.receive(receivePacket);
					String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

					if (message.startsWith(ServerOutputThread.PROTOCOL_MESSAGE_SERVER_HELLO)) {
						final String[] messageParts = message.split(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);
						int serverProtocolVersion = Integer.parseInt(messageParts[1]);
						if (ServerOutputThread.PROTOCOL_VERSION != serverProtocolVersion) {
							JOptionPane.showMessageDialog(main.getFrame(),
									rb.getString("PROTOCOL_VERSION_MISMATCH_DIALOG_TEXT_PART_1")
											+ ServerOutputThread.PROTOCOL_VERSION
											+ rb.getString("PROTOCOL_VERSION_MISMATCH_DIALOG_TEXT_PART_2")
											+ serverProtocolVersion
											+ rb.getString("PROTOCOL_VERSION_MISMATCH_DIALOG_TEXT_PART_3"),
									rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
							retry = -1;
						} else {
							updateRate = Long.parseLong(messageParts[2]);
							success = true;
						}
					} else {
						retry--;
						main.setStatusbarText(
								rb.getString("STATUS_INVALID_MESSAGE_RETRYING_PART_1") + (N_CONNECTION_RETRIES - retry)
										+ rb.getString("STATUS_INVALID_MESSAGE_RETRYING_PART_2") + N_CONNECTION_RETRIES
										+ rb.getString("STATUS_INVALID_MESSAGE_RETRYING_PART_3"));
					}
				} catch (SocketTimeoutException e) {
					e.printStackTrace();
					retry--;
					main.setStatusbarText(rb.getString("STATUS_TIMEOUT_RETRYING_PART_1")
							+ (N_CONNECTION_RETRIES - retry) + rb.getString("STATUS_TIMEOUT_RETRYING_PART_2")
							+ N_CONNECTION_RETRIES + rb.getString("STATUS_TIMEOUT_RETRYING_PART_3"));
				}
			} while (!success && retry > 0 && run);

			if (success) {
				clientState = ClientState.Connected;
				main.setStatusbarText(rb.getString("STATUS_CONNECTED_TO_PART_1") + host
						+ rb.getString("STATUS_CONNECTED_TO_PART_2") + port + rb.getString("STATUS_CONNECTED_TO_PART_3")
						+ updateRate + rb.getString("STATUS_CONNECTED_TO_PART_4"));
			} else {
				if (retry != -1 && run)
					JOptionPane.showMessageDialog(main.getFrame(),
							rb.getString("COULD_NOT_CONNECT_DIALOG_TEXT_PREFIX") + N_CONNECTION_RETRIES
									+ rb.getString("COULD_NOT_CONNECT_DIALOG_TEXT_SUFFIX"),
							rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				run = false;
			}

			break;
		case Connected:
			try {
				final DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
				clientSocket.receive(receivePacket);
				final String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

				if (message.startsWith(ServerOutputThread.PROTOCOL_MESSAGE_UPDATE)) {
					final String[] messageParts = message.split(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);

					final long newCounter = Long.parseLong(messageParts[1]);
					if (newCounter > counter) {
						axisX = new LONG(Integer.parseInt(messageParts[2]));
						axisY = new LONG(Integer.parseInt(messageParts[3]));
						axisZ = new LONG(Integer.parseInt(messageParts[4]));
						axisRX = new LONG(Integer.parseInt(messageParts[5]));
						axisRY = new LONG(Integer.parseInt(messageParts[6]));
						axisRZ = new LONG(Integer.parseInt(messageParts[7]));
						axisS0 = new LONG(Integer.parseInt(messageParts[8]));
						axisS1 = new LONG(Integer.parseInt(messageParts[9]));

						buttons = new BOOL[nButtons];
						for (int i = 1; i <= nButtons; i++) {
							final boolean button = Boolean.parseBoolean(messageParts[9 + i]);
							buttons[i - 1] = new BOOL(button ? 1L : 0L);
						}

						final Point currentPosition = MouseInfo.getPointerInfo().getLocation();
						cursorX = currentPosition.x + Integer.parseInt(messageParts[10 + nButtons]);
						input.setCursorDeltaX(0);
						cursorY = currentPosition.y + Integer.parseInt(messageParts[11 + nButtons]);
						input.setCursorDeltaY(0);

						final int nDownMouseButtons = Integer.parseInt(messageParts[12 + nButtons]);
						newDownMouseButtons = new HashSet<Integer>(nDownMouseButtons);
						for (int i = 1; i <= nDownMouseButtons; i++)
							newDownMouseButtons.add(Integer.parseInt(messageParts[12 + nButtons + i]));
						oldDownMouseButtons.removeAll(newDownMouseButtons);
						newUpMouseButtons = new HashSet<Integer>(oldDownMouseButtons);
						oldDownMouseButtons.clear();
						oldDownMouseButtons.addAll(newDownMouseButtons);

						final int nDownUpMouseButtons = Integer
								.parseInt(messageParts[13 + nButtons + nDownMouseButtons]);
						downUpMouseButtons = new HashSet<Integer>(nDownUpMouseButtons);
						for (int i = 1; i <= nDownUpMouseButtons; i++) {
							final int b = Integer.parseInt(messageParts[13 + nButtons + nDownMouseButtons + i]);
							System.out.println("Added d/u mb: " + b);
							downUpMouseButtons.add(b);
						}

						final int nDownKeyCodes = Integer
								.parseInt(messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons]);
						newDownKeyCodes = new HashSet<Integer>(nDownKeyCodes);
						for (int i = 1; i <= nDownKeyCodes; i++)
							newDownKeyCodes.add(Integer.parseInt(
									messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons + i]));
						oldDownKeyCodes.removeAll(newDownKeyCodes);
						newUpKeyCodes = new HashSet<Integer>(oldDownKeyCodes);
						oldDownKeyCodes.clear();
						oldDownKeyCodes.addAll(newDownKeyCodes);

						int nDownUpKeyStrokes = Integer.parseInt(
								messageParts[15 + nButtons + nDownMouseButtons + nDownUpMouseButtons + nDownKeyCodes]);
						downUpKeyStrokes = new HashSet<KeyStroke>(nDownUpKeyStrokes);
						for (int i = 1; i <= nDownUpKeyStrokes; i++) {
							final KeyStroke keyStroke = new KeyStroke();
							downUpKeyStrokes.add(keyStroke);

							final int nDownUpModifierCodes = Integer.parseInt(messageParts[15 + nButtons
									+ nDownMouseButtons + nDownUpMouseButtons + nDownKeyCodes + i]);
							final Integer[] modifierCodes = new Integer[nDownUpModifierCodes];
							for (int j = 1; j <= nDownUpModifierCodes; j++) {
								final int k = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownKeyCodes + i + j]);
								modifierCodes[j - 1] = k;
							}
							keyStroke.setModifierCodes(modifierCodes);

							final int nDownUpKeyCodes = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
									+ nDownUpMouseButtons + nDownKeyCodes + nDownUpModifierCodes + i]);
							final Integer[] keyCodes = new Integer[nDownUpKeyCodes];
							for (int j = 1; j <= nDownUpKeyCodes; j++) {
								final int k = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownKeyCodes + nDownUpModifierCodes + i + j]);
								keyCodes[j - 1] = k;
							}
							keyStroke.setKeyCodes(keyCodes);

							final int spacing = nDownUpModifierCodes + nDownUpKeyCodes + 1;
							nDownUpKeyStrokes += spacing;
							i += spacing;
						}

						scrollClicks = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyCodes + nDownUpKeyStrokes]);

						counter = newCounter;
						retVal = true;
					}
				}

				if (message.startsWith(ServerOutputThread.PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE)) {
					final byte[] sendBuf1 = ServerOutputThread.PROTOCOL_MESSAGE_CLIENT_ALIVE.getBytes("ASCII");
					final DatagramPacket sendPacket1 = new DatagramPacket(sendBuf1, sendBuf1.length, hostAddress, port);
					clientSocket.send(sendPacket1);
				}
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(main.getFrame(), rb.getString("CONNECTION_LOST_DIALOG_TEXT"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			}
			break;
		}

		return retVal;
	}

	@Override
	protected void deInit() {
		super.deInit();
		main.stopClient();
	}

}

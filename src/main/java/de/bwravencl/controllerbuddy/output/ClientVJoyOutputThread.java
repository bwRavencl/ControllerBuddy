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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.LONG;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;

public class ClientVJoyOutputThread extends VJoyOutputThread {

	private enum ClientState {
		Connecting, Connected
	}

	public static final String DEFAULT_HOST = "127.0.0.1";
	private static final int N_CONNECTION_RETRIES = 10;

	private String host = DEFAULT_HOST;
	private int port = ServerOutputThread.DEFAULT_PORT;
	private int timeout = ServerOutputThread.DEFAULT_TIMEOUT;
	private ClientState clientState = ClientState.Connecting;
	private InetAddress hostAddress;
	private DatagramSocket clientSocket;
	private final byte[] receiveBuf = new byte[1024];
	private long counter = -1;

	public ClientVJoyOutputThread(final Main main, final Input input) {
		super(main, input);
	}

	@Override
	protected void deInit() {
		super.deInit();
		main.stopClient(false);

		if (restart)
			main.restartLast();
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public int getTimeout() {
		return timeout;
	}

	@Override
	protected boolean readInput() throws IOException {
		boolean retVal = false;

		switch (clientState) {
		case Connecting:
			main.setStatusBarText(rb.getString("STATUS_CONNECTING_TO_HOST_PART_1") + host
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
					final String message = new String(receivePacket.getData(), 0, receivePacket.getLength(),
							StandardCharsets.US_ASCII);

					if (message.startsWith(ServerOutputThread.PROTOCOL_MESSAGE_SERVER_HELLO)) {
						final String[] messageParts = message.split(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);
						final int serverProtocolVersion = Integer.parseInt(messageParts[1]);
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
							pollInterval = Long.parseLong(messageParts[2]);
							success = true;
						}
					} else {
						retry--;
						main.setStatusBarText(
								rb.getString("STATUS_INVALID_MESSAGE_RETRYING_PART_1") + (N_CONNECTION_RETRIES - retry)
										+ rb.getString("STATUS_INVALID_MESSAGE_RETRYING_PART_2") + N_CONNECTION_RETRIES
										+ rb.getString("STATUS_INVALID_MESSAGE_RETRYING_PART_3"));
					}
				} catch (final SocketTimeoutException e) {
					e.printStackTrace();
					retry--;
					main.setStatusBarText(rb.getString("STATUS_TIMEOUT_RETRYING_PART_1")
							+ (N_CONNECTION_RETRIES - retry) + rb.getString("STATUS_TIMEOUT_RETRYING_PART_2")
							+ N_CONNECTION_RETRIES + rb.getString("STATUS_TIMEOUT_RETRYING_PART_3"));
				}
			} while (!success && retry > 0 && run);

			if (success) {
				clientState = ClientState.Connected;
				main.setStatusBarText(rb.getString("STATUS_CONNECTED_TO_PART_1") + host
						+ rb.getString("STATUS_CONNECTED_TO_PART_2") + port + rb.getString("STATUS_CONNECTED_TO_PART_3")
						+ pollInterval + rb.getString("STATUS_CONNECTED_TO_PART_4"));
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
				final String message = new String(receivePacket.getData(), 0, receivePacket.getLength(),
						StandardCharsets.US_ASCII);

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

						cursorDeltaX = Integer.parseInt(messageParts[10 + nButtons]);
						cursorDeltaY = Integer.parseInt(messageParts[11 + nButtons]);

						final int nDownMouseButtons = Integer.parseInt(messageParts[12 + nButtons]);
						final Set<Integer> sourceDownMouseButtons = new HashSet<>(nDownMouseButtons);
						for (int i = 1; i <= nDownMouseButtons; i++)
							sourceDownMouseButtons.add(Integer.parseInt(messageParts[12 + nButtons + i]));
						updateOutputSets(sourceDownMouseButtons, oldDownMouseButtons, newUpMouseButtons,
								newDownMouseButtons, false);

						downUpMouseButtons.clear();
						final int nDownUpMouseButtons = Integer
								.parseInt(messageParts[13 + nButtons + nDownMouseButtons]);
						for (int i = 1; i <= nDownUpMouseButtons; i++) {
							final int b = Integer.parseInt(messageParts[13 + nButtons + nDownMouseButtons + i]);
							downUpMouseButtons.add(b);
						}

						final Set<Integer> sourceModifiers = new HashSet<>();
						final Set<Integer> sourceNormalKeys = new HashSet<>();
						int nDownKeyStrokes = Integer
								.parseInt(messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons]);
						for (int i = 1; i <= nDownKeyStrokes; i++) {
							final int nDownModifierCodes = Integer.parseInt(
									messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons + i]);
							for (int j = 1; j <= nDownModifierCodes; j++) {
								final int k = Integer.parseInt(
										messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons + i + j]);
								sourceModifiers.add(k);
							}

							final int nDownKeyCodes = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
									+ nDownUpMouseButtons + nDownModifierCodes + i]);
							for (int j = 1; j <= nDownKeyCodes; j++) {
								final int k = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownModifierCodes + i + j]);
								sourceNormalKeys.add(k);
							}

							final int spacing = nDownModifierCodes + nDownKeyCodes + 1;
							nDownKeyStrokes += spacing;
							i += spacing;
						}
						updateOutputSets(sourceModifiers, oldDownModifiers, newUpModifiers, newDownModifiers, false);
						updateOutputSets(sourceNormalKeys, oldDownNormalKeys, newUpNormalKeys, newDownNormalKeys, true);

						downUpKeyStrokes.clear();
						int nDownUpKeyStrokes = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes]);
						for (int i = 1; i <= nDownUpKeyStrokes; i++) {
							final KeyStroke keyStroke = new KeyStroke();

							final int nDownUpModifierCodes = Integer.parseInt(messageParts[15 + nButtons
									+ nDownMouseButtons + nDownUpMouseButtons + nDownKeyStrokes + i]);
							final Integer[] modifierCodes = new Integer[nDownUpModifierCodes];
							for (int j = 1; j <= nDownUpModifierCodes; j++) {
								final int k = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownKeyStrokes + i + j]);
								modifierCodes[j - 1] = k;
							}
							keyStroke.setModifierCodes(modifierCodes);

							final int nDownUpKeyCodes = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
									+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpModifierCodes + i]);
							final Integer[] keyCodes = new Integer[nDownUpKeyCodes];
							for (int j = 1; j <= nDownUpKeyCodes; j++) {
								final int k = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpModifierCodes + i + j]);
								keyCodes[j - 1] = k;
							}
							keyStroke.setKeyCodes(keyCodes);
							downUpKeyStrokes.add(keyStroke);

							final int spacing = nDownUpModifierCodes + nDownUpKeyCodes + 1;
							nDownUpKeyStrokes += spacing;
							i += spacing;
						}

						scrollClicks = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes]);

						final int nOnLockKeys = Integer.parseInt(messageParts[17 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes]);
						for (int i = 1; i <= nOnLockKeys; i++)
							onLockKeys.add(i);

						final int nOffLockKeys = Integer.parseInt(messageParts[18 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes + nOnLockKeys]);
						for (int i = 1; i <= nOffLockKeys; i++)
							offLockKeys.add(i);

						counter = newCounter;
						retVal = true;
					}
				}

				if (message.startsWith(ServerOutputThread.PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE)) {
					final byte[] sendBuf1 = ServerOutputThread.PROTOCOL_MESSAGE_CLIENT_ALIVE.getBytes("ASCII");
					final DatagramPacket sendPacket1 = new DatagramPacket(sendBuf1, sendBuf1.length, hostAddress, port);
					clientSocket.send(sendPacket1);
				}
			} catch (final SocketTimeoutException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(main.getFrame(), rb.getString("CONNECTION_LOST_DIALOG_TEXT"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				run = false;
			}
			break;
		}

		return retVal;
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
		} catch (final UnknownHostException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(main.getFrame(),
					rb.getString("INVALID_HOST_ADDRESS_DIALOG_TEXT_PREFIX") + host
							+ rb.getString("INVALID_HOST_ADDRESS_DIALOG_TEXT_SUFFIX"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		} catch (final IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(main.getFrame(), rb.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		} finally {
			if (clientSocket != null)
				clientSocket.close();
			deInit();
		}
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

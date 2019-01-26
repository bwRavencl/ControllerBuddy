/* Copyright (C) 2019  Matteo Hausner
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
import java.lang.System.Logger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.LONG;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.version.VersionUtils;

public class ClientVJoyOutputThread extends VJoyOutputThread {

	private enum ClientState {
		Connecting, Connected
	}

	private static final Logger log = System.getLogger(ClientVJoyOutputThread.class.getName());

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
	boolean readInput() throws IOException {
		boolean retVal = false;

		switch (clientState) {
		case Connecting:
			SwingUtilities.invokeLater(() -> {
				main.setStatusBarText(rb.getString("STATUS_CONNECTING_TO_HOST_PART_1") + host
						+ rb.getString("STATUS_CONNECTING_TO_HOST_PART_2") + port
						+ rb.getString("STATUS_CONNECTING_TO_HOST_PART_3"));
			});

			final var sw = new StringWriter();
			sw.append(ServerOutputThread.PROTOCOL_MESSAGE_CLIENT_HELLO);
			sw.append(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);
			sw.append(String.valueOf(minAxisValue));
			sw.append(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);
			sw.append(String.valueOf(maxAxisValue));
			sw.append(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);
			sw.append(String.valueOf(nButtons));

			final var sendBuf = sw.toString().getBytes("ASCII");
			final var sendPacket = new DatagramPacket(sendBuf, sendBuf.length, hostAddress, port);

			var success = false;
			var retry = N_CONNECTION_RETRIES;
			do {
				clientSocket.send(sendPacket);

				final var receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
				try {
					clientSocket.receive(receivePacket);
					final var message = new String(receivePacket.getData(), 0, receivePacket.getLength(),
							StandardCharsets.US_ASCII);

					if (message.startsWith(ServerOutputThread.PROTOCOL_MESSAGE_SERVER_HELLO)) {
						final var messageParts = message.split(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);
						final var serverProtocolVersion = messageParts[1];
						final var versionsComparisonResult = VersionUtils.compareVersions(serverProtocolVersion);
						if (versionsComparisonResult.isEmpty() || versionsComparisonResult.get() != 0) {
							SwingUtilities.invokeLater(() -> {
								JOptionPane.showMessageDialog(main.getFrame(),
										rb.getString("PROTOCOL_VERSION_MISMATCH_DIALOG_TEXT_PART_1")
												+ VersionUtils.getMajorAndMinorVersion()
												+ rb.getString("PROTOCOL_VERSION_MISMATCH_DIALOG_TEXT_PART_2")
												+ serverProtocolVersion
												+ rb.getString("PROTOCOL_VERSION_MISMATCH_DIALOG_TEXT_PART_3"),
										rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
							});
							retry = -1;
						} else {
							pollInterval = Long.parseLong(messageParts[2]);
							success = true;
						}
					} else {
						retry--;
						final var finalRetry = retry;
						SwingUtilities.invokeLater(() -> {
							main.setStatusBarText(rb.getString("STATUS_INVALID_MESSAGE_RETRYING_PART_1")
									+ (N_CONNECTION_RETRIES - finalRetry)
									+ rb.getString("STATUS_INVALID_MESSAGE_RETRYING_PART_2") + N_CONNECTION_RETRIES
									+ rb.getString("STATUS_INVALID_MESSAGE_RETRYING_PART_3"));
						});
					}
				} catch (final SocketTimeoutException e) {
					log.log(Logger.Level.INFO, e.getMessage(), e);
					retry--;
					final var finalRetry = retry;
					SwingUtilities.invokeLater(() -> {
						main.setStatusBarText(rb.getString("STATUS_TIMEOUT_RETRYING_PART_1")
								+ (N_CONNECTION_RETRIES - finalRetry) + rb.getString("STATUS_TIMEOUT_RETRYING_PART_2")
								+ N_CONNECTION_RETRIES + rb.getString("STATUS_TIMEOUT_RETRYING_PART_3"));
					});
				}
			} while (!success && retry > 0 && !Thread.currentThread().isInterrupted());

			if (success) {
				clientState = ClientState.Connected;
				SwingUtilities.invokeLater(() -> {
					main.setStatusBarText(rb.getString("STATUS_CONNECTED_TO_PART_1") + host
							+ rb.getString("STATUS_CONNECTED_TO_PART_2") + port
							+ rb.getString("STATUS_CONNECTED_TO_PART_3") + pollInterval
							+ rb.getString("STATUS_CONNECTED_TO_PART_4"));
				});
			} else {
				if (retry != -1 && !Thread.currentThread().isInterrupted())
					SwingUtilities.invokeLater(() -> {
						JOptionPane.showMessageDialog(main.getFrame(),
								rb.getString("COULD_NOT_CONNECT_DIALOG_TEXT_PREFIX") + N_CONNECTION_RETRIES
										+ rb.getString("COULD_NOT_CONNECT_DIALOG_TEXT_SUFFIX"),
								rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
					});
				interrupt();
			}

			break;
		case Connected:
			try {
				final var receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
				clientSocket.receive(receivePacket);
				final var message = new String(receivePacket.getData(), 0, receivePacket.getLength(),
						StandardCharsets.US_ASCII);

				if (message.startsWith(ServerOutputThread.PROTOCOL_MESSAGE_UPDATE)) {
					final var messageParts = message.split(ServerOutputThread.PROTOCOL_MESSAGE_DELIMITER);

					final var newCounter = Long.parseLong(messageParts[1]);
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
						for (var i = 1; i <= nButtons; i++) {
							final var b = Boolean.parseBoolean(messageParts[9 + i]);
							buttons[i - 1] = new BOOL(b ? 1L : 0L);
						}

						cursorDeltaX = Integer.parseInt(messageParts[10 + nButtons]);
						cursorDeltaY = Integer.parseInt(messageParts[11 + nButtons]);

						final var nDownMouseButtons = Integer.parseInt(messageParts[12 + nButtons]);
						final var sourceDownMouseButtons = new HashSet<Integer>(nDownMouseButtons);
						for (var i = 1; i <= nDownMouseButtons; i++)
							sourceDownMouseButtons.add(Integer.parseInt(messageParts[12 + nButtons + i]));
						updateOutputSets(sourceDownMouseButtons, oldDownMouseButtons, newUpMouseButtons,
								newDownMouseButtons, false);

						downUpMouseButtons.clear();
						final var nDownUpMouseButtons = Integer
								.parseInt(messageParts[13 + nButtons + nDownMouseButtons]);
						for (var i = 1; i <= nDownUpMouseButtons; i++) {
							final var b = Integer.parseInt(messageParts[13 + nButtons + nDownMouseButtons + i]);
							downUpMouseButtons.add(b);
						}

						final var sourceModifiers = new HashSet<Integer>();
						final var sourceNormalKeys = new HashSet<Integer>();
						int nDownKeyStrokes = Integer
								.parseInt(messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons]);
						for (var i = 1; i <= nDownKeyStrokes; i++) {
							final var nDownModifierCodes = Integer.parseInt(
									messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons + i]);
							for (var j = 1; j <= nDownModifierCodes; j++) {
								final var k = Integer.parseInt(
										messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons + i + j]);
								sourceModifiers.add(k);
							}

							final var nDownKeyCodes = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
									+ nDownUpMouseButtons + nDownModifierCodes + i]);
							for (var j = 1; j <= nDownKeyCodes; j++) {
								final var k = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownModifierCodes + i + j]);
								sourceNormalKeys.add(k);
							}

							final var spacing = nDownModifierCodes + nDownKeyCodes + 1;
							nDownKeyStrokes += spacing;
							i += spacing;
						}
						updateOutputSets(sourceModifiers, oldDownModifiers, newUpModifiers, newDownModifiers, false);
						updateOutputSets(sourceNormalKeys, oldDownNormalKeys, newUpNormalKeys, newDownNormalKeys, true);

						downUpKeyStrokes.clear();
						var nDownUpKeyStrokes = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes]);
						for (var i = 1; i <= nDownUpKeyStrokes; i++) {
							final var keyStroke = new KeyStroke();

							final var nDownUpModifierCodes = Integer.parseInt(messageParts[15 + nButtons
									+ nDownMouseButtons + nDownUpMouseButtons + nDownKeyStrokes + i]);
							final var modifierCodes = new Integer[nDownUpModifierCodes];
							for (var j = 1; j <= nDownUpModifierCodes; j++) {
								final var k = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownKeyStrokes + i + j]);
								modifierCodes[j - 1] = k;
							}
							keyStroke.setModifierCodes(modifierCodes);

							final var nDownUpKeyCodes = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
									+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpModifierCodes + i]);
							final var keyCodes = new Integer[nDownUpKeyCodes];
							for (var j = 1; j <= nDownUpKeyCodes; j++) {
								final var k = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpModifierCodes + i + j]);
								keyCodes[j - 1] = k;
							}
							keyStroke.setKeyCodes(keyCodes);
							downUpKeyStrokes.add(keyStroke);

							final var spacing = nDownUpModifierCodes + nDownUpKeyCodes + 1;
							nDownUpKeyStrokes += spacing;
							i += spacing;
						}

						scrollClicks = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes]);

						final var nOnLockKeys = Integer.parseInt(messageParts[17 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes]);
						for (var i = 1; i <= nOnLockKeys; i++)
							onLockKeys.add(i);

						final var nOffLockKeys = Integer.parseInt(messageParts[18 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes + nOnLockKeys]);
						for (var i = 1; i <= nOffLockKeys; i++)
							offLockKeys.add(i);

						counter = newCounter;
						retVal = true;
					}
				}

				if (message.startsWith(ServerOutputThread.PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE)) {
					final var sendBuf1 = ServerOutputThread.PROTOCOL_MESSAGE_CLIENT_ALIVE.getBytes("ASCII");
					final var sendPacket1 = new DatagramPacket(sendBuf1, sendBuf1.length, hostAddress, port);
					clientSocket.send(sendPacket1);
				}
			} catch (final SocketTimeoutException e) {
				log.log(Logger.Level.INFO, e.getMessage(), e);
				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(main.getFrame(), rb.getString("CONNECTION_LOST_DIALOG_TEXT"),
							rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				});
				interrupt();
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

				while (!Thread.currentThread().isInterrupted())
					if (readInput())
						writeOutput();
			}
		} catch (final UnknownHostException e) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(main.getFrame(),
						rb.getString("INVALID_HOST_ADDRESS_DIALOG_TEXT_PREFIX") + host
								+ rb.getString("INVALID_HOST_ADDRESS_DIALOG_TEXT_SUFFIX"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			});
		} catch (final IOException e) {
			log.log(Logger.Level.ERROR, e.getMessage(), e);
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(main.getFrame(), rb.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			});
		} catch (final InterruptedException e) {
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

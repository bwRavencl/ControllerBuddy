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
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.ScanCode;
import de.bwravencl.controllerbuddy.version.VersionUtils;

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

	@Override
	Logger getLogger() {
		return log;
	}

	@Override
	boolean readInput() throws IOException {
		super.readInput();

		var retVal = false;

		switch (clientState) {
		case Connecting -> {
			log.log(Level.INFO, "Connecting to " + host + ":" + port);
			EventQueue.invokeLater(() -> {
				main.setStatusBarText(
						MessageFormat.format(Main.strings.getString("STATUS_CONNECTING_TO_HOST"), host, port));
			});

			final var sb = new StringBuilder();
			sb.append(ServerRunMode.PROTOCOL_MESSAGE_CLIENT_HELLO);
			sb.append(ServerRunMode.PROTOCOL_MESSAGE_DELIMITER);
			sb.append(String.valueOf(minAxisValue));
			sb.append(ServerRunMode.PROTOCOL_MESSAGE_DELIMITER);
			sb.append(String.valueOf(maxAxisValue));
			sb.append(ServerRunMode.PROTOCOL_MESSAGE_DELIMITER);
			sb.append(String.valueOf(nButtons));

			final var helloBuf = sb.toString().getBytes("ASCII");
			final var helloPacket = new DatagramPacket(helloBuf, helloBuf.length, hostAddress, port);

			var success = false;
			var retry = N_CONNECTION_RETRIES;
			do {
				clientSocket.send(helloPacket);

				final var receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
				try {
					clientSocket.receive(receivePacket);
					final var message = new String(receivePacket.getData(), 0, receivePacket.getLength(),
							StandardCharsets.US_ASCII);

					if (message.startsWith(ServerRunMode.PROTOCOL_MESSAGE_SERVER_HELLO)) {
						final var messageParts = message.split(ServerRunMode.PROTOCOL_MESSAGE_DELIMITER);
						final var serverProtocolVersion = messageParts[1];
						final var versionsComparisonResult = VersionUtils.compareVersions(serverProtocolVersion);
						if (versionsComparisonResult.isEmpty() || versionsComparisonResult.get() != 0) {
							final var clientVersion = VersionUtils.getMajorAndMinorVersion();
							log.log(Level.WARNING, "Protocol version mismatch: client " + clientVersion + " vs server "
									+ serverProtocolVersion);
							EventQueue.invokeLater(() -> {
								GuiUtils.showMessageDialog(main.getFrame(),
										MessageFormat.format(
												Main.strings.getString("PROTOCOL_VERSION_MISMATCH_DIALOG_TEXT"),
												clientVersion, serverProtocolVersion),
										Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
							});
							retry = -1;
						} else {
							pollInterval = Long.parseLong(messageParts[2]);
							success = true;
						}
					} else {
						retry--;
						final var finalRetry = retry;
						EventQueue.invokeLater(() -> {
							main.setStatusBarText(
									MessageFormat.format(Main.strings.getString("STATUS_INVALID_MESSAGE_RETRYING"),
											N_CONNECTION_RETRIES - finalRetry, N_CONNECTION_RETRIES));
						});
					}
				} catch (final SocketTimeoutException e) {
					log.log(Level.INFO, e.getMessage(), e);
					retry--;
					final var finalRetry = retry;
					EventQueue.invokeLater(() -> {
						main.setStatusBarText(MessageFormat.format(Main.strings.getString("STATUS_TIMEOUT_RETRYING"),
								N_CONNECTION_RETRIES - finalRetry, N_CONNECTION_RETRIES));
					});
				}
			} while (!success && retry > 0 && !Thread.currentThread().isInterrupted());

			if (success) {
				clientState = ClientState.Connected;
				log.log(Level.INFO, "Successfully connected");
				EventQueue.invokeLater(() -> {
					main.setStatusBarText(MessageFormat.format(Main.strings.getString("STATUS_CONNECTED_TO"), host,
							port, pollInterval));
				});
			} else {
				if (retry != -1 && !Thread.currentThread().isInterrupted()) {
					log.log(Level.INFO, "Could not connect after " + N_CONNECTION_RETRIES + " retries");
					EventQueue.invokeLater(() -> {
						GuiUtils.showMessageDialog(main.getFrame(),
								MessageFormat.format(Main.strings.getString("COULD_NOT_CONNECT_DIALOG_TEXT"),
										N_CONNECTION_RETRIES),
								Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
					});
				}

				forceStop = true;
				Thread.currentThread().interrupt();
			}
		}
		case Connected -> {
			try {
				final var receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
				clientSocket.receive(receivePacket);
				final var message = new String(receivePacket.getData(), 0, receivePacket.getLength(),
						StandardCharsets.US_ASCII);

				if (message.startsWith(ServerRunMode.PROTOCOL_MESSAGE_UPDATE)) {
					final var messageParts = message.split(ServerRunMode.PROTOCOL_MESSAGE_DELIMITER);

					final var newCounter = Long.parseLong(messageParts[1]);
					if (newCounter > counter) {
						final var inputAxisX = Integer.parseInt(messageParts[2]);
						axisX.setValue(inputAxisX);

						final var inputAxisY = Integer.parseInt(messageParts[3]);
						axisY.setValue(inputAxisY);

						final var inputAxisZ = Integer.parseInt(messageParts[4]);
						axisZ.setValue(inputAxisZ);

						final var inputAxisRX = Integer.parseInt(messageParts[5]);
						axisRX.setValue(inputAxisRX);

						final var inputAxisRY = Integer.parseInt(messageParts[6]);
						axisRY.setValue(inputAxisRY);

						final var inputAxisRZ = Integer.parseInt(messageParts[7]);
						axisRZ.setValue(inputAxisRZ);

						final var inputAxisS0 = Integer.parseInt(messageParts[8]);
						axisS0.setValue(inputAxisS0);

						final var inputAxisS1 = Integer.parseInt(messageParts[9]);
						axisS1.setValue(inputAxisS1);

						for (var i = 0; i < nButtons; i++) {
							final var buttonDown = Boolean.parseBoolean(messageParts[10 + i]);
							buttons[i].setValue(buttonDown ? 1 : 0);
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

						final var sourceModifiers = new HashSet<ScanCode>();
						final var sourceNormalKeys = new HashSet<ScanCode>();
						var nDownKeyStrokes = Integer
								.parseInt(messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons]);
						for (var i = 1; i <= nDownKeyStrokes; i++) {
							final var nDownModifierCodes = Integer.parseInt(
									messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons + i]);
							for (var j = 1; j <= nDownModifierCodes; j++) {
								final var k = Integer.parseInt(
										messageParts[14 + nButtons + nDownMouseButtons + nDownUpMouseButtons + i + j]);
								sourceModifiers.add(ScanCode.keyCodeToScanCodeMap.get(k));
							}

							final var nDownKeyCodes = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
									+ nDownUpMouseButtons + nDownModifierCodes + i]);
							for (var j = 1; j <= nDownKeyCodes; j++) {
								final var k = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownModifierCodes + i + j]);
								sourceNormalKeys.add(ScanCode.keyCodeToScanCodeMap.get(k));
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
							final var modifierCodes = new ScanCode[nDownUpModifierCodes];
							for (var j = 1; j <= nDownUpModifierCodes; j++) {
								final var k = Integer.parseInt(messageParts[15 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownKeyStrokes + i + j]);
								modifierCodes[j - 1] = ScanCode.keyCodeToScanCodeMap.get(k);
							}
							keyStroke.setModifierCodes(modifierCodes);

							final var nDownUpKeyCodes = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
									+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpModifierCodes + i]);
							final var keyCodes = new ScanCode[nDownUpKeyCodes];
							for (var j = 1; j <= nDownUpKeyCodes; j++) {
								final var k = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
										+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpModifierCodes + i + j]);
								keyCodes[j - 1] = ScanCode.keyCodeToScanCodeMap.get(k);
							}
							keyStroke.setKeyCodes(keyCodes);
							downUpKeyStrokes.add(keyStroke);

							final var spacing = nDownUpModifierCodes + nDownUpKeyCodes + 1;
							nDownUpKeyStrokes += spacing;
							i += spacing;
						}

						scrollClicks = Integer.parseInt(messageParts[16 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes]);

						onLockKeys.clear();
						final var nOnLockKeys = Integer.parseInt(messageParts[17 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes]);
						for (var i = 1; i <= nOnLockKeys; i++) {
							final var virtualKeyCode = Integer.parseInt(messageParts[17 + nButtons + nDownMouseButtons
									+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes + i]);
							final var lockKey = LockKey.virtualKeyCodeToLockKeyMap.get(virtualKeyCode);
							onLockKeys.add(lockKey);
						}

						offLockKeys.clear();
						final var nOffLockKeys = Integer.parseInt(messageParts[18 + nButtons + nDownMouseButtons
								+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes + nOnLockKeys]);
						for (var i = 1; i <= nOffLockKeys; i++) {
							final var virtualKeyCode = Integer.parseInt(messageParts[18 + nButtons + nDownMouseButtons
									+ nDownUpMouseButtons + nDownKeyStrokes + nDownUpKeyStrokes + nOnLockKeys + i]);
							final var lockKey = LockKey.virtualKeyCodeToLockKeyMap.get(virtualKeyCode);
							offLockKeys.add(lockKey);
						}

						counter = newCounter;
						retVal = true;
					}
				}

				if (message.startsWith(ServerRunMode.PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE)) {
					final var keepAliveBuf = ServerRunMode.PROTOCOL_MESSAGE_CLIENT_ALIVE.getBytes("ASCII");
					final var keepAlivePacket = new DatagramPacket(keepAliveBuf, keepAliveBuf.length, hostAddress,
							port);
					clientSocket.send(keepAlivePacket);
				}
			} catch (final SocketTimeoutException e) {
				log.log(Level.FINE, e.getMessage(), e);
				EventQueue.invokeLater(() -> {
					GuiUtils.showMessageDialog(main.getFrame(), Main.strings.getString("CONNECTION_LOST_DIALOG_TEXT"),
							Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				});

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
				clientSocket.setSoTimeout(timeout);

				while (!Thread.interrupted())
					if (readInput())
						writeOutput();
			} else
				forceStop = true;
		} catch (final UnknownHostException e) {
			log.log(Level.INFO, "Could not resolve host: " + host);
			EventQueue.invokeLater(() -> {
				GuiUtils.showMessageDialog(main.getFrame(),
						MessageFormat.format(Main.strings.getString("INVALID_HOST_ADDRESS_DIALOG_TEXT"), host),
						Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			});
		} catch (final IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			EventQueue.invokeLater(() -> {
				GuiUtils.showMessageDialog(main.getFrame(),
						Main.strings.getString("GENERAL_INPUT_OUTPUT_ERROR_DIALOG_TEXT"),
						Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			});
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

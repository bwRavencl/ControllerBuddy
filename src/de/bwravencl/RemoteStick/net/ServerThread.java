package de.bwravencl.RemoteStick.net;

import java.io.IOException;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import net.brockmatt.util.ResourceBundleUtil;
import de.bwravencl.RemoteStick.gui.Main;
import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.KeyStroke;

public class ServerThread extends Thread {

	public static final int REQUEST_ALIVE_INTERVAL = 100;

	public static final int DEFAULT_PORT = 28789;
	public static final int DEFAULT_CLIENT_TIMEOUT = 1000;
	public static final long DEFAULT_UPDATE_RATE = 10L;

	public static final int PROTOCOL_VERSION = 1;
	public static final String PROTOCOL_MESSAGE_DELIMITER = ":";
	public static final String PROTOCOL_MESSAGE_CLIENT_HELLO = "CLIENT_HELLO";
	public static final String PROTOCOL_MESSAGE_SERVER_HELLO = "SERVER_HELLO";
	public static final String PROTOCOL_MESSAGE_UPDATE = "UPDATE";
	public static final String PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE = PROTOCOL_MESSAGE_UPDATE
			+ "_ALIVE";
	public static final String PROTOCOL_MESSAGE_CLIENT_ALIVE = "CLIENT_ALIVE";

	private enum ServerState {
		Listening, Connected
	}

	private int port = DEFAULT_PORT;
	private int clientTimeout = DEFAULT_CLIENT_TIMEOUT;
	private long updateRate = DEFAULT_UPDATE_RATE;
	private InetAddress clientIPAddress = null;
	private ServerState serverState = ServerState.Listening;
	private final Main main;
	private final Input input;
	private DatagramSocket serverSocket = null;

	private final ResourceBundle rb = new ResourceBundleUtil()
			.getResourceBundle(Main.STRING_RESOURCE_BUNDLE_BASENAME,
					Locale.getDefault());

	public ServerThread(Main main, Input input) {
		this.main = main;
		this.input = input;
		input.setServerThread(this);
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
					receivePacket = new DatagramPacket(receiveBuf,
							receiveBuf.length);
					serverSocket.setSoTimeout(0);
					serverSocket.receive(receivePacket);
					clientIPAddress = receivePacket.getAddress();
					message = new String(receivePacket.getData(), 0,
							receivePacket.getLength());

					if (message.startsWith(PROTOCOL_MESSAGE_CLIENT_HELLO)) {
						final String[] messageParts = message
								.split(PROTOCOL_MESSAGE_DELIMITER);

						if (messageParts.length == 3) {
							long maxAxisValue = Long.parseLong(messageParts[1]);
							int nButtons = Integer.parseInt(messageParts[2]);

							input.setMaxAxisValue(maxAxisValue);
							input.setnButtons(nButtons);

							StringWriter sw = new StringWriter();
							sw.append(PROTOCOL_MESSAGE_SERVER_HELLO);
							sw.append(PROTOCOL_MESSAGE_DELIMITER);
							sw.append(String.valueOf(PROTOCOL_VERSION));
							sw.append(PROTOCOL_MESSAGE_DELIMITER);
							sw.append(String.valueOf(updateRate));

							final byte[] sendBuf = sw.toString().getBytes(
									"ASCII");
							final DatagramPacket sendPacket = new DatagramPacket(
									sendBuf, sendBuf.length, clientIPAddress,
									port);
							serverSocket.send(sendPacket);

							serverState = ServerState.Connected;
							main.setStatusbarText(rb
									.getString("STATUS_CONNECTED")
									+ clientIPAddress.getCanonicalHostName());
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

					sw.append(PROTOCOL_MESSAGE_DELIMITER
							+ input.getCursorDeltaX()
							+ PROTOCOL_MESSAGE_DELIMITER
							+ input.getCursorDeltaY());

					input.setCursorDeltaX(0);
					input.setCursorDeltaY(0);

					sw.append(PROTOCOL_MESSAGE_DELIMITER
							+ input.getScrollClicks());

					input.setScrollClicks(0);

					sw.append(PROTOCOL_MESSAGE_DELIMITER
							+ input.getDownKeyCodes().size());
					for (String s : input.getDownKeyCodes())
						sw.append(PROTOCOL_MESSAGE_DELIMITER + s);

					sw.append(PROTOCOL_MESSAGE_DELIMITER
							+ input.getDownUpKeyStrokes().size());

					for (KeyStroke k : input.getDownUpKeyStrokes()) {
						sw.append(PROTOCOL_MESSAGE_DELIMITER
								+ k.getModifierCodes().length);
						for (String s : k.getModifierCodes())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + s);

						sw.append(PROTOCOL_MESSAGE_DELIMITER
								+ k.getKeyCodes().length);
						for (String s : k.getKeyCodes())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + s);
					}

					input.getDownUpKeyStrokes().clear();

					final byte[] sendBuf = sw.toString().getBytes("ASCII");
					final DatagramPacket sendPacket = new DatagramPacket(
							sendBuf, sendBuf.length, clientIPAddress, port);
					serverSocket.send(sendPacket);

					if (doAliveCheck) {
						receivePacket = new DatagramPacket(receiveBuf,
								receiveBuf.length);
						serverSocket.setSoTimeout(clientTimeout);
						try {
							serverSocket.receive(receivePacket);

							if (clientIPAddress.equals(receivePacket
									.getAddress())) {
								message = new String(receivePacket.getData(),
										0, receivePacket.getLength());

								if (PROTOCOL_MESSAGE_CLIENT_ALIVE
										.equals(message))
									counter++;
							}
						} catch (SocketTimeoutException e) {
							serverState = ServerState.Listening;

							main.setStatusbarText(rb
									.getString("STATUS_TIMEOUT"));
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

	public long getUpdateRate() {
		return updateRate;
	}

	public void setUpdateRate(long updateRate) {
		this.updateRate = updateRate;
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

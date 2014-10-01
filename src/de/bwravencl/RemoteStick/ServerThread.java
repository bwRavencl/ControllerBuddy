package de.bwravencl.RemoteStick;

import java.io.IOException;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.KeyStroke;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class ServerThread extends Thread {

	public static final int DEFAULT_PORT = 28789;
	public static final int DEFAULT_REQUEST_ALIVE_INTERVAL = 100;
	public static final int DEFAULT_ALIVE_TIMEOUT = 1000;
	public static final long DEFAULT_UPDATE_RATE = 5L;

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
	private int requestAliveInterval = DEFAULT_REQUEST_ALIVE_INTERVAL;
	private int aliveTimeout = DEFAULT_ALIVE_TIMEOUT;
	private long updateRate = DEFAULT_UPDATE_RATE;
	private InetAddress clientIPAddress = null;
	private ServerState serverState = ServerState.Listening;
	private Input joystick;
	private DatagramSocket serverSocket = null;

	public ServerThread() {
		final Controller[] controllers = ControllerEnvironment
				.getDefaultEnvironment().getControllers();
		joystick = new Input(this, controllers[4]);
	}

	@Override
	public void run() {
		super.run();

		DatagramPacket receivePacket = null;
		String message = null;
		long counter = 0;

		try {
			serverSocket = new DatagramSocket(port);
			final byte[] receiveBuf = new byte[1024];

			while (true) {

				switch (serverState) {
				case Listening:
					System.out.println("Listenign on: " + port + "...");
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

							joystick.setMaxAxisValue(maxAxisValue);
							joystick.setnButtons(nButtons);

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
							System.out.println("Entering State: Connected");
						} else
							System.out
									.println("Invalid CLIENT_HELLO - ingnoring!");
					}
					break;
				case Connected:
					Thread.sleep(updateRate);

					StringWriter sw = new StringWriter();
					boolean doAliveCheck = false;
					if (counter % requestAliveInterval == 0) {
						sw.append(PROTOCOL_MESSAGE_UPDATE_REQUEST_ALIVE);
						doAliveCheck = true;
					} else
						sw.append(PROTOCOL_MESSAGE_UPDATE);
					sw.append(PROTOCOL_MESSAGE_DELIMITER + counter);

					joystick.poll();

					for (int v : joystick.getAxis())
						sw.append(PROTOCOL_MESSAGE_DELIMITER + v);

					for (boolean v : joystick.getButtons())
						sw.append(PROTOCOL_MESSAGE_DELIMITER + v);

					sw.append(PROTOCOL_MESSAGE_DELIMITER
							+ joystick.getCursorDeltaX()
							+ PROTOCOL_MESSAGE_DELIMITER
							+ joystick.getCursorDeltaY());

					joystick.setCursorDeltaX(0);
					joystick.setCursorDeltaY(0);

					sw.append(PROTOCOL_MESSAGE_DELIMITER
							+ joystick.getScrollClicks());

					joystick.setScrollClicks(0);

					sw.append(PROTOCOL_MESSAGE_DELIMITER
							+ joystick.getDownKeyCodes().size());
					for (String s : joystick.getDownKeyCodes())
						sw.append(PROTOCOL_MESSAGE_DELIMITER + s);

					sw.append(PROTOCOL_MESSAGE_DELIMITER
							+ joystick.getDownUpKeyStrokes().size());

					for (KeyStroke k : joystick.getDownUpKeyStrokes()) {
						sw.append(PROTOCOL_MESSAGE_DELIMITER
								+ k.getModifierCodes().length);
						for (String s : k.getModifierCodes())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + s);

						sw.append(PROTOCOL_MESSAGE_DELIMITER
								+ k.getKeyCodes().length);
						for (String s : k.getKeyCodes())
							sw.append(PROTOCOL_MESSAGE_DELIMITER + s);
					}

					joystick.getDownUpKeyStrokes().clear();

					final byte[] sendBuf = sw.toString().getBytes("ASCII");
					final DatagramPacket sendPacket = new DatagramPacket(
							sendBuf, sendBuf.length, clientIPAddress, port);
					serverSocket.send(sendPacket);

					if (doAliveCheck) {
						receivePacket = new DatagramPacket(receiveBuf,
								receiveBuf.length);
						serverSocket.setSoTimeout(aliveTimeout);
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
							System.out.println("Client "
									+ clientIPAddress.getCanonicalHostName()
									+ " timed out!");
							serverState = ServerState.Listening;
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
			if (serverSocket != null)
				serverSocket.close();
		}
	}

	public void stopServer() {
		if (serverSocket != null)
			serverSocket.close();
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

}

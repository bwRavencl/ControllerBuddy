package de.bwravencl.RemoteStick;

public class Main {

	public static void main(String[] args) {

		final ServerThread serverThread = new ServerThread();
		serverThread.start();
	}

}

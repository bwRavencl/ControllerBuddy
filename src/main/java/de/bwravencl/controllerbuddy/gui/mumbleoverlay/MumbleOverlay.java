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

package de.bwravencl.controllerbuddy.gui.mumbleoverlay;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.trolltech.qt.QSignalEmitter;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.QProcess;
import com.trolltech.qt.network.QLocalServer;
import com.trolltech.qt.network.QLocalSocket;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgHeader;

public class MumbleOverlay extends QObject {

	public static final int DEFAULT_MUMBLE_OVERLAY_UPDATE_INTERVAL = 125;
	private static final int DUPLICATE_SAME_ACCESS = 0x02;
	private static final String PIPE_NAME = "MumbleOverlayPipe";
	private static final String HELPER_32_BINARY = "mumble_ol_helper.exe";
	private static final String HELPER_64_BINARY = "mumble_ol_helper_x64.exe";

	public static String getDefaultMumbleInstallationPath() {
		return System.getenv("ProgramFiles") + File.separator + "Mumble";
	}

	public static String getMumbleHelperFilePath(String mumbleDirectory, boolean x64) {
		final File versionsDirectory = new File(mumbleDirectory + File.separator + "Versions");
		if (versionsDirectory.exists() && versionsDirectory.isDirectory()) {
			final File[] files = versionsDirectory.listFiles();
			if (files != null) {
				for (final File f : files) {
					if (f.isDirectory()) {
						final File helper = new File(
								f.getAbsolutePath() + File.separator + (x64 ? HELPER_64_BINARY : HELPER_32_BINARY));
						if (helper.exists() && helper.isFile())
							return helper.getAbsolutePath();
					}
				}
			}
		}

		return null;
	}

	private final Main main;
	private final HANDLE processHandle;
	private final QProcess helper32Process = new QProcess(this);
	private final QProcess helper64Process = new QProcess(this);
	private final QLocalServer localServer = new QLocalServer(this);

	private final List<MumbleOverlayClient> clients = new ArrayList<MumbleOverlayClient>();

	public MumbleOverlay(Main main) throws Exception {
		this.main = main;

		final HANDLE currentProcess = Kernel32.INSTANCE.GetCurrentProcess();
		final HANDLEByReference processHandleRef = new HANDLEByReference();
		if (!Kernel32.INSTANCE.DuplicateHandle(currentProcess, currentProcess, currentProcess, processHandleRef, 0,
				true, DUPLICATE_SAME_ACCESS))
			throw new Exception(getClass().getName() + ": Unable to duplicate handle to the Mumble process.");
		processHandle = processHandleRef.getValue();

		helper32Process.finished.connect(this, "onHelperProcessExited()");
		helper64Process.finished.connect(this, "onHelperProcessExited()");

		if (!localServer.listen(PIPE_NAME))
			throw new Exception(getClass().getName() + ": Failed to create communication with overlay at " + PIPE_NAME
					+ ": " + localServer.errorString());

		localServer.newConnection.connect(this, "newConnection()");

		startHelper(helper32Process);
		startHelper(helper64Process);
	}

	public void deInit() {
		helper32Process.disconnect();
		helper64Process.disconnect();

		helper32Process.terminate();
		helper64Process.terminate();

		Kernel32.INSTANCE.CloseHandle(processHandle);

		for (final MumbleOverlayClient c : clients) {
			c.getLocalSocket().disconnected.disconnect(this, "disconnected()");
			c.getLocalSocket().error.disconnect(this, "error(QLocalSocket$LocalSocketError)");
			c.deInit();
		}

		localServer.close();
	}

	private void disconnected() {
		for (final MumbleOverlayClient c : clients) {
			if (c.getLocalSocket().equals(QSignalEmitter.signalSender())) {
				c.deInit();
				clients.remove(c);
				return;
			}
		}
	}

	@SuppressWarnings("unused")
	private void error(QLocalSocket.LocalSocketError error) {
		disconnected();
	}

	@SuppressWarnings("unused")
	private void newConnection() {
		while (true) {
			final QLocalSocket localSocket = localServer.nextPendingConnection();
			if (localSocket == null)
				break;

			final MumbleOverlayClient client = new MumbleOverlayClient(this, localSocket);
			clients.add(client);

			localSocket.disconnected.connect(this, "disconnected()");
			localSocket.error.connect(this, "error(QLocalSocket$LocalSocketError)");
		}
	}

	@SuppressWarnings("unused")
	private void onHelperProcessExited() {
		try {
			startHelper((QProcess) QSignalEmitter.signalSender());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void render(BufferedImage image) {
		for (final MumbleOverlayClient c : clients) {
			if (c.getSharedMemory() != null && c.getSharedMemory().getData() != null) {
				final int[] textureBuffer = new int[c.getWidth() * image.getHeight()];

				for (int y = 0; y < image.getHeight(); y++) {
					for (int x = 0; x < c.getWidth(); x++) {
						final int ix = x - (c.getWidth() - image.getWidth());

						if (ix > 0 && ix < image.getWidth() && y < image.getHeight()) {
							final Color color = new Color(image.getRGB(ix, y), true);

							textureBuffer[(y * c.getWidth()) + x] = (color.getAlpha() << 24)
									| ((color.getRed() * color.getAlpha() / 255) << 16)
									| ((color.getGreen() * color.getAlpha() / 255) << 8)
									| (color.getBlue() * color.getAlpha() / 255);
						}
					}
				}

				c.getSharedMemory().getData().write((c.getWidth() * c.getHeight() - textureBuffer.length) * 4,
						textureBuffer, 0, textureBuffer.length);

				c.render(c.getWidth() - image.getWidth(), c.getHeight() - image.getHeight(), c.getWidth(),
						c.getHeight());
			}
		}
	}

	private void startHelper(QProcess helper) throws Exception {
		if (helper.state().equals(QProcess.ProcessState.NotRunning)) {
			final List<String> args = new ArrayList<String>(2);
			args.add(Integer.toString(OverlayMsgHeader.OVERLAY_MAGIC_NUMBER));
			args.add(Integer.toString(Integer.decode(processHandle.toString().substring(7))));

			final boolean x64;
			if (helper == helper32Process)
				x64 = false;
			else if (helper == helper64Process)
				x64 = true;
			else
				throw new Exception(getClass().getName() + ": Invalid helper passed to startHelper().");

			final String helperFilePath = getMumbleHelperFilePath(main.getPreferences()
					.get(Main.PREFERENCES_MUMBLE_DIRECTORY, MumbleOverlay.getDefaultMumbleInstallationPath()), x64);

			if (helperFilePath != null)
				helper.start(helperFilePath, args);
			else
				throw new Exception(getClass().getName() + ": Couldn't find helper binary.");
		}
	}

	public void updateOverlay() {
		for (final MumbleOverlayClient c : clients) {
			if (!c.update()) {
				clients.remove(c);
				c.deInit();
				break;
			}
		}

	}

}

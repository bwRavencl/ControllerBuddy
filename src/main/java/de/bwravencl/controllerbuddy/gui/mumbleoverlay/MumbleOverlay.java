/* Copyright (C) 2017  Matteo Hausner
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

package de.bwravencl.controllerbuddy.gui.mumbleoverlay;

import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgHeader;
import de.bwravencl.controllerbuddy.util.RunnableWithDefaultExceptionHandler;
import io.qt.core.QCoreApplication;
import io.qt.core.QObject;
import io.qt.core.QProcess;
import io.qt.core.QProcess.ProcessError;
import io.qt.network.QLocalServer;
import io.qt.network.QLocalSocket;

public final class MumbleOverlay extends QObject {

	public enum HelperBinary {

		x86(HELPER_BINARY_NAME_x86), x64(HELPER_BINARY_NAME_x64);

		private final String binaryName;

		HelperBinary(final String binaryName) {
			this.binaryName = binaryName;
		}
	}

	private static class MumbleOverlayInitializationException extends RuntimeException {

		private static final long serialVersionUID = 2338055180798649448L;

		private final String errorMessageString;

		private MumbleOverlayInitializationException(final String message, final String errorMessageString) {
			super(message);
			this.errorMessageString = errorMessageString;
		}
	}

	private static record ImageCacheEntry(BufferedImage image, Graphics graphics) {
	}

	public static final boolean DEFAULT_USE_MUMBLE_OVERLAY = false;

	private static final String HELPER_BINARY_NAME_x86 = "mumble_ol_helper.exe";

	private static final String HELPER_BINARY_NAME_x64 = "mumble_ol_helper_x64.exe";

	private static final int DUPLICATE_SAME_ACCESS = 0x02;

	private static final String PIPE_NAME = "MumbleOverlayPipe";

	private static final Logger log = Logger.getLogger(MumbleOverlay.class.getName());

	private static volatile MumbleOverlay instance;

	public static String getDefaultMumblePath() {
		return System.getenv("ProgramFiles") + File.separator + "Mumble" + File.separator + "client";
	}

	private static String getMumbleHelperPath(final Main main, final HelperBinary platform) {
		final var mumblePath = main.getPreferences().get(Main.PREFERENCES_MUMBLE_DIRECTORY, getDefaultMumblePath());

		return getMumbleHelperPath(mumblePath, platform);
	}

	public static String getMumbleHelperPath(final String mumblePath, final HelperBinary platform) {
		final var helperBinaryFile = new File(mumblePath + File.separator + platform.binaryName);

		if (helperBinaryFile.isFile())
			return helperBinaryFile.getAbsolutePath();

		return null;
	}

	public static synchronized MumbleOverlay start(final Main main) {
		if (!main.getPreferences().getBoolean(Main.PREFERENCES_USE_MUMBLE_OVERLAY, DEFAULT_USE_MUMBLE_OVERLAY)
				|| !main.getInput().getProfile().isUseMumbleOverlay())
			return null;

		final var qtApplicationThread = Thread.ofVirtual().start(() -> {
			while (!Thread.currentThread().isInterrupted() && QCoreApplication.instance() != null)
				try {
					Thread.sleep(5L);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}

			final var coreApplication = QCoreApplication.initialize(new String[0]);
			try {
				instance = new MumbleOverlay(main);
				QCoreApplication.exec();
			} catch (final Throwable t) {
				log.log(Level.SEVERE, t.getMessage(), t);

				final String errorMessageString;
				if (t instanceof final MumbleOverlayInitializationException mumbleOverlayInitializationException)
					errorMessageString = mumbleOverlayInitializationException.errorMessageString;
				else
					errorMessageString = "MUMBLE_OVERLAY_GENERAL_INITIALIZATION_ERROR_DIALOG_TEXT";

				EventQueue.invokeLater(
						() -> GuiUtils.showMessageDialog(main.getFrame(), Main.strings.getString(errorMessageString),
								Main.strings.getString("WARNING_DIALOG_TITLE"), JOptionPane.WARNING_MESSAGE));
			} finally {
				coreApplication.dispose();
			}
		});

		while (instance == null && qtApplicationThread.isAlive())
			try {
				Thread.sleep(5L);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		main.getOverlayFrame().setAlwaysOnTop(false);
		main.getOnScreenKeyboard().setAlwaysOnTop(false);

		return instance;
	}

	public static void stop(final Main main) {
		try {
			if (instance != null) {
				instance.executorService.shutdown();

				try {
					if (instance.executorService.awaitTermination(2L, TimeUnit.SECONDS)) {
						instance.helperProcessX86.disconnect();
						instance.helperProcessX64.disconnect();

						instance.helperProcessX86.terminate();
						instance.helperProcessX64.terminate();

						Kernel32.INSTANCE.CloseHandle(instance.processHandle);

						final var mumbleOverlayClientsLock = instance.mumbleOverlayClientsLock;
						mumbleOverlayClientsLock.lock();
						try {
							for (final var mumbleOverlayClient : instance.mumbleOverlayClients) {
								mumbleOverlayClient.getLocalSocket().disconnected.disconnect(instance,
										"disconnected()");
								mumbleOverlayClient.getLocalSocket().errorOccurred.disconnect(instance,
										"error(QLocalSocket$LocalSocketError)");
								mumbleOverlayClient.deInit();
							}
						} finally {
							mumbleOverlayClientsLock.unlock();
						}

						instance.localServer.close();

						QCoreApplication.exit();

						for (final var imageCacheEntry : instance.containerImageCache.values())
							imageCacheEntry.graphics.dispose();

						instance.containerImageCache.clear();
					}
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					instance = null;
				}
			}
		} finally {
			main.getOverlayFrame().setAlwaysOnTop(true);
			main.getOnScreenKeyboard().setAlwaysOnTop(true);
		}
	}

	private final Main main;
	private final HANDLE processHandle;
	private final QProcess helperProcessX86 = new QProcess(this);
	private final QProcess helperProcessX64 = new QProcess(this);
	private final QLocalServer localServer = new QLocalServer(this);
	private final List<MumbleOverlayClient> mumbleOverlayClients = new ArrayList<>();
	private final Lock mumbleOverlayClientsLock = new ReentrantLock();
	private final Map<Container, ImageCacheEntry> containerImageCache = new HashMap<>();

	private ScheduledExecutorService executorService;

	private MumbleOverlay(final Main main) {
		this.main = main;

		final var localSocket = new QLocalSocket(this);
		localSocket.connectToServer(PIPE_NAME);
		final var pipeExists = localSocket.isValid();
		localSocket.disconnectFromServer();

		if (pipeExists) {
			log.log(Level.WARNING, "Pipe " + PIPE_NAME + " already exists");
			throw new MumbleOverlayInitializationException("Pipe " + PIPE_NAME + " already exists",
					"CANNOT_INITIALIZE_THE_MUMBLE_OVERLAY_DIALOG_TEXT");
		}

		final var currentProcessHandle = Kernel32.INSTANCE.GetCurrentProcess();
		final var processHandleRef = new HANDLEByReference();

		if (!Kernel32.INSTANCE.DuplicateHandle(currentProcessHandle, currentProcessHandle, currentProcessHandle,
				processHandleRef, 0, true, DUPLICATE_SAME_ACCESS)) {
			log.log(Level.WARNING, "Failed to duplicate process handle");
			throw new RuntimeException("DuplicateHandle() failed");
		}
		processHandle = processHandleRef.getValue();

		final var listening = localServer.listen(PIPE_NAME);
		try {
			if (!listening) {
				log.log(Level.WARNING, "Failed to establish communication with overlay via " + PIPE_NAME + ": "
						+ localServer.errorString());
				throw new RuntimeException("Listen() failed");
			}

			log.log(Level.INFO, "Listening for connection on " + localServer.fullServerName());
			localServer.newConnection.connect(this, "newConnection()");

			startHelper(helperProcessX86);
			startHelper(helperProcessX64);

			executorService = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
			executorService.scheduleAtFixedRate(new RunnableWithDefaultExceptionHandler(this::renderDirtyClients), 0L,
					1L, TimeUnit.SECONDS);
		} catch (final Throwable t) {
			localServer.close();

			throw t;
		}
	}

	private void disconnected() {
		mumbleOverlayClientsLock.lock();
		try {
			mumbleOverlayClients.stream()
					.filter(mumbleOverlayClient -> mumbleOverlayClient.getLocalSocket().equals(sender())).findFirst()
					.ifPresent(mumbleOverlayClient -> {
						mumbleOverlayClient.deInit();
						mumbleOverlayClients.remove(mumbleOverlayClient);
						log.log(Level.INFO,
								"Overlay client with PID " + mumbleOverlayClient.getPid() + " disconnected");
					});
		} finally {
			mumbleOverlayClientsLock.unlock();
		}
	}

	@SuppressWarnings("unused")
	private void error(final QLocalSocket.LocalSocketError error) {
		disconnected();
	}

	Main getMain() {
		return main;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	private void initHelperProcess(final QProcess helperProcess) {
		helperProcess.started.connect(this, "onHelperProcessStarted()");
		helperProcessX86.errorOccurred.connect(this, "onHelperProcessError(ProcessError)");
		helperProcess.finished.connect(this, "onHelperProcessExited()");
	}

	@SuppressWarnings("unused")
	private void newConnection() {
		while (localServer.hasPendingConnections()) {
			final var localSocket = localServer.nextPendingConnection();
			if (localSocket == null)
				break;

			final var mumbleOverlayClient = new MumbleOverlayClient(this, localSocket);
			mumbleOverlayClientsLock.lock();
			try {
				mumbleOverlayClients.add(mumbleOverlayClient);
			} finally {
				mumbleOverlayClientsLock.unlock();
			}

			localSocket.disconnected.connect(this, "disconnected()");
			localSocket.errorOccurred.connect(this, "error(QLocalSocket$LocalSocketError)");

			log.log(Level.INFO, "New overlay client connected to socket: " + localSocket.socketDescriptor());
		}
	}

	@SuppressWarnings("unused")
	private void onHelperProcessError(final ProcessError processError) {
		if (!(sender() instanceof final QProcess helperProcess))
			throw new IllegalStateException();

		final var errorMessage = switch (processError) {
		case FailedToStart -> "has failed to start";
		case Crashed -> "has crashed";
		case Timedout -> "encountered a time out during a wait operation";
		case WriteError -> "encountered an error when attempting to write to the process";
		case ReadError -> "encountered an error when attempting to read from the process";
		default -> "encountered an unknown error";
		};

		log.log(Level.WARNING, "Helper process with PID " + helperProcess.processId() + errorMessage);
	}

	@SuppressWarnings("unused")
	private void onHelperProcessExited() {
		if (!(sender() instanceof final QProcess helperProcess))
			throw new IllegalStateException();

		log.log(Level.INFO, "Helper process with PID " + helperProcess.processId() + " exited with status "
				+ helperProcess.exitStatus());

		startHelper(helperProcess);
	}

	@SuppressWarnings("unused")
	private void onHelperProcessStarted() {
		if (!(sender() instanceof final QProcess helperProcess))
			throw new IllegalStateException();

		final var pid = helperProcess.processId();

		String path;
		if (helperProcess == helperProcessX86)
			path = getMumbleHelperPath(main, HelperBinary.x86);
		else if (helperProcess == helperProcessX64)
			path = getMumbleHelperPath(main, HelperBinary.x64);
		else {
			log.log(Level.SEVERE, "Detected unknown helper process with PID " + pid);
			return;
		}

		log.log(Level.INFO, "Started helper process " + path + " with PID " + pid);
	}

	private void paintContainer(final Container container) {
		mumbleOverlayClientsLock.lock();
		try {
			if (mumbleOverlayClients.isEmpty())
				return;

			final var containerWidth = container.getWidth();
			final var containerHeight = container.getHeight();
			if (containerWidth < 1 || containerHeight < 1)
				return;

			var imageCacheEntry = containerImageCache.get(container);
			if (imageCacheEntry == null || imageCacheEntry.image.getWidth() != containerWidth
					|| imageCacheEntry.image.getHeight() != containerHeight) {
				final var image = new BufferedImage(containerWidth, containerHeight, BufferedImage.TYPE_INT_ARGB_PRE);
				final var graphics = image.getGraphics();

				imageCacheEntry = new ImageCacheEntry(image, graphics);
				final var previousImageCacheEntry = containerImageCache.put(container, imageCacheEntry);
				if (previousImageCacheEntry != null)
					previousImageCacheEntry.graphics.dispose();
			}

			container.print(imageCacheEntry.graphics);

			final var originX = container.getX();
			final var originY = container.getY();

			for (final var mumbleOverlayClient : mumbleOverlayClients)
				mumbleOverlayClient.paintImage(imageCacheEntry.image, originX, originY);
		} finally {
			mumbleOverlayClientsLock.unlock();
		}
	}

	public void paintContainers(final boolean renderAllClients) {
		final var overlayFrame = main.getOverlayFrame();
		if (overlayFrame != null && overlayFrame.isVisible())
			paintContainer(overlayFrame);

		final var onScreenKeyboard = main.getOnScreenKeyboard();
		if (onScreenKeyboard != null && onScreenKeyboard.isVisible())
			paintContainer(onScreenKeyboard);

		if (renderAllClients) {
			mumbleOverlayClientsLock.lock();
			try {
				for (final var mumbleOverlayClient : mumbleOverlayClients)
					mumbleOverlayClient.render();
			} finally {
				mumbleOverlayClientsLock.unlock();
			}
		}
	}

	private void renderDirtyClients() {
		mumbleOverlayClientsLock.lock();
		try {
			final var dirtyMumbleOverlayClients = mumbleOverlayClients.stream().filter(MumbleOverlayClient::isDirty)
					.collect(Collectors.toList());

			if (dirtyMumbleOverlayClients.isEmpty())
				return;

			paintContainers(false);

			for (final var mumbleOverlayClient : dirtyMumbleOverlayClients)
				mumbleOverlayClient.render();
		} finally {
			mumbleOverlayClientsLock.unlock();
		}
	}

	private void startHelper(final QProcess helper) throws MumbleOverlayInitializationException {
		if (QProcess.ProcessState.NotRunning.equals(helper.state())) {
			final var args = List.of(Integer.toString(OverlayMsgHeader.OVERLAY_MAGIC_NUMBER),
					Integer.toString(Integer.decode(processHandle.toString().substring(7))));

			final HelperBinary helperBinary;
			if (helper == helperProcessX86) {
				initHelperProcess(helperProcessX86);
				helperBinary = HelperBinary.x86;
			} else if (helper == helperProcessX64) {
				initHelperProcess(helperProcessX64);
				helperBinary = HelperBinary.x64;
			} else
				throw new IllegalArgumentException();

			final var helperBinaryPath = getMumbleHelperPath(main, helperBinary);
			if (helperBinaryPath == null)
				throw new MumbleOverlayInitializationException(
						"Could not find helper binary for platform " + helperBinary,
						"COULD_NOT_LAUNCH_MUMBLE_OVERLAY_HELPER_DIALOG_TEXT");

			log.log(Level.INFO, "Starting overlay helper: " + helperBinaryPath
					+ args.stream().collect(Collectors.joining(" ", " ", "")));
			helper.start(helperBinaryPath, args);
		}
	}

	void updateOverlay() {
		mumbleOverlayClientsLock.lock();
		try {
			final var it = mumbleOverlayClients.iterator();
			while (it.hasNext()) {
				final var mumbleOverlayClient = it.next();
				if (!mumbleOverlayClient.update()) {
					log.log(Level.INFO, "Detected dead client with PID " + mumbleOverlayClient.getPid());

					mumbleOverlayClient.deInit();
					it.remove();
				}
			}
		} finally {
			mumbleOverlayClientsLock.unlock();
		}
	}
}

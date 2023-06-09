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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsg;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgActive;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgHeader;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgInit;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgPid;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgShmem;
import io.qt.core.QElapsedTimer;
import io.qt.core.QObject;
import io.qt.core.QTimer;
import io.qt.network.QLocalSocket;

final class MumbleOverlayClient extends QObject {

	private final MumbleOverlay mumbleOverlay;

	private final QLocalSocket localSocket;

	private final OverlayMsg overlayMessage = new OverlayMsg(new OverlayMsgHeader(-1, -1), null);

	private SharedMemory sharedMemory;

	private int width;

	private int height;

	private BufferedImage bufferedImage;

	private Graphics2D graphics;

	private final QElapsedTimer elapsedTimer = new QElapsedTimer();

	private boolean dirty = true;

	private long lastFpsMessage;

	private int pid;

	private int[] textureBuffer;

	MumbleOverlayClient(final MumbleOverlay mumbleOverlay, final QLocalSocket localSocket) {
		this.mumbleOverlay = mumbleOverlay;

		this.localSocket = localSocket;
		localSocket.setParent(null);
		localSocket.readyRead.connect(this, "readyRead()");
	}

	void deInit() {
		localSocket.disconnectFromServer();

		if (!localSocket.waitForDisconnected(1000))
			localSocket.abort();

		if (graphics != null)
			graphics.dispose();
	}

	QLocalSocket getLocalSocket() {
		return localSocket;
	}

	int getPid() {
		return pid;
	}

	SharedMemory getSharedMemory() {
		return sharedMemory;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	boolean isDirty() {
		return dirty;
	}

	void paintImage(final BufferedImage image, final int originX, final int originY) {
		if (sharedMemory == null || sharedMemory.getData() == null || graphics == null)
			return;

		final var totalDisplayBounds = mumbleOverlay.getMain().getTotalDisplayBounds();
		if (totalDisplayBounds == null)
			return;

		final var imageWidth = image.getWidth();
		final var imageHeight = image.getHeight();

		final var scalingFactorX = (float) (width - imageWidth) / (float) (totalDisplayBounds.width - imageWidth);
		final var scalingFactorY = (float) (height - imageHeight) / (float) (totalDisplayBounds.height - imageHeight);

		graphics.drawImage(image, (int) (originX * scalingFactorX), (int) (originY * scalingFactorY), null);
	}

	@SuppressWarnings("unused")
	private void readyRead() {
		for (;;) {
			var ready = localSocket.bytesAvailable();

			if (overlayMessage.headerPart.iLength == -1) {
				if (ready < OverlayMsgHeader.SIZE)
					break;
				final var headerBytes = new byte[OverlayMsgHeader.SIZE];
				localSocket.read(headerBytes);
				overlayMessage.headerPart = new OverlayMsgHeader(headerBytes);

				if (overlayMessage.headerPart.uiMagic != OverlayMsgHeader.OVERLAY_MAGIC_NUMBER
						|| overlayMessage.headerPart.iLength < 0
						|| overlayMessage.headerPart.iLength > OverlayMsgShmem.SIZE) {
					disconnect();
					return;
				}

				ready -= OverlayMsgHeader.SIZE;
			}

			if (ready < overlayMessage.headerPart.iLength)
				break;
			byte[] msgBuffer;
			if (overlayMessage.headerPart.iLength > 0)
				msgBuffer = new byte[overlayMessage.headerPart.iLength];
			else
				msgBuffer = new byte[OverlayMsgShmem.SIZE];

			final long length = localSocket.read(msgBuffer);

			if (length != overlayMessage.headerPart.iLength) {
				disconnect();
				return;
			}

			switch (overlayMessage.headerPart.uiType) {
			case OverlayMsgHeader.OVERLAY_MSGTYPE_INIT:
				overlayMessage.messagePart = new OverlayMsgInit(msgBuffer);
				readyReadMsgInit(length);
				break;
			case OverlayMsgHeader.OVERLAY_MSGTYPE_SHMEM:
				overlayMessage.messagePart = new OverlayMsgShmem(msgBuffer);
				if (sharedMemory != null)
					sharedMemory.systemRelease();
				break;
			case OverlayMsgHeader.OVERLAY_MSGTYPE_PID:
				if (length != OverlayMsgPid.SIZE)
					break;

				final var messagePid = new OverlayMsgPid(msgBuffer);
				overlayMessage.messagePart = messagePid;
				pid = messagePid.pid;

				final var handle = Kernel32.INSTANCE
						.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ, false, messagePid.pid);
				if (handle != null)
					Kernel32.INSTANCE.CloseHandle(handle);
				break;
			case OverlayMsgHeader.OVERLAY_MSGTYPE_FPS:
				lastFpsMessage = System.currentTimeMillis();
				QTimer.singleShot(0, mumbleOverlay, "updateOverlay()");
				break;
			default:
				disconnect();
				throw new IllegalArgumentException("Unknown message type " + overlayMessage.headerPart.uiType);
			}

			overlayMessage.headerPart.iLength = -1;
		}
	}

	private void readyReadMsgInit(final long length) {
		if (length != OverlayMsgInit.SIZE)
			return;

		final var omi = (OverlayMsgInit) overlayMessage.messagePart;

		width = omi.uiWidth;
		height = omi.uiHeight;

		if (sharedMemory != null)
			sharedMemory.deInit();

		sharedMemory = new SharedMemory(width * height * 4);
		if (sharedMemory.getData() == null) {
			sharedMemory.deInit();
			sharedMemory = null;
			return;
		}

		bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
		if (graphics != null)
			graphics.dispose();
		graphics = bufferedImage.createGraphics();
		graphics.setBackground(Main.TRANSPARENT);

		textureBuffer = new int[width * height];

		final var messagePart = new OverlayMsgShmem(sharedMemory.getName());
		final var headerPart = new OverlayMsgHeader(messagePart.getSize(), OverlayMsgHeader.OVERLAY_MSGTYPE_SHMEM);
		final var message = new OverlayMsg(headerPart, messagePart);
		localSocket.write(message.getBytes());

		sharedMemory.erase();

		QTimer.singleShot(0, mumbleOverlay, "updateOverlay()");
	}

	void render() {
		if (sharedMemory == null || sharedMemory.getData() == null || textureBuffer == null)
			return;

		for (var y = 0; y < height; y++)
			for (var x = 0; x < width; x++) {
				final var color = new Color(bufferedImage.getRGB(x, y), true);

				textureBuffer[y * width + x] = color.getAlpha() << 24 | color.getRed() * color.getAlpha() / 255 << 16
						| color.getGreen() * color.getAlpha() / 255 << 8 | color.getBlue() * color.getAlpha() / 255;
			}

		sharedMemory.getData().write((width * height - textureBuffer.length) * 4, textureBuffer, 0,
				textureBuffer.length);

		if (lastFpsMessage == 0L || System.currentTimeMillis() - lastFpsMessage < 300L) {
			final var messagePart = new OverlayMsgActive(0, 0, width, height);
			final var headerPart = new OverlayMsgHeader(messagePart.getSize(), OverlayMsgHeader.OVERLAY_MSGTYPE_ACTIVE);
			final var message = new OverlayMsg(headerPart, messagePart);

			localSocket.write(message.getBytes());
			localSocket.flush();
			dirty = false;
		} else
			dirty = true;

		graphics.clearRect(0, 0, width, height);
	}

	boolean update() {
		if (width == 0 || height == 0 || sharedMemory == null)
			return true;

		if (localSocket.bytesToWrite() > 1024L)
			return elapsedTimer.elapsed() <= 5000000;
		elapsedTimer.restart();
		return true;
	}
}

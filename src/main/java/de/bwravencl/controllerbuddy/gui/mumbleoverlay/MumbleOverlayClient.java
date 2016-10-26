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

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.QTime;
import com.trolltech.qt.core.QTimer;
import com.trolltech.qt.network.QLocalSocket;

import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsg;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgActive;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgHeader;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgInit;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgPid;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.message.OverlayMsgShmem;

public class MumbleOverlayClient extends QObject {

	private final MumbleOverlay overlay;
	private final QLocalSocket localSocket;
	private final OverlayMsg overlayMessage = new OverlayMsg(new OverlayMsgHeader(-1, -1), null);
	private SharedMemory sharedMemory;
	private int width = 0;
	private int height = 0;
	private final QTime t = new QTime();
	private boolean dirty = true;
	private long lastFpsMessage = 0L;

	public MumbleOverlayClient(final MumbleOverlay overlay, final QLocalSocket localSocket) {
		this.overlay = overlay;
		this.localSocket = localSocket;
		localSocket.setParent(null);

		localSocket.readyRead.connect(this, "readyRead()");
	}

	public void deInit() {
		localSocket.disconnectFromServer();

		if (!localSocket.waitForDisconnected(1000))
			localSocket.abort();
	}

	public int getHeight() {
		return height;
	}

	public QLocalSocket getLocalSocket() {
		return localSocket;
	}

	public SharedMemory getSharedMemory() {
		return sharedMemory;
	}

	public int getWidth() {
		return width;
	}

	public boolean isDirty() {
		return dirty;
	}

	@SuppressWarnings("unused")
	private void readyRead() {
		while (true) {
			long ready = localSocket.bytesAvailable();

			if (overlayMessage.headerPart.iLength == -1) {
				if (ready < OverlayMsgHeader.SIZE)
					break;
				else {
					final byte[] headerBytes = new byte[OverlayMsgHeader.SIZE];
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
			}

			if (ready >= overlayMessage.headerPart.iLength) {
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
					try {
						readyReadMsgInit(length);
					} catch (final Exception e) {
						e.printStackTrace();
					}
					break;
				case OverlayMsgHeader.OVERLAY_MSGTYPE_SHMEM:
					overlayMessage.messagePart = new OverlayMsgShmem(msgBuffer);
					if (sharedMemory != null)
						sharedMemory.systemRelease();
					break;
				case OverlayMsgHeader.OVERLAY_MSGTYPE_PID:
					if (length != OverlayMsgPid.SIZE)
						break;

					final OverlayMsgPid messagePid = new OverlayMsgPid(msgBuffer);
					overlayMessage.messagePart = messagePid;

					final HANDLE handle = Kernel32.INSTANCE.OpenProcess(
							WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ, false, messagePid.pid);
					if (handle != null)
						Kernel32.INSTANCE.CloseHandle(handle);
					break;
				case OverlayMsgHeader.OVERLAY_MSGTYPE_FPS:
					lastFpsMessage = System.currentTimeMillis();
					QTimer.singleShot(0, overlay, "updateOverlay()");
					break;
				}

				overlayMessage.headerPart.iLength = -1;
			} else
				break;
		}
	}

	private void readyReadMsgInit(final long length) throws Exception {
		if (length != OverlayMsgInit.SIZE)
			return;

		final OverlayMsgInit omi = (OverlayMsgInit) overlayMessage.messagePart;

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

		final OverlayMsgShmem messagePart = new OverlayMsgShmem(sharedMemory.getName());
		final OverlayMsgHeader headerPart = new OverlayMsgHeader(messagePart.getSize(),
				OverlayMsgHeader.OVERLAY_MSGTYPE_SHMEM);
		final OverlayMsg message = new OverlayMsg(headerPart, messagePart);
		localSocket.write(message.getBytes());

		sharedMemory.erase();

		QTimer.singleShot(0, overlay, "updateOverlay()");
	}

	public void render(final int x, final int y, final int w, final int h) {
		dirty = true;

		if (lastFpsMessage == 0L || System.currentTimeMillis() - lastFpsMessage < 300L) {
			final OverlayMsgActive messagePart = new OverlayMsgActive(x, y, w, h);
			final OverlayMsgHeader headerPart = new OverlayMsgHeader(messagePart.getSize(),
					OverlayMsgHeader.OVERLAY_MSGTYPE_ACTIVE);
			final OverlayMsg message = new OverlayMsg(headerPart, messagePart);

			localSocket.write(message.getBytes());
			localSocket.flush();
			dirty = false;
		}
	}

	public boolean update() {
		if (width == 0 || height == 0 || sharedMemory == null)
			return true;

		if (localSocket.bytesToWrite() > 1024L)
			return t.elapsed() <= 5000000;
		else {
			t.restart();
			return true;
		}
	}

}

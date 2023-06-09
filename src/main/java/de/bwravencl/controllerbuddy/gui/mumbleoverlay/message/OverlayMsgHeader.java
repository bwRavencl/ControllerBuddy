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

package de.bwravencl.controllerbuddy.gui.mumbleoverlay.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class OverlayMsgHeader implements IMessagePart {

	public static final int OVERLAY_MAGIC_NUMBER = 0x00000005;
	public static final int OVERLAY_MSGTYPE_INIT = 0;
	public static final int OVERLAY_MSGTYPE_SHMEM = 1;
	public static final int OVERLAY_MSGTYPE_BLIT = 2;
	public static final int OVERLAY_MSGTYPE_ACTIVE = 3;
	public static final int OVERLAY_MSGTYPE_PID = 4;
	public static final int OVERLAY_MSGTYPE_FPS = 5;
	public static final int OVERLAY_MSGTYPE_INTERACTIVE = 6;
	public static final int SIZE = 3 * Integer.BYTES;

	public int uiMagic = OVERLAY_MAGIC_NUMBER;
	public int iLength;
	public int uiType;

	public OverlayMsgHeader(final byte[] bytes) {
		setBytes(bytes);
	}

	public OverlayMsgHeader(final int iLength, final int uiType) {
		this.iLength = iLength;
		this.uiType = uiType;
	}

	public OverlayMsgHeader(final int uiMagic, final int iLength, final int uiType) {
		this.uiMagic = uiMagic;
		this.iLength = iLength;
		this.uiType = uiType;
	}

	@Override
	public byte[] getBytes() {
		final var bb = ByteBuffer.allocate(getSize());
		bb.order(ByteOrder.LITTLE_ENDIAN);

		bb.putInt(uiMagic);
		bb.putInt(iLength);
		bb.putInt(uiType);

		return bb.array();
	}

	@Override
	public int getSize() {
		return SIZE;
	}

	@Override
	public void setBytes(final byte[] bytes) {
		final var bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		uiMagic = bb.getInt();
		iLength = bb.getInt();
		uiType = bb.getInt();
	}
}

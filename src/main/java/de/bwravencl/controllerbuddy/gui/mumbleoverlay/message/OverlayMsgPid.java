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

package de.bwravencl.controllerbuddy.gui.mumbleoverlay.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OverlayMsgPid implements IMessagePart {

	public static final int SIZE = Integer.BYTES;

	public int pid;

	public OverlayMsgPid(final byte[] bytes) {
		setBytes(bytes);
	}

	public OverlayMsgPid(final int pid) {
		this.pid = pid;
	}

	@Override
	public byte[] getBytes() {
		final ByteBuffer bb = ByteBuffer.allocate(getSize());
		bb.order(ByteOrder.LITTLE_ENDIAN);

		bb.putInt(pid);

		return bb.array();
	}

	@Override
	public int getSize() {
		return SIZE;
	}

	@Override
	public void setBytes(final byte[] bytes) {
		final ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		pid = bb.getInt();
	}

}

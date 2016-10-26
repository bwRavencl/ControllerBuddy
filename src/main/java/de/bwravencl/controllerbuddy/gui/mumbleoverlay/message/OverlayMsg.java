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

public class OverlayMsg {

	public OverlayMsgHeader headerPart;
	public IMessagePart messagePart;

	public OverlayMsg(final OverlayMsgHeader headerPart, final IMessagePart messagePart) {
		this.headerPart = headerPart;
		this.messagePart = messagePart;
	}

	public byte[] getBytes() {
		final ByteBuffer bb = ByteBuffer.allocate(OverlayMsgHeader.SIZE + messagePart.getSize());
		bb.order(ByteOrder.LITTLE_ENDIAN);

		bb.put(headerPart.getBytes());
		bb.put(messagePart.getBytes());

		return bb.array();
	}

}

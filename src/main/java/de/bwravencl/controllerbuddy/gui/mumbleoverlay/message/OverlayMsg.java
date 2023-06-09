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

public final class OverlayMsg {

	public OverlayMsgHeader headerPart;
	public IMessagePart messagePart;

	public OverlayMsg(final OverlayMsgHeader headerPart, final IMessagePart messagePart) {
		this.headerPart = headerPart;
		this.messagePart = messagePart;
	}

	public byte[] getBytes() {
		final var bb = ByteBuffer.allocate(OverlayMsgHeader.SIZE + messagePart.getSize());
		bb.order(ByteOrder.LITTLE_ENDIAN);

		bb.put(headerPart.getBytes());
		bb.put(messagePart.getBytes());

		return bb.array();
	}
}

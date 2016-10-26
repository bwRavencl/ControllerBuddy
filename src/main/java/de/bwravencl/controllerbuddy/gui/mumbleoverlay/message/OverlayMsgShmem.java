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

import java.nio.charset.StandardCharsets;

public class OverlayMsgShmem implements IMessagePart {

	public static final int SIZE = 2048;

	public String a_cName;

	public OverlayMsgShmem(final byte[] bytes) {
		setBytes(bytes);
	}

	public OverlayMsgShmem(final String a_cName) {
		this.a_cName = a_cName;
	}

	@Override
	public byte[] getBytes() {
		return (a_cName + '\0').getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public int getSize() {
		return getBytes().length;
	}

	@Override
	public void setBytes(final byte[] bytes) {
		a_cName = new String(bytes, StandardCharsets.UTF_8);
	}

}

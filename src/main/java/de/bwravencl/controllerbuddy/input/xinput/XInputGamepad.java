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

package de.bwravencl.controllerbuddy.input.xinput;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

public class XInputGamepad extends Structure {

	public short wButtons;

	public byte bLeftTrigger, bRightTrigger;

	public short sThumbLX, sThumbLY, sThumbRX, sThumbRY;

	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList(new String[] { "wButtons", "bLeftTrigger", "bRightTrigger", "sThumbLX", "sThumbLY",
				"sThumbRX", "sThumbRY" });
	}

}
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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;

public class AxisToAxisAction extends ToAxisAction implements ISuspendableAction {

	@Override
	public void doAction(final Input input, float value) {
		if (!isSuspended()) {
			if (Math.abs(value) <= deadZone)
				value = 0.0f;
			else {
				if (value >= 0.0f)
					value = Input.normalize(value, deadZone, 1.0f, 0.0f, 1.0f);
				else
					value = Input.normalize(value, -1.0f, -deadZone, -1.0f, 0.0f);
			}

			input.setAxis(virtualAxis, invert ? -value : value);
		}
	}

}

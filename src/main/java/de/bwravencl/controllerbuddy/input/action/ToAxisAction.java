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

import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;

public abstract class ToAxisAction extends InvertableAction {

	public static final float DEFAULT_DEAD_ZONE = 0.0f;

	protected VirtualAxis virtualAxis = VirtualAxis.X;

	protected float deadZone = DEFAULT_DEAD_ZONE;

	public float getDeadZone() {
		return deadZone;
	}

	public VirtualAxis getVirtualAxis() {
		return virtualAxis;
	}

	public void setDeadZone(final Float deadZone) {
		this.deadZone = deadZone;
	}

	public void setVirtualAxis(final VirtualAxis virtualAxis) {
		this.virtualAxis = virtualAxis;
	}

	@Override
	public String toString() {
		return rb.getString("TO_AXIS_ACTION_STRING");
	}

}

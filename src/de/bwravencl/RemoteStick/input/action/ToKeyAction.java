/* Copyright (C) 2014  Matteo Hausner
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

package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.KeyStroke;

public abstract class ToKeyAction extends InvertableAction {

	protected boolean downUp = false;
	protected boolean wasUp = true;

	protected KeyStroke keystroke = new KeyStroke();

	public boolean isDownUp() {
		return downUp;
	}

	public void setDownUp(Boolean downUp) {
		this.downUp = downUp;
	}

	public KeyStroke getKeystroke() {
		return keystroke;
	}

	public void setKeystroke(KeyStroke keystroke) {
		this.keystroke = keystroke;
	}

	@Override
	public String toString() {
		return rb.getString("TO_KEY_ACTION_STRING");
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final ToKeyAction toKeyAction = (ToKeyAction) super.clone();
		toKeyAction.setKeystroke((KeyStroke) keystroke.clone());

		return toKeyAction;
	}

}

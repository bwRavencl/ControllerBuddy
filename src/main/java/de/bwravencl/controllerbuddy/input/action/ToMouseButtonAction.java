/* Copyright (C) 2019  Matteo Hausner
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

import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.DownUpEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.MouseButtonEditorBuilder;

abstract class ToMouseButtonAction<V extends Number> implements IAction<V> {

	private static final int DEFAULT_MOUSE_BUTTON = 1;

	@ActionProperty(label = "DOWN_UP", editorBuilder = DownUpEditorBuilder.class, order = 11)
	boolean downUp = false;

	@ActionProperty(label = "MOUSE_BUTTON", editorBuilder = MouseButtonEditorBuilder.class, order = 10)
	int mouseButton = DEFAULT_MOUSE_BUTTON;

	transient boolean wasUp = true;
	transient boolean initiator = false;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public int getMouseButton() {
		return mouseButton;
	}

	public boolean isDownUp() {
		return downUp;
	}

	public void setDownUp(final boolean downUp) {
		this.downUp = downUp;
	}

	public void setMouseButton(final int mouseButton) {
		this.mouseButton = mouseButton;
	}
}

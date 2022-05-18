/* Copyright (C) 2020  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import java.text.MessageFormat;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.VirtualAxisEditorBuilder;

abstract class ToAxisAction<V extends Number> extends InvertableAction<V> {

	@ActionProperty(label = "VIRTUAL_AXIS", editorBuilder = VirtualAxisEditorBuilder.class, order = 10)
	VirtualAxis virtualAxis = VirtualAxis.X;

	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty())
			return super.getDescription(input);

		return MessageFormat.format(Main.strings.getString("VJOY_AXIS_NAME"), virtualAxis);
	}

	public VirtualAxis getVirtualAxis() {
		return virtualAxis;
	}

	public void setVirtualAxis(final VirtualAxis virtualAxis) {
		this.virtualAxis = virtualAxis;
	}
}

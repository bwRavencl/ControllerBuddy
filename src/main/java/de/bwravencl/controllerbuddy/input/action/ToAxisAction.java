/*
 * Copyright (C) 2014 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.VirtualAxisEditorBuilder;
import java.lang.constant.Constable;
import java.text.MessageFormat;

/// Abstract base class for actions that map controller input to a virtual
/// joystick axis.
///
/// Subclasses define how specific input types (analog axes, buttons, etc.) are
/// translated into axis values for the target [VirtualAxis].
///
/// @param <V> the type of input value this action processes
public abstract class ToAxisAction<V extends Constable> extends InvertableAction<V> {

	/// The target virtual axis to which input values are mapped.
	@ActionProperty(title = "VIRTUAL_AXIS_TITLE", description = "VIRTUAL_AXIS_DESCRIPTION", editorBuilder = VirtualAxisEditorBuilder.class, order = 10)
	VirtualAxis virtualAxis = VirtualAxis.X;

	/// Returns a description including the target virtual axis name.
	///
	/// @param input the current input state
	/// @return the action description
	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty()) {
			return super.getDescription(input);
		}

		return MessageFormat.format(Main.STRINGS.getString("JOYSTICK_AXIS_NAME"), virtualAxis);
	}

	/// Returns the target virtual axis.
	///
	/// @return the virtual axis
	public VirtualAxis getVirtualAxis() {
		return virtualAxis;
	}

	/// Sets the target virtual axis.
	///
	/// @param virtualAxis the virtual axis to target
	public void setVirtualAxis(final VirtualAxis virtualAxis) {
		this.virtualAxis = virtualAxis;
	}
}

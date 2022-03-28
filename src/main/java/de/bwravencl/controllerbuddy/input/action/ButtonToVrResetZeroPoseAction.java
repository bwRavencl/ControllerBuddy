/* Copyright (C) 2022  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;

@Action(label = "TO_VR_RESET_ZERO_POSE_ACTION", category = ActionCategory.BUTTON_AND_CYCLES, order = 610)
public class ButtonToVrResetZeroPoseAction extends ToVrResetZeroPoseAction<Byte> implements IButtonToAction {

	@Override
	public void doAction(final Input input, final int component, Byte value) {
		value = handleLongPress(input, component, value);
		handleAction(value != 0, input);
	}
}

/* Copyright (C) 2022  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.extension;

import java.util.List;

import org.lwjgl.glfw.GLFWGamepadState;

import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.extension.sony.DualSenseExtension;
import de.bwravencl.controllerbuddy.input.extension.sony.DualShock4Extension;

public abstract class InputExtension {

	public static InputExtension getIfAvailable(final Input input, final List<ControllerInfo> presentControllers,
			final ControllerInfo selectedController) {
		InputExtension inputExtension = DualShock4Extension.getIfAvailable(input, presentControllers,
				selectedController);
		if (inputExtension != null)
			return inputExtension;

		inputExtension = DualSenseExtension.getIfAvailable(input, presentControllers, selectedController);
		if (inputExtension != null)
			return inputExtension;

		return XInputExtension.getIfAvailable(input, presentControllers, selectedController);
	}

	protected final Input input;
	protected final ControllerInfo controller;
	protected volatile boolean ready;

	protected InputExtension(final Input input, final ControllerInfo controller) {
		this.input = input;
		this.controller = controller;
	}

	public void deInit(final boolean disconnected) {
		ready = false;
	}

	public abstract boolean getGamepadState(final GLFWGamepadState state);

	public abstract String getTooltip(String title);

	public boolean isReady() {
		return ready;
	}

	public abstract void rumbleLight();

	public abstract void rumbleStrong();
}

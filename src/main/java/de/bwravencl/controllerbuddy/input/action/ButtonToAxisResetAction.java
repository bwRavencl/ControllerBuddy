/*
 * Copyright (C) 2015 Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.AxisValueEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DelayEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.VirtualAxisEditorBuilder;
import java.text.MessageFormat;

/// Maps a button press to resetting a virtual axis to a configurable value.
///
/// When activated, the target [VirtualAxis] is set (or moved, if fluid mode is
/// enabled) to the configured reset value. Supports configurable activation
/// modes and delay.
@Action(icon = "🕹", title = "BUTTON_TO_AXIS_RESET_ACTION_TITLE", description = "BUTTON_TO_AXIS_RESET_ACTION_DESCRIPTION", category = ActionCategory.BUTTON_AND_CYCLES, order = 135)
public final class ButtonToAxisResetAction extends DescribableAction<Boolean>
		implements IButtonToDelayableAction, IActivatableAction<Boolean> {

	/// Default fluid mode enabled state.
	private static final boolean DEFAULT_FLUID = false;

	/// Default value to which the virtual axis is reset.
	private static final float DEFAULT_RESET_VALUE = 0f;

	/// Transient activatable state used for edge-triggered activation modes.
	private transient Activatable activatable;

	/// Activation mode that determines when the axis reset fires.
	@ActionProperty(icon = "🚀", title = "ACTIVATION_TITLE", description = "ACTIVATION_DESCRIPTION", editorBuilder = ActivationEditorBuilder.class, order = 40)
	private Activation activation = Activation.ON_PRESS;

	/// Delay in milliseconds before this action becomes active.
	@ActionProperty(icon = "⏱️", title = "DELAY_TITLE", description = "DELAY_DESCRIPTION", editorBuilder = DelayEditorBuilder.class, order = 50)
	private long delay = DEFAULT_DELAY;

	/// Whether to use relative movement instead of absolute axis positioning.
	@ActionProperty(icon = "∿", title = "FLUID_TITLE", description = "FLUID_DESCRIPTION", editorBuilder = BooleanEditorBuilder.class, order = 30)
	private boolean fluid = DEFAULT_FLUID;

	/// Value to which the virtual axis is reset when this action fires.
	@ActionProperty(icon = "⏪", title = "RESET_VALUE_TITLE", description = "RESET_VALUE_DESCRIPTION", editorBuilder = AxisValueEditorBuilder.class, order = 20)
	private float resetValue = DEFAULT_RESET_VALUE;

	/// Virtual axis targeted by this reset action.
	@ActionProperty(icon = "✥", title = "VIRTUAL_AXIS_TITLE", description = "VIRTUAL_AXIS_DESCRIPTION", editorBuilder = VirtualAxisEditorBuilder.class, order = 10)
	private VirtualAxis virtualAxis = VirtualAxis.X;

	/// Processes a button input value and resets the configured virtual axis based
	/// on the current activation mode.
	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleDelay(input, component, value);

		switch (activation) {
		case WHILE_PRESSED -> {
			if (value) {
				resetAxis(input);
			}
		}
		case ON_PRESS -> {
			if (!value) {
				activatable = Activatable.YES;
			} else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				resetAxis(input);
			}
		}
		case ON_RELEASE -> {
			if (value) {
				if (activatable == Activatable.NO) {
					activatable = Activatable.YES;
				} else if (activatable == Activatable.DENIED_BY_OTHER_ACTION) {
					activatable = Activatable.NO;
				}
			} else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				resetAxis(input);
			}
		}
		}
	}

	@Override
	public Activatable getActivatable() {
		return activatable;
	}

	@Override
	public Activation getActivation() {
		return activation;
	}

	@Override
	public long getDelay() {
		return delay;
	}

	/// Returns a human-readable description, defaulting to a formatted message that
	/// includes the target virtual axis name.
	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty()) {
			return super.getDescription(input);
		}

		return MessageFormat.format(Main.STRINGS.getString("RESET_JOYSTICK_AXIS_NAME"), virtualAxis);
	}

	/// Returns the value to which the virtual axis is reset when this action fires.
	///
	/// @return the reset value
	public float getResetValue() {
		return resetValue;
	}

	/// Returns the virtual axis targeted by this reset action.
	///
	/// @return the virtual axis
	public VirtualAxis getVirtualAxis() {
		return virtualAxis;
	}

	/// Returns whether fluid mode is enabled, which uses relative movement instead
	/// of absolute axis positioning.
	///
	/// @return `true` if fluid mode is enabled
	public boolean isFluid() {
		return fluid;
	}

	/// Resets the target virtual axis to the configured reset value.
	///
	/// Uses [Input#moveAxis] if fluid mode is enabled, otherwise uses
	/// [Input#setAxis] for an absolute position reset.
	///
	/// @param input the current input state
	private void resetAxis(final Input input) {
		if (fluid) {
			input.moveAxis(virtualAxis, resetValue);
		} else {
			input.setAxis(virtualAxis, resetValue, false, null, null, null);
		}
	}

	@Override
	public void setActivatable(final Activatable activatable) {
		this.activatable = activatable;
	}

	@Override
	public void setActivation(final Activation activation) {
		this.activation = activation;
	}

	@Override
	public void setDelay(final long delay) {
		this.delay = delay;
	}

	/// Sets whether fluid mode is enabled.
	///
	/// @param fluid `true` to enable fluid mode
	public void setFluid(final boolean fluid) {
		this.fluid = fluid;
	}

	/// Sets the value to which the virtual axis is reset.
	///
	/// @param resetValue the reset value
	public void setResetValue(final float resetValue) {
		this.resetValue = resetValue;
	}

	/// Sets the virtual axis targeted by this reset action.
	///
	/// @param virtualAxis the virtual axis
	public void setVirtualAxis(final VirtualAxis virtualAxis) {
		this.virtualAxis = virtualAxis;
	}
}

/* Copyright (C) 2015  Matteo Hausner
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

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.AxisValueEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.VirtualAxisEditorBuilder;
import java.text.MessageFormat;

@Action(label = "BUTTON_TO_AXIS_RESET_ACTION", category = ActionCategory.BUTTON_AND_CYCLES, order = 135)
public final class ButtonToAxisResetAction extends DescribableAction<Boolean>
		implements IButtonToAction, IActivatableAction<Boolean> {

	private static final boolean DEFAULT_FLUID = false;

	private static final float DEFAULT_RESET_VALUE = 0f;

	private transient Activatable activatable;

	@ActionProperty(label = "ACTIVATION", editorBuilder = ActivationEditorBuilder.class, order = 40)
	private Activation activation = Activation.ON_PRESS;

	@ActionProperty(label = "FLUID", editorBuilder = BooleanEditorBuilder.class, order = 30)
	private boolean fluid = DEFAULT_FLUID;

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 50)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@ActionProperty(label = "RESET_VALUE", editorBuilder = AxisValueEditorBuilder.class, order = 20)
	private float resetValue = DEFAULT_RESET_VALUE;

	@ActionProperty(label = "VIRTUAL_AXIS", editorBuilder = VirtualAxisEditorBuilder.class, order = 10)
	private VirtualAxis virtualAxis = VirtualAxis.X;

	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleLongPress(input, component, value);

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
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty()) {
			return super.getDescription(input);
		}

		return MessageFormat.format(Main.STRINGS.getString("RESET_JOYSTICK_AXIS_NAME"), virtualAxis);
	}

	public float getResetValue() {
		return resetValue;
	}

	public VirtualAxis getVirtualAxis() {
		return virtualAxis;
	}

	public boolean isFluid() {
		return fluid;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

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

	public void setFluid(final boolean fluid) {
		this.fluid = fluid;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}

	public void setResetValue(final float resetValue) {
		this.resetValue = resetValue;
	}

	public void setVirtualAxis(final VirtualAxis virtualAxis) {
		this.virtualAxis = virtualAxis;
	}
}

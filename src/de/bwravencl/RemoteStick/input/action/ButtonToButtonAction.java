package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class ButtonToButtonAction extends ToButtonAction implements IAction {

	@Override
	public void doAction(Input joystick, float value) {
		joystick.setButtons(buttonId, invert ? -value : value);
	}

}

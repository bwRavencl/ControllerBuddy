package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class ButtonToButtonAction extends ToButtonAction implements IAction {

	@Override
	public void doAction(Joystick joystick, float rValue) {
		if (vButtonId != Joystick.ID_BUTTON_NONE)
			joystick.setButtons(vButtonId, invert ? -rValue : rValue);
	}

}

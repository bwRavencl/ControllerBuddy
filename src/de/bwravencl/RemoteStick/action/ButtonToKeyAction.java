package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class ButtonToKeyAction extends ToKeyAction implements IAction {

	@Override
	public void doAction(Joystick joystick, float rValue) {
		if ((rValue < 0.5f) && !invert)
			joystick.getDownKeys().remove(keyCode);
		else
			joystick.getDownKeys().add(keyCode);
	}

}

package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class ButtonToScrollAction extends ToScrollAction implements IAction {

	@Override
	public void doAction(Input joystick, float value) {
		if (value > 0.5f)
			joystick.setScrollClicks(joystick.getScrollClicks()
					+ (invert ? -clicks : clicks));
	}
	
}

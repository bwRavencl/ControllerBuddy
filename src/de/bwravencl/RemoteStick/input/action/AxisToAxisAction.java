package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToAxisAction extends ToAxisAction implements IAction {
	
	@Override
	public void doAction(Input joystick, float value) {
		if (axisId != Input.ID_AXIS_NONE)
			joystick.setAxis(axisId, invert ? -value : value);
	}

}

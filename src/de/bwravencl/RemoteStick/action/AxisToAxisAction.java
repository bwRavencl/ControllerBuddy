package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class AxisToAxisAction extends ToAxisAction implements IAction {
	
	@Override
	public void doAction(Joystick joystick, float rValue) {
		if (vAxisId != Joystick.ID_AXIS_NONE)
		{
			/*if (Math.abs(rValue) < joystick.getDeadZone())
				rValue = 0.0f;*/
			
			joystick.setAxis(vAxisId, invert ? -rValue : rValue);
		}
	}

}

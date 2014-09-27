package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class AxisToAxisAction extends InvertableAction implements IAction {

	protected int vAxisId = Joystick.ID_AXIS_NONE;

	public int getvAxisId() {
		return vAxisId;
	}

	public void setvAxisId(int vAxisId) {
		this.vAxisId = vAxisId;
	}

	@Override
	public void doAction(Joystick joystick, float rValue) {
		if (vAxisId != Joystick.ID_AXIS_NONE)
			joystick.setAxis(vAxisId, invert ? -rValue : rValue);
	}

}

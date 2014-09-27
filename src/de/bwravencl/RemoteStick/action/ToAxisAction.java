package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public abstract class ToAxisAction extends InvertableAction {

	protected int vAxisId = Joystick.ID_AXIS_NONE;

	public int getvAxisId() {
		return vAxisId;
	}

	public void setvAxisId(int vAxisId) {
		this.vAxisId = vAxisId;
	}

}

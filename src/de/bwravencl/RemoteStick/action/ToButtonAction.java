package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public abstract class ToButtonAction extends InvertableAction {
	
	protected int vButtonId = Joystick.ID_BUTTON_NONE;

	public int getvButtonId() {
		return vButtonId;
	}

	public void setvButtonId(int vButtonId) {
		this.vButtonId = vButtonId;
	}
	
}

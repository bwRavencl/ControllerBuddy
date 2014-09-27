package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class ButtonToButtonAction extends InvertableAction implements IAction {

	private int vButtonId = Joystick.ID_BUTTON_NONE;

	public int getvButtonId() {
		return vButtonId;
	}

	public void setvButtonId(int vButtonId) {
		this.vButtonId = vButtonId;
	}

	@Override
	public void doAction(Joystick joystick, float rValue) {
		if (vButtonId != Joystick.ID_BUTTON_NONE)
			joystick.setButtons(vButtonId, invert ? -rValue : rValue);
	}

}

package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class AxisToButtonAction extends InvertableAction implements IAction {

	private int vButtonId = Joystick.ID_BUTTON_NONE;
	private float minAxisValueButtonDown = 0.5f;
	private float maxAxisValueButtonDown = 1.0f;

	public int getvButtonId() {
		return vButtonId;
	}

	public void setvButtonId(int vButtonId) {
		this.vButtonId = vButtonId;
	}

	@Override
	public void doAction(Joystick joystick, float rValue) {
		boolean down = (rValue >= minAxisValueButtonDown && rValue <= maxAxisValueButtonDown);

		joystick.setButtons(vButtonId, invert ? !down : invert);
	}

}

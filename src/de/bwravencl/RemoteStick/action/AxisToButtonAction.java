package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class AxisToButtonAction extends ToButtonAction implements IAction {

	private float minAxisValueButtonDown = 0.5f;
	private float maxAxisValueButtonDown = 1.0f;
	
	public float getMinAxisValueButtonDown() {
		return minAxisValueButtonDown;
	}
	
	public void setMinAxisValueButtonDown(float minAxisValueButtonDown) {
		this.minAxisValueButtonDown = minAxisValueButtonDown;
	}
	
	public float getMaxAxisValueButtonDown() {
		return maxAxisValueButtonDown;
	}
	
	public void setMaxAxisValueButtonDown(float maxAxisValueButtonDown) {
		this.maxAxisValueButtonDown = maxAxisValueButtonDown;
	}

	@Override
	public void doAction(Joystick joystick, float rValue) {
		boolean down = (rValue >= minAxisValueButtonDown && rValue <= maxAxisValueButtonDown);

		joystick.setButtons(vButtonId, invert ? !down : down);
	}

}

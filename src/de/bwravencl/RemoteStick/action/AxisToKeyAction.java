package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class AxisToKeyAction extends ToKeyAction implements IAction {

	private float minAxisValueKeyDown = 0.5f;
	private float maxAxisValueKeyDown = 1.0f;

	public float getMinAxisValueKeyDown() {
		return minAxisValueKeyDown;
	}

	public void setMinAxisValueKeyDown(float minAxisValueKeyDown) {
		this.minAxisValueKeyDown = minAxisValueKeyDown;
	}

	public float getMaxAxisValueKeyDown() {
		return maxAxisValueKeyDown;
	}

	public void setMaxAxisValueKeyDown(float maxAxisValueKeyDown) {
		this.maxAxisValueKeyDown = maxAxisValueKeyDown;
	}

	@Override
	public void doAction(Joystick joystick, float rValue) {
		if ((rValue >= minAxisValueKeyDown && rValue <= maxAxisValueKeyDown)
				&& !invert)
			joystick.getDownKeys().add(keyCode);
		else
			joystick.getDownKeys().remove(keyCode);
	}
}

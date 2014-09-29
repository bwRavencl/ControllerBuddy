package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToButtonAction extends ToButtonAction implements IAction {

	public static final float DEFAULT_MIN_AXIS_VALUE = 0.5f;
	public static final float DEFAULT_MAX_AXIS_VALUE = 1.0f;

	private float minAxisValue = 0.5f;
	private float maxAxisValue = 1.0f;

	public float getMinAxisValue() {
		return minAxisValue;
	}

	public void setMinAxisValue(float minAxisValue) {
		this.minAxisValue = minAxisValue;
	}

	public float getMaxAxisValue() {
		return maxAxisValue;
	}

	public void setMaxAxisValue(float maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	@Override
	public void doAction(Input joystick, float value) {
		boolean down = (value >= minAxisValue && value <= maxAxisValue);

		joystick.setButtons(buttonId, invert ? !down : down);
	}

}

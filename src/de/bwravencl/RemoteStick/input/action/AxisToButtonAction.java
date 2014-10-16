package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToButtonAction extends ToButtonAction {

	public static final float DEFAULT_MIN_AXIS_VALUE = 0.5f;
	public static final float DEFAULT_MAX_AXIS_VALUE = 1.0f;

	private float minAxisValue = 0.5f;
	private float maxAxisValue = 1.0f;

	public float getMinAxisValue() {
		return minAxisValue;
	}

	public void setMinAxisValue(Float minAxisValue) {
		this.minAxisValue = minAxisValue;
	}

	public float getMaxAxisValue() {
		return maxAxisValue;
	}

	public void setMaxAxisValue(Float maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	@Override
	public void doAction(Input input, float value) {
		boolean down = (value >= minAxisValue && value <= maxAxisValue);

		input.setButtons(buttonId, invert ? !down : down);
	}

}

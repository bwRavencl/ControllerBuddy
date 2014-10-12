package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToKeyAction extends ToKeyAction implements IAction {

	public static final float DEFAULT_MIN_AXIS_VALUE = 0.5f;
	public static final float DEFAULT_MAX_AXIS_VALUE = 1.0f;

	private float minAxisValue = DEFAULT_MIN_AXIS_VALUE;
	private float maxAxisValue = DEFAULT_MAX_AXIS_VALUE;

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
		if ((value >= minAxisValue && value <= maxAxisValue) && !invert) {
			if (downUp) {
				if (wasUp) {
					input.getDownUpKeyStrokes().add(keystroke);
					wasUp = false;
				}
			} else {
				for (String s : keystroke.getModifierCodes())
					input.getDownKeyCodes().add(s);
				for (String s : keystroke.getKeyCodes())
					input.getDownKeyCodes().add(s);
			}
		} else {
			if (downUp)
				wasUp = true;
			else {
				for (String s : keystroke.getModifierCodes())
					input.getDownKeyCodes().remove(s);
				for (String s : keystroke.getKeyCodes())
					input.getDownKeyCodes().remove(s);
			}
		}
	}
}

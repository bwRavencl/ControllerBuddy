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
				&& !invert) {
			if (downUp) {
				if (wasUp) {
					joystick.getDownUpKeyStrokes().add(keystroke);
					wasUp = false;
				}
			} else {
				for (String s : keystroke.getModifierCodes())
					joystick.getDownKeyCodes().add(s);
				for (String s : keystroke.getKeyCodes())
					joystick.getDownKeyCodes().add(s);
			}
		} else {
			if (downUp)
				wasUp = true;
			else {
				for (String s : keystroke.getModifierCodes())
					joystick.getDownKeyCodes().remove(s);
				for (String s : keystroke.getKeyCodes())
					joystick.getDownKeyCodes().remove(s);
			}
		}
	}
}

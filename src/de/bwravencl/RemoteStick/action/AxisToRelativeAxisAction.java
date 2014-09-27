package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;
import de.bwravencl.RemoteStick.Util;

public class AxisToRelativeAxisAction extends AxisToAxisAction {

	public final float DEFAULT_SENSITIVITY = 1.0f;

	private float sensitivity = 1.0f;

	public float getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(float sensitivity) {
		this.sensitivity = sensitivity;
	}

	@Override
	public void doAction(Joystick joystick, float rValue) {
		float d = 0.0f;

		if (rValue < 0.5f) {
			d = 1000L / joystick.getServerThread().getUpdateRate()
					* sensitivity
					* Util.normalize(rValue, -1.0f, 0.0f, 0.0f, 1.0f);
		} else if (rValue > 0.5f) {
			d = 1000L / joystick.getServerThread().getUpdateRate()
					* sensitivity
					* Util.normalize(rValue, 0.0f, 1.0f, 0.0f, 1.0f);
		}

		joystick.setAxis(vAxisId, joystick.getAxis()[vAxisId]
				+ (invert ? -d : d));
	}

}

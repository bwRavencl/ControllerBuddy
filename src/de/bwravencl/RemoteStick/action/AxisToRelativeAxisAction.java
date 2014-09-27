package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;
import de.bwravencl.RemoteStick.Util;

public class AxisToRelativeAxisAction extends AxisToAxisAction {

	public final float DEFAULT_DEAD_ZONE = 0.25f;
	public final float DEFAULT_SENSITIVITY = 1.0f;

	private float deadZone = DEFAULT_DEAD_ZONE;
	private float sensitivity = DEFAULT_SENSITIVITY;

	public float getDeadZone() {
		return deadZone;
	}

	public void setDeadZone(float deadZone) {
		this.deadZone = deadZone;
	}

	public float getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(float sensitivity) {
		this.sensitivity = sensitivity;
	}

	@Override
	public void doAction(Joystick joystick, float rValue) {
		if (Math.abs(rValue) > deadZone) {

			float d = rValue * sensitivity
					* (float) joystick.getServerThread().getUpdateRate()
					/ (float) 1000L;

			float oldValue = Util.normalize(joystick.getAxis()[vAxisId], 0.0f,
					joystick.getMaxAxisValue(), -1.0f, 1.0f);

			joystick.setAxis(vAxisId, oldValue + (invert ? -d : d));
		}
	}

}

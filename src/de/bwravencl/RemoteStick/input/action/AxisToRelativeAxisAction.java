package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToRelativeAxisAction extends AxisToAxisAction {

	public final float DEFAULT_DEAD_ZONE = 0.25f;
	public final float DEFAULT_SENSITIVITY = 1.0f;

	private float deadZone = DEFAULT_DEAD_ZONE;
	private float sensitivity = DEFAULT_SENSITIVITY;

	public float getDeadZone() {
		return deadZone;
	}

	public void setDeadZone(Float deadZone) {
		this.deadZone = deadZone;
	}

	public float getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(Float sensitivity) {
		this.sensitivity = sensitivity;
	}

	@Override
	public void doAction(Input joystick, float value) {
		if (Math.abs(value) > deadZone) {
			final float d = value * sensitivity
					* (float) joystick.getServerThread().getUpdateRate()
					/ (float) 1000L;

			final float oldValue = Input.normalize(
					joystick.getAxis().get(virtualAxis), 0.0f,
					joystick.getMaxAxisValue(), -1.0f, 1.0f);

			joystick.setAxis(virtualAxis, oldValue + (invert ? -d : d));
		}
	}

	@Override
	public String toString() {
		return rb.getString("AXIS_TO_RELATIVE_AXIS_ACTION_STRING");
	}

}

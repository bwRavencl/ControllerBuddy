package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.Util;
import de.bwravencl.RemoteStick.input.Input;

public class CursorAction extends InvertableAction implements IAction {

	public final float DEFAULT_DEAD_ZONE = 0.25f;
	public final float DEFAULT_MAX_SPEED = 5.0f;

	private float deadZone = DEFAULT_DEAD_ZONE;
	private float maxSpeed = DEFAULT_MAX_SPEED;

	public enum Axis {
		X, Y
	}

	private Axis axis = Axis.X;

	public float getDeadZone() {
		return deadZone;
	}

	public void setDeadZone(float deadZone) {
		this.deadZone = deadZone;
	}

	public float getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(float maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public Axis getAxis() {
		return axis;
	}

	public void setAxis(Axis axis) {
		this.axis = axis;
	}

	@Override
	public void doAction(Input joystick, float value) {
		if (Math.abs(value) > deadZone) {
			final float rateMultiplier = (float) joystick.getServerThread()
					.getUpdateRate() / (float) 1000L;

			final float d = Util.normalize(value * rateMultiplier, -1.0f
					* rateMultiplier, 1.0f * rateMultiplier, -maxSpeed,
					maxSpeed);

			if (axis.equals(Axis.X))
				joystick.setCursorDeltaX((int) (joystick.getCursorDeltaX() + (invert ? -d
						: d)));
			else
				joystick.setCursorDeltaY((int) (joystick.getCursorDeltaY() + (invert ? -d
						: d)));
		}
	}
	
}

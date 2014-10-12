package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToCursorAction extends InvertableAction implements IAction {

	public final float DEFAULT_DEAD_ZONE = 0.25f;
	public final float DEFAULT_MAX_SPEED = 750.0f;

	private float deadZone = DEFAULT_DEAD_ZONE;
	private float maxSpeed = DEFAULT_MAX_SPEED;

	public enum MouseAxis {
		X, Y
	}

	private MouseAxis axis = MouseAxis.X;

	public float getDeadZone() {
		return deadZone;
	}

	public void setDeadZone(Float deadZone) {
		this.deadZone = deadZone;
	}

	public float getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(Float maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public MouseAxis getAxis() {
		return axis;
	}

	public void setAxis(MouseAxis axis) {
		this.axis = axis;
	}

	@Override
	public void doAction(Input input, float value) {
		if (Math.abs(value) > deadZone) {
			final float rateMultiplier = (float) input.getServerThread()
					.getUpdateRate() / (float) 1000L;

			float d = Input.normalize(value, -1.0f, 1.0f, -maxSpeed, maxSpeed)
					* rateMultiplier;

			if (axis.equals(MouseAxis.X))
				input.setCursorDeltaX((int) (input.getCursorDeltaX() + (invert ? -d
						: d)));
			else
				input.setCursorDeltaY((int) (input.getCursorDeltaY() + (invert ? -d
						: d)));
		}
	}

	@Override
	public String toString() {
		return "Cursor";
	}

}

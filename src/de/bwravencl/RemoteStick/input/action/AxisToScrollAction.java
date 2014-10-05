package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToScrollAction extends ToScrollAction implements IAction {

	public final float DEFAULT_DEAD_ZONE = 0.25f;

	private float deadZone = DEFAULT_DEAD_ZONE;

	public float getDeadZone() {
		return deadZone;
	}

	public void setDeadZone(Float deadZone) {
		this.deadZone = deadZone;
	}

	@Override
	public void doAction(Input joystick, float value) {
		if (Math.abs(value) > deadZone) {
			final float rateMultiplier = (float) joystick.getServerThread()
					.getUpdateRate() / (float) 1000L;

			final float d = Input.normalize(value * rateMultiplier, -1.0f
					* rateMultiplier, 1.0f * rateMultiplier, -clicks, clicks);

			joystick.setScrollClicks((int) (joystick.getScrollClicks() + (invert ? -d
					: d)));
		}
	}

}

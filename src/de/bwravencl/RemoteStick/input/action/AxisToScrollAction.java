package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToScrollAction extends ToScrollAction {

	public final float DEFAULT_DEAD_ZONE = 0.25f;

	private float deadZone = DEFAULT_DEAD_ZONE;

	public float getDeadZone() {
		return deadZone;
	}

	public void setDeadZone(Float deadZone) {
		this.deadZone = deadZone;
	}

	@Override
	public void doAction(Input input, float value) {
		if (Math.abs(value) > deadZone) {
			final float rateMultiplier = (float) input.getServerThread()
					.getUpdateRate() / (float) 1000L;

			final float d = Input.normalize(value * rateMultiplier, -1.0f
					* rateMultiplier, 1.0f * rateMultiplier, -clicks, clicks);

			input.setScrollClicks((int) (input.getScrollClicks() + (invert ? -d
					: d)));
		}
	}

}

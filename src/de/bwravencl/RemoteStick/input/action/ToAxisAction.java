package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public abstract class ToAxisAction extends InvertableAction {

	public static final String description = "Axis";

	protected int axisId = Input.ID_AXIS_NONE;

	public int getAxisId() {
		return axisId;
	}

	public void setAxisId(Integer axisId) {
		this.axisId = axisId;
	}

	@Override
	public String toString() {
		return "Axis";
	}

}

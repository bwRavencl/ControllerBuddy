package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input.VirtualAxis;

public abstract class ToAxisAction extends InvertableAction {

	public static final String description = "Axis";

	protected VirtualAxis virtualAxis = VirtualAxis.X;

	public VirtualAxis getVirtualAxis() {
		return virtualAxis;
	}

	public void setVirtualAxis(VirtualAxis virtualAxis) {
		this.virtualAxis = virtualAxis;
	}

	@Override
	public String toString() {
		return "Axis";
	}

}

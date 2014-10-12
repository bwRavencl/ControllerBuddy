package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToAxisAction extends ToAxisAction implements IAction {

	@Override
	public void doAction(Input input, float value) {
		input.setAxis(virtualAxis, invert ? -value : value);
	}

}

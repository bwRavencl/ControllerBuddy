package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class ButtonToScrollAction extends ToScrollAction {

	@Override
	public void doAction(Input input, float value) {
		if (value > 0.5f)
			input.setScrollClicks(input.getScrollClicks()
					+ (invert ? -clicks : clicks));
	}
	
}

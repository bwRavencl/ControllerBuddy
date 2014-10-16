package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class ButtonToKeyAction extends ToKeyAction {

	@Override
	public void doAction(Input input, float value) {
		if ((value < 0.5f) && !invert) {
			if (downUp)
				wasUp = true;
			else {
				for (String s : keystroke.getModifierCodes())
					input.getDownKeyCodes().remove(s);
				for (String s : keystroke.getKeyCodes())
					input.getDownKeyCodes().remove(s);
			}
		} else {
			if (downUp) {
				if (wasUp) {
					input.getDownUpKeyStrokes().add(keystroke);
					wasUp = false;
				}
			} else {
				for (String s : keystroke.getModifierCodes())
					input.getDownKeyCodes().add(s);
				for (String s : keystroke.getKeyCodes())
					input.getDownKeyCodes().add(s);
			}
		}
	}

}

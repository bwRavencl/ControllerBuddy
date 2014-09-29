package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class ButtonToKeyAction extends ToKeyAction implements IAction {

	@Override
	public void doAction(Joystick joystick, float rValue) {
		if ((rValue < 0.5f) && !invert) {
			if (downUp)
				wasUp = true;
			else {
				for (String s : keystroke.getModifierCodes())
					joystick.getDownKeyCodes().remove(s);
				for (String s : keystroke.getKeyCodes())
					joystick.getDownKeyCodes().remove(s);
			}
		} else {
			if (downUp) {
				if (wasUp) {
					joystick.getDownUpKeyStrokes().add(keystroke);
					wasUp = false;
				}
			} else {
				for (String s : keystroke.getModifierCodes())
					joystick.getDownKeyCodes().add(s);
				for (String s : keystroke.getKeyCodes())
					joystick.getDownKeyCodes().add(s);
			}
		}
	}

}

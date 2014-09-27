package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public interface IAction {
	public void doAction(Joystick joystick, float rValue);
}

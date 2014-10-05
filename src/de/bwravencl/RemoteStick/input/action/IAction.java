package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public interface IAction {

	public void doAction(Input joystick, float value);

	public Object clone() throws CloneNotSupportedException;
}

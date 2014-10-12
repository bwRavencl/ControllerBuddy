package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public interface IAction extends Cloneable {

	public void doAction(Input input, float value);

	public Object clone() throws CloneNotSupportedException;
}

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;

public interface IInitializationAction extends IAction {

	void init(final Input input);

}

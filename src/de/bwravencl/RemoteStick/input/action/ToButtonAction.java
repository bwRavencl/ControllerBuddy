package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public abstract class ToButtonAction extends InvertableAction {
	
	protected int buttonId = Input.ID_BUTTON_NONE;

	public int getButtonId() {
		return buttonId;
	}

	public void setButtonId(int buttonId) {
		this.buttonId = buttonId;
	}
	
}

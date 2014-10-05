package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public abstract class ToButtonAction extends InvertableAction {
	
	public static final String description = "Button";
	
	protected int buttonId = Input.ID_BUTTON_NONE;

	public int getButtonId() {
		return buttonId;
	}

	public void setButtonId(Integer buttonId) {
		this.buttonId = buttonId;
	}
	
	@Override
	public String toString() {
		return "Button";
	}
	
}

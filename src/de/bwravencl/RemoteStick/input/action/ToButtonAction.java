package de.bwravencl.RemoteStick.input.action;

public abstract class ToButtonAction extends InvertableAction {

	public static final String description = "Button";

	protected int buttonId = 0;

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

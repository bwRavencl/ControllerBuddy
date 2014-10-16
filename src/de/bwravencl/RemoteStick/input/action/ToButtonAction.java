package de.bwravencl.RemoteStick.input.action;

public abstract class ToButtonAction extends InvertableAction {

	protected int buttonId = 0;

	public int getButtonId() {
		return buttonId;
	}

	public void setButtonId(Integer buttonId) {
		this.buttonId = buttonId;
	}

	@Override
	public String toString() {
		return rb.getString("TO_BUTTON_ACTION_STRING");
	}

}

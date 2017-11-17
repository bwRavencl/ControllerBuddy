package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;

public class ButtonToOnScreenKeyboardAction implements IButtonToAction {

	private float activationValue = DEFAULT_ACTIVATION_VALUE;
	private boolean longPress = DEFAULT_LONG_PRESS;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public void doAction(final Input input, float value) {
		value = handleLongPress(value);

		if (value == activationValue)
			input.getMain().toggleOnScreenKeyboard();
	}

	@Override
	public float getActivationValue() {
		return activationValue;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void setActivationValue(final Float activationValue) {
		this.activationValue = activationValue;
	}

	@Override
	public void setLongPress(final Boolean longPress) {
		this.longPress = longPress;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_ON_SCREEN_KEYBOARD_ACTION_STRING");
	}

}

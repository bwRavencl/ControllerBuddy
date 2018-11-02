package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;

public class ButtonToPressOnScreenKeyboardKeyAction implements IButtonToAction {

	private boolean lockKey = false;

	private boolean longPress = DEFAULT_LONG_PRESS;

	private transient boolean wasUp = true;

	private transient boolean wasDown = false;

	private float activationValue = DEFAULT_ACTIVATION_VALUE;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public void doAction(final Input input, float value) {
		value = handleLongPress(input, value);

		if (!IButtonToAction.floatEquals(value, activationValue)) {
			if (lockKey)
				wasUp = true;
			else if (wasDown) {
				Main.onScreenKeyboard.releaseSelected();
				wasDown = false;
			} else
				wasDown = false;
		} else if (lockKey) {
			if (wasUp) {
				Main.onScreenKeyboard.toggleLock();
				wasUp = false;
			}
		} else {
			Main.onScreenKeyboard.pressSelected();
			wasDown = true;
		}
	}

	@Override
	public float getActivationValue() {
		return activationValue;
	}

	public boolean isLockKey() {
		return lockKey;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void setActivationValue(final float activationValue) {
		this.activationValue = activationValue;
	}

	public void setLockKey(final boolean lockKey) {
		this.lockKey = lockKey;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_PRESS_ON_SCREEN_KEYBOARD_KEY_ACTION_STRING");
	}

}

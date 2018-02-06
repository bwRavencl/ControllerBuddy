package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.Input;

public class ButtonToSelectOnScreenKeyboardKeyAction implements IButtonToAction {

	public enum Direction {
		UP, DOWN, LEFT, RIGHT
	}

	private static final long MIN_ELAPSE_TIME = 150L;

	private boolean longPress = DEFAULT_LONG_PRESS;

	private transient long lastPressTime;

	private float activationValue = DEFAULT_ACTIVATION_VALUE;

	private Direction direction = Direction.UP;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public void doAction(final Input input, float value) {
		value = handleLongPress(input, value);

		if (IButtonToAction.floatEquals(value, activationValue)) {
			final long currentTime = System.currentTimeMillis();
			if (currentTime - lastPressTime >= MIN_ELAPSE_TIME) {
				final OnScreenKeyboard onScreenKeyboard = input.getMain().getOnScreenKeyboard();

				switch (direction) {
				case UP:
					onScreenKeyboard.moveSelectorUp();
					break;
				case DOWN:
					onScreenKeyboard.moveSelectorDown();
					break;
				case LEFT:
					onScreenKeyboard.moveSelectorLeft();
					break;
				case RIGHT:
					onScreenKeyboard.moveSelectorRight();
					break;
				default:
					break;
				}

				lastPressTime = currentTime;
			}
		}
	}

	@Override
	public float getActivationValue() {
		return activationValue;
	}

	public Direction getDirection() {
		return direction;
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void setActivationValue(final Float activationValue) {
		this.activationValue = activationValue;
	}

	public void setDirection(final Direction direction) {
		this.direction = direction;
	}

	@Override
	public void setLongPress(final Boolean longPress) {
		this.longPress = longPress;
	}

	@Override
	public String toString() {
		return rb.getString("BUTTON_TO_SELECT_ON_SCREEN_KEYBOARD_KEY_ACTION_STRING");
	}

}

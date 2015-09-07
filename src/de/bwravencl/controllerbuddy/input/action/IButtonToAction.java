package de.bwravencl.controllerbuddy.input.action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.bwravencl.controllerbuddy.input.Input;

public interface IButtonToAction extends IAction {

	public static final long MIN_LONG_PRESS_TIME = 1000L;
	public static final boolean DEFAULT_LONG_PRESS = false;
	public static final float DEFAULT_ACTIVATION_VALUE = 1.0f;
	static final Set<IButtonToAction> actionToWasDown = new HashSet<IButtonToAction>();
	static final Map<IButtonToAction, Long> actionToDownSinceMap = new HashMap<IButtonToAction, Long>();

	public float getActivationValue();

	public void setActivationValue(Float activationValue);

	public boolean isLongPress();

	public void setLongPress(Boolean longPress);

	static boolean isDownUpAction(IAction action) {
		if (action instanceof ToKeyAction) {
			final ToKeyAction toKeyAction = (ToKeyAction) action;
			return toKeyAction.isDownUp();
		} else if (action instanceof ToMouseButtonAction) {
			final ToMouseButtonAction toMouseButtonAction = (ToMouseButtonAction) action;
			return toMouseButtonAction.isDownUp();
		}

		return false;
	}

	default float handleLongPress(float value) {
		final float activationValue = getActivationValue();

		if (isLongPress()) {
			final long currentTime = System.currentTimeMillis();

			if (value == activationValue) {
				if (!actionToDownSinceMap.containsKey(this))
					actionToDownSinceMap.put(this, currentTime);
				else if (currentTime - actionToDownSinceMap.get(this) >= MIN_LONG_PRESS_TIME)
					return value;
			} else if (actionToDownSinceMap.containsKey(this)) {
				if (currentTime - actionToDownSinceMap.get(this) >= MIN_LONG_PRESS_TIME) {
					for (List<IAction> actions : Input.getProfile().getActiveMode().getComponentToActionsMap()
							.values()) {
						if (actions.contains(this))
							actionToWasDown.removeAll(actions);
						break;
					}
				}
				actionToDownSinceMap.remove(this);
			}

			return activationValue - 1.0f;
		} else {
			if (isDownUpAction(this)) {
				if (value == activationValue)
					actionToWasDown.add(this);
				else if (actionToWasDown.contains(this)) {
					actionToWasDown.remove(this);
					return activationValue;
				}

				return activationValue - 1.0f;
			} else
				return value;
		}
	}
}

/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input;

import java.awt.EventQueue;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjgl.system.MemoryStack;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.gui.Main.HotSwappingButton;
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAxisToLongPressAction;
import de.bwravencl.controllerbuddy.input.action.IButtonToAction;
import de.bwravencl.controllerbuddy.input.action.IInitializationAction;
import de.bwravencl.controllerbuddy.input.action.IResetableAction;
import de.bwravencl.controllerbuddy.input.driver.Driver;
import de.bwravencl.controllerbuddy.input.driver.IGamepadStateProvider;
import de.bwravencl.controllerbuddy.runmode.RunMode;

public final class Input {

	public enum VirtualAxis {
		X, Y, Z, RX, RY, RZ, S0, S1
	}

	private static final Logger log = Logger.getLogger(Input.class.getName());

	private static final float AXIS_MOVEMENT_MIN_DELTA_FACTOR = 0.1f;

	private static final float AXIS_MOVEMENT_MAX_DELTA_FACTOR = 4f;

	private static final float ABORT_SUSPENSION_ACTION_DEADZONE = 0.25f;

	private static final long SUSPENSION_TIME = 500L;

	public static final int MAX_N_BUTTONS = 128;

	private static final long HOT_SWAP_POLL_INTERVAL = 50L;

	private static final long HOT_SWAP_POLL_INITIAL_SUSPENSION_INTERVAL = 2000L;

	private static float clamp(final float v) {
		return Math.min(Math.max(v, -1f), 1f);
	}

	private static double correctNumericalImprecision(final double d) {
		if (d < 0.0000001)
			return 0d;
		return d;
	}

	private static boolean isValidButton(final int button) {
		return button >= 0 && button <= GLFW.GLFW_GAMEPAD_BUTTON_LAST;
	}

	private static void mapCircularAxesToSquareAxes(final GLFWGamepadState state, final int xAxisIndex,
			final int yAxisIndex) {
		final var u = clamp(state.axes(xAxisIndex));
		final var v = clamp(state.axes(yAxisIndex));

		final var u2 = u * u;
		final var v2 = v * v;

		final var subtermX = 2d + u2 - v2;
		final var subtermY = 2d - u2 + v2;

		final var twoSqrt2 = 2d * Math.sqrt(2d);

		var termX1 = subtermX + u * twoSqrt2;
		var termX2 = subtermX - u * twoSqrt2;
		var termY1 = subtermY + v * twoSqrt2;
		var termY2 = subtermY - v * twoSqrt2;

		termX1 = correctNumericalImprecision(termX1);
		termY1 = correctNumericalImprecision(termY1);
		termX2 = correctNumericalImprecision(termX2);
		termY2 = correctNumericalImprecision(termY2);

		final var x = 0.5 * Math.sqrt(termX1) - 0.5 * Math.sqrt(termX2);
		final var y = 0.5 * Math.sqrt(termY1) - 0.5 * Math.sqrt(termY2);

		state.axes(xAxisIndex, clamp((float) x));
		state.axes(yAxisIndex, clamp((float) y));
	}

	public static float normalize(final float value, final float inMin, final float inMax, final float outMin,
			final float outMax) {
		final var oldRange = inMax - inMin;
		if (oldRange == 0f)
			return outMin;

		final var newRange = outMax - outMin;

		return (value - inMin) * newRange / oldRange + outMin;
	}

	private final Main main;
	private final ControllerInfo controller;
	private final EnumMap<VirtualAxis, Integer> axes;
	private Profile profile;
	private RunMode runMode;
	private boolean[] buttons;
	private volatile int cursorDeltaX;
	private volatile int cursorDeltaY;
	private volatile int scrollClicks;
	private final Set<Integer> downMouseButtons = ConcurrentHashMap.newKeySet();
	private final Set<Integer> downUpMouseButtons = new HashSet<>();
	private final Set<KeyStroke> downKeyStrokes = new HashSet<>();
	private final Set<KeyStroke> downUpKeyStrokes = new HashSet<>();
	private final Set<LockKey> onLockKeys = new HashSet<>();
	private final Set<LockKey> offLockKeys = new HashSet<>();
	private boolean clearOnNextPoll;
	private boolean repeatModeActionWalk;
	private final Map<VirtualAxis, Integer> virtualAxisToTargetValueMap = new HashMap<>();
	private long lastPollTime;
	private long lastHotSwapPollTime;
	private float rateMultiplier;
	private final Map<Integer, Long> axisToEndSuspensionTimestampMap = new HashMap<>();
	private Driver driver;
	private final Map<Integer, Driver> jidToDriverMap = new HashMap<>();
	private final Set<Integer> hotSwappingButtonDownJids = new HashSet<>();
	private float planckLength;
	private int hotSwappingButtonId = HotSwappingButton.None.id;
	private boolean skipAxisInitialization;
	private boolean initialized;
	private boolean hapticFeedbackEnabled;

	public Input(final Main main, final ControllerInfo controller, final EnumMap<VirtualAxis, Integer> axes) {
		this.main = main;
		this.controller = controller;

		skipAxisInitialization = axes != null;

		if (skipAxisInitialization)
			this.axes = axes;
		else {
			this.axes = new EnumMap<>(VirtualAxis.class);
			EnumSet.allOf(Input.VirtualAxis.class).forEach(virtualAxis -> this.axes.put(virtualAxis, 0));
		}

		resetLastHotSwapPollTime();

		profile = new Profile();
	}

	public void deInit(final boolean deviceDisconnected) {
		if (driver != null) {
			driver.deInit(deviceDisconnected);
			driver = null;
		}
	}

	public int floatToIntAxisValue(float value) {
		value = Math.max(value, -1f);
		value = Math.min(value, 1f);

		final var minAxisValue = runMode.getMinAxisValue();
		final var maxAxisValue = runMode.getMaxAxisValue();

		return (int) normalize(value, -1f, 1f, minAxisValue, maxAxisValue);
	}

	public EnumMap<VirtualAxis, Integer> getAxes() {
		return axes;
	}

	public boolean[] getButtons() {
		return buttons;
	}

	public ControllerInfo getController() {
		return controller;
	}

	public int getCursorDeltaX() {
		return cursorDeltaX;
	}

	public int getCursorDeltaY() {
		return cursorDeltaY;
	}

	public Set<KeyStroke> getDownKeyStrokes() {
		return downKeyStrokes;
	}

	public Set<Integer> getDownMouseButtons() {
		return downMouseButtons;
	}

	public Set<KeyStroke> getDownUpKeyStrokes() {
		return downUpKeyStrokes;
	}

	public Set<Integer> getDownUpMouseButtons() {
		return downUpMouseButtons;
	}

	public Driver getDriver() {
		return driver;
	}

	public Main getMain() {
		return main;
	}

	public Set<LockKey> getOffLockKeys() {
		return offLockKeys;
	}

	public Set<LockKey> getOnLockKeys() {
		return onLockKeys;
	}

	public float getPlanckLength() {
		return planckLength;
	}

	public Profile getProfile() {
		return profile;
	}

	public float getRateMultiplier() {
		return rateMultiplier;
	}

	public RunMode getRunMode() {
		return runMode;
	}

	public int getScrollClicks() {
		return scrollClicks;
	}

	public void init() {
		hapticFeedbackEnabled = main.getPreferences().getBoolean(Main.PREFERENCES_HAPTIC_FEEDBACK, true);

		final var presentControllers = Main.getPresentControllers();

		if (controller != null)
			driver = Driver.getIfAvailable(this, presentControllers, controller);

		if (presentControllers.size() > 1) {
			hotSwappingButtonId = main.getSelectedHotSwappingButtonId();

			if (hotSwappingButtonId != HotSwappingButton.None.id && controller != null) {
				if (driver != null)
					jidToDriverMap.put(controller.jid(), driver);

				for (final var controller : presentControllers) {
					if (controller.jid() == this.controller.jid())
						continue;

					final var driver = Driver.getIfAvailable(this, presentControllers, controller);
					if (driver != null)
						jidToDriverMap.put(controller.jid(), driver);
				}
			}
		}

		planckLength = 2f / (runMode.getMaxAxisValue() - runMode.getMinAxisValue());

		profile.getModes().forEach(mode -> mode.getAllActions().forEach(action -> {
			if (action instanceof final IInitializationAction<?> initializationAction)
				initializationAction.init(this);
		}));

		initialized = true;
	}

	public void initButtons() {
		buttons = new boolean[Math.min(runMode.getnButtons(), MAX_N_BUTTONS)];
	}

	public boolean isAxisSuspended(final int axis) {
		return axisToEndSuspensionTimestampMap.containsKey(axis);
	}

	public boolean isHapticFeedbackEnabled() {
		return hapticFeedbackEnabled;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public boolean isSkipAxisInitialization() {
		return skipAxisInitialization;
	}

	public void moveAxis(final VirtualAxis virtualAxis, final float targetValue) {
		final var integerTargetValue = floatToIntAxisValue(targetValue);

		if (axes.get(virtualAxis) != integerTargetValue)
			virtualAxisToTargetValueMap.put(virtualAxis, integerTargetValue);
	}

	public boolean poll() {
		final var currentTime = System.currentTimeMillis();

		axisToEndSuspensionTimestampMap.values().removeIf(timestamp -> timestamp < currentTime);

		var elapsedTime = runMode.getPollInterval();
		if (lastPollTime > 0L)
			elapsedTime = currentTime - lastPollTime;
		lastPollTime = currentTime;
		rateMultiplier = (float) elapsedTime / 1000L;

		try (var stack = MemoryStack.stackPush()) {
			final var state = GLFWGamepadState.calloc(stack);

			if (hotSwappingButtonId != HotSwappingButton.None.id
					&& currentTime - lastHotSwapPollTime > HOT_SWAP_POLL_INTERVAL) {
				for (final var controller : Main.getPresentControllers()) {
					if (controller.jid() == this.controller.jid())
						continue;

					final boolean gotState;
					final var driver = jidToDriverMap.get(controller.jid());
					if (driver instanceof final IGamepadStateProvider gamepadStateProvider)
						gotState = gamepadStateProvider.getGamepadState(state);
					else
						gotState = GLFW.glfwGetGamepadState(controller.jid(), state);

					if (gotState)
						if (state.buttons(hotSwappingButtonId) != 0)
							hotSwappingButtonDownJids.add(controller.jid());
						else if (hotSwappingButtonDownJids.contains(controller.jid())) {
							log.log(Level.INFO,
									Main.assembleControllerLoggingMessage("Initiating hot swap to ", controller));

							hotSwappingButtonId = HotSwappingButton.None.id;
							EventQueue.invokeLater(() -> {
								main.setSelectedControllerAndUpdateInput(controller, axes);
								main.updateDeviceMenuSelection();
								main.restartLast();
							});

							break;
						}
				}

				lastHotSwapPollTime = currentTime;
			}

			final boolean gotState;
			if (driver instanceof final IGamepadStateProvider gamepadStateProvider) {
				if (!driver.isReady())
					return true;

				gotState = gamepadStateProvider.getGamepadState(state);
			} else
				gotState = GLFW.glfwGetGamepadState(controller.jid(), state);

			if (!gotState)
				return false;

			Arrays.fill(buttons, false);

			final var onScreenKeyboard = main.getOnScreenKeyboard();

			if (clearOnNextPoll) {
				downKeyStrokes.clear();
				downMouseButtons.clear();

				onScreenKeyboard.forceRepoll();

				clearOnNextPoll = false;
			}

			onScreenKeyboard.poll(this);

			virtualAxisToTargetValueMap.entrySet().removeIf(entry -> {
				final var virtualAxis = entry.getKey();
				final var targetValue = entry.getValue();

				final var currentValue = axes.get(virtualAxis);
				final var delta = targetValue - currentValue;
				if (delta != 0) {
					final var axisRange = runMode.getMaxAxisValue() - runMode.getMinAxisValue();

					final var deltaFactor = normalize(Math.abs(delta), 0, axisRange, AXIS_MOVEMENT_MIN_DELTA_FACTOR,
							AXIS_MOVEMENT_MAX_DELTA_FACTOR);

					final var d = Integer.signum(delta) * (int) (axisRange * deltaFactor * rateMultiplier);

					var newValue = currentValue + d;
					if (delta > 0)
						newValue = Math.min(newValue, targetValue);
					else
						newValue = Math.max(newValue, targetValue);

					setAxis(virtualAxis, newValue, false, (Integer) null);

					return newValue == targetValue;
				}

				return true;
			});

			final var modes = profile.getModes();
			final var activeMode = profile.getActiveMode();
			final var axisToActionMap = activeMode.getAxisToActionsMap();
			final var buttonToActionMap = activeMode.getButtonToActionsMap();

			mapCircularAxesToSquareAxes(state, GLFW.GLFW_GAMEPAD_AXIS_LEFT_X, GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y);
			mapCircularAxesToSquareAxes(state, GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X, GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);

			for (var axis = 0; axis <= GLFW.GLFW_GAMEPAD_AXIS_LAST; axis++) {
				final var axisValue = state.axes(axis);

				if (Math.abs(axisValue) <= ABORT_SUSPENSION_ACTION_DEADZONE)
					axisToEndSuspensionTimestampMap.remove(axis);

				var actions = axisToActionMap.get(axis);
				if (actions == null) {
					final var buttonToModeActionStack = ButtonToModeAction.getButtonToModeActionStack();
					for (var i = 1; i < buttonToModeActionStack.size(); i++) {
						actions = buttonToModeActionStack.get(i).getMode(this).getAxisToActionsMap().get(axis);

						if (actions != null)
							break;
					}
				}

				if (actions == null)
					actions = modes.get(0).getAxisToActionsMap().get(axis);

				if (actions != null)
					for (final var action : actions)
						action.doAction(this, axis, axisValue);
			}

			for (var button = 0; button <= GLFW.GLFW_GAMEPAD_BUTTON_LAST; button++) {
				var actions = buttonToActionMap.get(button);
				if (actions == null) {
					final var buttonToModeActionStack = ButtonToModeAction.getButtonToModeActionStack();
					for (var i = 1; i < buttonToModeActionStack.size(); i++) {
						actions = buttonToModeActionStack.get(i).getMode(this).getButtonToActionsMap().get(button);

						if (actions != null)
							break;
					}
				}

				if (actions == null)
					actions = modes.get(0).getButtonToActionsMap().get(button);

				if (actions != null)
					for (final var action : actions)
						action.doAction(this, button, state.buttons(button));
			}

			for (;;) {
				for (var button = 0; button <= GLFW.GLFW_GAMEPAD_BUTTON_LAST; button++) {
					final var buttonToModeActions = profile.getButtonToModeActionsMap().get(button);
					if (buttonToModeActions != null)
						for (final var action : buttonToModeActions)
							action.doAction(this, button, state.buttons(button));
				}

				if (!repeatModeActionWalk)
					break;
				repeatModeActionWalk = false;
			}
		}

		EventQueue.invokeLater(() -> main.updateOverlayAxisIndicators(false));
		main.handleOnScreenKeyboardModeChange();

		return true;
	}

	public void repeatModeActionWalk() {
		repeatModeActionWalk = true;
	}

	public void reset() {
		clearOnNextPoll = false;
		repeatModeActionWalk = false;
		skipAxisInitialization = false;
		initialized = false;
		lastPollTime = 0;
		rateMultiplier = 0f;
		virtualAxisToTargetValueMap.clear();
		axisToEndSuspensionTimestampMap.clear();
		jidToDriverMap.clear();
		hotSwappingButtonDownJids.clear();
		hotSwappingButtonId = HotSwappingButton.None.id;

		resetLastHotSwapPollTime();

		profile.setActiveMode(this, 0);

		IAxisToLongPressAction.reset();
		IButtonToAction.reset();

		profile.getButtonToModeActionsMap().values().forEach(buttonToModeActions -> buttonToModeActions
				.forEach(buttonToModeAction -> buttonToModeAction.reset(this)));

		profile.getModes().forEach(mode -> mode.getAllActions().forEach(action -> {
			if (action instanceof final IResetableAction resetableAction)
				resetableAction.reset(this);
		}));
	}

	private void resetLastHotSwapPollTime() {
		lastHotSwapPollTime = System.currentTimeMillis() + HOT_SWAP_POLL_INITIAL_SUSPENSION_INTERVAL;
	}

	void scheduleClearOnNextPoll() {
		clearOnNextPoll = true;
	}

	public void setAxis(final VirtualAxis virtualAxis, final float value, final boolean hapticFeedback,
			final Float dententValue) {
		setAxis(virtualAxis, floatToIntAxisValue(value), hapticFeedback,
				dententValue != null ? floatToIntAxisValue(dententValue) : null);
	}

	private void setAxis(final VirtualAxis virtualAxis, int value, final boolean hapticFeedback,
			final Integer dententValue) {
		final var minAxisValue = runMode.getMinAxisValue();
		final var maxAxisValue = runMode.getMaxAxisValue();

		value = Math.max(value, minAxisValue);
		value = Math.min(value, maxAxisValue);

		final var prevValue = axes.put(virtualAxis, value);

		if (hapticFeedbackEnabled && hapticFeedback && driver != null && prevValue != null && prevValue != value)
			if (value == minAxisValue || value == maxAxisValue)
				driver.rumbleStrong();
			else if (dententValue != null && (prevValue > dententValue && value <= dententValue
					|| prevValue < dententValue && value >= dententValue))
				driver.rumbleLight();
	}

	public void setButton(final int id, final boolean value) {
		if (id < buttons.length)
			buttons[id] = value;
		else
			log.log(Level.WARNING, "Unable to set value for non-existent button " + id);
	}

	public void setCursorDeltaX(final int cursorDeltaX) {
		this.cursorDeltaX = cursorDeltaX;
	}

	public void setCursorDeltaY(final int cursorDeltaY) {
		this.cursorDeltaY = cursorDeltaY;
	}

	public boolean setProfile(final Profile profile) {
		if (profile == null)
			throw new IllegalArgumentException();

		for (final var button : profile.getButtonToModeActionsMap().keySet())
			if (!isValidButton(button))
				return false;

		final var modes = profile.getModes();
		modes.sort((o1, o2) -> {
			final var o1IsDefaultMode = Profile.defaultMode.equals(o1);
			final var o2IsDefaultMode = Profile.defaultMode.equals(o2);

			if (o1IsDefaultMode && o2IsDefaultMode)
				return 0;

			if (o1IsDefaultMode)
				return -1;

			if (o2IsDefaultMode)
				return 1;

			final var o1IsOnScreenKeyboardMode = OnScreenKeyboard.onScreenKeyboardMode.equals(o1);
			final var o2IsOnScreenKeyboardMode = OnScreenKeyboard.onScreenKeyboardMode.equals(o2);

			if (o1IsOnScreenKeyboardMode && o2IsOnScreenKeyboardMode)
				return 0;

			if (o1IsOnScreenKeyboardMode)
				return -1;

			if (o2IsOnScreenKeyboardMode)
				return 1;

			return o1.getDescription().compareTo(o2.getDescription());
		});

		for (final var mode : modes) {
			for (final var axis : mode.getAxisToActionsMap().keySet())
				if (axis < 0 || axis > GLFW.GLFW_GAMEPAD_AXIS_LAST)
					return false;

			for (final var button : mode.getButtonToActionsMap().keySet())
				if (!isValidButton(button))
					return false;

			for (final var actions : mode.getButtonToActionsMap().values())
				actions.sort((o1, o2) -> {
					if (o1 instanceof final IButtonToAction buttonToAction1
							&& o2 instanceof final IButtonToAction buttonToAction2) {
						final var o1IsLongPress = buttonToAction1.isLongPress();
						final var o2IsLongPress = buttonToAction2.isLongPress();

						if (o1IsLongPress && !o2IsLongPress)
							return -1;
						if (!o1IsLongPress && o2IsLongPress)
							return 1;
					}

					return 0;
				});
		}

		this.profile = profile;
		return true;
	}

	public void setRunMode(final RunMode runMode) {
		this.runMode = runMode;
	}

	public void setScrollClicks(final int scrollClicks) {
		this.scrollClicks = scrollClicks;
	}

	public void suspendAxis(final int axis) {
		axisToEndSuspensionTimestampMap.put(axis, System.currentTimeMillis() + SUSPENSION_TIME);
	}
}

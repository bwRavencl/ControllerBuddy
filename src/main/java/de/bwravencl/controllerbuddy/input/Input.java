/* Copyright (C) 2014  Matteo Hausner
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

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.Controller;
import de.bwravencl.controllerbuddy.gui.Main.HotSwappingButton;
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAxisToLongPressAction;
import de.bwravencl.controllerbuddy.input.action.IButtonToAction;
import de.bwravencl.controllerbuddy.input.action.IInitializationAction;
import de.bwravencl.controllerbuddy.input.action.IResetableAction;
import de.bwravencl.controllerbuddy.runmode.RunMode;
import java.awt.EventQueue;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDLProperties;

public final class Input {

	public static final int MAX_N_BUTTONS = 128;

	private static final float ABORT_SUSPENSION_ACTION_DEADZONE = 0.25f;

	private static final float AXIS_MOVEMENT_MAX_DELTA_FACTOR = 4f;

	private static final float AXIS_MOVEMENT_MIN_DELTA_FACTOR = 0.1f;

	private static final long HOT_SWAP_POLL_INITIAL_SUSPENSION_INTERVAL = 2000L;

	private static final long HOT_SWAP_POLL_INTERVAL = 50L;

	private static final Logger LOGGER = Logger.getLogger(Input.class.getName());

	private static final long SUSPENSION_TIME = 500L;

	private final EnumMap<VirtualAxis, Integer> axes;

	private final Map<Integer, Long> axisToEndSuspensionTimestampMap = new HashMap<>();

	private final Set<KeyStroke> downKeyStrokes = new HashSet<>();

	private final Set<Integer> downMouseButtons = new HashSet<>();

	private final Set<KeyStroke> downUpKeyStrokes = new HashSet<>();

	private final Set<Integer> downUpMouseButtons = new HashSet<>();

	private final Set<Integer> hotSwappingButtonDownInstanceIds = new HashSet<>();

	private final Main main;

	private final Set<LockKey> offLockKeys = new HashSet<>();

	private final Set<LockKey> onLockKeys = new HashSet<>();

	private final Map<Long, GamepadState> sdlGamepadToGamepadStateMap = new HashMap<>();

	private final Controller selectedController;

	private final Map<VirtualAxis, Integer> virtualAxisToTargetValueMap = new HashMap<>();

	private boolean[] buttons;

	private boolean clearOnNextPoll;

	private volatile int cursorDeltaX;

	private volatile int cursorDeltaY;

	private boolean hapticFeedback;

	private int hotSwappingButtonId = HotSwappingButton.None.id;

	private boolean initialized;

	private long lastHotSwapPollTime;

	private long lastPollTime;

	private boolean mapCircularAxesToSquareAxes;

	private float planckLength;

	private Profile profile;

	private float rateMultiplier;

	private boolean repeatModeActionWalk;

	private RunMode runMode;

	private RumbleEffect scheduledRumbleEffect;

	private volatile int scrollClicks;

	private long selectedSdlGamepad;

	private boolean skipAxisInitialization;

	private boolean swapLeftAndRightSticks;

	public Input(final Main main, final Controller selectedController, final EnumMap<VirtualAxis, Integer> axes) {
		this.main = main;
		this.selectedController = selectedController;

		skipAxisInitialization = axes != null;

		if (skipAxisInitialization) {
			this.axes = axes;
		} else {
			this.axes = new EnumMap<>(VirtualAxis.class);
			EnumSet.allOf(Input.VirtualAxis.class).forEach(virtualAxis -> this.axes.put(virtualAxis, 0));
		}

		resetLastHotSwapPollTime();

		profile = new Profile();
	}

	private static float clamp(final float v) {
		return Math.min(Math.max(v, -1f), 1f);
	}

	private static double correctNumericalImprecision(final double d) {
		if (d < 0.000_000_1) {
			return 0d;
		}
		return d;
	}

	private static boolean isValidButton(final int button) {
		return button > SDLGamepad.SDL_GAMEPAD_BUTTON_INVALID && button <= SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
	}

	public static float normalize(final float value, final float inMin, final float inMax, final float outMin,
			final float outMax) {
		final var oldRange = inMax - inMin;
		if (oldRange == 0f) {
			return outMin;
		}

		final var newRange = outMax - outMin;

		return (value - inMin) * newRange / oldRange + outMin;
	}

	public void deInit() {
		if (selectedSdlGamepad != 0) {
			final var gamepadProperties = SDLGamepad.SDL_GetGamepadProperties(selectedSdlGamepad);
			if (SDLProperties.SDL_GetBooleanProperty(gamepadProperties,
					SDLGamepad.SDL_PROP_GAMEPAD_CAP_PLAYER_LED_BOOLEAN, false)) {
				SDLGamepad.SDL_SetGamepadPlayerIndex(selectedSdlGamepad, -1);
			}
		}

		final var sdlGamepadsIterator = sdlGamepadToGamepadStateMap.keySet().iterator();
		while (sdlGamepadsIterator.hasNext()) {
			final var sdlGamepad = sdlGamepadsIterator.next();
			SDLGamepad.SDL_CloseGamepad(sdlGamepad);
			sdlGamepadsIterator.remove();
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

	public Controller getSelectedController() {
		return selectedController;
	}

	public boolean init() {
		if (initialized) {
			throw new IllegalStateException("Already initialized");
		}

		swapLeftAndRightSticks = main.isSwapLeftAndRightSticks();
		mapCircularAxesToSquareAxes = main.isMapCircularAxesToSquareAxes();

		sdlGamepadToGamepadStateMap.clear();
		for (final var controller : main.getControllers()) {
			if (!openController(controller) && controller.equals(selectedController)) {
				Main.logSdlError("Could not open gamepad");

				return false;
			}
		}

		planckLength = 2f / (runMode.getMaxAxisValue() - runMode.getMinAxisValue());

		profile.getModes().forEach(mode -> mode.getAllActions().forEach(action -> {
			if (action instanceof final IInitializationAction<?> initializationAction) {
				initializationAction.init(this);
			}
		}));

		initialized = true;

		return true;
	}

	public void initButtons() {
		buttons = new boolean[Math.min(runMode.getNumButtons(), MAX_N_BUTTONS)];
	}

	public boolean isAxisSuspended(final int axis) {
		return axisToEndSuspensionTimestampMap.containsKey(axis);
	}

	public boolean isInitialized() {
		return initialized;
	}

	public boolean isSkipAxisInitialization() {
		return skipAxisInitialization;
	}

	public void moveAxis(final VirtualAxis virtualAxis, final float targetValue) {
		final var integerTargetValue = floatToIntAxisValue(targetValue);

		if (axes.get(virtualAxis) != integerTargetValue) {
			virtualAxisToTargetValueMap.put(virtualAxis, integerTargetValue);
		}
	}

	public boolean openController(final Controller controller) {
		final var isSelectedController = controller.equals(selectedController);

		final var sdlGamepad = SDLGamepad.SDL_OpenGamepad(controller.instanceId());
		if (sdlGamepad == 0L) {
			return false;
		}

		sdlGamepadToGamepadStateMap.put(sdlGamepad, new GamepadState(sdlGamepad));
		updateHotSwappingButtonId();

		final var gamepadProperties = SDLGamepad.SDL_GetGamepadProperties(sdlGamepad);

		if (isSelectedController) {
			selectedSdlGamepad = sdlGamepad;

			if (main.isHapticFeedback()) {
				hapticFeedback = SDLProperties.SDL_GetBooleanProperty(gamepadProperties,
						SDLGamepad.SDL_PROP_GAMEPAD_CAP_RUMBLE_BOOLEAN, false);
			}
		}

		if (SDLProperties.SDL_GetBooleanProperty(gamepadProperties, SDLGamepad.SDL_PROP_GAMEPAD_CAP_RGB_LED_BOOLEAN,
				false)) {
			final var ledColor = main.getLedColor();
			SDLGamepad.SDL_SetGamepadLED(sdlGamepad, Integer.valueOf(ledColor.getRed()).byteValue(),
					Integer.valueOf(ledColor.getGreen()).byteValue(), Integer.valueOf(ledColor.getBlue()).byteValue());
		}

		if (SDLProperties.SDL_GetBooleanProperty(gamepadProperties, SDLGamepad.SDL_PROP_GAMEPAD_CAP_PLAYER_LED_BOOLEAN,
				false)) {
			SDLGamepad.SDL_SetGamepadPlayerIndex(sdlGamepad, isSelectedController ? 0 : -1);
		}

		return true;
	}

	public boolean poll() {
		Objects.requireNonNull(selectedController, "Field selectedController must not be null");

		final var currentTime = System.currentTimeMillis();

		axisToEndSuspensionTimestampMap.values().removeIf(timestamp -> timestamp < currentTime);

		var elapsedTime = runMode.getPollInterval();
		if (lastPollTime > 0L) {
			elapsedTime = currentTime - lastPollTime;
		}
		lastPollTime = currentTime;
		rateMultiplier = (float) elapsedTime / 1000L;

		if (hotSwappingButtonId != HotSwappingButton.None.id
				&& currentTime - lastHotSwapPollTime > HOT_SWAP_POLL_INTERVAL) {

			final var sdlGamepadToGamepadStateMapIterator = sdlGamepadToGamepadStateMap.entrySet().iterator();
			while (sdlGamepadToGamepadStateMapIterator.hasNext()) {
				final var entry = sdlGamepadToGamepadStateMapIterator.next();
				final var sdlGamepad = entry.getKey();

				if (sdlGamepad == selectedSdlGamepad) {
					continue;
				}

				final var gamepadState = entry.getValue();

				if (gamepadState.update()) {
					final var instanceId = SDLGamepad.SDL_GetGamepadID(sdlGamepad);

					if (gamepadState.buttons[hotSwappingButtonId]) {
						hotSwappingButtonDownInstanceIds.add(instanceId);
					} else if (hotSwappingButtonDownInstanceIds.contains(instanceId)) {
						final var optionalController = main.getControllers().stream()
								.filter(controller -> controller.instanceId() == instanceId).findFirst();

						if (optionalController.isPresent()) {
							final var controller = optionalController.get();

							LOGGER.log(Level.INFO, Main.assembleControllerLoggingMessage(
									"Initiating hot swap to controller ", controller));

							hotSwappingButtonId = HotSwappingButton.None.id;
							EventQueue.invokeLater(() -> {
								main.setSelectedControllerAndUpdateInput(controller, axes);
								main.updateDeviceMenuSelection();
								main.restartLast();
							});

							break;
						}
					}
				} else {
					sdlGamepadToGamepadStateMapIterator.remove();
					updateHotSwappingButtonId();
				}
			}

			lastHotSwapPollTime = currentTime;
		}

		final var gamepadState = sdlGamepadToGamepadStateMap.get(selectedSdlGamepad);
		if (gamepadState == null || !gamepadState.update()) {
			return false;
		}

		final var onScreenKeyboard = main.getOnScreenKeyboard();

		if (clearOnNextPoll) {
			Arrays.fill(buttons, false);

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
				if (delta > 0) {
					newValue = Math.min(newValue, targetValue);
				} else {
					newValue = Math.max(newValue, targetValue);
				}

				setAxis(virtualAxis, newValue, false, (Integer) null);

				return newValue == targetValue;
			}

			return true;
		});

		final var modes = profile.getModes();
		final var activeMode = profile.getActiveMode();
		final var axisToActionMap = activeMode.getAxisToActionsMap();
		final var buttonToActionMap = activeMode.getButtonToActionsMap();

		for (var axis = 0; axis < SDLGamepad.SDL_GAMEPAD_AXIS_COUNT; axis++) {
			final var axisValue = gamepadState.axes[axis];

			if (Math.abs(axisValue) <= ABORT_SUSPENSION_ACTION_DEADZONE) {
				axisToEndSuspensionTimestampMap.remove(axis);
			}

			var actions = axisToActionMap.get(axis);
			if (actions == null) {
				final var buttonToModeActionStack = ButtonToModeAction.getButtonToModeActionStack();
				for (var i = 1; i < buttonToModeActionStack.size(); i++) {
					actions = buttonToModeActionStack.get(i).getMode(this).getAxisToActionsMap().get(axis);

					if (actions != null) {
						break;
					}
				}
			}

			if (actions == null) {
				actions = modes.getFirst().getAxisToActionsMap().get(axis);
			}

			if (actions != null) {
				for (final var action : actions) {
					action.doAction(this, axis, axisValue);
				}
			}
		}

		for (var button = 0; button <= SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT; button++) {
			var actions = buttonToActionMap.get(button);
			if (actions == null) {
				final var buttonToModeActionStack = ButtonToModeAction.getButtonToModeActionStack();
				for (var i = 1; i < buttonToModeActionStack.size(); i++) {
					actions = buttonToModeActionStack.get(i).getMode(this).getButtonToActionsMap().get(button);

					if (actions != null) {
						break;
					}
				}
			}

			if (actions == null) {
				actions = modes.getFirst().getButtonToActionsMap().get(button);
			}

			if (actions != null) {
				for (final var action : actions) {
					action.doAction(this, button, gamepadState.buttons[button]);
				}
			}
		}

		for (;;) {
			for (var button = 0; button <= SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT; button++) {
				final var buttonToModeActions = profile.getButtonToModeActionsMap().get(button);
				if (buttonToModeActions != null) {
					for (final var action : buttonToModeActions) {
						action.doAction(this, button, gamepadState.buttons[button]);
					}
				}
			}

			if (!repeatModeActionWalk) {
				break;
			}
			repeatModeActionWalk = false;
		}

		if (hapticFeedback && scheduledRumbleEffect != null) {
			SDLGamepad.SDL_RumbleGamepad(selectedSdlGamepad, scheduledRumbleEffect.lowFrequencyRumble,
					scheduledRumbleEffect.highFrequencyRumble, scheduledRumbleEffect.duration);
			scheduledRumbleEffect = null;
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
		scheduledRumbleEffect = null;
		lastPollTime = 0;
		rateMultiplier = 0f;
		buttons = null;
		sdlGamepadToGamepadStateMap.clear();
		virtualAxisToTargetValueMap.clear();
		axisToEndSuspensionTimestampMap.clear();
		hotSwappingButtonDownInstanceIds.clear();
		hotSwappingButtonId = HotSwappingButton.None.id;

		resetLastHotSwapPollTime();

		profile.setActiveMode(this, 0);

		IAxisToLongPressAction.reset();
		IButtonToAction.reset();

		profile.getButtonToModeActionsMap().values().forEach(buttonToModeActions -> buttonToModeActions
				.forEach(buttonToModeAction -> buttonToModeAction.reset(this)));

		profile.getModes().forEach(mode -> mode.getAllActions().forEach(action -> {
			if (action instanceof final IResetableAction<?> resetableAction) {
				resetableAction.reset(this);
			}
		}));
	}

	private void resetLastHotSwapPollTime() {
		lastHotSwapPollTime = System.currentTimeMillis() + HOT_SWAP_POLL_INITIAL_SUSPENSION_INTERVAL;
	}

	void scheduleClearOnNextPoll() {
		clearOnNextPoll = true;
	}

	public void setAxis(final VirtualAxis virtualAxis, final float value, final boolean hapticFeedback,
			final Float detentValue) {
		setAxis(virtualAxis, floatToIntAxisValue(value), hapticFeedback,
				detentValue != null ? floatToIntAxisValue(detentValue) : null);
	}

	private void setAxis(final VirtualAxis virtualAxis, int value, final boolean hapticFeedback,
			final Integer detentValue) {
		final var minAxisValue = runMode.getMinAxisValue();
		final var maxAxisValue = runMode.getMaxAxisValue();

		value = Math.max(value, minAxisValue);
		value = Math.min(value, maxAxisValue);

		final var prevValue = axes.put(virtualAxis, value);

		if (hapticFeedback && prevValue != null && prevValue != value) {
			if (value == minAxisValue || value == maxAxisValue) {
				scheduledRumbleEffect = RumbleEffect.Strong;
			} else if (detentValue != null && ((prevValue > detentValue && value <= detentValue)
					|| (prevValue < detentValue && value >= detentValue))) {
				scheduledRumbleEffect = RumbleEffect.Light;
			}
		}
	}

	public void setButton(final int id, final boolean value) {
		if (id < buttons.length) {
			buttons[id] = value;
		} else {
			LOGGER.log(Level.WARNING, "Unable to set value for non-existent button " + id);
		}
	}

	public void setCursorDeltaX(final int cursorDeltaX) {
		this.cursorDeltaX = cursorDeltaX;
	}

	public void setCursorDeltaY(final int cursorDeltaY) {
		this.cursorDeltaY = cursorDeltaY;
	}

	public boolean setProfile(final Profile profile) {
		Objects.requireNonNull(profile, "Parameter profile must not be null");

		for (final var button : profile.getButtonToModeActionsMap().keySet()) {
			if (!isValidButton(button)) {
				return false;
			}
		}

		final var modes = profile.getModes();
		modes.sort((o1, o2) -> {
			final var o1IsDefaultMode = Profile.DEFAULT_MODE.equals(o1);
			final var o2IsDefaultMode = Profile.DEFAULT_MODE.equals(o2);

			if (o1IsDefaultMode && o2IsDefaultMode) {
				return 0;
			}

			if (o1IsDefaultMode) {
				return -1;
			}

			if (o2IsDefaultMode) {
				return 1;
			}

			final var o1IsOnScreenKeyboardMode = OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.equals(o1);
			final var o2IsOnScreenKeyboardMode = OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.equals(o2);

			if (o1IsOnScreenKeyboardMode && o2IsOnScreenKeyboardMode) {
				return 0;
			}

			if (o1IsOnScreenKeyboardMode) {
				return -1;
			}

			if (o2IsOnScreenKeyboardMode) {
				return 1;
			}

			return o1.getDescription().compareTo(o2.getDescription());
		});

		for (final var mode : modes) {
			for (final var axis : mode.getAxisToActionsMap().keySet()) {
				if (axis < 0 || axis >= SDLGamepad.SDL_GAMEPAD_AXIS_COUNT) {
					return false;
				}
			}

			for (final var button : mode.getButtonToActionsMap().keySet()) {
				if (!isValidButton(button)) {
					return false;
				}
			}

			for (final var actions : mode.getButtonToActionsMap().values()) {
				actions.sort((o1, o2) -> {
					if (o1 instanceof final IButtonToAction buttonToAction1
							&& o2 instanceof final IButtonToAction buttonToAction2) {
						final var o1IsLongPress = buttonToAction1.isLongPress();
						final var o2IsLongPress = buttonToAction2.isLongPress();

						if (o1IsLongPress && !o2IsLongPress) {
							return -1;
						}
						if (!o1IsLongPress && o2IsLongPress) {
							return 1;
						}
					}

					return 0;
				});
			}
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

	private void updateHotSwappingButtonId() {
		final var hotSwappingPossible = sdlGamepadToGamepadStateMap.size() > 1;

		if (hotSwappingPossible && hotSwappingButtonId == HotSwappingButton.None.id) {
			hotSwappingButtonId = main.getSelectedHotSwappingButtonId();
		} else if (!hotSwappingPossible && hotSwappingButtonId != HotSwappingButton.None.id) {
			hotSwappingButtonId = HotSwappingButton.None.id;
		}
	}

	private enum RumbleEffect {

		Light((short) 0, Short.MAX_VALUE, 64), Strong(Short.MAX_VALUE, Short.MAX_VALUE, 72);

		private final int duration;

		private final short highFrequencyRumble;

		private final short lowFrequencyRumble;

		@SuppressWarnings("SameParameterValue")
		RumbleEffect(final short lowFrequencyRumble, final short highFrequencyRumble, final int duration) {
			this.lowFrequencyRumble = lowFrequencyRumble;
			this.highFrequencyRumble = highFrequencyRumble;
			this.duration = duration;
		}
	}

	public enum VirtualAxis {
		X, Y, Z, RX, RY, RZ, S0, S1
	}

	private class GamepadState {

		private final float[] axes = new float[SDLGamepad.SDL_GAMEPAD_AXIS_COUNT];

		private final boolean[] buttons = new boolean[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT + 1];

		private final long sdlGamepad;

		private GamepadState(final long sdlGamepad) {
			this.sdlGamepad = sdlGamepad;
		}

		private void mapCircularAxesToSquareAxes(final int xAxisIndex, final int yAxisIndex) {
			final var u = clamp(axes[xAxisIndex]);
			final var v = clamp(axes[yAxisIndex]);

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

			axes[xAxisIndex] = clamp((float) x);
			axes[yAxisIndex] = clamp((float) y);
		}

		private boolean update() {
			if (!SDLGamepad.SDL_GamepadConnected(sdlGamepad)) {
				return false;
			}

			axes[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX
					: SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = normalize(
							SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX),
							Short.MIN_VALUE, Short.MAX_VALUE, -1f, 1f);
			axes[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY
					: SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY] = normalize(
							SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY),
							Short.MIN_VALUE, Short.MAX_VALUE, -1f, 1f);
			axes[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX
					: SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX] = normalize(
							SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX),
							Short.MIN_VALUE, Short.MAX_VALUE, -1f, 1f);
			axes[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY
					: SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY] = normalize(
							SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY),
							Short.MIN_VALUE, Short.MAX_VALUE, -1f, 1f);

			if (mapCircularAxesToSquareAxes) {
				mapCircularAxesToSquareAxes(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY);
				mapCircularAxesToSquareAxes(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY);
			}

			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER] = normalize(
					SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER), 0,
					Short.MAX_VALUE, -1f, 1f);

			axes[SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER] = normalize(
					SDLGamepad.SDL_GetGamepadAxis(sdlGamepad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER), 0,
					Short.MAX_VALUE, -1f, 1f);

			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_EAST] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_EAST);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_WEST] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_WEST);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_BACK] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_BACK);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_START] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_START);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK);

			buttons[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK
					: SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
							SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK);
			buttons[swapLeftAndRightSticks ? SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK
					: SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
							SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK);

			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT);
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT] = SDLGamepad.SDL_GetGamepadButton(sdlGamepad,
					SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT);

			return true;
		}
	}
}

/*
 * Copyright (C) 2014 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.Controller;
import de.bwravencl.controllerbuddy.gui.Main.HotSwappingButton;
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.OverlayAxis.OverlayAxisOrientation;
import de.bwravencl.controllerbuddy.input.OverlayAxis.OverlayAxisStyle;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAxisToDelayableAction;
import de.bwravencl.controllerbuddy.input.action.IButtonToDelayableAction;
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
import java.util.logging.Logger;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDLProperties;

/// Manages gamepad input processing, including axis and button state, virtual
/// axis mapping, hot-swapping between controllers, and haptic feedback
/// scheduling.
///
/// This class serves as the central input handler that polls SDL gamepads,
/// dispatches actions defined in the active [Profile], and maintains the
/// current state of all virtual axes, buttons, keystrokes, and mouse inputs.
public final class Input {

	/// Maximum number of virtual buttons supported.
	public static final int MAX_N_BUTTONS = 128;

	/// Minimum axis movement required to abort a suspension action.
	private static final float ABORT_SUSPENSION_ACTION_DEADZONE = 0.25f;

	/// Maximum delta factor applied to axis movement per poll cycle.
	private static final float AXIS_MOVEMENT_MAX_DELTA_FACTOR = 4f;

	/// Minimum delta factor applied to axis movement per poll cycle.
	private static final float AXIS_MOVEMENT_MIN_DELTA_FACTOR = 0.1f;

	/// Duration in milliseconds to suppress hot-swap polling after a swap.
	private static final long HOT_SWAP_POLL_INITIAL_SUSPENSION_INTERVAL = 2000L;

	/// Interval in milliseconds between hot-swap button polls.
	private static final long HOT_SWAP_POLL_INTERVAL = 50L;

	private static final Logger LOGGER = Logger.getLogger(Input.class.getName());

	/// Duration in milliseconds for which axis suspension is held.
	private static final long SUSPENSION_TIME = 500L;

	/// Current integer values for all virtual axes.
	private final Map<VirtualAxis, Integer> axes;

	/// Map from axis index to the timestamp when its suspension ends.
	private final Map<Integer, Long> axisToEndSuspensionTimestampMap = new HashMap<>();

	/// Keystrokes currently held down continuously.
	private final Set<Keystroke> downKeystrokes = new HashSet<>();

	/// Mouse buttons currently held down continuously.
	private final Set<Integer> downMouseButtons = new HashSet<>();

	/// Keystrokes to be pressed and immediately released this poll cycle.
	private final Set<Keystroke> downUpKeystrokes = new HashSet<>();

	/// Mouse buttons to be pressed and immediately released this poll cycle.
	private final Set<Integer> downUpMouseButtons = new HashSet<>();

	/// Instance IDs of non-selected controllers whose hot-swap button is currently
	/// down.
	private final Set<Integer> hotSwappingButtonDownInstanceIds = new HashSet<>();

	/// The main application instance.
	private final Main main;

	/// Lock keys that should be turned off this poll cycle.
	private final Set<LockKey> offLockKeys = new HashSet<>();

	/// Lock keys that should be turned on this poll cycle.
	private final Set<LockKey> onLockKeys = new HashSet<>();

	/// Map from SDL gamepad handle to its current gamepad state.
	private final Map<Long, GamepadState> sdlGamepadToGamepadStateMap = new HashMap<>();

	/// The controller selected for primary input.
	private final Controller selectedController;

	/// Map from virtual axis to its currently targeted integer value for smooth
	/// movement.
	private final Map<VirtualAxis, Integer> virtualAxisToTargetValueMap = new EnumMap<>(VirtualAxis.class);

	/// Boolean state array for all virtual buttons.
	private boolean[] buttons;

	/// Flag indicating that all button and keystroke state should be reset on the
	/// next poll.
	private boolean clearOnNextPoll;

	/// Horizontal mouse cursor movement delta, written by actions and read by the
	/// output run mode.
	private volatile int cursorDeltaX;

	/// Vertical mouse cursor movement delta, written by actions and read by the
	/// output run mode.
	private volatile int cursorDeltaY;

	/// Whether haptic feedback (rumble) is enabled on the selected controller.
	private boolean hapticFeedback;

	/// The SDL button ID used for hot-swap detection, or
	/// `HotSwappingButton.NONE.id` if disabled.
	private int hotSwappingButtonId = HotSwappingButton.NONE.id;

	/// Whether the input system has been successfully initialized.
	private boolean initialized;

	/// Timestamp of the last hot-swap poll in milliseconds.
	private long lastHotSwapPollTime;

	/// Timestamp of the last input poll in milliseconds.
	private long lastPollTime;

	/// Whether circular axis values should be remapped to square axes.
	private boolean mapCircularAxesToSquareAxes;

	/// The smallest representable axis step, computed from the run mode's axis
	/// range.
	private float minAxisStep;

	/// The currently active input profile.
	private Profile profile;

	/// Multiplier for time-dependent actions, derived from elapsed time since the
	/// last poll.
	private float rateMultiplier;

	/// Whether the mode action walk sequence should repeat continuously.
	private boolean repeatModeActionWalk;

	/// The current run mode that consumes the processed input.
	private RunMode runMode;

	/// A rumble effect scheduled to be played on the next poll cycle, or `null` if
	/// none.
	private RumbleEffect scheduledRumbleEffect;

	/// Number of scroll wheel clicks to emit this poll cycle.
	private volatile int scrollClicks;

	/// SDL gamepad handle for the currently selected controller.
	private long selectedSdlGamepad;

	/// Whether axis initialization should be skipped because axes were provided
	/// externally.
	private boolean skipAxisInitialization;

	/// Whether the left and right sticks should be swapped during input processing.
	private boolean swapLeftAndRightSticks;

	/// Constructs a new Input instance for the given controller.
	///
	/// @param main the main application instance
	/// @param selectedController the controller selected for input
	/// @param axes initial axis values, or `null` to initialize all axes to zero
	public Input(final Main main, final Controller selectedController, final Map<VirtualAxis, Integer> axes) {
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

	/// Clamps a float value to the range [-1, 1].
	///
	/// @param v the value to clamp
	/// @return the clamped value in the range [-1, 1]
	private static float clamp(final float v) {
		return Math.clamp(v, -1f, 1f);
	}

	/// Corrects near-zero numerical imprecision by snapping values smaller than
	/// a small epsilon to exactly zero.
	///
	/// @param d the value to correct
	/// @return `0` if `d` is smaller than the epsilon threshold, otherwise `d`
	private static double correctNumericalImprecision(final double d) {
		if (d < 0.000_000_1) {
			return 0d;
		}
		return d;
	}

	/// Returns whether the given SDL button index is a valid gamepad button.
	///
	/// @param button the SDL button index to check
	/// @return `true` if the button index falls within the valid SDL gamepad button
	/// range
	private static boolean isValidButton(final int button) {
		return button > SDLGamepad.SDL_GAMEPAD_BUTTON_INVALID && button <= SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
	}

	/// Normalizes a value from one range to another.
	///
	/// @param value the value to normalize
	/// @param inMin the minimum of the input range
	/// @param inMax the maximum of the input range
	/// @param outMin the minimum of the output range
	/// @param outMax the maximum of the output range
	/// @return the normalized value mapped to the output range
	public static float normalize(final float value, final float inMin, final float inMax, final float outMin,
			final float outMax) {
		final var oldRange = inMax - inMin;
		if (oldRange == 0f) {
			return outMin;
		}

		final var newRange = outMax - outMin;

		return (value - inMin) * newRange / oldRange + outMin;
	}

	/// Releases all SDL gamepad resources and clears player LED indices.
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

	/// Converts a floating-point axis value in the range `[-1, 1]` to an integer
	/// axis value
	/// scaled to the current run mode's axis range.
	///
	/// @param value the float axis value, clamped to `[-1, 1]`
	/// @return the corresponding integer axis value
	public int floatToIntAxisValue(float value) {
		value = Math.max(value, -1f);
		value = Math.min(value, 1f);

		final var minAxisValue = runMode.getMinAxisValue();
		final var maxAxisValue = runMode.getMaxAxisValue();

		return (int) normalize(value, -1f, 1f, minAxisValue, maxAxisValue);
	}

	/// Returns the map of virtual axes to their current integer values.
	///
	/// @return the virtual axis value map
	public Map<VirtualAxis, Integer> getAxes() {
		return axes;
	}

	/// Returns the current button state array.
	///
	/// @return the boolean array of button states
	public boolean[] getButtons() {
		return buttons;
	}

	/// Returns the horizontal cursor movement delta.
	///
	/// @return the cursor delta along the X axis
	public int getCursorDeltaX() {
		return cursorDeltaX;
	}

	/// Returns the vertical cursor movement delta.
	///
	/// @return the cursor delta along the Y axis
	public int getCursorDeltaY() {
		return cursorDeltaY;
	}

	/// Returns the set of keystrokes currently held down.
	///
	/// @return the set of currently pressed keystrokes
	public Set<Keystroke> getDownKeystrokes() {
		return downKeystrokes;
	}

	/// Returns the set of mouse buttons currently held down.
	///
	/// @return the set of currently pressed mouse button identifiers
	public Set<Integer> getDownMouseButtons() {
		return downMouseButtons;
	}

	/// Returns the set of keystrokes to be pressed and immediately released.
	///
	/// @return the set of down-up keystrokes
	public Set<Keystroke> getDownUpKeystrokes() {
		return downUpKeystrokes;
	}

	/// Returns the set of mouse buttons to be pressed and immediately released.
	///
	/// @return the set of down-up mouse button identifiers
	public Set<Integer> getDownUpMouseButtons() {
		return downUpMouseButtons;
	}

	/// Returns the main application instance.
	///
	/// @return the [Main] instance
	public Main getMain() {
		return main;
	}

	/// Returns the smallest representable axis movement step size based on the run
	/// mode's axis range.
	///
	/// @return the minimum axis step
	public float getMinAxisStep() {
		return minAxisStep;
	}

	/// Returns the set of lock keys to be turned off.
	///
	/// @return the set of lock keys to deactivate
	public Set<LockKey> getOffLockKeys() {
		return offLockKeys;
	}

	/// Returns the set of lock keys to be turned on.
	///
	/// @return the set of lock keys to activate
	public Set<LockKey> getOnLockKeys() {
		return onLockKeys;
	}

	/// Returns the current input profile.
	///
	/// @return the active [Profile]
	public Profile getProfile() {
		return profile;
	}

	/// Returns the rate multiplier based on elapsed time since the last poll.
	/// Used to scale time-dependent actions for frame-rate independence.
	///
	/// @return the rate multiplier
	public float getRateMultiplier() {
		return rateMultiplier;
	}

	/// Returns the current run mode.
	///
	/// @return the active [RunMode]
	public RunMode getRunMode() {
		return runMode;
	}

	/// Returns the number of pending scroll wheel clicks.
	///
	/// @return the scroll click count
	public int getScrollClicks() {
		return scrollClicks;
	}

	/// Returns the selected controller.
	///
	/// @return the [Controller] currently selected for input
	public Controller getSelectedController() {
		return selectedController;
	}

	/// Initializes the input system by opening all controllers, computing the
	/// minimum axis step, and invoking initialization actions defined in the
	/// profile.
	///
	/// @return `true` if initialization succeeded, `false` if the selected
	/// controller could not be opened
	/// @throws IllegalStateException if already initialized
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

		minAxisStep = 2f / (runMode.getMaxAxisValue() - runMode.getMinAxisValue());

		profile.getModes().forEach(mode -> mode.getAllActions().forEach(action -> {
			if (action instanceof final IInitializationAction<?> initializationAction) {
				initializationAction.init(this);
			}
		}));

		initialized = true;

		return true;
	}

	/// Initializes the button state array based on the current run mode's button
	/// count, capped at [#MAX_N_BUTTONS].
	public void initButtons() {
		buttons = new boolean[Math.min(runMode.getNumButtons(), MAX_N_BUTTONS)];
	}

	/// Checks whether the specified axis is currently suspended.
	///
	/// @param axis the axis index to check
	/// @return `true` if the axis is suspended
	public boolean isAxisSuspended(final int axis) {
		return axisToEndSuspensionTimestampMap.containsKey(axis);
	}

	/// Returns whether this input instance has been initialized.
	///
	/// @return `true` if [#init()] has completed successfully
	public boolean isInitialized() {
		return initialized;
	}

	/// Returns whether axis initialization should be skipped.
	///
	/// @return `true` if axis initialization should be skipped
	public boolean isSkipAxisInitialization() {
		return skipAxisInitialization;
	}

	/// Moves a virtual axis towards the specified target value.
	///
	/// @param virtualAxis the virtual axis to move
	/// @param targetValue the target value to move towards
	public void moveAxis(final VirtualAxis virtualAxis, final float targetValue) {
		final var integerTargetValue = floatToIntAxisValue(targetValue);

		if (axes.get(virtualAxis) != integerTargetValue) {
			virtualAxisToTargetValueMap.put(virtualAxis, integerTargetValue);
		}
	}

	/// Opens the specified controller and registers its gamepad state.
	///
	/// @param controller the controller to open
	/// @return `true` if the controller was opened successfully
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

	/// Polls all connected gamepads and processes their input actions.
	///
	/// @return `true` if the poll completed successfully
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

		if (hotSwappingButtonId != HotSwappingButton.NONE.id
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

							LOGGER.info(Main.assembleControllerLoggingMessage("Initiating hot swap to controller ",
									controller));

							hotSwappingButtonId = HotSwappingButton.NONE.id;
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

			downKeystrokes.clear();
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

				setAxis(virtualAxis, newValue, false, (Integer) null, null, null);

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

	/// Requests a repeated walk of mode actions on the next poll cycle.
	public void repeatModeActionWalk() {
		repeatModeActionWalk = true;
	}

	/// Resets the input state to its initial configuration.
	public void reset() {
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
		hotSwappingButtonId = HotSwappingButton.NONE.id;

		resetLastHotSwapPollTime();

		profile.setActiveMode(this, 0);
		clearOnNextPoll = false;

		IAxisToDelayableAction.reset();
		IButtonToDelayableAction.reset();

		profile.getButtonToModeActionsMap().values().forEach(buttonToModeActions -> buttonToModeActions
				.forEach(buttonToModeAction -> buttonToModeAction.reset(this)));

		profile.getModes().forEach(mode -> mode.getAllActions().forEach(action -> {
			if (action instanceof final IResetableAction<?> resetableAction) {
				resetableAction.reset(this);
			}
		}));
	}

	/// Resets the hot-swap poll timer by advancing it by the initial suspension
	/// interval, suppressing hot-swap checks immediately after a swap.
	private void resetLastHotSwapPollTime() {
		lastHotSwapPollTime = System.currentTimeMillis() + HOT_SWAP_POLL_INITIAL_SUSPENSION_INTERVAL;
	}

	/// Schedules a full state clear to be performed at the start of the next
	/// poll cycle, resetting all virtual outputs to their default values.
	void scheduleClearOnNextPoll() {
		clearOnNextPoll = true;
	}

	/// Sets a virtual axis to the specified value with optional haptic feedback.
	///
	/// @param virtualAxis the virtual axis to set
	/// @param value the value to set
	/// @param hapticFeedback whether to trigger haptic feedback on value changes
	/// @param minValue the minimum axis value, or `null` if unbounded
	/// @param maxValue the maximum axis value, or `null` if unbounded
	/// @param detentValue the detent value for haptic feedback, or `null` if none
	public void setAxis(final VirtualAxis virtualAxis, final float value, final boolean hapticFeedback,
			final Float minValue, final Float maxValue, final Float detentValue) {
		setAxis(virtualAxis, floatToIntAxisValue(value), hapticFeedback,
				minValue != null ? floatToIntAxisValue(minValue) : null,
				maxValue != null ? floatToIntAxisValue(maxValue) : null,
				detentValue != null ? floatToIntAxisValue(detentValue) : null);
	}

	/// Sets a virtual axis to the specified raw integer value, clamping it to the
	/// run mode's axis range and optionally scheduling a rumble effect when the
	/// value crosses a boundary or detent.
	///
	/// @param virtualAxis the virtual axis to set
	/// @param value the raw integer axis value to set
	/// @param hapticFeedback whether to trigger haptic feedback on value changes
	/// @param minValue the minimum axis value for boundary rumble, or `null` if
	/// none
	/// @param maxValue the maximum axis value for boundary rumble, or `null` if
	/// none
	/// @param detentValue the detent value for light rumble feedback, or `null` if
	/// none
	private void setAxis(final VirtualAxis virtualAxis, int value, final boolean hapticFeedback, final Integer minValue,
			final Integer maxValue, final Integer detentValue) {
		value = Math.clamp(value, runMode.getMinAxisValue(), runMode.getMaxAxisValue());

		final var prevValue = axes.put(virtualAxis, value);

		if (hapticFeedback && prevValue != null && prevValue != value) {
			if (value == minValue || value == maxValue) {
				scheduledRumbleEffect = RumbleEffect.STRONG;
			} else if (detentValue != null && ((prevValue > detentValue && value <= detentValue)
					|| (prevValue < detentValue && value >= detentValue))) {
				scheduledRumbleEffect = RumbleEffect.LIGHT;
			}
		}
	}

	/// Sets the horizontal cursor movement delta.
	///
	/// @param cursorDeltaX the horizontal cursor delta in pixels
	public void setCursorDeltaX(final int cursorDeltaX) {
		this.cursorDeltaX = cursorDeltaX;
	}

	/// Sets the vertical cursor movement delta.
	///
	/// @param cursorDeltaY the vertical cursor delta in pixels
	public void setCursorDeltaY(final int cursorDeltaY) {
		this.cursorDeltaY = cursorDeltaY;
	}

	/// Sets the active input profile, validating its button mappings.
	///
	/// @param profile the profile to activate
	/// @return `true` if the profile was set successfully
	public boolean setProfile(final Profile profile) {
		Objects.requireNonNull(profile, "Parameter profile must not be null");

		for (final var button : profile.getButtonToModeActionsMap().keySet()) {
			if (!isValidButton(button)) {
				return false;
			}
		}

		final var modes = profile.getModes();
		modes.sort((mode1, mode2) -> {
			final var mode1IsDefaultMode = Profile.DEFAULT_MODE.equals(mode1);
			final var mode2IsDefaultMode = Profile.DEFAULT_MODE.equals(mode2);

			if (mode1IsDefaultMode && mode2IsDefaultMode) {
				return 0;
			}

			if (mode1IsDefaultMode) {
				return -1;
			}

			if (mode2IsDefaultMode) {
				return 1;
			}

			final var mode1IsOnScreenKeyboardMode = OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.equals(mode1);
			final var mode2IsOnScreenKeyboardMode = OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.equals(mode2);

			if (mode1IsOnScreenKeyboardMode && mode2IsOnScreenKeyboardMode) {
				return 0;
			}

			if (mode1IsOnScreenKeyboardMode) {
				return -1;
			}

			if (mode2IsOnScreenKeyboardMode) {
				return 1;
			}

			return mode1.getDescription().compareTo(mode2.getDescription());
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
				actions.sort((action1, action2) -> {
					if (action1 instanceof final IButtonToDelayableAction buttonToDelayableAction1
							&& action2 instanceof final IButtonToDelayableAction buttonToDelayableAction2) {
						final var mode1IsDelayed = buttonToDelayableAction1.isDelayed();
						final var mode2IsDelayed = buttonToDelayableAction2.isDelayed();

						if (mode1IsDelayed && !mode2IsDelayed) {
							return -1;
						}
						if (!mode1IsDelayed && mode2IsDelayed) {
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

	/// Sets the current run mode.
	///
	/// @param runMode the run mode to set
	public void setRunMode(final RunMode runMode) {
		this.runMode = runMode;
	}

	/// Sets the number of scroll wheel clicks to emit.
	///
	/// @param scrollClicks the number of scroll clicks
	public void setScrollClicks(final int scrollClicks) {
		this.scrollClicks = scrollClicks;
	}

	/// Temporarily suspends processing of the specified axis.
	///
	/// @param axis the axis index to suspend
	public void suspendAxis(final int axis) {
		axisToEndSuspensionTimestampMap.put(axis, System.currentTimeMillis() + SUSPENSION_TIME);
	}

	/// Updates the active hot-swapping button ID based on whether multiple
	/// gamepads are currently connected, enabling or disabling hot-swap support
	/// accordingly.
	private void updateHotSwappingButtonId() {
		final var hotSwappingPossible = sdlGamepadToGamepadStateMap.size() > 1;

		if (hotSwappingPossible && hotSwappingButtonId == HotSwappingButton.NONE.id) {
			hotSwappingButtonId = main.getSelectedHotSwappingButtonId();
		} else if (!hotSwappingPossible && hotSwappingButtonId != HotSwappingButton.NONE.id) {
			hotSwappingButtonId = HotSwappingButton.NONE.id;
		}
	}

	/// Defines the preset rumble intensities and durations used for haptic
	/// feedback.
	///
	/// Each constant encodes a distinct combination of low-frequency and
	/// high-frequency motor strengths together with a duration in milliseconds,
	/// covering a light notification pulse and a strong impact effect.
	private enum RumbleEffect {

		/// Light notification pulse using only the high-frequency motor.
		LIGHT((short) 0, Short.MAX_VALUE, 64),
		/// Strong impact effect using both motors at maximum intensity.
		STRONG(Short.MAX_VALUE, Short.MAX_VALUE, 72);

		/// Duration of the rumble effect in milliseconds.
		private final int duration;

		/// High-frequency (light) motor intensity.
		private final short highFrequencyRumble;

		/// Low-frequency (heavy) motor intensity.
		private final short lowFrequencyRumble;

		/// Creates a rumble effect with the specified motor intensities and duration.
		///
		/// @param lowFrequencyRumble the low-frequency (heavy) motor intensity
		/// @param highFrequencyRumble the high-frequency (light) motor intensity
		/// @param duration the duration of the rumble effect in milliseconds
		@SuppressWarnings("SameParameterValue")
		RumbleEffect(final short lowFrequencyRumble, final short highFrequencyRumble, final int duration) {
			this.lowFrequencyRumble = lowFrequencyRumble;
			this.highFrequencyRumble = highFrequencyRumble;
			this.duration = duration;
		}
	}

	/// Represents the virtual axes available for input mapping.
	///
	/// Each constant corresponds to one axis channel on the virtual device and
	/// carries default overlay orientation and rendering style metadata used
	/// when displaying the axis value in the on-screen overlay.
	public enum VirtualAxis {

		/// X axis (left stick horizontal).
		X(OverlayAxisOrientation.HORIZONTAL, OverlayAxisStyle.LINE),
		/// Y axis (left stick vertical).
		Y(OverlayAxisOrientation.VERTICAL, OverlayAxisStyle.SOLID),
		/// Z axis (additional axis).
		Z(OverlayAxisOrientation.VERTICAL, OverlayAxisStyle.SOLID),
		/// RX axis (right stick horizontal).
		RX(OverlayAxisOrientation.HORIZONTAL, OverlayAxisStyle.LINE),
		/// RY axis (right stick vertical).
		RY(OverlayAxisOrientation.VERTICAL, OverlayAxisStyle.SOLID),
		/// RZ axis (additional rotational axis).
		RZ(OverlayAxisOrientation.HORIZONTAL, OverlayAxisStyle.LINE),
		/// Slider 0 axis.
		S0(OverlayAxisOrientation.VERTICAL, OverlayAxisStyle.SOLID),
		/// Slider 1 axis.
		S1(OverlayAxisOrientation.VERTICAL, OverlayAxisStyle.SOLID);

		/// The default overlay orientation for this virtual axis.
		private final OverlayAxisOrientation defaultOverlayAxisOrientation;

		/// The default overlay rendering style for this virtual axis.
		private final OverlayAxisStyle defaultOverlayAxisStyle;

		/// Creates a virtual axis constant with the given default overlay orientation
		/// and rendering style.
		///
		/// @param defaultOverlayAxisOrientation the default overlay orientation for
		/// this axis
		/// @param defaultOverlayAxisStyle the default overlay rendering style for this
		/// axis
		VirtualAxis(final OverlayAxisOrientation defaultOverlayAxisOrientation,
				final OverlayAxisStyle defaultOverlayAxisStyle) {
			this.defaultOverlayAxisOrientation = defaultOverlayAxisOrientation;
			this.defaultOverlayAxisStyle = defaultOverlayAxisStyle;
		}

		/// Returns the default overlay orientation for this virtual axis.
		///
		/// @return the default overlay axis orientation
		public OverlayAxisOrientation getDefaultOrientation() {
			return defaultOverlayAxisOrientation;
		}

		/// Returns the default overlay rendering style for this virtual axis.
		///
		/// @return the default overlay axis style
		public OverlayAxisStyle getDefaultStyle() {
			return defaultOverlayAxisStyle;
		}
	}

	/// Holds the most recently polled axis and button state for a single SDL
	/// gamepad.
	///
	/// Wraps a native SDL gamepad handle and provides an [#update()] method that
	/// reads all axis and button values via SDL, applies optional transformations
	/// such as stick-swap and circular-to-square axis remapping, and stores the
	/// normalized results for use by the input pipeline on each polling cycle.
	private final class GamepadState {

		/// Normalized axis values for all SDL gamepad axes.
		private final float[] axes = new float[SDLGamepad.SDL_GAMEPAD_AXIS_COUNT];

		/// Button pressed states for all SDL gamepad buttons.
		private final boolean[] buttons = new boolean[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT + 1];

		/// The SDL gamepad handle associated with this state.
		private final long sdlGamepad;

		/// Creates a gamepad state for the given SDL gamepad handle.
		///
		/// @param sdlGamepad the native SDL gamepad handle
		private GamepadState(final long sdlGamepad) {
			this.sdlGamepad = sdlGamepad;
		}

		/// Remaps the circular range of a pair of axes to a square range using disc to
		/// square mapping as described in ["Analytical Methods for Squaring the Disc"
		/// by Chamberlain Fong](https://arxiv.org/abs/1509.06344), updating the axis
		/// values in place.
		///
		/// @param xAxisIndex the index of the horizontal axis in the axes array
		/// @param yAxisIndex the index of the vertical axis in the axes array
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

		/// Polls the SDL gamepad and refreshes all axis and button state arrays.
		///
		/// Reads normalized axis values for sticks and triggers, optionally swapping
		/// left and right sticks and remapping circular to square axes, then reads
		/// all button states.
		///
		/// @return `true` if the gamepad is still connected and state was updated,
		/// `false` if the gamepad has been disconnected
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

/*
 * Copyright (C) 2026 Matteo Hausner
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
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard.Direction;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.action.AxisToAxisAction;
import de.bwravencl.controllerbuddy.input.action.AxisToButtonAction;
import de.bwravencl.controllerbuddy.input.action.AxisToCursorAction;
import de.bwravencl.controllerbuddy.input.action.AxisToKeyAction;
import de.bwravencl.controllerbuddy.input.action.AxisToMouseButtonAction;
import de.bwravencl.controllerbuddy.input.action.AxisToRelativeAxisAction;
import de.bwravencl.controllerbuddy.input.action.AxisToScrollAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToAxisResetAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToButtonAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToCursorAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToKeyAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToLockKeyAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToMouseButtonAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToPressOnScreenKeyboardKeyAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToReleaseAllOnScreenKeyboardKeysAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToScrollAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToSelectOnScreenKeyboardKeyAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import de.bwravencl.controllerbuddy.input.action.NullAction;
import de.bwravencl.controllerbuddy.input.action.ToCursorAction.MouseAxis;
import de.bwravencl.controllerbuddy.runmode.RunMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.lwjgl.sdl.SDLGamepad;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@TestClassOrder(ClassOrderer.Random.class)
@SuppressWarnings("SameParameterValue")
final class InputPipelineTest {

	private GamepadStateInjector injector;

	private Input input;

	private OnScreenKeyboard mockOnScreenKeyboard;

	private MockedStatic<SDLGamepad> sdlGamepadMock;

	private static void assertAxisEquals(final int expected, final int actual) {
		final var tolerance = 1;
		if (Math.abs(expected - actual) > tolerance) {
			Assertions.assertEquals(expected, actual);
		}
	}

	private static AxisToAxisAction newAxisToAxisAction(final VirtualAxis virtualAxis) {
		final var action = new AxisToAxisAction();
		action.setVirtualAxis(virtualAxis);
		return action;
	}

	private static AxisToButtonAction newAxisToButtonAction(final int buttonId, final float minAxisValue,
			final float maxAxisValue) {
		final var action = new AxisToButtonAction();
		action.setButtonId(buttonId);
		action.setMinAxisValue(minAxisValue);
		action.setMaxAxisValue(maxAxisValue);
		return action;
	}

	private static AxisToCursorAction newAxisToCursorAction(final MouseAxis mouseAxis, final int sensitivity,
			final float deadZone) {
		final var action = new AxisToCursorAction();
		action.setAxis(mouseAxis);
		action.setCursorSensitivity(sensitivity);
		action.setDeadZone(deadZone);
		return action;
	}

	private static AxisToKeyAction newAxisToKeyAction(final float minAxisValue, final float maxAxisValue,
			final Scancode... keyCodes) {
		final var action = new AxisToKeyAction();
		action.setMinAxisValue(minAxisValue);
		action.setMaxAxisValue(maxAxisValue);
		action.setKeystroke(new Keystroke(keyCodes, new Scancode[0]));
		return action;
	}

	private static AxisToMouseButtonAction newAxisToMouseButtonAction(final int mouseButton, final float minAxisValue,
			final float maxAxisValue) {
		final var action = new AxisToMouseButtonAction();
		action.setMouseButton(mouseButton);
		action.setMinAxisValue(minAxisValue);
		action.setMaxAxisValue(maxAxisValue);
		return action;
	}

	private static AxisToRelativeAxisAction newAxisToRelativeAxisAction(final VirtualAxis virtualAxis,
			final float maxRelativeSpeed, final float deadZone) {
		final var action = new AxisToRelativeAxisAction();
		action.setVirtualAxis(virtualAxis);
		action.setMaxRelativeSpeed(maxRelativeSpeed);
		action.setDeadZone(deadZone);
		return action;
	}

	private static AxisToScrollAction newAxisToScrollAction(final int clicks, final float deadZone) {
		final var action = new AxisToScrollAction();
		action.setClicks(clicks);
		action.setDeadZone(deadZone);
		return action;
	}

	private static ButtonToAxisResetAction newButtonToAxisResetAction(final VirtualAxis virtualAxis,
			final float resetValue, final Activation activation, final boolean fluid) {
		final var action = new ButtonToAxisResetAction();
		action.setVirtualAxis(virtualAxis);
		action.setResetValue(resetValue);
		action.setActivation(activation);
		action.setFluid(fluid);
		return action;
	}

	private static ButtonToButtonAction newButtonToButtonAction(final int buttonId) {
		final var action = new ButtonToButtonAction();
		action.setButtonId(buttonId);
		return action;
	}

	private static ButtonToCursorAction newButtonToCursorAction(final MouseAxis mouseAxis, final int sensitivity) {
		final var action = new ButtonToCursorAction();
		action.setAxis(mouseAxis);
		action.setCursorSensitivity(sensitivity);
		return action;
	}

	private static ButtonToCycleAction newButtonToCycleAction(final Activation activation,
			final List<IAction<Boolean>> subActions) {
		final var action = new ButtonToCycleAction();
		action.setActivation(activation);
		action.setActions(new ArrayList<>(subActions));
		return action;
	}

	private static ButtonToKeyAction newButtonToKeyAction(final Scancode... keyCodes) {
		final var action = new ButtonToKeyAction();
		action.setKeystroke(new Keystroke(keyCodes, new Scancode[0]));
		return action;
	}

	private static ButtonToKeyAction newButtonToKeyActionWithModifiers(final Scancode[] keyCodes,
			final Scancode[] modifierCodes) {
		final var action = new ButtonToKeyAction();
		action.setKeystroke(new Keystroke(keyCodes, modifierCodes));
		return action;
	}

	private static ButtonToLockKeyAction newButtonToLockKeyAction(final LockKey lockKey, final boolean on) {
		final var action = new ButtonToLockKeyAction();
		action.setLockKey(lockKey);
		action.setOn(on);
		return action;
	}

	private static ButtonToModeAction newButtonToModeAction(final Mode mode, final boolean toggle) {
		final var action = new ButtonToModeAction();
		action.setMode(mode);
		action.setToggle(toggle);
		return action;
	}

	private static ButtonToMouseButtonAction newButtonToMouseButtonAction(final int mouseButton) {
		final var action = new ButtonToMouseButtonAction();
		action.setMouseButton(mouseButton);
		return action;
	}

	private static ButtonToPressOnScreenKeyboardKeyAction newButtonToPressOnScreenKeyboardKeyAction(
			final boolean lockKey) {
		final var action = new ButtonToPressOnScreenKeyboardKeyAction();
		action.setLockKey(lockKey);
		return action;
	}

	private static ButtonToReleaseAllOnScreenKeyboardKeysAction newButtonToReleaseAllOnScreenKeyboardKeysAction() {
		return new ButtonToReleaseAllOnScreenKeyboardKeysAction();
	}

	private static ButtonToScrollAction newButtonToScrollAction(final int clicks) {
		final var action = new ButtonToScrollAction();
		action.setClicks(clicks);
		return action;
	}

	private static ButtonToSelectOnScreenKeyboardKeyAction newButtonToSelectOnScreenKeyboardKeyAction(
			final Direction direction) {
		final var action = new ButtonToSelectOnScreenKeyboardKeyAction();
		action.setDirection(direction);
		return action;
	}

	private static float[] noAxes() {
		return new float[SDLGamepad.SDL_GAMEPAD_AXIS_COUNT];
	}

	private static boolean[] noButtons() {
		return new boolean[SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT + 1];
	}

	private static Scancode scancode(final String name) {
		return Scancode.NAME_TO_SCAN_CODE_MAP.get(name);
	}

	private OutputCapture pollWithFrame(final float[] axes, final boolean[] buttons) {
		injector.injectFrame(axes, buttons);
		input.poll();
		return OutputCapture.captureAndReset(input);
	}

	private void setProfile(final Profile profile) {
		input.setProfile(profile);
	}

	@BeforeEach
	void setUp() {
		Profile.DEFAULT_MODE.getAxisToActionsMap().clear();
		Profile.DEFAULT_MODE.getButtonToActionsMap().clear();
		final var mockMain = Mockito.mock(Main.class);
		final var mockController = Mockito.mock(Controller.class);
		mockOnScreenKeyboard = Mockito.mock(OnScreenKeyboard.class);
		final var mockRunMode = Mockito.mock(RunMode.class);

		Mockito.lenient().when(mockMain.getOnScreenKeyboard()).thenReturn(mockOnScreenKeyboard);
		Mockito.lenient().when(mockMain.getPollInterval()).thenReturn(RunMode.DEFAULT_POLL_INTERVAL);
		Mockito.lenient().when(mockMain.isSwapLeftAndRightSticks()).thenReturn(false);
		Mockito.lenient().when(mockMain.isMapCircularAxesToSquareAxes()).thenReturn(false);
		Mockito.lenient().when(mockMain.isHapticFeedback()).thenReturn(false);
		Mockito.lenient().when(mockMain.getControllers()).thenReturn(Set.of(mockController));
		Mockito.lenient().when(mockRunMode.getMinAxisValue()).thenReturn((int) Short.MIN_VALUE);
		Mockito.lenient().when(mockRunMode.getMaxAxisValue()).thenReturn((int) Short.MAX_VALUE);
		Mockito.lenient().when(mockRunMode.getNumButtons()).thenReturn(128);
		Mockito.lenient().when(mockRunMode.getPollInterval()).thenReturn(RunMode.DEFAULT_POLL_INTERVAL);

		input = new Input(mockMain, mockController, null);
		input.setRunMode(mockRunMode);
		input.initButtons();

		sdlGamepadMock = Mockito.mockStatic(SDLGamepad.class);

		injector = new GamepadStateInjector(input, sdlGamepadMock);

		sdlGamepadMock.when(() -> SDLGamepad.SDL_GamepadConnected(injector.getDummySdlGamepadHandle()))
				.thenReturn(true);
	}

	private void setUpAxisProfile(final int sdlAxis, final VirtualAxis virtualAxis) {
		final var profile = new Profile();
		final var defaultMode = profile.getModes().getFirst();
		defaultMode.getAxisToActionsMap().put(sdlAxis, new ArrayList<>(List.of(newAxisToAxisAction(virtualAxis))));
		setProfile(profile);
	}

	private void setUpButtonProfile(final int sdlButton, final int virtualButton) {
		final var profile = new Profile();
		final var defaultMode = profile.getModes().getFirst();
		defaultMode.getButtonToActionsMap().put(sdlButton,
				new ArrayList<>(List.of(newButtonToButtonAction(virtualButton))));
		setProfile(profile);
	}

	private void setUpButtonToKeyProfile(final int sdlButton, final Scancode... keyCodes) {
		final var profile = new Profile();
		final var defaultMode = profile.getModes().getFirst();
		defaultMode.getButtonToActionsMap().put(sdlButton, new ArrayList<>(List.of(newButtonToKeyAction(keyCodes))));
		setProfile(profile);
	}

	private void setUpButtonToMouseButtonProfile(final int sdlButton, final int mouseButton) {
		final var profile = new Profile();
		final var defaultMode = profile.getModes().getFirst();
		defaultMode.getButtonToActionsMap().put(sdlButton,
				new ArrayList<>(List.of(newButtonToMouseButtonAction(mouseButton))));
		setProfile(profile);
	}

	private Profile setUpMultiModeProfile(final int modeButtonSdlButton, final boolean toggle, final int sdlAxis,
			final VirtualAxis mode0Axis, final VirtualAxis mode1Axis) {
		final var profile = new Profile();
		final var defaultMode = profile.getModes().getFirst();
		defaultMode.getAxisToActionsMap().put(sdlAxis, List.of(newAxisToAxisAction(mode0Axis)));

		final var mode1 = new Mode();
		mode1.setDescription("Mode 1");
		mode1.getAxisToActionsMap().put(sdlAxis, List.of(newAxisToAxisAction(mode1Axis)));
		profile.getModes().add(mode1);

		final var buttonToModeActions = new ArrayList<ButtonToModeAction>();
		buttonToModeActions.add(newButtonToModeAction(mode1, toggle));
		profile.getButtonToModeActionsMap().put(modeButtonSdlButton, buttonToModeActions);

		setProfile(profile);
		return profile;
	}

	@AfterEach
	void tearDown() {
		if (input != null) {
			input.reset();
		}

		if (sdlGamepadMock != null) {
			sdlGamepadMock.close();
		}
	}

	private static final class GamepadStateInjector {

		private static final long DUMMY_SDL_GAMEPAD_HANDLE = 1L;

		private final MockedStatic<SDLGamepad> sdlGamepadMock;

		private GamepadStateInjector(final Input input, final MockedStatic<SDLGamepad> sdlGamepadMock) {
			this.sdlGamepadMock = sdlGamepadMock;

			sdlGamepadMock.when(() -> SDLGamepad.SDL_OpenGamepad(Mockito.anyInt()))
					.thenReturn(DUMMY_SDL_GAMEPAD_HANDLE);

			input.init();
		}

		private static short floatToShortAxis(final float value, final boolean isTrigger) {
			if (isTrigger) {
				return (short) Math.round(Input.normalize(value, -1f, 1f, 0, Short.MAX_VALUE));
			}
			return (short) Math.round(Input.normalize(value, -1f, 1f, Short.MIN_VALUE, Short.MAX_VALUE));
		}

		private long getDummySdlGamepadHandle() {
			return DUMMY_SDL_GAMEPAD_HANDLE;
		}

		private void injectFrame(final float[] axes, final boolean[] buttons) {
			for (var i = 0; i < SDLGamepad.SDL_GAMEPAD_AXIS_COUNT; i++) {
				final var axis = i;
				final var isTrigger = axis == SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER
						|| axis == SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER;
				final var shortValue = floatToShortAxis(axes[axis], isTrigger);
				sdlGamepadMock.when(() -> SDLGamepad.SDL_GetGamepadAxis(DUMMY_SDL_GAMEPAD_HANDLE, axis))
						.thenReturn(shortValue);
			}

			final var buttonIds = new int[] { SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, SDLGamepad.SDL_GAMEPAD_BUTTON_EAST,
					SDLGamepad.SDL_GAMEPAD_BUTTON_WEST, SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH,
					SDLGamepad.SDL_GAMEPAD_BUTTON_BACK, SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE,
					SDLGamepad.SDL_GAMEPAD_BUTTON_START, SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK,
					SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK, SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER,
					SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP,
					SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT,
					SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT };

			for (final var buttonId : buttonIds) {
				final var pressed = buttonId < buttons.length && buttons[buttonId];
				sdlGamepadMock.when(() -> SDLGamepad.SDL_GetGamepadButton(DUMMY_SDL_GAMEPAD_HANDLE, buttonId))
						.thenReturn(pressed);
			}
		}
	}

	@SuppressWarnings("ClassCanBeRecord")
	private static final class OutputCapture {

		private final Map<VirtualAxis, Integer> axes;

		private final boolean[] buttons;

		private final int cursorDeltaX;

		private final int cursorDeltaY;

		private final Set<Keystroke> downKeystrokes;

		private final Set<Integer> downMouseButtons;

		private final Set<Keystroke> downUpKeystrokes;

		private final Set<Integer> downUpMouseButtons;

		private final Set<LockKey> offLockKeys;

		private final Set<LockKey> onLockKeys;

		private final int scrollClicks;

		private OutputCapture(final Map<VirtualAxis, Integer> axes, final boolean[] buttons,
				final Set<Keystroke> downKeystrokes, final Set<Keystroke> downUpKeystrokes,
				final Set<Integer> downMouseButtons, final Set<Integer> downUpMouseButtons, final int cursorDeltaX,
				final int cursorDeltaY, final int scrollClicks, final Set<LockKey> onLockKeys,
				final Set<LockKey> offLockKeys) {
			this.axes = axes;
			this.buttons = buttons;
			this.downKeystrokes = downKeystrokes;
			this.downUpKeystrokes = downUpKeystrokes;
			this.downMouseButtons = downMouseButtons;
			this.downUpMouseButtons = downUpMouseButtons;
			this.cursorDeltaX = cursorDeltaX;
			this.cursorDeltaY = cursorDeltaY;
			this.scrollClicks = scrollClicks;
			this.onLockKeys = onLockKeys;
			this.offLockKeys = offLockKeys;
		}

		private static OutputCapture captureAndReset(final Input input) {
			final var axes = new EnumMap<>(input.getAxes());
			final var buttons = Arrays.copyOf(input.getButtons(), input.getButtons().length);
			final var downKeystrokes = Set.copyOf(input.getDownKeystrokes());
			final var downUpKeystrokes = Set.copyOf(input.getDownUpKeystrokes());
			final var downMouseButtons = Set.copyOf(input.getDownMouseButtons());
			final var downUpMouseButtons = Set.copyOf(input.getDownUpMouseButtons());
			final var cursorDeltaX = input.getCursorDeltaX();
			final var cursorDeltaY = input.getCursorDeltaY();
			final var scrollClicks = input.getScrollClicks();
			final var onLockKeys = Set.copyOf(input.getOnLockKeys());
			final var offLockKeys = Set.copyOf(input.getOffLockKeys());

			final var inputButtons = input.getButtons();
			Arrays.fill(inputButtons, false);

			input.setCursorDeltaX(0);
			input.setCursorDeltaY(0);
			input.setScrollClicks(0);
			input.getDownUpKeystrokes().clear();
			input.getDownUpMouseButtons().clear();
			input.getOnLockKeys().clear();
			input.getOffLockKeys().clear();

			return new OutputCapture(axes, buttons, downKeystrokes, downUpKeystrokes, downMouseButtons,
					downUpMouseButtons, cursorDeltaX, cursorDeltaY, scrollClicks, onLockKeys, offLockKeys);
		}

		private Map<VirtualAxis, Integer> axes() {
			return axes;
		}

		private boolean[] buttons() {
			return buttons;
		}

		private Set<Keystroke> downKeystrokes() {
			return downKeystrokes;
		}

		private Set<Integer> downMouseButtons() {
			return downMouseButtons;
		}

		private Set<Keystroke> downUpKeystrokes() {
			return downUpKeystrokes;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != getClass()) {
				return false;
			}
			final var that = (OutputCapture) obj;
			return Objects.equals(axes, that.axes) && Arrays.equals(buttons, that.buttons)
					&& Objects.equals(downKeystrokes, that.downKeystrokes)
					&& Objects.equals(downUpKeystrokes, that.downUpKeystrokes)
					&& Objects.equals(downMouseButtons, that.downMouseButtons)
					&& Objects.equals(downUpMouseButtons, that.downUpMouseButtons) && cursorDeltaX == that.cursorDeltaX
					&& cursorDeltaY == that.cursorDeltaY && scrollClicks == that.scrollClicks
					&& Objects.equals(onLockKeys, that.onLockKeys) && Objects.equals(offLockKeys, that.offLockKeys);
		}

		@Override
		public int hashCode() {
			return Objects.hash(axes, Arrays.hashCode(buttons), downKeystrokes, downUpKeystrokes, downMouseButtons,
					downUpMouseButtons, cursorDeltaX, cursorDeltaY, scrollClicks, onLockKeys, offLockKeys);
		}

		private Set<LockKey> offLockKeys() {
			return offLockKeys;
		}

		private Set<LockKey> onLockKeys() {
			return onLockKeys;
		}

		private int scrollClicks() {
			return scrollClicks;
		}

		@Override
		public String toString() {
			return "OutputCapture[" + "axes=" + axes + ", " + "buttons=" + Arrays.toString(buttons) + ", "
					+ "downKeystrokes=" + downKeystrokes + ", " + "downUpKeystrokes=" + downUpKeystrokes + ", "
					+ "downMouseButtons=" + downMouseButtons + ", " + "downUpMouseButtons=" + downUpMouseButtons + ", "
					+ "cursorDeltaX=" + cursorDeltaX + ", " + "cursorDeltaY=" + cursorDeltaY + ", " + "scrollClicks="
					+ scrollClicks + ", " + "onLockKeys=" + onLockKeys + ", " + "offLockKeys=" + offLockKeys + ']';
		}
	}

	@Nested
	@DisplayName("Axis mapping")
	final class AxisMappingTests {

		@Test
		@DisplayName("axis within dead zone produces center value")
		void axisWithinDeadZoneProducesCenterValue() {
			final var action = newAxisToAxisAction(VirtualAxis.X);
			action.setDeadZone(0.2f);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, new ArrayList<>(List.of(action)));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.15f;

			final var output = pollWithFrame(axes, noButtons());

			assertAxisEquals(input.floatToIntAxisValue(0f), output.axes().get(VirtualAxis.X));
		}

		@Test
		@DisplayName("inverted axis reverses output")
		void invertedAxisReversesOutput() {
			final var action = newAxisToAxisAction(VirtualAxis.X);
			action.setInvert(true);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, new ArrayList<>(List.of(action)));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.5f;

			final var output = pollWithFrame(axes, noButtons());

			assertAxisEquals(input.floatToIntAxisValue(-0.5f), output.axes().get(VirtualAxis.X));
		}

		@Test
		@DisplayName("maps SDL left stick X to virtual axis X")
		void mapsLeftStickXToVirtualAxisX() {
			setUpAxisProfile(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, VirtualAxis.X);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.75f;

			final var output = pollWithFrame(axes, noButtons());

			final var expectedValue = input.floatToIntAxisValue(0.75f);
			assertAxisEquals(expectedValue, output.axes().get(VirtualAxis.X));
		}

		@Test
		@DisplayName("maps multiple axes simultaneously")
		void mapsMultipleAxes() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToAxisAction(VirtualAxis.X))));
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY,
					new ArrayList<>(List.of(newAxisToAxisAction(VirtualAxis.Y))));
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX,
					new ArrayList<>(List.of(newAxisToAxisAction(VirtualAxis.RX))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 1.0f;
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY] = -1.0f;
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX] = 0.5f;

			final var output = pollWithFrame(axes, noButtons());

			assertAxisEquals(input.floatToIntAxisValue(1.0f), output.axes().get(VirtualAxis.X));
			assertAxisEquals(input.floatToIntAxisValue(-1.0f), output.axes().get(VirtualAxis.Y));
			assertAxisEquals(input.floatToIntAxisValue(0.5f), output.axes().get(VirtualAxis.RX));
		}

		@Test
		@DisplayName("maps SDL right stick Y to virtual axis RY")
		void mapsRightStickYToVirtualAxisRY() {
			setUpAxisProfile(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY, VirtualAxis.RY);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY] = -0.5f;

			final var output = pollWithFrame(axes, noButtons());

			final var expectedValue = input.floatToIntAxisValue(-0.5f);
			assertAxisEquals(expectedValue, output.axes().get(VirtualAxis.RY));
		}

		@Test
		@DisplayName("zero axis input produces center value")
		void zeroAxisProducesCenterValue() {
			setUpAxisProfile(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, VirtualAxis.X);

			final var output = pollWithFrame(noAxes(), noButtons());

			assertAxisEquals(input.floatToIntAxisValue(0f), output.axes().get(VirtualAxis.X));
		}
	}

	@Nested
	@DisplayName("Axis to button mapping")
	final class AxisToButtonActionTests {

		@Test
		@DisplayName("axis in zone activates virtual button")
		void axisInZoneActivatesButton() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToButtonAction(0, 0.5f, 1.0f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.75f;

			final var output = pollWithFrame(axes, noButtons());

			Assertions.assertTrue(output.buttons()[0]);
		}

		@Test
		@DisplayName("axis leaving zone deactivates virtual button")
		void axisLeavingZoneDeactivatesButton() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToButtonAction(0, 0.5f, 1.0f))));
			setProfile(profile);

			final var axesInZone = noAxes();
			axesInZone[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.75f;
			pollWithFrame(axesInZone, noButtons());

			final var axesOutOfZone = noAxes();
			axesOutOfZone[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.3f;
			final var output = pollWithFrame(axesOutOfZone, noButtons());

			Assertions.assertFalse(output.buttons()[0]);
		}

		@Test
		@DisplayName("axis outside zone does not activate virtual button")
		void axisOutsideZoneDoesNotActivateButton() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToButtonAction(0, 0.5f, 1.0f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.3f;

			final var output = pollWithFrame(axes, noButtons());

			Assertions.assertFalse(output.buttons()[0]);
		}
	}

	@Nested
	@DisplayName("Axis to cursor mapping")
	final class AxisToCursorActionTests {

		@Test
		@DisplayName("axis drives cursor X movement")
		void axisDrivesCursorX() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToCursorAction(MouseAxis.X, 500000, 0.05f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;

			final var output = pollWithFrame(axes, noButtons());

			Assertions.assertNotEquals(0, output.cursorDeltaX);
		}

		@Test
		@DisplayName("axis drives cursor Y movement")
		void axisDrivesCursorY() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY,
					new ArrayList<>(List.of(newAxisToCursorAction(MouseAxis.Y, 500000, 0.05f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY] = 0.8f;

			final var output = pollWithFrame(axes, noButtons());

			Assertions.assertNotEquals(0, output.cursorDeltaY);
		}

		@Test
		@DisplayName("axis in dead zone produces no cursor movement")
		void axisInDeadZoneProducesNoCursorMovement() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToCursorAction(MouseAxis.X, 500000, 0.1f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.05f;

			final var output = pollWithFrame(axes, noButtons());

			Assertions.assertEquals(0, output.cursorDeltaX);
		}
	}

	@Nested
	@DisplayName("Axis to key mapping")
	final class AxisToKeyActionTests {

		@Test
		@DisplayName("axis in zone activates keystroke")
		void axisInZoneActivatesKeystroke() {
			final var wScancode = scancode(Scancode.DIK_W);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToKeyAction(0.5f, 1.0f, wScancode))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;

			final var output = pollWithFrame(axes, noButtons());

			final var expectedKeystroke = new Keystroke(new Scancode[] { wScancode }, new Scancode[0]);
			Assertions.assertTrue(output.downKeystrokes().contains(expectedKeystroke));
		}

		@Test
		@DisplayName("axis leaving zone deactivates keystroke")
		void axisLeavingZoneDeactivatesKeystroke() {
			final var wScancode = scancode(Scancode.DIK_W);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToKeyAction(0.5f, 1.0f, wScancode))));
			setProfile(profile);

			final var axesInZone = noAxes();
			axesInZone[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;
			pollWithFrame(axesInZone, noButtons());

			final var axesOutOfZone = noAxes();
			axesOutOfZone[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.3f;
			final var output = pollWithFrame(axesOutOfZone, noButtons());

			Assertions.assertTrue(output.downKeystrokes().isEmpty());
		}

		@Test
		@DisplayName("axis outside zone does not activate keystroke")
		void axisOutsideZoneDoesNotActivateKeystroke() {
			final var wScancode = scancode(Scancode.DIK_W);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToKeyAction(0.5f, 1.0f, wScancode))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.3f;

			final var output = pollWithFrame(axes, noButtons());

			Assertions.assertTrue(output.downKeystrokes().isEmpty());
		}
	}

	@Nested
	@DisplayName("Axis to mouse button mapping")
	final class AxisToMouseButtonActionTests {

		@Test
		@DisplayName("axis in zone activates mouse button")
		void axisInZoneActivatesMouseButton() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToMouseButtonAction(1, 0.5f, 1.0f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;

			final var output = pollWithFrame(axes, noButtons());

			Assertions.assertTrue(output.downMouseButtons().contains(1));
		}

		@Test
		@DisplayName("axis leaving zone deactivates mouse button")
		void axisLeavingZoneDeactivatesMouseButton() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToMouseButtonAction(1, 0.5f, 1.0f))));
			setProfile(profile);

			final var axesInZone = noAxes();
			axesInZone[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;
			pollWithFrame(axesInZone, noButtons());

			final var axesOutOfZone = noAxes();
			axesOutOfZone[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.3f;
			final var output = pollWithFrame(axesOutOfZone, noButtons());

			Assertions.assertFalse(output.downMouseButtons().contains(1));
		}

		@Test
		@DisplayName("axis outside zone does not activate mouse button")
		void axisOutsideZoneDoesNotActivateMouseButton() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToMouseButtonAction(1, 0.5f, 1.0f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.3f;

			final var output = pollWithFrame(axes, noButtons());

			Assertions.assertFalse(output.downMouseButtons().contains(1));
		}
	}

	@Nested
	@DisplayName("Axis to relative axis mapping")
	final class AxisToRelativeAxisActionTests {

		@Test
		@DisplayName("relative axis accumulates over multiple frames")
		void relativeAxisAccumulatesOverFrames() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToRelativeAxisAction(VirtualAxis.RX, 100f, 0.05f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;

			final var initialValue = input.getAxes().get(VirtualAxis.RX);

			for (var i = 0; i < 10; i++) {
				pollWithFrame(axes, noButtons());
			}

			Assertions.assertNotEquals(initialValue, input.getAxes().get(VirtualAxis.RX));
		}

		@Test
		@DisplayName("relative axis in dead zone does not change value")
		void relativeAxisDeadZoneIgnoresInput() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToRelativeAxisAction(VirtualAxis.RX, 100f, 0.3f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.2f;

			final var initialValue = input.getAxes().get(VirtualAxis.RX);

			for (var i = 0; i < 10; i++) {
				pollWithFrame(axes, noButtons());
			}

			Assertions.assertEquals(initialValue, input.getAxes().get(VirtualAxis.RX));
		}

		@Test
		@DisplayName("inverted relative axis reverses direction")
		void relativeAxisInvertReversesDirection() {
			final var action = newAxisToRelativeAxisAction(VirtualAxis.RX, 100f, 0.05f);
			action.setInvert(true);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, new ArrayList<>(List.of(action)));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;

			final var initialValue = input.getAxes().get(VirtualAxis.RX);

			for (var i = 0; i < 10; i++) {
				pollWithFrame(axes, noButtons());
			}

			Assertions.assertTrue(input.getAxes().get(VirtualAxis.RX) < initialValue);
		}
	}

	@Nested
	@DisplayName("Axis to scroll mapping")
	final class AxisToScrollActionTests {

		@Test
		@DisplayName("axis drives scroll")
		void axisDrivesScroll() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY,
					new ArrayList<>(List.of(newAxisToScrollAction(5000, 0.05f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY] = 0.9f;

			final var output = pollWithFrame(axes, noButtons());

			Assertions.assertNotEquals(0, output.scrollClicks());
		}

		@Test
		@DisplayName("axis in dead zone produces no scroll")
		void axisInDeadZoneProducesNoScroll() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY,
					new ArrayList<>(List.of(newAxisToScrollAction(5000, 0.1f))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY] = 0.05f;

			final var output = pollWithFrame(axes, noButtons());

			Assertions.assertEquals(0, output.scrollClicks());
		}

		@Test
		@DisplayName("axis returning to dead zone stops scroll")
		void axisReturningToDeadZoneStopsScroll() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY,
					new ArrayList<>(List.of(newAxisToScrollAction(5000, 0.05f))));
			setProfile(profile);

			final var axesActive = noAxes();
			axesActive[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY] = 0.9f;
			pollWithFrame(axesActive, noButtons());

			final var output = pollWithFrame(noAxes(), noButtons());

			Assertions.assertEquals(0, output.scrollClicks());
		}
	}

	@Nested
	@DisplayName("Button mapping")
	final class ButtonMappingTests {

		@Test
		@DisplayName("button release clears virtual button state")
		void buttonReleaseClearsState() {
			setUpButtonProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, 0);

			final var pressedButtons = noButtons();
			pressedButtons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			pollWithFrame(noAxes(), pressedButtons);

			final var output = pollWithFrame(noAxes(), noButtons());

			Assertions.assertFalse(output.buttons()[0]);
		}

		@Test
		@DisplayName("maps SDL button SOUTH to virtual button 0")
		void mapsButtonSouthToVirtualButton0() {
			setUpButtonProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, 0);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output = pollWithFrame(noAxes(), buttons);

			Assertions.assertTrue(output.buttons()[0]);
		}

		@Test
		@DisplayName("maps multiple buttons simultaneously")
		void mapsMultipleButtons() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToButtonAction(0))));
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_EAST,
					new ArrayList<>(List.of(newButtonToButtonAction(1))));
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_WEST,
					new ArrayList<>(List.of(newButtonToButtonAction(2))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_EAST] = true;
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_WEST] = true;

			final var output = pollWithFrame(noAxes(), buttons);

			Assertions.assertTrue(output.buttons()[0]);
			Assertions.assertTrue(output.buttons()[1]);
			Assertions.assertTrue(output.buttons()[2]);
		}
	}

	@Nested
	@DisplayName("Button to axis reset mapping")
	final class ButtonToAxisResetActionTests {

		@Test
		@DisplayName("axis reset with ON_PRESS resets axis on button press")
		void axisResetOnPressResetsAxis() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToAxisAction(VirtualAxis.X))));
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, new ArrayList<>(
					List.of(newButtonToAxisResetAction(VirtualAxis.X, 0f, Activation.ON_PRESS, false))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;
			pollWithFrame(axes, noButtons());

			final var buttonsPressed = noButtons();
			buttonsPressed[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			final var output = pollWithFrame(axes, buttonsPressed);

			assertAxisEquals(input.floatToIntAxisValue(0f), output.axes().get(VirtualAxis.X));
		}

		@Test
		@DisplayName("axis reset with ON_RELEASE resets axis on button release")
		void axisResetOnReleaseFiresOnRelease() {
			final var resetAction = newButtonToAxisResetAction(VirtualAxis.X, 0f, Activation.ON_RELEASE, false);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToAxisAction(VirtualAxis.X))));
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(resetAction)));
			setProfile(profile);

			resetAction.init(input);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;

			final var buttonsPressed = noButtons();
			buttonsPressed[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			pollWithFrame(axes, buttonsPressed);

			final var output = pollWithFrame(axes, noButtons());

			assertAxisEquals(input.floatToIntAxisValue(0f), output.axes().get(VirtualAxis.X));
		}

		@Test
		@DisplayName("axis reset with WHILE_PRESSED resets axis every frame while held")
		void axisResetWhilePressedFiresEveryFrame() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToAxisAction(VirtualAxis.X))));
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, new ArrayList<>(
					List.of(newButtonToAxisResetAction(VirtualAxis.X, 0f, Activation.WHILE_PRESSED, false))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;

			final var buttonsPressed = noButtons();
			buttonsPressed[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			final var output = pollWithFrame(axes, buttonsPressed);

			assertAxisEquals(input.floatToIntAxisValue(0f), output.axes().get(VirtualAxis.X));
		}
	}

	@Nested
	@DisplayName("Button to cursor mapping")
	final class ButtonToCursorActionTests {

		@Test
		@DisplayName("button drives cursor X movement")
		void buttonDrivesCursorX() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToCursorAction(MouseAxis.X, 500000))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output = pollWithFrame(noAxes(), buttons);

			Assertions.assertNotEquals(0, output.cursorDeltaX);
		}

		@Test
		@DisplayName("button drives cursor Y movement")
		void buttonDrivesCursorY() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToCursorAction(MouseAxis.Y, 500000))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output = pollWithFrame(noAxes(), buttons);

			Assertions.assertNotEquals(0, output.cursorDeltaY);
		}

		@Test
		@DisplayName("releasing button stops cursor movement")
		void releaseStopsCursorMovement() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToCursorAction(MouseAxis.X, 500000))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			pollWithFrame(noAxes(), buttons);

			final var output = pollWithFrame(noAxes(), noButtons());

			Assertions.assertEquals(0, output.cursorDeltaX);
		}
	}

	@Nested
	@DisplayName("Button to cycle mapping")
	final class ButtonToCycleActionTests {

		@Test
		@DisplayName("cycle advances to next sub-action on each press")
		void cycleAdvancesOnEachPress() {
			final var scancode1 = scancode(Scancode.DIK_1);
			final var scancode2 = scancode(Scancode.DIK_2);
			final var scancode3 = scancode(Scancode.DIK_3);

			final var cycleAction = newButtonToCycleAction(Activation.ON_PRESS, List.of(newButtonToKeyAction(scancode1),
					newButtonToKeyAction(scancode2), newButtonToKeyAction(scancode3)));
			cycleAction.init(input);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(cycleAction)));
			setProfile(profile);

			final var buttonsPressed = noButtons();
			buttonsPressed[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output1 = pollWithFrame(noAxes(), buttonsPressed);
			Assertions.assertTrue(
					output1.downUpKeystrokes().contains(new Keystroke(new Scancode[] { scancode1 }, new Scancode[0])));

			pollWithFrame(noAxes(), noButtons());
			final var output2 = pollWithFrame(noAxes(), buttonsPressed);
			Assertions.assertTrue(
					output2.downUpKeystrokes().contains(new Keystroke(new Scancode[] { scancode2 }, new Scancode[0])));

			pollWithFrame(noAxes(), noButtons());
			final var output3 = pollWithFrame(noAxes(), buttonsPressed);
			Assertions.assertTrue(
					output3.downUpKeystrokes().contains(new Keystroke(new Scancode[] { scancode3 }, new Scancode[0])));
		}

		@Test
		@DisplayName("cycle reset returns to first sub-action")
		void cycleResetReturnsToFirstAction() {
			final var scancode1 = scancode(Scancode.DIK_1);
			final var scancode2 = scancode(Scancode.DIK_2);

			final var cycleAction = newButtonToCycleAction(Activation.ON_PRESS,
					List.of(newButtonToKeyAction(scancode1), newButtonToKeyAction(scancode2)));
			cycleAction.init(input);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(cycleAction)));
			setProfile(profile);

			final var buttonsPressed = noButtons();
			buttonsPressed[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			pollWithFrame(noAxes(), buttonsPressed);
			pollWithFrame(noAxes(), noButtons());

			cycleAction.reset(input);

			final var output = pollWithFrame(noAxes(), buttonsPressed);
			Assertions.assertTrue(
					output.downUpKeystrokes().contains(new Keystroke(new Scancode[] { scancode1 }, new Scancode[0])));
		}

		@Test
		@DisplayName("cycle wraps back to first sub-action after last")
		void cycleWrapsAfterLastAction() {
			final var scancode1 = scancode(Scancode.DIK_1);
			final var scancode2 = scancode(Scancode.DIK_2);

			final var cycleAction = newButtonToCycleAction(Activation.ON_PRESS,
					List.of(newButtonToKeyAction(scancode1), newButtonToKeyAction(scancode2)));
			cycleAction.init(input);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(cycleAction)));
			setProfile(profile);

			final var buttonsPressed = noButtons();
			buttonsPressed[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			pollWithFrame(noAxes(), buttonsPressed);
			pollWithFrame(noAxes(), noButtons());
			pollWithFrame(noAxes(), buttonsPressed);
			pollWithFrame(noAxes(), noButtons());

			final var output = pollWithFrame(noAxes(), buttonsPressed);
			Assertions.assertTrue(
					output.downUpKeystrokes().contains(new Keystroke(new Scancode[] { scancode1 }, new Scancode[0])));
		}
	}

	@Nested
	@DisplayName("Button to lock key mapping")
	final class ButtonToLockKeyActionTests {

		@Test
		@DisplayName("holding button does not repeat lock key toggle")
		void holdDoesNotRepeatToggle() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToLockKeyAction(LockKey.CAPS_LOCK_LOCK_KEY, true))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output1 = pollWithFrame(noAxes(), buttons);
			Assertions.assertTrue(output1.onLockKeys().contains(LockKey.CAPS_LOCK_LOCK_KEY));

			final var output2 = pollWithFrame(noAxes(), buttons);
			Assertions.assertFalse(output2.onLockKeys().contains(LockKey.CAPS_LOCK_LOCK_KEY));
		}

		@Test
		@DisplayName("lock key off adds to off lock keys set")
		void lockKeyOffAddsToOffSet() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToLockKeyAction(LockKey.NUM_LOCK_LOCK_KEY, false))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output = pollWithFrame(noAxes(), buttons);

			Assertions.assertTrue(output.offLockKeys().contains(LockKey.NUM_LOCK_LOCK_KEY));
		}

		@Test
		@DisplayName("pressing button toggles lock key on")
		void pressTogglesLockKeyOn() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToLockKeyAction(LockKey.CAPS_LOCK_LOCK_KEY, true))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output = pollWithFrame(noAxes(), buttons);

			Assertions.assertTrue(output.onLockKeys().contains(LockKey.CAPS_LOCK_LOCK_KEY));
		}
	}

	@Nested
	@DisplayName("Button to scroll mapping")
	final class ButtonToScrollActionTests {

		@Test
		@DisplayName("button drives scroll")
		void buttonDrivesScroll() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToScrollAction(5000))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output = pollWithFrame(noAxes(), buttons);

			Assertions.assertNotEquals(0, output.scrollClicks());
		}

		@Test
		@DisplayName("inverted scroll reverses direction")
		void invertReversesScrollDirection() {
			final var normalAction = newButtonToScrollAction(5000);

			final var invertedAction = newButtonToScrollAction(5000);
			invertedAction.setInvert(true);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(normalAction)));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			final var normalOutput = pollWithFrame(noAxes(), buttons);

			input.reset();
			sdlGamepadMock.close();
			setUp();

			final var invertedProfile = new Profile();
			final var invertedDefaultMode = invertedProfile.getModes().getFirst();
			invertedDefaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(invertedAction)));
			setProfile(invertedProfile);

			final var invertedButtons = noButtons();
			invertedButtons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			final var invertedOutput = pollWithFrame(noAxes(), invertedButtons);

			Assertions.assertNotEquals(0, normalOutput.scrollClicks());
			Assertions.assertNotEquals(0, invertedOutput.scrollClicks());
			Assertions.assertTrue((normalOutput.scrollClicks() > 0 && invertedOutput.scrollClicks() < 0)
					|| (normalOutput.scrollClicks() < 0 && invertedOutput.scrollClicks() > 0));
		}

		@Test
		@DisplayName("releasing button stops scroll")
		void releaseStopsScroll() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToScrollAction(5000))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			pollWithFrame(noAxes(), buttons);

			final var output = pollWithFrame(noAxes(), noButtons());

			Assertions.assertEquals(0, output.scrollClicks());
		}
	}

	@Nested
	@DisplayName("Keystroke mapping")
	final class KeystrokeMappingTests {

		@Test
		@DisplayName("maps button to keystroke")
		void mapsButtonToKeystroke() {
			final var wScancode = scancode(Scancode.DIK_W);
			setUpButtonToKeyProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, wScancode);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output = pollWithFrame(noAxes(), buttons);

			final var expectedKeystroke = new Keystroke(new Scancode[] { wScancode }, new Scancode[0]);
			Assertions.assertTrue(output.downKeystrokes().contains(expectedKeystroke));
		}

		@Test
		@DisplayName("maps button to keystroke with modifier")
		void mapsButtonToKeystrokeWithModifier() {
			final var aScancode = scancode(Scancode.DIK_A);
			final var lCtrlScancode = scancode(Scancode.DIK_LCONTROL);

			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToKeyActionWithModifiers(new Scancode[] { aScancode },
							new Scancode[] { lCtrlScancode }))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output = pollWithFrame(noAxes(), buttons);

			final var expectedKeystroke = new Keystroke(new Scancode[] { aScancode }, new Scancode[] { lCtrlScancode });
			Assertions.assertTrue(output.downKeystrokes().contains(expectedKeystroke));
		}

		@Test
		@DisplayName("releasing button clears keystroke")
		void releasingButtonClearsKeystroke() {
			final var wScancode = scancode(Scancode.DIK_W);
			setUpButtonToKeyProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, wScancode);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			pollWithFrame(noAxes(), buttons);

			final var output = pollWithFrame(noAxes(), noButtons());

			Assertions.assertTrue(output.downKeystrokes().isEmpty());
		}
	}

	@Nested
	@DisplayName("Mode switching")
	final class ModeSwitchingTests {

		@Test
		@DisplayName("momentary mode switch changes axis mapping while held")
		void momentaryModeSwitchChangesMapping() {
			setUpMultiModeProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH, false, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					VirtualAxis.X, VirtualAxis.Y);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.8f;

			final var outputMode0 = pollWithFrame(axes, noButtons());
			assertAxisEquals(input.floatToIntAxisValue(0.8f), outputMode0.axes().get(VirtualAxis.X));

			final var buttonsWithModeSwitch = noButtons();
			buttonsWithModeSwitch[SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH] = true;
			pollWithFrame(axes, buttonsWithModeSwitch);

			final var outputMode1 = pollWithFrame(axes, buttonsWithModeSwitch);
			assertAxisEquals(input.floatToIntAxisValue(0.8f), outputMode1.axes().get(VirtualAxis.Y));
		}

		@Test
		@DisplayName("releasing momentary mode switch returns to default mode")
		void releasingMomentaryModeSwitchReturns() {
			final var profile = setUpMultiModeProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH, false,
					SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, VirtualAxis.X, VirtualAxis.Y);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.6f;

			final var buttonsWithModeSwitch = noButtons();
			buttonsWithModeSwitch[SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH] = true;
			pollWithFrame(axes, buttonsWithModeSwitch);

			pollWithFrame(axes, noButtons());

			Assertions.assertEquals(Profile.DEFAULT_MODE, profile.getActiveMode());
		}

		@Test
		@DisplayName("toggle mode switch persists after button release")
		void toggleModeSwitchPersists() {
			setUpMultiModeProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH, true, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					VirtualAxis.X, VirtualAxis.Y);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.7f;

			final var buttonsWithModeSwitch = noButtons();
			buttonsWithModeSwitch[SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH] = true;
			pollWithFrame(axes, buttonsWithModeSwitch);

			final var outputAfterRelease = pollWithFrame(axes, noButtons());
			assertAxisEquals(input.floatToIntAxisValue(0.7f), outputAfterRelease.axes().get(VirtualAxis.Y));
		}
	}

	@Nested
	@DisplayName("Mouse button mapping")
	final class MouseButtonMappingTests {

		@Test
		@DisplayName("maps button to left mouse button")
		void mapsButtonToLeftMouseButton() {
			setUpButtonToMouseButtonProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, 1);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output = pollWithFrame(noAxes(), buttons);

			Assertions.assertTrue(output.downMouseButtons().contains(1));
		}

		@Test
		@DisplayName("releasing button clears mouse button")
		void releasingButtonClearsMouseButton() {
			setUpButtonToMouseButtonProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, 1);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			pollWithFrame(noAxes(), buttons);

			final var output = pollWithFrame(noAxes(), noButtons());

			Assertions.assertFalse(output.downMouseButtons().contains(1));
		}
	}

	@Nested
	@DisplayName("Multi-frame sequences")
	final class MultiFrameTests {

		@Test
		@DisplayName("axis and button combined in same frame")
		void axisAndButtonCombined() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getAxisToActionsMap().put(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX,
					new ArrayList<>(List.of(newAxisToAxisAction(VirtualAxis.X))));
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToButtonAction(0))));
			setProfile(profile);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = -0.3f;
			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output = pollWithFrame(axes, buttons);

			assertAxisEquals(input.floatToIntAxisValue(-0.3f), output.axes().get(VirtualAxis.X));
			Assertions.assertTrue(output.buttons()[0]);
		}

		@Test
		@DisplayName("axis value persists across multiple frames")
		void axisValuePersistsAcrossFrames() {
			setUpAxisProfile(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, VirtualAxis.X);

			final var axes = noAxes();
			axes[SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX] = 0.5f;

			final var output1 = pollWithFrame(axes, noButtons());
			final var output2 = pollWithFrame(axes, noButtons());

			Assertions.assertEquals(output1.axes().get(VirtualAxis.X), output2.axes().get(VirtualAxis.X));
		}

		@Test
		@DisplayName("button held across multiple frames stays pressed")
		void buttonHeldAcrossFramesStaysPressed() {
			setUpButtonProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, 0);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var output1 = pollWithFrame(noAxes(), buttons);
			final var output2 = pollWithFrame(noAxes(), buttons);

			Assertions.assertTrue(output1.buttons()[0]);
			Assertions.assertTrue(output2.buttons()[0]);
		}

		@Test
		@DisplayName("keystroke held across multiple frames stays in downKeystrokes")
		void keystrokeHeldAcrossFrames() {
			final var sScancode = scancode(Scancode.DIK_S);
			setUpButtonToKeyProfile(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, sScancode);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;

			final var expectedKeystroke = new Keystroke(new Scancode[] { sScancode }, new Scancode[0]);

			final var output1 = pollWithFrame(noAxes(), buttons);
			final var output2 = pollWithFrame(noAxes(), buttons);

			Assertions.assertTrue(output1.downKeystrokes().contains(expectedKeystroke));
			Assertions.assertTrue(output2.downKeystrokes().contains(expectedKeystroke));
		}
	}

	@Nested
	@DisplayName("Null action")
	final class NullActionTests {

		@Test
		@DisplayName("null action produces no output")
		void nullActionProducesNoOutput() {
			final var before = OutputCapture.captureAndReset(input);
			new NullAction().doAction(input, 0, 0f);
			final var after = OutputCapture.captureAndReset(input);

			Assertions.assertEquals(before, after);
		}
	}

	@Nested
	@DisplayName("On-screen keyboard actions")
	final class OnScreenKeyboardActionTests {

		@Test
		@DisplayName("press on-screen keyboard key and release on button release")
		void pressOnScreenKeyboardKey() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToPressOnScreenKeyboardKeyAction(false))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			pollWithFrame(noAxes(), buttons);

			Mockito.verify(mockOnScreenKeyboard).pressSelectedButton();

			pollWithFrame(noAxes(), noButtons());

			Mockito.verify(mockOnScreenKeyboard).releaseSelectedButton();
		}

		@Test
		@DisplayName("release all on-screen keyboard keys on button press")
		void releaseAllOnScreenKeyboardKeys() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToReleaseAllOnScreenKeyboardKeysAction())));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			pollWithFrame(noAxes(), buttons);

			Mockito.verify(mockOnScreenKeyboard).releaseAllButtons();
		}

		@Test
		@DisplayName("select on-screen keyboard key moves selector in configured direction")
		void selectOnScreenKeyboardKey() {
			final var profile = new Profile();
			final var defaultMode = profile.getModes().getFirst();
			defaultMode.getButtonToActionsMap().put(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,
					new ArrayList<>(List.of(newButtonToSelectOnScreenKeyboardKeyAction(Direction.RIGHT))));
			setProfile(profile);

			final var buttons = noButtons();
			buttons[SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH] = true;
			pollWithFrame(noAxes(), buttons);

			Mockito.verify(mockOnScreenKeyboard).moveSelector(Direction.RIGHT);
		}
	}

	@Nested
	@DisplayName("Reflection sanity checks")
	final class ReflectionSanityTests {

		@Test
		@DisplayName("all reflected Input fields exist")
		void allReflectedInputFieldsExist() {
			final var fieldNames = new String[] { "sdlGamepadToGamepadStateMap", "selectedSdlGamepad", "initialized",
					"lastPollTime", "swapLeftAndRightSticks", "mapCircularAxesToSquareAxes" };

			for (final var fieldName : fieldNames) {
				try {
					final var field = Input.class.getDeclaredField(fieldName);
					field.setAccessible(true);
				} catch (final NoSuchFieldException e) {
					throw new AssertionError("Expected field not found on Input: " + fieldName, e);
				}
			}
		}

		@SuppressWarnings({ "RethrowReflectiveOperationExceptionAsLinkageError", "ReturnValueIgnored" })
		@Test
		@DisplayName("GamepadState inner final class exists with expected fields")
		void gamepadStateClassExists() {
			try {
				final var gamepadStateClass = Class.forName("de.bwravencl.controllerbuddy.input.Input$GamepadState");
				gamepadStateClass.getDeclaredField("axes");
				gamepadStateClass.getDeclaredField("buttons");
				gamepadStateClass.getDeclaredField("sdlGamepad");
			} catch (final ReflectiveOperationException e) {
				throw new AssertionError("GamepadState inner final class or its fields not found", e);
			}
		}
	}
}

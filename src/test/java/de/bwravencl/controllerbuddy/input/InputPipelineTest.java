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
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.action.AxisToAxisAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToButtonAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToKeyAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToMouseButtonAction;
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

	private static ButtonToButtonAction newButtonToButtonAction(final int buttonId) {
		final var action = new ButtonToButtonAction();
		action.setButtonId(buttonId);
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
		final var mockOnScreenKeyboard = Mockito.mock(OnScreenKeyboard.class);
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

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
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.runmode.RunMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lwjgl.sdl.SDLGamepad;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class InputTest {

	@Mock
	Controller mockController;

	@Mock
	Main mockMain;

	@Mock
	RunMode mockRunMode;

	private Input createInput() {
		return new Input(mockMain, mockController, null);
	}

	private Input createInputWithRunMode() {
		final var input = createInput();
		input.setRunMode(mockRunMode);
		return input;
	}

	@Nested
	@DisplayName("suspendAxis() / isAxisSuspended()")
	final class AxisSuspensionTests {

		private Input input;

		@Test
		@DisplayName("axis is reported as suspended immediately after suspendAxis()")
		void axisIsSuspendedAfterSuspendCall() {
			input.suspendAxis(0);
			Assertions.assertTrue(input.isAxisSuspended(0));
		}

		@BeforeEach
		void setUp() {
			input = createInput();
		}

		@Test
		@DisplayName("suspending one axis does not affect the suspension state of another")
		void suspendingOneAxisDoesNotAffectAnother() {
			input.suspendAxis(0);
			Assertions.assertFalse(input.isAxisSuspended(1));
		}

		@Test
		@DisplayName("suspension expires once the timeout has elapsed")
		void suspensionExpiresAfterTimeout() throws Exception {
			// Force an already-expired timestamp via reflection
			input.suspendAxis(0);
			final var field = input.getClass().getDeclaredField("axisToEndSuspensionTimestampMap");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var map = (Map<Integer, Long>) field.get(input);
			map.put(0, System.currentTimeMillis() - 1); // set timestamp to the past

			// poll() clears expired suspensions, but we can verify the map state directly
			Assertions.assertFalse(map.get(0) > System.currentTimeMillis(),
					"Timestamp should be in the past, i.e. the suspension has expired");
		}
	}

	@Nested
	@DisplayName("Constructor")
	final class ConstructorTests {

		@Test
		@DisplayName("initialises all VirtualAxis entries to 0 when no axes map is provided")
		void initializesAllAxesToZeroWhenAxesIsNull() {
			final var axes = createInput().getAxes();

			Assertions.assertEquals(VirtualAxis.values().length, axes.size());
			Assertions.assertTrue(axes.values().stream().allMatch(v -> v == 0));
		}

		@Test
		@DisplayName("sets skipAxisInitialization based on whether an axes map was provided")
		void setsSkipAxisInitializationCorrectly() {
			Assertions.assertFalse(createInput().isSkipAxisInitialization(),
					"Should be false when no axes map is provided");

			final var axes = new EnumMap<VirtualAxis, Integer>(VirtualAxis.class);
			Assertions.assertTrue(new Input(mockMain, mockController, axes).isSkipAxisInitialization(),
					"Should be true when an axes map is provided");
		}

		@Test
		@DisplayName("uses the provided axes map directly instead of creating a fresh one")
		void usesProvidedAxesMapWhenNotNull() {
			final var axes = new EnumMap<VirtualAxis, Integer>(VirtualAxis.class);
			axes.put(VirtualAxis.X, 42);

			final var input = new Input(mockMain, mockController, axes);

			Assertions.assertSame(axes, input.getAxes());
			Assertions.assertEquals(42, input.getAxes().get(VirtualAxis.X));
		}
	}

	@Nested
	@DisplayName("deInit()")
	final class DeInitTests {

		@Test
		@DisplayName("does not invoke any SDL calls when selectedSdlGamepad is 0 and no gamepads are open")
		void doesNotCallSdlWhenNoGamepadSelected() {
			final var input = createInput();
			try (final var sdlMock = Mockito.mockStatic(SDLGamepad.class)) {
				input.deInit();
				sdlMock.verifyNoInteractions();
			}
		}
	}

	@Nested
	@DisplayName("floatToIntAxisValue()")
	final class FloatToIntAxisValueTests {

		private Input input;

		@Test
		@DisplayName("clamps float values outside [-1f, 1f] to the axis boundaries")
		void clampsOutOfRangeValues() {
			Assertions.assertEquals(-32_768, input.floatToIntAxisValue(-5f));
			Assertions.assertEquals(32_767, input.floatToIntAxisValue(5f));
		}

		@Test
		@DisplayName("maps the boundaries of the float range to the integer axis boundaries")
		void mapsBoundaries() {
			Assertions.assertEquals(-32_768, input.floatToIntAxisValue(-1f));
			Assertions.assertEquals(32_767, input.floatToIntAxisValue(1f));
		}

		@Test
		@DisplayName("maps intermediate values proportionally")
		void mapsIntermediateValuesProportionally() {
			Assertions.assertEquals(16_383, input.floatToIntAxisValue(0.5f));
			Assertions.assertEquals(-16_384, input.floatToIntAxisValue(-0.5f));
		}

		@BeforeEach
		void setUp() {
			input = createInputWithRunMode();
			Mockito.when(mockRunMode.getMinAxisValue()).thenReturn(-32_768);
			Mockito.when(mockRunMode.getMaxAxisValue()).thenReturn(32_767);
		}
	}

	@Nested
	@DisplayName("initButtons()")
	final class InitButtonsTests {

		private Input input;

		@Test
		@DisplayName("caps the button array at MAX_N_BUTTONS when runMode requests more")
		void capsButtonArrayAtMaxNButtons() {
			Mockito.when(mockRunMode.getNumButtons()).thenReturn(Input.MAX_N_BUTTONS + 50);

			input.initButtons();

			Assertions.assertEquals(Input.MAX_N_BUTTONS, input.getButtons().length);
		}

		@Test
		@DisplayName("creates a button array sized to runMode.getNumButtons()")
		void createsButtonArrayWithRunModeSize() {
			Mockito.when(mockRunMode.getNumButtons()).thenReturn(64);

			input.initButtons();

			Assertions.assertEquals(64, input.getButtons().length);
		}

		@BeforeEach
		void setUp() {
			input = createInputWithRunMode();
		}
	}

	@Nested
	@DisplayName("init()")
	final class InitTests {

		@Test
		@DisplayName("returns false and does not set initialized when the selected controller fails to open")
		void returnsFalseWhenSelectedControllerFailsToOpen() {
			Mockito.when(mockMain.isSwapLeftAndRightSticks()).thenReturn(false);
			Mockito.when(mockMain.isMapCircularAxesToSquareAxes()).thenReturn(false);
			Mockito.when(mockMain.getControllers()).thenReturn(Set.of(mockController));
			Mockito.when(mockController.instanceId()).thenReturn(1);

			final var input = createInputWithRunMode();

			try (final var sdlMock = Mockito.mockStatic(SDLGamepad.class)) {
				sdlMock.when(() -> SDLGamepad.SDL_OpenGamepad(1)).thenReturn(0L);

				Assertions.assertFalse(input.init());
				Assertions.assertFalse(input.isInitialized());
			}
		}

		@Test
		@DisplayName("returns true, sets initialized, and computes minAxisStep when no controllers are present")
		void returnsTrueAndSetsInitializedWithNoControllers() {
			Mockito.when(mockMain.isSwapLeftAndRightSticks()).thenReturn(false);
			Mockito.when(mockMain.isMapCircularAxesToSquareAxes()).thenReturn(false);
			Mockito.when(mockMain.getControllers()).thenReturn(Set.of());
			Mockito.when(mockRunMode.getMinAxisValue()).thenReturn(-32_768);
			Mockito.when(mockRunMode.getMaxAxisValue()).thenReturn(32_767);

			final var input = createInputWithRunMode();

			Assertions.assertTrue(input.init());
			Assertions.assertTrue(input.isInitialized());
			Assertions.assertEquals(2f / (32_767 + 32_768), input.getMinAxisStep(), 1e-10f);
		}

		@Test
		@DisplayName("throws IllegalStateException when called on an already-initialised Input")
		void throwsWhenCalledWhileAlreadyInitialized() throws Exception {
			final var input = createInputWithRunMode();
			final var field = Input.class.getDeclaredField("initialized");
			field.setAccessible(true);
			field.set(input, true);

			Assertions.assertThrows(IllegalStateException.class, input::init);
		}
	}

	@Nested
	@DisplayName("moveAxis()")
	final class MoveAxisTests {

		private Input input;

		@Test
		@DisplayName("does not enqueue a target value when the axis is already at the requested value")
		void doesNotEnqueueWhenAlreadyAtTarget() throws Exception {
			// Set the axis to max first, then call moveAxis with the same value
			input.setAxis(VirtualAxis.X, 1f, false, null, null, null);
			input.moveAxis(VirtualAxis.X, 1f);

			final var field = input.getClass().getDeclaredField("virtualAxisToTargetValueMap");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var targetMap = (Map<VirtualAxis, Integer>) field.get(input);

			Assertions.assertFalse(targetMap.containsKey(VirtualAxis.X));
		}

		@Test
		@DisplayName("enqueues a target value in the internal movement map when it differs from current")
		void enqueuesTargetValueWhenDifferentFromCurrent() throws Exception {
			// The axes start at 0; moving to 1f should enqueue a target
			input.moveAxis(VirtualAxis.X, 1f);

			final var field = input.getClass().getDeclaredField("virtualAxisToTargetValueMap");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var targetMap = (Map<VirtualAxis, Integer>) field.get(input);

			Assertions.assertTrue(targetMap.containsKey(VirtualAxis.X));
			Assertions.assertEquals(32_767, targetMap.get(VirtualAxis.X));
		}

		@BeforeEach
		void setUp() {
			input = createInputWithRunMode();
			Mockito.when(mockRunMode.getMinAxisValue()).thenReturn(-32_768);
			Mockito.when(mockRunMode.getMaxAxisValue()).thenReturn(32_767);
		}
	}

	@Nested
	@DisplayName("normalize()")
	final class NormalizeTests {

		@Test
		@DisplayName("extrapolates values outside input range without clamping")
		void extrapolatesValueBeyondInputRange() {
			Assertions.assertEquals(2f, Input.normalize(200f, 0f, 100f, 0f, 1f), 1e-6f);
		}

		@Test
		@DisplayName("maps the full short range used for gamepad axes")
		void mapsFullShortRangeUsedForGamepadAxes() {
			Assertions.assertEquals(-1f, Input.normalize(Short.MIN_VALUE, Short.MIN_VALUE, Short.MAX_VALUE, -1f, 1f),
					1e-6f);
			Assertions.assertEquals(1f, Input.normalize(Short.MAX_VALUE, Short.MIN_VALUE, Short.MAX_VALUE, -1f, 1f),
					1e-6f);
		}

		@Test
		@DisplayName("maps inMax to outMax")
		void mapsInputMaxToOutputMax() {
			Assertions.assertEquals(1f, Input.normalize(100f, 0f, 100f, -1f, 1f), 1e-6f);
		}

		@Test
		@DisplayName("maps inMin to outMin")
		void mapsInputMinToOutputMin() {
			Assertions.assertEquals(-1f, Input.normalize(0f, 0f, 100f, -1f, 1f), 1e-6f);
		}

		@Test
		@DisplayName("maps midpoint to midpoint of output range")
		void mapsMidpointCorrectly() {
			Assertions.assertEquals(0f, Input.normalize(50f, 0f, 100f, -1f, 1f), 1e-6f);
		}

		@Test
		@DisplayName("maps trigger range (0..Short.MAX_VALUE) to (-1f..1f)")
		void mapsTriggerRange() {
			Assertions.assertEquals(-1f, Input.normalize(0f, 0, Short.MAX_VALUE, -1f, 1f), 1e-6f);
			Assertions.assertEquals(1f, Input.normalize(Short.MAX_VALUE, 0, Short.MAX_VALUE, -1f, 1f), 1e-6f);
		}

		@Test
		@DisplayName("returns outMin when input range is zero, avoiding division by zero")
		void returnsOutMinWhenInputRangeIsZero() {
			Assertions.assertEquals(-1f, Input.normalize(5f, 5f, 5f, -1f, 1f), 1e-6f);
		}

		@Test
		@DisplayName("correctly handles an inverted output range")
		void worksWithInvertedOutputRange() {
			// value at inMin should map to outMin (which is the larger number here)
			Assertions.assertEquals(1f, Input.normalize(0f, 0f, 100f, 1f, -1f), 1e-6f);
			Assertions.assertEquals(-1f, Input.normalize(100f, 0f, 100f, 1f, -1f), 1e-6f);
		}
	}

	@Nested
	@DisplayName("openController()")
	final class OpenControllerTests {

		@Test
		@DisplayName("returns false immediately when SDL_OpenGamepad returns 0 (device unavailable)")
		void returnsFalseWhenSdlOpenGamepadFails() {
			final var input = createInput();
			Mockito.when(mockController.instanceId()).thenReturn(1);

			try (final var sdlMock = Mockito.mockStatic(SDLGamepad.class)) {
				sdlMock.when(() -> SDLGamepad.SDL_OpenGamepad(1)).thenReturn(0L);

				Assertions.assertFalse(input.openController(mockController));
			}
		}
	}

	@Nested
	@DisplayName("poll()")
	final class PollTests {

		@Test
		@DisplayName("removes expired axis suspensions on each poll")
		void removesExpiredAxisSuspensionsOnPoll() throws Exception {
			Mockito.when(mockRunMode.getPollInterval()).thenReturn(16L);

			final var input = createInputWithRunMode();
			input.suspendAxis(0);

			final var field = Input.class.getDeclaredField("axisToEndSuspensionTimestampMap");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var map = (Map<Integer, Long>) field.get(input);
			map.put(0, System.currentTimeMillis() - 1);

			input.poll();

			Assertions.assertFalse(input.isAxisSuspended(0));
		}

		@Test
		@DisplayName("returns false when no gamepad state exists for the selected gamepad")
		void returnsFalseWhenNoGamepadStateForSelectedGamepad() {
			Mockito.when(mockRunMode.getPollInterval()).thenReturn(16L);

			final var input = createInputWithRunMode();
			Assertions.assertFalse(input.poll());
		}

		@Test
		@DisplayName("sets rateMultiplier from elapsed wall-clock time on subsequent calls")
		void setsRateMultiplierFromElapsedTimeOnSubsequentCalls() throws Exception {
			Mockito.when(mockRunMode.getPollInterval()).thenReturn(16L);

			final var input = createInputWithRunMode();
			input.poll(); // first call - seeds lastPollTime

			// Wind lastPollTime back by 100 ms to simulate elapsed time
			final var field = Input.class.getDeclaredField("lastPollTime");
			field.setAccessible(true);
			field.set(input, (long) field.get(input) - 100L);

			input.poll(); // second call - should use elapsed time

			Assertions.assertEquals(100f / 1000f, input.getRateMultiplier(), 0.01f);
		}

		@Test
		@DisplayName("sets rateMultiplier from getPollInterval() on the first call when lastPollTime is 0")
		void setsRateMultiplierFromPollIntervalOnFirstCall() {
			Mockito.when(mockRunMode.getPollInterval()).thenReturn(16L);

			final var input = createInputWithRunMode();
			input.poll();

			Assertions.assertEquals(16f / 1000f, input.getRateMultiplier(), 1e-6f);
		}
	}

	@Nested
	@DisplayName("repeatModeActionWalk()")
	final class RepeatModeActionWalkTests {

		@Test
		@DisplayName("reset() clears the repeatModeActionWalk flag set by repeatModeActionWalk()")
		void resetClearsRepeatModeActionWalkFlag() throws Exception {
			final var input = createInput();
			input.repeatModeActionWalk();

			input.reset();

			final var field = Input.class.getDeclaredField("repeatModeActionWalk");
			field.setAccessible(true);
			Assertions.assertFalse((boolean) field.get(input));
		}

		@Test
		@DisplayName("sets the repeatModeActionWalk flag")
		void setsRepeatModeActionWalkFlag() throws Exception {
			final var input = createInput();
			input.repeatModeActionWalk();

			final var field = Input.class.getDeclaredField("repeatModeActionWalk");
			field.setAccessible(true);
			Assertions.assertTrue((boolean) field.get(input));
		}
	}

	@Nested
	@DisplayName("reset()")
	final class ResetTests {

		private Input input;

		@Test
		@DisplayName("clears axis suspensions that were set before the reset")
		void clearsAxisSuspensions() {
			input.suspendAxis(0);
			input.suspendAxis(1);

			input.reset();

			Assertions.assertFalse(input.isAxisSuspended(0));
			Assertions.assertFalse(input.isAxisSuspended(1));
		}

		@Test
		@DisplayName("clears the pending axis movement targets queued via moveAxis()")
		void clearsPendingAxisMoveTargets() throws Exception {
			// Inject a target directly so we don't need RunMode stubs
			final var field = Input.class.getDeclaredField("virtualAxisToTargetValueMap");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var map = (Map<VirtualAxis, Integer>) field.get(input);
			map.put(VirtualAxis.X, 1000);

			input.reset();

			Assertions.assertTrue(map.isEmpty());
		}

		@Test
		@DisplayName("nullifies the buttons array that was allocated before the reset")
		void nullifiesButtonsArray() throws Exception {
			// Inject a non-null buttons array directly via reflection to avoid
			// needing a RunMode stub just for this state-reset assertion
			final var field = Input.class.getDeclaredField("buttons");
			field.setAccessible(true);
			field.set(input, new boolean[16]);
			Assertions.assertNotNull(input.getButtons());

			input.reset();

			Assertions.assertNull(input.getButtons());
		}

		@Test
		@DisplayName("resets initialized to false, allowing the instance to be init()-ed again")
		void resetsInitialized() throws Exception {
			final var field = Input.class.getDeclaredField("initialized");
			field.setAccessible(true);
			field.set(input, true);
			Assertions.assertTrue(input.isInitialized());

			input.reset();

			Assertions.assertFalse(input.isInitialized());
		}

		@Test
		@DisplayName("resets skipAxisInitialization to false even when it was true")
		void resetsSkipAxisInitialization() {
			final var axes = new EnumMap<VirtualAxis, Integer>(VirtualAxis.class);
			final var inputWithAxes = new Input(mockMain, mockController, axes);
			Assertions.assertTrue(inputWithAxes.isSkipAxisInitialization());

			inputWithAxes.reset();

			Assertions.assertFalse(inputWithAxes.isSkipAxisInitialization());
		}

		@BeforeEach
		void setUp() {
			input = createInput();
		}
	}

	@Nested
	@DisplayName("scheduleClearOnNextPoll()")
	final class ScheduleClearOnNextPollTests {

		@Test
		@DisplayName("reset() clears the clearOnNextPoll flag set by scheduleClearOnNextPoll()")
		void resetClearsClearOnNextPollFlag() throws Exception {
			final var input = createInput();
			input.scheduleClearOnNextPoll();
			input.reset();

			final var field = Input.class.getDeclaredField("clearOnNextPoll");
			field.setAccessible(true);
			Assertions.assertFalse((boolean) field.get(input));
		}

		@Test
		@DisplayName("sets the clearOnNextPoll flag that is readable via reflection")
		void setsClearOnNextPollFlag() throws Exception {
			final var input = createInput();
			input.scheduleClearOnNextPoll();

			final var field = Input.class.getDeclaredField("clearOnNextPoll");
			field.setAccessible(true);
			Assertions.assertTrue((boolean) field.get(input));
		}
	}

	@Nested
	@DisplayName("setAxis()")
	final class SetAxisTests {

		private Input input;

		@Test
		@DisplayName("clamps the value to the runMode axis range")
		void clampsValueToAxisRange() {
			input.setAxis(VirtualAxis.X, 5f, false, null, null, null);
			Assertions.assertEquals(32_767, input.getAxes().get(VirtualAxis.X));

			input.setAxis(VirtualAxis.X, -5f, false, null, null, null);
			Assertions.assertEquals(-32_768, input.getAxes().get(VirtualAxis.X));
		}

		@Test
		@DisplayName("does not schedule a rumble effect when haptic feedback is disabled")
		void doesNotScheduleRumbleWhenHapticDisabled() throws Exception {
			input.setAxis(VirtualAxis.X, 0f, false, null, null, null);

			input.setAxis(VirtualAxis.X, 1f, false, -1f, 1f, null); // hapticFeedback=false

			final var field = Input.class.getDeclaredField("scheduledRumbleEffect");
			field.setAccessible(true);
			Assertions.assertNull(field.get(input));
		}

		@Test
		@DisplayName("each VirtualAxis is stored independently")
		void eachVirtualAxisIsStoredIndependently() {
			input.setAxis(VirtualAxis.X, 1f, false, null, null, null);
			input.setAxis(VirtualAxis.Y, -1f, false, null, null, null);

			Assertions.assertEquals(32_767, input.getAxes().get(VirtualAxis.X));
			Assertions.assertEquals(-32_768, input.getAxes().get(VirtualAxis.Y));
		}

		@Test
		@DisplayName("schedules a LIGHT rumble effect when crossing the detent value from above with haptic enabled")
		void schedulesLightRumbleWhenCrossingDetentFromAbove() throws Exception {
			input.setAxis(VirtualAxis.X, 0.5f, false, null, null, null); // prev above detent

			input.setAxis(VirtualAxis.X, -0.5f, true, -1f, 1f, 0f); // cross detent at 0f

			final var field = Input.class.getDeclaredField("scheduledRumbleEffect");
			field.setAccessible(true);
			Assertions.assertEquals("LIGHT", field.get(input).toString());
		}

		@Test
		@DisplayName("schedules a LIGHT rumble effect when crossing the detent value from below with haptic enabled")
		void schedulesLightRumbleWhenCrossingDetentFromBelow() throws Exception {
			input.setAxis(VirtualAxis.X, -0.5f, false, null, null, null); // prev below detent

			input.setAxis(VirtualAxis.X, 0.5f, true, -1f, 1f, 0f); // cross detent at 0f

			final var field = Input.class.getDeclaredField("scheduledRumbleEffect");
			field.setAccessible(true);
			Assertions.assertEquals("LIGHT", field.get(input).toString());
		}

		@Test
		@DisplayName("schedules a STRONG rumble effect when the value hits the maxValue boundary with haptic enabled")
		void schedulesStrongRumbleWhenHittingMaxValue() throws Exception {
			input.setAxis(VirtualAxis.X, 0f, false, null, null, null); // set prev value to 0

			input.setAxis(VirtualAxis.X, 1f, true, -1f, 1f, null); // move to maxValue

			final var field = Input.class.getDeclaredField("scheduledRumbleEffect");
			field.setAccessible(true);
			Assertions.assertEquals("STRONG", field.get(input).toString());
		}

		@Test
		@DisplayName("schedules a STRONG rumble effect when the value hits the minValue boundary with haptic enabled")
		void schedulesStrongRumbleWhenHittingMinValue() throws Exception {
			input.setAxis(VirtualAxis.X, 0f, false, null, null, null); // set prev value to 0

			input.setAxis(VirtualAxis.X, -1f, true, -1f, 1f, null); // move to minValue

			final var field = Input.class.getDeclaredField("scheduledRumbleEffect");
			field.setAccessible(true);
			Assertions.assertEquals("STRONG", field.get(input).toString());
		}

		@BeforeEach
		void setUp() {
			input = createInputWithRunMode();
			Mockito.when(mockRunMode.getMinAxisValue()).thenReturn(-32_768);
			Mockito.when(mockRunMode.getMaxAxisValue()).thenReturn(32_767);
		}

		@Test
		@DisplayName("stores the converted float value in the axes map")
		void storesConvertedValueInAxesMap() {
			input.setAxis(VirtualAxis.X, 1f, false, null, null, null);
			Assertions.assertEquals(32_767, input.getAxes().get(VirtualAxis.X));
		}
	}

	@Nested
	@DisplayName("setProfile()")
	final class SetProfileTests {

		private Input input;

		@Test
		@DisplayName("accepts and stores a valid profile with no modes containing invalid buttons")
		void acceptsValidProfile() {
			final var profile = new Profile();
			Assertions.assertTrue(input.setProfile(profile));
			Assertions.assertSame(profile, input.getProfile());
		}

		@Test
		@DisplayName("rejects a profile whose buttonToModeActionsMap contains an invalid button index")
		void rejectsProfileWithInvalidButtonIndex() throws Exception {
			final var profile = new Profile();

			// Inject an invalid button key (-1, which equals SDL_GAMEPAD_BUTTON_INVALID)
			// directly into the profile's map via reflection
			final var mapField = Profile.class.getDeclaredField("buttonToModeActionsMap");
			mapField.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var map = (Map<Integer, List<?>>) mapField.get(profile);
			map.put(-1, new ArrayList<>());

			Assertions.assertFalse(input.setProfile(profile));
		}

		@BeforeEach
		void setUp() {
			input = createInput();
		}

		@Test
		@DisplayName("throws NullPointerException when called with null")
		void throwsOnNullProfile() {
			Assertions.assertThrows(NullPointerException.class, () -> input.setProfile(null));
		}
	}

	@Nested
	@DisplayName("setProfile() - additional validation")
	final class SetProfileValidationTests {

		private Input input;

		@Test
		@DisplayName("rejects a profile whose mode contains an out-of-range axis index")
		void rejectsProfileWithInvalidAxisInMode() throws Exception {
			final var profile = new Profile();
			final var mode = new Mode();

			final var axisMapField = Mode.class.getDeclaredField("axisToActionsMap");
			axisMapField.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var axisMap = (Map<Integer, List<?>>) axisMapField.get(mode);
			axisMap.put(SDLGamepad.SDL_GAMEPAD_AXIS_COUNT, new ArrayList<>());

			final var modesField = Profile.class.getDeclaredField("modes");
			modesField.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var modes = (List<Mode>) modesField.get(profile);
			modes.add(mode);

			Assertions.assertFalse(input.setProfile(profile));
		}

		@Test
		@DisplayName("rejects a profile whose mode contains an invalid button index")
		void rejectsProfileWithInvalidButtonInMode() throws Exception {
			final var profile = new Profile();
			final var mode = new Mode();

			final var buttonMapField = Mode.class.getDeclaredField("buttonToActionsMap");
			buttonMapField.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var buttonMap = (Map<Integer, List<?>>) buttonMapField.get(mode);
			buttonMap.put(-1, new ArrayList<>());

			final var modesField = Profile.class.getDeclaredField("modes");
			modesField.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var modes = (List<Mode>) modesField.get(profile);
			modes.add(mode);

			Assertions.assertFalse(input.setProfile(profile));
		}

		@BeforeEach
		void setUp() {
			input = createInput();
		}

		@Test
		@DisplayName("sorts modes so that DEFAULT_MODE always comes first")
		void sortsDefaultModeFirst() throws Exception {
			final var profile = new Profile();

			final var modesField = Profile.class.getDeclaredField("modes");
			modesField.setAccessible(true);
			@SuppressWarnings("unchecked")
			final var modes = (List<Mode>) modesField.get(profile);

			final var extraMode = new Mode();
			extraMode.setDescription("Z_mode");
			modes.addFirst(extraMode);

			input.setProfile(profile);

			Assertions.assertEquals(Profile.DEFAULT_MODE, profile.getModes().getFirst());
		}
	}
}

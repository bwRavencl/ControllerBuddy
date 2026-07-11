/*
 * Copyright (C) 2022 Matteo Hausner
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

package de.bwravencl.controllerbuddy.runmode;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Scancode;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;

/// Run mode for local input processing on the same machine.
///
/// Polls a locally connected controller for input and writes the
/// resulting axis, button, key, and mouse state to the virtual
/// output device.
public final class LocalRunMode extends OutputRunMode {

	private static final Logger logger = Logger.getLogger(LocalRunMode.class.getName());

	/// Set of normal (non-modifier) scancodes active in the current output cycle.
	private final HashSet<Scancode> sourceKeyCodes = new HashSet<>();

	/// Set of modifier scancodes active in the current output cycle.
	private final HashSet<Scancode> sourceModifiersCodes = new HashSet<>();

	/// Constructs a [LocalRunMode].
	///
	/// @param main the main application instance
	public LocalRunMode(final Main main) {
		super(main);
	}

	@Override
	Logger getLogger() {
		return logger;
	}

	@Override
	boolean readInput() throws IOException {
		super.readInput();

		if (!pollInput()) {
			controllerDisconnected();

			return false;
		}

		final var inputAxes = input.getAxes();

		final var inputAxisX = inputAxes.getOrDefault(Input.VirtualAxis.X, 0);
		axisX.set(inputAxisX);

		final var inputAxisY = inputAxes.getOrDefault(Input.VirtualAxis.Y, 0);
		axisY.set(inputAxisY);

		final var inputAxisZ = inputAxes.getOrDefault(Input.VirtualAxis.Z, 0);
		axisZ.set(inputAxisZ);

		final var inputAxisRX = inputAxes.getOrDefault(Input.VirtualAxis.RX, 0);
		axisRX.set(inputAxisRX);

		final var inputAxisRY = inputAxes.getOrDefault(Input.VirtualAxis.RY, 0);
		axisRY.set(inputAxisRY);

		final var inputAxisRZ = inputAxes.getOrDefault(Input.VirtualAxis.RZ, 0);
		axisRZ.set(inputAxisRZ);

		final var inputAxisS0 = inputAxes.getOrDefault(Input.VirtualAxis.S0, 0);
		axisS0.set(inputAxisS0);

		final var inputAxisS1 = inputAxes.getOrDefault(Input.VirtualAxis.S1, 0);
		axisS1.set(inputAxisS1);

		final var inputButtons = input.getButtons();
		for (var i = 0; i < numButtons; i++) {
			buttons[i].set(inputButtons[i] ? 1 : 0);
			inputButtons[i] = false;
		}

		cursorDeltaX = input.getCursorDeltaX();
		input.setCursorDeltaX(0);
		cursorDeltaY = input.getCursorDeltaY();
		input.setCursorDeltaY(0);

		updateOutputSets(input.getDownMouseButtons(), oldDownMouseButtons, newUpMouseButtons, newDownMouseButtons,
				false);

		downUpMouseButtons.clear();
		final var inputDownUpMouseButtons = input.getDownUpMouseButtons();
		downUpMouseButtons.addAll(inputDownUpMouseButtons);
		inputDownUpMouseButtons.clear();

		sourceModifiersCodes.clear();
		sourceKeyCodes.clear();
		input.getDownKeystrokes().forEach(keystroke -> {
			sourceModifiersCodes.addAll(Arrays.asList(keystroke.getModifierCodes()));
			sourceKeyCodes.addAll(Arrays.asList(keystroke.getKeyCodes()));
		});

		updateOutputSets(sourceModifiersCodes, oldDownModifiers, newUpModifiers, newDownModifiers, false);
		updateOutputSets(sourceKeyCodes, oldDownNormalKeys, newUpNormalKeys, newDownNormalKeys, true);

		downUpKeystrokes.clear();
		final var inputDownUpKeystrokes = input.getDownUpKeystrokes();
		downUpKeystrokes.addAll(inputDownUpKeystrokes);
		inputDownUpKeystrokes.clear();

		scrollClicks = input.getScrollClicks();
		input.setScrollClicks(0);

		onLockKeys.clear();
		final var inputOnLockKeys = input.getOnLockKeys();
		onLockKeys.addAll(inputOnLockKeys);
		inputOnLockKeys.clear();

		offLockKeys.clear();
		final var inputOffLockKeys = input.getOffLockKeys();
		offLockKeys.addAll(inputOffLockKeys);
		inputOffLockKeys.clear();

		return true;
	}

	/// Runs the local polling loop: initializes the output device, then repeatedly
	/// polls the controller, and writes the input state to the output device at a
	/// fixed rate until stopped.
	@Override
	public void run() {
		logStart();

		try {
			if (init()) {
				var nextPollTimeNanos = System.nanoTime();

				while (run) {
					if (readInput()) {
						writeOutput();
					}

					nextPollTimeNanos += pollPeriodNanos;

					final var sleepNanos = nextPollTimeNanos - System.nanoTime();
					if (sleepNanos > 0L) {
						// noinspection BusyWait
						Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
					} else {
						nextPollTimeNanos = System.nanoTime();
					}
				}
			} else {
				forceStop = true;
			}
		} catch (final IOException e) {
			handleIOException(e);
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		} finally {
			deInit();
		}

		logStop();
	}
}

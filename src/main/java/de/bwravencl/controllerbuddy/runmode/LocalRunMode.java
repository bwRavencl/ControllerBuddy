/* Copyright (C) 2022  Matteo Hausner
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

package de.bwravencl.controllerbuddy.runmode;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.ScanCode;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;

public final class LocalRunMode extends OutputRunMode {

	private static final Logger log = Logger.getLogger(LocalRunMode.class.getName());

	private final HashSet<ScanCode> sourceKeyCodes = new HashSet<>();

	private final HashSet<ScanCode> sourceModifiersCodes = new HashSet<>();

	public LocalRunMode(final Main main, final Input input) {
		super(main, input);
	}

	@Override
	Logger getLogger() {
		return log;
	}

	@Override
	boolean readInput() throws IOException {
		super.readInput();

		if (!input.poll()) {
			controllerDisconnected();

			return false;
		}

		final var inputAxes = input.getAxes();

		final var inputAxisX = inputAxes.get(Input.VirtualAxis.X);
		axisX.setValue(inputAxisX);

		final var inputAxisY = inputAxes.get(Input.VirtualAxis.Y);
		axisY.setValue(inputAxisY);

		final var inputAxisZ = inputAxes.get(Input.VirtualAxis.Z);
		axisZ.setValue(inputAxisZ);

		final var inputAxisRX = inputAxes.get(Input.VirtualAxis.RX);
		axisRX.setValue(inputAxisRX);

		final var inputAxisRY = inputAxes.get(Input.VirtualAxis.RY);
		axisRY.setValue(inputAxisRY);

		final var inputAxisRZ = inputAxes.get(Input.VirtualAxis.RZ);
		axisRZ.setValue(inputAxisRZ);

		final var inputAxisS0 = inputAxes.get(Input.VirtualAxis.S0);
		axisS0.setValue(inputAxisS0);

		final var inputAxisS1 = inputAxes.get(Input.VirtualAxis.S1);
		axisS1.setValue(inputAxisS1);

		final var inputButtons = input.getButtons();
		for (var i = 0; i < nButtons; i++) {
			buttons[i].setValue(inputButtons[i] ? 1 : 0);
		}

		cursorDeltaX = input.getCursorDeltaX();
		input.setCursorDeltaX(0);
		cursorDeltaY = input.getCursorDeltaY();
		input.setCursorDeltaY(0);

		final var inputDownMouseButtons = input.getDownMouseButtons();
		synchronized (inputDownMouseButtons) {
			updateOutputSets(inputDownMouseButtons, oldDownMouseButtons, newUpMouseButtons, newDownMouseButtons, false);
		}

		downUpMouseButtons.clear();
		final var inputDownUpMouseButtons = input.getDownUpMouseButtons();
		downUpMouseButtons.addAll(inputDownUpMouseButtons);
		inputDownUpMouseButtons.clear();

		sourceModifiersCodes.clear();
		sourceKeyCodes.clear();
		input.getDownKeyStrokes().forEach(keyStroke -> {
			sourceModifiersCodes.addAll(Arrays.asList(keyStroke.getModifierCodes()));
			sourceKeyCodes.addAll(Arrays.asList(keyStroke.getKeyCodes()));
		});

		updateOutputSets(sourceModifiersCodes, oldDownModifiers, newUpModifiers, newDownModifiers, false);
		updateOutputSets(sourceKeyCodes, oldDownNormalKeys, newUpNormalKeys, newDownNormalKeys, true);

		downUpKeyStrokes.clear();
		final var inputDownUpKeyStrokes = input.getDownUpKeyStrokes();
		downUpKeyStrokes.addAll(inputDownUpKeyStrokes);
		inputDownUpKeyStrokes.clear();

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

	@Override
	public void run() {
		logStart();

		try {
			if (init()) {
				while (!Thread.currentThread().isInterrupted()) {
					if (readInput()) {
						writeOutput();
					}
					// noinspection BusyWait
					Thread.sleep(pollInterval);
				}
			} else {
				forceStop = true;
			}
		} catch (final IOException e) {
			handleIOException(e);
		} catch (final InterruptedException _) {
			// expected whenever the run mode gets stopped
		} finally {
			deInit();
		}

		logStop();
	}
}

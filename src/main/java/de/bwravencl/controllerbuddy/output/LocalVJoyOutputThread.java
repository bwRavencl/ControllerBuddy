/* Copyright (C) 2019  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.controllerbuddy.output;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;

public final class LocalVJoyOutputThread extends VJoyOutputThread {

	private static final Logger log = Logger.getLogger(LocalVJoyOutputThread.class.getName());

	public LocalVJoyOutputThread(final Main main, final Input input) {
		super(main, input);
	}

	@Override
	Logger getLogger() {
		return log;
	}

	@Override
	boolean readInput() {
		if (!input.poll()) {
			controllerDisconnected();

			return false;
		}

		final var inputAxes = input.getAxes();

		final var inputAxisX = inputAxes.get(Input.VirtualAxis.X);
		if (axisX.intValue() != inputAxisX) {
			axisX.setValue(inputAxisX);
			axisXChanged = true;
		}

		final var inputAxisY = inputAxes.get(Input.VirtualAxis.Y);
		if (axisY.intValue() != inputAxisY) {
			axisY.setValue(inputAxisY);
			axisYChanged = true;
		}

		final var inputAxisZ = inputAxes.get(Input.VirtualAxis.Z);
		if (axisZ.intValue() != inputAxisZ) {
			axisZ.setValue(inputAxisZ);
			axisZChanged = true;
		}

		final var inputAxisRX = inputAxes.get(Input.VirtualAxis.RX);
		if (axisRX.intValue() != inputAxisRX) {
			axisRX.setValue(inputAxisRX);
			axisRXChanged = true;
		}

		final var inputAxisRY = inputAxes.get(Input.VirtualAxis.RY);
		if (axisRY.intValue() != inputAxisRY) {
			axisRY.setValue(inputAxisRY);
			axisRYChanged = true;
		}

		final var inputAxisRZ = inputAxes.get(Input.VirtualAxis.RZ);
		if (axisRZ.intValue() != inputAxisRZ) {
			axisRZ.setValue(inputAxisRZ);
			axisRZChanged = true;
		}

		final var inputAxisS0 = inputAxes.get(Input.VirtualAxis.S0);
		if (axisS0.intValue() != inputAxisS0) {
			axisS0.setValue(inputAxisS0);
			axisS0Changed = true;
		}

		final var inputAxisS1 = inputAxes.get(Input.VirtualAxis.S1);
		if (axisS1.intValue() != inputAxisS1) {
			axisS1.setValue(inputAxisS1);
			axisS1Changed = true;
		}

		final var inputButtons = input.getButtons();
		for (var i = 0; i < nButtons; i++) {
			if (buttons[i].booleanValue() != inputButtons[i]) {
				buttons[i].setValue(inputButtons[i] ? 1L : 0L);
				buttonsChanged[i] = true;
			}

			inputButtons[i] = false;
		}

		cursorDeltaX = input.getCursorDeltaX();
		input.setCursorDeltaX(0);
		cursorDeltaY = input.getCursorDeltaY();
		input.setCursorDeltaY(0);

		final var downMouseButtons = input.getDownMouseButtons();
		synchronized (downMouseButtons) {
			updateOutputSets(downMouseButtons, oldDownMouseButtons, newUpMouseButtons, newDownMouseButtons, false);
		}

		downUpMouseButtons.clear();
		downUpMouseButtons.addAll(input.getDownUpMouseButtons());
		input.getDownUpMouseButtons().clear();

		final var sourceModifiers = new HashSet<Integer>();
		final var sourceNormalKeys = new HashSet<Integer>();
		for (final var keyStroke : input.getDownKeyStrokes()) {
			sourceModifiers.addAll(Arrays.asList(keyStroke.getModifierCodes()));
			sourceNormalKeys.addAll(Arrays.asList(keyStroke.getKeyCodes()));
		}
		updateOutputSets(sourceModifiers, oldDownModifiers, newUpModifiers, newDownModifiers, false);
		updateOutputSets(sourceNormalKeys, oldDownNormalKeys, newUpNormalKeys, newDownNormalKeys, true);

		downUpKeyStrokes.clear();
		downUpKeyStrokes.addAll(input.getDownUpKeyStrokes());
		input.getDownUpKeyStrokes().clear();

		scrollClicks = input.getScrollClicks();
		input.setScrollClicks(0);

		onLockKeys.clear();
		onLockKeys.addAll(input.getOnLockKeys());
		input.getOnLockKeys().clear();

		offLockKeys.clear();
		offLockKeys.addAll(input.getOffLockKeys());
		input.getOffLockKeys().clear();

		return true;
	}

	@Override
	public void run() {
		logStart();

		try {
			if (init())
				while (!Thread.currentThread().isInterrupted()) {
					if (readInput())
						writeOutput();
					Thread.sleep(pollInterval);
				}
			else
				forceStop = true;
		} catch (final InterruptedException e) {
		} finally {
			deInit();
		}

		logStop();
	}
}

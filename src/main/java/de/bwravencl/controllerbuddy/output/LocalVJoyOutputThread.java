/* Copyright (C) 2016  Matteo Hausner
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
import java.util.Set;

import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.LONG;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;

public class LocalVJoyOutputThread extends VJoyOutputThread {

	public LocalVJoyOutputThread(final Main main, final Input input) {
		super(main, input);
	}

	@Override
	protected void deInit() {
		super.deInit();
		main.stopLocal(false);

		if (restart)
			main.restartLast();
	}

	@Override
	protected boolean readInput() {
		if (!input.poll()) {
			controllerDisconnected();

			return false;
		}

		axisX = new LONG(Input.getAxis().get(Input.VirtualAxis.X));
		axisY = new LONG(Input.getAxis().get(Input.VirtualAxis.Y));
		axisZ = new LONG(Input.getAxis().get(Input.VirtualAxis.Z));
		axisRX = new LONG(Input.getAxis().get(Input.VirtualAxis.RX));
		axisRY = new LONG(Input.getAxis().get(Input.VirtualAxis.RY));
		axisRZ = new LONG(Input.getAxis().get(Input.VirtualAxis.RZ));
		axisS0 = new LONG(Input.getAxis().get(Input.VirtualAxis.S0));
		axisS1 = new LONG(Input.getAxis().get(Input.VirtualAxis.S1));

		buttons = new BOOL[nButtons];
		for (int i = 0; i < nButtons; i++) {
			buttons[i] = new BOOL(input.getButtons()[i] ? 1L : 0L);
			input.getButtons()[i] = false;
		}

		cursorDeltaX = input.getCursorDeltaX();
		input.setCursorDeltaX(0);
		cursorDeltaY = input.getCursorDeltaY();
		input.setCursorDeltaY(0);

		updateOutputSets(input.getDownMouseButtons(), oldDownMouseButtons, newUpMouseButtons, newDownMouseButtons,
				false);

		downUpMouseButtons.clear();
		downUpMouseButtons.addAll(input.getDownUpMouseButtons());
		input.getDownUpMouseButtons().clear();

		final Set<Integer> sourceModifiers = new HashSet<>();
		final Set<Integer> sourceNormalKeys = new HashSet<>();
		for (final KeyStroke ks : input.getDownKeyStrokes()) {
			sourceModifiers.addAll(Arrays.asList(ks.getModifierCodes()));
			sourceNormalKeys.addAll(Arrays.asList(ks.getKeyCodes()));
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
		if (init()) {
			while (run) {
				if (readInput())
					writeOutput();

				try {
					Thread.sleep(pollInterval);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		deInit();
	}

}

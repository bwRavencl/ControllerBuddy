/* Copyright (C) 2015  Matteo Hausner
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

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.HashSet;

import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.LONG;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;

public class LocalVJoyOutputThread extends VJoyOutputThread {

	public LocalVJoyOutputThread(Main main, Input input) {
		super(main, input);
	}

	@Override
	public void run() {
		if (init()) {
			while (run) {
				if (readInput())
					writeOutput();

				try {
					Thread.sleep(updateRate);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		deInit();
	}

	@Override
	protected boolean readInput() {
		input.poll();

		axisX = new LONG(input.getAxis().get(Input.VirtualAxis.X));
		axisY = new LONG(input.getAxis().get(Input.VirtualAxis.Y));
		axisZ = new LONG(input.getAxis().get(Input.VirtualAxis.Z));
		axisRX = new LONG(input.getAxis().get(Input.VirtualAxis.RX));
		axisRY = new LONG(input.getAxis().get(Input.VirtualAxis.RY));
		axisRZ = new LONG(input.getAxis().get(Input.VirtualAxis.RZ));
		axisS0 = new LONG(input.getAxis().get(Input.VirtualAxis.S0));
		axisS1 = new LONG(input.getAxis().get(Input.VirtualAxis.S1));

		buttons = new BOOL[nButtons];
		for (int i = 0; i < nButtons; i++) {
			buttons[i] = new BOOL(input.getButtons()[i] ? 1L : 0L);
			input.getButtons()[i] = false;
		}

		final Point currentPosition = MouseInfo.getPointerInfo().getLocation();
		cursorX = currentPosition.x + input.getCursorDeltaX();
		input.setCursorDeltaX(0);
		cursorY = currentPosition.y + input.getCursorDeltaY();
		input.setCursorDeltaY(0);

		newDownMouseButtons = new HashSet<Integer>(input.getDownMouseButtons());
		input.getDownMouseButtons().clear();
		oldDownMouseButtons.removeAll(newDownMouseButtons);
		newUpMouseButtons = new HashSet<Integer>(oldDownMouseButtons);
		oldDownMouseButtons.clear();
		oldDownMouseButtons.addAll(newDownMouseButtons);

		downUpMouseButtons = new HashSet<Integer>(input.getDownUpMouseButtons());
		input.getDownUpMouseButtons().clear();

		newDownKeyCodes = new HashSet<Integer>(input.getDownKeyCodes());
		input.getDownKeyCodes().clear();
		oldDownKeyCodes.removeAll(newDownKeyCodes);
		newUpKeyCodes = new HashSet<Integer>(oldDownKeyCodes);
		oldDownKeyCodes.clear();
		oldDownKeyCodes.addAll(newDownKeyCodes);

		downUpKeyStrokes = new HashSet<KeyStroke>(input.getDownUpKeyStrokes());
		input.getDownUpKeyStrokes().clear();

		scrollClicks = input.getScrollClicks();
		input.setScrollClicks(0);

		return true;
	}

	@Override
	protected void deInit() {
		super.deInit();
		main.stopLocal();
	}

}

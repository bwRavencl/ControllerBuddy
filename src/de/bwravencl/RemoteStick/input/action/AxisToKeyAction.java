/* Copyright (C) 2014  Matteo Hausner
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

package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class AxisToKeyAction extends ToKeyAction {

	public static final float DEFAULT_MIN_AXIS_VALUE = 0.5f;
	public static final float DEFAULT_MAX_AXIS_VALUE = 1.0f;

	private float minAxisValue = DEFAULT_MIN_AXIS_VALUE;
	private float maxAxisValue = DEFAULT_MAX_AXIS_VALUE;

	public float getMinAxisValue() {
		return minAxisValue;
	}

	public void setMinAxisValue(Float minAxisValue) {
		this.minAxisValue = minAxisValue;
	}

	public float getMaxAxisValue() {
		return maxAxisValue;
	}

	public void setMaxAxisValue(Float maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	@Override
	public void doAction(Input input, float value) {
		if ((value >= minAxisValue && value <= maxAxisValue) && !invert) {
			if (downUp) {
				if (wasUp) {
					input.getDownUpKeyStrokes().add(keystroke);
					wasUp = false;
				}
			} else {
				for (String s : keystroke.getModifierCodes())
					input.getDownKeyCodes().add(s);
				for (String s : keystroke.getKeyCodes())
					input.getDownKeyCodes().add(s);
			}
		} else {
			if (downUp)
				wasUp = true;
			else {
				for (String s : keystroke.getModifierCodes())
					input.getDownKeyCodes().remove(s);
				for (String s : keystroke.getKeyCodes())
					input.getDownKeyCodes().remove(s);
			}
		}
	}
	
}

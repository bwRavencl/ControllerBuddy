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

import java.util.Locale;
import java.util.ResourceBundle;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import net.brockmatt.util.ResourceBundleUtil;

public abstract class OutputThread extends Thread {

	public static final long DEFAULT_UPDATE_RATE = 10L;

	protected final Main main;
	protected final Input input;

	protected long updateRate = DEFAULT_UPDATE_RATE;
	protected int minAxisValue;
	protected int maxAxisValue;
	protected int nButtons;

	protected final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(Main.STRING_RESOURCE_BUNDLE_BASENAME,
			Locale.getDefault());

	public OutputThread(Main main, Input input) {
		this.main = main;
		this.input = input;
		input.setOutputThread(this);
	}

	public long getUpdateRate() {
		return updateRate;
	}

	public void setUpdateRate(long updateRate) {
		this.updateRate = updateRate;
	}

	public int getMinAxisValue() {
		return minAxisValue;
	}

	public void setMinAxisValue(int minAxisValue) {
		this.minAxisValue = minAxisValue;
	}

	public int getMaxAxisValue() {
		return maxAxisValue;
	}

	public void setMaxAxisValue(int maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	public int getnButtons() {
		return nButtons;
	}

	public void setnButtons(int nButtons) {
		this.nButtons = nButtons;
		input.setnButtons(nButtons);
	}

	public void stopOutput() {
		input.reset();
	}

}

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

import java.awt.EventQueue;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import net.brockmatt.util.ResourceBundleUtil;
import net.java.games.input.Controller;

public abstract class OutputThread extends Thread {

	public static final int DEFAULT_POLL_INTERVAL = 10;

	protected final Main main;
	protected final Input input;
	protected boolean run = true;
	protected long pollInterval = DEFAULT_POLL_INTERVAL;
	protected int minAxisValue;
	protected int maxAxisValue;
	protected int nButtons;
	protected final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(Main.STRING_RESOURCE_BUNDLE_BASENAME,
			Locale.getDefault());

	public OutputThread(final Main main, final Input input) {
		this.main = main;
		this.input = input;
		input.setOutputThread(this);
	}

	protected void controllerDisconnected() {
		JOptionPane.showMessageDialog(main.getFrame(),
				rb.getString("CONTROLLER_DISCONNECTED_DIALOG_TEXT_PREFIX") + input.getController().getName()
						+ rb.getString("CONTROLLER_DISCONNECTED_DIALOG_TEXT_SUFFIX"),
				rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);

		input.getController();

		for (final Controller c : Input.getControllers()) {
			if (c.poll()) {
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						main.setSelectedController(c);
					}
				});
				break;
			}
		}
	}

	public int getMaxAxisValue() {
		return maxAxisValue;
	}

	public int getMinAxisValue() {
		return minAxisValue;
	}

	public int getnButtons() {
		return nButtons;
	}

	public long getPollInterval() {
		return pollInterval;
	}

	public void setMaxAxisValue(final int maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	public void setMinAxisValue(final int minAxisValue) {
		this.minAxisValue = minAxisValue;
	}

	public void setnButtons(final int nButtons) {
		this.nButtons = nButtons;
		input.setnButtons(nButtons);
	}

	public void setPollInterval(final long pollInterval) {
		this.pollInterval = pollInterval;
	}

	public void stopOutput() {
		run = false;
		input.reset();
	}

}

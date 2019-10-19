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

import static de.bwravencl.controllerbuddy.gui.Main.STRING_RESOURCE_BUNDLE_BASENAME;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.util.ResourceBundleUtil;

public abstract class OutputThread extends Thread {

	private static final Logger log = Logger.getLogger(OutputThread.class.getName());

	public static final int DEFAULT_POLL_INTERVAL = 2;

	final Main main;
	final Input input;
	long pollInterval = DEFAULT_POLL_INTERVAL;
	int minAxisValue;
	int maxAxisValue;

	int nButtons;

	final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(STRING_RESOURCE_BUNDLE_BASENAME,
			Locale.getDefault());

	OutputThread(final Main main, final Input input) {
		this.main = main;
		this.input = input;
		input.setOutputThread(this);
	}

	final void controllerDisconnected() throws InterruptedException {
		new Thread() {

			@Override
			public void run() {
				main.stopAll();
			}
		}.start();

		log.log(Level.WARNING, "Could not read from controller");
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(main.getFrame(), rb.getString("CONTROLLER_DISCONNECTED_DIALOG_TEXT"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		});
	}

	abstract Logger getLogger();

	public final int getMaxAxisValue() {
		return maxAxisValue;
	}

	public final int getMinAxisValue() {
		return minAxisValue;
	}

	public final int getnButtons() {
		return nButtons;
	}

	public final long getPollInterval() {
		return pollInterval;
	}

	final void logStart() {
		getLogger().log(Level.INFO, "Starting output");
	}

	final void logStop() {
		getLogger().log(Level.INFO, "Stopped output");
	}

	void setnButtons(final int nButtons) {
		this.nButtons = nButtons;
		input.setnButtons(nButtons);
	}

	public final void setPollInterval(final long pollInterval) {
		this.pollInterval = pollInterval;
	}

	public void stopOutput() {
		input.reset();
		interrupt();
	}

}

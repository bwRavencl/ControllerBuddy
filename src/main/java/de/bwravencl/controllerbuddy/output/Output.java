/* Copyright (C) 2020  Matteo Hausner
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

import static de.bwravencl.controllerbuddy.gui.GuiUtils.showMessageDialog;
import static de.bwravencl.controllerbuddy.gui.Main.strings;
import static java.awt.EventQueue.invokeLater;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static javax.swing.JOptionPane.ERROR_MESSAGE;

import java.util.logging.Logger;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;

public abstract class Output implements Runnable {

	private static final Logger log = Logger.getLogger(Output.class.getName());

	public static final int DEFAULT_POLL_INTERVAL = 1;

	final Main main;
	final Input input;
	long pollInterval = DEFAULT_POLL_INTERVAL;
	int minAxisValue;
	int maxAxisValue;
	int nButtons;

	Output(final Main main, final Input input) {
		this.main = main;
		this.input = input;
		input.setOutput(this);
	}

	final void controllerDisconnected() {
		new Thread() {

			@Override
			public void run() {
				main.stopAll(true);
			}
		}.start();

		log.log(WARNING, "Could not read from controller");
		invokeLater(() -> {
			showMessageDialog(main.getFrame(), strings.getString("CONTROLLER_DISCONNECTED_DIALOG_TEXT"),
					strings.getString("ERROR_DIALOG_TITLE"), ERROR_MESSAGE);
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
		getLogger().log(INFO, "Starting output");
	}

	final void logStop() {
		getLogger().log(INFO, "Stopped output");
	}

	void setnButtons(final int nButtons) {
		this.nButtons = nButtons;
		input.setnButtons(nButtons);
	}

	public final void setPollInterval(final long pollInterval) {
		this.pollInterval = pollInterval;
	}
}

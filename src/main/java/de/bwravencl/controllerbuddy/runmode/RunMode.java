/* Copyright (C) 2020  Matteo Hausner
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

import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;

public abstract class RunMode implements Runnable {

	private static final Logger log = Logger.getLogger(RunMode.class.getName());

	public static final int DEFAULT_POLL_INTERVAL = 1;

	final Main main;
	final Input input;
	long pollInterval = DEFAULT_POLL_INTERVAL;
	int minAxisValue;
	int maxAxisValue;
	int nButtons;

	RunMode(final Main main, final Input input) {
		this.main = main;
		this.input = input;
		input.setRunMode(this);
	}

	final void controllerDisconnected() {
		new Thread() {

			@Override
			public void run() {
				main.stopAll(true, false, true);
			}
		}.start();

		log.log(Level.WARNING, "Could not read from controller");
		EventQueue.invokeLater(() -> {
			GuiUtils.showMessageDialog(main.getFrame(), Main.strings.getString("CONTROLLER_DISCONNECTED_DIALOG_TEXT"),
					Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
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
}

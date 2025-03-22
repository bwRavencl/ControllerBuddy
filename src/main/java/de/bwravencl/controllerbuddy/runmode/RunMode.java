/* Copyright (C) 2015  Matteo Hausner
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

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public abstract class RunMode implements Runnable {

	public static final int DEFAULT_POLL_INTERVAL = 1;

	private static final Logger LOGGER = Logger.getLogger(RunMode.class.getName());

	final Input input;

	final Main main;

	int maxAxisValue;

	int minAxisValue;

	int numButtons;

	long pollInterval;

	private boolean stopping;

	RunMode(final Main main, final Input input) {
		this.main = main;
		this.input = input;
		pollInterval = main.getPollInterval();

		input.setRunMode(this);
	}

	final void controllerDisconnected() {
		if (stopping) {
			return;
		}

		Thread.startVirtualThread(() -> main.stopAll(true, !main.isAutoRestartOutput(), true));

		final var controller = input.getSelectedController();
		if (controller != null) {
			LOGGER.log(Level.WARNING,
					Main.assembleControllerLoggingMessage("Could not read from controller ", controller));
		}

		if (!main.isSkipControllerDialogs()) {
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
					Main.STRINGS.getString("COULD_NOT_READ_FROM_CONTROLLER_DIALOG_TEXT"),
					Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		}

		stopping = true;
	}

	abstract Logger getLogger();

	public final int getMaxAxisValue() {
		return maxAxisValue;
	}

	public final int getMinAxisValue() {
		return minAxisValue;
	}

	public final int getNumButtons() {
		return numButtons;
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

	final void process() {
		main.pollSdlEvents();
		main.getMainLoop().yield();
	}

	void setNumButtons(final int numButtons) {
		this.numButtons = numButtons;
		input.initButtons();
	}
}

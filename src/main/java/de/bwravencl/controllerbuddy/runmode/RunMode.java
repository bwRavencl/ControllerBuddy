/*
 * Copyright (C) 2015 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.runmode;

import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import java.awt.EventQueue;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/// Abstract base class for all run modes in the ControllerBuddy application.
///
/// A run mode defines how controller input is processed and output is
/// generated. Concrete subclasses implement specific execution strategies such
/// as local output, client-server networking, or server-side input polling.
/// Each run mode runs on its own thread via [Runnable].
public abstract class RunMode implements Runnable {

	/// The default polling interval in milliseconds.
	public static final int DEFAULT_POLL_INTERVAL = 1;

	private static final Logger LOGGER = Logger.getLogger(RunMode.class.getName());

	/// The input instance providing controller state to this run mode.
	final Input input;

	/// The main application instance.
	final Main main;

	/// The maximum axis value reported by the output device.
	int maxAxisValue;

	/// The minimum axis value reported by the output device.
	int minAxisValue;

	/// The number of buttons available on the output device.
	int numButtons;

	/// The polling interval in milliseconds.
	long pollInterval;

	/// Flag controlling whether the run loop continues; set to `false` to stop.
	volatile boolean run = true;

	/// Flag set when the run mode is in the process of stopping.
	private boolean stopping;

	/// Constructs a run mode, storing references to the main application and input
	/// instance and registering this run mode with the input.
	///
	/// @param main the main application instance
	/// @param input the input instance for controller state
	RunMode(final Main main, final Input input) {
		this.main = main;
		this.input = input;
		pollInterval = main.getPollInterval();

		input.setRunMode(this);
	}

	/// Handles a controller disconnection by stopping the run mode, logging a
	/// warning, and optionally showing a dialog to the user.
	///
	/// Does nothing if the run mode is already in the process of stopping.
	final void controllerDisconnected() {
		if (stopping) {
			return;
		}

		Thread.startVirtualThread(() -> main.stopAll(true, !main.isAutoRestartOutput(), true));

		final var controller = input.getSelectedController();
		if (controller != null) {
			LOGGER.warning(Main.assembleControllerLoggingMessage("Could not read from controller ", controller));
		}

		if (!main.isSkipControllerDialogs()) {
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main.getFrame(),
					Main.STRINGS.getString("COULD_NOT_READ_FROM_CONTROLLER_DIALOG_TEXT"),
					Main.STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		}

		stopping = true;
	}

	/// Returns the logger used by this run mode for diagnostic output.
	///
	/// @return the logger instance
	abstract Logger getLogger();

	/// Returns the maximum axis value supported by the output device.
	///
	/// @return the maximum axis value
	public final int getMaxAxisValue() {
		return maxAxisValue;
	}

	/// Returns the minimum axis value supported by the output device.
	///
	/// @return the minimum axis value
	public final int getMinAxisValue() {
		return minAxisValue;
	}

	/// Returns the number of buttons available on the output device.
	///
	/// @return the number of buttons
	public final int getNumButtons() {
		return numButtons;
	}

	/// Returns the poll interval in milliseconds used for input polling.
	///
	/// @return the poll interval in milliseconds
	public final long getPollInterval() {
		return pollInterval;
	}

	/// Logs an informational message indicating that output has started.
	final void logStart() {
		getLogger().info("Starting output");
	}

	/// Logs an informational message indicating that output has stopped.
	final void logStop() {
		getLogger().info("Stopped output");
	}

	/// Polls SDL events and yields the main loop, advancing the application event
	/// cycle by one step.
	final void process() {
		main.pollSdlEvents();
		main.getMainLoop().yield();
	}

	/// Requests the run mode to stop by setting the run flag to false.
	/// Subclasses may override to perform additional cleanup such as closing
	/// sockets.
	public void requestStop() {
		run = false;
	}

	/// Sets the number of buttons and reinitializes the button state in the input.
	///
	/// @param numButtons the number of buttons available on the virtual device
	void setNumButtons(final int numButtons) {
		this.numButtons = numButtons;
		input.initButtons();
	}
}

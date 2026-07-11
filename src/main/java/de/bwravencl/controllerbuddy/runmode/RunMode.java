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
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/// Abstract base class for all run modes in the ControllerBuddy application.
///
/// A run mode defines how controller input is processed and output is
/// generated. Concrete subclasses implement specific execution strategies such
/// as local output, client-server networking, or server-side input polling.
/// Each run mode runs on its own thread via [Runnable].
public abstract class RunMode implements Runnable {

	/// The default maximum polling rate in hertz.
	public static final int DEFAULT_MAX_POLLING_RATE_HZ = 500;

	/// The default minimum polling rate in hertz.
	public static final int DEFAULT_MIN_POLLING_RATE_HZ = 125;

	private static final Logger logger = Logger.getLogger(RunMode.class.getName());

	/// The input instance providing controller state to this run mode.
	final Input input;

	/// The main application instance.
	final Main main;

	/// The maximum polling rate.
	private final int maxPollingRate;

	/// The minimum polling rate.
	private final int minPollingRate;

	/// The maximum axis value reported by the output device.
	int maxAxisValue;

	/// The minimum axis value reported by the output device.
	int minAxisValue;

	/// The number of buttons available on the output device.
	int numButtons;

	/// The polling period in nanoseconds.
	long pollingPeriodNanos;

	/// Flag controlling whether the run loop continues; set to `false` to stop.
	volatile boolean run = true;

	/// Flag set when the run mode is in the process of stopping.
	private boolean stopping;

	/// Constructs a [RunMode], storing references to the main application and input
	/// instance and registering this run mode with the input.
	///
	/// @param main the main application instance
	RunMode(final Main main) {
		this.main = main;
		input = Objects.requireNonNull(main.getInput(), "Field input must not be null");

		minPollingRate = main.getMinPollingRate();
		maxPollingRate = main.getMaxPollingRate();

		useMinPollingRate();

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
			logger.warning(Main.assembleControllerLoggingMessage("Could not read from controller ", controller));
		}

		if (!main.isSkipControllerDialogs()) {
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, main,
					Main.strings.getString("COULD_NOT_READ_FROM_CONTROLLER_DIALOG_TEXT"),
					Main.strings.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
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

	/// Returns the polling period in nanoseconds used for input polling.
	///
	/// @return the polling period in nanoseconds
	public final long getPollingPeriodNanos() {
		return pollingPeriodNanos;
	}

	/// Logs an informational message indicating that output has started.
	final void logStart() {
		getLogger().info("Starting output");
	}

	/// Logs an informational message indicating that output has stopped.
	final void logStop() {
		getLogger().info("Stopped output");
	}

	/// Resets polling rate to minimum and then polls the input device for new
	/// state.
	///
	/// @return `true` if the input device is available, `false` otherwise
	final boolean pollInput() {
		useMinPollingRate();
		return input.poll();
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

	/// Sets the polling rate in hertz, calculating the corresponding polling period
	/// in nanoseconds.
	///
	/// @param pollingRate the desired polling rate in hertz
	private void setPollingRate(final int pollingRate) {
		pollingPeriodNanos = Input.NANOS_PER_SECOND / pollingRate;
	}

	/// Enables the maximum polling rate.
	public final void useMaxPollingRate() {
		setPollingRate(maxPollingRate);
	}

	/// Enables the minimum polling rate.
	final void useMinPollingRate() {
		setPollingRate(minPollingRate);
	}
}

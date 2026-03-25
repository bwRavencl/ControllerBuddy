/*
 * Copyright (C) 2024 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationIntervalEditorBuilder;
import java.lang.constant.Constable;

/// Abstract base class for actions that support configurable minimum and
/// maximum activation intervals.
///
/// The minimum interval enforces a hold time before release takes effect, and
/// the maximum interval limits how long a pressed activation can remain active.
/// Both intervals are optional and configured independently.
///
/// @param <V> the type of input value this action processes
public abstract class ActivationIntervalAction<V extends Constable> extends DescribableAction<V>
		implements IActivatableAction<V>, IInitializationAction<V> {

	/// Title key for the maximum activation interval property.
	public static final String MAX_ACTIVATION_INTERVAL_TITLE = "MAX_ACTIVATION_INTERVAL_TITLE";

	/// Initial value for the maximum activation time sentinel (no deadline set).
	private static final long INITIAL_MAX_ACTIVATION_TIME = Long.MAX_VALUE;

	/// Initial value for the minimum activation time (no hold pending).
	private static final long INITIAL_MIN_ACTIVATION_TIME = 0L;

	/// Initial value for the wasUp flag (input starts in the released state).
	private static final boolean INITIAL_WAS_UP = true;

	/// The activation mode controlling when the action triggers.
	@ActionProperty(icon = "🚀", title = "ACTIVATION_TITLE", description = "ACTIVATION_DESCRIPTION", editorBuilder = ActivationEditorBuilder.class, order = 11)
	Activation activation = Activation.WHILE_PRESSED;

	/// The minimum time in milliseconds the input must be held before release takes
	/// effect.
	@ActionProperty(icon = "⏳", title = "MIN_ACTIVATION_INTERVAL_TITLE", description = "MIN_ACTIVATION_INTERVAL_DESCRIPTION", editorBuilder = ActivationIntervalEditorBuilder.class, order = 500)
	int minActivationInterval;

	/// The maximum time in milliseconds the action remains active while pressed.
	@ActionProperty(icon = "⌛", title = MAX_ACTIVATION_INTERVAL_TITLE, description = "MAX_ACTIVATION_INTERVAL_DESCRIPTION", editorBuilder = ActivationIntervalEditorBuilder.class, order = 501)
	private int maxActivationInterval;

	/// The absolute timestamp in milliseconds at which the maximum activation
	/// interval expires.
	private transient long maxActivationTime = INITIAL_MAX_ACTIVATION_TIME;

	/// The absolute timestamp in milliseconds at which the minimum activation
	/// interval expires.
	private transient long minActivationTime = INITIAL_MIN_ACTIVATION_TIME;

	/// Whether the input was last observed in the released (up) state.
	private transient boolean wasUp = INITIAL_WAS_UP;

	/// Returns whether the given activation mode supports a maximum activation
	/// interval.
	///
	/// Only [Activation#WHILE_PRESSED] supports a maximum interval.
	///
	/// @param activation the activation mode to check
	/// @return `true` if the activation mode supports a maximum interval
	public static boolean activationSupportsMaxInterval(final Activation activation) {
		return activation == Activation.WHILE_PRESSED;
	}

	@Override
	public Activation getActivation() {
		return activation;
	}

	/// Returns the maximum activation interval in milliseconds.
	///
	/// @return the maximum activation interval, or zero if unlimited
	public int getMaxActivationInterval() {
		return maxActivationInterval;
	}

	/// Returns the minimum activation interval in milliseconds.
	///
	/// @return the minimum activation interval, or zero if none
	public int getMinActivationInterval() {
		return minActivationInterval;
	}

	/// Applies the configured minimum and maximum activation interval logic to
	/// the given hot state.
	///
	/// When the input transitions from up to down, the minimum and maximum
	/// activation deadlines are recorded. While held, returning `false` stops
	/// the action once the maximum interval elapses. On release, returning `true`
	/// holds the action active until the minimum interval has passed.
	///
	/// @param hot `true` if the input is currently active (pressed/in-range)
	/// @return the effective hot state after applying interval constraints
	boolean handleActivationInterval(final boolean hot) {
		final var hasMinActivationInterval = minActivationInterval > 0L;
		final var hasMaxActivationInterval = maxActivationInterval > 0L && activationSupportsMaxInterval(activation);

		if (hasMinActivationInterval || hasMaxActivationInterval) {
			final var currentTime = System.currentTimeMillis();

			if (hot) {
				if (hasMaxActivationInterval && currentTime > maxActivationTime) {
					return false;
				}

				if (wasUp) {
					wasUp = false;
					if (hasMinActivationInterval) {
						minActivationTime = currentTime + minActivationInterval;
					}
					if (hasMaxActivationInterval) {
						maxActivationTime = currentTime + maxActivationInterval;
					}
				}
			} else {
				wasUp = true;

				if (hasMinActivationInterval && currentTime <= minActivationTime) {
					return true;
				}

				minActivationTime = 0L;
				maxActivationTime = INITIAL_MAX_ACTIVATION_TIME;
			}
		}

		return hot;
	}

	/// Initializes activation state and resets all transient timing fields to their
	/// defaults.
	///
	/// @param input the current input state
	@Override
	public void init(final Input input) {
		IActivatableAction.super.init(input);

		wasUp = INITIAL_WAS_UP;
		minActivationTime = INITIAL_MIN_ACTIVATION_TIME;
		maxActivationTime = INITIAL_MAX_ACTIVATION_TIME;
	}

	/// Sets the activation mode and clears the maximum activation interval if the
	/// new mode does not support it.
	///
	/// @param activation the activation mode to set
	@Override
	public void setActivation(final Activation activation) {
		this.activation = activation;

		if (!activationSupportsMaxInterval(activation)) {
			maxActivationInterval = 0;
		}
	}

	/// Sets the maximum activation interval in milliseconds.
	///
	/// @param maxActivationInterval the maximum interval to set
	public void setMaxActivationInterval(final int maxActivationInterval) {
		this.maxActivationInterval = maxActivationInterval;
	}

	/// Sets the minimum activation interval in milliseconds.
	///
	/// @param minActivationInterval the minimum interval to set
	public void setMinActivationInterval(final int minActivationInterval) {
		this.minActivationInterval = minActivationInterval;
	}
}

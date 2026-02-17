/* Copyright (C) 2024  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationIntervalEditorBuilder;
import java.lang.constant.Constable;

public abstract class ActivationIntervalAction<V extends Constable> extends DescribableAction<V>
		implements IActivatableAction<V>, IInitializationAction<V> {

	public static final String MAX_ACTIVATION_INTERVAL_LABEL = "MAX_ACTIVATION_INTERVAL";

	@ActionProperty(label = "ACTIVATION", editorBuilder = ActivationEditorBuilder.class, order = 11)
	Activation activation = Activation.WHILE_PRESSED;

	@ActionProperty(label = "MIN_ACTIVATION_INTERVAL", editorBuilder = ActivationIntervalEditorBuilder.class, order = 500)
	int minActivationInterval;

	@ActionProperty(label = MAX_ACTIVATION_INTERVAL_LABEL, editorBuilder = ActivationIntervalEditorBuilder.class, order = 501)
	private int maxActivationInterval;

	private transient long maxActivationTime = Integer.MAX_VALUE;

	private transient long minActivationTime;

	private transient boolean wasUp = true;

	public static boolean activationSupportsMaxInterval(final Activation activation) {
		return activation == Activation.WHILE_PRESSED;
	}

	@Override
	public Activation getActivation() {
		return activation;
	}

	public int getMaxActivationInterval() {
		return maxActivationInterval;
	}

	public int getMinActivationInterval() {
		return minActivationInterval;
	}

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
				maxActivationTime = Long.MAX_VALUE;
			}
		}

		return hot;
	}

	@Override
	public void init(final Input input) {
		IActivatableAction.super.init(input);

		wasUp = true;
		minActivationTime = 0;
		maxActivationTime = Long.MAX_VALUE;
	}

	@Override
	public void setActivation(final Activation activation) {
		this.activation = activation;

		if (!activationSupportsMaxInterval(activation)) {
			maxActivationInterval = 0;
		}
	}

	public void setMaxActivationInterval(final int maxActivationInterval) {
		this.maxActivationInterval = maxActivationInterval;
	}

	public void setMinActivationInterval(final int minActivationInterval) {
		this.minActivationInterval = minActivationInterval;
	}
}

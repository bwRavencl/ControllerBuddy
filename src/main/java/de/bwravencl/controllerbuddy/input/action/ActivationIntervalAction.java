/* Copyright (C) 2023  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.gui.ActivationIntervalEditorBuilder;

abstract class ActivationIntervalAction<V extends Number> extends DescribableAction<V>
		implements IInitializationAction<V> {

	@ActionProperty(label = "MIN_ACTIVATION_INTERVAL", editorBuilder = ActivationIntervalEditorBuilder.class, order = 500)
	private int minActivationInterval = 0;

	@ActionProperty(label = "MAX_ACTIVATION_INTERVAL", editorBuilder = ActivationIntervalEditorBuilder.class, order = 501)
	private int maxActivationInterval = 0;

	private transient boolean wasUp = true;

	private transient long minActivationTime = 0L;

	private transient long maxActivationTime = Integer.MAX_VALUE;

	public int getMaxActivationInterval() {
		return maxActivationInterval;
	}

	public int getMinActivationInterval() {
		return minActivationInterval;
	}

	boolean handleActivationInterval(final boolean hot) {
		final var hasMinActivationInterval = minActivationInterval > 0L;
		final var hasMaxActivationInterval = maxActivationInterval > 0L;

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
		wasUp = true;
		minActivationTime = 0;
		maxActivationTime = Long.MAX_VALUE;
	}

	public void setMaxActivationInterval(final int maxActivationInterval) {
		this.maxActivationInterval = maxActivationInterval;
	}

	public void setMinActivationInterval(final int minActivationInterval) {
		this.minActivationInterval = minActivationInterval;
	}
}

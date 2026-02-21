/* Copyright (C) 2017  Matteo Hausner
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

import java.lang.constant.Constable;

public interface IDelayableAction<V extends Constable> extends IAction<V> {

	long DEFAULT_DELAY = 0L;

	String DELAYED_SYMBOL = "⟿";

	String INSTANT_SYMBOL = "⇝";

	long getDelay();

	default boolean isDelayed() {
		return getDelay() > 0L;
	}

	void setDelay(final long delay);
}

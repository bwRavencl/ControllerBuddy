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

public interface ILongPressAction<V extends Constable> extends IAction<V> {

	boolean DEFAULT_LONG_PRESS = false;

	long MIN_LONG_PRESS_TIME = 1000L;

	boolean isLongPress();

	void setLongPress(final boolean longPress);
}

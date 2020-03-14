/* Copyright (C) 2019  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public interface ISuspendableAction extends IAction<Float> {

	long SUSPEND_TIME = 500L;

	Map<ISuspendableAction, Integer> suspendedActionToAxisMap = new ConcurrentHashMap<>();

	static void reset() {
		suspendedActionToAxisMap.clear();
	}

	default boolean isSuspended() {
		return suspendedActionToAxisMap.containsKey(this);
	}

	default void suspendAxis(final Timer timer, final int axis) {
		suspendedActionToAxisMap.remove(this);
		suspendedActionToAxisMap.put(this, axis);

		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				suspendedActionToAxisMap.remove(ISuspendableAction.this);
			}
		}, SUSPEND_TIME);
	}
}

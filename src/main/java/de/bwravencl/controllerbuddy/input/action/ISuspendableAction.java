/* Copyright (C) 2018  Matteo Hausner
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
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import de.bwravencl.controllerbuddy.gui.Main;

public interface ISuspendableAction extends IAction {

	static final long SUSPEND_TIME = 500L;

	static final Map<ISuspendableAction, String> componentToSuspendedActionsMap = new ConcurrentHashMap<>();

	default boolean isSuspended() {
		return componentToSuspendedActionsMap.containsKey(this);
	}

	default void suspend(final String componentName) {
		componentToSuspendedActionsMap.remove(this);
		componentToSuspendedActionsMap.put(this, componentName);

		Main.getTimer().schedule(new TimerTask() {

			@Override
			public void run() {
				componentToSuspendedActionsMap.remove(ISuspendableAction.this);
			}

		}, SUSPEND_TIME);
	}

}

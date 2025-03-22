/* Copyright (C) 2014  Matteo Hausner
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

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import java.lang.constant.Constable;

public interface IAction<V extends Constable> extends Cloneable {

	static String getDefaultDescription(final IAction<?> action) {
		return getLabel(action.getClass());
	}

	static String getLabel(final Class<?> actionClass) {
		final var annotation = actionClass.getAnnotation(Action.class);
		if (annotation == null) {
			throw new RuntimeException(
					actionClass.getName() + ": missing " + Action.class.getSimpleName() + " annotation");
		}

		return Main.STRINGS.getString(annotation.label());
	}

	Object clone() throws CloneNotSupportedException;

	void doAction(final Input input, int component, V value);

	String getDescription(final Input input);
}

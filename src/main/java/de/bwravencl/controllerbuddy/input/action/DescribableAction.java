/* Copyright (C) 2020  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.gui.StringEditorBuilder;

public abstract class DescribableAction<V extends Number> implements IAction<V> {

	@ActionProperty(label = "DESCRIPTION", editorBuilder = StringEditorBuilder.class, order = 0)
	private String description;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty()) {
			return description;
		}

		return IAction.getDefaultDescription(this);
	}

	boolean isDescriptionEmpty() {
		return description == null || description.isEmpty();
	}

	public void setDescription(final String description) {
		this.description = description;
	}
}

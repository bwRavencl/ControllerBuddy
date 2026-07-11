/*
 * Copyright (C) 2014 Matteo Hausner
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

import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import java.lang.constant.Constable;

/// Abstract base class for actions whose effect can be inverted.
///
/// When inversion is enabled, the action reverses its output direction (e.g.,
/// axis direction or scroll direction).
///
/// @param <V> the type of input value this action processes
abstract class InvertableAction<V extends Constable> extends DescribableAction<V> {

	/// Whether the action output is inverted.
	@ActionProperty(icon = "🔃", title = "INVERT_TITLE", description = "INVERT_DESCRIPTION", editorBuilder = BooleanEditorBuilder.class, order = 500)
	boolean invert;

	/// Returns whether inversion is enabled for this action.
	///
	/// @return `true` if the action output is inverted
	public boolean isInvert() {
		return invert;
	}

	/// Sets whether inversion is enabled for this action.
	///
	/// @param invert `true` to invert the action output
	public void setInvert(final boolean invert) {
		this.invert = invert;
	}
}

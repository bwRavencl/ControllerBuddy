/*
 * Copyright (C) 2026 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action.gui;

import org.jspecify.annotations.NullMarked;

/// Interface for editor components that need to support updating if certain
/// conditions change.
///
/// When another editor wants to notify components created by a certain type of
/// editor builder that they need to update their state, it calls
/// [de.bwravencl.controllerbuddy.gui.EditActionsDialog#updateEditorComponents]
/// with the class of said editor builder.
/// If the supplied class is assignable from the editor builder class
/// returned by [#getEditorBuilderClass], the [#onUpdate] method will be called.
/// Within [#onUpdate], the editor component can then adapt to the new state.
@NullMarked
public interface IUpdatableEditorComponent {

	/// Returns the class of the editor builder that created this editor component.
	///
	/// @return class of the editor builder
	Class<? extends EditorBuilder> getEditorBuilderClass();

	/// Returns the title of the property corresponding to this editor component.
	///
	/// @return title of the property
	String getPropertyTitle();

	/// Called when the editor component needs to update its state.
	void onUpdate();
}

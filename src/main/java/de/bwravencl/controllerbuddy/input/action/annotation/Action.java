/*
 * Copyright (C) 2019 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotation that marks a class as a controller action and provides metadata
/// for the UI.
///
/// Applied to action types to define their category, display title,
/// description, and ordering within the editor.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Action {

	/// Returns the category that this action belongs to, determining where it
	/// appears in the UI.
	///
	/// @return the action category
	ActionCategory category();

	/// Returns a human-readable description of what this action does.
	///
	/// @return the action description
	String description();

	/// Returns the UTF-8 icon for this action in the UI.
	///
	/// @return the action icon
	String icon();

	/// Returns the display order for this action within its category.
	///
	/// @return the sort order value
	int order();

	/// Returns the display title for this action in the UI.
	///
	/// @return the action title
	String title();

	/// Classifies actions by the type of input they apply to.
	///
	/// The category determines which input component types an action is offered for
	/// in the profile editor UI.
	enum ActionCategory {
		/// No restrictions
		ALL,
		/// Axis actions
		AXIS,
		/// Axis and trigger actions
		AXIS_AND_TRIGGER,
		/// Button actions
		BUTTON,
		/// Button and cycle actions
		BUTTON_AND_CYCLES,
		/// On-screen keyboard node
		ON_SCREEN_KEYBOARD_MODE
	}
}

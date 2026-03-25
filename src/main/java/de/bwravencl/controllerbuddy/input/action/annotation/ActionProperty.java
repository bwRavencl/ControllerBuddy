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

import de.bwravencl.controllerbuddy.input.action.gui.EditorBuilder;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotation that marks a field as a configurable property of an action.
///
/// Applied to fields within action classes to expose them as editable
/// properties in the UI, specifying their title, description, editor, and
/// ordering.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ActionProperty {

	/// Returns a human-readable description for this property.
	///
	/// @return the property description
	String description();

	/// Returns the [EditorBuilder] class used to create the UI editor for this
	/// property.
	///
	/// @return the editor builder class
	Class<? extends EditorBuilder> editorBuilder();

	/// Returns the UTF-8 icon for this property in the UI.
	///
	/// @return the property icon
	String icon();

	/// Returns the display order for this property within the action's property
	/// list.
	///
	/// @return the sort order value
	int order();

	/// Returns an alternative field name to use instead of the annotated field's
	/// name.
	///
	/// @return the override field name, or an empty string if not overridden
	String overrideFieldName() default "";

	/// Returns an alternative field type to use instead of the annotated field's
	/// declared type.
	///
	/// @return the override field type, or [Void] if not overridden
	Class<?> overrideFieldType() default Void.class;

	/// Returns the display title for this property in the UI.
	///
	/// @return the property title
	String title();
}

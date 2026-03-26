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

package de.bwravencl.controllerbuddy.input.action.gui;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.input.action.IAction;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;

/// Abstract editor builder for numeric properties.
///
/// Renders a [JSpinner] with a configurable minimum, maximum, and step size.
/// Subclasses define the numeric range and step by implementing
/// [#getMinimum()], [#getMaximum()], and [#getStepSize()].
///
/// @param <T> the numeric type of the property being edited
abstract class NumberEditorBuilder<T extends Number> extends EditorBuilder {

	/// Number of decimal places to which float spinner values are rounded.
	private static final int FLOAT_ROUNDING_DECIMALS = 3;

	private static final Logger LOGGER = Logger.getLogger(NumberEditorBuilder.class.getName());

	/// The spinner component used to display and edit the numeric value.
	JSpinner spinner;

	/// The text field component inside the spinner editor.
	JFormattedTextField textField;

	/// Constructs a number editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose numeric property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws IllegalAccessException if the property cannot be accessed
	/// @throws InvocationTargetException if the property getter throws an exception
	/// @throws NoSuchMethodException if the property getter method is not found
	NumberEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action, final String fieldName,
			final Class<?> fieldType) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	/// Rounds a float value to a fixed number of decimal places using half-up
	/// rounding.
	///
	/// @param value the float value to round
	/// @return the rounded float value
	static float roundFloat(final Float value) {
		return new BigDecimal(value.toString()).setScale(FLOAT_ROUNDING_DECIMALS, RoundingMode.HALF_UP).floatValue();
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var model = new SpinnerNumberModel((Number) initialValue, getMinimum(), getMaximum(), getStepSize());
		spinner = new JSpinner(model);

		final var editor = spinner.getEditor();
		textField = ((JSpinner.DefaultEditor) editor).getTextField();
		textField.setColumns(4);

		final var formatter = (DefaultFormatter) textField.getFormatter();
		formatter.setCommitsOnValidEdit(true);

		spinner.addChangeListener(new JSpinnerSetPropertyChangeListener(action, setterMethod, this));

		parentPanel.add(spinner);
	}

	/// Returns the maximum value for the spinner model.
	///
	/// @return the maximum value
	abstract Comparable<T> getMaximum();

	/// Returns the minimum value for the spinner model.
	///
	/// @return the minimum value
	abstract Comparable<T> getMinimum();

	/// Returns the step size used when incrementing or decrementing the spinner.
	///
	/// @return the step size
	abstract Number getStepSize();

	/// Change listener that applies the current [JSpinner] value to the action
	/// property and optionally forwards it to a registered value consumer.
	///
	/// Float values are rounded to a fixed number of decimal places before being
	/// passed to the setter, preventing floating-point drift from accumulating
	/// as the user steps through values. An optional [Consumer] can be registered
	/// via [#setValueConsumer(Consumer)] to receive each new value, for example,
	/// to update a live preview component.
	static final class JSpinnerSetPropertyChangeListener extends PropertySetter implements ChangeListener {

		/// The editor builder providing rounding and step-size configuration.
		private final NumberEditorBuilder<?> numberEditorBuilder;

		/// Optional consumer notified with each new value after it is applied.
		private Consumer<Object> valueConsumer;

		/// Constructs the change listener with the target action and setter method.
		///
		/// @param action the action whose property is updated on spinner change
		/// @param setterMethod the setter method to invoke with the new value
		/// @param numberEditorBuilder the editor builder providing rounding and
		/// step-size configuration
		private JSpinnerSetPropertyChangeListener(final IAction<?> action, final Method setterMethod,
				final NumberEditorBuilder<?> numberEditorBuilder) {
			super(action, setterMethod);

			this.numberEditorBuilder = numberEditorBuilder;
		}

		/// Sets a consumer that receives each new value after it is applied to the
		/// action.
		///
		/// @param valueConsumer the consumer to notify on each value change, or `null`
		/// to remove the current consumer
		void setValueConsumer(final Consumer<Object> valueConsumer) {
			this.valueConsumer = valueConsumer;
		}

		@Override
		public void stateChanged(final ChangeEvent e) {
			try {
				var value = ((JSpinner) e.getSource()).getValue();

				if (value instanceof final Float floatValue) {
					value = roundFloat(floatValue);
				}

				setterMethod.invoke(action, value);

				if (valueConsumer != null) {
					valueConsumer.accept(value);
				}

				numberEditorBuilder.onNewValueSet();
			} catch (final IllegalAccessException | InvocationTargetException e1) {
				LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}
}

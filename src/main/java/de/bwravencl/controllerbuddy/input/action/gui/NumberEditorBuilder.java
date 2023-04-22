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

package de.bwravencl.controllerbuddy.input.action.gui;

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

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.input.action.IAction;

abstract class NumberEditorBuilder<T extends Number> extends EditorBuilder {

	static final class JSpinnerSetPropertyChangeListener extends PropertySetter implements ChangeListener {

		private Consumer<Object> valueConsumer;

		private JSpinnerSetPropertyChangeListener(final IAction<?> action, final Method setterMethod) {
			super(action, setterMethod);
		}

		void setValueConsumer(final Consumer<Object> valueConsumer) {
			this.valueConsumer = valueConsumer;
		}

		@Override
		public void stateChanged(final ChangeEvent e) {
			try {
				var value = ((JSpinner) e.getSource()).getValue();

				if (value instanceof final Float floatValue)
					value = roundFloat(floatValue);

				setterMethod.invoke(action, value);

				if (valueConsumer != null)
					valueConsumer.accept(value);
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				log.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}

	private static final int FLOAT_ROUNDING_DECIMALS = 3;

	private static final Logger log = Logger.getLogger(NumberEditorBuilder.class.getName());

	static final float roundFloat(final Float value) {
		return new BigDecimal(value.toString()).setScale(FLOAT_ROUNDING_DECIMALS, RoundingMode.HALF_UP).floatValue();
	}

	JSpinner spinner;

	JFormattedTextField textField;

	NumberEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action, final String fieldName,
			final Class<?> fieldType) throws NoSuchFieldException, SecurityException, NoSuchMethodException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
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

		spinner.addChangeListener(new JSpinnerSetPropertyChangeListener(action, setterMethod));

		parentPanel.add(spinner);
	}

	abstract Comparable<T> getMaximum();

	abstract Comparable<T> getMinimum();

	abstract Number getStepSize();
}

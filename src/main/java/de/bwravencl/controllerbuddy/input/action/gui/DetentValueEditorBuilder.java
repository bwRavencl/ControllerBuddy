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
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/// Editor builder for optional detent value properties, providing a spinner
/// with a range of -1.0 to 1.0 and a checkbox to enable or disable the
/// detent.
///
/// When the checkbox is unchecked, the property is set to `null`, indicating
/// no detent position. When checked, the spinner value is applied as the
/// detent point on the axis.
public final class DetentValueEditorBuilder extends NumberEditorBuilder<Float> {

	private static final Logger LOGGER = Logger.getLogger(DetentValueEditorBuilder.class.getName());

	/// Constructs a detent value editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose detent value property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws ReflectiveOperationException if reflection operations fail
	public DetentValueEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws ReflectiveOperationException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final boolean enabled;
		if (initialValue == null) {
			enabled = false;
			initialValue = 0f;
		} else {
			enabled = true;
		}

		super.buildEditor(parentPanel);
		spinner.setEnabled(enabled);

		final var checkBox = new JCheckBox(new AbstractAction() {

			@Serial
			private static final long serialVersionUID = 3326369393786088402L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				final var selected = ((JCheckBox) e.getSource()).isSelected();

				spinner.setEnabled(selected);

				final Float value;
				if (selected) {
					value = roundFloat((Float) spinner.getValue());
				} else {
					value = null;
				}

				try {
					setterMethod.invoke(action, value);
				} catch (final ReflectiveOperationException e1) {
					LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
				}

				spinner.setEnabled(selected);
				onNewValueSet();
			}
		});
		checkBox.setSelected(enabled);
		parentPanel.add(checkBox, 1);
	}

	@Override
	Comparable<Float> getMaximum() {
		return 1f;
	}

	@Override
	Comparable<Float> getMinimum() {
		return -1f;
	}

	@Override
	Number getStepSize() {
		return 0.05f;
	}
}

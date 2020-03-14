/* Copyright (C) 2019  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action.gui;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.input.action.IAction;

public final class DetentValueEditorBuilder extends NumberEditorBuilder<Float> {

	private static final Logger log = Logger.getLogger(DetentValueEditorBuilder.class.getName());

	public DetentValueEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws NoSuchFieldException, SecurityException,
			NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final boolean enabled;
		if (initialValue == null) {
			enabled = false;
			initialValue = 0f;
		} else
			enabled = true;

		super.buildEditor(parentPanel);

		final var checkBox = new JCheckBox(new AbstractAction() {

			private static final long serialVersionUID = 3326369393786088402L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				final var selected = ((JCheckBox) e.getSource()).isSelected();

				final Float value;
				if (selected)
					value = ((Double) spinner.getValue()).floatValue();
				else
					value = null;

				try {
					setterMethod.invoke(action, value);
				} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
					log.log(Level.SEVERE, e1.getMessage(), e1);
				}

				spinner.setEnabled(selected);
			}
		});
		checkBox.setSelected(enabled);
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

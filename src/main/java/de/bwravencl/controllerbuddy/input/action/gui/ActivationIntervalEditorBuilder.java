/* Copyright (C) 2023  Matteo Hausner
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

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.action.ActivationIntervalAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.text.DefaultFormatter;

public final class ActivationIntervalEditorBuilder extends NumberEditorBuilder<Integer> {

	private final boolean disable;

	public ActivationIntervalEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws SecurityException, NoSuchMethodException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);

		if (editActionsDialog.isCycleEditor()) {
			disable = true;
		} else if (action instanceof final IActivatableAction<?> activatableAction
				&& activatableAction.getActivation() != IActivatableAction.Activation.REPEAT) {
			final var fieldToActionPropertyMap = EditActionsDialog
					.getFieldToActionPropertiesMap(ActivationIntervalAction.class);
			disable = fieldToActionPropertyMap.entrySet().stream().filter(e -> fieldName.equals(e.getKey().getName()))
					.findFirst()
					.map(e -> ActivationIntervalAction.MAX_ACTIVATION_INTERVAL_LABEL.equals(e.getValue().label()))
					.orElse(false);
		} else {
			disable = false;
		}
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		if (disable) {
			initialValue = 0;
		}

		super.buildEditor(parentPanel);

		final var editor = new JSpinner.NumberEditor(spinner, "# " + Main.STRINGS.getString("MILLISECOND_SYMBOL"));
		spinner.setEditor(editor);
		textField = editor.getTextField();
		textField.setColumns(6);

		final var formatter = (DefaultFormatter) textField.getFormatter();
		formatter.setCommitsOnValidEdit(true);

		if (disable) {
			spinner.setEnabled(false);
		}
	}

	@Override
	Comparable<Integer> getMaximum() {
		return 10_000;
	}

	@Override
	Comparable<Integer> getMinimum() {
		return 0;
	}

	@Override
	Number getStepSize() {
		return 10;
	}
}

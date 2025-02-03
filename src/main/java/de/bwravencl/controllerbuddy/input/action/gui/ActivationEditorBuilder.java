/* Copyright (C) 2019  Matteo Hausner
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
import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JPanel;

public final class ActivationEditorBuilder extends ArrayEditorBuilder<Activation> {

	public ActivationEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws SecurityException, NoSuchMethodException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var cycleEditor = editActionsDialog.isCycleEditor();

		if (cycleEditor) {
			initialValue = Activation.SINGLE_IMMEDIATELY;
		}

		super.buildEditor(parentPanel);

		if (cycleEditor) {
			comboBox.setEnabled(false);
		}
	}

	@Override
	Activation[] getValues() {
		if (action instanceof ButtonToCycleAction) {
			return new Activation[] { Activation.SINGLE_IMMEDIATELY, Activation.SINGLE_ON_RELEASE };
		}

		return Activation.values();
	}
}

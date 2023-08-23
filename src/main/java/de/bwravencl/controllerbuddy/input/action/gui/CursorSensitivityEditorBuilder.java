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
import de.bwravencl.controllerbuddy.input.action.IAction;
import java.lang.reflect.InvocationTargetException;

public final class CursorSensitivityEditorBuilder extends NumberEditorBuilder<Integer> {

    public CursorSensitivityEditorBuilder(
            final EditActionsDialog editActionsDialog,
            final IAction<?> action,
            final String fieldName,
            final Class<?> fieldType)
            throws SecurityException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException {
        super(editActionsDialog, action, fieldName, fieldType);
    }

    @Override
    Comparable<Integer> getMaximum() {
        return 100_000;
    }

    @Override
    Comparable<Integer> getMinimum() {
        return 1;
    }

    @Override
    Number getStepSize() {
        return 1f;
    }
}

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

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;

public final class ActionsEditorBuilder extends EditorBuilder {

    public ActionsEditorBuilder(
            final EditActionsDialog editActionsDialog,
            final IAction<?> action,
            final String fieldName,
            final Class<?> fieldType)
            throws SecurityException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException {
        super(editActionsDialog, action, fieldName, fieldType);
    }

    @Override
    public void buildEditor(final JPanel parentPanel) {
        final var editActionsButton = new JButton(new EditActionsAction());
        editActionsButton.setPreferredSize(Main.BUTTON_DIMENSION);
        parentPanel.add(editActionsButton);
    }

    private final class EditActionsAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -6538021954760621595L;

        private EditActionsAction() {
            putValue(NAME, Main.strings.getString("EDIT_ACTIONS_ACTION_NAME"));
            putValue(
                    SHORT_DESCRIPTION,
                    MessageFormat.format(
                            Main.strings.getString("EDIT_ACTIONS_ACTION_DESCRIPTION"),
                            IAction.getLabel(action.getClass())));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final var editComponentDialog = new EditActionsDialog(editActionsDialog, (ButtonToCycleAction) action);
            editComponentDialog.setVisible(true);
        }
    }
}

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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ButtonEditorBuilder;
import java.text.MessageFormat;

public abstract class ToButtonAction<V extends Number> extends DescribableAction<V> {

    @ActionProperty(label = "BUTTON_ID", editorBuilder = ButtonEditorBuilder.class, order = 10)
    int buttonId = 0;

    public int getButtonId() {
        return buttonId;
    }

    @Override
    public String getDescription(final Input input) {
        if (!isDescriptionEmpty()) {
            return super.getDescription(input);
        }

        return MessageFormat.format(Main.strings.getString("VJOY_BUTTON_NO"), buttonId + 1);
    }

    final boolean isAlreadyPressed(final Input input) {
        final var buttons = input.getButtons();
        return buttonId < buttons.length && buttons[buttonId];
    }

    public void setButtonId(final int buttonId) {
        this.buttonId = buttonId;
    }
}

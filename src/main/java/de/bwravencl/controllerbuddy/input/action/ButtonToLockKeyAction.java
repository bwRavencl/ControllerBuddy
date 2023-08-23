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
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.BooleanEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LockKeyEditorBuilder;
import java.text.MessageFormat;

@Action(label = "BUTTON_TO_LOCK_KEY_ACTION", category = ActionCategory.BUTTON, order = 116)
public final class ButtonToLockKeyAction extends DescribableAction<Byte> implements IButtonToAction {

    private boolean longPress = DEFAULT_LONG_PRESS;

    @ActionProperty(
            label = "KEY",
            editorBuilder = LockKeyEditorBuilder.class,
            overrideFieldName = "lockKey",
            overrideFieldType = LockKey.class,
            order = 10)
    private LockKey virtualKeyCode = LockKey.CapsLockLockKey;

    @ActionProperty(label = "ON", editorBuilder = BooleanEditorBuilder.class, order = 11)
    private boolean on = true;

    private transient boolean wasUp = true;

    @Override
    public void doAction(final Input input, final int component, Byte value) {
        value = handleLongPress(input, component, value);

        if (value != 0) {
            if (wasUp) {
                wasUp = false;
                if (on) input.getOnLockKeys().add(virtualKeyCode);
                else input.getOffLockKeys().add(virtualKeyCode);
            }
        } else wasUp = true;
    }

    @Override
    public String getDescription(final Input input) {
        if (!isDescriptionEmpty()) return super.getDescription(input);

        return MessageFormat.format(Main.strings.getString(on ? "LOCK_KEY_ON" : "LOCK_KEY_OFF"), getLockKey());
    }

    public LockKey getLockKey() {
        return virtualKeyCode;
    }

    @Override
    public boolean isLongPress() {
        return longPress;
    }

    public boolean isOn() {
        return on;
    }

    public void setLockKey(final LockKey lockKey) {
        virtualKeyCode = lockKey;
    }

    @Override
    public void setLongPress(final boolean longPress) {
        this.longPress = longPress;
    }

    public void setOn(final boolean on) {
        this.on = on;
    }
}

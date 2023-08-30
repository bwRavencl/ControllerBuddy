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
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.KeystrokeEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;
import java.text.MessageFormat;

public abstract class ToKeyAction<V extends Number> extends DescribableAction<V>
        implements IActivatableAction<V>, ILongPressAction<V>, IResetableAction<V> {

    @ActionProperty(label = "ACTIVATION", editorBuilder = ActivationEditorBuilder.class, order = 11)
    private Activation activation = Activation.REPEAT;

    @ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
    private boolean longPress = DEFAULT_LONG_PRESS;

    @ActionProperty(label = "KEYSTROKE", editorBuilder = KeystrokeEditorBuilder.class, order = 10)
    private KeyStroke keystroke = new KeyStroke();

    private transient Activatable activatable;

    private transient boolean wasDown;

    @Override
    public Object clone() throws CloneNotSupportedException {
        final var toKeyAction = (ToKeyAction<?>) super.clone();
        toKeyAction.setKeystroke((KeyStroke) keystroke.clone());

        return toKeyAction;
    }

    @Override
    public Activatable getActivatable() {
        return activatable;
    }

    @Override
    public Activation getActivation() {
        return activation;
    }

    @Override
    public String getDescription(final Input input) {
        if (!isDescriptionEmpty()) return super.getDescription(input);

        return MessageFormat.format(Main.strings.getString("PRESS"), keystroke);
    }

    public KeyStroke getKeystroke() {
        return keystroke;
    }

    void handleAction(final boolean hot, final Input input) {
        if (activatable == Activatable.ALWAYS) {
            input.getDownUpKeyStrokes().add(keystroke);
            return;
        }

        switch (activation) {
            case REPEAT -> {
                final var downKeyStrokes = input.getDownKeyStrokes();
                if (!hot) {
                    if (wasDown) {
                        downKeyStrokes.remove(keystroke);
                        wasDown = false;
                    }
                } else {
                    downKeyStrokes.add(keystroke);
                    wasDown = true;
                }
            }
            case SINGLE_IMMEDIATELY -> {
                if (!hot) activatable = Activatable.YES;
                else if (activatable == Activatable.YES) {
                    activatable = Activatable.NO;
                    input.getDownUpKeyStrokes().add(keystroke);
                }
            }
            case SINGLE_ON_RELEASE -> {
                if (hot) {
                    if (activatable == Activatable.NO) activatable = Activatable.YES;
                    else if (activatable == Activatable.DENIED_BY_OTHER_ACTION) activatable = Activatable.NO;
                } else if (activatable == Activatable.YES) {
                    activatable = Activatable.NO;
                    input.getDownUpKeyStrokes().add(keystroke);
                }
            }
        }
    }

    @Override
    public boolean isLongPress() {
        return longPress;
    }

    @Override
    public void reset(final Input input) {
        wasDown = false;
    }

    @Override
    public void setActivatable(final Activatable activatable) {
        this.activatable = activatable;
    }

    @Override
    public void setActivation(final Activation activation) {
        this.activation = activation;
    }

    public void setKeystroke(final KeyStroke keystroke) {
        this.keystroke = keystroke;
    }

    @Override
    public void setLongPress(final boolean longPress) {
        this.longPress = longPress;
    }
}

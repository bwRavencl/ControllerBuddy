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
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActionsEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Action(label = "BUTTON_TO_CYCLE_ACTION", category = ActionCategory.BUTTON, order = 140)
public final class ButtonToCycleAction extends DescribableAction<Byte>
        implements IActivatableAction<Byte>, IButtonToAction, IResetableAction<Byte> {

    private transient Activatable activatable = Activatable.YES;

    private transient int index = 0;

    @ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
    private boolean longPress = DEFAULT_LONG_PRESS;

    @ActionProperty(label = "ACTIONS", editorBuilder = ActionsEditorBuilder.class, order = 10)
    private List<IAction<Byte>> actions = new ArrayList<>();

    @ActionProperty(label = "ACTIVATION", editorBuilder = ActivationEditorBuilder.class, order = 11)
    private Activation activation = Activation.SINGLE_IMMEDIATELY;

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() throws CloneNotSupportedException {
        final var cycleAction = (ButtonToCycleAction) super.clone();

        final var clonedActions = new ArrayList<IAction<Byte>>();
        for (final var action : actions) {
            clonedActions.add((IAction<Byte>) action.clone());
        }
        cycleAction.setActions(clonedActions);

        return cycleAction;
    }

    @Override
    public void doAction(final Input input, final int component, Byte value) {
        value = handleLongPress(input, component, value);

        final var hot = value != 0;

        switch (activation) {
            case REPEAT -> throw new IllegalStateException();
            case SINGLE_IMMEDIATELY -> {
                if (!hot) {
                    activatable = Activatable.YES;
                } else if (activatable == Activatable.YES) {
                    activatable = Activatable.NO;
                    doActionAndAdvanceIndex(input, component);
                }
            }
            case SINGLE_ON_RELEASE -> {
                if (hot) {
                    if (activatable == Activatable.NO) {
                        activatable = Activatable.YES;
                    } else if (activatable == Activatable.DENIED_BY_OTHER_ACTION) {
                        activatable = Activatable.NO;
                    }
                } else if (activatable == Activatable.YES) {
                    activatable = Activatable.NO;
                    doActionAndAdvanceIndex(input, component);
                }
            }
        }
    }

    private void doActionAndAdvanceIndex(final Input input, final int component) {
        actions.get(index).doAction(input, component, Byte.MAX_VALUE);

        if (index == actions.size() - 1) {
            index = 0;
        } else {
            index++;
        }
    }

    public List<IAction<Byte>> getActions() {
        return actions;
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
        if (!isDescriptionEmpty()) {
            return super.getDescription(input);
        }

        return MessageFormat.format(
                Main.strings.getString("CYCLE"),
                actions.stream().map(action -> action.getDescription(input)).collect(Collectors.joining(" → ")));
    }

    @Override
    public void init(final Input input) {
        IActivatableAction.super.init(input);

        actions.forEach(action -> {
            if (action instanceof final IInitializationAction<?> initializationAction) {
                initializationAction.init(input);
            }

            if (action instanceof final IActivatableAction<?> activatableAction) {
                activatableAction.setActivatable(Activatable.ALWAYS);
            }
        });
    }

    @Override
    public boolean isLongPress() {
        return longPress;
    }

    @Override
    public void reset(final Input input) {
        index = 0;
    }

    public void setActions(final List<IAction<Byte>> actions) {
        this.actions = actions;
    }

    @Override
    public void setActivatable(final Activatable activatable) {
        this.activatable = activatable;
    }

    @Override
    public void setActivation(final Activation activation) {
        this.activation = activation;
    }

    @Override
    public void setLongPress(final boolean longPress) {
        this.longPress = longPress;
    }
}

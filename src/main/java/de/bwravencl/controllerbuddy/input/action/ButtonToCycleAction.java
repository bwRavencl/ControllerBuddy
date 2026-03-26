/*
 * Copyright (C) 2014 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActionsEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.DelayEditorBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/// Maps a button press to cycling through a list of sub-actions.
///
/// Each activation advances to the next action in the list, wrapping back to
/// the first action after the last one is reached. Supports on-press and
/// on-release activation modes, configurable delay, and can be reset to the
/// beginning of the cycle.
@Action(icon = "🔁", title = "BUTTON_TO_CYCLE_ACTION_TITLE", description = "BUTTON_TO_CYCLE_ACTION_DESCRIPTION", category = ActionCategory.BUTTON, order = 140)
public final class ButtonToCycleAction extends DescribableAction<Boolean>
		implements IActivatableAction<Boolean>, IButtonToDelayableAction, IResetableAction<Boolean> {

	/// Symbol used to visually represent a cycle action.
	public static final String CYCLE_SYMBOL = "⟳";

	/// Ordered list of sub-actions that this cycle iterates through.
	@ActionProperty(icon = "🛠️", title = "ACTIONS_TITLE", description = "ACTIONS_DESCRIPTION", editorBuilder = ActionsEditorBuilder.class, order = 10)
	private List<IAction<Boolean>> actions = new ArrayList<>();

	/// Transient activatable state used for edge-triggered activation modes.
	private transient Activatable activatable = Activatable.YES;

	/// Activation mode that controls when the cycle advances.
	@ActionProperty(icon = "🚀", title = "ACTIVATION_TITLE", description = "ACTIVATION_DESCRIPTION", editorBuilder = ActivationEditorBuilder.class, order = 11)
	private Activation activation = Activation.ON_PRESS;

	/// Delay in milliseconds before this action becomes active.
	@ActionProperty(icon = "⏱️", title = "DELAY_TITLE", description = "DELAY_DESCRIPTION", editorBuilder = DelayEditorBuilder.class, order = 400)
	private long delay = DEFAULT_DELAY;

	/// Current position in the sub-action list.
	private transient int index;

	/// Creates a deep copy of this cycle action, including clones of all
	/// sub-actions.
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() throws CloneNotSupportedException {
		final var cycleAction = (ButtonToCycleAction) super.clone();

		final var clonedActions = new ArrayList<IAction<Boolean>>();
		for (final var action : actions) {
			clonedActions.add((IAction<Boolean>) action.clone());
		}
		cycleAction.setActions(clonedActions);

		return cycleAction;
	}

	/// Processes a button input value and executes the current sub-action in the
	/// cycle
	/// when the configured activation condition is met.
	@Override
	public void doAction(final Input input, final int component, Boolean value) {
		value = handleDelay(input, component, value);

		switch (activation) {
		case WHILE_PRESSED -> throw new IllegalStateException(ButtonToCycleAction.class.getSimpleName()
				+ " must not have activation value: " + Activation.WHILE_PRESSED);
		case ON_PRESS -> {
			if (!value) {
				activatable = Activatable.YES;
			} else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				doActionAndAdvanceIndex(input, component);
			}
		}
		case ON_RELEASE -> {
			if (value) {
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

	/// Fires the sub-action at the current cycle index and advances the index.
	///
	/// After invoking the sub-action, resets the `wasUp` state if it is a
	/// [ButtonToLockKeyAction], then increments the index, and wraps it back to
	/// zero after the last sub-action.
	///
	/// @param input the current input state
	/// @param component the button component index
	private void doActionAndAdvanceIndex(final Input input, final int component) {
		final var action = actions.get(index);

		action.doAction(input, component, true);
		if (action instanceof final ButtonToLockKeyAction buttonToLockKeyAction) {
			buttonToLockKeyAction.resetWasUp();
		}

		if (index == actions.size() - 1) {
			index = 0;
		} else {
			index++;
		}
	}

	/// Returns the list of sub-actions that this cycle iterates through.
	///
	/// @return the list of sub-actions
	public List<IAction<Boolean>> getActions() {
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
	public long getDelay() {
		return delay;
	}

	/// Returns a human-readable description composed of the descriptions of all
	/// sub-actions joined with arrow separators and wrapped in cycle symbols.
	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty() || actions.isEmpty()) {
			return super.getDescription(input);
		}

		return actions.stream().map(action -> action.getDescription(input))
				.collect(Collectors.joining(" → ", CYCLE_SYMBOL + " ", " ⟲"));
	}

	/// Initializes this action and all sub-actions, setting each sub-action's
	/// activatable
	/// state to [Activatable#ALWAYS] so they fire unconditionally within the cycle.
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

	/// Resets the cycle index back to the first action.
	@Override
	public void reset(final Input input) {
		index = 0;
	}

	/// Sets the list of sub-actions for this cycle.
	///
	/// @param actions the list of sub-actions
	public void setActions(final List<IAction<Boolean>> actions) {
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
	public void setDelay(final long delay) {
		this.delay = delay;
	}
}

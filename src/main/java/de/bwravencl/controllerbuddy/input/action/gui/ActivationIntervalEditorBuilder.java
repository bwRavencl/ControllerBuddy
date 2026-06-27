/*
 * Copyright (C) 2023 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action.gui;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.input.action.ActivationIntervalAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction;
import java.io.Serial;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import org.jspecify.annotations.NullMarked;

/// Editor builder for activation interval properties, rendering a millisecond
/// spinner with a range of 0 to 10,000 ms.
///
/// The spinner is disabled when the current activation mode does not support a
/// maximum interval or when editing inside a cycle editor.
@NullMarked
public final class ActivationIntervalEditorBuilder extends NumberEditorBuilder<Integer> {

	/// The title of the property this editor builder handles.
	private final String propertyTitle;

	/// Constructs an [ActivationIntervalEditorBuilder] for the specified action
	/// property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose activation interval property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws ReflectiveOperationException if reflection operations fail
	public ActivationIntervalEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws ReflectiveOperationException {
		super(editActionsDialog, action, fieldName, fieldType);

		final var fieldToActionPropertyMap = EditActionsDialog
				.getFieldToActionPropertiesMap(ActivationIntervalAction.class);
		propertyTitle = fieldToActionPropertyMap.entrySet().stream().filter(e -> fieldName.equals(e.getKey().getName()))
				.findFirst().map(e -> e.getValue().title()).orElseThrow();
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		super.buildEditor(parentPanel);
		Objects.requireNonNull(spinner, "Field spinner must not be null");

		GuiUtils.makeMillisecondSpinner(spinner, 6);
		((IUpdatableEditorComponent) spinner).onUpdate();
	}

	@Override
	JSpinner createSpinner(final SpinnerNumberModel model) {
		return new ActivationIntervalJSpinner(model);
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

	/// A [JSpinner] that updates its enabled state based on the current
	/// [IActivatableAction]'s [IActivatableAction.Activation] and the active editor
	/// context.
	///
	/// The spinner is disabled in the following cases:
	///
	/// - The parent [#editActionsDialog] is a cycle editor.
	/// - The selected [IActivatableAction.Activation] does not support a maximum
	/// activation interval.
	///
	/// When disabled, the spinner's value is reset to `0`.
	private final class ActivationIntervalJSpinner extends JSpinner implements IUpdatableEditorComponent {

		@Serial
		private static final long serialVersionUID = -2748747027292654756L;

		/// Constructs an [ActivationIntervalJSpinner] for the given model.
		///
		/// @param model a model for the new spinner
		private ActivationIntervalJSpinner(final SpinnerModel model) {
			super(model);
		}

		@Override
		public Class<? extends EditorBuilder> getEditorBuilderClass() {
			return ActivationIntervalEditorBuilder.class;
		}

		@Override
		public String getPropertyTitle() {
			return propertyTitle;
		}

		@Override
		public void onUpdate() {
			final var disabled = editActionsDialog.isCycleEditor()
					|| (ActivationIntervalAction.MAX_ACTIVATION_INTERVAL_TITLE.equals(propertyTitle)
							&& (action instanceof final IActivatableAction<?> activatableAction
									&& !ActivationIntervalAction
											.activationSupportsMaxInterval(activatableAction.getActivation())));

			setEnabled(!disabled);
			if (disabled) {
				setValue(0);
			}
		}
	}
}

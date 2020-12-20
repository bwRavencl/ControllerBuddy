/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.controllerbuddy.input.action;

import static de.bwravencl.controllerbuddy.gui.Main.strings;
import static java.text.MessageFormat.format;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.KeystrokeEditorBuilder;

public abstract class ToKeyAction<V extends Number> extends DescribableAction<V> implements IActivatableAction<V> {

	@ActionProperty(label = "ACTIVATION", editorBuilder = ActivationEditorBuilder.class, order = 11)
	Activation activation = Activation.REPEAT;

	@ActionProperty(label = "KEYSTROKE", editorBuilder = KeystrokeEditorBuilder.class, order = 10)
	KeyStroke keystroke = new KeyStroke();

	private transient Activatable activatable;

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
		if (!isDescriptionEmpty())
			return super.getDescription(input);

		return format(strings.getString("PRESS"), keystroke);
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
		case REPEAT:
			final var downKeyStrokes = input.getDownKeyStrokes();
			if (!hot)
				downKeyStrokes.remove(keystroke);
			else
				downKeyStrokes.add(keystroke);
			break;
		case SINGLE_IMMEDIATELY:
			if (!hot)
				activatable = Activatable.YES;
			else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				input.getDownUpKeyStrokes().add(keystroke);
			}
			break;
		case SINGLE_ON_RELEASE:
			if (hot) {
				if (activatable == Activatable.NO)
					activatable = Activatable.YES;
				else if (activatable == Activatable.DENIED_BY_OTHER_ACTION)
					activatable = Activatable.NO;
			} else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				input.getDownUpKeyStrokes().add(keystroke);
			}
			break;
		}
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
}

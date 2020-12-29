/* Copyright (C) 2019  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action.gui;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.gui.OnScreenKeyboard;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.IAction;

public final class ModeEditorBuilder extends ArrayEditorBuilder<Mode> {

	public ModeEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action, final String fieldName,
			final Class<?> fieldType) throws NoSuchFieldException, SecurityException, NoSuchMethodException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	Mode[] getValues() {
		final var profile = editActionsDialog.getInput().getProfile();

		final var modes = profile.getModes().stream().filter(m -> !Profile.defaultMode.equals(m))
				.collect(Collectors.toList());

		if (!profile.getModes().contains(OnScreenKeyboard.onScreenKeyboardMode))
			modes.add(OnScreenKeyboard.onScreenKeyboardMode);

		return modes.toArray(new Mode[modes.size()]);
	}
}

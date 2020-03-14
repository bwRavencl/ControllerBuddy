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

package de.bwravencl.controllerbuddy.input;

public final class KeyStroke implements Cloneable {

	private Integer[] keyCodes;
	private Integer[] modifierCodes;

	public KeyStroke() {
		this(new Integer[0], new Integer[0]);
	}

	public KeyStroke(final Integer[] keyCodes, final Integer[] modifierCodes) {
		this.keyCodes = keyCodes;
		this.modifierCodes = modifierCodes;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final var keyStroke = (KeyStroke) super.clone();

		final var clonedKeyCodes = new Integer[keyCodes.length];
		for (var i = 0; i < keyCodes.length; i++)
			clonedKeyCodes[i] = keyCodes[i];
		keyStroke.setKeyCodes(clonedKeyCodes);

		final var clonedModifierCodes = new Integer[modifierCodes.length];
		for (var i = 0; i < modifierCodes.length; i++)
			clonedModifierCodes[i] = modifierCodes[i];
		keyStroke.setModifierCodes(clonedModifierCodes);

		return keyStroke;
	}

	public Integer[] getKeyCodes() {
		return keyCodes;
	}

	public Integer[] getModifierCodes() {
		return modifierCodes;
	}

	public void setKeyCodes(final Integer[] keyCodes) {
		this.keyCodes = keyCodes;
	}

	public void setModifierCodes(final Integer[] modifierCodes) {
		this.modifierCodes = modifierCodes;
	}
}

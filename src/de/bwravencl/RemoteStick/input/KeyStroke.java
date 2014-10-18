/* Copyright (C) 2014  Matteo Hausner
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

package de.bwravencl.RemoteStick.input;

public class KeyStroke {

	public static final String[] KEY_CODES = { "VK_A", "VK_B", "VK_C" };
	public static final String[] MODIFIER_CODES = { "CTRL", "ALT", "SHIFT" };

	private String[] keyCodes = new String[0];
	private String[] modifierCodes = new String[0];

	public String[] getKeyCodes() {
		return keyCodes;
	}

	public void setKeyCodes(String[] keyCodes) {
		this.keyCodes = keyCodes;
	}

	public String[] getModifierCodes() {
		return modifierCodes;
	}

	public void setModifierCodes(String[] modifierCodes) {
		this.modifierCodes = modifierCodes;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final KeyStroke keyStroke = new KeyStroke();

		final String[] clonedKeyCodes = new String[keyCodes.length];
		for (int i = 0; i < keyCodes.length; i++)
			clonedKeyCodes[i] = new String(keyCodes[i]);
		keyStroke.setKeyCodes(clonedKeyCodes);

		final String[] clonedModifierCodes = new String[modifierCodes.length];
		for (int i = 0; i < modifierCodes.length; i++)
			clonedModifierCodes[i] = new String(modifierCodes[i]);
		keyStroke.setModifierCodes(clonedModifierCodes);

		return keyStroke;
	}

}

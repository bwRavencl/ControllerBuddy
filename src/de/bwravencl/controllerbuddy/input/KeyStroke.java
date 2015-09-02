/* Copyright (C) 2015  Matteo Hausner
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

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

public class KeyStroke {

	private static class RightModifierKey {

		public RightModifierKey(String name, int keyCode) {
			this.name = name;
			this.keyCode = keyCode;
		}

		public String name;

		public int keyCode;

	}

	private static final RightModifierKey[] rightModifierKeys = { new RightModifierKey("Right Shift", 0xA1),
			new RightModifierKey("Right Control", 0xA3), new RightModifierKey("Right Alt", 0xA5),
			new RightModifierKey("Right Windows", 0x5C) };

	public static final int[] MODIFIER_CODES = { KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT,
			KeyEvent.VK_WINDOWS, KeyEvent.VK_CONTEXT_MENU, KeyEvent.VK_ALT_GRAPH };

	public static Map<String, Integer> getKeyCodeMap() {
		final Map<String, Integer> keyCodeMap = new TreeMap<String, Integer>();

		for (RightModifierKey rmk : rightModifierKeys)
			keyCodeMap.put(rmk.name, rmk.keyCode);

		final Field[] fields = KeyEvent.class.getDeclaredFields();
		for (Field f : fields) {
			if (Modifier.isStatic(f.getModifiers())) {
				if (f.getType() == int.class && f.getName().startsWith("VK_")) {
					try {
						int keyCode = f.getInt(null);
						keyCodeMap.put(KeyEvent.getKeyText(keyCode), keyCode);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return keyCodeMap;
	}

	private Integer[] keyCodes = {};
	private Integer[] modifierCodes = {};

	public Integer[] getKeyCodes() {
		return keyCodes;
	}

	public void setKeyCodes(Integer[] keyCodes) {
		this.keyCodes = keyCodes;
	}

	public Integer[] getModifierCodes() {
		return modifierCodes;
	}

	public void setModifierCodes(Integer[] modifierCodes) {
		this.modifierCodes = modifierCodes;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		final KeyStroke keyStroke = new KeyStroke();

		final Integer[] clonedKeyCodes = new Integer[keyCodes.length];
		for (int i = 0; i < keyCodes.length; i++)
			clonedKeyCodes[i] = new Integer(keyCodes[i]);
		keyStroke.setKeyCodes(clonedKeyCodes);

		final Integer[] clonedModifierCodes = new Integer[modifierCodes.length];
		for (int i = 0; i < modifierCodes.length; i++)
			clonedModifierCodes[i] = new Integer(modifierCodes[i]);
		keyStroke.setModifierCodes(clonedModifierCodes);

		return keyStroke;
	}

}

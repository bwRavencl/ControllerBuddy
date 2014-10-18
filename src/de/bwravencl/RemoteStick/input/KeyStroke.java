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

	public static final String[] KEY_CODES = { "LBUTTON", "RBUTTON", "CANCEL",
			"MBUTTON", "XBUTTON1", "XBUTTON2", "BACK", "TAB", "CLEAR",
			"RETURN", "SHIFT", "CONTROL", "MENU", "PAUSE", "CAPITAL", "KANA",
			"HANGEUL", "HANGUL", "JUNJA", "FINAL", "KANJI", "HANJA", "ESCAPE",
			"CONVERT", "NONCONVERT", "ACCEPT", "MODECHANGE", "SPACE", "PRIOR",
			"NEXT", "END", "HOME", "LEFT", "UP", "RIGHT", "DOWN", "SELECT",
			"PRINT", "EXECUTE", "SNAPSHOT", "INSERT", "DELETE", "HELP", "VK_0",
			"VK_1", "VK_2", "VK_3", "VK_4", "VK_5", "VK_6", "VK_7", "VK_8",
			"VK_9", "VK_A", "VK_B", "VK_C", "VK_D", "VK_E", "VK_F", "VK_G",
			"VK_H", "VK_I", "VK_J", "VK_K", "VK_L", "VK_M", "VK_N", "VK_N",
			"VK_O", "VK_P", "VK_Q", "VK_R", "VK_S", "VK_T", "VK_U", "VK_V",
			"VK_W", "VK_X", "VK_Y", "VK_Z", "LWIN", "RWIN", "APPS", "SLEEP",
			"NUMPAD0", "NUMPAD1", "NUMPAD2", "NUMPAD3", "NUMPAD4", "NUMPAD5",
			"NUMPAD6", "NUMPAD7", "NUMPAD8", "NUMPAD9", "MULTIPLY", "ADD",
			"SEPARATOR", "SUBTRACT", "DECIMAL", "DIVIDE", "F1", "F2", "F3",
			"F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "F13",
			"F14", "F15", "F16", "F17", "F18", "F19", "F20", "F21", "F22",
			"F23", "F24", "NUMLOCK", "SCROLL", "LSHIFT", "RSHIFT", "LCONTROL",
			"RCONTROL", "LMENU", "RMENU", "BROWSER_BACK", "BROWSER_FORWARD",
			"BROWSER_REFRESH", "BROWSER_STOP", "BROWSER_SEARCH",
			"BROWSER_FAVORITES", "BROWSER_HOME", "VOLUME_MUTE", "VOLUME_DOWN",
			"VOLUME_UP", "MEDIA_NEXT_TRACK", "MEDIA_PREV_TRACK", "MEDIA_STOP",
			"MEDIA_PLAY_PAUSE", "LAUNCH_MAIL", "LAUNCH_MEDIA_SELECT",
			"LAUNCH_APP1", "LAUNCH_APP2", "OEM_1", "OEM_PLUS", "OEM_COMMA",
			"OEM_MINUS", "OEM_PERIOD", "OEM_2", "OEM_2", "OEM_3", "OEM_4",
			"OEM_5", "OEM_6", "OEM_7", "OEM_8", "OEM_102", "PROCESSKEY",
			"PACKET", "ATTN", "CRSEL", "EXSEL", "EREOF", "PLAY", "ZOOM",
			"NONAME", "PA1", "OEM_CLEAR" };
	public static final String[] MODIFIER_CODES = { "SHIFT", "CONTROL", "MENU",
			"LSHIFT", "RSHIFT", "LCONTROL", "RCONTROL", "LMENU", "RMENU",
			"LWIN", "RWIN" };

	private String[] keyCodes = {};
	private String[] modifierCodes = {};

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

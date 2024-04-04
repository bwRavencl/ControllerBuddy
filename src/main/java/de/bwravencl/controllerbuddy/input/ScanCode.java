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

package de.bwravencl.controllerbuddy.input;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import uk.co.bithatch.linuxio.EventCode;

public record ScanCode(String name, int keyCode, EventCode eventCode) {

	public static final String DIK_0 = "0";
	public static final String DIK_1 = "1";
	public static final String DIK_2 = "2";
	public static final String DIK_3 = "3";
	public static final String DIK_4 = "4";
	public static final String DIK_5 = "5";
	public static final String DIK_6 = "6";
	public static final String DIK_7 = "7";
	public static final String DIK_8 = "8";
	public static final String DIK_9 = "9";
	public static final String DIK_A = "A";
	public static final String DIK_ADD = "Num+";
	public static final String DIK_APOSTROPHE = "'";
	public static final String DIK_B = "B";
	public static final String DIK_BACK = "Back";
	public static final String DIK_BACKSLASH = "\\";
	public static final String DIK_C = "C";
	public static final String DIK_COMMA = ",";
	public static final String DIK_D = "D";
	public static final String DIK_DECIMAL = "Num.";
	public static final String DIK_DELETE = "Del";
	public static final String DIK_DIVIDE = "Num/";
	public static final String DIK_DOWN = "Down Arrow";
	public static final String DIK_E = "E";
	public static final String DIK_END = "End";
	public static final String DIK_EQUALS = "=";
	public static final String DIK_ESCAPE = "Esc";
	public static final String DIK_F = "F";
	public static final String DIK_F1 = "F1";
	public static final String DIK_F10 = "F10";
	public static final String DIK_F11 = "F11";
	public static final String DIK_F12 = "F12";
	public static final String DIK_F2 = "F2";
	public static final String DIK_F3 = "F3";
	public static final String DIK_F4 = "F4";
	public static final String DIK_F5 = "F5";
	public static final String DIK_F6 = "F6";
	public static final String DIK_F7 = "F7";
	public static final String DIK_F8 = "F8";
	public static final String DIK_F9 = "F9";
	public static final String DIK_G = "G";
	public static final String DIK_GRAVE = "`";
	public static final String DIK_H = "H";
	public static final String DIK_HOME = "Home";
	public static final String DIK_I = "I";
	public static final String DIK_INSERT = "Ins";
	public static final String DIK_J = "J";
	public static final String DIK_K = "K";
	public static final String DIK_L = "L";
	public static final String DIK_LBRACKET = "[";
	public static final String DIK_LCONTROL = "L Ctrl";
	public static final String DIK_LEFT = "Left Arrow";
	public static final String DIK_LMENU = "L Alt";
	public static final String DIK_LSHIFT = "L Shift";
	public static final String DIK_LWIN = "L Win";
	public static final String DIK_M = "M";
	public static final String DIK_MINUS = "-";
	public static final String DIK_MULTIPLY = "Num*";
	public static final String DIK_N = "N";
	public static final String DIK_NEXT = "PgDn";
	public static final String DIK_NUMPAD0 = "Num0";
	public static final String DIK_NUMPAD1 = "Num1";
	public static final String DIK_NUMPAD2 = "Num2";
	public static final String DIK_NUMPAD3 = "Num3";
	public static final String DIK_NUMPAD4 = "Num4";
	public static final String DIK_NUMPAD5 = "Num5";
	public static final String DIK_NUMPAD6 = "Num6";
	public static final String DIK_NUMPAD7 = "Num7";
	public static final String DIK_NUMPAD8 = "Num8";
	public static final String DIK_NUMPAD9 = "Num9";
	public static final String DIK_NUMPADENTER = "NumEnter";
	public static final String DIK_O = "O";
	public static final String DIK_P = "P";
	public static final String DIK_PAUSE = "Pause";
	public static final String DIK_PERIOD = ".";
	public static final String DIK_PRIOR = "PgUp";
	public static final String DIK_Q = "Q";
	public static final String DIK_R = "R";
	public static final String DIK_RBRACKET = "]";
	public static final String DIK_RCONTROL = "R Ctrl";
	public static final String DIK_RETURN = "Return";
	public static final String DIK_RIGHT = "Right Arrow";
	public static final String DIK_RMENU = "R Alt";
	public static final String DIK_RSHIFT = "R Shift";
	public static final String DIK_RWIN = "R Win";
	public static final String DIK_S = "S";
	public static final String DIK_SEMICOLON = ";";
	public static final String DIK_SLASH = "/";
	public static final String DIK_SPACE = "Space";
	public static final String DIK_SUBTRACT = "Num-";
	public static final String DIK_SYSRQ = "SysRq";
	public static final String DIK_T = "T";
	public static final String DIK_TAB = "Tab";
	public static final String DIK_U = "U";
	public static final String DIK_UP = "Up Arrow";
	public static final String DIK_V = "V";
	public static final String DIK_W = "W";
	public static final String DIK_X = "X";
	public static final String DIK_Y = "Y";
	public static final String DIK_Z = "Z";
	public static final Map<String, ScanCode> nameToScanCodeMap;
	public static final Map<Integer, ScanCode> keyCodeToScanCodeMap;
	public static final Set<Integer> extendedKeyScanCodesSet;
	private static final String DIK_CAPITAL = "CapsLock";
	private static final String DIK_NEXTTRACK = "Next";
	private static final String DIK_NUMLOCK = "NumLock";
	private static final String DIK_NUMPADEQUALS = "Num=";
	private static final String DIK_OEM_102 = "OEM_102";
	private static final String DIK_SCROLL = "ScrollLock";
	private static final ScanCode[] KEY_CODES = { new ScanCode(DIK_ESCAPE, 0x1, EventCode.KEY_ESC),
			new ScanCode(DIK_1, 0x2, EventCode.KEY_1), new ScanCode(DIK_2, 0x3, EventCode.KEY_2),
			new ScanCode(DIK_3, 0x4, EventCode.KEY_3), new ScanCode(DIK_4, 0x5, EventCode.KEY_4),
			new ScanCode(DIK_5, 0x6, EventCode.KEY_5), new ScanCode(DIK_6, 0x7, EventCode.KEY_6),
			new ScanCode(DIK_7, 0x8, EventCode.KEY_7), new ScanCode(DIK_8, 0x9, EventCode.KEY_8),
			new ScanCode(DIK_9, 0xA, EventCode.KEY_9), new ScanCode(DIK_0, 0xB, EventCode.KEY_0),
			new ScanCode(DIK_MINUS, 0xC, EventCode.KEY_MINUS), new ScanCode(DIK_EQUALS, 0xD, EventCode.KEY_EQUAL),
			new ScanCode(DIK_BACK, 0xE, EventCode.KEY_BACKSPACE), new ScanCode(DIK_TAB, 0xF, EventCode.KEY_TAB),
			new ScanCode(DIK_Q, 0x10, EventCode.KEY_Q), new ScanCode(DIK_W, 0x11, EventCode.KEY_W),
			new ScanCode(DIK_E, 0x12, EventCode.KEY_E), new ScanCode(DIK_R, 0x13, EventCode.KEY_R),
			new ScanCode(DIK_T, 0x14, EventCode.KEY_T), new ScanCode(DIK_Y, 0x15, EventCode.KEY_Y),
			new ScanCode(DIK_U, 0x16, EventCode.KEY_U), new ScanCode(DIK_I, 0x17, EventCode.KEY_I),
			new ScanCode(DIK_O, 0x18, EventCode.KEY_O), new ScanCode(DIK_P, 0x19, EventCode.KEY_P),
			new ScanCode(DIK_LBRACKET, 0x1A, EventCode.KEY_LEFTBRACE),
			new ScanCode(DIK_RBRACKET, 0x1B, EventCode.KEY_RIGHTBRACE),
			new ScanCode(DIK_RETURN, 0x1C, EventCode.KEY_ENTER),
			new ScanCode(DIK_LCONTROL, 0x1D, EventCode.KEY_LEFTCTRL), new ScanCode(DIK_A, 0x1E, EventCode.KEY_A),
			new ScanCode(DIK_S, 0x1F, EventCode.KEY_S), new ScanCode(DIK_D, 0x20, EventCode.KEY_D),
			new ScanCode(DIK_F, 0x21, EventCode.KEY_F), new ScanCode(DIK_G, 0x22, EventCode.KEY_G),
			new ScanCode(DIK_H, 0x23, EventCode.KEY_H), new ScanCode(DIK_J, 0x24, EventCode.KEY_J),
			new ScanCode(DIK_K, 0x25, EventCode.KEY_K), new ScanCode(DIK_L, 0x26, EventCode.KEY_L),
			new ScanCode(DIK_SEMICOLON, 0x27, EventCode.KEY_SEMICOLON),
			new ScanCode(DIK_APOSTROPHE, 0x28, EventCode.KEY_APOSTROPHE),
			new ScanCode(DIK_GRAVE, 0x29, EventCode.KEY_GRAVE), new ScanCode(DIK_LSHIFT, 0x2A, EventCode.KEY_LEFTSHIFT),
			new ScanCode(DIK_BACKSLASH, 0x2B, EventCode.KEY_BACKSLASH), new ScanCode(DIK_Z, 0x2C, EventCode.KEY_Z),
			new ScanCode(DIK_X, 0x2D, EventCode.KEY_X), new ScanCode(DIK_C, 0x2E, EventCode.KEY_C),
			new ScanCode(DIK_V, 0x2F, EventCode.KEY_V), new ScanCode(DIK_B, 0x30, EventCode.KEY_B),
			new ScanCode(DIK_N, 0x31, EventCode.KEY_N), new ScanCode(DIK_M, 0x32, EventCode.KEY_M),
			new ScanCode(DIK_COMMA, 0x33, EventCode.KEY_COMMA), new ScanCode(DIK_PERIOD, 0x34, EventCode.KEY_DOT),
			new ScanCode(DIK_SLASH, 0x35, EventCode.KEY_SLASH),
			new ScanCode(DIK_RSHIFT, 0x36, EventCode.KEY_RIGHTSHIFT),
			new ScanCode(DIK_MULTIPLY, 0x37, EventCode.KEY_KPASTERISK),
			new ScanCode(DIK_LMENU, 0x38, EventCode.KEY_LEFTALT), new ScanCode(DIK_SPACE, 0x39, EventCode.KEY_SPACE),
			new ScanCode(DIK_CAPITAL, 0x3A, EventCode.KEY_CAPSLOCK), new ScanCode(DIK_F1, 0x3B, EventCode.KEY_F1),
			new ScanCode(DIK_F2, 0x3C, EventCode.KEY_F2), new ScanCode(DIK_F3, 0x3D, EventCode.KEY_F3),
			new ScanCode(DIK_F4, 0x3E, EventCode.KEY_F4), new ScanCode(DIK_F5, 0x3F, EventCode.KEY_F5),
			new ScanCode(DIK_F6, 0x40, EventCode.KEY_F6), new ScanCode(DIK_F7, 0x41, EventCode.KEY_F7),
			new ScanCode(DIK_F8, 0x42, EventCode.KEY_F8), new ScanCode(DIK_F9, 0x43, EventCode.KEY_F9),
			new ScanCode(DIK_F10, 0x44, EventCode.KEY_F10), new ScanCode(DIK_NUMLOCK, 0x45, EventCode.KEY_NUMLOCK),
			new ScanCode(DIK_SCROLL, 0x46, EventCode.KEY_SCROLLLOCK),
			new ScanCode(DIK_NUMPAD7, 0x47, EventCode.KEY_KP7), new ScanCode(DIK_NUMPAD8, 0x48, EventCode.KEY_KP8),
			new ScanCode(DIK_NUMPAD9, 0x49, EventCode.KEY_KP9), new ScanCode(DIK_SUBTRACT, 0x4A, EventCode.KEY_KPMINUS),
			new ScanCode(DIK_NUMPAD4, 0x4B, EventCode.KEY_KP4), new ScanCode(DIK_NUMPAD5, 0x4C, EventCode.KEY_KP5),
			new ScanCode(DIK_NUMPAD6, 0x4D, EventCode.KEY_KP6), new ScanCode(DIK_ADD, 0x4E, EventCode.KEY_KPPLUS),
			new ScanCode(DIK_NUMPAD1, 0x4F, EventCode.KEY_KP1), new ScanCode(DIK_NUMPAD2, 0x50, EventCode.KEY_KP2),
			new ScanCode(DIK_NUMPAD3, 0x51, EventCode.KEY_KP3), new ScanCode(DIK_NUMPAD0, 0x52, EventCode.KEY_KP0),
			new ScanCode(DIK_DECIMAL, 0x53, EventCode.KEY_KPDOT), new ScanCode(DIK_OEM_102, 0x56, EventCode.KEY_102ND),
			new ScanCode(DIK_F11, 0x57, EventCode.KEY_F11), new ScanCode(DIK_F12, 0x58, EventCode.KEY_F12),
			new ScanCode(DIK_NUMPADEQUALS, 0x8D, EventCode.KEY_KPEQUAL),
			new ScanCode(DIK_NEXTTRACK, 0x99, EventCode.KEY_NEXTSONG),
			new ScanCode(DIK_NUMPADENTER, 0x9C, EventCode.KEY_KPENTER),
			new ScanCode(DIK_RCONTROL, 0x9D, EventCode.KEY_RIGHTCTRL),
			new ScanCode(DIK_DIVIDE, 0xB5, EventCode.KEY_KPSLASH), new ScanCode(DIK_SYSRQ, 0xB7, EventCode.KEY_SYSRQ),
			new ScanCode(DIK_RMENU, 0xB8, EventCode.KEY_RIGHTALT), new ScanCode(DIK_PAUSE, 0xC5, EventCode.KEY_PAUSE),
			new ScanCode(DIK_HOME, 0xC7, EventCode.KEY_HOME), new ScanCode(DIK_UP, 0xC8, EventCode.KEY_UP),
			new ScanCode(DIK_PRIOR, 0xC9, EventCode.KEY_PAGEUP), new ScanCode(DIK_LEFT, 0xCB, EventCode.KEY_LEFT),
			new ScanCode(DIK_RIGHT, 0xCD, EventCode.KEY_RIGHT), new ScanCode(DIK_END, 0xCF, EventCode.KEY_END),
			new ScanCode(DIK_DOWN, 0xD0, EventCode.KEY_DOWN), new ScanCode(DIK_NEXT, 0xD1, EventCode.KEY_PAGEDOWN),
			new ScanCode(DIK_INSERT, 0xD2, EventCode.KEY_INSERT), new ScanCode(DIK_DELETE, 0xD3, EventCode.KEY_DELETE),
			new ScanCode(DIK_LWIN, 0xDB, EventCode.KEY_LEFTMETA),
			new ScanCode(DIK_RWIN, 0xDC, EventCode.KEY_RIGHTMETA) };

	static {
		final var modifiableNameToScanCodeMap = new TreeMap<String, ScanCode>();
		final var modifiableKeyCodeToScanCodeMap = new HashMap<Integer, ScanCode>();

		for (final ScanCode sc : KEY_CODES) {
			modifiableNameToScanCodeMap.put(sc.name, sc);
			modifiableKeyCodeToScanCodeMap.put(sc.keyCode, sc);
		}

		nameToScanCodeMap = Collections.unmodifiableMap(modifiableNameToScanCodeMap);
		keyCodeToScanCodeMap = Collections.unmodifiableMap(modifiableKeyCodeToScanCodeMap);

		extendedKeyScanCodesSet = keyCodeToScanCodeMap.entrySet().stream().filter(entry -> {
			final var name = entry.getValue().name;
			return DIK_RCONTROL.equals(name) || DIK_RMENU.equals(name) || DIK_INSERT.equals(name)
					|| DIK_DELETE.equals(name) || DIK_HOME.equals(name) || DIK_END.equals(name)
					|| DIK_PRIOR.equals(name) || DIK_NEXT.equals(name) || DIK_UP.equals(name) || DIK_DOWN.equals(name)
					|| DIK_LEFT.equals(name) || DIK_RIGHT.equals(name) || DIK_SYSRQ.equals(name)
					|| DIK_DIVIDE.equals(name) || DIK_NUMPADENTER.equals(name);
		}).map(Entry::getKey).collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public String toString() {
		return name;
	}
}

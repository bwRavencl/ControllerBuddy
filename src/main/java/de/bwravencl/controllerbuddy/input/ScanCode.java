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

package de.bwravencl.controllerbuddy.input;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record ScanCode(String name, int scanCode) {

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
	private static final String DIK_ABNT_C1 = "bzAbnt_C1";
	private static final String DIK_ABNT_C2 = "bzAbnt_C2";
	public static final String DIK_ADD = "Num+";
	public static final String DIK_APOSTROPHE = "'";
	public static final String DIK_APPS = "App Menu";
	private static final String DIK_AT = "pc-98 @";
	private static final String DIK_AX = "jAX";
	public static final String DIK_B = "B";
	public static final String DIK_BACK = "Back";
	public static final String DIK_BACKSLASH = "\\";
	public static final String DIK_C = "C";
	private static final String DIK_CALCULATOR = "Calc";
	private static final String DIK_CAPITAL = "CapsLock";
	private static final String DIK_COLON = "pc-98 :";
	public static final String DIK_COMMA = ",";
	private static final String DIK_CONVERT = "jConvert";
	public static final String DIK_D = "D";
	private static final String DIK_DECIMAL = "Num.";
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
	private static final String DIK_F13 = "pc-98 F13";
	private static final String DIK_F14 = "pc-98 F14";
	private static final String DIK_F15 = "pc-98 F15";
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
	private static final String DIK_KANA = "jKana";
	private static final String DIK_KANJI = "jKanji";
	public static final String DIK_L = "L";
	public static final String DIK_LBRACKET = "[";
	public static final String DIK_LCONTROL = "L Ctrl";
	public static final String DIK_LEFT = "Left Arrow";
	public static final String DIK_LMENU = "L Alt";
	public static final String DIK_LSHIFT = "L Shift";
	public static final String DIK_LWIN = "L Win";
	public static final String DIK_M = "M";
	private static final String DIK_MAIL = "Mail";
	private static final String DIK_MEDIASELECT = "Media Select";
	private static final String DIK_MEDIASTOP = "Stop";
	public static final String DIK_MINUS = "-";
	public static final String DIK_MULTIPLY = "Num*";
	private static final String DIK_MUTE = "Mute";
	private static final String DIK_MYCOMPUTER = "My Computer";
	public static final String DIK_N = "N";
	public static final String DIK_NEXT = "PgDn";
	private static final String DIK_NEXTTRACK = "Next";
	private static final String DIK_NOCONVERT = "jNoConvert";
	private static final String DIK_NUMLOCK = "NumLock";
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
	public static final String DIK_NUMPADCOMMA = "Num,";
	public static final String DIK_NUMPADENTER = "NumEnter";
	private static final String DIK_NUMPADEQUALS = "Num=";
	public static final String DIK_O = "O";
	private static final String DIK_OEM_102 = "OEM_102";
	public static final String DIK_P = "P";
	public static final String DIK_PAUSE = "Pause";
	public static final String DIK_PERIOD = ".";
	private static final String DIK_PLAYPAUSE = "Play/Pause";
	private static final String DIK_POWER = "Power";
	private static final String DIK_PREVTRACK = "Prev";
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
	private static final String DIK_SCROLL = "ScrollLock";
	public static final String DIK_SEMICOLON = ";";
	public static final String DIK_SLASH = "/";
	private static final String DIK_SLEEP = "Sleep";
	public static final String DIK_SPACE = "Space";
	private static final String DIK_STOP = "pc-98 Stop";
	public static final String DIK_SUBTRACT = "Num-";
	public static final String DIK_SYSRQ = "SysRq";
	public static final String DIK_T = "T";
	public static final String DIK_TAB = "Tab";
	public static final String DIK_U = "U";
	private static final String DIK_UNDERLINE = "pc-98 _";
	private static final String DIK_UNLABELED = "J3100";
	public static final String DIK_UP = "Up Arrow";
	public static final String DIK_V = "V";
	private static final String DIK_VOLUMEDOWN = "Vol-";
	private static final String DIK_VOLUMEUP = "Vol+";
	public static final String DIK_W = "W";
	private static final String DIK_WAKE = "Wake";
	private static final String DIK_WEBBACK = "webBack";
	private static final String DIK_WEBFAVORITES = "webFavs";
	private static final String DIK_WEBFORWARD = "webForward";
	private static final String DIK_WEBHOME = "webHome";
	private static final String DIK_WEBREFRESH = "webRefresh";
	private static final String DIK_WEBSEARCH = "webSearch";
	private static final String DIK_WEBSTOP = "webStop";
	public static final String DIK_X = "X";
	public static final String DIK_Y = "Y";
	private static final String DIK_YEN = "jYen";
	public static final String DIK_Z = "Z";

	private static final ScanCode[] KEY_CODES = { new ScanCode(DIK_ESCAPE, 0x1), new ScanCode(DIK_1, 0x2),
			new ScanCode(DIK_2, 0x3), new ScanCode(DIK_3, 0x4), new ScanCode(DIK_4, 0x5), new ScanCode(DIK_5, 0x6),
			new ScanCode(DIK_6, 0x7), new ScanCode(DIK_7, 0x8), new ScanCode(DIK_8, 0x9), new ScanCode(DIK_9, 0xA),
			new ScanCode(DIK_0, 0xB), new ScanCode(DIK_MINUS, 0xC), new ScanCode(DIK_EQUALS, 0xD),
			new ScanCode(DIK_BACK, 0xE), new ScanCode(DIK_TAB, 0xF), new ScanCode(DIK_Q, 0x10),
			new ScanCode(DIK_W, 0x11), new ScanCode(DIK_E, 0x12), new ScanCode(DIK_R, 0x13), new ScanCode(DIK_T, 0x14),
			new ScanCode(DIK_Y, 0x15), new ScanCode(DIK_U, 0x16), new ScanCode(DIK_I, 0x17), new ScanCode(DIK_O, 0x18),
			new ScanCode(DIK_P, 0x19), new ScanCode(DIK_LBRACKET, 0x1A), new ScanCode(DIK_RBRACKET, 0x1B),
			new ScanCode(DIK_RETURN, 0x1C), new ScanCode(DIK_LCONTROL, 0x1D), new ScanCode(DIK_A, 0x1E),
			new ScanCode(DIK_S, 0x1F), new ScanCode(DIK_D, 0x20), new ScanCode(DIK_F, 0x21), new ScanCode(DIK_G, 0x22),
			new ScanCode(DIK_H, 0x23), new ScanCode(DIK_J, 0x24), new ScanCode(DIK_K, 0x25), new ScanCode(DIK_L, 0x26),
			new ScanCode(DIK_SEMICOLON, 0x27), new ScanCode(DIK_APOSTROPHE, 0x28), new ScanCode(DIK_GRAVE, 0x29),
			new ScanCode(DIK_LSHIFT, 0x2A), new ScanCode(DIK_BACKSLASH, 0x2B), new ScanCode(DIK_Z, 0x2C),
			new ScanCode(DIK_X, 0x2D), new ScanCode(DIK_C, 0x2E), new ScanCode(DIK_V, 0x2F), new ScanCode(DIK_B, 0x30),
			new ScanCode(DIK_N, 0x31), new ScanCode(DIK_M, 0x32), new ScanCode(DIK_COMMA, 0x33),
			new ScanCode(DIK_PERIOD, 0x34), new ScanCode(DIK_SLASH, 0x35), new ScanCode(DIK_RSHIFT, 0x36),
			new ScanCode(DIK_MULTIPLY, 0x37), new ScanCode(DIK_LMENU, 0x38), new ScanCode(DIK_SPACE, 0x39),
			new ScanCode(DIK_CAPITAL, 0x3A), new ScanCode(DIK_F1, 0x3B), new ScanCode(DIK_F2, 0x3C),
			new ScanCode(DIK_F3, 0x3D), new ScanCode(DIK_F4, 0x3E), new ScanCode(DIK_F5, 0x3F),
			new ScanCode(DIK_F6, 0x40), new ScanCode(DIK_F7, 0x41), new ScanCode(DIK_F8, 0x42),
			new ScanCode(DIK_F9, 0x43), new ScanCode(DIK_F10, 0x44), new ScanCode(DIK_NUMLOCK, 0x45),
			new ScanCode(DIK_SCROLL, 0x46), new ScanCode(DIK_NUMPAD7, 0x47), new ScanCode(DIK_NUMPAD8, 0x48),
			new ScanCode(DIK_NUMPAD9, 0x49), new ScanCode(DIK_SUBTRACT, 0x4A), new ScanCode(DIK_NUMPAD4, 0x4B),
			new ScanCode(DIK_NUMPAD5, 0x4C), new ScanCode(DIK_NUMPAD6, 0x4D), new ScanCode(DIK_ADD, 0x4E),
			new ScanCode(DIK_NUMPAD1, 0x4F), new ScanCode(DIK_NUMPAD2, 0x50), new ScanCode(DIK_NUMPAD3, 0x51),
			new ScanCode(DIK_NUMPAD0, 0x52), new ScanCode(DIK_DECIMAL, 0x53), new ScanCode(DIK_OEM_102, 0x56),
			new ScanCode(DIK_F11, 0x57), new ScanCode(DIK_F12, 0x58), new ScanCode(DIK_F13, 0x64),
			new ScanCode(DIK_F14, 0x65), new ScanCode(DIK_F15, 0x66), new ScanCode(DIK_KANA, 0x70),
			new ScanCode(DIK_ABNT_C1, 0x73), new ScanCode(DIK_CONVERT, 0x79), new ScanCode(DIK_NOCONVERT, 0x7B),
			new ScanCode(DIK_YEN, 0x7D), new ScanCode(DIK_ABNT_C2, 0x7E), new ScanCode(DIK_NUMPADEQUALS, 0x8D),
			new ScanCode(DIK_PREVTRACK, 0x90), new ScanCode(DIK_AT, 0x91), new ScanCode(DIK_COLON, 0x92),
			new ScanCode(DIK_UNDERLINE, 0x93), new ScanCode(DIK_KANJI, 0x94), new ScanCode(DIK_STOP, 0x95),
			new ScanCode(DIK_AX, 0x96), new ScanCode(DIK_UNLABELED, 0x97), new ScanCode(DIK_NEXTTRACK, 0x99),
			new ScanCode(DIK_NUMPADENTER, 0x9C), new ScanCode(DIK_RCONTROL, 0x9D), new ScanCode(DIK_MUTE, 0xA0),
			new ScanCode(DIK_CALCULATOR, 0xA1), new ScanCode(DIK_PLAYPAUSE, 0xA2), new ScanCode(DIK_MEDIASTOP, 0xA4),
			new ScanCode(DIK_VOLUMEDOWN, 0xAE), new ScanCode(DIK_VOLUMEUP, 0xB0), new ScanCode(DIK_WEBHOME, 0xB2),
			new ScanCode(DIK_NUMPADCOMMA, 0xB3), new ScanCode(DIK_DIVIDE, 0xB5), new ScanCode(DIK_SYSRQ, 0xB7),
			new ScanCode(DIK_RMENU, 0xB8), new ScanCode(DIK_PAUSE, 0xC5), new ScanCode(DIK_HOME, 0xC7),
			new ScanCode(DIK_UP, 0xC8), new ScanCode(DIK_PRIOR, 0xC9), new ScanCode(DIK_LEFT, 0xCB),
			new ScanCode(DIK_RIGHT, 0xCD), new ScanCode(DIK_END, 0xCF), new ScanCode(DIK_DOWN, 0xD0),
			new ScanCode(DIK_NEXT, 0xD1), new ScanCode(DIK_INSERT, 0xD2), new ScanCode(DIK_DELETE, 0xD3),
			new ScanCode(DIK_LWIN, 0xDB), new ScanCode(DIK_RWIN, 0xDC), new ScanCode(DIK_APPS, 0xDD),
			new ScanCode(DIK_POWER, 0xDE), new ScanCode(DIK_SLEEP, 0xDF), new ScanCode(DIK_WAKE, 0xE3),
			new ScanCode(DIK_WEBSEARCH, 0xE5), new ScanCode(DIK_WEBFAVORITES, 0xE6), new ScanCode(DIK_WEBREFRESH, 0xE7),
			new ScanCode(DIK_WEBSTOP, 0xE8), new ScanCode(DIK_WEBFORWARD, 0xE9), new ScanCode(DIK_WEBBACK, 0xEA),
			new ScanCode(DIK_MYCOMPUTER, 0xEB), new ScanCode(DIK_MAIL, 0xEC), new ScanCode(DIK_MEDIASELECT, 0xED) };

	public static final Map<String, Integer> nameToKeyCodeMap;
	public static final Map<Integer, String> keyCodeToNameMap;
	public static final Set<Integer> extendedKeyScanCodesSet;

	static {
		final var modifiableNameToKeyCodeMap = new TreeMap<String, Integer>();
		final var modifiableKeyCodeToNameMap = new HashMap<Integer, String>();

		for (final ScanCode sc : KEY_CODES) {
			modifiableNameToKeyCodeMap.put(sc.name, sc.scanCode);
			modifiableKeyCodeToNameMap.put(sc.scanCode, sc.name);
		}

		nameToKeyCodeMap = Collections.unmodifiableMap(modifiableNameToKeyCodeMap);
		keyCodeToNameMap = Collections.unmodifiableMap(modifiableKeyCodeToNameMap);

		extendedKeyScanCodesSet = keyCodeToNameMap.entrySet().stream().filter(entry -> {
			final var name = entry.getValue();
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

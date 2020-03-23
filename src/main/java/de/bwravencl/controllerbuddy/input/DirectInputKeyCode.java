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
import java.util.TreeMap;

public final class DirectInputKeyCode {

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

	private static final DirectInputKeyCode[] KEY_CODES = { new DirectInputKeyCode(DIK_ESCAPE, 0x1),
			new DirectInputKeyCode(DIK_1, 0x2), new DirectInputKeyCode(DIK_2, 0x3), new DirectInputKeyCode(DIK_3, 0x4),
			new DirectInputKeyCode(DIK_4, 0x5), new DirectInputKeyCode(DIK_5, 0x6), new DirectInputKeyCode(DIK_6, 0x7),
			new DirectInputKeyCode(DIK_7, 0x8), new DirectInputKeyCode(DIK_8, 0x9), new DirectInputKeyCode(DIK_9, 0xA),
			new DirectInputKeyCode(DIK_0, 0xB), new DirectInputKeyCode(DIK_MINUS, 0xC),
			new DirectInputKeyCode(DIK_EQUALS, 0xD), new DirectInputKeyCode(DIK_BACK, 0xE),
			new DirectInputKeyCode(DIK_TAB, 0xF), new DirectInputKeyCode(DIK_Q, 0x10),
			new DirectInputKeyCode(DIK_W, 0x11), new DirectInputKeyCode(DIK_E, 0x12),
			new DirectInputKeyCode(DIK_R, 0x13), new DirectInputKeyCode(DIK_T, 0x14),
			new DirectInputKeyCode(DIK_Y, 0x15), new DirectInputKeyCode(DIK_U, 0x16),
			new DirectInputKeyCode(DIK_I, 0x17), new DirectInputKeyCode(DIK_O, 0x18),
			new DirectInputKeyCode(DIK_P, 0x19), new DirectInputKeyCode(DIK_LBRACKET, 0x1A),
			new DirectInputKeyCode(DIK_RBRACKET, 0x1B), new DirectInputKeyCode(DIK_RETURN, 0x1C),
			new DirectInputKeyCode(DIK_LCONTROL, 0x1D), new DirectInputKeyCode(DIK_A, 0x1E),
			new DirectInputKeyCode(DIK_S, 0x1F), new DirectInputKeyCode(DIK_D, 0x20),
			new DirectInputKeyCode(DIK_F, 0x21), new DirectInputKeyCode(DIK_G, 0x22),
			new DirectInputKeyCode(DIK_H, 0x23), new DirectInputKeyCode(DIK_J, 0x24),
			new DirectInputKeyCode(DIK_K, 0x25), new DirectInputKeyCode(DIK_L, 0x26),
			new DirectInputKeyCode(DIK_SEMICOLON, 0x27), new DirectInputKeyCode(DIK_APOSTROPHE, 0x28),
			new DirectInputKeyCode(DIK_GRAVE, 0x29), new DirectInputKeyCode(DIK_LSHIFT, 0x2A),
			new DirectInputKeyCode(DIK_BACKSLASH, 0x2B), new DirectInputKeyCode(DIK_Z, 0x2C),
			new DirectInputKeyCode(DIK_X, 0x2D), new DirectInputKeyCode(DIK_C, 0x2E),
			new DirectInputKeyCode(DIK_V, 0x2F), new DirectInputKeyCode(DIK_B, 0x30),
			new DirectInputKeyCode(DIK_N, 0x31), new DirectInputKeyCode(DIK_M, 0x32),
			new DirectInputKeyCode(DIK_COMMA, 0x33), new DirectInputKeyCode(DIK_PERIOD, 0x34),
			new DirectInputKeyCode(DIK_SLASH, 0x35), new DirectInputKeyCode(DIK_RSHIFT, 0x36),
			new DirectInputKeyCode(DIK_MULTIPLY, 0x37), new DirectInputKeyCode(DIK_LMENU, 0x38),
			new DirectInputKeyCode(DIK_SPACE, 0x39), new DirectInputKeyCode(DIK_CAPITAL, 0x3A),
			new DirectInputKeyCode(DIK_F1, 0x3B), new DirectInputKeyCode(DIK_F2, 0x3C),
			new DirectInputKeyCode(DIK_F3, 0x3D), new DirectInputKeyCode(DIK_F4, 0x3E),
			new DirectInputKeyCode(DIK_F5, 0x3F), new DirectInputKeyCode(DIK_F6, 0x40),
			new DirectInputKeyCode(DIK_F7, 0x41), new DirectInputKeyCode(DIK_F8, 0x42),
			new DirectInputKeyCode(DIK_F9, 0x43), new DirectInputKeyCode(DIK_F10, 0x44),
			new DirectInputKeyCode(DIK_NUMLOCK, 0x45), new DirectInputKeyCode(DIK_SCROLL, 0x46),
			new DirectInputKeyCode(DIK_NUMPAD7, 0x47), new DirectInputKeyCode(DIK_NUMPAD8, 0x48),
			new DirectInputKeyCode(DIK_NUMPAD9, 0x49), new DirectInputKeyCode(DIK_SUBTRACT, 0x4A),
			new DirectInputKeyCode(DIK_NUMPAD4, 0x4B), new DirectInputKeyCode(DIK_NUMPAD5, 0x4C),
			new DirectInputKeyCode(DIK_NUMPAD6, 0x4D), new DirectInputKeyCode(DIK_ADD, 0x4E),
			new DirectInputKeyCode(DIK_NUMPAD1, 0x4F), new DirectInputKeyCode(DIK_NUMPAD2, 0x50),
			new DirectInputKeyCode(DIK_NUMPAD3, 0x51), new DirectInputKeyCode(DIK_NUMPAD0, 0x52),
			new DirectInputKeyCode(DIK_DECIMAL, 0x53), new DirectInputKeyCode(DIK_OEM_102, 0x56),
			new DirectInputKeyCode(DIK_F11, 0x57), new DirectInputKeyCode(DIK_F12, 0x58),
			new DirectInputKeyCode(DIK_F13, 0x64), new DirectInputKeyCode(DIK_F14, 0x65),
			new DirectInputKeyCode(DIK_F15, 0x66), new DirectInputKeyCode(DIK_KANA, 0x70),
			new DirectInputKeyCode(DIK_ABNT_C1, 0x73), new DirectInputKeyCode(DIK_CONVERT, 0x79),
			new DirectInputKeyCode(DIK_NOCONVERT, 0x7B), new DirectInputKeyCode(DIK_YEN, 0x7D),
			new DirectInputKeyCode(DIK_ABNT_C2, 0x7E), new DirectInputKeyCode(DIK_NUMPADEQUALS, 0x8D),
			new DirectInputKeyCode(DIK_PREVTRACK, 0x90), new DirectInputKeyCode(DIK_AT, 0x91),
			new DirectInputKeyCode(DIK_COLON, 0x92), new DirectInputKeyCode(DIK_UNDERLINE, 0x93),
			new DirectInputKeyCode(DIK_KANJI, 0x94), new DirectInputKeyCode(DIK_STOP, 0x95),
			new DirectInputKeyCode(DIK_AX, 0x96), new DirectInputKeyCode(DIK_UNLABELED, 0x97),
			new DirectInputKeyCode(DIK_NEXTTRACK, 0x99), new DirectInputKeyCode(DIK_NUMPADENTER, 0x9C),
			new DirectInputKeyCode(DIK_RCONTROL, 0x9D), new DirectInputKeyCode(DIK_MUTE, 0xA0),
			new DirectInputKeyCode(DIK_CALCULATOR, 0xA1), new DirectInputKeyCode(DIK_PLAYPAUSE, 0xA2),
			new DirectInputKeyCode(DIK_MEDIASTOP, 0xA4), new DirectInputKeyCode(DIK_VOLUMEDOWN, 0xAE),
			new DirectInputKeyCode(DIK_VOLUMEUP, 0xB0), new DirectInputKeyCode(DIK_WEBHOME, 0xB2),
			new DirectInputKeyCode(DIK_NUMPADCOMMA, 0xB3), new DirectInputKeyCode(DIK_DIVIDE, 0xB5),
			new DirectInputKeyCode(DIK_SYSRQ, 0xB7), new DirectInputKeyCode(DIK_RMENU, 0xB8),
			new DirectInputKeyCode(DIK_PAUSE, 0xC5), new DirectInputKeyCode(DIK_HOME, 0xC7),
			new DirectInputKeyCode(DIK_UP, 0xC8), new DirectInputKeyCode(DIK_PRIOR, 0xC9),
			new DirectInputKeyCode(DIK_LEFT, 0xCB), new DirectInputKeyCode(DIK_RIGHT, 0xCD),
			new DirectInputKeyCode(DIK_END, 0xCF), new DirectInputKeyCode(DIK_DOWN, 0xD0),
			new DirectInputKeyCode(DIK_NEXT, 0xD1), new DirectInputKeyCode(DIK_INSERT, 0xD2),
			new DirectInputKeyCode(DIK_DELETE, 0xD3), new DirectInputKeyCode(DIK_LWIN, 0xDB),
			new DirectInputKeyCode(DIK_RWIN, 0xDC), new DirectInputKeyCode(DIK_APPS, 0xDD),
			new DirectInputKeyCode(DIK_POWER, 0xDE), new DirectInputKeyCode(DIK_SLEEP, 0xDF),
			new DirectInputKeyCode(DIK_WAKE, 0xE3), new DirectInputKeyCode(DIK_WEBSEARCH, 0xE5),
			new DirectInputKeyCode(DIK_WEBFAVORITES, 0xE6), new DirectInputKeyCode(DIK_WEBREFRESH, 0xE7),
			new DirectInputKeyCode(DIK_WEBSTOP, 0xE8), new DirectInputKeyCode(DIK_WEBFORWARD, 0xE9),
			new DirectInputKeyCode(DIK_WEBBACK, 0xEA), new DirectInputKeyCode(DIK_MYCOMPUTER, 0xEB),
			new DirectInputKeyCode(DIK_MAIL, 0xEC), new DirectInputKeyCode(DIK_MEDIASELECT, 0xED) };

	public static final Map<String, Integer> nameToKeyCodeMap;
	public static final Map<Integer, String> keyCodeToNameMap;

	static {
		final var modifiableNameToKeyCodeMap = new TreeMap<String, Integer>();
		final var modifiableKeyCodeToNameMap = new HashMap<Integer, String>();

		for (final DirectInputKeyCode sc : KEY_CODES) {
			modifiableNameToKeyCodeMap.put(sc.name, sc.keyCode);
			modifiableKeyCodeToNameMap.put(sc.keyCode, sc.name);
		}

		nameToKeyCodeMap = Collections.unmodifiableMap(modifiableNameToKeyCodeMap);
		keyCodeToNameMap = Collections.unmodifiableMap(modifiableKeyCodeToNameMap);
	}

	private final int keyCode;
	private final String name;

	private DirectInputKeyCode(final String name, final int DirectInputKeyCode) {
		this.name = name;
		this.keyCode = DirectInputKeyCode;
	}

	@Override
	public String toString() {
		return name;
	}
}

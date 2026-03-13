/*
 * Copyright (C) 2015 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input;

import de.bwravencl.controllerbuddy.runmode.UinputDevice.Event;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/// Represents a keyboard scan code mapping a display name to a DirectInput key
/// code and a Linux input event.
///
/// Instances are held in a static registry and are used throughout the input
/// pipeline to translate between platform-specific key identifiers.
///
/// @param name the human-readable display name for the key
/// @param keyCode the DirectInput scan code value
/// @param event the corresponding Linux uinput event
public record ScanCode(String name, int keyCode, Event event) {

	/// '0' key
	public static final String DIK_0 = "0";

	/// '1' key
	public static final String DIK_1 = "1";

	/// '2' key
	public static final String DIK_2 = "2";

	/// '3' key
	public static final String DIK_3 = "3";

	/// '4' key
	public static final String DIK_4 = "4";

	/// '5' key
	public static final String DIK_5 = "5";

	/// '6' key
	public static final String DIK_6 = "6";

	/// '7' key
	public static final String DIK_7 = "7";

	/// '8' key
	public static final String DIK_8 = "8";

	/// '9' key
	public static final String DIK_9 = "9";

	/// 'A' key
	public static final String DIK_A = "A";

	/// Numeric keypad plus ('+') key
	public static final String DIK_ADD = "Num+";

	/// Apostrophe ("'") key
	public static final String DIK_APOSTROPHE = "'";

	/// 'B' key
	public static final String DIK_B = "B";

	/// Backspace key
	public static final String DIK_BACK = "Back";

	/// Backslash ("\") key
	public static final String DIK_BACKSLASH = "\\";

	/// 'C' key
	public static final String DIK_C = "C";

	/// Comma (",") key
	public static final String DIK_COMMA = ",";

	/// 'D' key
	public static final String DIK_D = "D";

	/// Numeric keypad decimal (".") key
	public static final String DIK_DECIMAL = "Num.";

	/// Delete key
	public static final String DIK_DELETE = "Del";

	/// Numeric keypad divide ("/") key
	public static final String DIK_DIVIDE = "Num/";

	/// Down arrow key
	public static final String DIK_DOWN = "Down Arrow";

	/// 'E' key
	public static final String DIK_E = "E";

	/// End key
	public static final String DIK_END = "End";

	/// Equals ("=") key
	public static final String DIK_EQUALS = "=";

	/// Escape key
	public static final String DIK_ESCAPE = "Esc";

	/// 'F' key
	public static final String DIK_F = "F";

	/// Function key F1
	public static final String DIK_F1 = "F1";

	/// Function key F10
	public static final String DIK_F10 = "F10";

	/// Function key F11
	public static final String DIK_F11 = "F11";

	/// Function key F12
	public static final String DIK_F12 = "F12";

	/// Function key F2
	public static final String DIK_F2 = "F2";

	/// Function key F3
	public static final String DIK_F3 = "F3";

	/// Function key F4
	public static final String DIK_F4 = "F4";

	/// Function key F5
	public static final String DIK_F5 = "F5";

	/// Function key F6
	public static final String DIK_F6 = "F6";

	/// Function key F7
	public static final String DIK_F7 = "F7";

	/// Function key F8
	public static final String DIK_F8 = "F8";

	/// Function key F9
	public static final String DIK_F9 = "F9";

	/// 'G' key
	public static final String DIK_G = "G";

	/// Grave ("`") key
	public static final String DIK_GRAVE = "`";

	/// 'H' key
	public static final String DIK_H = "H";

	/// Home key
	public static final String DIK_HOME = "Home";

	/// 'I' key
	public static final String DIK_I = "I";

	/// Insert key
	public static final String DIK_INSERT = "Ins";

	/// 'J' key
	public static final String DIK_J = "J";

	/// 'K' key
	public static final String DIK_K = "K";

	/// 'L' key
	public static final String DIK_L = "L";

	/// Left bracket ("[") key
	public static final String DIK_LBRACKET = "[";

	/// Left control key
	public static final String DIK_LCONTROL = "L Ctrl";

	/// Left arrow key
	public static final String DIK_LEFT = "Left Arrow";

	/// Left alt / menu key
	public static final String DIK_LMENU = "L Alt";

	/// Left shift key
	public static final String DIK_LSHIFT = "L Shift";

	/// Left Windows / Meta key
	public static final String DIK_LWIN = "L Win";

	/// 'M' key
	public static final String DIK_M = "M";

	/// Minus ("-") key
	public static final String DIK_MINUS = "-";

	/// Numeric keypad multiply ("*") key
	public static final String DIK_MULTIPLY = "Num*";

	/// 'N' key
	public static final String DIK_N = "N";

	/// Page down key
	public static final String DIK_NEXT = "PgDn";

	/// Numeric keypad 0
	public static final String DIK_NUMPAD0 = "Num0";

	/// Numeric keypad 1
	public static final String DIK_NUMPAD1 = "Num1";

	/// Numeric keypad 2
	public static final String DIK_NUMPAD2 = "Num2";

	/// Numeric keypad 3
	public static final String DIK_NUMPAD3 = "Num3";

	/// Numeric keypad 4
	public static final String DIK_NUMPAD4 = "Num4";

	/// Numeric keypad 5
	public static final String DIK_NUMPAD5 = "Num5";

	/// Numeric keypad 6
	public static final String DIK_NUMPAD6 = "Num6";

	/// Numeric keypad 7
	public static final String DIK_NUMPAD7 = "Num7";

	/// Numeric keypad 8
	public static final String DIK_NUMPAD8 = "Num8";

	/// Numeric keypad 9
	public static final String DIK_NUMPAD9 = "Num9";

	/// Numeric keypad enter key
	public static final String DIK_NUMPADENTER = "NumEnter";

	/// 'O' key
	public static final String DIK_O = "O";

	/// 'P' key
	public static final String DIK_P = "P";

	/// Pause key
	public static final String DIK_PAUSE = "Pause";

	/// Period (".") key
	public static final String DIK_PERIOD = ".";

	/// Page up key
	public static final String DIK_PRIOR = "PgUp";

	/// 'Q' key
	public static final String DIK_Q = "Q";

	/// 'R' key
	public static final String DIK_R = "R";

	/// Right bracket ("]") key
	public static final String DIK_RBRACKET = "]";

	/// Right control key
	public static final String DIK_RCONTROL = "R Ctrl";

	/// Return / Enter key
	public static final String DIK_RETURN = "Return";

	/// Right arrow key
	public static final String DIK_RIGHT = "Right Arrow";

	/// Right alt / menu key
	public static final String DIK_RMENU = "R Alt";

	/// Right shift key
	public static final String DIK_RSHIFT = "R Shift";

	/// Right Windows / Meta key
	public static final String DIK_RWIN = "R Win";

	/// 'S' key
	public static final String DIK_S = "S";

	/// Semicolon (";") key
	public static final String DIK_SEMICOLON = ";";

	/// Slash ("/") key
	public static final String DIK_SLASH = "/";

	/// Space bar
	public static final String DIK_SPACE = "Space";

	/// Numeric keypad subtract ("-") key
	public static final String DIK_SUBTRACT = "Num-";

	/// System Request / Print Screen key
	public static final String DIK_SYSRQ = "SysRq";

	/// 'T' key
	public static final String DIK_T = "T";

	/// Tab key
	public static final String DIK_TAB = "Tab";

	/// 'U' key
	public static final String DIK_U = "U";

	/// Up arrow key
	public static final String DIK_UP = "Up Arrow";

	/// 'V' key
	public static final String DIK_V = "V";

	/// 'W' key
	public static final String DIK_W = "W";

	/// 'X' key
	public static final String DIK_X = "X";

	/// 'Y' key
	public static final String DIK_Y = "Y";

	/// 'Z' key
	public static final String DIK_Z = "Z";

	/// Set of scan codes that correspond to extended keys.
	public static final Set<Integer> EXTENDED_KEY_SCAN_CODES_SET;

	/// Map from AWT key codes to their corresponding scan code instances.
	public static final Map<Integer, ScanCode> KEY_CODE_TO_SCAN_CODE_MAP;

	/// Map from key names to their corresponding scan code instances.
	public static final Map<String, ScanCode> NAME_TO_SCAN_CODE_MAP;

	private static final String DIK_CAPITAL = "CapsLock";

	private static final String DIK_NEXTTRACK = "Next";

	private static final String DIK_NUMLOCK = "NumLock";

	private static final String DIK_NUMPADEQUALS = "Num=";

	private static final String DIK_OEM_102 = "OEM_102";

	private static final String DIK_SCROLL = "ScrollLock";

	private static final ScanCode[] KEY_CODES = { new ScanCode(DIK_ESCAPE, 0x1, Event.KEY_ESC),
			new ScanCode(DIK_1, 0x2, Event.KEY_1), new ScanCode(DIK_2, 0x3, Event.KEY_2),
			new ScanCode(DIK_3, 0x4, Event.KEY_3), new ScanCode(DIK_4, 0x5, Event.KEY_4),
			new ScanCode(DIK_5, 0x6, Event.KEY_5), new ScanCode(DIK_6, 0x7, Event.KEY_6),
			new ScanCode(DIK_7, 0x8, Event.KEY_7), new ScanCode(DIK_8, 0x9, Event.KEY_8),
			new ScanCode(DIK_9, 0xA, Event.KEY_9), new ScanCode(DIK_0, 0xB, Event.KEY_0),
			new ScanCode(DIK_MINUS, 0xC, Event.KEY_MINUS), new ScanCode(DIK_EQUALS, 0xD, Event.KEY_EQUAL),
			new ScanCode(DIK_BACK, 0xE, Event.KEY_BACKSPACE), new ScanCode(DIK_TAB, 0xF, Event.KEY_TAB),
			new ScanCode(DIK_Q, 0x10, Event.KEY_Q), new ScanCode(DIK_W, 0x11, Event.KEY_W),
			new ScanCode(DIK_E, 0x12, Event.KEY_E), new ScanCode(DIK_R, 0x13, Event.KEY_R),
			new ScanCode(DIK_T, 0x14, Event.KEY_T), new ScanCode(DIK_Y, 0x15, Event.KEY_Y),
			new ScanCode(DIK_U, 0x16, Event.KEY_U), new ScanCode(DIK_I, 0x17, Event.KEY_I),
			new ScanCode(DIK_O, 0x18, Event.KEY_O), new ScanCode(DIK_P, 0x19, Event.KEY_P),
			new ScanCode(DIK_LBRACKET, 0x1A, Event.KEY_LEFTBRACE),
			new ScanCode(DIK_RBRACKET, 0x1B, Event.KEY_RIGHTBRACE), new ScanCode(DIK_RETURN, 0x1C, Event.KEY_ENTER),
			new ScanCode(DIK_LCONTROL, 0x1D, Event.KEY_LEFTCTRL), new ScanCode(DIK_A, 0x1E, Event.KEY_A),
			new ScanCode(DIK_S, 0x1F, Event.KEY_S), new ScanCode(DIK_D, 0x20, Event.KEY_D),
			new ScanCode(DIK_F, 0x21, Event.KEY_F), new ScanCode(DIK_G, 0x22, Event.KEY_G),
			new ScanCode(DIK_H, 0x23, Event.KEY_H), new ScanCode(DIK_J, 0x24, Event.KEY_J),
			new ScanCode(DIK_K, 0x25, Event.KEY_K), new ScanCode(DIK_L, 0x26, Event.KEY_L),
			new ScanCode(DIK_SEMICOLON, 0x27, Event.KEY_SEMICOLON),
			new ScanCode(DIK_APOSTROPHE, 0x28, Event.KEY_APOSTROPHE), new ScanCode(DIK_GRAVE, 0x29, Event.KEY_GRAVE),
			new ScanCode(DIK_LSHIFT, 0x2A, Event.KEY_LEFTSHIFT), new ScanCode(DIK_BACKSLASH, 0x2B, Event.KEY_BACKSLASH),
			new ScanCode(DIK_Z, 0x2C, Event.KEY_Z), new ScanCode(DIK_X, 0x2D, Event.KEY_X),
			new ScanCode(DIK_C, 0x2E, Event.KEY_C), new ScanCode(DIK_V, 0x2F, Event.KEY_V),
			new ScanCode(DIK_B, 0x30, Event.KEY_B), new ScanCode(DIK_N, 0x31, Event.KEY_N),
			new ScanCode(DIK_M, 0x32, Event.KEY_M), new ScanCode(DIK_COMMA, 0x33, Event.KEY_COMMA),
			new ScanCode(DIK_PERIOD, 0x34, Event.KEY_DOT), new ScanCode(DIK_SLASH, 0x35, Event.KEY_SLASH),
			new ScanCode(DIK_RSHIFT, 0x36, Event.KEY_RIGHTSHIFT),
			new ScanCode(DIK_MULTIPLY, 0x37, Event.KEY_KPASTERISK), new ScanCode(DIK_LMENU, 0x38, Event.KEY_LEFTALT),
			new ScanCode(DIK_SPACE, 0x39, Event.KEY_SPACE), new ScanCode(DIK_CAPITAL, 0x3A, Event.KEY_CAPSLOCK),
			new ScanCode(DIK_F1, 0x3B, Event.KEY_F1), new ScanCode(DIK_F2, 0x3C, Event.KEY_F2),
			new ScanCode(DIK_F3, 0x3D, Event.KEY_F3), new ScanCode(DIK_F4, 0x3E, Event.KEY_F4),
			new ScanCode(DIK_F5, 0x3F, Event.KEY_F5), new ScanCode(DIK_F6, 0x40, Event.KEY_F6),
			new ScanCode(DIK_F7, 0x41, Event.KEY_F7), new ScanCode(DIK_F8, 0x42, Event.KEY_F8),
			new ScanCode(DIK_F9, 0x43, Event.KEY_F9), new ScanCode(DIK_F10, 0x44, Event.KEY_F10),
			new ScanCode(DIK_NUMLOCK, 0x45, Event.KEY_NUMLOCK), new ScanCode(DIK_SCROLL, 0x46, Event.KEY_SCROLLLOCK),
			new ScanCode(DIK_NUMPAD7, 0x47, Event.KEY_KP7), new ScanCode(DIK_NUMPAD8, 0x48, Event.KEY_KP8),
			new ScanCode(DIK_NUMPAD9, 0x49, Event.KEY_KP9), new ScanCode(DIK_SUBTRACT, 0x4A, Event.KEY_KPMINUS),
			new ScanCode(DIK_NUMPAD4, 0x4B, Event.KEY_KP4), new ScanCode(DIK_NUMPAD5, 0x4C, Event.KEY_KP5),
			new ScanCode(DIK_NUMPAD6, 0x4D, Event.KEY_KP6), new ScanCode(DIK_ADD, 0x4E, Event.KEY_KPPLUS),
			new ScanCode(DIK_NUMPAD1, 0x4F, Event.KEY_KP1), new ScanCode(DIK_NUMPAD2, 0x50, Event.KEY_KP2),
			new ScanCode(DIK_NUMPAD3, 0x51, Event.KEY_KP3), new ScanCode(DIK_NUMPAD0, 0x52, Event.KEY_KP0),
			new ScanCode(DIK_DECIMAL, 0x53, Event.KEY_KPDOT), new ScanCode(DIK_OEM_102, 0x56, Event.KEY_102ND),
			new ScanCode(DIK_F11, 0x57, Event.KEY_F11), new ScanCode(DIK_F12, 0x58, Event.KEY_F12),
			new ScanCode(DIK_NUMPADEQUALS, 0x8D, Event.KEY_KPEQUAL),
			new ScanCode(DIK_NEXTTRACK, 0x99, Event.KEY_NEXTSONG),
			new ScanCode(DIK_NUMPADENTER, 0x9C, Event.KEY_KPENTER),
			new ScanCode(DIK_RCONTROL, 0x9D, Event.KEY_RIGHTCTRL), new ScanCode(DIK_DIVIDE, 0xB5, Event.KEY_KPSLASH),
			new ScanCode(DIK_SYSRQ, 0xB7, Event.KEY_SYSRQ), new ScanCode(DIK_RMENU, 0xB8, Event.KEY_RIGHTALT),
			new ScanCode(DIK_PAUSE, 0xC5, Event.KEY_PAUSE), new ScanCode(DIK_HOME, 0xC7, Event.KEY_HOME),
			new ScanCode(DIK_UP, 0xC8, Event.KEY_UP), new ScanCode(DIK_PRIOR, 0xC9, Event.KEY_PAGEUP),
			new ScanCode(DIK_LEFT, 0xCB, Event.KEY_LEFT), new ScanCode(DIK_RIGHT, 0xCD, Event.KEY_RIGHT),
			new ScanCode(DIK_END, 0xCF, Event.KEY_END), new ScanCode(DIK_DOWN, 0xD0, Event.KEY_DOWN),
			new ScanCode(DIK_NEXT, 0xD1, Event.KEY_PAGEDOWN), new ScanCode(DIK_INSERT, 0xD2, Event.KEY_INSERT),
			new ScanCode(DIK_DELETE, 0xD3, Event.KEY_DELETE), new ScanCode(DIK_LWIN, 0xDB, Event.KEY_LEFTMETA),
			new ScanCode(DIK_RWIN, 0xDC, Event.KEY_RIGHTMETA) };

	static {
		final var modifiableNameToScanCodeMap = new TreeMap<String, ScanCode>();
		final var modifiableKeyCodeToScanCodeMap = new HashMap<Integer, ScanCode>();

		for (final var scanCode : KEY_CODES) {
			modifiableNameToScanCodeMap.put(scanCode.name, scanCode);
			modifiableKeyCodeToScanCodeMap.put(scanCode.keyCode, scanCode);
		}

		NAME_TO_SCAN_CODE_MAP = Collections.unmodifiableMap(modifiableNameToScanCodeMap);
		KEY_CODE_TO_SCAN_CODE_MAP = Collections.unmodifiableMap(modifiableKeyCodeToScanCodeMap);

		EXTENDED_KEY_SCAN_CODES_SET = KEY_CODE_TO_SCAN_CODE_MAP.entrySet().stream().filter(entry -> {
			final var name = entry.getValue().name;
			return DIK_RCONTROL.equals(name) || DIK_RMENU.equals(name) || DIK_INSERT.equals(name)
					|| DIK_DELETE.equals(name) || DIK_HOME.equals(name) || DIK_END.equals(name)
					|| DIK_PRIOR.equals(name) || DIK_NEXT.equals(name) || DIK_UP.equals(name) || DIK_DOWN.equals(name)
					|| DIK_LEFT.equals(name) || DIK_RIGHT.equals(name) || DIK_SYSRQ.equals(name)
					|| DIK_DIVIDE.equals(name) || DIK_NUMPADENTER.equals(name);
		}).map(Entry::getKey).collect(Collectors.toUnmodifiableSet());
	}

	/// Returns the display name of this scan code.
	///
	/// @return the human-readable key name
	@Override
	public String toString() {
		return name;
	}
}

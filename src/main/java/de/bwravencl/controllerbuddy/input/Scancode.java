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

/// Represents a keyboard scancode mapping a display name to a DirectInput key
/// code and a Linux input event.
///
/// Instances are held in a static registry and are used throughout the input
/// pipeline to translate between platform-specific key identifiers.
///
/// @param name the human-readable display name for the key
/// @param keyCode the DirectInput scancode value
/// @param event the corresponding Linux uinput event
public record Scancode(String name, int keyCode, Event event) {

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

	/// Set of scancodes that correspond to extended keys.
	public static final Set<Integer> EXTENDED_KEY_SCAN_CODES_SET;

	/// Map from AWT key codes to their corresponding scancode instances.
	public static final Map<Integer, Scancode> KEY_CODE_TO_SCAN_CODE_MAP;

	/// Map from key names to their corresponding scancode instances.
	public static final Map<String, Scancode> NAME_TO_SCAN_CODE_MAP;

	private static final String DIK_CAPITAL = "CapsLock";

	private static final String DIK_NEXTTRACK = "Next";

	private static final String DIK_NUMLOCK = "NumLock";

	private static final String DIK_NUMPADEQUALS = "Num=";

	private static final String DIK_OEM_102 = "OEM_102";

	private static final String DIK_SCROLL = "ScrollLock";

	private static final Scancode[] KEY_CODES = { new Scancode(DIK_ESCAPE, 0x1, Event.KEY_ESC),
			new Scancode(DIK_1, 0x2, Event.KEY_1), new Scancode(DIK_2, 0x3, Event.KEY_2),
			new Scancode(DIK_3, 0x4, Event.KEY_3), new Scancode(DIK_4, 0x5, Event.KEY_4),
			new Scancode(DIK_5, 0x6, Event.KEY_5), new Scancode(DIK_6, 0x7, Event.KEY_6),
			new Scancode(DIK_7, 0x8, Event.KEY_7), new Scancode(DIK_8, 0x9, Event.KEY_8),
			new Scancode(DIK_9, 0xA, Event.KEY_9), new Scancode(DIK_0, 0xB, Event.KEY_0),
			new Scancode(DIK_MINUS, 0xC, Event.KEY_MINUS), new Scancode(DIK_EQUALS, 0xD, Event.KEY_EQUAL),
			new Scancode(DIK_BACK, 0xE, Event.KEY_BACKSPACE), new Scancode(DIK_TAB, 0xF, Event.KEY_TAB),
			new Scancode(DIK_Q, 0x10, Event.KEY_Q), new Scancode(DIK_W, 0x11, Event.KEY_W),
			new Scancode(DIK_E, 0x12, Event.KEY_E), new Scancode(DIK_R, 0x13, Event.KEY_R),
			new Scancode(DIK_T, 0x14, Event.KEY_T), new Scancode(DIK_Y, 0x15, Event.KEY_Y),
			new Scancode(DIK_U, 0x16, Event.KEY_U), new Scancode(DIK_I, 0x17, Event.KEY_I),
			new Scancode(DIK_O, 0x18, Event.KEY_O), new Scancode(DIK_P, 0x19, Event.KEY_P),
			new Scancode(DIK_LBRACKET, 0x1A, Event.KEY_LEFTBRACE),
			new Scancode(DIK_RBRACKET, 0x1B, Event.KEY_RIGHTBRACE), new Scancode(DIK_RETURN, 0x1C, Event.KEY_ENTER),
			new Scancode(DIK_LCONTROL, 0x1D, Event.KEY_LEFTCTRL), new Scancode(DIK_A, 0x1E, Event.KEY_A),
			new Scancode(DIK_S, 0x1F, Event.KEY_S), new Scancode(DIK_D, 0x20, Event.KEY_D),
			new Scancode(DIK_F, 0x21, Event.KEY_F), new Scancode(DIK_G, 0x22, Event.KEY_G),
			new Scancode(DIK_H, 0x23, Event.KEY_H), new Scancode(DIK_J, 0x24, Event.KEY_J),
			new Scancode(DIK_K, 0x25, Event.KEY_K), new Scancode(DIK_L, 0x26, Event.KEY_L),
			new Scancode(DIK_SEMICOLON, 0x27, Event.KEY_SEMICOLON),
			new Scancode(DIK_APOSTROPHE, 0x28, Event.KEY_APOSTROPHE), new Scancode(DIK_GRAVE, 0x29, Event.KEY_GRAVE),
			new Scancode(DIK_LSHIFT, 0x2A, Event.KEY_LEFTSHIFT), new Scancode(DIK_BACKSLASH, 0x2B, Event.KEY_BACKSLASH),
			new Scancode(DIK_Z, 0x2C, Event.KEY_Z), new Scancode(DIK_X, 0x2D, Event.KEY_X),
			new Scancode(DIK_C, 0x2E, Event.KEY_C), new Scancode(DIK_V, 0x2F, Event.KEY_V),
			new Scancode(DIK_B, 0x30, Event.KEY_B), new Scancode(DIK_N, 0x31, Event.KEY_N),
			new Scancode(DIK_M, 0x32, Event.KEY_M), new Scancode(DIK_COMMA, 0x33, Event.KEY_COMMA),
			new Scancode(DIK_PERIOD, 0x34, Event.KEY_DOT), new Scancode(DIK_SLASH, 0x35, Event.KEY_SLASH),
			new Scancode(DIK_RSHIFT, 0x36, Event.KEY_RIGHTSHIFT),
			new Scancode(DIK_MULTIPLY, 0x37, Event.KEY_KPASTERISK), new Scancode(DIK_LMENU, 0x38, Event.KEY_LEFTALT),
			new Scancode(DIK_SPACE, 0x39, Event.KEY_SPACE), new Scancode(DIK_CAPITAL, 0x3A, Event.KEY_CAPSLOCK),
			new Scancode(DIK_F1, 0x3B, Event.KEY_F1), new Scancode(DIK_F2, 0x3C, Event.KEY_F2),
			new Scancode(DIK_F3, 0x3D, Event.KEY_F3), new Scancode(DIK_F4, 0x3E, Event.KEY_F4),
			new Scancode(DIK_F5, 0x3F, Event.KEY_F5), new Scancode(DIK_F6, 0x40, Event.KEY_F6),
			new Scancode(DIK_F7, 0x41, Event.KEY_F7), new Scancode(DIK_F8, 0x42, Event.KEY_F8),
			new Scancode(DIK_F9, 0x43, Event.KEY_F9), new Scancode(DIK_F10, 0x44, Event.KEY_F10),
			new Scancode(DIK_NUMLOCK, 0x45, Event.KEY_NUMLOCK), new Scancode(DIK_SCROLL, 0x46, Event.KEY_SCROLLLOCK),
			new Scancode(DIK_NUMPAD7, 0x47, Event.KEY_KP7), new Scancode(DIK_NUMPAD8, 0x48, Event.KEY_KP8),
			new Scancode(DIK_NUMPAD9, 0x49, Event.KEY_KP9), new Scancode(DIK_SUBTRACT, 0x4A, Event.KEY_KPMINUS),
			new Scancode(DIK_NUMPAD4, 0x4B, Event.KEY_KP4), new Scancode(DIK_NUMPAD5, 0x4C, Event.KEY_KP5),
			new Scancode(DIK_NUMPAD6, 0x4D, Event.KEY_KP6), new Scancode(DIK_ADD, 0x4E, Event.KEY_KPPLUS),
			new Scancode(DIK_NUMPAD1, 0x4F, Event.KEY_KP1), new Scancode(DIK_NUMPAD2, 0x50, Event.KEY_KP2),
			new Scancode(DIK_NUMPAD3, 0x51, Event.KEY_KP3), new Scancode(DIK_NUMPAD0, 0x52, Event.KEY_KP0),
			new Scancode(DIK_DECIMAL, 0x53, Event.KEY_KPDOT), new Scancode(DIK_OEM_102, 0x56, Event.KEY_102ND),
			new Scancode(DIK_F11, 0x57, Event.KEY_F11), new Scancode(DIK_F12, 0x58, Event.KEY_F12),
			new Scancode(DIK_NUMPADEQUALS, 0x8D, Event.KEY_KPEQUAL),
			new Scancode(DIK_NEXTTRACK, 0x99, Event.KEY_NEXTSONG),
			new Scancode(DIK_NUMPADENTER, 0x9C, Event.KEY_KPENTER),
			new Scancode(DIK_RCONTROL, 0x9D, Event.KEY_RIGHTCTRL), new Scancode(DIK_DIVIDE, 0xB5, Event.KEY_KPSLASH),
			new Scancode(DIK_SYSRQ, 0xB7, Event.KEY_SYSRQ), new Scancode(DIK_RMENU, 0xB8, Event.KEY_RIGHTALT),
			new Scancode(DIK_PAUSE, 0xC5, Event.KEY_PAUSE), new Scancode(DIK_HOME, 0xC7, Event.KEY_HOME),
			new Scancode(DIK_UP, 0xC8, Event.KEY_UP), new Scancode(DIK_PRIOR, 0xC9, Event.KEY_PAGEUP),
			new Scancode(DIK_LEFT, 0xCB, Event.KEY_LEFT), new Scancode(DIK_RIGHT, 0xCD, Event.KEY_RIGHT),
			new Scancode(DIK_END, 0xCF, Event.KEY_END), new Scancode(DIK_DOWN, 0xD0, Event.KEY_DOWN),
			new Scancode(DIK_NEXT, 0xD1, Event.KEY_PAGEDOWN), new Scancode(DIK_INSERT, 0xD2, Event.KEY_INSERT),
			new Scancode(DIK_DELETE, 0xD3, Event.KEY_DELETE), new Scancode(DIK_LWIN, 0xDB, Event.KEY_LEFTMETA),
			new Scancode(DIK_RWIN, 0xDC, Event.KEY_RIGHTMETA) };

	static {
		final var modifiableNameToScancodeMap = new TreeMap<String, Scancode>();
		final var modifiableKeyCodeToScancodeMap = new HashMap<Integer, Scancode>();

		for (final var scancode : KEY_CODES) {
			modifiableNameToScancodeMap.put(scancode.name, scancode);
			modifiableKeyCodeToScancodeMap.put(scancode.keyCode, scancode);
		}

		NAME_TO_SCAN_CODE_MAP = Collections.unmodifiableMap(modifiableNameToScancodeMap);
		KEY_CODE_TO_SCAN_CODE_MAP = Collections.unmodifiableMap(modifiableKeyCodeToScancodeMap);

		EXTENDED_KEY_SCAN_CODES_SET = KEY_CODE_TO_SCAN_CODE_MAP.entrySet().stream().filter(entry -> {
			final var name = entry.getValue().name;
			return DIK_RCONTROL.equals(name) || DIK_RMENU.equals(name) || DIK_INSERT.equals(name)
					|| DIK_DELETE.equals(name) || DIK_HOME.equals(name) || DIK_END.equals(name)
					|| DIK_PRIOR.equals(name) || DIK_NEXT.equals(name) || DIK_UP.equals(name) || DIK_DOWN.equals(name)
					|| DIK_LEFT.equals(name) || DIK_RIGHT.equals(name) || DIK_SYSRQ.equals(name)
					|| DIK_DIVIDE.equals(name) || DIK_NUMPADENTER.equals(name);
		}).map(Entry::getKey).collect(Collectors.toUnmodifiableSet());
	}

	/// Returns the display name of this scancode.
	///
	/// @return the human-readable key name
	@Override
	public String toString() {
		return name;
	}
}

/* Copyright (C) 2016  Matteo Hausner
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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ScanCode {

	private static final ScanCode[] SCAN_CODES = { new ScanCode("Sleep", 223), new ScanCode("Next", 209),
			new ScanCode("Stop", 149), new ScanCode("Convert", 121), new ScanCode("Decimal", 83), new ScanCode("X", 45),
			new ScanCode("Y", 21), new ScanCode("Escape", 1), new ScanCode("Circumflex", 144),
			new ScanCode("PageDown", 209), new ScanCode("DownArrow", 208), new ScanCode("RightArrow", 205),
			new ScanCode("LeftArrow", 203), new ScanCode("PageUp", 201), new ScanCode("UpArrow", 200),
			new ScanCode("RightAlt", 184), new ScanCode("NumPadSlash", 181), new ScanCode("NumPadPeriod", 83),
			new ScanCode("NumPadPlus", 78), new ScanCode("NumPadMinus", 74), new ScanCode("CapsLock", 58),
			new ScanCode("LeftAlt", 56), new ScanCode("NumPadStar", 55), new ScanCode("BackSpace", 14),
			new ScanCode("MediaSelect", 237), new ScanCode("Mail", 236), new ScanCode("MyComputer", 235),
			new ScanCode("WebBack", 234), new ScanCode("WebForward", 233), new ScanCode("WebStop", 232),
			new ScanCode("WebRefresh", 231), new ScanCode("WebFavorites", 230), new ScanCode("WebSearch", 229),
			new ScanCode("Wake", 227), new ScanCode("Power", 222), new ScanCode("Apps", 221),
			new ScanCode("RightWindows", 220), new ScanCode("LeftWindows", 219), new ScanCode("Down", 208),
			new ScanCode("End", 207), new ScanCode("Prior", 201), new ScanCode("Up", 200), new ScanCode("Home", 199),
			new ScanCode("RightMenu", 184), new ScanCode("SysRq", 183), new ScanCode("Divide", 181),
			new ScanCode("NumPadComma", 179), new ScanCode("WebHome", 178), new ScanCode("VolumeUp", 176),
			new ScanCode("VolumeDown", 174), new ScanCode("MediaStop", 164), new ScanCode("PlayPause", 162),
			new ScanCode("Calculator", 161), new ScanCode("Mute", 160), new ScanCode("RightControl", 157),
			new ScanCode("NumPadEnter", 156), new ScanCode("NextTrack", 153), new ScanCode("Unlabeled", 151),
			new ScanCode("AX", 150), new ScanCode("Kanji", 148), new ScanCode("Underline", 147),
			new ScanCode("Colon", 146), new ScanCode("At", 145), new ScanCode("PrevTrack", 144),
			new ScanCode("NumPadEquals", 141), new ScanCode("AbntC2", 126), new ScanCode("Yen", 125),
			new ScanCode("NoConvert", 123), new ScanCode("AbntC1", 115), new ScanCode("Kana", 112),
			new ScanCode("F15", 102), new ScanCode("F14", 101), new ScanCode("F13", 100), new ScanCode("F12", 88),
			new ScanCode("F11", 87), new ScanCode("OEM102", 86), new ScanCode("NumPad0", 82),
			new ScanCode("NumPad3", 81), new ScanCode("NumPad2", 80), new ScanCode("NumPad1", 79),
			new ScanCode("NumPad6", 77), new ScanCode("NumPad5", 76), new ScanCode("NumPad4", 75),
			new ScanCode("Subtract", 74), new ScanCode("NumPad9", 73), new ScanCode("NumPad8", 72),
			new ScanCode("NumPad7", 71), new ScanCode("Scroll", 70), new ScanCode("Numlock", 69),
			new ScanCode("F10", 68), new ScanCode("F9", 67), new ScanCode("F8", 66), new ScanCode("F7", 65),
			new ScanCode("F6", 64), new ScanCode("F5", 63), new ScanCode("F4", 62), new ScanCode("F3", 61),
			new ScanCode("F2", 60), new ScanCode("F1", 59), new ScanCode("Capital", 58), new ScanCode("Space", 57),
			new ScanCode("LeftMenu", 56), new ScanCode("Multiply", 55), new ScanCode("RightShift", 54),
			new ScanCode("Slash", 53), new ScanCode("Period", 52), new ScanCode("Comma", 51), new ScanCode("M", 50),
			new ScanCode("N", 49), new ScanCode("B", 48), new ScanCode("V", 47), new ScanCode("C", 46),
			new ScanCode("Z", 44), new ScanCode("BackSlash", 43), new ScanCode("LeftShift", 42),
			new ScanCode("Grave", 41), new ScanCode("Apostrophe", 40), new ScanCode("SemiColon", 39),
			new ScanCode("L", 38), new ScanCode("K", 37), new ScanCode("J", 36), new ScanCode("H", 35),
			new ScanCode("G", 34), new ScanCode("F", 33), new ScanCode("D", 32), new ScanCode("S", 31),
			new ScanCode("A", 30), new ScanCode("LeftControl", 29), new ScanCode("Return", 28),
			new ScanCode("RightBracket", 27), new ScanCode("LeftBracket", 26), new ScanCode("P", 25),
			new ScanCode("O", 24), new ScanCode("I", 23), new ScanCode("U", 22), new ScanCode("T", 20),
			new ScanCode("R", 19), new ScanCode("E", 18), new ScanCode("W", 17), new ScanCode("Tab", 15),
			new ScanCode("Back", 14), new ScanCode("Equals", 13), new ScanCode("Minus", 12), new ScanCode("D0", 11),
			new ScanCode("D9", 10), new ScanCode("D8", 9), new ScanCode("D7", 8), new ScanCode("D6", 7),
			new ScanCode("D5", 6), new ScanCode("D4", 5), new ScanCode("D3", 4), new ScanCode("D2", 3),
			new ScanCode("D1", 2), new ScanCode("Insert", 210), new ScanCode("Right", 205), new ScanCode("Left", 203),
			new ScanCode("Pause", 197), new ScanCode("Add", 78), new ScanCode("Delete", 211), new ScanCode("Q", 16) };

	public static final Map<String, Integer> nameToScanCodeMap;
	public static final Map<Integer, String> scanCodeToNameMap;

	static {
		nameToScanCodeMap = new TreeMap<>();
		scanCodeToNameMap = new HashMap<>();

		for (final ScanCode sc : SCAN_CODES) {
			nameToScanCodeMap.put(sc.name, sc.scanCode);
			scanCodeToNameMap.put(sc.scanCode, sc.name);
		}
	}

	public final String name;
	public final int scanCode;

	public ScanCode(final String name, final int scanCode) {
		this.name = name;
		this.scanCode = scanCode;
	}

	@Override
	public String toString() {
		return name;
	}

}

package de.bwravencl.controllerbuddy.input;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class KeyCode {

	private static final KeyCode[] NORMAL_KEY_CODES = { new KeyCode("VK_LBUTTON", 0x01, "Left mouse button"),
			new KeyCode("VK_RBUTTON", 0x02, "Right mouse button"),
			new KeyCode("VK_CANCEL", 0x03, "Control-break processing"),
			new KeyCode("VK_MBUTTON", 0x04, "Middle mouse button (three-button mouse)"),
			new KeyCode("VK_XBUTTON1", 0x05, "X1 mouse button"), new KeyCode("VK_XBUTTON2", 0x06, "X2 mouse button"),
			new KeyCode("VK_BACK", 0x08, "BACKSPACE key"), new KeyCode("VK_TAB", 0x09, "TAB key"),
			new KeyCode("VK_CLEAR", 0x0C, "CLEAR key"), new KeyCode("VK_RETURN", 0x0D, "ENTER key"),
			new KeyCode("VK_PAUSE", 0x13, "PAUSE key"), new KeyCode("VK_CAPITAL", 0x14, "CAPS LOCK key"),
			new KeyCode("VK_KANA", 0x15, "IME Kana mode"),
			new KeyCode("VK_HANGUEL", 0x15, "IME Hanguel mode (maintained for compatibility; use VK_HANGUL)"),
			new KeyCode("VK_HANGUL", 0x15, "IME Hangul mode"), new KeyCode("VK_JUNJA", 0x17, "IME Junja mode"),
			new KeyCode("VK_FINAL", 0x18, "IME final mode"), new KeyCode("VK_HANJA", 0x19, "IME Hanja mode"),
			new KeyCode("VK_KANJI", 0x19, "IME Kanji mode"), new KeyCode("VK_ESCAPE", 0x1B, "ESC key"),
			new KeyCode("VK_CONVERT", 0x1C, "IME convert"), new KeyCode("VK_NONCONVERT", 0x1D, "IME nonconvert"),
			new KeyCode("VK_ACCEPT", 0x1E, "IME accept"), new KeyCode("VK_MODECHANGE", 0x1F, "IME mode change request"),
			new KeyCode("VK_SPACE", 0x20, "SPACEBAR"), new KeyCode("VK_PRIOR", 0x21, "PAGE UP key"),
			new KeyCode("VK_NEXT", 0x22, "PAGE DOWN key"), new KeyCode("VK_END", 0x23, "END key"),
			new KeyCode("VK_HOME", 0x24, "HOME key"), new KeyCode("VK_LEFT", 0x25, "LEFT ARROW key"),
			new KeyCode("VK_UP", 0x26, "UP ARROW key"), new KeyCode("VK_RIGHT", 0x27, "RIGHT ARROW key"),
			new KeyCode("VK_DOWN", 0x28, "DOWN ARROW key"), new KeyCode("VK_SELECT", 0x29, "SELECT key"),
			new KeyCode("VK_PRINT", 0x2A, "PRINT key"), new KeyCode("VK_EXECUTE", 0x2B, "EXECUTE key"),
			new KeyCode("VK_SNAPSHOT", 0x2C, "PRINT SCREEN key"), new KeyCode("VK_INSERT", 0x2D, "INS key"),
			new KeyCode("VK_DELETE", 0x2E, "DEL key"), new KeyCode("VK_HELP", 0x2F, "HELP key"),
			new KeyCode("0 key", 0x30, "0 key"), new KeyCode("1 key", 0x31, "1 key"),
			new KeyCode("2 key", 0x32, "2 key"), new KeyCode("3 key", 0x33, "3 key"),
			new KeyCode("4 key", 0x34, "4 key"), new KeyCode("5 key", 0x35, "5 key"),
			new KeyCode("6 key", 0x36, "6 key"), new KeyCode("7 key", 0x37, "7 key"),
			new KeyCode("8 key", 0x38, "8 key"), new KeyCode("9 key", 0x39, "9 key"),
			new KeyCode("A key", 0x41, "A key"), new KeyCode("B key", 0x42, "B key"),
			new KeyCode("C key", 0x43, "C key"), new KeyCode("D key", 0x44, "D key"),
			new KeyCode("E key", 0x45, "E key"), new KeyCode("F key", 0x46, "F key"),
			new KeyCode("G key", 0x47, "G key"), new KeyCode("H key", 0x48, "H key"),
			new KeyCode("I key", 0x49, "I key"), new KeyCode("J key", 0x4A, "J key"),
			new KeyCode("K key", 0x4B, "K key"), new KeyCode("L key", 0x4C, "L key"),
			new KeyCode("M key", 0x4D, "M key"), new KeyCode("N key", 0x4E, "N key"),
			new KeyCode("O key", 0x4F, "O key"), new KeyCode("P key", 0x50, "P key"),
			new KeyCode("Q key", 0x51, "Q key"), new KeyCode("R key", 0x52, "R key"),
			new KeyCode("S key", 0x53, "S key"), new KeyCode("T key", 0x54, "T key"),
			new KeyCode("U key", 0x55, "U key"), new KeyCode("V key", 0x56, "V key"),
			new KeyCode("W key", 0x57, "W key"), new KeyCode("X key", 0x58, "X key"),
			new KeyCode("Y key", 0x59, "Y key"), new KeyCode("Z key", 0x5A, "Z key"),
			new KeyCode("VK_APPS", 0x5D, "Applications key (Natural keyboard)"),
			new KeyCode("VK_SLEEP", 0x5F, "Computer Sleep key"),
			new KeyCode("VK_NUMPAD0", 0x60, "Numeric keypad 0 key"),
			new KeyCode("VK_NUMPAD1", 0x61, "Numeric keypad 1 key"),
			new KeyCode("VK_NUMPAD2", 0x62, "Numeric keypad 2 key"),
			new KeyCode("VK_NUMPAD3", 0x63, "Numeric keypad 3 key"),
			new KeyCode("VK_NUMPAD4", 0x64, "Numeric keypad 4 key"),
			new KeyCode("VK_NUMPAD5", 0x65, "Numeric keypad 5 key"),
			new KeyCode("VK_NUMPAD6", 0x66, "Numeric keypad 6 key"),
			new KeyCode("VK_NUMPAD7", 0x67, "Numeric keypad 7 key"),
			new KeyCode("VK_NUMPAD8", 0x68, "Numeric keypad 8 key"),
			new KeyCode("VK_NUMPAD9", 0x69, "Numeric keypad 9 key"), new KeyCode("VK_MULTIPLY", 0x6A, "Multiply key"),
			new KeyCode("VK_ADD", 0x6B, "Add key"), new KeyCode("VK_SEPARATOR", 0x6C, "Separator key"),
			new KeyCode("VK_SUBTRACT", 0x6D, "Subtract key"), new KeyCode("VK_DECIMAL", 0x6E, "Decimal key"),
			new KeyCode("VK_DIVIDE", 0x6F, "Divide key"), new KeyCode("VK_F1", 0x70, "F1 key"),
			new KeyCode("VK_F2", 0x71, "F2 key"), new KeyCode("VK_F3", 0x72, "F3 key"),
			new KeyCode("VK_F4", 0x73, "F4 key"), new KeyCode("VK_F5", 0x74, "F5 key"),
			new KeyCode("VK_F6", 0x75, "F6 key"), new KeyCode("VK_F7", 0x76, "F7 key"),
			new KeyCode("VK_F8", 0x77, "F8 key"), new KeyCode("VK_F9", 0x78, "F9 key"),
			new KeyCode("VK_F10", 0x79, "F10 key"), new KeyCode("VK_F11", 0x7A, "F11 key"),
			new KeyCode("VK_F12", 0x7B, "F12 key"), new KeyCode("VK_F13", 0x7C, "F13 key"),
			new KeyCode("VK_F14", 0x7D, "F14 key"), new KeyCode("VK_F15", 0x7E, "F15 key"),
			new KeyCode("VK_F16", 0x7F, "F16 key"), new KeyCode("VK_F17", 0x80, "F17 key"),
			new KeyCode("VK_F18", 0x81, "F18 key"), new KeyCode("VK_F19", 0x82, "F19 key"),
			new KeyCode("VK_F20", 0x83, "F20 key"), new KeyCode("VK_F21", 0x84, "F21 key"),
			new KeyCode("VK_F22", 0x85, "F22 key"), new KeyCode("VK_F23", 0x86, "F23 key"),
			new KeyCode("VK_F24", 0x87, "F24 key"), new KeyCode("VK_NUMLOCK", 0x90, "NUM LOCK key"),
			new KeyCode("VK_SCROLL", 0x91, "SCROLL LOCK key"), new KeyCode("VK_NUMLOCK", 0x90, "NUM LOCK key"),
			new KeyCode("VK_SCROLL", 0x91, "SCROLL LOCK key"), new KeyCode("VK_BROWSER_BACK", 0xA6, "Browser Back key"),
			new KeyCode("VK_BROWSER_FORWARD", 0xA7, "Browser Forward key"),
			new KeyCode("VK_BROWSER_REFRESH", 0xA8, "Browser Refresh key"),
			new KeyCode("VK_BROWSER_STOP", 0xA9, "Browser Stop key"),
			new KeyCode("VK_BROWSER_SEARCH", 0xAA, "Browser Search key"),
			new KeyCode("VK_BROWSER_FAVORITES", 0xAB, "Browser Favorites key"),
			new KeyCode("VK_BROWSER_HOME", 0xAC, "Browser Start and Home key"),
			new KeyCode("VK_VOLUME_MUTE", 0xAD, "Volume Mute key"),
			new KeyCode("VK_VOLUME_DOWN", 0xAE, "Volume Down key"), new KeyCode("VK_VOLUME_UP", 0xAF, "Volume Up key"),
			new KeyCode("VK_MEDIA_NEXT_TRACK", 0xB0, "Next Track key"),
			new KeyCode("VK_MEDIA_PREV_TRACK", 0xB1, "Previous Track key"),
			new KeyCode("VK_MEDIA_STOP", 0xB2, "Stop Media key"),
			new KeyCode("VK_MEDIA_PLAY_PAUSE", 0xB3, "Play/Pause Media key"),
			new KeyCode("VK_LAUNCH_MAIL", 0xB4, "Start Mail key"),
			new KeyCode("VK_LAUNCH_MEDIA_SELECT", 0xB5, "Select Media key"),
			new KeyCode("VK_LAUNCH_APP1", 0xB6, "Start Application 1 key"),
			new KeyCode("VK_LAUNCH_APP2", 0xB7, "Start Application 2 key"),
			new KeyCode("VK_OEM_1", 0xBA,
					"Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the \';:\' key VK_OEM_PLUS"),
			new KeyCode("VK_OEM_PLUS", 0xBB, "For any country/region, the \'+\' key"),
			new KeyCode("VK_OEM_COMMA", 0xBC, "For any country/region, the \',\' key"),
			new KeyCode("VK_OEM_MINUS", 0xBD, "For any country/region, the \'-\' key"),
			new KeyCode("VK_OEM_PERIOD", 0xBE, "For any country/region, the \'.\' key"),
			new KeyCode("VK_OEM_2", 0xBF,
					"Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the \'/?\' key"),
			new KeyCode("VK_OEM_3", 0xC0,
					"Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the \'`~\' key"),
			new KeyCode("VK_OEM_4", 0xDB,
					"Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the \'[{\' key"),
			new KeyCode("VK_OEM_5", 0xDC,
					"Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the \'\\|\' key"),
			new KeyCode("VK_OEM_6", 0xDD,
					"Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the \']}\' key"),
			new KeyCode("VK_OEM_7", 0xDE,
					"Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the \'single-quote/double-quote\' key"),
			new KeyCode("VK_OEM_8", 0xDF, "Used for miscellaneous characters; it can vary by keyboard."),
			new KeyCode("VK_OEM_102", 0xE2,
					"Either the angle bracket key or the backslash key on the RT 102-key keyboard"),
			new KeyCode("VK_PROCESSKEY", 0xE5, "IME PROCESS key"),
			new KeyCode("VK_PACKET", 0xE7,
					"Used to pass Unicode characters as if they were keystrokes. The VK_PACKET key is the low word of a 32-bit Virtual Key value used for non-keyboard input methods. For more information, see Remark in KEYBDINPUT, SendInput, WM_KEYDOWN, and WM_KEYUP"),
			new KeyCode("VK_PACKET", 0xE7,
					"Used to pass Unicode characters as if they were keystrokes. The VK_PACKET key is the low word of a 32-bit Virtual Key value used for non-keyboard input methods. For more information, see Remark in KEYBDINPUT, SendInput, WM_KEYDOWN, and WM_KEYUP"),
			new KeyCode("VK_ATTN", 0xF6, "Attn key"), new KeyCode("VK_CRSEL", 0xF7, "CrSel key"),
			new KeyCode("VK_EXSEL", 0xF8, "ExSel key"), new KeyCode("VK_EREOF", 0xF9, "Erase EOF key"),
			new KeyCode("VK_PLAY", 0xFA, "Play key"), new KeyCode("VK_ZOOM", 0xFB, "Zoom key"),
			new KeyCode("VK_NONAME", 0xFC, "Reserved"), new KeyCode("VK_PA1", 0xFD, "PA1 key"),
			new KeyCode("VK_OEM_CLEAR", 0xFE, "Clear key") };

	public static final KeyCode[] MODIFIER_KEY_CODES = { new KeyCode("VK_MENU", 0x12, "ALT key"),
			new KeyCode("VK_SHIFT", 0x10, "SHIFT key"), new KeyCode("VK_CONTROL", 0x11, "CTRL key"),
			new KeyCode("VK_LSHIFT", 0xA0, "Left SHIFT key"), new KeyCode("VK_RSHIFT", 0xA1, "Right SHIFT key"),
			new KeyCode("VK_LCONTROL", 0xA2, "Left CONTROL key"), new KeyCode("VK_RCONTROL", 0xA3, "Right CONTROL key"),
			new KeyCode("VK_LMENU", 0xA4, "Left MENU key"), new KeyCode("VK_RMENU", 0xA5, "Right MENU key"),
			new KeyCode("VK_LWIN", 0x5B, "Left Windows key (Natural keyboard)"),
			new KeyCode("VK_RWIN", 0x5C, "Right Windows key (Natural keyboard)") };

	public static final Map<String, Integer> nameToKeyCodeMap;
	public static final Map<Integer, String> keyCodeToNameMap;

	static {
		nameToKeyCodeMap = new TreeMap<String, Integer>();
		keyCodeToNameMap = new HashMap<Integer, String>();

		for (KeyCode kc : NORMAL_KEY_CODES) {
			nameToKeyCodeMap.put(kc.name, kc.keyCode);
			keyCodeToNameMap.put(kc.keyCode, kc.name);
		}

		for (KeyCode kc : MODIFIER_KEY_CODES) {
			nameToKeyCodeMap.put(kc.name, kc.keyCode);
			keyCodeToNameMap.put(kc.keyCode, kc.name);
		}
	}

	public String name;

	public int keyCode;

	public String description;

	public KeyCode(String name, int keyCode, String description) {
		this.name = name;
		this.keyCode = keyCode;
		this.description = description;
	}

	@Override
	public String toString() {
		return name;
	}

}

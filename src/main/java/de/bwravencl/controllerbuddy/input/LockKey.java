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

import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.KeySym;

import de.bwravencl.controllerbuddy.runmode.X11WithLockKeyFunctions;

public record LockKey(String name, int virtualKeyCode, int mask, KeySym keySym) {

	public static final String LOCK_SUFFIX = " Lock";
	public static final String CAPS_LOCK = "Caps" + LOCK_SUFFIX;
	private static final String NUM_LOCK = "Num" + LOCK_SUFFIX;
	private static final String SCROLL_LOCK = "Scroll" + LOCK_SUFFIX;

	public static final LockKey CapsLockLockKey = new LockKey(CAPS_LOCK, KeyEvent.VK_CAPS_LOCK,
			X11WithLockKeyFunctions.STATE_CAPS_LOCK_MASK, new KeySym(X11.XK_CapsLock));
	public static final LockKey NumLockLockKey = new LockKey(NUM_LOCK, KeyEvent.VK_NUM_LOCK,
			X11WithLockKeyFunctions.STATE_NUM_LOCK_MASK, new KeySym(X11WithLockKeyFunctions.XK_NumLock));
	public static final LockKey ScrollLockLockKey = new LockKey(SCROLL_LOCK, KeyEvent.VK_SCROLL_LOCK,
			X11WithLockKeyFunctions.STATE_SCROLL_LOCK_MASK, new KeySym(X11WithLockKeyFunctions.XK_ScrollLock));

	public static final LockKey[] LOCK_KEYS = { CapsLockLockKey, NumLockLockKey, ScrollLockLockKey };

	public static final Map<String, LockKey> nameToLockKeyMap;
	public static final Map<Integer, LockKey> virtualKeyCodeToLockKeyMap;

	static {
		final var modifiableNameToLockKeyMap = new HashMap<String, LockKey>();
		final var modifiableVirtualKeyCodeToLockKeyMap = new HashMap<Integer, LockKey>();

		for (final var lockKey : LOCK_KEYS) {
			modifiableNameToLockKeyMap.put(lockKey.name, lockKey);
			modifiableVirtualKeyCodeToLockKeyMap.put(lockKey.virtualKeyCode, lockKey);
		}

		nameToLockKeyMap = Collections.unmodifiableMap(modifiableNameToLockKeyMap);
		virtualKeyCodeToLockKeyMap = Collections.unmodifiableMap(modifiableVirtualKeyCodeToLockKeyMap);
	}

	@Override
	public String toString() {
		return name;
	}
}

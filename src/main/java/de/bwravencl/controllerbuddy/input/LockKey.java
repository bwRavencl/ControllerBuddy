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
import java.util.List;
import java.util.Map;
import uk.co.bithatch.linuxio.EventCode;

public record LockKey(String name, int virtualKeyCode, EventCode eventCode, String sysfsLedName) {

	public static final String LOCK_SUFFIX = " Lock";
	public static final String CAPS_LOCK = "Caps" + LOCK_SUFFIX;
	public static final LockKey CapsLockLockKey = new LockKey(CAPS_LOCK, KeyEvent.VK_CAPS_LOCK, EventCode.KEY_CAPSLOCK,
			"capslock");
	public static final String NUM_LOCK = "Num" + LOCK_SUFFIX;
	public static final LockKey NumLockLockKey = new LockKey(NUM_LOCK, KeyEvent.VK_NUM_LOCK, EventCode.KEY_NUMLOCK,
			"numlock");
	public static final Map<String, LockKey> nameToLockKeyMap;
	public static final Map<Integer, LockKey> virtualKeyCodeToLockKeyMap;
	private static final String SCROLL_LOCK = "Scroll" + LOCK_SUFFIX;
	public static final LockKey ScrollLockLockKey = new LockKey(SCROLL_LOCK, KeyEvent.VK_SCROLL_LOCK,
			EventCode.KEY_SCROLLLOCK, "scrolllock");
	public static final List<LockKey> LOCK_KEYS = List.of(CapsLockLockKey, NumLockLockKey, ScrollLockLockKey);

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

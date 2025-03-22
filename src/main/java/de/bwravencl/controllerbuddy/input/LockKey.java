/* Copyright (C) 2016  Matteo Hausner
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

import de.bwravencl.controllerbuddy.runmode.UinputDevice.Event;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LockKey(String name, int virtualKeyCode, Event event, String sysfsLedName) {

	public static final String LOCK_SUFFIX = " Lock";

	public static final String CAPS_LOCK = "Caps" + LOCK_SUFFIX;

	public static final LockKey CAPS_LOCK_LOCK_KEY = new LockKey(CAPS_LOCK, KeyEvent.VK_CAPS_LOCK, Event.KEY_CAPSLOCK,
			"capslock");

	public static final Map<String, LockKey> NAME_TO_LOCK_KEY_MAP;

	public static final String NUM_LOCK = "Num" + LOCK_SUFFIX;

	public static final LockKey NUM_LOCK_LOCK_KEY = new LockKey(NUM_LOCK, KeyEvent.VK_NUM_LOCK, Event.KEY_NUMLOCK,
			"numlock");

	public static final Map<Integer, LockKey> VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP;

	private static final String SCROLL_LOCK = "Scroll" + LOCK_SUFFIX;

	public static final LockKey SCROLL_LOCK_LOCK_KEY = new LockKey(SCROLL_LOCK, KeyEvent.VK_SCROLL_LOCK,
			Event.KEY_SCROLLLOCK, "scrolllock");

	public static final List<LockKey> LOCK_KEYS = List.of(CAPS_LOCK_LOCK_KEY, NUM_LOCK_LOCK_KEY, SCROLL_LOCK_LOCK_KEY);

	static {
		final var modifiableNameToLockKeyMap = new HashMap<String, LockKey>();
		final var modifiableVirtualKeyCodeToLockKeyMap = new HashMap<Integer, LockKey>();

		for (final var lockKey : LOCK_KEYS) {
			modifiableNameToLockKeyMap.put(lockKey.name, lockKey);
			modifiableVirtualKeyCodeToLockKeyMap.put(lockKey.virtualKeyCode, lockKey);
		}

		NAME_TO_LOCK_KEY_MAP = Collections.unmodifiableMap(modifiableNameToLockKeyMap);
		VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP = Collections.unmodifiableMap(modifiableVirtualKeyCodeToLockKeyMap);
	}

	@Override
	public String toString() {
		return name;
	}
}

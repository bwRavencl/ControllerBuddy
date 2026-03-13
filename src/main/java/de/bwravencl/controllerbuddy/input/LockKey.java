/*
 * Copyright (C) 2016 Matteo Hausner
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
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Represents a toggle lock key such as Caps Lock, Num Lock, or Scroll Lock.
///
/// Each instance bundles the AWT virtual key code, the corresponding Linux
/// input event, and the sysfs LED path needed to read the current lock state on
/// Linux.
///
/// @param name the human-readable display name for the lock key
/// @param virtualKeyCode the AWT virtual key code for this lock key
/// @param event the corresponding Linux uinput event
/// @param sysfsLedName the sysfs LED name used to read lock state on Linux
public record LockKey(String name, int virtualKeyCode, Event event, String sysfsLedName) {

	/// Suffix appended to key names to form lock key display names.
	public static final String LOCK_SUFFIX = " Lock";

	/// Display name for the Caps Lock key.
	public static final String CAPS_LOCK = "Caps" + LOCK_SUFFIX;

	/// Lock key instance for Caps Lock.
	public static final LockKey CAPS_LOCK_LOCK_KEY = new LockKey(CAPS_LOCK, KeyEvent.VK_CAPS_LOCK, Event.KEY_CAPSLOCK,
			"capslock");

	/// Map from lock key display name to the corresponding lock key instance.
	public static final Map<String, LockKey> NAME_TO_LOCK_KEY_MAP;

	/// Display name for the Num Lock key.
	public static final String NUM_LOCK = "Num" + LOCK_SUFFIX;

	/// Lock key instance for Num Lock.
	public static final LockKey NUM_LOCK_LOCK_KEY = new LockKey(NUM_LOCK, KeyEvent.VK_NUM_LOCK, Event.KEY_NUMLOCK,
			"numlock");

	/// Map from AWT virtual key code to the corresponding lock key instance.
	public static final Map<Integer, LockKey> VIRTUAL_KEY_CODE_TO_LOCK_KEY_MAP;

	private static final String SCROLL_LOCK = "Scroll" + LOCK_SUFFIX;

	/// Lock key instance for Scroll Lock.
	public static final LockKey SCROLL_LOCK_LOCK_KEY = new LockKey(SCROLL_LOCK, KeyEvent.VK_SCROLL_LOCK,
			Event.KEY_SCROLLLOCK, "scrolllock");

	/// Immutable list of all supported lock keys.
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

	/// Returns the display name of this lock key.
	///
	/// @return the human-readable lock key name
	@Override
	public String toString() {
		return name;
	}
}

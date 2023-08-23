/* Copyright (C) 2022  Matteo Hausner
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

package de.bwravencl.controllerbuddy.runmode;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;

public interface X11WithLockKeyFunctions extends X11 {

    int XkbUseCoreKbd = 0x0100;

    int STATE_CAPS_LOCK_MASK = 0x0001;
    int STATE_NUM_LOCK_MASK = 0x0002;
    int STATE_SCROLL_LOCK_MASK = 0x0004;

    int XK_NumLock = 0xFF7F;
    int XK_ScrollLock = 0xFF14;

    X11WithLockKeyFunctions INSTANCE = Native.load("X11", X11WithLockKeyFunctions.class);

    int XkbGetIndicatorState(Display display, int device_spec, Pointer state_return);

    int XkbKeysymToModifiers(Display dpy, KeySym ks);

    boolean XkbLockModifiers(Display display, int device_spec, int affect, int values);
}

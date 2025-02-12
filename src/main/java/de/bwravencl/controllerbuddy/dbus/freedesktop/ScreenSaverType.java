/* Copyright (C) 2024  Matteo Hausner
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

package de.bwravencl.controllerbuddy.dbus.freedesktop;

public enum ScreenSaverType {

	ScreenSaver(de.bwravencl.controllerbuddy.dbus.freedesktop.ScreenSaver.class, "org.freedesktop.ScreenSaver",
			"/ScreenSaver"),
	PowerManagement(Inhibit.class, "org.freedesktop.PowerManagement", "/org/freedesktop/PowerManagement/Inhibit");

	public final String busname;

	public final Class<? extends ScreenSaver> clazz;

	public final String objectpath;

	ScreenSaverType(final Class<? extends ScreenSaver> clazz, final String busname, final String objectpath) {
		this.clazz = clazz;
		this.busname = busname;
		this.objectpath = objectpath;
	}
}

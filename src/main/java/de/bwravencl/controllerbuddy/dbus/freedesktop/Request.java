/* Copyright (C) 2025  Matteo Hausner
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

import java.util.Map;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

@SuppressWarnings("unused")
@DBusInterfaceName("org.freedesktop.portal.Request")
public interface Request extends DBusInterface {

	class Response extends DBusSignal {

		private final UInt32 response;

		public Response(final String path, final UInt32 response, final Map<String, Variant<?>> results)
				throws DBusException {
			super(path, response, results);

			this.response = response;
		}

		public UInt32 getResponse() {
			return response;
		}
	}
}

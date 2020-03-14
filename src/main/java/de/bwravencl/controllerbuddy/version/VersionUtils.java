/* Copyright (C) 2019  Matteo Hausner
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

package de.bwravencl.controllerbuddy.version;

import java.util.Optional;

public final class VersionUtils {

	public static Optional<Integer> compareVersions(final String version) throws IllegalArgumentException {
		if (version == null)
			return Optional.empty();

		final var versionParts = getVersionParts(version);
		if (versionParts.length < 2)
			return Optional.empty();

		final var currentVersionParts = getVersionParts(Version.VERSION);
		for (var i = 0; i < 2; i++)
			try {
				if (Integer.parseInt(versionParts[i]) < Integer.parseInt(currentVersionParts[i]))
					return Optional.of(-1);
				if (Integer.parseInt(versionParts[i]) > Integer.parseInt(currentVersionParts[i]))
					return Optional.of(1);
			} catch (final NumberFormatException e) {
				return Optional.empty();
			}

		return Optional.of(0);
	}

	public static String getMajorAndMinorVersion() {
		return Version.VERSION.substring(0, Version.VERSION.lastIndexOf('.'));
	}

	private static String[] getVersionParts(final String version) {
		return version.split("\\.");
	}
}

/*
 * Copyright (C) 2019 Matteo Hausner
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

package de.bwravencl.controllerbuddy.util;

import de.bwravencl.controllerbuddy.constants.Constants;
import java.util.Arrays;
import java.util.Optional;

/// Utilities for parsing and comparing version strings.
///
/// Provides methods to split a version string into integer parts, get the
/// major and minor versions of the running application, and compare an
/// arbitrary version string against the current application version.
public final class VersionUtils {

	/// Prevents instantiation.
	private VersionUtils() {
	}

	/// Compares the given version against the current application version.
	///
	/// Only the major and minor version parts are compared.
	///
	/// @param otherVersion the version string to compare
	/// @return `-1` if older, `0` if equal, `1` if newer, or empty if the version
	/// is invalid
	/// @throws IllegalArgumentException if the version string cannot be parsed
	public static Optional<Integer> compareVersions(final String otherVersion) throws IllegalArgumentException {
		if (otherVersion == null) {
			return Optional.empty();
		}

		try {
			final var otherVersionParts = getVersionIntegerParts(otherVersion);
			if (otherVersionParts.length < 2) {
				return Optional.empty();
			}

			final var currentVersionParts = getVersionIntegerParts(Constants.VERSION);
			for (var i = 0; i < 2; i++) {
				if (otherVersionParts[i] < currentVersionParts[i]) {
					return Optional.of(-1);
				}
				if (otherVersionParts[i] > currentVersionParts[i]) {
					return Optional.of(1);
				}
			}
		} catch (final NumberFormatException e) {
			return Optional.empty();
		}

		return Optional.of(0);
	}

	/// Returns the major and minor version of the current application as a string
	/// (e.g. "1.8").
	///
	/// @return the major.minor version string
	public static String getMajorAndMinorVersion() {
		final var versionWithoutSuffix = stripHashSuffix(Constants.VERSION);
		return versionWithoutSuffix.substring(0, versionWithoutSuffix.lastIndexOf('.'));
	}

	/// Parses a version string into its integer components, stripping any hash
	/// suffix.
	///
	/// @param version the version string to parse (e.g. "1.8.24-ecce3da")
	/// @return an array of integer version parts (e.g. [1, 8, 24])
	public static int[] getVersionIntegerParts(final String version) {
		final var versionWithoutSuffix = stripHashSuffix(version);
		return Arrays.stream(versionWithoutSuffix.split("\\.")).mapToInt(Integer::parseInt).toArray();
	}

	/// Strips a hash suffix from a version string.
	///
	/// If the string contains a hyphen (e.g. `"1.8.24-ecce3da"`) everything from
	/// the hyphen onward is removed. Strings without a hyphen are returned
	/// unchanged.
	///
	/// @param version the version string to process
	/// @return the version string with any hash suffix removed
	private static String stripHashSuffix(final String version) {
		final var dashIndex = version.indexOf('-');

		if (dashIndex < 0) {
			return version;
		}

		return version.substring(0, dashIndex);
	}
}

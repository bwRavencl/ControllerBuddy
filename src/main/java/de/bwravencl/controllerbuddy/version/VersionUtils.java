/* Copyright (C) 2019  Matteo Hausner
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

package de.bwravencl.controllerbuddy.version;

import java.util.Arrays;
import java.util.Optional;

public final class VersionUtils {

    public static Optional<Integer> compareVersions(final String otherVersion) throws IllegalArgumentException {
        if (otherVersion == null) {
            return Optional.empty();
        }

        try {
            final var otherVersionParts = getVersionIntegerParts(otherVersion);
            if (otherVersionParts.length < 2) {
                return Optional.empty();
            }

            final var currentVersionParts = getVersionIntegerParts(Version.VERSION);
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

    private static String stripHashSuffix(final String version) {
        final var dashIndex = version.indexOf('-');

        if (dashIndex < 0) {
            return version;
        }

        return version.substring(0, dashIndex);
    }

    public static String getMajorAndMinorVersion() {
        final var versionWithoutSuffix = stripHashSuffix(Version.VERSION);
        return versionWithoutSuffix.substring(0, versionWithoutSuffix.lastIndexOf('.'));
    }

    private static int[] getVersionIntegerParts(final String version) {
        final var versionWithoutSuffix = stripHashSuffix(version);
        return Arrays.stream(versionWithoutSuffix.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}

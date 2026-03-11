/* Copyright (C) 2026  Matteo Hausner
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

package de.bwravencl.controllerbuddy.util;

import de.bwravencl.controllerbuddy.constants.Constants;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VersionUtilsTest {

	@Nested
	@DisplayName("compareVersions()")
	class CompareVersionsTests {

		@Test
		@DisplayName("returns empty when all parts of the version string are non-numeric")
		void returnsEmptyForFullyNonNumericVersion() {
			Assertions.assertEquals(Optional.empty(), VersionUtils.compareVersions("abc.def.ghi"));
		}

		@Test
		@DisplayName("returns empty when only the minor part is non-numeric")
		void returnsEmptyForNonNumericMinorPart() {
			Assertions.assertEquals(Optional.empty(), VersionUtils.compareVersions("1.x.0"));
		}

		@Test
		@DisplayName("returns empty when null is passed")
		void returnsEmptyForNull() {
			Assertions.assertEquals(Optional.empty(), VersionUtils.compareVersions(null));
		}

		@Test
		@DisplayName("returns empty when the version string has only one numeric part")
		void returnsEmptyForSinglePart() {
			Assertions.assertEquals(Optional.empty(), VersionUtils.compareVersions("1"));
		}

		@Test
		@DisplayName("returns -1 when the other version has a lower major number than the current version")
		void returnsMinusOneForLowerMajor() {
			final var currentParts = VersionUtils.getVersionIntegerParts(Constants.VERSION);
			final var lowerMajor = (currentParts[0] - 1) + ".999.0";
			Assertions.assertEquals(Optional.of(-1), VersionUtils.compareVersions(lowerMajor));
		}

		@Test
		@DisplayName("returns -1 when major matches but the other version has a lower minor number")
		void returnsMinusOneForLowerMinor() {
			final var currentParts = VersionUtils.getVersionIntegerParts(Constants.VERSION);
			final var lowerMinor = currentParts[0] + "." + (currentParts[1] - 1) + ".0";
			Assertions.assertEquals(Optional.of(-1), VersionUtils.compareVersions(lowerMinor));
		}

		@Test
		@DisplayName("returns 1 when the other version has a higher major number than the current version")
		void returnsOneForHigherMajor() {
			final var currentParts = VersionUtils.getVersionIntegerParts(Constants.VERSION);
			final var higherMajor = (currentParts[0] + 1) + ".0.0";
			Assertions.assertEquals(Optional.of(1), VersionUtils.compareVersions(higherMajor));
		}

		@Test
		@DisplayName("returns 1 when major matches but the other version has a higher minor number")
		void returnsOneForHigherMinor() {
			final var currentParts = VersionUtils.getVersionIntegerParts(Constants.VERSION);
			final var higherMinor = currentParts[0] + "." + (currentParts[1] + 1) + ".0";
			Assertions.assertEquals(Optional.of(1), VersionUtils.compareVersions(higherMinor));
		}

		@Test
		@DisplayName("returns 0 when major and minor match but the patch segment differs")
		void returnsZeroWhenOnlyPatchDiffers() {
			final var currentParts = VersionUtils.getVersionIntegerParts(Constants.VERSION);
			final var patchDifferent = currentParts[0] + "." + currentParts[1] + "." + (currentParts[2] + 1);
			Assertions.assertEquals(Optional.of(0), VersionUtils.compareVersions(patchDifferent));
		}

		@Test
		@DisplayName("strips the hash suffix from the other version before comparing")
		void stripsHashSuffixBeforeComparing() {
			// Constants.VERSION itself carries a hash suffix; comparing to itself must
			// yield 0
			Assertions.assertEquals(Optional.of(0), VersionUtils.compareVersions(Constants.VERSION));
		}
	}

	@Nested
	@DisplayName("getMajorAndMinorVersion()")
	class GetMajorAndMinorVersionTests {

		@Test
		@DisplayName("returns the major and minor components of Constants.VERSION as major.minor")
		void returnsMajorDotMinorFromCurrentVersion() {
			final var currentParts = VersionUtils.getVersionIntegerParts(Constants.VERSION);
			final var expected = currentParts[0] + "." + currentParts[1];
			Assertions.assertEquals(expected, VersionUtils.getMajorAndMinorVersion());
		}

		@Test
		@DisplayName("returns a string with no dash suffix")
		void returnsNoDashSuffix() {
			Assertions.assertFalse(VersionUtils.getMajorAndMinorVersion().contains("-"));
		}

		@Test
		@DisplayName("returns a string containing exactly one dot")
		void returnsStringWithExactlyOneDot() {
			final var result = VersionUtils.getMajorAndMinorVersion();
			Assertions.assertEquals(1, result.chars().filter(c -> c == '.').count());
		}
	}

	@Nested
	@DisplayName("getVersionIntegerParts()")
	class GetVersionIntegerPartsTests {

		@Test
		@DisplayName("parses a plain three-part version string into an array of three integers")
		void parsesThreePartVersion() {
			Assertions.assertArrayEquals(new int[] { 1, 8, 24 }, VersionUtils.getVersionIntegerParts("1.8.24"));
		}

		@Test
		@DisplayName("parses a two-part version string into an array of two integers")
		void parsesTwoPartVersion() {
			Assertions.assertArrayEquals(new int[] { 2, 3 }, VersionUtils.getVersionIntegerParts("2.3"));
		}

		@Test
		@DisplayName("strips a suffix that begins with a dash even when no characters follow the dash")
		void stripsBareDashSuffix() {
			Assertions.assertArrayEquals(new int[] { 3, 0, 1 }, VersionUtils.getVersionIntegerParts("3.0.1-"));
		}

		@Test
		@DisplayName("strips the hash suffix before parsing the version parts")
		void stripsHashSuffixBeforeParsing() {
			Assertions.assertArrayEquals(new int[] { 1, 8, 24 }, VersionUtils.getVersionIntegerParts("1.8.24-abc1234"));
		}
	}
}

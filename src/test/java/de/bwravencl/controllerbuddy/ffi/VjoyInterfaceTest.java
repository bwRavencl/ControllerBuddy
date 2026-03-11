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

package de.bwravencl.controllerbuddy.ffi;

import de.bwravencl.controllerbuddy.gui.Main;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VjoyInterfaceTest {

	@Nested
	@DisplayName("GetVJoyArchFolderName()")
	class GetVJoyArchFolderNameTests {

		@Test
		@DisplayName("returns the raw OS architecture string for non-amd64 architectures")
		void returnsRawArchStringForNonAmd64() {
			if (!"amd64".equals(Main.OS_ARCH)) {
				Assertions.assertEquals(Main.OS_ARCH, VjoyInterface.GetVJoyArchFolderName());
			}
		}

		@Test
		@DisplayName("returns 'x64' when the OS architecture is 'amd64'")
		void returnsX64ForAmd64Architecture() {
			if ("amd64".equals(Main.OS_ARCH)) {
				Assertions.assertEquals("x64", VjoyInterface.GetVJoyArchFolderName());
			}
		}
	}

	@Nested
	@DisplayName("isInitialized()")
	class IsInitializedTests {

		@Test
		@DisplayName("returns false before init() is called")
		void returnsFalseBeforeInit() {
			Assertions.assertFalse(VjoyInterface.isInitialized());
		}
	}
}

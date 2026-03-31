/*
 * Copyright (C) 2026 Matteo Hausner
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

package de.bwravencl.controllerbuddy.runmode;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class RunModeTest {

	@Mock
	Input mockInput;

	@Mock
	Main mockMain;

	private RunMode createRunMode() {
		return new RunMode(mockMain, mockInput) {

			@Override
			Logger getLogger() {
				return Logger.getLogger("test");
			}

			@Override
			public void run() {
			}
		};
	}

	@Nested
	@DisplayName("controllerDisconnected()")
	final class ControllerDisconnectedTests {

		private RunMode runMode;

		@Test
		@DisplayName("calls stopAll() on the first invocation")
		void callsStopAllOnFirstInvocation() {
			runMode.controllerDisconnected();
			Mockito.verify(mockMain, Mockito.timeout(1000)).stopAll(Mockito.anyBoolean(), Mockito.anyBoolean(),
					Mockito.anyBoolean());
		}

		@Test
		@DisplayName("does not call stopAll() a second time when called again after stopping")
		void doesNotCallStopAllAgainAfterStopping() {
			runMode.controllerDisconnected();
			Mockito.verify(mockMain, Mockito.timeout(1000)).stopAll(Mockito.anyBoolean(), Mockito.anyBoolean(),
					Mockito.anyBoolean());

			runMode.controllerDisconnected();
			Mockito.verify(mockMain, Mockito.times(1)).stopAll(Mockito.anyBoolean(), Mockito.anyBoolean(),
					Mockito.anyBoolean());
		}

		@Test
		@DisplayName("does not invoke the dialog when isSkipControllerDialogs() returns true")
		void doesNotInvokeDialogWhenSkipped() {
			runMode.controllerDisconnected();
			Mockito.verify(mockMain).isSkipControllerDialogs();
			Assertions.assertDoesNotThrow(() -> runMode.controllerDisconnected());
		}

		@BeforeEach
		void setUp() {
			Mockito.when(mockMain.isSkipControllerDialogs()).thenReturn(true);
			runMode = createRunMode();
		}
	}
}

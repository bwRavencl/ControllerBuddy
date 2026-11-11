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

import de.bwravencl.controllerbuddy.gui.MainFrame;
import de.bwravencl.controllerbuddy.input.Input;
import java.net.DatagramSocket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@NullMarked
@ExtendWith(MockitoExtension.class)
final class ClientRunModeTest {

	@Mock
	Input mockInput;

	@Mock
	MainFrame mockMainFrame;

	private ClientRunMode clientRunMode;

	@BeforeEach
	void setUp() {
		Mockito.when(mockMainFrame.getInput()).thenReturn(mockInput);
		Mockito.when(mockMainFrame.getMinPollingRate()).thenReturn(RunMode.DEFAULT_MIN_POLLING_RATE_HZ);
		Mockito.when(mockMainFrame.getMaxPollingRate()).thenReturn(RunMode.DEFAULT_MAX_POLLING_RATE_HZ);
		Mockito.when(mockMainFrame.getRandom()).thenReturn(new Random(0L));
		Mockito.when(mockMainFrame.getPassword()).thenReturn("test-password");

		clientRunMode = new ClientRunMode(mockMainFrame);
	}

	@Nested
	@DisplayName("requestStop()")
	final class RequestStopTests {

		@Test
		@DisplayName("sets the force stop flag before closing the socket")
		void setsForceStopBeforeClosingSocket() throws ReflectiveOperationException {
			final var forceStopWhenClosed = new AtomicBoolean();
			final var mockSocket = Mockito.mock(DatagramSocket.class);
			Mockito.doAnswer(_ -> {
				forceStopWhenClosed.set(clientRunMode.forceStop);
				return null;
			}).when(mockSocket).close();

			final var clientSocketField = ClientRunMode.class.getDeclaredField("clientSocket");
			clientSocketField.setAccessible(true);
			clientSocketField.set(clientRunMode, mockSocket);

			clientRunMode.requestStop();

			Mockito.verify(mockSocket).close();
			Assertions.assertTrue(forceStopWhenClosed.get());
			Assertions.assertFalse(clientRunMode.run);
		}

		@Test
		@DisplayName("only clears the run flag when no socket has been created yet")
		void onlyClearsRunFlagWithoutSocket() {
			clientRunMode.requestStop();

			Assertions.assertFalse(clientRunMode.run);
			Assertions.assertFalse(clientRunMode.forceStop);
		}
	}
}

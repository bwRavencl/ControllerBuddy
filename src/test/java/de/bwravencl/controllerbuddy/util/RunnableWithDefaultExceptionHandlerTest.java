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

package de.bwravencl.controllerbuddy.util;

import org.junit.jupiter.api.AfterEach;
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
final class RunnableWithDefaultExceptionHandlerTest {

	@Mock
	Thread.UncaughtExceptionHandler mockExceptionHandler;

	@Mock
	Runnable mockRunnable;

	@Nested
	@DisplayName("run()")
	final class RunTests {

		private Thread.UncaughtExceptionHandler originalHandler;

		@Test
		@DisplayName("delegates to the wrapped runnable when no exception is thrown")
		void delegatesToWrappedRunnableWhenNoExceptionThrown() {
			new RunnableWithDefaultExceptionHandler(mockRunnable).run();

			Mockito.verify(mockRunnable).run();
			Mockito.verifyNoInteractions(mockExceptionHandler);
		}

		@Test
		@DisplayName("passes an Error to the default uncaught exception handler without rethrowing it")
		void forwardsErrorToHandlerWithoutRethrowing() {
			final var error = new Error("test");
			Mockito.doThrow(error).when(mockRunnable).run();

			Assertions.assertDoesNotThrow(() -> new RunnableWithDefaultExceptionHandler(mockRunnable).run());

			Mockito.verify(mockExceptionHandler).uncaughtException(Thread.currentThread(), error);
		}

		@Test
		@DisplayName("passes a RuntimeException to the default uncaught exception handler without rethrowing it")
		void forwardsRuntimeExceptionToHandlerWithoutRethrowing() {
			final var exception = new RuntimeException("test");
			Mockito.doThrow(exception).when(mockRunnable).run();

			Assertions.assertDoesNotThrow(() -> new RunnableWithDefaultExceptionHandler(mockRunnable).run());

			Mockito.verify(mockExceptionHandler).uncaughtException(Thread.currentThread(), exception);
		}

		@AfterEach
		void restoreHandler() {
			Thread.setDefaultUncaughtExceptionHandler(originalHandler);
		}

		@BeforeEach
		void setUp() {
			originalHandler = Thread.getDefaultUncaughtExceptionHandler();
			Thread.setDefaultUncaughtExceptionHandler(mockExceptionHandler);
		}
	}
}

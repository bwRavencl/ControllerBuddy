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

import de.bwravencl.controllerbuddy.gui.Main;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.sdl.SDLInit;

/// The SDL main-thread event loop and task scheduler.
///
/// All SDL API calls must be executed on this loop's thread. Tasks can be
/// submitted synchronously (blocking the caller) or asynchronously. The loop
/// also polls SDL events when SDL event polling is active.
public final class MainLoop {

	private static final Logger logger = Logger.getLogger(MainLoop.class.getName());

	/// Queue of pending tasks to be executed on the SDL main thread.
	private final BlockingQueue<TaskQueueEntry> taskQueue = new LinkedBlockingDeque<>();

	/// The SDL main thread that runs this loop.
	private final Thread thread = Thread.currentThread();

	/// The task currently being executed, or `null` if idle.
	private volatile TaskQueueEntry currentTaskQueueEntry;

	/// The main application instance.
	private volatile Main main;

	/// Whether the loop should poll SDL events on each iteration.
	private volatile boolean pollSdlEvents;

	/// Whether the loop is in the process of shutting down.
	private volatile boolean shuttingDown;

	/// Enqueues a task entry and wakes the main loop thread.
	///
	/// @param taskQueueEntry the entry to enqueue
	/// @throws IllegalStateException if the main loop thread is no longer alive
	/// or is shutting down
	private void addTaskQueueEntry(final TaskQueueEntry taskQueueEntry) {
		if (!thread.isAlive()) {
			throw new IllegalStateException("Main loop thread is not alive");
		}

		if (shuttingDown) {
			throw new IllegalStateException("Main loop is shutting down");
		}

		synchronized (this) {
			taskQueue.add(taskQueueEntry);
			notifyAll();
		}
	}

	/// Starts the main loop, processing tasks and polling SDL events until
	/// interrupted.
	///
	/// Continuously dequeues and executes [TaskQueueEntry] instances. When no
	/// tasks are pending and SDL event polling is not active, the thread waits.
	/// If an uncaught exception escapes a task, all remaining queued tasks are
	/// completed exceptionally and the exception is re-thrown. On exit, SDL is
	/// shut down via `SDL_Quit`.
	public void enterLoop() {
		logger.info("Entering main loop");
		try {
			while (!Thread.interrupted()) {
				currentTaskQueueEntry = taskQueue.poll();
				if (currentTaskQueueEntry != null) {
					try {
						executeTaskQueueEntry(currentTaskQueueEntry);
					} finally {
						currentTaskQueueEntry = null;
					}
				} else if (pollSdlEvents && main != null) {
					main.pollSdlEvents();

					try {
						// noinspection BusyWait
						Thread.sleep(10L);
					} catch (final InterruptedException _) {
						return;
					}
				} else {
					try {
						synchronized (this) {
							if ( taskQueue.isEmpty() && !pollSdlEvents) {
								wait();
							}
						}
					} catch (final InterruptedException _) {
						return;
					}
				}
			}
		} catch (final Throwable t) {
			synchronized (this) {
				while (!taskQueue.isEmpty()) {
					final var taskQueueEntry = taskQueue.poll();
					if (taskQueueEntry.futureResult != null) {
						taskQueueEntry.futureResult.completeExceptionally(
								new Exception("Task aborted due to uncaught exception in previous task", t));
					}
				}
			}

			throw t;
		} finally {
			shuttingDown = true;

			try {
				SDLInit.SDL_Quit();
			} catch (final Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

			logger.info("Exiting main loop");
		}
	}

	/// Executes a single queued task entry on the main loop thread.
	///
	/// Calls [Callable#call] or [Runnable#run] depending on the task type and
	/// completes the associated [CompletableFuture] with the result. On
	/// failure, completes the future exceptionally when one is present, or
	/// sets the shutting-down flag and re-throws wrapped in a
	/// [RuntimeException] otherwise.
	///
	/// @param taskQueueEntry the entry to execute
	private void executeTaskQueueEntry(final TaskQueueEntry taskQueueEntry) {
		try {
			if (taskQueueEntry.task instanceof final Callable<?> callable) {
				final var result = callable.call();
				taskQueueEntry.futureResult.complete(result);
			} else if (taskQueueEntry.task instanceof final Runnable runnable) {
				runnable.run();
				if (taskQueueEntry.futureResult != null) {
					taskQueueEntry.futureResult.complete(null);
				}
			}
		} catch (final Throwable t) {
			if (taskQueueEntry.futureResult != null) {
				taskQueueEntry.futureResult.completeExceptionally(t);
			} else {
				shuttingDown = true;
				throw new RuntimeException(t);
			}
		}
	}

	/// Returns whether the main loop is available to accept new tasks.
	///
	/// @return `true` if the loop thread is alive and not shutting down
	public boolean isAvailable() {
		return thread.isAlive() && !shuttingDown;
	}

	/// Returns whether the currently executing task is an instance of the
	/// given class.
	///
	/// @param clazz the class to check against the current task
	/// @return `true` if a task is running and its type is assignable from
	/// `clazz`
	public boolean isTaskOfTypeRunning(final Class<?> clazz) {
		if (currentTaskQueueEntry == null) {
			return false;
		}

		return clazz.isAssignableFrom(currentTaskQueueEntry.task.getClass());
	}

	/// Submits a [Runnable] task for asynchronous execution on the main loop
	/// thread.
	///
	/// Returns immediately without waiting for the task to complete.
	///
	/// @param runnable the task to execute
	public void runAsync(final Runnable runnable) {
		addTaskQueueEntry(new TaskQueueEntry(runnable, null));
	}

	/// Submits a [Runnable] task for synchronous execution on the main loop
	/// thread and blocks until it completes.
	///
	/// @param runnable the task to execute
	/// @throws RuntimeException if the task throws an exception
	public void runSync(final Runnable runnable) {
		final var futureResult = new CompletableFuture<>();
		addTaskQueueEntry(new TaskQueueEntry(runnable, futureResult));

		try {
			futureResult.get();
		} catch (final ExecutionException e) {
			throw new RuntimeException(e);
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		}
	}

	/// Submits a [Callable] task for synchronous execution on the main loop
	/// thread and blocks until it completes.
	///
	/// @param <V> the return type of the callable
	/// @param callable the task to execute
	/// @return an [Optional] containing the task result, or empty if the
	/// calling thread was interrupted
	/// @throws RuntimeException if the task throws an exception
	@SuppressWarnings("unchecked")
	public <V> Optional<V> runSync(final Callable<V> callable) {
		final var futureResult = new CompletableFuture<>();
		addTaskQueueEntry(new TaskQueueEntry(callable, futureResult));

		try {
			return Optional.ofNullable((V) futureResult.get());
		} catch (final ExecutionException e) {
			throw new RuntimeException(e);
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		}

		return Optional.empty();
	}

	/// Sets the [Main] instance.
	///
	/// This method must be called from within the [Main] constructor after the
	/// [MainLoop] has already been constructed.
	/// @param main the [Main] instance
	public void setMain(final Main main) {
		this.main = main;
	}

	/// Shuts down the main loop by interrupting its thread and waiting for it
	/// to terminate.
	///
	/// Disables SDL event polling, interrupts the loop thread, and spins until
	/// the thread has fully stopped.
	public void shutdown() {
		pollSdlEvents = false;

		thread.interrupt();

		while (thread.isAlive()) {
			try {
				// noinspection BusyWait
				Thread.sleep(10L);
			} catch (final InterruptedException _) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/// Enables SDL event polling in the main loop and wakes the loop thread.
	///
	/// Once enabled, each loop iteration calls `pollSdlEvents` after
	/// processing the task queue.
	public void startSdlEventPolling() {
		synchronized (this) {
			pollSdlEvents = true;
			notifyAll();
		}
	}

	/// Blocks the calling thread until all queued tasks and the currently
	/// executing task have completed.
	public void waitForTask() {
		while (currentTaskQueueEntry != null || !taskQueue.isEmpty()) {
			try {
				// noinspection BusyWait
				Thread.sleep(10L);
			} catch (final InterruptedException _) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/// Drains and executes all pending tasks in the queue, then returns. Must be
	/// called from the main loop thread.
	@SuppressWarnings("NamedLikeContextualKeyword")
	public void yield() {
		if (!Thread.currentThread().equals(thread)) {
			throw new RuntimeException("yield() can only be called on " + thread.getName());
		}

		for (;;) {
			final var taskQueueEntry = taskQueue.poll();
			if (taskQueueEntry == null) {
				break;
			}
			executeTaskQueueEntry(taskQueueEntry);
		}
	}

	/// Internal record pairing a task ([Runnable] or [Callable]) with its
	/// [CompletableFuture] result.
	///
	/// The `futureResult` is completed by the main loop thread once the task
	/// finishes, allowing synchronous callers to block until the result is
	/// available.
	///
	/// @param task the task to execute, either a [Runnable] or a [Callable]
	/// @param futureResult the future that is completed with the task's result
	private record TaskQueueEntry(Object task, CompletableFuture<Object> futureResult) {
	}
}

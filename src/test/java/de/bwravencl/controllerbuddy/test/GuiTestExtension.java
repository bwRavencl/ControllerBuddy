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

package de.bwravencl.controllerbuddy.test;

import de.bwravencl.controllerbuddy.gui.Main;
import java.awt.EventQueue;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/// JUnit extension that ensures safe GUI behavior during tests.
///
/// On loading, it suppresses Swing message dialogs by setting
/// [Main]`.skipMessageDialogs` to `true`. After every test it drains the AWT
/// event queue (if the EDT is running) so that asynchronously posted events
/// cannot leak into the following tests.
public final class GuiTestExtension implements AfterEachCallback, BeforeAllCallback {

	private static volatile boolean initialized;

	@Override
	public void afterEach(final ExtensionContext context) throws Exception {
		if (Thread.getAllStackTraces().keySet().stream().anyMatch(t -> "AWT-EventQueue-0".equals(t.getName()))) {
			EventQueue.invokeAndWait(() -> {
			});
		}
	}

	@Override
	public void beforeAll(final ExtensionContext context) throws Exception {
		if (!initialized) {
			final var field = Main.class.getDeclaredField("skipMessageDialogs");
			field.setAccessible(true);
			field.setBoolean(null, true);
			initialized = true;
		}
	}
}

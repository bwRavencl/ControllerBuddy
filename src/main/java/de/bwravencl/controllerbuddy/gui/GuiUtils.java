/* Copyright (C) 2018  Matteo Hausner
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

package de.bwravencl.controllerbuddy.gui;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import de.bwravencl.controllerbuddy.input.Mode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

@SuppressWarnings({ "exports", "missing-explicit-ctor" })
public final class GuiUtils {

	static JComboBox<Mode> addModePanel(final Container container, final List<Mode> modes,
			final AbstractAction actionListener) {
		final var modePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, Main.DEFAULT_HGAP, Main.DEFAULT_VGAP));
		container.add(modePanel, BorderLayout.NORTH);

		modePanel.add(new JLabel(Main.strings.getString("MODE_LABEL")));

		final var modeComboBox = new JComboBox<>(modes.toArray(Mode[]::new));
		modeComboBox.addActionListener(actionListener);
		modePanel.add(modeComboBox);

		return modeComboBox;
	}

	static Rectangle getAndStoreTotalDisplayBounds(final Main main) {
		final var totalDisplayBounds = new Rectangle();

		for (final var graphicsDevice : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			final var graphicsConfiguration = graphicsDevice.getConfigurations()[0];
			final var bounds = graphicsConfiguration.getBounds();

			final var maxX = bounds.x + bounds.width;
			final var maxY = bounds.y + bounds.height;

			totalDisplayBounds.x = Math.min(totalDisplayBounds.x, bounds.x);
			totalDisplayBounds.x = Math.min(totalDisplayBounds.x, maxX);
			totalDisplayBounds.y = Math.min(totalDisplayBounds.y, bounds.y);
			totalDisplayBounds.y = Math.min(totalDisplayBounds.y, maxY);

			totalDisplayBounds.width = Math.max(totalDisplayBounds.width, bounds.x);
			totalDisplayBounds.width = Math.max(totalDisplayBounds.width, maxX);
			totalDisplayBounds.height = Math.max(totalDisplayBounds.height, bounds.y);
			totalDisplayBounds.height = Math.max(totalDisplayBounds.height, maxY);
		}

		if (main != null) {
			main.setTotalDisplayBounds(totalDisplayBounds);
		}

		return totalDisplayBounds;
	}

	private static Optional<String> getFrameLocationPreferencesKey(final JFrame frame) {
		final var title = frame.getTitle();
		if (title == null || title.isBlank()) {
			return Optional.empty();
		}

		var underscoreTitle = title.codePoints().mapToObj(c -> {
			if (c == ' ') {
				return "_";
			}
			return (Character.isUpperCase(c) ? "_" : "") + Character.toLowerCase((char) c);
		}).collect(Collectors.joining());
		underscoreTitle = underscoreTitle.startsWith("_") ? underscoreTitle.substring(1) : underscoreTitle;

		return Optional.of(underscoreTitle + "_location");
	}

	static Rectangle getTotalDisplayBounds() {
		return getAndStoreTotalDisplayBounds(null);
	}

	static void invokeOnEventDispatchThreadIfRequired(final Runnable runnable) {
		if (EventQueue.isDispatchThread()) {
			runnable.run();
		} else {
			EventQueue.invokeLater(runnable);
		}
	}

	static void loadFrameLocation(final Preferences preferences, final JFrame frame, final Point defaultLocation,
			final Rectangle totalDisplayBounds) {
		final var location = new Point(defaultLocation);

		getFrameLocationPreferencesKey(frame).ifPresent(preferencesKey -> {
			final var locationString = preferences.get(preferencesKey, null);
			if (locationString != null) {
				final var parts = locationString.split(",", -1);

				if (parts.length == 2) {
					try {
						location.x = Math.round(Float.parseFloat(parts[0]) * totalDisplayBounds.width);
						location.y = Math.round(Float.parseFloat(parts[1]) * totalDisplayBounds.height);
					} catch (final NumberFormatException _) {
						// ignore an invalid location string that does not contain numeric values
					}
				}
			}
		});

		setFrameLocationRespectingBounds(frame, location, totalDisplayBounds);
	}

	static void makeWindowTopmost(final Window window) {
		if (Main.isWindows) {
			final var windowHwnd = new HWND(Native.getWindowPointer(window));
			User32.INSTANCE.SetWindowPos(windowHwnd, new HWND(new Pointer(-1L)), 0, 0, 0, 0,
					WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE);
		} else {
			window.setAlwaysOnTop(false);
			window.setAlwaysOnTop(true);
		}
	}

	static void setEnabledRecursive(final Component component, final boolean enabled) {
		if (component == null) {
			return;
		}

		component.setEnabled(enabled);

		if (component instanceof final Container container) {
			for (final var child : container.getComponents()) {
				setEnabledRecursive(child, enabled);
			}
		}
	}

	private static void setFrameLocationRespectingBounds(final Frame frame, final Point location,
			final Rectangle totalDisplayBounds) {
		location.x = Math.max(totalDisplayBounds.x, Math.min(totalDisplayBounds.width - frame.getWidth(), location.x));
		location.y = Math.max(totalDisplayBounds.y,
				Math.min(totalDisplayBounds.height - frame.getHeight(), location.y));
		frame.setLocation(location);
	}

	public static void showMessageDialog(final Main main, @SuppressWarnings("exports") final Component parentComponent,
			final Object message, final String title, final int messageType) throws HeadlessException {
		showMessageDialog(main, parentComponent, message, title, messageType, null);
	}

	public static void showMessageDialog(final Main main, @SuppressWarnings("exports") final Component parentComponent,
			final Object message, final String title, final int messageType,
			@SuppressWarnings("exports") final Icon icon) throws HeadlessException {
		if (Main.skipMessageDialogs) {
			return;
		}

		if (main != null) {
			main.show();
		}

		// noinspection MagicConstant
		JOptionPane.showMessageDialog(parentComponent, message, title, messageType, icon);
	}

	static JScrollPane wrapComponentInScrollPane(final java.awt.Component component) {
		return wrapComponentInScrollPane(component, null);
	}

	public static JScrollPane wrapComponentInScrollPane(final java.awt.Component component,
			final Dimension preferredSize) {
		final var scrollPane = new JScrollPane(component);

		if (preferredSize != null) {
			scrollPane.setPreferredSize(preferredSize);
		}

		return scrollPane;
	}

	static class FrameDragListener extends MouseAdapter {

		private final JFrame frame;

		private final Main main;

		private Point mouseDownLocation = null;

		FrameDragListener(final Main main, final JFrame frame) {
			this.main = main;
			this.frame = frame;
		}

		final boolean isDragging() {
			return mouseDownLocation != null;
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			if (mouseDownLocation == null) {
				return;
			}

			final var currentMouseLocation = e.getLocationOnScreen();
			final var newFrameLocation = new Point(currentMouseLocation.x - mouseDownLocation.x,
					currentMouseLocation.y - mouseDownLocation.y);

			final var totalDisplayBounds = getAndStoreTotalDisplayBounds(main);
			setFrameLocationRespectingBounds(frame, newFrameLocation, totalDisplayBounds);
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			mouseDownLocation = e.getPoint();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			mouseDownLocation = null;

			final var frameLocation = frame.getLocation();
			final var totalDisplayBounds = getAndStoreTotalDisplayBounds(main);

			getFrameLocationPreferencesKey(frame).ifPresent(preferencesKey -> main.getPreferences().put(preferencesKey,
					frameLocation.x / (float) totalDisplayBounds.width + ","
							+ frameLocation.y / (float) totalDisplayBounds.height));
		}
	}
}

/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.controllerbuddy.gui;

import static de.bwravencl.controllerbuddy.gui.Main.DEFAULT_HGAP;
import static de.bwravencl.controllerbuddy.gui.Main.DEFAULT_VGAP;
import static de.bwravencl.controllerbuddy.gui.Main.isWindows;
import static de.bwravencl.controllerbuddy.gui.Main.strings;
import static java.awt.EventQueue.invokeLater;
import static java.awt.EventQueue.isDispatchThread;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.joining;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
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
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

import de.bwravencl.controllerbuddy.input.Mode;

public final class GuiUtils {

	static class FrameDragListener extends MouseAdapter {

		private final Main main;
		private final JFrame frame;
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
			if (mouseDownLocation == null)
				return;

			final var currentMouseLocation = e.getLocationOnScreen();
			final var newFrameLocation = new Point(currentMouseLocation.x - mouseDownLocation.x,
					currentMouseLocation.y - mouseDownLocation.y);

			final var maxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			setFrameLocationRespectingBounds(frame, newFrameLocation, maxWindowBounds);
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			mouseDownLocation = e.getPoint();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			mouseDownLocation = null;

			final var frameLocation = frame.getLocation();
			main.getPreferences().put(getFrameLocationPreferencesKey(frame), frameLocation.x + "," + frameLocation.y);
		}
	}

	static final int DEFAULT_SCROLL_PANE_BORDER_WIDTH = 10;

	static JComboBox<Mode> addModePanel(final Container container, final List<Mode> modes,
			final AbstractAction actionListener) {
		final var modePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, DEFAULT_HGAP, DEFAULT_VGAP));
		container.add(modePanel, BorderLayout.NORTH);

		modePanel.add(new JLabel(strings.getString("MODE_LABEL")));

		final var modeComboBox = new JComboBox<>(modes.toArray(new Mode[modes.size()]));
		modeComboBox.addActionListener(actionListener);
		modePanel.add(modeComboBox);

		return modeComboBox;
	}

	private static String getFrameLocationPreferencesKey(final JFrame frame) {
		final var title = frame.getTitle();
		if (title == null || title.isBlank())
			return null;

		var underscoreTitle = title.codePoints().mapToObj(c -> {
			if (c == ' ')
				return "_";
			return (Character.isUpperCase(c) ? "_" : "") + Character.toLowerCase((char) c);
		}).collect(joining());
		underscoreTitle = underscoreTitle.startsWith("_") ? underscoreTitle.substring(1) : underscoreTitle;

		return underscoreTitle + "_location";
	}

	static void invokeOnEventDispatchThreadIfRequired(final Runnable runnable) {
		if (isDispatchThread())
			runnable.run();
		else
			invokeLater(runnable);
	}

	static void loadFrameLocation(final Preferences preferences, final JFrame frame, final Point defaultLocation,
			final Rectangle maxWindowBounds) {
		final var location = defaultLocation;

		final var locationString = preferences.get(getFrameLocationPreferencesKey(frame), null);
		if (locationString != null) {
			final var parts = locationString.split(",");
			if (parts.length == 2)
				try {
					final var x = Integer.parseInt(parts[0]);
					final var y = Integer.parseInt(parts[1]);
					location.x = x;
					location.y = y;
				} catch (final NumberFormatException e) {
				}
		}

		setFrameLocationRespectingBounds(frame, location, maxWindowBounds);
	}

	static void makeWindowTopmost(final Window window) {
		if (isWindows) {
			final var windowHwnd = new HWND(Native.getWindowPointer(window));
			User32.INSTANCE.SetWindowPos(windowHwnd, new HWND(new Pointer(-1L)), 0, 0, 0, 0,
					WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE);
		} else {
			window.setAlwaysOnTop(false);
			window.setAlwaysOnTop(true);
		}
	}

	static void setEnabledRecursive(final Component component, final boolean enabled) {
		if (component == null)
			return;

		component.setEnabled(enabled);

		if (component instanceof Container)
			for (final var child : ((Container) component).getComponents())
				setEnabledRecursive(child, enabled);
	}

	private static void setFrameLocationRespectingBounds(final Frame frame, final Point location,
			final Rectangle maxWindowBounds) {
		location.x = max(maxWindowBounds.x,
				min(maxWindowBounds.width + maxWindowBounds.x - frame.getWidth(), location.x));
		location.y = max(maxWindowBounds.y,
				min(maxWindowBounds.height + maxWindowBounds.y - frame.getHeight(), location.y));
		frame.setLocation(location);
	}

	public static void showMessageDialog(final Component parentComponent, final Object message)
			throws HeadlessException {
		showMessageDialog(parentComponent, message,
				UIManager.getString("OptionPane.messageDialogTitle", parentComponent.getLocale()), INFORMATION_MESSAGE);
	}

	public static void showMessageDialog(final Component parentComponent, final Object message, final String title,
			final int messageType) throws HeadlessException {
		showMessageDialog(parentComponent, message, title, messageType, null);
	}

	public static void showMessageDialog(final Component parentComponent, final Object message, final String title,
			final int messageType, final Icon icon) throws HeadlessException {
		if (Main.skipMessageDialogs)
			return;

		JOptionPane.showMessageDialog(parentComponent, message, title, messageType, icon);
	}
}

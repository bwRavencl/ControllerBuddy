package de.bwravencl.controllerbuddy.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

class GuiUtils {

	static class FrameDragListener extends MouseAdapter {
		private final Main main;
		private final JFrame frame;
		private Point mouseDownLocation = null;

		FrameDragListener(final Main main, final JFrame frame) {
			this.main = main;
			this.frame = frame;
		}

		boolean isDragging() {
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

	private static String getFrameLocationPreferencesKey(final JFrame frame) {
		final var title = frame.getTitle();
		if (title == null || title.isBlank())
			return null;

		var underscoreTitle = title.codePoints().mapToObj((c) -> {
			if (c == ' ')
				return "_";
			return (Character.isUpperCase(c) ? "_" : "") + Character.toLowerCase((char) c);
		}).collect(Collectors.joining());
		underscoreTitle = underscoreTitle.startsWith("_") ? underscoreTitle.substring(1) : underscoreTitle;

		return underscoreTitle + "_location";
	}

	static void invokeOnEventDispatchThreadIfRequired(final Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread())
			runnable.run();
		else
			SwingUtilities.invokeLater(runnable);
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
		location.x = Math.max(maxWindowBounds.x,
				Math.min(maxWindowBounds.width + maxWindowBounds.x - frame.getWidth(), location.x));
		location.y = Math.max(maxWindowBounds.y,
				Math.min(maxWindowBounds.height + maxWindowBounds.y - frame.getHeight(), location.y));
		frame.setLocation(location);
	}

}
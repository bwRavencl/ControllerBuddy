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

package de.bwravencl.controllerbuddy.gui;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import org.jspecify.annotations.Nullable;

/// A mouse adapter that enables dragging an undecorated frame by clicking and
/// dragging anywhere on its surface.
///
/// Frame location is clamped to the total display bounds during the drag.
/// On mouse release the frame's position is persisted to preferences as
/// normalized coordinates relative to the total display bounds, so the
/// position is restored correctly across different screen configurations.
class FrameDragListener extends MouseAdapter {

	/// The frame being dragged.
	private final JFrame frame;

	/// The main application instance.
	private final Main main;

	/// The mouse position at the start of the current drag, or `null` if not
	/// dragging.
	private @Nullable Point mouseDownLocation = null;

	/// Constructs a [FrameDragListener] for the given main window and frame.
	///
	/// @param main the main application instance used to access display bounds
	/// and preferences
	/// @param frame the undecorated frame that should be draggable
	FrameDragListener(final Main main, final JFrame frame) {
		this.main = main;
		this.frame = frame;
	}

	/// Returns whether a drag operation is currently in progress.
	///
	/// @return `true` if the user is actively dragging the frame
	final boolean isDragging() {
		return mouseDownLocation != null;
	}

	/// Moves the frame to follow the mouse cursor during a drag.
	@Override
	public void mouseDragged(final MouseEvent e) {
		if (mouseDownLocation == null) {
			return;
		}

		final var currentMouseLocation = e.getLocationOnScreen();
		final var newFrameLocation = new Point(currentMouseLocation.x - mouseDownLocation.x,
				currentMouseLocation.y - mouseDownLocation.y);

		final var totalDisplayBounds = GuiUtils.getAndStoreTotalDisplayBounds(main);
		GuiUtils.setFrameLocationRespectingBounds(frame, newFrameLocation, totalDisplayBounds);
	}

	/// Records the initial mouse-click position to begin a drag operation.
	@Override
	public void mousePressed(final MouseEvent e) {
		mouseDownLocation = e.getPoint();
	}

	/// Ends the drag operation and persists the frame location as normalized
	/// coordinates.
	@Override
	public void mouseReleased(final MouseEvent e) {
		mouseDownLocation = null;

		final var frameLocation = frame.getLocation();
		final var totalDisplayBounds = GuiUtils.getAndStoreTotalDisplayBounds(main);

		GuiUtils.getFrameLocationPreferencesKey(frame)
				.ifPresent(preferencesKey -> main.getPreferences().put(preferencesKey,
						frameLocation.x / (float) totalDisplayBounds.width + ","
								+ frameLocation.y / (float) totalDisplayBounds.height));
	}
}

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

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/// Panel that renders an [SVGDocument] with interactive zoom and pan support.
///
/// Left-clicking zooms in and right-clicking zooms out at the cursor position.
/// When zoomed in, the user can pan by dragging the panel. The mouse wheel also
/// adjusts the zoom level incrementally.
final class SVGPanel extends JPanel {

	/// Maximum zoom factor the panel allows.
	private static final float MAX_ZOOM_FACTOR = 5f;

	/// Cursor shown when the panel is in pan mode (zoomed in).
	private static final Cursor MOVE_CURSOR = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

	/// Zoom factor multiplier applied per click.
	private static final float ZOOM_CLICK_FACTOR = 1.3f;

	/// Cursor shown when the panel is in zoom mode.
	private static final Cursor ZOOM_CURSOR;

	/// Hot-spot point within the zoom cursor image.
	private static final Point ZOOM_CURSOR_HOT_SPOT = new Point(11, 11);

	/// Classpath resource path for the zoom cursor GIF image.
	private static final String ZOOM_GIF_RESOURCE_PATH = "/zoom.gif";

	/// Panning weight applied when zooming in via click.
	private static final float ZOOM_IN_PANNING_FACTOR = 1.5f;

	/// Exponent controlling how fast the view re-centers when zooming out.
	private static final double ZOOM_OUT_RETURN_TO_CENTER_EXPONENT = 2.0;

	/// Base zoom factor per unit of scroll input
	private static final float ZOOM_SCROLL_BASE = 1.05f;

	@Serial
	private static final long serialVersionUID = 3771880542091875983L;

	static {
		final var inputStream = SVGPanel.class.getResourceAsStream(ZOOM_GIF_RESOURCE_PATH);
		if (inputStream == null) {
			throw new RuntimeException("Resource not found: " + ZOOM_GIF_RESOURCE_PATH);
		}

		try {
			final var bufferedImage = ImageIO.read(inputStream);

			ZOOM_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(bufferedImage, ZOOM_CURSOR_HOT_SPOT, "zoom");
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/// Current horizontal pan offset in panel coordinates.
	private float offsetX = 0f;

	/// Current vertical pan offset in panel coordinates.
	private float offsetY = 0f;

	/// The SVG document currently rendered by this panel.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private SVGDocument svgDocument;

	/// Current zoom factor applied to the SVG rendering.
	private float zoomFactor = 1f;

	/// Constructs an [SVGPanel] with zoom and pan mouse listeners registered.
	SVGPanel() {
		setCursor(ZOOM_CURSOR);

		final var mouseAdapter = new MouseAdapter() {

			private Point lastMouseLocation;

			@Override
			public void mouseClicked(final MouseEvent e) {
				super.mouseClicked(e);

				final float zoomRatio;
				switch (e.getButton()) {
				case MouseEvent.BUTTON1 -> zoomRatio = ZOOM_CLICK_FACTOR;
				case MouseEvent.BUTTON3 -> zoomRatio = 1f / ZOOM_CLICK_FACTOR;
				default -> {
					return;
				}
				}

				handleZoom(zoomRatio, e.getX(), e.getY());
			}

			@Override
			public void mouseDragged(final MouseEvent e) {
				super.mouseDragged(e);

				if (zoomFactor <= 1f || lastMouseLocation == null) {
					return;
				}

				final var maxOffsetX = (getZoomedWidth() - getWidth()) / 2f;
				final var maxOffsetY = (getZoomedHeight() - getHeight()) / 2f;

				offsetX = Math.clamp(offsetX + e.getX() - lastMouseLocation.x, -maxOffsetX, maxOffsetX);
				offsetY = Math.clamp(offsetY + e.getY() - lastMouseLocation.y, -maxOffsetY, maxOffsetY);

				lastMouseLocation = e.getPoint();

				repaint();

				if (!MOVE_CURSOR.equals(getCursor())) {
					setCursor(MOVE_CURSOR);
				}
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				super.mousePressed(e);

				if (e.getButton() != MouseEvent.BUTTON1 || zoomFactor <= 1f) {
					return;
				}

				lastMouseLocation = e.getPoint();
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				super.mouseReleased(e);

				if (e.getButton() != MouseEvent.BUTTON1) {
					return;
				}

				lastMouseLocation = null;

				if (!ZOOM_CURSOR.equals(getCursor())) {
					setCursor(ZOOM_CURSOR);
				}
			}
		};

		addMouseListener(mouseAdapter);
		addMouseMotionListener(mouseAdapter);

		addMouseWheelListener(e -> {
			if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
				return;
			}

			final var preciseWheelRotation = e.getPreciseWheelRotation();
			if (Math.abs(preciseWheelRotation) < 0.1) {
				return;
			}

			final var zoomRatio = (float) Math.pow(ZOOM_SCROLL_BASE, -preciseWheelRotation);

			handleZoom(zoomRatio, e.getX(), e.getY());
		});
	}

	/// Returns the component height scaled by the current zoom factor.
	///
	/// @return the zoomed height in pixels
	private int getZoomedHeight() {
		return Math.round(getHeight() * zoomFactor);
	}

	/// Returns the component width scaled by the current zoom factor.
	///
	/// @return the zoomed width in pixels
	private int getZoomedWidth() {
		return Math.round(getWidth() * zoomFactor);
	}

	/// Applies a zoom ratio relative to the given mouse cursor position and
	/// repaints the panel.
	///
	/// Clamps the resulting zoom factor between `1.0` and [#MAX_ZOOM_FACTOR].
	/// When zooming in, the pan offset is adjusted so the area under the
	/// cursor stays centered. When zooming out, the offset is reduced toward
	/// zero to smoothly return the view to the center.
	///
	/// @param zoomRatio the multiplicative factor to apply to the current zoom
	/// @param mouseX the x-coordinate of the zoom focus point in component space
	/// @param mouseY the y-coordinate of the zoom focus point in component space
	private void handleZoom(final float zoomRatio, final int mouseX, final int mouseY) {
		zoomFactor = Math.clamp(zoomFactor * zoomRatio, 1f, MAX_ZOOM_FACTOR);

		if (zoomFactor >= MAX_ZOOM_FACTOR) {
			return;
		}

		if (zoomFactor == 1f) {
			offsetX = 0;
			offsetY = 0;
		} else {
			if (zoomRatio >= 1f) {
				final var x = (mouseX - getWidth() / 2f) * ZOOM_IN_PANNING_FACTOR;
				final var y = (mouseY - getHeight() / 2f) * ZOOM_IN_PANNING_FACTOR;

				offsetX -= (x - offsetX) * (1f - 1f / zoomRatio);
				offsetY -= (y - offsetY) * (1f - 1f / zoomRatio);
			} else {
				final var factor = (float) Math.pow(zoomRatio, ZOOM_OUT_RETURN_TO_CENTER_EXPONENT);

				offsetX *= factor;
				offsetY *= factor;
			}
		}

		repaint();
	}

	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);

		if (svgDocument == null) {
			return;
		}

		final var g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		final var width = getWidth();
		final var height = getHeight();

		final var zoomedWidth = getZoomedWidth();
		final var zoomedHeight = getZoomedHeight();

		svgDocument.render(this, g2d, new ViewBox(-(zoomedWidth - width) / 2f + offsetX,
				-(zoomedHeight - height) / 2f + offsetY, zoomedWidth, zoomedHeight));
	}

	/// Prevents deserialization.
	///
	/// @param ignoredStream unused stream parameter
	/// @throws NotSerializableException always
	@Serial
	private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(SVGPanel.class.getName());
	}

	/// Sets the SVG document to render and resets the zoom and pan state.
	///
	/// After updating the document the panel is repainted to display the new
	/// content.
	///
	/// @param svgDocument the SVG document to display, or `null` to clear the
	/// panel
	void setSvgDocument(final SVGDocument svgDocument) {
		this.svgDocument = svgDocument;
		zoomFactor = 1f;
		offsetX = 0f;
		offsetY = 0f;

		repaint();
	}

	/// Prevents serialization.
	///
	/// @param ignoredStream unused stream parameter
	/// @throws NotSerializableException always
	@Serial
	private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(SVGPanel.class.getName());
	}
}

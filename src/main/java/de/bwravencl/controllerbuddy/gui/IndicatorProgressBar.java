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

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.OverlayAxis;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Set;
import javax.swing.JProgressBar;

/// Custom progress bar used as an overlay axis indicator, supporting solid and
/// line styles, orientation, inversion, detent markers, and overlay scaling.
///
/// Renders the current virtual axis value as a colored bar or line drawn
/// directly in the component's `paintComponent` method. Detent positions are
/// shown as tick marks, and the bar dimensions are scaled by the configured
/// overlay scaling factor.
final class IndicatorProgressBar extends JProgressBar {

	/// Long dimension in pixels of the overlay progress indicator bar.
	private static final int OVERLAY_INDICATOR_PROGRESS_LONG_DIMENSION = 150;

	/// Short dimension in pixels of the overlay progress indicator bar.
	private static final int OVERLAY_INDICATOR_PROGRESS_SHORT_DIMENSION = 20;

	/// Number of subdivisions used to compute detent tick-mark positions.
	private static final int SUBDIVISIONS = 3;

	@Serial
	private static final long serialVersionUID = 8167193907929992395L;

	/// Set of normalized axis values at which detent tick marks are drawn.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final Set<Float> detentValues;

	/// Whether axis values should be inverted before rendering.
	private final boolean inverted;

	/// Overlay axis configuration used to derive rendering parameters.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final OverlayAxis overlayAxis;

	/// Scale factor applied to subdivisions based on overlay scaling.
	private final int subdivisionScale;

	/// Constructs an [IndicatorProgressBar] for the given overlay axis and detent
	/// values.
	///
	/// Configures the orientation, dimensions, inversion, border, and foreground
	/// color from the [OverlayAxis] and the current overlay scaling factor.
	///
	/// @param main the main application instance
	/// @param overlayAxis the overlay axis configuration defining orientation,
	/// style, color, and inversion
	/// @param detentValues the set of normalized axis values at which detent tick
	/// marks should be drawn
	IndicatorProgressBar(final Main main, final OverlayAxis overlayAxis, final Set<Float> detentValues) {
		final var overlayAxisOrientation = overlayAxis.getOrientation();
		super(overlayAxisOrientation.toSwingConstant());

		this.overlayAxis = overlayAxis;
		this.detentValues = detentValues;

		final var overlayScaling = main.getOverlayScaling();
		subdivisionScale = Math.round(overlayScaling);

		setBorder(GuiUtils.createOverlayBorder());

		final int width;
		final int height;
		switch (overlayAxisOrientation) {
		case HORIZONTAL -> {
			width = OVERLAY_INDICATOR_PROGRESS_LONG_DIMENSION;
			height = OVERLAY_INDICATOR_PROGRESS_SHORT_DIMENSION;

			inverted = !overlayAxis.isInverted();
		}
		case VERTICAL -> {
			width = OVERLAY_INDICATOR_PROGRESS_SHORT_DIMENSION;
			height = OVERLAY_INDICATOR_PROGRESS_LONG_DIMENSION;

			inverted = overlayAxis.isInverted();
		}
		default -> throw new IllegalArgumentException("Unsupported orientation: " + orientation);
		}

		setPreferredSize(new Dimension(Math.round(width * overlayScaling), Math.round(height * overlayScaling)));
		setForeground(overlayAxis.getColor());
	}

	/// Calculates the x-coordinate of the left edge of a centered vertical line
	/// at the given position.
	///
	/// @param pos the center position in pixels
	/// @param lineThickness the thickness of the line in pixels
	/// @return the left x-coordinate for the line rectangle
	private static int calculateLineX(final int pos, final int lineThickness) {
		return pos - lineThickness / 2 + ((pos % 2 == 0) ? 0 : -1);
	}

	/// Calculates the y-coordinate of the top edge of a centered horizontal line
	/// at the given position.
	///
	/// @param pos the center position in pixels
	/// @param lineThickness the thickness of the line in pixels
	/// @return the top y-coordinate for the line rectangle
	private static int calculateLineY(final int pos, final int lineThickness) {
		return pos - lineThickness / 2 + ((pos % 2 == 0) ? 0 : -1);
	}

	/// Draws a subdivision tick mark or detent marker at the given position.
	///
	/// For horizontal orientation the tick is a vertical rectangle; for
	/// vertical orientation it is a horizontal rectangle. The width of the
	/// rectangle is scaled by the `subdivisionScale` factor.
	///
	/// @param g the graphics context to draw into
	/// @param pos the pixel position along the progress bar's major axis
	/// @param width the current component width in pixels
	/// @param height the current component height in pixels
	private void drawDivider(final Graphics g, final int pos, final int width, final int height) {
		final var halfScale = subdivisionScale / 2;

		switch (overlayAxis.getOrientation()) {
		case HORIZONTAL -> {
			final var x = calculateLineX(pos, 1) - halfScale;
			g.fillRect(x, 0, subdivisionScale, height);
		}
		case VERTICAL -> {
			final var y = calculateLineY(pos, 1) - halfScale;
			g.fillRect(0, y, width, subdivisionScale);
		}
		}
	}

	@Override
	public int getMaximum() {
		return inverted ? -super.getMinimum() : super.getMaximum();
	}

	@Override
	public int getMinimum() {
		return inverted ? -super.getMaximum() : super.getMinimum();
	}

	@Override
	public int getValue() {
		return inverted ? -super.getValue() : super.getValue();
	}

	@Override
	protected void paintComponent(final Graphics g) {
		final var overlayAxisOrientation = overlayAxis.getOrientation();
		final var width = getWidth();
		final var height = getHeight();

		final var size = switch (overlayAxisOrientation) {
		case HORIZONTAL -> width;
		case VERTICAL -> height;
		};

		switch (overlayAxis.getStyle()) {
		case SOLID -> super.paintComponent(g);
		case LINE -> {
			final var g2d = (Graphics2D) g.create();

			g2d.setColor(getBackground());
			g2d.fillRect(0, 0, width, height);
			g2d.setColor(getForeground());

			final var percent = (float) getPercentComplete();

			switch (overlayAxisOrientation) {
			case HORIZONTAL -> {
				final var lineThickness = width / 10;
				final var progressWidth = Math.round(percent * width);
				final var x = calculateLineX(progressWidth, lineThickness);
				final var w = lineThickness + ((lineThickness % 2 == 0) ? 1 : 0);

				g2d.fillRect(x, 0, w, height);
			}
			case VERTICAL -> {
				final var lineThickness = height / 10;
				final var progressHeight = Math.round(percent * height);
				final var y = height - calculateLineY(progressHeight, lineThickness);
				final var h = lineThickness + ((lineThickness % 2 == 0) ? 1 : 0);

				g2d.fillRect(0, y, width, h);
			}
			}

			g2d.dispose();
		}
		}

		g.setColor(Color.WHITE);
		for (var i = 1; i <= SUBDIVISIONS; i++) {
			final var pos = i * (size / (SUBDIVISIONS + 1));
			drawDivider(g, pos, width, height);
		}

		g.setColor(Color.RED);
		for (final var detentValue : detentValues) {
			final var pos = (int) Input.normalize(detentValue, -1f, 1f, 0, size);
			drawDivider(g, pos, width, height);
		}
	}

	/// Prevents deserialization.
	///
	/// @param ignoredStream unused stream parameter
	/// @throws NotSerializableException always
	@Serial
	private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(IndicatorProgressBar.class.getName());
	}

	@Override
	public void setMaximum(final int n) {
		if (inverted) {
			super.setMinimum(-n);
		} else {
			super.setMaximum(n);
		}
	}

	@Override
	public void setMinimum(final int n) {
		if (inverted) {
			super.setMaximum(-n);
		} else {
			super.setMinimum(n);
		}
	}

	@Override
	public void setValue(final int n) {
		super.setValue(inverted ? -n : n);
	}

	/// Prevents serialization.
	///
	/// @param ignoredStream unused stream parameter
	/// @throws NotSerializableException always
	@Serial
	private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(IndicatorProgressBar.class.getName());
	}
}

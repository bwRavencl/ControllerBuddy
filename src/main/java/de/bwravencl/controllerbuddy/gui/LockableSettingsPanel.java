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

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Arrays;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.LayerUI;
import org.jspecify.annotations.Nullable;

/// Custom panel whose content gets locked behind a blurred overlay when
/// disabled.
///
/// When disabled, the panel stops forwarding mouse events to its content,
/// renders a blurred snapshot of that content tiled across its bounds, and
/// overlays a stop-execution hint. Clicking the hint stops the current run mode
/// and thus unlocks the panel.
final class LockableSettingsPanel extends JPanel {

	/// Alpha channel value of the stop-execution hint background
	public static final int HINT_BACKGROUND_ALPHA = 164;

	/// Convolution operator that blurs the tile buffer used to render the
	/// disabled-state background.
	private static final ConvolveOp BLUR_OP;

	/// Corner radius, in pixels, of the stop-execution hint's rounded rectangle
	/// background.
	private static final float HINT_CORNER_RADIUS = 24f;

	/// Font size, in points, used for the stop-execution hint text.
	private static final float HINT_FONT_SIZE = 16f;

	/// Padding, in pixels, between the stop-execution hint text and its background
	/// rectangle.
	private static final int HINT_PADDING = 10;

	/// Stroke width, in pixels, of the stop-execution hint's outline.
	private static final float HINT_STROKE_WIDTH = 2f;

	/// Stop-execution hint text
	private static final String HINT_TEXT = Main.strings.getString("STOP_EXECUTION_HINT");

	/// Radius, in pixels, of the blur kernel used by [#BLUR_OP].
	private static final int KERNEL_RADIUS = 4;

	/// Edge length, in pixels, of the square blur kernel, derived from
	/// [#KERNEL_RADIUS].
	private static final int KERNEL_SIZE = 2 * KERNEL_RADIUS + 1;

	/// Time without resize events after which the panel counts as settled and gets
	/// blurred.
	private static final int RESIZE_SETTLE_DELAY_MS = 100;

	/// Extra pixels painted around each tile so that the blur is seamless across
	/// tile borders.
	private static final int TILE_MARGIN = KERNEL_RADIUS;

	/// Edge length of the visible part of a tile.
	private static final int TILE_SIZE = 256;

	/// Edge length, in pixels, of the tile buffer image, including the margin
	/// painted on each side.
	private static final int BUFFER_SIZE = TILE_SIZE + 2 * TILE_MARGIN;

	@Serial
	private static final long serialVersionUID = -45498586038994599L;

	static {
		final var data = new float[KERNEL_SIZE * KERNEL_SIZE];
		Arrays.fill(data, 1f / (KERNEL_SIZE * KERNEL_SIZE));

		BLUR_OP = new ConvolveOp(new Kernel(KERNEL_SIZE, KERNEL_SIZE, data), ConvolveOp.EDGE_NO_OP, null);
	}

	/// Buffer holding one rendered tile, including its margin, before it is blurred
	/// and drawn.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final BufferedImage bufferImage = new BufferedImage(BUFFER_SIZE, BUFFER_SIZE,
			BufferedImage.TYPE_INT_ARGB_PRE);

	/// Graphics context used to render into [#bufferImage].
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final Graphics2D bufferGraphics = bufferImage.createGraphics();

	/// Layer that intercepts and consumes mouse events over the settings content
	/// while the panel is disabled.
	private final JLayer<JComponent> contentLayer;

	/// Container that holds the settings content while the panel is disabled.
	private final JPanel layerContainer = new JPanel(new BorderLayout());

	/// Timer that marks the panel as settled, re-enabling the blur, once resize
	/// events stop arriving.
	private final Timer resizeSettleTimer;

	/// Reusable transform applied to [#bufferGraphics] while rendering each tile.
	private final AffineTransform tileTransform = new AffineTransform();

	/// Background color of the stop-execution hint in its default state.
	private @Nullable Color hintBackground;

	/// Foreground color used for the stop-execution hint's text and outline.
	private @Nullable Color hintForeground;

	/// Background color of the stop-execution hint while the pointer hovers over
	/// it.
	private @Nullable Color hintHoverBackground;

	/// Background color of the stop-execution hint while it is pressed.
	private @Nullable Color hintPressedBackground;

	/// Whether the panel is currently within the resize-settle delay window, during
	/// which blurring is skipped.
	private boolean resizing;

	/// Bounds of the stop-execution hint's background rectangle, or `null` if the
	/// hint has not been painted yet.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private @Nullable RoundRectangle2D stopHintBounds;

	/// Whether the pointer currently hovers over the stop-execution hint.
	private boolean stopHintHovered;

	/// Whether the stop-execution hint is currently pressed.
	private boolean stopHintPressed;

	/// Constructs a [LockableSettingsPanel].
	///
	/// Sets up the mouse-intercepting layer, the resize-settle timer, and the
	/// listener that restarts it on resize.
	///
	/// @param main the main application instance
	LockableSettingsPanel(final Main main) {
		super(new BorderLayout());

		final var layerUI = new LayerUI<JComponent>() {

			@Serial
			private static final long serialVersionUID = 1953694278167846722L;

			@Override
			public void eventDispatched(final AWTEvent e, final JLayer<? extends JComponent> l) {
				if (isEnabled() || !(e instanceof final MouseEvent mouseEvent)) {
					super.eventDispatched(e, l);
					return;
				}

				final var point = SwingUtilities.convertPoint(mouseEvent.getComponent(), mouseEvent.getPoint(), l);
				final var isHovered = stopHintBounds != null && stopHintBounds.contains(point);
				final var isLeftButton = SwingUtilities.isLeftMouseButton(mouseEvent);

				var needsRepaint = false;
				var triggerStop = false;

				if (stopHintHovered != isHovered) {
					stopHintHovered = isHovered;
					needsRepaint = true;
				}

				if (mouseEvent.getID() == MouseEvent.MOUSE_PRESSED && isLeftButton) {
					if (isHovered != stopHintPressed) {
						stopHintPressed = isHovered;
						needsRepaint = true;
					}
				} else if (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED && isLeftButton) {
					final var wasPressed = stopHintPressed;
					if (stopHintPressed) {
						stopHintPressed = false;
						needsRepaint = true;
					}

					triggerStop = wasPressed && isHovered;
				} else if (mouseEvent.getID() == MouseEvent.MOUSE_DRAGGED) {
					final var shouldBePressed = isLeftButton && isHovered;
					if (stopHintPressed != shouldBePressed) {
						stopHintPressed = shouldBePressed;
						needsRepaint = true;
					}
				}

				if (needsRepaint) {
					l.repaint();
				}

				l.setCursor(isHovered ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());

				mouseEvent.consume();

				if (triggerStop) {
					main.stopAll(true, true, false);
				}
			}

			@Override
			public void installUI(final JComponent c) {
				super.installUI(c);
				((JLayer<?>) c).setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
			}

			@Override
			public void uninstallUI(final JComponent c) {
				((JLayer<?>) c).setLayerEventMask(0);
				super.uninstallUI(c);
			}
		};

		contentLayer = new JLayer<>(layerContainer, layerUI);

		resizeSettleTimer = new Timer(RESIZE_SETTLE_DELAY_MS, (final var _) -> {
			resizing = false;
			if (!isEnabled()) {
				repaint();
			}
		});
		resizeSettleTimer.setRepeats(false);

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {
				resizing = true;
				resizeSettleTimer.restart();
			}
		});
	}

	@Override
	protected void addImpl(final Component comp, final Object constraints, final int index) {
		if (comp == contentLayer) {
			super.addImpl(comp, constraints, index);
			return;
		}

		final var targetContainer = isEnabled() ? this : layerContainer;

		// noinspection ConstantValue
		if (constraints == null || BorderLayout.CENTER.equals(constraints)) {
			targetContainer.removeAll();
			super.addImpl(comp, BorderLayout.CENTER, -1);
		} else {
			super.addImpl(comp, constraints, index);
		}
	}

	@Override
	protected boolean isPaintingOrigin() {
		return !isEnabled();
	}

	@Override
	protected void paintChildren(final Graphics g) {
		if (isEnabled()) {
			super.paintChildren(g);
			return;
		}

		if (!resizing) {
			final var width = getWidth();
			final var height = getHeight();

			for (var tileY = 0; tileY < height; tileY += TILE_SIZE) {
				final var drawHeight = Math.min(TILE_SIZE, height - tileY);

				for (var tileX = 0; tileX < width; tileX += TILE_SIZE) {
					final var drawWidth = Math.min(TILE_SIZE, width - tileX);

					if (!g.hitClip(tileX, tileY, drawWidth, drawHeight)) {
						continue;
					}

					final var originX = tileX - TILE_MARGIN;
					final var originY = tileY - TILE_MARGIN;

					tileTransform.setToIdentity();
					bufferGraphics.setTransform(tileTransform);
					bufferGraphics.setClip(null);
					bufferGraphics.setComposite(AlphaComposite.Clear);
					bufferGraphics.fillRect(0, 0, BUFFER_SIZE, BUFFER_SIZE);
					bufferGraphics.setComposite(AlphaComposite.SrcOver);

					tileTransform.setToTranslation(-originX, -originY);
					bufferGraphics.setTransform(tileTransform);
					bufferGraphics.setClip(originX, originY, BUFFER_SIZE, BUFFER_SIZE);
					super.paintChildren(bufferGraphics);

					final var tileGraphics = (Graphics2D) g.create(tileX, tileY, drawWidth, drawHeight);
					try {
						tileGraphics.drawImage(bufferImage, BLUR_OP, -TILE_MARGIN, -TILE_MARGIN);
					} finally {
						tileGraphics.dispose();
					}
				}
			}
		}

		final var g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, HINT_FONT_SIZE));

		final var fontMetrics = g2d.getFontMetrics();
		final var textWidth = fontMetrics.stringWidth(HINT_TEXT);
		final var textAscent = fontMetrics.getAscent();
		final var textDescent = fontMetrics.getDescent();
		final var textHeight = textAscent + textDescent;

		final var x = (getWidth() - textWidth) / 2;
		final var y = (getHeight() + textAscent - textDescent) / 2;

		final var rectX = x - HINT_PADDING;
		final var rectY = y - textAscent - HINT_PADDING;
		final var rectWidth = textWidth + (HINT_PADDING * 2);
		final var rectHeight = textHeight + (HINT_PADDING * 2);

		stopHintBounds = new RoundRectangle2D.Float(rectX, rectY, rectWidth, rectHeight, HINT_CORNER_RADIUS,
				HINT_CORNER_RADIUS);

		final Color currentBackground;
		if (stopHintPressed) {
			currentBackground = hintPressedBackground;
		} else if (stopHintHovered) {
			currentBackground = hintHoverBackground;
		} else {
			currentBackground = hintBackground;
		}

		g2d.setColor(currentBackground);
		g2d.fill(stopHintBounds);

		g2d.setColor(hintForeground);
		g2d.drawString(HINT_TEXT, x, y);

		g2d.setStroke(new BasicStroke(HINT_STROKE_WIDTH));
		g2d.setColor(hintForeground);

		final var strokeOffset = HINT_STROKE_WIDTH / 2f;
		final var outline = new RoundRectangle2D.Float(rectX + strokeOffset, rectY + strokeOffset,
				rectWidth - HINT_STROKE_WIDTH, rectHeight - HINT_STROKE_WIDTH, HINT_CORNER_RADIUS, HINT_CORNER_RADIUS);
		g2d.draw(outline);
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
	public void removeNotify() {
		resizeSettleTimer.stop();
		resizing = false;
		stopHintHovered = false;
		stopHintPressed = false;

		super.removeNotify();
	}

	@Override
	public void setEnabled(final boolean enabled) {
		if (isEnabled() == enabled) {
			return;
		}

		super.setEnabled(enabled);

		if (!(getLayout() instanceof final BorderLayout borderLayout)) {
			return;
		}

		if (enabled) {
			stopHintHovered = false;
			stopHintPressed = false;

			final var centerComp = layerContainer.getLayout() instanceof final BorderLayout layerLayout
					? layerLayout.getLayoutComponent(BorderLayout.CENTER)
					: null;

			remove(contentLayer);
			layerContainer.removeAll();

			if (centerComp != null) {
				super.addImpl(centerComp, BorderLayout.CENTER, -1);
			}
		} else {
			final var centerComp = borderLayout.getLayoutComponent(BorderLayout.CENTER);

			if (centerComp != null) {
				remove(centerComp);
				layerContainer.add(centerComp, BorderLayout.CENTER);
			}

			super.addImpl(contentLayer, BorderLayout.CENTER, -1);
		}
	}

	@Override
	public void updateUI() {
		super.updateUI();

		final var buttonBackground = UIManager.getColor("Button.background");
		hintBackground = new Color(buttonBackground.getRed(), buttonBackground.getGreen(), buttonBackground.getBlue(),
				HINT_BACKGROUND_ALPHA);

		hintForeground = UIManager.getColor("Button.foreground");

		final var buttonHoverBackground = UIManager.getColor("Button.hoverBackground");
		hintHoverBackground = new Color(buttonHoverBackground.getRed(), buttonHoverBackground.getGreen(),
				buttonHoverBackground.getBlue(), HINT_BACKGROUND_ALPHA);

		final var buttonPressedBackground = UIManager.getColor("Button.pressedBackground");
		hintPressedBackground = new Color(buttonPressedBackground.getRed(), buttonPressedBackground.getGreen(),
				buttonPressedBackground.getBlue(), HINT_BACKGROUND_ALPHA);
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

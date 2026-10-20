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

import com.formdev.flatlaf.ui.FlatUIUtils;
import de.bwravencl.controllerbuddy.Main;
import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
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
///
/// The tile buffers needed for blurring are only allocated while the panel is
/// locked and are released as soon as it gets unlocked or removed from its
/// parent.
final class LockableSettingsPanel extends JPanel {

	/// Alpha channel value of the stop-execution hint background
	public static final int HINT_BACKGROUND_ALPHA = 164;

	/// Font size, in points, used for the stop-execution hint text.
	private static final float HINT_FONT_SIZE = 16f;

	/// Padding, in pixels, between the stop-execution hint text and its background
	/// rectangle.
	private static final int HINT_PADDING = 10;

	/// Stroke width, in pixels, of the stop-execution hint's outline.
	private static final float HINT_STROKE_WIDTH = 2f;

	/// Shared stroke used to draw the stop-execution hint's outline.
	private static final BasicStroke HINT_STROKE = new BasicStroke(HINT_STROKE_WIDTH);

	/// Stop-execution hint text
	private static final String HINT_TEXT = Main.strings.getString("STOP_EXECUTION_HINT");

	/// Convolution operator performing the horizontal pass of the separable box
	/// blur.
	private static final ConvolveOp HORIZONTAL_BLUR_OP;

	/// Identity transform used to reset [#bufferGraphics] before rendering each
	/// tile.
	private static final AffineTransform IDENTITY_TRANSFORM = new AffineTransform();

	/// Radius, in pixels, of the blur kernel.
	private static final int KERNEL_RADIUS = 4;

	/// Length, in pixels, of the one-dimensional blur kernel, derived from
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

	/// Edge length, in pixels, of the tile buffer images, including the margin
	/// painted on each side.
	private static final int BUFFER_SIZE = TILE_SIZE + 2 * TILE_MARGIN;

	/// Convolution operator performing the vertical pass of the separable box blur.
	private static final ConvolveOp VERTICAL_BLUR_OP;

	@Serial
	private static final long serialVersionUID = -45498586038994599L;

	static {
		final var data = new float[KERNEL_SIZE];
		Arrays.fill(data, 1f / KERNEL_SIZE);

		HORIZONTAL_BLUR_OP = new ConvolveOp(new Kernel(KERNEL_SIZE, 1, data), ConvolveOp.EDGE_NO_OP, null);
		VERTICAL_BLUR_OP = new ConvolveOp(new Kernel(1, KERNEL_SIZE, data), ConvolveOp.EDGE_NO_OP, null);
	}

	/// Layer that intercepts and consumes mouse events over the settings content
	/// while the panel is disabled.
	private final JLayer<JComponent> contentLayer;

	/// Container that holds the settings content while the panel is disabled.
	private final JPanel layerContainer = new JPanel(new BorderLayout());

	/// Timer that marks the panel as settled, re-enabling the blur, once resize
	/// events stop arriving.
	private final Timer resizeSettleTimer;

	/// Graphics context used to render into [#bufferImage], or `null` while no
	/// buffers are allocated.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private @Nullable Graphics2D bufferGraphics;

	/// Buffer holding one rendered tile, including its margin. Also receives the
	/// final result of the blur. `null` while the panel is unlocked.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private @Nullable BufferedImage bufferImage;

	/// Arc diameter, in pixels, of the stop-execution hint's rounded rectangle
	/// background.
	private float hintArc;

	/// Background color of the stop-execution hint in its default state.
	private @Nullable Color hintBackground;

	/// Base font from which [#hintFont] was derived, used to detect font changes.
	private @Nullable Font hintBaseFont;

	/// Cached bold font used for the stop-execution hint text.
	private @Nullable Font hintFont;

	/// Foreground color used for the stop-execution hint's text and outline.
	private @Nullable Color hintForeground;

	/// Background color of the stop-execution hint while the pointer hovers over
	/// it.
	private @Nullable Color hintHoverBackground;

	/// Panel height for which the cached hint layout was computed.
	private int hintLayoutHeight;

	/// Panel width for which the cached hint layout was computed.
	private int hintLayoutWidth;

	/// Background color of the stop-execution hint while it is pressed.
	private @Nullable Color hintPressedBackground;

	/// Cached x coordinate of the stop-execution hint text baseline origin.
	private int hintTextX;

	/// Cached y coordinate of the stop-execution hint text baseline.
	private int hintTextY;

	/// Whether the panel is currently within the resize-settle delay window, during
	/// which blurring is skipped.
	private boolean resizing;

	/// Intermediate buffer receiving the horizontal blur pass. `null` while the
	/// panel is unlocked.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private @Nullable BufferedImage scratchImage;

	/// Bounds of the stop-execution hint's background rectangle, or `null` if the
	/// hint has not been laid out yet.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private @Nullable Shape stopHintBounds;

	/// Whether the pointer currently hovers over the stop-execution hint.
	private boolean stopHintHovered;

	/// Cached outline shape of the stop-execution hint.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private @Nullable Shape stopHintOutline;

	/// Whether the stop-execution hint is currently pressed.
	private boolean stopHintPressed;

	/// Constructs a [LockableSettingsPanel].
	///
	/// Sets up the mouse-intercepting layer, the resize-settle timer, and the
	/// listener that restarts it on resize.
	///
	/// @param mainFrame the main frame instance
	LockableSettingsPanel(final MainFrame mainFrame) {
		super(new BorderLayout());

		final var layerUI = new LayerUI<JComponent>() {

			@Serial
			private static final long serialVersionUID = 1953694278167846722L;

			@Override
			public void eventDispatched(final AWTEvent e, final JLayer<? extends JComponent> l) {
				final var panel = LockableSettingsPanel.this;

				if (panel.isEnabled() || !(e instanceof final MouseEvent mouseEvent)) {
					super.eventDispatched(e, l);
					return;
				}

				final var point = SwingUtilities.convertPoint(mouseEvent.getComponent(), mouseEvent.getPoint(), panel);
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
					repaintStopHint();
				}

				mouseEvent.consume();

				if (triggerStop) {
					mainFrame.stopAll(true, true, false);
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

		// noinspection ConstantValue
		if (constraints == null || BorderLayout.CENTER.equals(constraints)) {
			if (isEnabled()) {
				removeAll();
				super.addImpl(comp, BorderLayout.CENTER, -1);
			} else {
				layerContainer.removeAll();
				layerContainer.add(comp, BorderLayout.CENTER);
			}
		} else {
			super.addImpl(comp, constraints, index);
		}
	}

	@Override
	protected boolean isPaintingOrigin() {
		return !isEnabled();
	}

	/// Renders the content tile by tile into the tile buffer, blurs each tile with
	/// a separable box blur and draws the visible part of it.
	///
	/// Allocates the tile buffers on first use. No images are allocated per paint.
	///
	/// @param g the graphics context to draw the blurred content into
	private void paintBlurredContent(final Graphics g) {
		var image = bufferImage;
		var scratch = scratchImage;
		var graphics = bufferGraphics;

		if (image == null || scratch == null || graphics == null) {
			releaseBuffers();

			image = new BufferedImage(BUFFER_SIZE, BUFFER_SIZE, BufferedImage.TYPE_INT_ARGB_PRE);
			scratch = new BufferedImage(BUFFER_SIZE, BUFFER_SIZE, BufferedImage.TYPE_INT_ARGB_PRE);
			graphics = image.createGraphics();

			bufferImage = image;
			scratchImage = scratch;
			bufferGraphics = graphics;
		}

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

				graphics.setTransform(IDENTITY_TRANSFORM);
				graphics.translate(-originX, -originY);
				graphics.setClip(originX, originY, BUFFER_SIZE, BUFFER_SIZE);
				graphics.setComposite(AlphaComposite.Clear);
				graphics.fillRect(originX, originY, BUFFER_SIZE, BUFFER_SIZE);
				graphics.setComposite(AlphaComposite.SrcOver);
				super.paintChildren(graphics);

				HORIZONTAL_BLUR_OP.filter(image, scratch);
				VERTICAL_BLUR_OP.filter(scratch, image);

				g.drawImage(image, tileX, tileY, tileX + drawWidth, tileY + drawHeight, TILE_MARGIN, TILE_MARGIN,
						TILE_MARGIN + drawWidth, TILE_MARGIN + drawHeight, null);
			}
		}
	}

	@Override
	protected void paintChildren(final Graphics g) {
		if (isEnabled()) {
			super.paintChildren(g);
			return;
		}

		if (!resizing) {
			paintBlurredContent(g);
		}

		paintStopHint((Graphics2D) g);
	}

	/// Paints the stop-execution hint centered on the panel.
	///
	/// @param g2d the graphics context to paint the hint into
	private void paintStopHint(final Graphics2D g2d) {
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		updateHintLayout(g2d);

		final var font = hintFont;
		final var bounds = stopHintBounds;
		final var outline = stopHintOutline;
		if (font == null || bounds == null || outline == null) {
			return;
		}

		final Color currentBackground;
		if (stopHintPressed && hintPressedBackground != null) {
			currentBackground = hintPressedBackground;
		} else if (stopHintHovered && hintHoverBackground != null) {
			currentBackground = hintHoverBackground;
		} else {
			currentBackground = hintBackground;
		}

		g2d.setColor(currentBackground);
		g2d.fill(bounds);

		g2d.setFont(font);
		g2d.setColor(hintForeground);
		g2d.drawString(HINT_TEXT, hintTextX, hintTextY);

		g2d.setStroke(HINT_STROKE);
		g2d.draw(outline);
	}

	/// Prevents deserialization.
	///
	/// @param ignoredStream unused stream parameter
	/// @throws NotSerializableException always
	@Serial
	private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(LockableSettingsPanel.class.getName());
	}

	/// Disposes the tile buffer graphics context and drops the tile buffers so they
	/// can be garbage collected.
	private void releaseBuffers() {
		if (bufferGraphics != null) {
			bufferGraphics.dispose();
			bufferGraphics = null;
		}

		bufferImage = null;
		scratchImage = null;
	}

	@Override
	public void removeNotify() {
		resizeSettleTimer.stop();
		resizing = false;
		stopHintHovered = false;
		stopHintPressed = false;
		releaseBuffers();

		super.removeNotify();
	}

	/// Repaints only the area covered by the stop-execution hint, so that hover and
	/// press feedback does not re-render and re-blur the entire content.
	private void repaintStopHint() {
		final var bounds = stopHintBounds;
		if (bounds == null) {
			repaint();
			return;
		}

		final var rect = bounds.getBounds();
		repaint(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		if (isEnabled() == enabled) {
			return;
		}

		super.setEnabled(enabled);

		if (enabled) {
			releaseBuffers();
		}

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

	/// Recomputes the cached hint font, text position and shapes if the panel size
	/// or font changed since the last computation.
	///
	/// @param g2d the graphics context whose font and rendering hints are used for
	/// measuring the hint text
	private void updateHintLayout(final Graphics2D g2d) {
		final var baseFont = g2d.getFont();
		final var width = getWidth();
		final var height = getHeight();

		if (hintFont != null && baseFont.equals(hintBaseFont) && width == hintLayoutWidth
				&& height == hintLayoutHeight) {
			return;
		}

		final var font = baseFont.deriveFont(Font.BOLD, HINT_FONT_SIZE);
		final var fontMetrics = g2d.getFontMetrics(font);
		final var textWidth = fontMetrics.stringWidth(HINT_TEXT);
		final var textAscent = fontMetrics.getAscent();
		final var textDescent = fontMetrics.getDescent();
		final var textHeight = textAscent + textDescent;

		final var x = (width - textWidth) / 2;
		final var y = (height + textAscent - textDescent) / 2;

		final var rectX = x - HINT_PADDING;
		final var rectY = y - textAscent - HINT_PADDING;
		final var rectWidth = textWidth + (HINT_PADDING * 2);
		final var rectHeight = textHeight + (HINT_PADDING * 2);

		final var strokeOffset = HINT_STROKE_WIDTH / 2f;
		final var outlineArc = Math.max(0f, hintArc - HINT_STROKE_WIDTH);

		hintBaseFont = baseFont;
		hintFont = font;
		hintLayoutWidth = width;
		hintLayoutHeight = height;
		hintTextX = x;
		hintTextY = y;
		stopHintBounds = FlatUIUtils.createComponentRectangle(rectX, rectY, rectWidth, rectHeight, hintArc);
		stopHintOutline = FlatUIUtils.createComponentRectangle(rectX + strokeOffset, rectY + strokeOffset,
				rectWidth - HINT_STROKE_WIDTH, rectHeight - HINT_STROKE_WIDTH, outlineArc);
	}

	@Override
	public void updateUI() {
		super.updateUI();

		final var buttonBackground = UIManager.getColor("Button.background");
		hintBackground = new Color(buttonBackground.getRed(), buttonBackground.getGreen(), buttonBackground.getBlue(),
				HINT_BACKGROUND_ALPHA);

		hintForeground = UIManager.getColor("Button.foreground");

		final var buttonHoverBackground = UIManager.getColor("Button.hoverBackground");
		if (buttonHoverBackground != null) {
			hintHoverBackground = new Color(buttonHoverBackground.getRed(), buttonHoverBackground.getGreen(),
					buttonHoverBackground.getBlue(), HINT_BACKGROUND_ALPHA);
		}

		final var buttonPressedBackground = UIManager.getColor("Button.pressedBackground");
		if (buttonPressedBackground != null) {
			hintPressedBackground = new Color(buttonPressedBackground.getRed(), buttonPressedBackground.getGreen(),
					buttonPressedBackground.getBlue(), HINT_BACKGROUND_ALPHA);
		}

		hintArc = (float) UIManager.getInt("Button.arc");
		hintBaseFont = null;
	}

	/// Prevents serialization.
	///
	/// @param ignoredStream unused stream parameter
	/// @throws NotSerializableException always
	@Serial
	private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(LockableSettingsPanel.class.getName());
	}
}

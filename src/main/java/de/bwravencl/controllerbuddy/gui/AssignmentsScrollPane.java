/*
 * Copyright (C) 2018 Matteo Hausner
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

import com.formdev.flatlaf.ui.FlatButtonUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import de.bwravencl.controllerbuddy.input.Mode.Component;
import de.bwravencl.controllerbuddy.input.Mode.Component.ComponentType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.UIManager;
import org.lwjgl.sdl.SDLGamepad;

/// A scroll pane that displays a visual representation of a gamepad's controls
/// as interactive buttons.
///
/// Each button corresponds to a gamepad component (axis, button, or D-pad
/// direction) and opens an assignment editor dialog when clicked. The layout
/// mimics a physical gamepad, with sticks, triggers, bumpers, and face
/// buttons placed in their conventional positions.
final class AssignmentsScrollPane extends JScrollPane {

	/// Stroke used for drawing the border of the controller shape and component
	/// buttons.
	private static final Stroke BORDER_STROKE = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	/// Height in pixels used for all component buttons.
	private static final int BUTTON_HEIGHT = 50;

	/// Color used for filling the controller shape.
	private static final Color CONTROLLER_SHAPE_COLOR = new Color(Main.LIGHT_BLUE_COLOR.getRed(),
			Main.LIGHT_BLUE_COLOR.getGreen(), Main.LIGHT_BLUE_COLOR.getBlue(), 128);

	@Serial
	private static final long serialVersionUID = -4096911611882875787L;

	/// The panel that holds the arranged gamepad component buttons.
	private final JPanel assignmentsPanel = new JPanel();

	/// The main application instance.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final Main main;

	/// Creates an [AssignmentsScrollPane] with buttons arranged in a grid to
	/// resemble a gamepad layout.
	///
	/// @param main the main application instance
	AssignmentsScrollPane(final Main main) {
		this.main = main;

		assignmentsPanel.setLayout(new GridBagLayout());
		assignmentsPanel.setOpaque(false);

		final var constraints = new GridBagConstraints();
		constraints.insets = new Insets(16, 16, 16, 16);
		constraints.weightx = 1d;
		constraints.weighty = 1d;

		constraints.gridx = 0;
		constraints.gridy = 0;
		assignmentsPanel.add(
				createComponentButton(Main.STRINGS.getString("LEFT_TRIGGER"),
						new Component(main, ComponentType.AXIS, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER)),
				constraints);

		constraints.gridx = 4;
		constraints.gridy = 0;
		assignmentsPanel.add(
				createComponentButton(Main.STRINGS.getString("RIGHT_TRIGGER"),
						new Component(main, ComponentType.AXIS, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER)),
				constraints);

		constraints.gridx = 0;
		constraints.gridy = 1;
		assignmentsPanel.add(
				createComponentButton(Main.STRINGS.getString("LEFT_SHOULDER"),
						new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER)),
				constraints);

		constraints.gridx = 2;
		constraints.gridy = 1;
		assignmentsPanel.add(createComponentButton(Main.STRINGS.getString("GUIDE_BUTTON"),
				new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE)), constraints);

		constraints.gridx = 4;
		constraints.gridy = 1;
		assignmentsPanel.add(
				createComponentButton(Main.STRINGS.getString("RIGHT_SHOULDER"),
						new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER)),
				constraints);

		constraints.gridx = 0;
		constraints.gridy = 2;
		assignmentsPanel.add(new Stick(main, Stick.StickType.LEFT), constraints);

		constraints.gridx = 1;
		constraints.gridy = 2;
		assignmentsPanel.add(createComponentButton(Main.STRINGS.getString("BACK_BUTTON"),
				new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_BACK)), constraints);

		constraints.gridx = 3;
		constraints.gridy = 2;
		assignmentsPanel.add(createComponentButton(Main.STRINGS.getString("START_BUTTON"),
				new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_START)), constraints);

		constraints.gridx = 4;
		constraints.gridy = 2;
		assignmentsPanel.add(new FourWay(this, Main.STRINGS.getString("Y_BUTTON"),
				new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH),
				Main.STRINGS.getString("X_BUTTON"),
				new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_WEST),
				Main.STRINGS.getString("B_BUTTON"),
				new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_EAST),
				Main.STRINGS.getString("A_BUTTON"),
				new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH)), constraints);

		constraints.gridx = 1;
		constraints.gridy = 3;
		assignmentsPanel.add(
				new FourWay(this, Main.STRINGS.getString("DPAD_UP"),
						new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP),
						Main.STRINGS.getString("DPAD_LEFT"),
						new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT),
						Main.STRINGS.getString("DPAD_RIGHT"),
						new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT),
						Main.STRINGS.getString("DPAD_DOWN"),
						new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN)),
				constraints);

		constraints.gridx = 3;
		constraints.gridy = 3;
		assignmentsPanel.add(new Stick(main, Stick.StickType.RIGHT), constraints);

		final var centeringPanel = new JPanel(new GridBagLayout()) {

			@Serial
			private static final long serialVersionUID = -3144368069964550322L;

			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);

				final var g2 = (Graphics2D) g.create();
				try {
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					final var bounds = assignmentsPanel.getBounds();
					g2.translate(bounds.getCenterX(), bounds.getCenterY() - 72);

					final var w = bounds.getWidth();
					final var h = bounds.getHeight();

					final var path = new Path2D.Float();
					path.moveTo(-0.28 * w, -0.42 * h);
					path.quadTo(0.0, -0.3696 * h, 0.28 * w, -0.42 * h);
					path.curveTo(0.42 * w, -0.4704 * h, 0.49 * w, -0.42 * h, 0.56 * w, -0.168 * h);
					path.curveTo(0.665 * w, 0.168 * h, 0.735 * w, 0.504 * h, 0.665 * w, 0.7056 * h);
					path.curveTo(0.63 * w, 0.8736 * h, 0.42 * w, 0.8736 * h, 0.315 * w, 0.672 * h);
					path.quadTo(0.0, 0.5544 * h, -0.315 * w, 0.672 * h);
					path.curveTo(-0.42 * w, 0.8736 * h, -0.63 * w, 0.8736 * h, -0.665 * w, 0.7056 * h);
					path.curveTo(-0.735 * w, 0.504 * h, -0.665 * w, 0.168 * h, -0.56 * w, -0.168 * h);
					path.curveTo(-0.49 * w, -0.42 * h, -0.42 * w, -0.4704 * h, -0.28 * w, -0.42 * h);
					path.closePath();

					g2.setStroke(BORDER_STROKE);
					g2.setColor(main.isDarkLookAndFeel() ? Color.LIGHT_GRAY : Color.BLACK);
					g2.draw(path);

					g2.setColor(CONTROLLER_SHAPE_COLOR);
					g2.fill(path);
				} finally {
					g2.dispose();
				}
			}
		};
		centeringPanel.add(assignmentsPanel);
		setViewportView(centeringPanel);
	}

	/// Throws an exception if the given dimension is not square (width != height).
	///
	/// @param dimension the dimension to check
	/// @throws IllegalArgumentException if `dimension` is not square
	private static void checkDimensionIsSquare(final Dimension dimension) {
		if (dimension.width != dimension.height) {
			throw new IllegalArgumentException("Parameter dimension is not square");
		}
	}

	/// Creates a [JButton] for a gamepad component, using a round
	/// [AssignmentsButton]
	/// for face and guide buttons and a standard [JButton] for all others.
	///
	/// @param name the localized display name of the component
	/// @param component the gamepad component this button represents
	/// @return the configured button
	private JButton createComponentButton(final String name, final Component component) {
		final boolean round;
		final JButton button;
		if (component.type() == ComponentType.BUTTON && (component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH
				|| component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_EAST
				|| component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_WEST
				|| component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH
				|| component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_BACK
				|| component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_START
				|| component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE)) {
			round = true;
			button = new AssignmentsButton(main, new EditComponentAction(main, name, component)) {

				@Serial
				private static final long serialVersionUID = 8467379031897370934L;

				@Override
				public boolean contains(final int x, final int y) {
					final var radius = getDiameter() / 2;
					return Point2D.distance(x, y, getWidth() / 2d, getHeight() / 2d) < radius;
				}

				private int getDiameter() {
					return Math.min(getWidth(), getHeight());
				}

				@Override
				public boolean isContentAreaFilled() {
					return false;
				}

				@Override
				protected void paintBorder(final Graphics g) {
					if (!isBorderPainted()) {
						return;
					}

					final var g2d = (Graphics2D) g;
					beginBorder(g2d);

					final var diameter = getDiameter() - 2;
					final var radius = diameter / 2;
					g2d.drawOval(getWidth() / 2 - radius, getHeight() / 2 - radius, diameter, diameter);
				}

				@Override
				protected void paintComponent(final Graphics g) {
					final var diameter = getDiameter() - 3;
					final var radius = diameter / 2;

					final var width = getWidth();
					final var height = getHeight();

					final var g2d = (Graphics2D) g;

					if (contentAreaFilled) {
						beginBackground(g2d);

						final var ovalWidth = width % 2 != 0 ? width + 1 : width;
						final var ovalHeight = height % 2 != 0 ? height + 1 : height;

						g2d.fillOval(ovalWidth / 2 - radius, ovalHeight / 2 - radius, diameter, diameter);
					}

					final var text = getText();
					if (text != null && !text.isEmpty()) {
						beginForeground(g2d);
						final var metrics = g2d.getFontMetrics(getFont());
						final var stringWidth = metrics.stringWidth(text);
						final var ascent = metrics.getAscent();
						final var textHeight = metrics.getHeight();

						final var tx = width / 2 - stringWidth / 2;
						final var ty = height / 2 + ascent - textHeight / 2;

						final var textRect = new Rectangle(tx, ty - ascent, stringWidth, textHeight);
						paintText(g, textRect, text);
					}
				}

				@Override
				public void setMaximumSize(final Dimension maximumSize) {
					checkDimensionIsSquare(maximumSize);
					super.setMaximumSize(maximumSize);
				}

				@Override
				public void setMinimumSize(final Dimension minimumSize) {
					checkDimensionIsSquare(minimumSize);
					super.setMinimumSize(minimumSize);
				}

				@Override
				public void setPreferredSize(final Dimension preferredSize) {
					checkDimensionIsSquare(preferredSize);
					super.setPreferredSize(preferredSize);
				}
			};
		} else {
			round = false;
			button = new AssignmentsButton(main, new EditComponentAction(main, name, component));
		}

		if (component.type() == ComponentType.BUTTON
				&& (round || component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN
						|| component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT
						|| component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT
						|| component.index() == SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP)) {
			// noinspection SuspiciousNameCombination
			button.setPreferredSize(new Dimension(BUTTON_HEIGHT, BUTTON_HEIGHT));
		} else {
			button.setPreferredSize(new Dimension(135, BUTTON_HEIGHT));
		}

		return button;
	}

	/// Prevents deserialization.
	///
	/// @param ignoredStream unused
	/// @throws NotSerializableException always
	@Serial
	private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(AssignmentsScrollPane.class.getName());
	}

	/// Enables or disables all child components recursively.
	@Override
	public void setEnabled(final boolean enabled) {
		GuiUtils.setEnabledRecursive(assignmentsPanel, enabled);
	}

	/// Prevents serialization.
	///
	/// @param ignoredStream unused
	/// @throws NotSerializableException always
	@Serial
	private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(AssignmentsScrollPane.class.getName());
	}

	/// The base class for all buttons in [AssignmentsScrollPane].
	///
	/// Handles theme-aware rendering using FlatLaf UI colors and supports round
	/// or shaped button painting with anti-aliased graphics.
	private static class AssignmentsButton extends JButton {

		@Serial
		private static final long serialVersionUID = 5458020346838696827L;

		/// The main application instance.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final Main main;

		/// Whether the content area of this button should be painted.
		boolean contentAreaFilled = true;

		/// The color used for disabled button text.
		Color disabledText;

		/// The theme color used for the button border in its normal state.
		private Color borderColor;

		/// The theme background color used when the button is disabled.
		private Color disabledBackground;

		/// The theme border color used when the button is disabled.
		private Color disabledBorderColor;

		/// The theme background color used when the button is focused.
		private Color focusedBackground;

		/// The theme border color used when the button is focused.
		private Color focusedBorderColor;

		/// The theme background color used when the button is hovered.
		private Color hoverBackground;

		/// The theme border color used when the button is hovered.
		private Color hoverBorderColor;

		/// The theme background color used when the button is pressed.
		private Color pressedBackground;

		/// Constructs an [AssignmentsButton] with no associated action.
		///
		/// @param main the main application instance
		private AssignmentsButton(final Main main) {
			this.main = main;
			super();
		}

		/// Constructs an [AssignmentsButton] with the given action.
		///
		/// @param main the main application instance
		/// @param action the action to associate with this button
		private AssignmentsButton(final Main main, final Action action) {
			this.main = main;
			super(action);
		}

		/// Configures the graphics context for painting the button's background by
		/// enabling antialiasing and setting the state-dependent background color.
		///
		/// @param g2d the graphics context to configure
		void beginBackground(final Graphics2D g2d) {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final var background = FlatButtonUI.buttonStateColor(this, getBackground(), disabledBackground,
					focusedBackground, hoverBackground, pressedBackground);

			g2d.setColor(FlatUIUtils.deriveColor(background, getBackground()));
		}

		/// Configures the graphics context for painting the button's border by
		/// enabling antialiasing and setting the state-dependent border color.
		///
		/// @param g2d the graphics context to configure
		void beginBorder(final Graphics2D g2d) {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setStroke(BORDER_STROKE);

			final var color = FlatButtonUI.buttonStateColor(this, borderColor, disabledBorderColor, focusedBorderColor,
					hoverBorderColor, null);
			g2d.setColor(color);
		}

		/// Configures the graphics context for painting the button's foreground
		/// (text and icons) by setting the state-dependent foreground color.
		///
		/// @param g2d the graphics context to configure
		void beginForeground(final Graphics2D g2d) {
			g2d.setColor(getForeground());
		}

		@Override
		protected void paintBorder(final Graphics g) {
			if (!isBorderPainted()) {
				return;
			}

			final var g2d = (Graphics2D) g;
			beginBorder(g2d);

			final int lineWidth;
			if (!(g2d.getStroke() instanceof final BasicStroke basicStroke)) {
				throw new UnsupportedOperationException();
			}
			lineWidth = Math.round(basicStroke.getLineWidth());
			final var halfLineWidth = lineWidth / 2;

			// noinspection SuspiciousNameCombination
			g2d.drawRect(halfLineWidth, halfLineWidth, getWidth() - lineWidth, getHeight() - lineWidth);
		}

		/// Paints the given text within the specified rectangle, wrapping it onto
		/// two lines at the centermost space when the text contains spaces.
		///
		/// @param g the graphics context
		/// @param textRect the bounding rectangle in which to paint the text
		/// @param text the text to paint
		void paintText(final Graphics g, final Rectangle textRect, final String text) {
			final var foreground = isEnabled() ? getForeground() : disabledText;

			if (text.length() > 1) {
				final var center = text.length() / 2;
				var centermostSpaceIndex = -1;
				var minDistance = Integer.MAX_VALUE;

				for (var i = 0; i < text.length(); i++) {
					if (text.charAt(i) == ' ') {
						final var distance = Math.abs(i - center);
						if (distance < minDistance) {
							minDistance = distance;
							centermostSpaceIndex = i;
						}
					}
				}

				if (centermostSpaceIndex != -1) {
					final var firstLine = text.substring(0, centermostSpaceIndex);
					final var secondLine = text.substring(centermostSpaceIndex + 1);

					final var metrics = getFontMetrics(g.getFont());
					final var firstLineWidth = metrics.stringWidth(firstLine);
					final var secondLineWidth = metrics.stringWidth(secondLine);
					final var textHeight = metrics.getHeight();

					final var firstLineRect = new Rectangle(textRect.x + (textRect.width - firstLineWidth) / 2,
							textRect.y + (textRect.height - 2 * textHeight) / 2, firstLineWidth, textHeight);
					final var secondLineRect = new Rectangle(textRect.x + (textRect.width - secondLineWidth) / 2,
							textRect.y + (textRect.height - 2 * textHeight) / 2 + textHeight, secondLineWidth,
							textHeight);

					FlatButtonUI.paintText(g, this, firstLineRect, firstLine, foreground);
					FlatButtonUI.paintText(g, this, secondLineRect, secondLine, foreground);
					return;
				}
			}

			FlatButtonUI.paintText(g, this, textRect, text, foreground);
		}

		/// Prevents deserialization.
		///
		/// @param ignoredStream unused
		/// @throws NotSerializableException always
		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(AssignmentsButton.class.getName());
		}

		/// Stores the content area filled flag without delegating to the superclass,
		/// since custom painting is handled manually.
		@Override
		public void setContentAreaFilled(final boolean b) {
			contentAreaFilled = b;
		}

		/// Refreshes all cached theme colors from the current [UIManager] look-and-feel
		/// settings.
		private void updateTheme() {
			focusedBackground = UIManager.getColor("Button.focusedBackground");
			hoverBackground = UIManager.getColor("Button.hoverBackground");
			pressedBackground = UIManager.getColor("Button.pressedBackground");
			disabledBackground = UIManager.getColor("Button.disabledBackground");
			disabledBorderColor = UIManager.getColor("Button.disabledBorderColor");
			disabledText = UIManager.getColor("Button.disabledText");

			focusedBorderColor = Main.LIGHT_BLUE_COLOR.brighter();

			if (main.isDarkLookAndFeel()) {
				borderColor = Color.LIGHT_GRAY;
				hoverBorderColor = Color.WHITE;
			} else {
				borderColor = Color.BLACK;
				hoverBorderColor = Color.GRAY;
			}
		}

		/// Updates the UI delegate and refreshes theme colors.
		@Override
		public void updateUI() {
			super.updateUI();

			updateTheme();

			final var background = FlatButtonUI.buttonStateColor(this, getBackground(), null, focusedBackground,
					hoverBackground, pressedBackground);
			setBackground(background);
		}

		/// Prevents serialization.
		///
		/// @param ignoredStream unused
		/// @throws NotSerializableException always
		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(AssignmentsButton.class.getName());
		}
	}

	/// A custom button used to represent compound stick controls, supporting
	/// pie-shaped segments for directional axes and a circular center region for
	/// the stick press button.
	///
	/// Multiple instances share a button model when they represent two ends of
	/// the same axis, so that hover and press state is synchronized. The
	/// button's hit area and painted shape are determined by its
	/// [CompoundButtonLocation].
	private static final class CompoundButton extends AssignmentsButton {

		@Serial
		private static final long serialVersionUID = 5560396295119690740L;

		/// The position of this button within the circular stick control.
		private final CompoundButtonLocation buttonLocation;

		/// The preferred size inherited from the parent panel.
		private final Dimension preferredSize;

		/// The cached bounding rectangle used to detect when the shape must be
		/// recomputed.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private Shape base;

		/// The cached hit and paint shape for this button.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private Shape shape;

		/// Whether this button can display a swap indicator when sticks are swapped.
		private boolean swapTextPossible;

		/// The display text rendered via the icon painter.
		private String text;

		/// Constructs a center [CompoundButton] for the stick-press component.
		///
		/// @param main the main application instance
		/// @param parentPanel the parent panel whose preferred size governs this
		/// button's preferred size
		/// @param component the gamepad component this button represents
		private CompoundButton(final Main main, final JPanel parentPanel, final Component component) {
			this(main, parentPanel, component, CompoundButtonLocation.CENTER, null);
		}

		/// Constructs a [CompoundButton] at the specified location, optionally sharing
		/// its button model with a peer button representing the opposite end of
		/// the same axis.
		///
		/// @param main the main application instance
		/// @param parentPanel the parent panel whose preferred size governs this
		/// button's preferred size
		/// @param component the gamepad component this button represents
		/// @param buttonLocation the position of this button within the circular
		/// stick control
		/// @param peer the opposing axis button that shares this button's model or
		/// `null` if there is no peer
		private CompoundButton(final Main main, final JPanel parentPanel, final Component component,
				final CompoundButtonLocation buttonLocation, final CompoundButton peer) {
			super(main);

			preferredSize = parentPanel.getPreferredSize();
			this.buttonLocation = buttonLocation;
			if (peer != null) {
				setModel(peer.getModel());
				model.removeActionListener(actionListener);
			}

			model.addChangeListener(_ -> {
				if (model.isRollover()) {
					getParent().setComponentZOrder(this, 0);
				}
			});

			final var componentType = component.type();
			final var componentIndex = component.index();
			final var swapLeftAndRightSticks = main.isSwapLeftAndRightSticks();

			if (componentType == ComponentType.BUTTON) {
				switch (componentIndex) {
				case SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK -> {
					setAction(new EditComponentAction(main,
							Main.STRINGS.getString(swapLeftAndRightSticks ? "RIGHT_STICK" : "LEFT_STICK"), component));

					text = Main.STRINGS.getString(swapLeftAndRightSticks ? "RIGHT_STICK" : "LEFT_STICK");

					swapTextPossible = true;
				}
				case SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK -> {
					setAction(new EditComponentAction(main,
							Main.STRINGS.getString(swapLeftAndRightSticks ? "LEFT_STICK" : "RIGHT_STICK"), component));
					text = Main.STRINGS.getString(swapLeftAndRightSticks ? "LEFT_STICK" : "RIGHT_STICK");

					swapTextPossible = true;
				}
				default -> throw buildInvalidComponentIndexException(componentType, componentIndex);
				}
			} else {
				switch (componentIndex) {
				case SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX -> setAction(new EditComponentAction(main,
						Main.STRINGS.getString(swapLeftAndRightSticks ? "RIGHT_STICK_X_AXIS" : "LEFT_STICK_X_AXIS"),
						component));
				case SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY -> setAction(new EditComponentAction(main,
						Main.STRINGS.getString(swapLeftAndRightSticks ? "RIGHT_STICK_Y_AXIS" : "LEFT_STICK_Y_AXIS"),
						component));
				case SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX -> setAction(new EditComponentAction(main,
						Main.STRINGS.getString(swapLeftAndRightSticks ? "LEFT_STICK_X_AXIS" : "RIGHT_STICK_X_AXIS"),
						component));
				case SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY -> setAction(new EditComponentAction(main,
						Main.STRINGS.getString(swapLeftAndRightSticks ? "LEFT_STICK_Y_AXIS" : "RIGHT_STICK_Y_AXIS"),
						component));
				default -> throw buildInvalidComponentIndexException(componentType, componentIndex);
				}
			}

			setIcon(new Icon() {

				@Override
				public int getIconHeight() {
					return preferredSize.height;
				}

				@Override
				public int getIconWidth() {
					return preferredSize.width;
				}

				private Rectangle getTextRectangle(final String text, final Graphics2D g2d, final int x, final int y,
						final int line) {
					final var metrics = g2d.getFontMetrics(getFont());
					final var textHeight = metrics.getHeight();
					final var ascent = metrics.getAscent();

					final var stringWidth = metrics.stringWidth(text);
					final var tx = x + (getIconWidth() - stringWidth) / 2;
					final var ty = y + (getIconHeight() - textHeight) / 2 + ascent;

					return new Rectangle(tx, ty - ascent + line * textHeight, stringWidth, textHeight);
				}

				@Override
				public void paintIcon(final java.awt.Component c, final Graphics g, final int x, final int y) {
					final var g2d = (Graphics2D) g;

					if (contentAreaFilled) {
						beginBackground(g2d);

						if (shape == null) {
							initShape();
						}

						g2d.fill(shape);
					}

					beginForeground(g2d);
					switch (buttonLocation) {
					case CENTER -> {
						final var textRect = getTextRectangle(text, g2d, x, y, 0);
						paintText(g, textRect, text);

						if (swapTextPossible && main.isSwapLeftAndRightSticks()) {
							final var swappedTextRect = getTextRectangle(Main.SWAPPED_SYMBOL, g2d, x, y, 1);
							paintText(g, swappedTextRect, Main.SWAPPED_SYMBOL);
						}
					}
					case EAST -> {
						final var text = "→";
						final var textRect = getTextRectangle(text, g2d, (int) (x + getWidth() * 0.375), y, 0);
						paintText(g, textRect, text);
					}
					case NORTH -> {
						final var text = "↑";
						final var textRect = getTextRectangle(text, g2d, x, y - (int) (x + getHeight() * 0.375), 0);
						paintText(g, textRect, text);
					}
					case WEST -> {
						final var text = "←";
						final var textRect = getTextRectangle(text, g2d, x - (int) (x + getWidth() * 0.375), y, 0);
						paintText(g, textRect, text);
					}
					case SOUTH -> {
						final var text = "↓";
						final var textRect = getTextRectangle(text, g2d, x, y + (int) (x + getHeight() * 0.375), 0);
						paintText(g, textRect, text);
					}
					}
				}
			});

			initShape();
		}

		/// Builds an [IllegalArgumentException] describing an unrecognized component
		/// index for the given component type.
		///
		/// @param componentType the type of component that was being processed
		/// @param componentIndex the unrecognized index value
		/// @return an exception with a descriptive message
		private static IllegalArgumentException buildInvalidComponentIndexException(final ComponentType componentType,
				final int componentIndex) {
			return new IllegalArgumentException("Invalid componentIndex for " + ComponentType.class.getSimpleName()
					+ " " + componentType + ": " + componentIndex);
		}

		/// Tests whether a point is within this button's shaped region.
		@Override
		public boolean contains(final int x, final int y) {
			if (shape == null) {
				initShape();
			}

			return shape.contains(x, y);
		}

		/// Returns the preferred size inherited from the parent panel.
		///
		/// @return the preferred size
		@Override
		public Dimension getPreferredSize() {
			return new Dimension(preferredSize);
		}

		@Override
		public String getText() {
			return null;
		}

		/// Initializes or recomputes the button's hit and paint shape based on its
		/// current bounds and [CompoundButtonLocation].
		///
		/// For the center location the shape is a filled circle; for directional
		/// locations it is a 90-degree pie arc with the center circle subtracted.
		/// The shape is only recomputed when the bounds have changed.
		private void initShape() {
			if (!getBounds().equals(base)) {
				base = getBounds();

				final var innerSize = getWidth() * 0.5f;
				final var innerPos = innerSize * 0.5f;
				final var innerShape = new Ellipse2D.Float(innerPos, innerPos, innerSize, innerSize);

				if (CompoundButtonLocation.CENTER == buttonLocation) {
					shape = innerShape;
				} else {
					final var outerShape = new Arc2D.Float(1, 1, getWidth() - 2, getHeight() - 2,
							buttonLocation.startAngle, 90f, Arc2D.PIE);
					final var outerArea = new Area(outerShape);
					outerArea.subtract(new Area(innerShape));
					shape = outerArea;
				}
			}
		}

		@Override
		public boolean isContentAreaFilled() {
			return false;
		}

		/// Paints the border by drawing the outline of this button's shape.
		@Override
		protected void paintBorder(final Graphics g) {
			if (!isBorderPainted()) {
				return;
			}

			final var g2d = (Graphics2D) g;
			beginBorder(g2d);

			if (shape == null) {
				initShape();
			}

			g2d.draw(shape);
		}

		/// Initializes the shape geometry before delegating to the superclass paint.
		@Override
		protected void paintComponent(final Graphics g) {
			initShape();
			super.paintComponent(g);
		}

		/// Prevents deserialization.
		///
		/// @param ignoredStream unused
		/// @throws NotSerializableException always
		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(CompoundButton.class.getName());
		}

		@Override
		public void setText(final String text) {
			this.text = text;
		}

		/// Prevents serialization.
		///
		/// @param ignoredStream unused
		/// @throws NotSerializableException always
		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(CompoundButton.class.getName());
		}

		/// Specifies the position of a compound button segment within a circular stick
		/// control.
		///
		/// Each location defines the arc start angle for rendering its pie-shaped
		/// region.
		private enum CompoundButtonLocation {

			/// East (right) segment.
			EAST(-45f),
			/// Center (stick press) region.
			CENTER(0f),
			/// North (up) segment.
			NORTH(45f),
			/// West (left) segment.
			WEST(135f),
			/// South (down) segment.
			SOUTH(225f);

			/// The arc start angle in degrees for this location's pie-shaped segment.
			final float startAngle;

			/// Constructs a [CompoundButtonLocation] constant with the given arc start
			/// angle.
			///
			/// @param startAngle the start angle in degrees for the pie-arc segment
			CompoundButtonLocation(final float startAngle) {
				this.startAngle = startAngle;
			}
		}
	}

	/// An action that opens the [EditActionsDialog] for editing the assignments of
	/// a specific gamepad component.
	///
	/// The action name and short description are derived from the component's
	/// localized label and are used as the button text and tooltip respectively.
	private static final class EditComponentAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -2879419156880580931L;

		/// The gamepad component whose assignments this action edits.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final Component component;

		/// The main application instance.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final Main main;

		/// The localized display name of the component.
		private final String name;

		/// Constructs an [EditComponentAction] that will open the assignment editor for
		/// the given component.
		///
		/// @param main the main application instance
		/// @param name the localized display name of the component
		/// @param component the gamepad component to edit
		private EditComponentAction(final Main main, final String name, final Component component) {
			this.main = main;
			this.name = name;
			this.component = component;

			putValue(NAME, name);
			putValue(SHORT_DESCRIPTION,
					MessageFormat.format(Main.STRINGS.getString("EDIT_COMPONENT_ACTION_DESCRIPTION"), name));
		}

		/// Opens the edit dialog for the associated component.
		@Override
		public void actionPerformed(final ActionEvent e) {
			final var editComponentDialog = new EditActionsDialog(main, component, name);
			editComponentDialog.setVisible(true);
		}

		/// Prevents deserialization.
		///
		/// @param ignoredStream unused
		/// @throws NotSerializableException always
		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(EditComponentAction.class.getName());
		}

		/// Prevents serialization.
		///
		/// @param ignoredStream unused
		/// @throws NotSerializableException always
		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(EditComponentAction.class.getName());
		}
	}

	/// A panel containing four buttons arranged in a cross-pattern, used for the
	/// D-pad and the face button cluster (A, B, X, Y).
	///
	/// Each arm of the cross is a separate component button created via
	/// [AssignmentsScrollPane#createComponentButton], laid out in a
	/// [java.awt.GridBagLayout] with the up button at row 0, left and right at
	/// row 1, and down at row 2.
	private static final class FourWay extends JPanel {

		@Serial
		private static final long serialVersionUID = -5178710302755638535L;

		/// Constructs a [FourWay] with buttons for the up, left, right, and down
		/// directions.
		///
		/// @param assignmentsScrollPane the parent assignments scroll pane
		/// @param upTitle the localized label for the up button
		/// @param upComponent the gamepad component for the up direction
		/// @param leftTitle the localized label for the left button
		/// @param leftComponent the gamepad component for the left direction
		/// @param rightTitle the localized label for the right button
		/// @param rightComponent the gamepad component for the right direction
		/// @param downTitle the localized label for the down button
		/// @param downComponent the gamepad component for the down direction
		private FourWay(final AssignmentsScrollPane assignmentsScrollPane, final String upTitle,
				final Component upComponent, final String leftTitle, final Component leftComponent,
				final String rightTitle, final Component rightComponent, final String downTitle,
				final Component downComponent) {
			super(new GridBagLayout());

			setOpaque(false);

			final var constraints = new GridBagConstraints();
			constraints.insets = new Insets(2, 2, 2, 2);
			constraints.weightx = 1d;
			constraints.weighty = 1d;

			constraints.gridx = 1;
			constraints.gridy = 0;
			add(assignmentsScrollPane.createComponentButton(upTitle, upComponent), constraints);
			constraints.gridx = 0;
			constraints.gridy = 1;
			add(assignmentsScrollPane.createComponentButton(leftTitle, leftComponent), constraints);
			constraints.gridx = 2;
			constraints.gridy = 1;
			add(assignmentsScrollPane.createComponentButton(rightTitle, rightComponent), constraints);
			constraints.gridx = 1;
			constraints.gridy = 2;
			add(assignmentsScrollPane.createComponentButton(downTitle, downComponent), constraints);
		}
	}

	/// A panel representing a gamepad analog stick, composed of compound buttons
	/// for the stick press.
	///
	/// Contains one [CompoundButton] for the center (stick-press) region and
	/// four pie-shaped buttons for the north, south, east, and west axis
	/// directions.
	private static final class Stick extends JPanel {

		@Serial
		private static final long serialVersionUID = -8389190445101809929L;

		/// Constructs a [Stick] for the given stick type, adding compound buttons for
		/// the stick press and the four axis directions.
		///
		/// @param main the main application instance
		/// @param type whether this panel represents the left or right stick
		private Stick(final Main main, final StickType type) {
			final var preferredSize = new Dimension(171, 171);
			setPreferredSize(preferredSize);

			setLayout(new OverlayLayout(this));
			setOpaque(false);

			final var left = type == StickType.LEFT;

			add(new CompoundButton(main, this, new Component(main, ComponentType.BUTTON,
					left ? SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK : SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK)));

			final var xComponent = new Component(main, ComponentType.AXIS,
					left ? SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX : SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX);
			final var yComponent = new Component(main, ComponentType.AXIS,
					left ? SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY : SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY);

			final var northernButton = new CompoundButton(main, this, yComponent,
					CompoundButton.CompoundButtonLocation.NORTH, null);
			add(northernButton);
			final var westernButton = new CompoundButton(main, this, xComponent,
					CompoundButton.CompoundButtonLocation.WEST, null);
			add(westernButton);
			add(new CompoundButton(main, this, xComponent, CompoundButton.CompoundButtonLocation.EAST, westernButton));
			add(new CompoundButton(main, this, yComponent, CompoundButton.CompoundButtonLocation.SOUTH,
					northernButton));
		}

		/// Identifies which analog stick (left or right) a [Stick] panel represents.
		///
		/// The value controls which SDL gamepad axis and button indices are used
		/// when constructing the [CompoundButton] children of the panel.
		private enum StickType {

			/// Left analog stick.
			LEFT,
			/// Right analog stick.
			RIGHT
		}
	}
}

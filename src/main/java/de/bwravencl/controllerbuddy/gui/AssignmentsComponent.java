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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
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
import javax.swing.plaf.UIResource;
import org.lwjgl.sdl.SDLGamepad;

/// A scroll pane that displays a visual representation of a gamepad's controls
/// as interactive buttons.
///
/// Each button corresponds to a gamepad component (axis, button, or D-pad
/// direction) and opens an assignment editor dialog when clicked. The layout
/// mimics a physical gamepad, with sticks, triggers, bumpers, and face
/// buttons placed in their conventional positions.
final class AssignmentsComponent extends JScrollPane {

	/// Height in pixels used for all component buttons.
	private static final int BUTTON_HEIGHT = 50;

	@Serial
	private static final long serialVersionUID = -4096911611882875787L;

	/// The panel that holds the arranged gamepad component buttons.
	private final JPanel assignmentsPanel = new JPanel();

	/// Creates a new assignments component with buttons arranged in a grid to
	/// resemble a gamepad layout.
	///
	/// @param main the main application instance
	AssignmentsComponent(final Main main) {
		assignmentsPanel.setLayout(new GridBagLayout());

		final var constraints = new GridBagConstraints();
		constraints.insets = new Insets(8, 8, 8, 8);
		constraints.weightx = 1d;
		constraints.weighty = 1d;

		constraints.gridx = 0;
		constraints.gridy = 0;
		assignmentsPanel.add(
				createComponentButton(main, Main.STRINGS.getString("LEFT_TRIGGER"),
						new Component(main, ComponentType.AXIS, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER)),
				constraints);

		constraints.gridx = 4;
		constraints.gridy = 0;
		assignmentsPanel.add(
				createComponentButton(main, Main.STRINGS.getString("RIGHT_TRIGGER"),
						new Component(main, ComponentType.AXIS, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER)),
				constraints);

		constraints.gridx = 0;
		constraints.gridy = 1;
		assignmentsPanel.add(
				createComponentButton(main, Main.STRINGS.getString("LEFT_SHOULDER"),
						new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER)),
				constraints);

		constraints.gridx = 2;
		constraints.gridy = 1;
		assignmentsPanel.add(createComponentButton(main, Main.STRINGS.getString("GUIDE_BUTTON"),
				new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE)), constraints);

		constraints.gridx = 4;
		constraints.gridy = 1;
		assignmentsPanel.add(
				createComponentButton(main, Main.STRINGS.getString("RIGHT_SHOULDER"),
						new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER)),
				constraints);

		constraints.gridx = 0;
		constraints.gridy = 2;
		assignmentsPanel.add(new Stick(main, Stick.StickType.LEFT), constraints);

		constraints.gridx = 1;
		constraints.gridy = 2;
		assignmentsPanel.add(createComponentButton(main, Main.STRINGS.getString("BACK_BUTTON"),
				new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_BACK)), constraints);

		constraints.gridx = 3;
		constraints.gridy = 2;
		assignmentsPanel.add(createComponentButton(main, Main.STRINGS.getString("START_BUTTON"),
				new Component(main, ComponentType.BUTTON, SDLGamepad.SDL_GAMEPAD_BUTTON_START)), constraints);

		constraints.gridx = 4;
		constraints.gridy = 2;
		assignmentsPanel.add(new FourWay(main, Main.STRINGS.getString("Y_BUTTON"),
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
				new FourWay(main, Main.STRINGS.getString("DPAD_UP"),
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

		setViewportView(assignmentsPanel);
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

	/// Creates a [JButton] for a gamepad component, using a round [CustomButton]
	/// for face and guide buttons and a standard [JButton] for all others.
	///
	/// @param main the main application instance
	/// @param name the localized display name of the component
	/// @param component the gamepad component this button represents
	/// @return the configured button
	private static JButton createComponentButton(final Main main, final String name, final Component component) {
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
			button = new CustomButton(new EditComponentAction(main, name, component)) {

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
				public void paintComponent(final Graphics g) {
					final var diameter = getDiameter() - 3;
					final var radius = diameter / 2;

					final var width = getWidth();
					final var height = getHeight();

					final var g2d = (Graphics2D) g;

					if (contentAreaFilled && isEnabled()) {
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
			button = new JButton(new EditComponentAction(main, name, component));
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

	/// Enables or disables all child components recursively.
	@Override
	public void setEnabled(final boolean enabled) {
		GuiUtils.setEnabledRecursive(assignmentsPanel, enabled);
	}

	/// A custom button used to represent compound stick controls, supporting
	/// pie-shaped segments for directional axes and a circular center region for
	/// the stick press button.
	///
	/// Multiple instances share a button model when they represent two ends of
	/// the same axis, so that hover and press state is synchronized. The
	/// button's hit area and painted shape are determined by its
	/// [CompoundButtonLocation].
	private static final class CompoundButton extends CustomButton {

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

		/// The opposing axis button that shares this button's model, or `null` if there
		/// is none.
		private CompoundButton peer;

		/// The cached hit and paint shape for this button.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private Shape shape;

		/// Whether this button can display a swap indicator when sticks are swapped.
		private boolean swapTextPossible;

		/// The display text rendered via the icon painter.
		private String text;

		/// Creates a center [CompoundButton] for the stick-press component.
		///
		/// @param main the main application instance
		/// @param parentPanel the parent panel whose preferred size governs this
		/// button's preferred size
		/// @param component the gamepad component this button represents
		private CompoundButton(final Main main, final JPanel parentPanel, final Component component) {
			this(main, parentPanel, component, CompoundButtonLocation.CENTER, null);
		}

		/// Creates a [CompoundButton] at the specified location, optionally sharing
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
			preferredSize = parentPanel.getPreferredSize();
			this.buttonLocation = buttonLocation;
			this.peer = peer;
			if (peer != null) {
				peer.setPeer(this);
				setModel(peer.getModel());
				model.removeActionListener(actionListener);
			}

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
					final var model = getModel();
					final var peerModel = CompoundButton.this.peer != null ? CompoundButton.this.peer.getModel() : null;

					final var g2d = (Graphics2D) g;

					if (contentAreaFilled && (model.isEnabled() || (peerModel != null && peerModel.isEnabled()))) {
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
			return preferredSize;
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

		/// Sets the peer button that represents the opposite end of the same axis.
		///
		/// @param peer the opposing [CompoundButton]
		private void setPeer(final CompoundButton peer) {
			this.peer = peer;
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

			/// Creates a location constant with the given arc start angle.
			///
			/// @param startAngle the start angle in degrees for the pie-arc segment
			CompoundButtonLocation(final float startAngle) {
				this.startAngle = startAngle;
			}
		}
	}

	/// Abstract base class for buttons with custom-painted backgrounds, borders,
	/// and foregrounds.
	///
	/// Handles theme-aware rendering using FlatLaf UI colors and supports round
	/// or shaped button painting with anti-aliased graphics.
	private abstract static class CustomButton extends JButton {

		@Serial
		private static final long serialVersionUID = 5458020346838696827L;

		/// Whether the content area of this button should be painted.
		boolean contentAreaFilled = true;

		/// The color used for disabled button text.
		Color disabledText;

		/// The theme color used for the button border in its normal state.
		private Color borderColor;

		/// The theme background color for default buttons.
		private Color defaultBackground;

		/// Whether default buttons should render their label in bold.
		private boolean defaultBoldText;

		/// The theme border color for default buttons.
		private Color defaultBorderColor;

		/// The theme background color for focused default buttons.
		private Color defaultFocusedBackground;

		/// The theme border color for focused default buttons.
		private Color defaultFocusedBorderColor;

		/// The theme foreground color for default buttons.
		private Color defaultForeground;

		/// The theme background color for hovered default buttons.
		private Color defaultHoverBackground;

		/// The theme border color for hovered default buttons.
		private Color defaultHoverBorderColor;

		/// The theme background color for pressed default buttons.
		private Color defaultPressedBackground;

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

		/// Creates a custom button with no associated action.
		private CustomButton() {
			updateTheme();
			super.setContentAreaFilled(false);
		}

		/// Creates a custom button with the given action.
		///
		/// @param action the action to associate with this button
		private CustomButton(final Action action) {
			super(action);
			updateTheme();
			super.setContentAreaFilled(false);
		}

		/// Configures the graphics context for painting the button's background by
		/// enabling anti-aliasing and setting the state-dependent background color.
		///
		/// @param g2d the graphics context to configure
		void beginBackground(final Graphics2D g2d) {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final var def = isDefaultButton();
			final var background = FlatButtonUI.buttonStateColor(this, def ? defaultBackground : getBackground(), null,
					def ? defaultFocusedBackground : focusedBackground, def ? defaultHoverBackground : hoverBackground,
					def ? defaultPressedBackground : pressedBackground);

			g2d.setColor(FlatUIUtils.deriveColor(background, def ? defaultBackground : getBackground()));
		}

		/// Configures the graphics context for painting the button's border by
		/// enabling anti-aliasing and setting the state-dependent border color.
		///
		/// @param g2d the graphics context to configure
		void beginBorder(final Graphics2D g2d) {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final var def = isDefaultButton();
			final var color = FlatButtonUI.buttonStateColor(this, def ? defaultBorderColor : borderColor,
					disabledBorderColor, def ? defaultFocusedBorderColor : focusedBorderColor,
					def ? defaultHoverBorderColor : hoverBorderColor, null);

			g2d.setColor(color);
		}

		/// Configures the graphics context for painting the button's foreground
		/// (text and icons) by setting the state-dependent foreground color.
		///
		/// @param g2d the graphics context to configure
		void beginForeground(final Graphics2D g2d) {
			final var color = isDefaultButton() ? defaultForeground : getForeground();
			g2d.setColor(color);
		}

		@Override
		public boolean isContentAreaFilled() {
			return false;
		}

		/// Paints the given text within the specified rectangle, wrapping it onto
		/// two lines at the centermost space when the text contains spaces.
		///
		/// @param g the graphics context
		/// @param textRect the bounding rectangle in which to paint the text
		/// @param text the text to paint
		void paintText(final Graphics g, final Rectangle textRect, final String text) {
			if (defaultBoldText && isDefaultButton() && getFont() instanceof UIResource) {
				final var boldFont = g.getFont().deriveFont(Font.BOLD);
				g.setFont(boldFont);

				final var boldWidth = getFontMetrics(boldFont).stringWidth(text);
				if (boldWidth > textRect.width) {
					textRect.x -= (boldWidth - textRect.width) / 2;
					textRect.width = boldWidth;
				}
			}

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

		/// Stores the content area filled flag without delegating to the superclass,
		/// since custom painting is handled manually.
		@Override
		public void setContentAreaFilled(final boolean b) {
			contentAreaFilled = b;
		}

		/// Refreshes all cached theme colors from the current [UIManager] look-and-feel
		/// settings.
		private void updateTheme() {
			defaultForeground = UIManager.getColor("Button.default.foreground");
			defaultBackground = UIManager.getColor("Button.default.background");
			focusedBackground = UIManager.getColor("Button.focusedBackground");
			hoverBackground = UIManager.getColor("Button.hoverBackground");
			pressedBackground = UIManager.getColor("Button.pressedBackground");
			defaultFocusedBackground = UIManager.getColor("Button.default.focusedBackground");
			defaultHoverBackground = UIManager.getColor("Button.default.hoverBackground");
			defaultPressedBackground = UIManager.getColor("Button.default.pressedBackground");
			borderColor = UIManager.getColor("Button.borderColor");
			disabledBorderColor = UIManager.getColor("Button.disabledBorderColor");
			focusedBorderColor = UIManager.getColor("Button.focusedBorderColor");
			hoverBorderColor = UIManager.getColor("Button.hoverBorderColor");
			defaultBorderColor = UIManager.getColor("Button.default.borderColor");
			defaultHoverBorderColor = UIManager.getColor("Button.default.hoverBorderColor");
			defaultFocusedBorderColor = UIManager.getColor("Button.default.focusedBorderColor");
			disabledText = UIManager.getColor("Button.disabledText");
			defaultBoldText = UIManager.getBoolean("Button.default.boldText");
		}

		/// Updates the UI delegate and refreshes theme colors.
		@Override
		public void updateUI() {
			super.updateUI();
			updateTheme();
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

		/// The main application instance used to open the edit dialog.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final Main main;

		/// The localized display name of the component.
		private final String name;

		/// Creates an action that will open the assignment editor for the given
		/// component.
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

	/// A panel containing four buttons arranged in a cross pattern, used for the
	/// D-pad and the face button cluster (A, B, X, Y).
	///
	/// Each arm of the cross is a separate component button created via
	/// [AssignmentsComponent#createComponentButton], laid out in a
	/// [java.awt.GridBagLayout] with the up button at row 0, left and right at
	/// row 1, and down at row 2.
	private static final class FourWay extends JPanel {

		@Serial
		private static final long serialVersionUID = -5178710302755638535L;

		/// Creates a four-way cross panel with buttons for the up, left, right, and
		/// down directions.
		///
		/// @param main the main application instance
		/// @param upTitle the localized label for the up button
		/// @param upComponent the gamepad component for the up direction
		/// @param leftTitle the localized label for the left button
		/// @param leftComponent the gamepad component for the left direction
		/// @param rightTitle the localized label for the right button
		/// @param rightComponent the gamepad component for the right direction
		/// @param downTitle the localized label for the down button
		/// @param downComponent the gamepad component for the down direction
		private FourWay(final Main main, final String upTitle, final Component upComponent, final String leftTitle,
				final Component leftComponent, final String rightTitle, final Component rightComponent,
				final String downTitle, final Component downComponent) {
			super(new GridBagLayout());

			final var constraints = new GridBagConstraints();
			constraints.insets = new Insets(2, 2, 2, 2);
			constraints.weightx = 1d;
			constraints.weighty = 1d;

			constraints.gridx = 1;
			constraints.gridy = 0;
			add(createComponentButton(main, upTitle, upComponent), constraints);
			constraints.gridx = 0;
			constraints.gridy = 1;
			add(createComponentButton(main, leftTitle, leftComponent), constraints);
			constraints.gridx = 2;
			constraints.gridy = 1;
			add(createComponentButton(main, rightTitle, rightComponent), constraints);
			constraints.gridx = 1;
			constraints.gridy = 2;
			add(createComponentButton(main, downTitle, downComponent), constraints);
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

		/// Creates a stick panel for the given stick type, adding compound buttons for
		/// the stick press and the four axis directions.
		///
		/// @param main the main application instance
		/// @param type whether this panel represents the left or right stick
		private Stick(final Main main, final StickType type) {
			final var preferredSize = new Dimension(171, 171);
			setPreferredSize(preferredSize);

			setLayout(new OverlayLayout(this));

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

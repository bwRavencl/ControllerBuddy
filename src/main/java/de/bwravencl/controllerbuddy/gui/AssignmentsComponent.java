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

final class AssignmentsComponent extends JScrollPane {

	private static final int BUTTON_HEIGHT = 50;

	@Serial
	private static final long serialVersionUID = -4096911611882875787L;

	private final JPanel assignmentsPanel = new JPanel();

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

	private static void checkDimensionIsSquare(final Dimension dimension) {
		if (dimension.width != dimension.height) {
			throw new IllegalArgumentException("Parameter dimension is not square");
		}
	}

	private static JButton createComponentButton(final Main main, final String name, final Component component) {
		final boolean round;
		final JButton button;
		if (component.getType() == ComponentType.BUTTON && (component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH
				|| component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_EAST
				|| component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_WEST
				|| component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH
				|| component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_BACK
				|| component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_START
				|| component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE)) {
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

		if (component.getType() == ComponentType.BUTTON
				&& (round || component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN
						|| component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT
						|| component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT
						|| component.getIndex() == SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP)) {
			// noinspection SuspiciousNameCombination
			button.setPreferredSize(new Dimension(BUTTON_HEIGHT, BUTTON_HEIGHT));
		} else {
			button.setPreferredSize(new Dimension(135, BUTTON_HEIGHT));
		}

		return button;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		GuiUtils.setEnabledRecursive(assignmentsPanel, enabled);
	}

	private static final class CompoundButton extends CustomButton {

		@Serial
		private static final long serialVersionUID = 5560396295119690740L;

		private final CompoundButtonLocation buttonLocation;

		private final Dimension preferredSize;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private Shape base;

		private CompoundButton peer;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private Shape shape;

		private boolean swapTextPossible;

		private String text;

		private CompoundButton(final Main main, final JPanel parentPanel, final Component component) {
			this(main, parentPanel, component, CompoundButtonLocation.CENTER, null);
		}

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

			final var componentType = component.getType();
			final var componentIndex = component.getIndex();
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

		private static IllegalArgumentException buildInvalidComponentIndexException(final ComponentType componentType,
				final int componentIndex) {
			return new IllegalArgumentException("Invalid componentIndex for " + ComponentType.class.getSimpleName()
					+ " " + componentType + ": " + componentIndex);
		}

		@Override
		public boolean contains(final int x, final int y) {
			if (shape == null) {
				initShape();
			}

			return shape.contains(x, y);
		}

		@Override
		public Dimension getPreferredSize() {
			return preferredSize;
		}

		@Override
		public String getText() {
			return null;
		}

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

		@Override
		protected void paintComponent(final Graphics g) {
			initShape();
			super.paintComponent(g);
		}

		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(CompoundButton.class.getName());
		}

		private void setPeer(final CompoundButton peer) {
			this.peer = peer;
		}

		@Override
		public void setText(final String text) {
			this.text = text;
		}

		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(CompoundButton.class.getName());
		}

		private enum CompoundButtonLocation {

			EAST(-45f), CENTER(0f), NORTH(45f), WEST(135f), SOUTH(225f);

			final float startAngle;

			CompoundButtonLocation(final float startAngle) {
				this.startAngle = startAngle;
			}
		}
	}

	private abstract static class CustomButton extends JButton {

		@Serial
		private static final long serialVersionUID = 5458020346838696827L;

		protected boolean defaultBoldText;

		boolean contentAreaFilled = true;

		Color disabledText;

		private Color borderColor;

		private Color defaultBackground;

		private Color defaultBorderColor;

		private Color defaultFocusedBackground;

		private Color defaultFocusedBorderColor;

		private Color defaultForeground;

		private Color defaultHoverBackground;

		private Color defaultHoverBorderColor;

		private Color defaultPressedBackground;

		private Color disabledBorderColor;

		private Color focusedBackground;

		private Color focusedBorderColor;

		private Color hoverBackground;

		private Color hoverBorderColor;

		private Color pressedBackground;

		private CustomButton() {
			updateTheme();
			super.setContentAreaFilled(false);
		}

		private CustomButton(final Action action) {
			super(action);
			updateTheme();
			super.setContentAreaFilled(false);
		}

		void beginBackground(final Graphics2D g2d) {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final var def = isDefaultButton();
			final var background = FlatButtonUI.buttonStateColor(this, def ? defaultBackground : getBackground(), null,
					def ? defaultFocusedBackground : focusedBackground, def ? defaultHoverBackground : hoverBackground,
					def ? defaultPressedBackground : pressedBackground);

			g2d.setColor(FlatUIUtils.deriveColor(background, def ? defaultBackground : getBackground()));
		}

		void beginBorder(final Graphics2D g2d) {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final var def = isDefaultButton();
			final var color = FlatButtonUI.buttonStateColor(this, def ? defaultBorderColor : borderColor,
					disabledBorderColor, def ? defaultFocusedBorderColor : focusedBorderColor,
					def ? defaultHoverBorderColor : hoverBorderColor, null);

			g2d.setColor(color);
		}

		void beginForeground(final Graphics2D g2d) {
			final var color = isDefaultButton() ? defaultForeground : getForeground();
			g2d.setColor(color);
		}

		@Override
		public boolean isContentAreaFilled() {
			return false;
		}

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

			FlatButtonUI.paintText(g, this, textRect, text, isEnabled() ? getForeground() : disabledText);
		}

		@Override
		public void setContentAreaFilled(final boolean b) {
			contentAreaFilled = b;
		}

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

		@Override
		public void updateUI() {
			super.updateUI();
			updateTheme();
		}
	}

	private static final class EditComponentAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -2879419156880580931L;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final Component component;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final Main main;

		private final String name;

		private EditComponentAction(final Main main, final String name, final Component component) {
			this.main = main;
			this.name = name;
			this.component = component;

			putValue(NAME, name);
			putValue(SHORT_DESCRIPTION,
					MessageFormat.format(Main.STRINGS.getString("EDIT_COMPONENT_ACTION_DESCRIPTION"), name));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var editComponentDialog = new EditActionsDialog(main, component, name);
			editComponentDialog.setVisible(true);
		}

		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(EditComponentAction.class.getName());
		}

		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(EditComponentAction.class.getName());
		}
	}

	private static final class FourWay extends JPanel {

		@Serial
		private static final long serialVersionUID = -5178710302755638535L;

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

	private static final class Stick extends JPanel {

		@Serial
		private static final long serialVersionUID = -8389190445101809929L;

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

		private enum StickType {
			LEFT, RIGHT
		}
	}
}

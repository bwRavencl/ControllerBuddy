/* Copyright (C) 2019  Matteo Hausner
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

import static de.bwravencl.controllerbuddy.gui.GuiUtils.setEnabledRecursive;
import static de.bwravencl.controllerbuddy.gui.GuiUtils.usingOceanTheme;
import static de.bwravencl.controllerbuddy.gui.Main.STRING_RESOURCE_BUNDLE_BASENAME;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_A;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_B;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_BACK;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_GUIDE;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_START;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_Y;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import de.bwravencl.controllerbuddy.input.Mode.Component;
import de.bwravencl.controllerbuddy.input.Mode.Component.ComponentType;
import de.bwravencl.controllerbuddy.util.ResourceBundleUtil;

public final class AssignmentsComponent extends JScrollPane {

	private static final class CompoundButton extends CustomButton {

		private enum CompoundButtonLocation {

			Center(0f), North(45f), East(135f), South(225f), West(-45f);

			private final float degree;

			CompoundButtonLocation(final float degree) {
				this.degree = degree;
			}

			public float getStartDegree() {
				return degree;
			}

		}

		private static final long serialVersionUID = 5560396295119690740L;

		private transient Shape shape;
		private transient Shape base;
		private final CompoundButtonLocation buttonLocation;
		private final Dimension preferredSize;
		private String text;
		private CompoundButton peer;

		private CompoundButton(final Main main, final JPanel parentPanel, final Component component) {
			this(main, parentPanel, component, CompoundButtonLocation.Center, null);
		}

		private CompoundButton(final Main main, final JPanel parentPanel, final Component component,
				final CompoundButtonLocation buttonLocation, final CompoundButton peer) {
			super();

			preferredSize = parentPanel.getPreferredSize();
			this.buttonLocation = buttonLocation;
			this.peer = peer;
			if (peer != null)
				peer.setPeer(this);

			if (component.type == ComponentType.BUTTON) {
				if (component.index == GLFW_GAMEPAD_BUTTON_LEFT_THUMB) {
					setAction(new EditComponentAction(main, rb.getString("LEFT_THUMB"), component));
					text = rb.getString("LEFT_STICK");
				} else if (component.index == GLFW_GAMEPAD_BUTTON_RIGHT_THUMB) {
					setAction(new EditComponentAction(main, rb.getString("RIGHT_THUMB"), component));
					text = rb.getString("RIGHT_STICK");
				} else
					throw new IllegalArgumentException();
			} else
				switch (component.index) {
				case GLFW_GAMEPAD_AXIS_LEFT_X:
					setAction(new EditComponentAction(main, rb.getString("LEFT_STICK_X_AXIS"), component));
					break;
				case GLFW_GAMEPAD_AXIS_LEFT_Y:
					setAction(new EditComponentAction(main, rb.getString("LEFT_STICK_Y_AXIS"), component));
					break;
				case GLFW_GAMEPAD_AXIS_RIGHT_X:
					setAction(new EditComponentAction(main, rb.getString("RIGHT_STICK_X_AXIS"), component));
					break;
				case GLFW_GAMEPAD_AXIS_RIGHT_Y:
					setAction(new EditComponentAction(main, rb.getString("RIGHT_STICK_Y_AXIS"), component));
					break;
				default:
					throw new IllegalArgumentException();
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

				@Override
				public void paintIcon(final java.awt.Component c, final Graphics g, final int x, final int y) {
					final var model = getModel();
					final var peerModel = CompoundButton.this.peer != null ? CompoundButton.this.peer.getModel() : null;

					final var g2d = (Graphics2D) g;

					final var enabled = model.isEnabled() || peerModel != null && peerModel.isEnabled();
					if (enabled) {
						final var armed = model.isArmed() || peerModel != null && peerModel.isArmed();
						final var pressed = model.isPressed() || peerModel != null && peerModel.isPressed();

						if (!pressed || armed) {
							beginBackground(g2d);

							if (shape == null)
								initShape();

							g2d.fill(shape);
						}
					}

					if (buttonLocation == CompoundButtonLocation.Center && text != null) {
						beginForeground(g2d);

						final var metrics = g2d.getFontMetrics(getFont());
						final int tx = x + (getIconWidth() - metrics.stringWidth(text)) / 2;
						final int ty = y + (getIconHeight() - metrics.getHeight()) / 2 + metrics.getAscent();

						g2d.drawString(text, tx, ty);
					}
				}
			});

			setFocusPainted(true);
			setContentAreaFilled(false);

			initShape();
		}

		@Override
		public boolean contains(final int x, final int y) {
			if (shape == null)
				initShape();

			return shape.contains(x, y);
		}

		@Override
		public Dimension getPreferredSize() {
			return preferredSize;
		}

		private void initShape() {
			if (!getBounds().equals(base)) {
				base = getBounds();
				final var ww = getWidth() * 0.5f;
				final var xx = ww * 0.5f;
				final var innerShape = new Ellipse2D.Float(xx, xx, ww, ww);
				if (CompoundButtonLocation.Center == buttonLocation)
					shape = innerShape;
				else {
					final var outerShape = new Arc2D.Float(1, 1, getWidth() - 2, getHeight() - 2,
							buttonLocation.getStartDegree(), 90f, Arc2D.PIE);
					final var outerArea = new Area(outerShape);
					outerArea.subtract(new Area(innerShape));
					shape = outerArea;
				}
			}
		}

		@Override
		protected void paintBorder(final Graphics g) {
			if (!isBorderPainted())
				return;

			final var g2d = (Graphics2D) g;
			beginBorder(g2d);

			if (shape == null)
				initShape();

			g2d.draw(shape);
		}

		@Override
		protected void paintComponent(final Graphics g) {
			initShape();
			super.paintComponent(g);
		}

		private void setPeer(final CompoundButton peer) {
			this.peer = peer;
		}

		@Override
		public void setText(final String text) {
			this.text = text;
		}

	}

	private static abstract class CustomButton extends JButton {

		private static final long serialVersionUID = 5458020346838696827L;

		Color selectColor;
		float[] gradientFractions = null;
		Color[] gradientColors = null;

		private CustomButton() {
			updateTheme();
		}

		private CustomButton(final Action action) {
			super(action);
			updateTheme();
		}

		void beginBackground(final Graphics2D g2d) {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if (!model.isPressed())
				if (gradientFractions != null && gradientColors != null)
					g2d.setPaint(
							new LinearGradientPaint(0f, 0f, 0f, getHeight() - 1f, gradientFractions, gradientColors));
				else
					g2d.setColor(getBackground());
			else if (model.isArmed())
				g2d.setColor(selectColor);
		}

		void beginBorder(final Graphics2D g2d) {
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if (usingOceanTheme() && isRolloverEnabled() && getModel().isRollover())
				g2d.setColor(MetalLookAndFeel.getControlShadow());
			else
				g2d.setColor(MetalLookAndFeel.getControlDarkShadow());
		}

		void beginForeground(final Graphics2D g2d) {
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			final Color color;
			if (isForegroundSet())
				color = getForeground();
			else
				color = isEnabled() ? MetalLookAndFeel.getControlTextColor()
						: MetalLookAndFeel.getInactiveControlTextColor();
			g2d.setColor(color);

			g2d.setFont(getFont());
		}

		private void updateTheme() {
			selectColor = (Color) UIManager.get("Button.select");

			if (usingOceanTheme()) {
				final var buttonGradient = UIManager.get("Button.gradient");
				if (buttonGradient instanceof List) {
					Float r1 = null;
					final var buttonGradientColors = new Color[3];

					var i = 0;
					for (final var e : (List<?>) buttonGradient) {
						if (e instanceof Float && r1 == null)
							r1 = (Float) e;

						if (e instanceof Color) {
							buttonGradientColors[i] = (Color) e;
							i++;
						}
					}

					if (r1 != null && i == 3) {
						gradientFractions = new float[] { 0f, r1, r1 * 2f, 1f };
						gradientColors = new Color[] { buttonGradientColors[0], buttonGradientColors[1],
								buttonGradientColors[0], buttonGradientColors[2] };
					}
				}
			} else {
				gradientFractions = null;
				gradientColors = null;
			}
		}

		@Override
		public void updateUI() {
			super.updateUI();
			updateTheme();
		}

	}

	private static final class EditComponentAction extends AbstractAction {

		private static final long serialVersionUID = 8811608785278071903L;

		private final Main main;
		private final String name;
		private final Component component;

		private EditComponentAction(final Main main, final String name, final Component component) {
			this.main = main;
			this.name = name;
			this.component = component;

			putValue(NAME, name);
			putValue(SHORT_DESCRIPTION, MessageFormat.format(rb.getString("EDIT_COMPONENT_ACTION_DESCRIPTION"), name));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final EditActionsDialog editComponentDialog = new EditActionsDialog(main, component, name);
			editComponentDialog.setVisible(true);
		}

	}

	private static final class FourWay extends JPanel {

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

		private enum StickType {
			Left, Right
		}

		private static final long serialVersionUID = -8389190445101809929L;

		private Stick(final Main main, final StickType type) {
			final var preferredSize = new Dimension(171, 171);
			setPreferredSize(preferredSize);

			setLayout(new OverlayLayout(this));

			final var left = type == StickType.Left;

			add(new CompoundButton(main, this, new Component(ComponentType.BUTTON,
					left ? GLFW_GAMEPAD_BUTTON_LEFT_THUMB : GLFW_GAMEPAD_BUTTON_RIGHT_THUMB)));

			final var xComponent = new Component(ComponentType.AXIS,
					left ? GLFW_GAMEPAD_AXIS_LEFT_X : GLFW_GAMEPAD_AXIS_RIGHT_X);
			final var yComponent = new Component(ComponentType.AXIS,
					left ? GLFW_GAMEPAD_AXIS_LEFT_Y : GLFW_GAMEPAD_AXIS_RIGHT_Y);

			final var northernButton = new CompoundButton(main, this, yComponent,
					CompoundButton.CompoundButtonLocation.North, null);
			add(northernButton);
			final var westernButton = new CompoundButton(main, this, xComponent,
					CompoundButton.CompoundButtonLocation.West, null);
			add(westernButton);
			add(new CompoundButton(main, this, xComponent, CompoundButton.CompoundButtonLocation.East, westernButton));
			add(new CompoundButton(main, this, yComponent, CompoundButton.CompoundButtonLocation.South,
					northernButton));
		}

	}

	private static final int BUTTON_HEIGHT = 50;

	private static final long serialVersionUID = -4096911611882875787L;

	private static final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(STRING_RESOURCE_BUNDLE_BASENAME,
			Locale.getDefault());

	private static void checkDimensionIsSquare(final Dimension dimension) {
		if (dimension.width != dimension.height)
			throw new IllegalArgumentException();
	}

	private static JButton createComponentButton(final Main main, final String name, final Component component) {
		final boolean round;
		final JButton button;
		if (component.type == ComponentType.BUTTON && (component.index == GLFW_GAMEPAD_BUTTON_A
				|| component.index == GLFW_GAMEPAD_BUTTON_B || component.index == GLFW_GAMEPAD_BUTTON_X
				|| component.index == GLFW_GAMEPAD_BUTTON_Y || component.index == GLFW_GAMEPAD_BUTTON_BACK
				|| component.index == GLFW_GAMEPAD_BUTTON_START || component.index == GLFW_GAMEPAD_BUTTON_GUIDE)) {
			round = true;
			button = new CustomButton(new EditComponentAction(main, name, component)) {

				private static final long serialVersionUID = 8467379031897370934L;

				@Override
				public boolean contains(final int x, final int y) {
					final var radius = getDiameter() / 2;
					return Point2D.distance(x, y, getWidth() / 2, getHeight() / 2) < radius;
				}

				private int getDiameter() {
					return Math.min(getWidth(), getHeight());
				}

				@Override
				protected void paintBorder(final Graphics g) {
					if (!isBorderPainted())
						return;

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

					final var ovalWidth = width % 2 != 0 ? width + 1 : width;
					final var ovalHeight = height % 2 != 0 ? height + 1 : height;

					final var g2d = (Graphics2D) g;

					if (model.isEnabled() && (!model.isPressed() || model.isArmed())) {
						beginBackground(g2d);
						g2d.fillOval(ovalWidth / 2 - radius, ovalHeight / 2 - radius, diameter, diameter);
					}

					beginForeground(g2d);

					final var text = getText();
					final var metrics = g2d.getFontMetrics(getFont());
					final int tx = width / 2 - metrics.stringWidth(text) / 2;
					final int ty = height / 2 + metrics.getAscent() - metrics.getHeight() / 2;

					g2d.drawString(text, tx, ty);
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

		if (component.type == ComponentType.BUTTON && (round || component.index == GLFW_GAMEPAD_BUTTON_DPAD_DOWN
				|| component.index == GLFW_GAMEPAD_BUTTON_DPAD_LEFT || component.index == GLFW_GAMEPAD_BUTTON_DPAD_RIGHT
				|| component.index == GLFW_GAMEPAD_BUTTON_DPAD_UP))
			button.setPreferredSize(new Dimension(BUTTON_HEIGHT, BUTTON_HEIGHT));
		else
			button.setPreferredSize(new Dimension(135, BUTTON_HEIGHT));

		return button;
	}

	private final JPanel assignmentsPanel = new JPanel();

	AssignmentsComponent(final Main main) {
		assignmentsPanel.setLayout(new GridBagLayout());

		final var constraints = new GridBagConstraints();
		constraints.insets = new Insets(8, 8, 8, 8);
		constraints.weightx = 1d;
		constraints.weighty = 1d;

		constraints.gridx = 0;
		constraints.gridy = 0;
		assignmentsPanel.add(createComponentButton(main, rb.getString("LEFT_TRIGGER"),
				new Component(ComponentType.AXIS, GLFW_GAMEPAD_AXIS_LEFT_TRIGGER)), constraints);

		constraints.gridx = 4;
		constraints.gridy = 0;
		assignmentsPanel.add(createComponentButton(main, rb.getString("RIGHT_TRIGGER"),
				new Component(ComponentType.AXIS, GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER)), constraints);

		constraints.gridx = 0;
		constraints.gridy = 1;
		assignmentsPanel.add(createComponentButton(main, rb.getString("LEFT_BUMPER"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_LEFT_BUMPER)), constraints);

		constraints.gridx = 2;
		constraints.gridy = 1;
		assignmentsPanel.add(createComponentButton(main, rb.getString("GUIDE_BUTTON"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_GUIDE)), constraints);

		constraints.gridx = 4;
		constraints.gridy = 1;
		assignmentsPanel.add(createComponentButton(main, rb.getString("RIGHT_BUMPER"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER)), constraints);

		constraints.gridx = 0;
		constraints.gridy = 2;
		assignmentsPanel.add(new Stick(main, Stick.StickType.Left), constraints);

		constraints.gridx = 1;
		constraints.gridy = 2;
		assignmentsPanel.add(createComponentButton(main, rb.getString("BACK_BUTTON"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_BACK)), constraints);

		constraints.gridx = 3;
		constraints.gridy = 2;
		assignmentsPanel.add(createComponentButton(main, rb.getString("START_BUTTON"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_START)), constraints);

		constraints.gridx = 4;
		constraints.gridy = 2;
		assignmentsPanel.add(
				new FourWay(main, rb.getString("Y_BUTTON"), new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_Y),
						rb.getString("X_BUTTON"), new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_X),
						rb.getString("B_BUTTON"), new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_B),
						rb.getString("A_BUTTON"), new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_A)),
				constraints);

		constraints.gridx = 1;
		constraints.gridy = 3;
		assignmentsPanel.add(new FourWay(main, rb.getString("DPAD_UP"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_DPAD_UP), rb.getString("DPAD_LEFT"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_DPAD_LEFT), rb.getString("DPAD_RIGHT"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_DPAD_RIGHT), rb.getString("DPAD_DOWN"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_DPAD_DOWN)), constraints);

		constraints.gridx = 3;
		constraints.gridy = 3;
		assignmentsPanel.add(new Stick(main, Stick.StickType.Right), constraints);

		setViewportBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		setViewportView(assignmentsPanel);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		setEnabledRecursive(assignmentsPanel, enabled);
	}

}

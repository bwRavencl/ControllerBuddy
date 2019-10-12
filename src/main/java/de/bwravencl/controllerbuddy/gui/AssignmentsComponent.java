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
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
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

public class AssignmentsComponent extends JScrollPane {

	private static class CompoundButton extends JButton {

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

		private final Color SELECT_COLOR = (Color) UIManager.get("Button.select");

		private float[] gradientFractions = null;
		private Color[] gradientColors = null;
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
			preferredSize = parentPanel.getPreferredSize();
			this.buttonLocation = buttonLocation;
			this.peer = peer;
			if (peer != null)
				peer.setPeer(this);

			updateTheme();

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
					final var g2d = (Graphics2D) g.create();
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					final var model = getModel();
					final var peerModel = CompoundButton.this.peer != null ? CompoundButton.this.peer.getModel() : null;

					final var enabled = model.isEnabled() || peerModel != null && peerModel.isEnabled();
					if (enabled) {
						final var armed = model.isArmed() || peerModel != null && peerModel.isArmed();
						final var pressed = model.isPressed() || peerModel != null && peerModel.isPressed();

						if (!pressed || armed) {
							if (!pressed)
								if (gradientFractions != null && gradientColors != null)
									g2d.setPaint(new LinearGradientPaint(0f, 0f, 0f, getHeight() - 1f,
											gradientFractions, gradientColors));
								else
									g2d.setColor(getBackground());
							else if (armed)
								g2d.setColor(SELECT_COLOR);

							if (shape == null)
								initShape();

							g2d.fill(shape);
						}
					}

					if (buttonLocation == CompoundButtonLocation.Center && text != null) {
						g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
								RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
						g2d.setColor(enabled ? MetalLookAndFeel.getControlTextColor()
								: MetalLookAndFeel.getInactiveControlTextColor());

						final var font = getFont();
						final var metrics = g.getFontMetrics(font);
						final int tx = x + (getIconWidth() - metrics.stringWidth(text)) / 2;
						final int ty = y + (getIconHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
						g2d.setFont(font);
						g2d.drawString(text, tx, ty);
					}

					g2d.dispose();
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
			final var g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if (isRolloverEnabled() && getModel().isRollover()
					|| peer != null && peer.isRolloverEnabled() && peer.getModel().isRollover())
				g2d.setColor(MetalLookAndFeel.getControlShadow());
			else
				g2d.setColor(MetalLookAndFeel.getControlDarkShadow());

			if (shape == null)
				initShape();

			g2d.draw(shape);
			g2d.dispose();
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

		private void updateTheme() {
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

			}
		}

		@Override
		public void updateUI() {
			super.updateUI();
			updateTheme();
		}

	}

	private static class EditComponentAction extends AbstractAction {

		private static final long serialVersionUID = 8811608785278071903L;

		private final Main main;
		private final String name;
		private final Component component;

		private EditComponentAction(final Main main, final String name, final Component component) {
			this.main = main;
			this.name = name;
			this.component = component;

			putValue(NAME, name);
			putValue(SHORT_DESCRIPTION, rb.getString("EDIT_COMPONENT_ACTION_DESCRIPTION_PREFIX") + name
					+ rb.getString("EDIT_COMPONENT_ACTION_DESCRIPTION_SUFFIX"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final EditActionsDialog editComponentDialog = new EditActionsDialog(main, component, name);
			editComponentDialog.setVisible(true);
		}

	}

	private static class FourWay extends JPanel {

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
			add(createButton(main, upTitle, upComponent), constraints);
			constraints.gridx = 0;
			constraints.gridy = 1;
			add(createButton(main, leftTitle, leftComponent), constraints);
			constraints.gridx = 2;
			constraints.gridy = 1;
			add(createButton(main, rightTitle, rightComponent), constraints);
			constraints.gridx = 1;
			constraints.gridy = 2;
			add(createButton(main, downTitle, downComponent), constraints);
		}

	}

	private static class Stick extends JPanel {

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

	private static JButton createButton(final Main main, final String name, final Component component) {
		final var button = new JButton(new EditComponentAction(main, name, component));

		if (component.type == ComponentType.BUTTON && (component.index == GLFW_GAMEPAD_BUTTON_A
				|| component.index == GLFW_GAMEPAD_BUTTON_B || component.index == GLFW_GAMEPAD_BUTTON_X
				|| component.index == GLFW_GAMEPAD_BUTTON_Y || component.index == GLFW_GAMEPAD_BUTTON_DPAD_DOWN
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
		assignmentsPanel.add(createButton(main, rb.getString("LEFT_TRIGGER"),
				new Component(ComponentType.AXIS, GLFW_GAMEPAD_AXIS_LEFT_TRIGGER)), constraints);

		constraints.gridx = 4;
		constraints.gridy = 0;
		assignmentsPanel.add(createButton(main, rb.getString("RIGHT_TRIGGER"),
				new Component(ComponentType.AXIS, GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER)), constraints);

		constraints.gridx = 0;
		constraints.gridy = 1;
		assignmentsPanel.add(createButton(main, rb.getString("LEFT_BUMPER"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_LEFT_BUMPER)), constraints);

		constraints.gridx = 2;
		constraints.gridy = 1;
		assignmentsPanel.add(createButton(main, rb.getString("GUIDE_BUTTON"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_GUIDE)), constraints);

		constraints.gridx = 4;
		constraints.gridy = 1;
		assignmentsPanel.add(createButton(main, rb.getString("RIGHT_BUMPER"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER)), constraints);

		constraints.gridx = 0;
		constraints.gridy = 2;
		assignmentsPanel.add(new Stick(main, Stick.StickType.Left), constraints);

		constraints.gridx = 1;
		constraints.gridy = 2;
		assignmentsPanel.add(createButton(main, rb.getString("BACK_BUTTON"),
				new Component(ComponentType.BUTTON, GLFW_GAMEPAD_BUTTON_BACK)), constraints);

		constraints.gridx = 3;
		constraints.gridy = 2;
		assignmentsPanel.add(createButton(main, rb.getString("START_BUTTON"),
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

		setViewportBorder(BorderFactory.createMatteBorder(10, 10, 0, 10, assignmentsPanel.getBackground()));
		setViewportView(assignmentsPanel);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		setEnabledRecursive(assignmentsPanel, enabled);
	}

}

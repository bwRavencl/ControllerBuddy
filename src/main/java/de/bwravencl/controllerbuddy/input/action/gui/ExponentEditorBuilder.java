/*
 * Copyright (C) 2019 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action.gui;

import com.formdev.flatlaf.ui.FlatRoundBorder;
import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.action.IAction;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.Serial;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

/// Editor builder for exponent (response curve) properties, providing a spinner
/// with a range of 0.1 to 10.0 alongside a live power-function plot.
///
/// The plot visualizes the current curve shape in real time as the user
/// adjusts the exponent value, making it easier to understand the effect of
/// different response curves on axis input.
public final class ExponentEditorBuilder extends NumberEditorBuilder<Float> {

	/// Constructs an exponent editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose exponent property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws ReflectiveOperationException if reflection operations fail
	public ExponentEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws ReflectiveOperationException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		super.buildEditor(parentPanel);

		parentPanel.add(Box.createHorizontalStrut(Main.DEFAULT_HGAP));

		final var powerFunctionPlotter = new PowerFunctionPlotter((float) initialValue);
		parentPanel.add(powerFunctionPlotter);

		for (final var changeListener : spinner.getChangeListeners()) {
			if (changeListener instanceof final JSpinnerSetPropertyChangeListener spinnerSetPropertyChangeListener) {
				spinnerSetPropertyChangeListener.setValueConsumer(powerFunctionPlotter);
				break;
			}
		}
	}

	@Override
	Comparable<Float> getMaximum() {
		return 10f;
	}

	@Override
	Comparable<Float> getMinimum() {
		return 0.1f;
	}

	@Override
	Number getStepSize() {
		return 0.1f;
	}

	/// Component that renders a live plot of the power function `y = x^power`
	/// within its bounds.
	///
	/// Implements [Consumer] so it can be registered as a value consumer on the
	/// spinner change listener; each time the exponent value changes, the component
	/// repaints itself to reflect the new curve shape.
	private static final class PowerFunctionPlotter extends JComponent implements Consumer<Object> {

		@Serial
		private static final long serialVersionUID = 5075932419255249325L;

		/// The background color used to fill the plot area.
		private final Color defaultBackground;

		/// The foreground color used to draw the curve.
		private final Color defaultForeground;

		/// The current exponent value for the power function curve.
		private float power;

		/// Constructs a plotter with the given initial exponent value.
		///
		/// @param power the initial exponent to use when drawing the curve
		private PowerFunctionPlotter(final float power) {
			this.power = power;

			defaultBackground = UIManager.getColor("Button.background");
			defaultForeground = UIManager.getColor("Button.foreground");

			setBorder(new FlatRoundBorder());
			setPreferredSize(new Dimension(48, 48));
		}

		@Override
		public void accept(final Object t) {
			if (t instanceof final Float floatValue) {
				power = floatValue;
				repaint();
			}
		}

		/// Calculates the Y pixel coordinate for a given X position on the power curve.
		///
		/// Maps the input X pixel position to an output Y pixel position according to
		/// the formula `y = x ^ power`, scaled to fit within the plot dimensions.
		///
		/// @param x the X pixel coordinate within the plot area
		/// @param plotWidth the total width of the plot area in pixels
		/// @param plotHeight the total height of the plot area in pixels
		/// @return the Y pixel coordinate corresponding to the given X
		private int calculateY(final int x, final int plotWidth, final int plotHeight) {
			return plotHeight - 1
					- (x == 0 ? 0 : (int) (plotHeight / Math.pow((double) (plotWidth - 1) / (double) x, power)));
		}

		@Override
		public void paintComponent(final Graphics g) {
			super.paintComponent(g);

			final var insets = getInsets();

			final var plotWidth = getWidth() - (insets.left + insets.right);
			final var plotHeight = getHeight() - (insets.bottom + insets.top);

			g.setColor(defaultBackground);
			g.fillRect(insets.left, insets.top, plotWidth, plotHeight);

			g.setColor(defaultForeground);

			for (var x1 = 0; x1 < plotWidth - 1; x1++) {
				final var y1 = calculateY(x1, plotWidth, plotHeight);
				final var x2 = x1 + 1;
				final var y2 = calculateY(x2, plotWidth, plotHeight);

				g.drawLine(x1 + insets.left, y1 + insets.bottom, x2 + insets.left, y2 + insets.bottom);
			}
		}
	}
}

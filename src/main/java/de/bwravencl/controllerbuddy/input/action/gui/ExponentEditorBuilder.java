/* Copyright (C) 2019  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.formdev.flatlaf.ui.FlatRoundBorder;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.action.IAction;

public final class ExponentEditorBuilder extends NumberEditorBuilder<Float> {

	private static final class PowerFunctionPlotter extends JComponent implements Consumer<Object> {

		@Serial
		private static final long serialVersionUID = 5075932419255249325L;

		private float power;

		private final Color defaultBackground;
		private final Color defaultForeground;

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

	public ExponentEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws SecurityException, NoSuchMethodException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		super.buildEditor(parentPanel);

		parentPanel.add(Box.createHorizontalStrut(Main.DEFAULT_HGAP));

		final var powerFunctionPlotter = new PowerFunctionPlotter((float) initialValue);
		parentPanel.add(powerFunctionPlotter);

		Arrays.stream(spinner.getChangeListeners())
				.filter(changeListener -> changeListener instanceof JSpinnerSetPropertyChangeListener).findFirst()
				.ifPresent(changeListener -> ((JSpinnerSetPropertyChangeListener) changeListener)
						.setValueConsumer(powerFunctionPlotter));
	}

	@Override
	Comparable<Float> getMaximum() {
		return 10f;
	}

	@Override
	Comparable<Float> getMinimum() {
		return 1f;
	}

	@Override
	Number getStepSize() {
		return 0.1f;
	}
}

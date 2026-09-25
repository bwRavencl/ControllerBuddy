package de.bwravencl.controllerbuddy.gui;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.Serial;
import javax.swing.JFrame;

class HideableCursorFrame extends JFrame {

	private static final Cursor BLANK_CURSOR = Toolkit.getDefaultToolkit()
			.createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank");

	@Serial
	private static final long serialVersionUID = 1720972235987331916L;

	private boolean cursorInvisible;

	private boolean makeCursorVisibleOnMovement;

	HideableCursorFrame() {
		init();
	}

	HideableCursorFrame(final String title) {
		super(title);
		init();
	}

	private void init() {
		final var glassPane = getGlassPane();

		glassPane.addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseMoved(final MouseEvent e) {
				super.mouseMoved(e);

				if (makeCursorVisibleOnMovement) {
					setCursor(null);
					cursorInvisible = false;
					makeCursorVisibleOnMovement = false;
					glassPane.setVisible(false);
				}
			}
		});
	}

	void setCursorInvisible(final boolean cursorInvisible) {
		if (!cursorInvisible) {
			makeCursorVisibleOnMovement = true;
			return;
		}

		makeCursorVisibleOnMovement = false;

		if (this.cursorInvisible) {
			return;
		}

		setCursor(BLANK_CURSOR);
		this.cursorInvisible = true;
		getGlassPane().setVisible(true);
	}
}

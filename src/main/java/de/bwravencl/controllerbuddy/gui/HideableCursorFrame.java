package de.bwravencl.controllerbuddy.gui;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.Serial;
import javax.swing.JFrame;

class HideableCursorFrame extends JFrame {

	private static final Cursor BLANK_CURSOR = Toolkit.getDefaultToolkit()
			.createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank");

	@Serial
	private static final long serialVersionUID = 1720972235987331916L;

	boolean cursorInvisible;

	HideableCursorFrame() {
	}

	HideableCursorFrame(final String title) {
		super(title);
	}

	boolean isCursorInvisible() {
		return cursorInvisible;
	}

	void setCursorInvisible(final boolean cursorInvisible) {
		if (this.cursorInvisible == cursorInvisible) {
			return;
		}

		setCursor(cursorInvisible ? BLANK_CURSOR : null);
		this.cursorInvisible = cursorInvisible;
	}
}

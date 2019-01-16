package de.bwravencl.controllerbuddy.input;

import java.awt.Color;

public class OverlayAxis implements Cloneable {

	public Color color;

	public boolean inverted;

	public OverlayAxis() {
		this(new Color(0, 0, 0, 128), false);
	}

	public OverlayAxis(final Color color, final boolean inverted) {
		this.color = color;
		this.inverted = inverted;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new OverlayAxis(new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()),
				inverted);
	}

}

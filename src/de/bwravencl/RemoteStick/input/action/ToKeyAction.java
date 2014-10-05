package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.KeyStroke;

public abstract class ToKeyAction extends InvertableAction {

	public static final String description = "Key";

	protected boolean downUp = false;
	protected boolean wasUp = true;

	protected KeyStroke keystroke;

	public boolean isDownUp() {
		return downUp;
	}

	public void setDownUp(boolean downUp) {
		this.downUp = downUp;
	}

	public KeyStroke getKeystroke() {
		return keystroke;
	}

	public void setKeystroke(KeyStroke keystroke) {
		this.keystroke = keystroke;
	}

	@Override
	public String toString() {
		return "Key";
	}

}

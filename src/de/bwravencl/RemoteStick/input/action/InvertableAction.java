package de.bwravencl.RemoteStick.input.action;

public abstract class InvertableAction {

	protected boolean invert = false;

	public boolean isInvert() {
		return invert;
	}

	public void setInvert(Boolean invert) {
		this.invert = invert;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
}

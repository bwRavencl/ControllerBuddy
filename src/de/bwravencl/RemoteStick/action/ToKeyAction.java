package de.bwravencl.RemoteStick.action;

public abstract class ToKeyAction extends InvertableAction {
	
	protected String keyCode = "";
	
	public String getKeyCode() {
		return keyCode;
	}
	
	public void setKeyCode(String keyCode) {
		this.keyCode = keyCode;
	}
}

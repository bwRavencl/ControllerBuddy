package de.bwravencl.RemoteStick.input.action;

public abstract class ToScrollAction extends InvertableAction {

	public static final int DEFAULT_CLICKS = 1;

	protected int clicks = DEFAULT_CLICKS;

	public int getClicks() {
		return clicks;
	}

	public void setClicks(Integer clicks) {
		this.clicks = clicks;
	}

	@Override
	public String toString() {
		return rb.getString("TO_SCROLL_ACTION_STRING");
	}

}

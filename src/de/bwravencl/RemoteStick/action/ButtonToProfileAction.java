package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class ButtonToProfileAction implements IAction {

	// public static final long TOGGLE_TIME = 250L;

	private boolean toggle = false;
	// private long lastToggle = 0L;
	private boolean up = true;
	private int profileId = 0;

	public boolean isToggle() {
		return toggle;
	}

	public void setToggle(boolean toggle) {
		this.toggle = toggle;
	}

	public int getProfileId() {
		return profileId;
	}

	public void setProfileId(int profileId) {
		this.profileId = profileId;
	}

	@Override
	public void doAction(Joystick joystick, float rValue) {
		/*
		 * if ((!toggle || (toggle && System.currentTimeMillis() - lastToggle >
		 * TOGGLE_TIME)) && rValue > 0.5f && joystick.getActiveProfile() == 0) {
		 * joystick.setActiveProfile(profileId); lastToggle =
		 * System.currentTimeMillis(); } else if (((rValue < 0.5f && !toggle) ||
		 * (rValue > 0.5f && toggle && System .currentTimeMillis() - lastToggle
		 * > TOGGLE_TIME)) && joystick.getActiveProfile() == profileId) {
		 * joystick.setActiveProfile(0); joystick.getDownKeys().clear();
		 * lastToggle = System.currentTimeMillis(); }
		 */

		if (rValue < 0.5f) {
			if (toggle)
				up = true;
			else {
				if (joystick.getActiveProfile() == profileId) {
					joystick.setActiveProfile(0);
					joystick.getDownKeyCodes().clear();
				}
			}
		} else {
			if (toggle) {
				if (up) {
					if (joystick.getActiveProfile() == 0)
						joystick.setActiveProfile(profileId);
					else if (joystick.getActiveProfile() == profileId) {
						joystick.setActiveProfile(0);
						joystick.getDownKeyCodes().clear();
					}
					up = false;
				}
			} else if (joystick.getActiveProfile() == 0)
				joystick.setActiveProfile(profileId);
		}
	}

}

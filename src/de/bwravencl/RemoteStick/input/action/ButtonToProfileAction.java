package de.bwravencl.RemoteStick.input.action;

import de.bwravencl.RemoteStick.input.Input;

public class ButtonToProfileAction implements IAction {

	private boolean toggle = false;
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
	public void doAction(Input joystick, float value) {
		if (value < 0.5f) {
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

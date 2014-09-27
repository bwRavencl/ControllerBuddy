package de.bwravencl.RemoteStick.action;

import de.bwravencl.RemoteStick.Joystick;

public class ButtonToProfileAction implements IAction {

	protected int profileId = 0;

	public int getProfileId() {
		return profileId;
	}

	public void setProfileId(int profileId) {
		this.profileId = profileId;
	}

	@Override
	public void doAction(Joystick joystick, float rValue) {
		if (rValue > 0.5f && joystick.getActiveProfile() == 0)
			joystick.setActiveProfile(profileId);
		else if (rValue < 0.5f && joystick.getActiveProfile() == profileId)
			joystick.setActiveProfile(0);
	}

}

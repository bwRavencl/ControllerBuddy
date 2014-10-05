package de.bwravencl.RemoteStick.input.action;

import java.util.UUID;

import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.Profile;

public class ButtonToProfileAction implements IAction {

	public static final String description = "Profile";

	private boolean toggle = false;
	private boolean up = true;

	private UUID profileUuid;

	public boolean isToggle() {
		return toggle;
	}

	public void setToggle(boolean toggle) {
		this.toggle = toggle;
	}

	public UUID getProfileUuid() {
		return profileUuid;
	}

	public void setProfileUuid(UUID profileUuid) {
		this.profileUuid = profileUuid;
	}

	@Override
	public void doAction(Input joystick, float value) {
		if (value < 0.5f) {
			if (toggle)
				up = true;
			else {
				if (joystick.getActiveProfile().getUuid().equals(profileUuid)) {
					joystick.setActiveProfile(0);
					joystick.getDownKeyCodes().clear();
				}
			}
		} else {
			if (toggle) {
				if (up) {
					if (joystick
							.getActiveProfile()
							.getUuid()
							.equals(UUID
									.fromString(Profile.DEFAULT_PROFILE_UUID_STRING)))
						joystick.setActiveProfile(profileUuid);
					else if (joystick.getActiveProfile().getUuid()
							.equals(profileUuid)) {
						joystick.setActiveProfile(0);
						joystick.getDownKeyCodes().clear();
					}
					up = false;
				}
			} else if (joystick
					.getActiveProfile()
					.getUuid()
					.equals(UUID
							.fromString(Profile.DEFAULT_PROFILE_UUID_STRING)))
				joystick.setActiveProfile(profileUuid);
		}
	}

	@Override
	public String toString() {
		return "Profile";
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}

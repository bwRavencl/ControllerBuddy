package de.bwravencl.RemoteStick.input.action;

import java.util.List;
import java.util.UUID;

import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.Profile;

public class ButtonToProfileAction implements IAction {

	public static final String description = "Profile";

	private boolean toggle = false;
	private boolean up = true;

	private UUID profileUuid;

	public ButtonToProfileAction() {
		final List<Profile> profiles = Input.getProfiles();

		if (profiles.size() > 1)
			profileUuid = profiles.get(1).getUuid();
	}

	public boolean isToggle() {
		return toggle;
	}

	public void setToggle(Boolean toggle) {
		this.toggle = toggle;
	}

	public Profile getProfile() {
		for (Profile p : Input.getProfiles())
			if (profileUuid.equals(p.getUuid()))
				return p;

		return null;
	}

	public void setProfile(Profile profile) {
		profileUuid = profile.getUuid();
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
					if (Input.isDefaultProfile(joystick.getActiveProfile()))
						joystick.setActiveProfile(profileUuid);
					else if (joystick.getActiveProfile().getUuid()
							.equals(profileUuid)) {
						joystick.setActiveProfile(0);
						joystick.getDownKeyCodes().clear();
					}
					up = false;
				}
			} else if (Input.isDefaultProfile(joystick.getActiveProfile()))
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

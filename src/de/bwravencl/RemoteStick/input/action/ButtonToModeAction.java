package de.bwravencl.RemoteStick.input.action;

import java.util.List;
import java.util.UUID;

import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.Mode;
import de.bwravencl.RemoteStick.input.Profile;

public class ButtonToModeAction implements IAction {

	public static final String description = "Mode";

	private boolean toggle = false;
	private boolean up = true;

	private UUID modeUuid;

	public ButtonToModeAction() {
		final List<Mode> modes = Input.getProfile().getModes();

		if (modes.size() > 1)
			modeUuid = modes.get(1).getUuid();
	}

	public boolean isToggle() {
		return toggle;
	}

	public void setToggle(Boolean toggle) {
		this.toggle = toggle;
	}

	public Mode getMode() {
		for (Mode m : Input.getProfile().getModes())
			if (modeUuid.equals(m.getUuid()))
				return m;

		return null;
	}

	public void setMode(Mode mode) {
		modeUuid = mode.getUuid();
	}

	@Override
	public void doAction(Input input, float value) {
		final Profile profile = Input.getProfile();

		if (value < 0.5f) {
			if (toggle)
				up = true;
			else {
				if (profile.getActiveMode().getUuid().equals(modeUuid)) {
					profile.setActiveMode(0);
					input.getDownKeyCodes().clear();
				}
			}
		} else {
			if (toggle) {
				if (up) {
					if (Profile.isDefaultMode(profile.getActiveMode()))
						profile.setActiveMode(modeUuid);
					else if (profile.getActiveMode().getUuid().equals(modeUuid)) {
						profile.setActiveMode(0);
						input.getDownKeyCodes().clear();
					}
					up = false;
				}
			} else if (Profile.isDefaultMode(profile.getActiveMode()))
				profile.setActiveMode(modeUuid);
		}
	}

	@Override
	public String toString() {
		return "Mode";
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}

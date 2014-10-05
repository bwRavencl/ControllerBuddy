package de.bwravencl.RemoteStick.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.bwravencl.RemoteStick.ServerThread;
import de.bwravencl.RemoteStick.input.action.AxisToAxisAction;
import de.bwravencl.RemoteStick.input.action.AxisToButtonAction;
import de.bwravencl.RemoteStick.input.action.AxisToKeyAction;
import de.bwravencl.RemoteStick.input.action.AxisToRelativeAxisAction;
import de.bwravencl.RemoteStick.input.action.AxisToScrollAction;
import de.bwravencl.RemoteStick.input.action.ButtonToButtonAction;
import de.bwravencl.RemoteStick.input.action.ButtonToKeyAction;
import de.bwravencl.RemoteStick.input.action.ButtonToProfileAction;
import de.bwravencl.RemoteStick.input.action.ButtonToScrollAction;
import de.bwravencl.RemoteStick.input.action.CursorAction;
import de.bwravencl.RemoteStick.input.action.IAction;
import net.java.games.input.Component;
import net.java.games.input.Controller;

public class Input {

	public static final int N_AXIS = 8;
	public static final int ID_AXIS_NONE = -1;
	public static final int ID_X_AXIS = 0;
	public static final int ID_Y_AXIS = 1;
	public static final int ID_Z_AXIS = 2;
	public static final int ID_RX_AXIS = 3;
	public static final int ID_RY_AXIS = 4;
	public static final int ID_RZ_AXIS = 5;
	public static final int ID_S0_AXIS = 6;
	public static final int ID_S1_AXIS = 7;
	public static final int ID_BUTTON_NONE = -1;

	public static final String DEFAULT_PROFILE_UUID_STRING = "067e6162-3b6f-4ae2-a171-2470b63dff00";
	public static final String DEFAULT_PROFILE_DESCRIPTION = "Default Profile";

	private ServerThread serverThread;
	private long maxAxisValue = 0;
	private int nButtons = 0;

	private int[] axis = new int[N_AXIS];
	private boolean[] buttons = new boolean[nButtons];

	private final Controller controller;
	private int activeProfileIndex = 0;

	private Map<String, ButtonToProfileAction> componentToProfileActionMap = new HashMap<String, ButtonToProfileAction>();
	private List<Profile> profiles = new ArrayList<Profile>();

	private int cursorDeltaX = 0;
	private int cursorDeltaY = 0;
	private final Set<String> downKeysCodes = new HashSet<String>();
	private final Set<KeyStroke> downUpKeyStrokes = new HashSet<KeyStroke>();
	private int scrollClicks = 0;

	public Input(Controller controller) {
		this.controller = controller;

		System.out.println("Controller: " + controller.getName());

		controller.poll();
		for (Component c : controller.getComponents()) {
			System.out.println(c.getName() + " " + c.getPollData());
		}

		final Profile defaultProfile = new Profile(DEFAULT_PROFILE_UUID_STRING);
		defaultProfile.setDescription(DEFAULT_PROFILE_DESCRIPTION);
		profiles.add(defaultProfile);

		Profile profile1 = new Profile("54947df8-0e9e-4471-a2f9-9af509fb5889");
		profile1.setDescription("View mode");
		Profile profile2 = new Profile("046b6c7f-0b8a-43b9-b35d-6489e6daee91");
		profile2.setDescription("Mouse mode");

		List<IAction> xAxisActionsP0 = new ArrayList<>();
		AxisToAxisAction xAxisAction0 = new AxisToAxisAction();
		xAxisAction0.setAxisId(ID_Z_AXIS);
		xAxisActionsP0.add(xAxisAction0);
		defaultProfile.getComponentToActionMap().put("x", xAxisActionsP0);
		List<IAction> xAxisActionsP1 = new ArrayList<>();
		AxisToKeyAction xAxisAction1 = new AxisToKeyAction();
		KeyStroke xAxisAction1Keystroke = new KeyStroke();
		xAxisAction1Keystroke.setKeyCodes(new String[] { "VK_I" });
		xAxisAction1.setKeystroke(xAxisAction1Keystroke);
		xAxisAction1.setMinAxisValue(0.9f);
		xAxisAction1.setMaxAxisValue(1.0f);
		xAxisAction1.setDownUp(false);
		xAxisActionsP1.add(xAxisAction1);
		profile1.getComponentToActionMap().put("x", xAxisActionsP1);

		List<IAction> yAxisActionsP0 = new ArrayList<>();
		AxisToRelativeAxisAction yAxisAction0P0 = new AxisToRelativeAxisAction();
		yAxisAction0P0.setAxisId(ID_S0_AXIS);
		yAxisAction0P0.setInvert(false);
		yAxisAction0P0.setSensitivity(2.0f);
		yAxisActionsP0.add(yAxisAction0P0);
		AxisToButtonAction yAxisAction1P0 = new AxisToButtonAction();
		yAxisAction1P0.setButtonId(1);
		yAxisAction1P0.setMinAxisValue(0.75f);
		yAxisAction1P0.setMinAxisValue(1.0f);
		yAxisActionsP0.add(yAxisAction1P0);
		defaultProfile.getComponentToActionMap().put("y", yAxisActionsP0);
		List<IAction> yAxisActionsP2 = new ArrayList<>();
		AxisToScrollAction yAxisAction0P2 = new AxisToScrollAction();
		yAxisAction0P2.setClicks(10);
		yAxisActionsP2.add(yAxisAction0P2);
		profile2.getComponentToActionMap().put("y", yAxisActionsP2);

		List<IAction> rxAxisActionsP0 = new ArrayList<>();
		AxisToAxisAction rxAxisAction0P0 = new AxisToAxisAction();
		rxAxisAction0P0.setAxisId(ID_X_AXIS);
		rxAxisActionsP0.add(rxAxisAction0P0);
		defaultProfile.getComponentToActionMap().put("z", rxAxisActionsP0);
		List<IAction> rxAxisActionsP2 = new ArrayList<>();
		CursorAction rxAxisAction0P2 = new CursorAction();
		rxAxisAction0P2.setAxis(CursorAction.Axis.X);
		rxAxisActionsP2.add(rxAxisAction0P2);
		profile2.getComponentToActionMap().put("z", rxAxisActionsP2);

		List<IAction> ryAxisActionsP0 = new ArrayList<>();
		AxisToAxisAction ryAxisAction0P0 = new AxisToAxisAction();
		ryAxisAction0P0.setAxisId(ID_Y_AXIS);
		ryAxisActionsP0.add(ryAxisAction0P0);
		defaultProfile.getComponentToActionMap().put("rz", ryAxisActionsP0);
		List<IAction> ryAxisActionsP2 = new ArrayList<>();
		CursorAction ryAxisAction0P2 = new CursorAction();
		ryAxisAction0P2.setAxis(CursorAction.Axis.Y);
		ryAxisActionsP2.add(ryAxisAction0P2);
		profile2.getComponentToActionMap().put("rz", ryAxisActionsP2);

		List<IAction> xButtonActionsP0 = new ArrayList<>();
		ButtonToButtonAction xButtonAction0P0 = new ButtonToButtonAction();
		xButtonAction0P0.setButtonId(0);
		xButtonActionsP0.add(xButtonAction0P0);
		defaultProfile.getComponentToActionMap().put("14", xButtonActionsP0);
		List<IAction> xButtonActionsP2 = new ArrayList<>();
		ButtonToKeyAction xButtonAction0P2 = new ButtonToKeyAction();
		KeyStroke xButtonAction0P2Keystroke = new KeyStroke();
		xButtonAction0P2Keystroke.setKeyCodes(new String[] { "LBUTTON" });
		xButtonAction0P2.setKeystroke(xButtonAction0P2Keystroke);
		xButtonActionsP2.add(xButtonAction0P2);
		profile2.getComponentToActionMap().put("14", xButtonActionsP2);

		List<IAction> oButtonActions = new ArrayList<>();
		ButtonToKeyAction oButtonAction0 = new ButtonToKeyAction();
		KeyStroke oButtonAction0Keystroke = new KeyStroke();
		oButtonAction0Keystroke.setModifierCodes(new String[] { "SHIFT" });
		oButtonAction0Keystroke.setKeyCodes(new String[] { "VK_A" });
		oButtonAction0.setKeystroke(oButtonAction0Keystroke);
		oButtonAction0.setDownUp(true);
		oButtonActions.add(oButtonAction0);
		defaultProfile.getComponentToActionMap().put("13", oButtonActions);

		List<IAction> triangleButtonActionsP2 = new ArrayList<>();
		ButtonToScrollAction triangleButtonAction = new ButtonToScrollAction();
		triangleButtonAction.setClicks(1);
		triangleButtonAction.setInvert(true);
		triangleButtonActionsP2.add(triangleButtonAction);
		profile2.getComponentToActionMap().put("12", triangleButtonActionsP2);

		List<IAction> squareButtonActionsP2 = new ArrayList<>();
		ButtonToScrollAction squareButtonAction = new ButtonToScrollAction();
		squareButtonAction.setClicks(1);
		squareButtonActionsP2.add(squareButtonAction);
		profile2.getComponentToActionMap().put("15", squareButtonActionsP2);

		List<IAction> r1ButtonActions = new ArrayList<>();
		ButtonToProfileAction r1ButtonAction0 = new ButtonToProfileAction();
		r1ButtonAction0.setProfileUuid(profile1.getUuid());
		r1ButtonActions.add(r1ButtonAction0);
		componentToProfileActionMap.put("11", r1ButtonAction0);

		List<IAction> l1ButtonActions = new ArrayList<>();
		ButtonToProfileAction l1ButtonAction0 = new ButtonToProfileAction();
		l1ButtonAction0.setProfileUuid(profile2.getUuid());
		l1ButtonAction0.setToggle(true);
		l1ButtonActions.add(l1ButtonAction0);
		componentToProfileActionMap.put("10", l1ButtonAction0);

		profiles.add(profile1);
		profiles.add(profile2);
	}

	public void poll() {
		controller.poll();

		for (Component c : controller.getComponents()) {
			ButtonToProfileAction profileAction = componentToProfileActionMap
					.get(c.getName());
			if (profileAction != null)
				profileAction.doAction(this, c.getPollData());

			if (profiles.size() > 0 && activeProfileIndex < profiles.size()) {
				Map<String, List<IAction>> componentToActionMap = profiles.get(
						activeProfileIndex).getComponentToActionMap();
				List<IAction> actions = componentToActionMap.get(c.getName());
				if (actions == null)
					actions = profiles.get(0).getComponentToActionMap()
							.get(c.getName());

				if (actions != null)
					for (IAction a : actions)
						a.doAction(this, c.getPollData());
			}
		}

		System.out.println("Profile " + String.valueOf(activeProfileIndex));
	}

	public Controller getController() {
		return controller;
	}

	public Map<String, ButtonToProfileAction> getComponentToProfileActionMap() {
		return componentToProfileActionMap;
	}

	public void setComponentToProfileActionMap(
			Map<String, ButtonToProfileAction> componentToProfileActionMap) {
		this.componentToProfileActionMap = componentToProfileActionMap;
	}

	public List<Profile> getProfiles() {
		return profiles;
	}

	public void setProfiles(List<Profile> profiles) {
		this.profiles = profiles;
	}

	public ServerThread getServerThread() {
		return serverThread;
	}

	public void setServerThread(ServerThread serverThread) {
		this.serverThread = serverThread;
	}

	public long getMaxAxisValue() {
		return maxAxisValue;
	}

	public void setMaxAxisValue(long maxAxisValue) {
		this.maxAxisValue = maxAxisValue;
	}

	public int getnButtons() {
		return nButtons;
	}

	public void setnButtons(int nButtons) {
		this.nButtons = nButtons;
		buttons = new boolean[nButtons];
	}

	public int[] getAxis() {
		return axis;
	}

	public void setAxis(int id, int value) {
		value = Math.max(value, 0);
		value = Math.min(value, (int) maxAxisValue);

		axis[id] = value;
	}

	public void setAxis(int id, float value) {
		value = Math.max(value, -1.0f);
		value = Math.min(value, 1.0f);

		setAxis(id, (int) normalize(value, -1.0f, 1.0f, 0.0f, maxAxisValue));
	}

	public boolean[] getButtons() {
		return buttons;
	}

	public void setButtons(int id, boolean value) {
		buttons[id] = value;
	}

	public void setButtons(int id, float value) {
		if (value < 0.5f)
			setButtons(id, false);
		else
			setButtons(id, true);
	}

	public Profile getActiveProfile() {
		return profiles.get(activeProfileIndex);
	}

	public void setActiveProfile(int activeProfileIndex) {
		if (profiles.size() > activeProfileIndex)
			this.activeProfileIndex = activeProfileIndex;
	}

	public void setActiveProfile(UUID profileUuid) {
		for (Profile p : profiles) {
			if (p.getUuid().equals(profileUuid)) {
				setActiveProfile(profiles.indexOf(p));
				return;
			}
		}
	}

	public Set<String> getDownKeyCodes() {
		return downKeysCodes;
	}

	public Set<KeyStroke> getDownUpKeyStrokes() {
		return downUpKeyStrokes;
	}

	public int getCursorDeltaX() {
		return cursorDeltaX;
	}

	public void setCursorDeltaY(int cursorDeltaY) {
		this.cursorDeltaY = cursorDeltaY;
	}

	public int getCursorDeltaY() {
		return cursorDeltaY;
	}

	public void setCursorDeltaX(int cursorDeltaX) {
		this.cursorDeltaX = cursorDeltaX;
	}

	public int getScrollClicks() {
		return scrollClicks;
	}

	public void setScrollClicks(int scrollClicks) {
		this.scrollClicks = scrollClicks;
	}

	public static float normalize(float value, float inMin, float inMax,
			float outMin, float outMax) {
		final float newValue;
		final float oldRange = (inMax - inMin);

		if (oldRange == 0)
			newValue = outMin;
		else {
			float newRange = (outMax - outMin);
			newValue = (((value - inMin) * newRange) / oldRange) + outMin;
		}

		return newValue;
	}

}

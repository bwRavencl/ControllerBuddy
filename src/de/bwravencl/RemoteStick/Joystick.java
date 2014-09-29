package de.bwravencl.RemoteStick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.bwravencl.RemoteStick.action.AxisToAxisAction;
import de.bwravencl.RemoteStick.action.AxisToButtonAction;
import de.bwravencl.RemoteStick.action.AxisToKeyAction;
import de.bwravencl.RemoteStick.action.AxisToRelativeAxisAction;
import de.bwravencl.RemoteStick.action.ButtonToButtonAction;
import de.bwravencl.RemoteStick.action.ButtonToKeyAction;
import de.bwravencl.RemoteStick.action.ButtonToProfileAction;
import de.bwravencl.RemoteStick.action.IAction;
import de.bwravencl.RemoteStick.action.CursorAction;
import net.java.games.input.Component;
import net.java.games.input.Controller;

public class Joystick {

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

	public static final int N_PROFILES = 16;

	private ServerThread serverThread;
	private long maxAxisValue = 0;
	private int nButtons = 0;

	private int[] axis = new int[N_AXIS];
	private boolean[] buttons = new boolean[nButtons];

	private final Controller controller;
	private int activeProfile = 0;

	private final Map<String, ButtonToProfileAction> componentToProfileActionMap = new HashMap<String, ButtonToProfileAction>();
	private final List<Profile> profiles = new ArrayList<Profile>();

	private int cursorDeltaX = 0;
	private int cursorDeltaY = 0;
	private final Set<String> downKeysCodes = new HashSet<String>();
	private final Set<KeyStroke> downUpKeyStrokes = new HashSet<KeyStroke>();

	public Joystick(ServerThread serverThread, Controller controller) {
		this.serverThread = serverThread;
		this.controller = controller;

		System.out.println("Controller: " + controller.getName());

		controller.poll();
		for (Component c : controller.getComponents()) {
			System.out.println(c.getName() + " " + c.getPollData());
		}

		Profile profile0 = new Profile();
		profile0.setDescription("Main profile");
		Profile profile1 = new Profile();
		profile1.setDescription("View mode");
		Profile profile2 = new Profile();
		profile2.setDescription("Mouse mode");

		HashSet<IAction> xAxisActionsP0 = new HashSet<>();
		AxisToAxisAction xAxisAction0 = new AxisToAxisAction();
		xAxisAction0.setvAxisId(ID_Z_AXIS);
		xAxisActionsP0.add(xAxisAction0);
		profile0.getComponentToActionMap().put("x", xAxisActionsP0);
		HashSet<IAction> xAxisActionsP1 = new HashSet<>();
		AxisToKeyAction xAxisAction1 = new AxisToKeyAction();
		KeyStroke xAxisAction1Keystroke = new KeyStroke();
		xAxisAction1Keystroke.setKeyCodes(new String[] { "VK_I" });
		xAxisAction1.setKeystroke(xAxisAction1Keystroke);
		xAxisAction1.setMinAxisValueKeyDown(0.9f);
		xAxisAction1.setMaxAxisValueKeyDown(1.0f);
		xAxisAction1.setDownUp(false);
		xAxisActionsP1.add(xAxisAction1);
		profile1.getComponentToActionMap().put("x", xAxisActionsP1);

		HashSet<IAction> yAxisActions = new HashSet<>();
		AxisToRelativeAxisAction yAxisAction0 = new AxisToRelativeAxisAction();
		yAxisAction0.setvAxisId(ID_S0_AXIS);
		yAxisAction0.setInvert(false);
		yAxisAction0.setSensitivity(2.0f);
		yAxisActions.add(yAxisAction0);
		AxisToButtonAction yAxisAction1 = new AxisToButtonAction();
		yAxisAction1.setvButtonId(1);
		yAxisAction1.setMinAxisValueButtonDown(0.75f);
		yAxisAction1.setMinAxisValueButtonDown(1.0f);
		yAxisActions.add(yAxisAction1);
		profile0.getComponentToActionMap().put("y", yAxisActions);

		HashSet<IAction> rxAxisActionsP0 = new HashSet<>();
		AxisToAxisAction rxAxisAction0P0 = new AxisToAxisAction();
		rxAxisAction0P0.setvAxisId(ID_X_AXIS);
		rxAxisActionsP0.add(rxAxisAction0P0);
		profile0.getComponentToActionMap().put("z", rxAxisActionsP0);
		HashSet<IAction> rxAxisActionsP2 = new HashSet<>();
		CursorAction rxAxisAction0P2 = new CursorAction();
		rxAxisAction0P2.setAxis(CursorAction.Axis.X);
		rxAxisActionsP2.add(rxAxisAction0P2);
		profile2.getComponentToActionMap().put("z", rxAxisActionsP2);

		HashSet<IAction> ryAxisActionsP0 = new HashSet<>();
		AxisToAxisAction ryAxisAction0P0 = new AxisToAxisAction();
		ryAxisAction0P0.setvAxisId(ID_Y_AXIS);
		ryAxisActionsP0.add(ryAxisAction0P0);
		profile0.getComponentToActionMap().put("rz", ryAxisActionsP0);
		HashSet<IAction> ryAxisActionsP2 = new HashSet<>();
		CursorAction ryAxisAction0P2 = new CursorAction();
		ryAxisAction0P2.setAxis(CursorAction.Axis.Y);
		ryAxisActionsP2.add(ryAxisAction0P2);
		profile2.getComponentToActionMap().put("rz", ryAxisActionsP2);

		HashSet<IAction> xButtonActionsP0 = new HashSet<>();
		ButtonToButtonAction xButtonAction0P0 = new ButtonToButtonAction();
		xButtonAction0P0.setvButtonId(0);
		xButtonActionsP0.add(xButtonAction0P0);
		profile0.getComponentToActionMap().put("14", xButtonActionsP0);
		HashSet<IAction> xButtonActionsP2 = new HashSet<>();
		ButtonToKeyAction xButtonAction0P2 = new ButtonToKeyAction();
		KeyStroke xButtonAction0P2Keystroke = new KeyStroke();
		xButtonAction0P2Keystroke.setKeyCodes(new String[] { "LBUTTON" });
		xButtonAction0P2.setKeystroke(xButtonAction0P2Keystroke);
		xButtonActionsP2.add(xButtonAction0P2);
		profile2.getComponentToActionMap().put("14", xButtonActionsP2);

		HashSet<IAction> oButtonActions = new HashSet<>();
		ButtonToKeyAction oButtonAction0 = new ButtonToKeyAction();
		KeyStroke oButtonAction0Keystroke = new KeyStroke();
		oButtonAction0Keystroke.setModifierCodes(new String[] { "SHIFT" });
		oButtonAction0Keystroke.setKeyCodes(new String[] { "VK_A" });
		oButtonAction0.setKeystroke(oButtonAction0Keystroke);
		oButtonAction0.setDownUp(true);
		oButtonActions.add(oButtonAction0);
		profile0.getComponentToActionMap().put("13", oButtonActions);

		HashSet<IAction> r1ButtonActions = new HashSet<>();
		ButtonToProfileAction r1ButtonAction0 = new ButtonToProfileAction();
		r1ButtonAction0.setProfileId(1);
		r1ButtonActions.add(r1ButtonAction0);
		componentToProfileActionMap.put("11", r1ButtonAction0);

		HashSet<IAction> l1ButtonActions = new HashSet<>();
		ButtonToProfileAction l1ButtonAction0 = new ButtonToProfileAction();
		l1ButtonAction0.setProfileId(2);
		l1ButtonAction0.setToggle(true);
		l1ButtonActions.add(l1ButtonAction0);
		componentToProfileActionMap.put("10", l1ButtonAction0);

		profiles.add(profile0);
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

			if (profiles.size() > 0 && activeProfile < profiles.size()) {
				Map<String, HashSet<IAction>> componentToActionMap = profiles
						.get(activeProfile).getComponentToActionMap();
				Set<IAction> actions = componentToActionMap.get(c.getName());
				if (actions == null)
					actions = profiles.get(0).getComponentToActionMap()
							.get(c.getName());

				if (actions != null)
					for (IAction a : actions)
						a.doAction(this, c.getPollData());
			}
		}

		System.out.println("Profile " + String.valueOf(activeProfile));
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

		setAxis(id,
				(int) Util.normalize(value, -1.0f, 1.0f, 0.0f, maxAxisValue));
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

	public int getActiveProfile() {
		return activeProfile;
	}

	public void setActiveProfile(int activeProfile) {
		this.activeProfile = activeProfile;
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

}

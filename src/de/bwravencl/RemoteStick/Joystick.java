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
	private final List<Map<String, List<IAction>>> profiles = new ArrayList<Map<String, List<IAction>>>();

	private final Set<String> pressedKeys = new HashSet<String>();

	public Joystick(ServerThread serverThread, Controller controller) {
		this.serverThread = serverThread;
		this.controller = controller;

		System.out.println("Controller: " + controller.getName());

		controller.poll();
		for (Component c : controller.getComponents()) {
			System.out.println(c.getName() + " " + c.getPollData());
		}

		Map<String, List<IAction>> profile0 = new HashMap<String, List<IAction>>();

		List<IAction> xAxisActions = new ArrayList<>();
		AxisToAxisAction xAxisAction0 = new AxisToAxisAction();
		xAxisAction0.setvAxisId(ID_Z_AXIS);
		xAxisActions.add(xAxisAction0);
		AxisToKeyAction xAxisAction1 = new AxisToKeyAction();
		xAxisAction1.setKeyCode("VK_I");
		xAxisAction1.setMinAxisValueKeyDown(0.9f);
		xAxisAction1.setMaxAxisValueKeyDown(1.0f);
		xAxisActions.add(xAxisAction1);
		profile0.put("x", xAxisActions);

		List<IAction> yAxisActions = new ArrayList<>();
		AxisToRelativeAxisAction yAxisAction0 = new AxisToRelativeAxisAction();
		yAxisAction0.setvAxisId(ID_S0_AXIS);
		yAxisAction0.setInvert(true);
		yAxisAction0.setSensitivity(2.0f);
		yAxisActions.add(yAxisAction0);
		AxisToButtonAction yAxisAction1 = new AxisToButtonAction();
		yAxisAction1.setvButtonId(1);
		yAxisAction1.setMinAxisValueButtonDown(0.75f);
		yAxisAction1.setMinAxisValueButtonDown(1.0f);
		yAxisActions.add(yAxisAction1);
		profile0.put("y", yAxisActions);

		List<IAction> rxAxisActions = new ArrayList<>();
		AxisToAxisAction rxAxisAction0 = new AxisToAxisAction();
		rxAxisAction0.setvAxisId(ID_X_AXIS);
		rxAxisActions.add(rxAxisAction0);
		profile0.put("z", rxAxisActions);

		List<IAction> ryAxisActions = new ArrayList<>();
		AxisToAxisAction ryAxisAction0 = new AxisToAxisAction();
		ryAxisAction0.setvAxisId(ID_Y_AXIS);
		ryAxisActions.add(ryAxisAction0);
		profile0.put("rz", ryAxisActions);

		List<IAction> xButtonActions = new ArrayList<>();
		ButtonToButtonAction xButtonAction0 = new ButtonToButtonAction();
		xButtonAction0.setvButtonId(0);
		xButtonActions.add(xButtonAction0);
		profile0.put("14", xButtonActions);
		
		List<IAction> oButtonActions = new ArrayList<>();
		ButtonToKeyAction oButtonAction0 = new ButtonToKeyAction();
		oButtonAction0.setKeyCode("VK_A");
		oButtonActions.add(oButtonAction0);
		profile0.put("13", oButtonActions);
		
		profiles.add(profile0);
	}

	public void poll() {
		controller.poll();

		for (Component c : controller.getComponents()) {
			ButtonToProfileAction profileAction = componentToProfileActionMap
					.get(c.getName());
			if (profileAction != null)
				profileAction.doAction(this, c.getPollData());

			if (activeProfile < profiles.size()) {
				Map<String, List<IAction>> componentToActionMap = profiles
						.get(activeProfile);
				List<IAction> actions = componentToActionMap.get(c.getName());
				if (actions != null)
					for (IAction a : actions)
						a.doAction(this, c.getPollData());
			}
		}
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

	public Set<String> getPressedKeys() {
		return pressedKeys;
	}

}

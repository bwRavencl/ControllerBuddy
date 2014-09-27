package de.bwravencl.RemoteStick;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private ServerThread serverThread;
	private long maxAxisValue = 0;
	private int nButtons = 0;

	private int[] axis = new int[N_AXIS];
	private boolean[] buttons = new boolean[nButtons];

	private final Controller controller;
	private final Map<Component, List<IAction>> componentToActionsMap = new HashMap<Component, List<IAction>>();

	public Joystick(ServerThread serverThread, Controller controller) {
		this.controller = controller;

		System.out.println("Controller: " + controller.getName());
	}

	private void poll() {
		controller.poll();

		for (Component c : controller.getComponents()) {
			List<IAction> actions = componentToActionsMap.get(c);
			for (IAction a : actions)
				a.doAction(this, c.getPollData());
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
		//poll();
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

		setAxis(id, (int) Util.normalize(value, -1.0f, 1.0f, 0.0f,
				(float) maxAxisValue));
	}

	public boolean[] getButtons() {
		//poll();
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

}

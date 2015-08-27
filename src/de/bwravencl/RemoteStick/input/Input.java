/* Copyright (C) 2015  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.RemoteStick.input;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.bwravencl.RemoteStick.input.action.AxisToAxisAction;
import de.bwravencl.RemoteStick.input.action.AxisToButtonAction;
import de.bwravencl.RemoteStick.input.action.AxisToKeyAction;
import de.bwravencl.RemoteStick.input.action.AxisToRelativeAxisAction;
import de.bwravencl.RemoteStick.input.action.AxisToScrollAction;
import de.bwravencl.RemoteStick.input.action.ButtonToButtonAction;
import de.bwravencl.RemoteStick.input.action.ButtonToKeyAction;
import de.bwravencl.RemoteStick.input.action.ButtonToModeAction;
import de.bwravencl.RemoteStick.input.action.ButtonToScrollAction;
import de.bwravencl.RemoteStick.input.action.AxisToCursorAction;
import de.bwravencl.RemoteStick.input.action.IAction;
import de.bwravencl.RemoteStick.net.ServerThread;
import net.java.games.input.Component;
import net.java.games.input.Controller;

public class Input {

	public static final int MAX_N_BUTTONS = 128;

	public enum VirtualAxis {
		X, Y, Z, RX, RY, RZ, S0, S1
	}

	private static Profile profile;
	private Controller controller;
	private ServerThread serverThread;
	private long maxAxisValue = 0;
	private int nButtons = MAX_N_BUTTONS;
	private EnumMap<VirtualAxis, Integer> axis = new EnumMap<VirtualAxis, Integer>(
			VirtualAxis.class);
	private boolean[] buttons = new boolean[nButtons];
	private int cursorDeltaX = 0;
	private int cursorDeltaY = 0;
	private int scrollClicks = 0;
	private final Set<String> downKeysCodes = new HashSet<String>();
	private final Set<KeyStroke> downUpKeyStrokes = new HashSet<KeyStroke>();

	public Input(Controller controller) {
		this.controller = controller;

		for (VirtualAxis va : VirtualAxis.values())
			axis.put(va, 0);

		profile = new Profile();
		/*Mode defaultMode = profile.getModes().get(0);
		Mode mode1 = new Mode("54947df8-0e9e-4471-a2f9-9af509fb5889");
		mode1.setDescription("View mode");
		Mode mode2 = new Mode("046b6c7f-0b8a-43b9-b35d-6489e6daee91");
		mode2.setDescription("Mouse mode");

		List<IAction> xAxisActionsP0 = new ArrayList<>();
		AxisToAxisAction xAxisAction0 = new AxisToAxisAction();
		xAxisAction0.setVirtualAxis(VirtualAxis.Z);
		xAxisActionsP0.add(xAxisAction0);
		defaultMode.getComponentToActionMap().put("x", xAxisActionsP0);
		List<IAction> xAxisActionsP1 = new ArrayList<>();
		AxisToKeyAction xAxisAction1 = new AxisToKeyAction();
		KeyStroke xAxisAction1Keystroke = new KeyStroke();
		xAxisAction1Keystroke.setKeyCodes(new String[] { "VK_I" });
		xAxisAction1.setKeystroke(xAxisAction1Keystroke);
		xAxisAction1.setMinAxisValue(0.9f);
		xAxisAction1.setMaxAxisValue(1.0f);
		xAxisAction1.setDownUp(false);
		xAxisActionsP1.add(xAxisAction1);
		mode1.getComponentToActionMap().put("x", xAxisActionsP1);

		List<IAction> yAxisActionsP0 = new ArrayList<>();
		AxisToRelativeAxisAction yAxisAction0P0 = new AxisToRelativeAxisAction();
		yAxisAction0P0.setVirtualAxis(VirtualAxis.S0);
		yAxisAction0P0.setInvert(false);
		yAxisAction0P0.setSensitivity(2.0f);
		yAxisActionsP0.add(yAxisAction0P0);
		AxisToButtonAction yAxisAction1P0 = new AxisToButtonAction();
		yAxisAction1P0.setButtonId(1);
		yAxisAction1P0.setMinAxisValue(0.75f);
		yAxisAction1P0.setMinAxisValue(1.0f);
		yAxisActionsP0.add(yAxisAction1P0);
		defaultMode.getComponentToActionMap().put("y", yAxisActionsP0);
		List<IAction> yAxisActionsP2 = new ArrayList<>();
		AxisToScrollAction yAxisAction0P2 = new AxisToScrollAction();
		yAxisAction0P2.setClicks(10);
		yAxisActionsP2.add(yAxisAction0P2);
		mode2.getComponentToActionMap().put("y", yAxisActionsP2);

		List<IAction> rxAxisActionsP0 = new ArrayList<>();
		AxisToAxisAction rxAxisAction0P0 = new AxisToAxisAction();
		rxAxisAction0P0.setVirtualAxis(VirtualAxis.X);
		rxAxisActionsP0.add(rxAxisAction0P0);
		defaultMode.getComponentToActionMap().put("z", rxAxisActionsP0);
		List<IAction> rxAxisActionsP2 = new ArrayList<>();
		AxisToCursorAction rxAxisAction0P2 = new AxisToCursorAction();
		rxAxisAction0P2.setAxis(AxisToCursorAction.MouseAxis.X);
		rxAxisActionsP2.add(rxAxisAction0P2);
		mode2.getComponentToActionMap().put("z", rxAxisActionsP2);

		List<IAction> ryAxisActionsP0 = new ArrayList<>();
		AxisToAxisAction ryAxisAction0P0 = new AxisToAxisAction();
		ryAxisAction0P0.setVirtualAxis(VirtualAxis.Y);
		ryAxisActionsP0.add(ryAxisAction0P0);
		defaultMode.getComponentToActionMap().put("rz", ryAxisActionsP0);
		List<IAction> ryAxisActionsP2 = new ArrayList<>();
		AxisToCursorAction ryAxisAction0P2 = new AxisToCursorAction();
		ryAxisAction0P2.setAxis(AxisToCursorAction.MouseAxis.Y);
		ryAxisActionsP2.add(ryAxisAction0P2);
		mode2.getComponentToActionMap().put("rz", ryAxisActionsP2);

		List<IAction> xButtonActionsP0 = new ArrayList<>();
		ButtonToButtonAction xButtonAction0P0 = new ButtonToButtonAction();
		xButtonAction0P0.setButtonId(0);
		xButtonActionsP0.add(xButtonAction0P0);
		defaultMode.getComponentToActionMap().put("14", xButtonActionsP0);
		List<IAction> xButtonActionsP2 = new ArrayList<>();
		ButtonToKeyAction xButtonAction0P2 = new ButtonToKeyAction();
		KeyStroke xButtonAction0P2Keystroke = new KeyStroke();
		xButtonAction0P2Keystroke.setKeyCodes(new String[] { "LBUTTON" });
		xButtonAction0P2.setKeystroke(xButtonAction0P2Keystroke);
		xButtonActionsP2.add(xButtonAction0P2);
		mode2.getComponentToActionMap().put("14", xButtonActionsP2);

		List<IAction> oButtonActions = new ArrayList<>();
		ButtonToKeyAction oButtonAction0 = new ButtonToKeyAction();
		KeyStroke oButtonAction0Keystroke = new KeyStroke();
		oButtonAction0Keystroke.setModifierCodes(new String[] { "SHIFT" });
		oButtonAction0Keystroke.setKeyCodes(new String[] { "VK_A" });
		oButtonAction0.setKeystroke(oButtonAction0Keystroke);
		oButtonAction0.setDownUp(true);
		oButtonActions.add(oButtonAction0);
		defaultMode.getComponentToActionMap().put("13", oButtonActions);

		List<IAction> triangleButtonActionsP2 = new ArrayList<>();
		ButtonToScrollAction triangleButtonAction = new ButtonToScrollAction();
		triangleButtonAction.setClicks(1);
		triangleButtonAction.setInvert(true);
		triangleButtonActionsP2.add(triangleButtonAction);
		mode2.getComponentToActionMap().put("12", triangleButtonActionsP2);

		List<IAction> squareButtonActionsP2 = new ArrayList<>();
		ButtonToScrollAction squareButtonAction = new ButtonToScrollAction();
		squareButtonAction.setClicks(1);
		squareButtonActionsP2.add(squareButtonAction);
		mode2.getComponentToActionMap().put("15", squareButtonActionsP2);

		List<IAction> r1ButtonActions = new ArrayList<>();
		ButtonToModeAction r1ButtonAction0 = new ButtonToModeAction();
		r1ButtonAction0.setMode(mode1);
		r1ButtonActions.add(r1ButtonAction0);
		profile.getComponentToModeActionMap().put("11", r1ButtonAction0);

		List<IAction> l1ButtonActions = new ArrayList<>();
		ButtonToModeAction l1ButtonAction0 = new ButtonToModeAction();
		l1ButtonAction0.setMode(mode2);
		l1ButtonAction0.setToggle(true);
		l1ButtonActions.add(l1ButtonAction0);
		profile.getComponentToModeActionMap().put("10", l1ButtonAction0);

		profile.getModes().add(mode1);
		profile.getModes().add(mode2);*/
	}

	public void poll() {
		controller.poll();

		for (Component c : controller.getComponents()) {
			final ButtonToModeAction modeAction = profile
					.getComponentToModeActionMap().get(c.getName());
			if (modeAction != null)
				modeAction.doAction(this, c.getPollData());

			final List<Mode> modes = profile.getModes();
			final Map<String, List<IAction>> componentToActionMap = profile
					.getActiveMode().getComponentToActionMap();

			List<IAction> actions = componentToActionMap.get(c.getName());
			if (actions == null)
				actions = modes.get(0).getComponentToActionMap()
						.get(c.getName());

			if (actions != null)
				for (IAction a : actions)
					a.doAction(this, c.getPollData());
		}
	}

	public static Profile getProfile() {
		return profile;
	}

	public static boolean setProfile(Profile profile, Controller controller) {
		if (controller == null)
			return false;
		else {
			for (String s : profile.getComponentToModeActionMap().keySet()) {
				boolean componentFound = false;

				for (Component c : controller.getComponents())
					if (s.equals(c.getName())) {
						componentFound = true;
						break;
					}

				if (!componentFound)
					return false;
			}

			for (Mode m : profile.getModes())
				for (String s : m.getComponentToActionMap().keySet()) {
					boolean componentFound = false;

					for (Component c : controller.getComponents())
						if (s.equals(c.getName())) {
							componentFound = true;
							break;
						}

					if (!componentFound)
						return false;
				}

			Input.profile = profile;
			return true;
		}
	}

	public Controller getController() {
		return controller;
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

	public EnumMap<VirtualAxis, Integer> getAxis() {
		return axis;
	}

	public void setAxis(VirtualAxis virtualAxis, int value) {
		value = Math.max(value, 0);
		value = Math.min(value, (int) maxAxisValue);

		axis.put(virtualAxis, value);
	}

	public void setAxis(VirtualAxis virtualAxis, float value) {
		value = Math.max(value, -1.0f);
		value = Math.min(value, 1.0f);

		setAxis(virtualAxis,
				(int) normalize(value, -1.0f, 1.0f, 0.0f, maxAxisValue));
	}

	public boolean[] getButtons() {
		return buttons;
	}

	public void setButtons(int id, boolean value) {
		if (id < buttons.length)
			buttons[id] = value;
	}

	public void setButtons(int id, float value) {
		if (value < 0.5f)
			setButtons(id, false);
		else
			setButtons(id, true);
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

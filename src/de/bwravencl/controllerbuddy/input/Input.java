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

package de.bwravencl.controllerbuddy.input;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.output.OutputThread;
import net.java.games.input.Component;
import net.java.games.input.Controller;

public class Input {

	public static final int MAX_N_BUTTONS = 128;

	public enum VirtualAxis {
		X, Y, Z, RX, RY, RZ, S0, S1
	}

	private static Profile profile;

	private Controller controller;
	private OutputThread outputThread;
	private EnumMap<VirtualAxis, Integer> axis = new EnumMap<VirtualAxis, Integer>(VirtualAxis.class);
	private boolean[] buttons;
	private int cursorDeltaX = 5;
	private int cursorDeltaY = 5;
	private int scrollClicks = 1;
	private final Set<Integer> downMouseButtons = new HashSet<Integer>();
	private final Set<Integer> downUpMouseButtons = new HashSet<Integer>();
	private final Set<KeyStroke> downKeyStrokes = new HashSet<KeyStroke>();
	private final Set<KeyStroke> downUpKeyStrokes = new HashSet<KeyStroke>();

	public Input(Controller controller) {
		this.controller = controller;

		for (VirtualAxis va : VirtualAxis.values())
			axis.put(va, 0);

		profile = new Profile();
	}

	public void poll() {
		controller.poll();

		for (Component c : controller.getComponents()) {
			final ButtonToModeAction modeAction = profile.getComponentToModeActionMap().get(c.getName());
			if (modeAction != null)
				modeAction.doAction(this, c.getPollData());

			final List<Mode> modes = profile.getModes();
			final Map<String, List<IAction>> componentToActionMap = profile.getActiveMode().getComponentToActionsMap();

			List<IAction> actions = componentToActionMap.get(c.getName());
			if (actions == null)
				actions = modes.get(0).getComponentToActionsMap().get(c.getName());

			if (actions != null)
				for (IAction a : actions)
					a.doAction(this, c.getPollData());
		}
	}

	public void reset() {
		profile.setActiveMode(0);

		for (Mode m : profile.getModes()) {
			for (List<IAction> actions : m.getComponentToActionsMap().values()) {
				for (IAction a : actions) {
					if (a instanceof ButtonToCycleAction)
						((ButtonToCycleAction) a).reset();
				}
			}
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
				for (String s : m.getComponentToActionsMap().keySet()) {
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

	public OutputThread getOutputThread() {
		return outputThread;
	}

	public void setOutputThread(OutputThread outputThread) {
		this.outputThread = outputThread;
	}

	public void setnButtons(int nButtons) {
		buttons = new boolean[Math.min(outputThread.getnButtons(), MAX_N_BUTTONS)];
	}

	public EnumMap<VirtualAxis, Integer> getAxis() {
		return axis;
	}

	public void setAxis(VirtualAxis virtualAxis, int value) {
		value = Math.max(value, outputThread.getMinAxisValue());
		value = Math.min(value, outputThread.getMaxAxisValue());

		axis.put(virtualAxis, value);
	}

	public void setAxis(VirtualAxis virtualAxis, float value) {
		value = Math.max(value, -1.0f);
		value = Math.min(value, 1.0f);

		setAxis(virtualAxis,
				(int) normalize(value, -1.0f, 1.0f, outputThread.getMinAxisValue(), outputThread.getMaxAxisValue()));
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

	public Set<KeyStroke> getDownKeyStrokes() {
		return downKeyStrokes;
	}

	public Set<KeyStroke> getDownUpKeyStrokes() {
		return downUpKeyStrokes;
	}

	public Set<Integer> getDownMouseButtons() {
		return downMouseButtons;
	}

	public Set<Integer> getDownUpMouseButtons() {
		return downUpMouseButtons;
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

	public static float normalize(float value, float inMin, float inMax, float outMin, float outMax) {
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

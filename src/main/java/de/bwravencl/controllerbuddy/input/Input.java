/* Copyright (C) 2016  Matteo Hausner
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

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HMODULE;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.IButtonToAction;
import de.bwravencl.controllerbuddy.input.xinput.XInputState;
import de.bwravencl.controllerbuddy.output.OutputThread;
import net.java.games.input.AbstractComponent;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class Input {

	private static class GuideButtonComponent extends AbstractComponent {

		private final Function function;

		@SuppressWarnings("unused")
		private final Library xInput = (Library) Native.loadLibrary(XINPUT_LIBRARY_FILENAME, Library.class);

		private final int dwUserIndex;

		private final XInputState pState = new XInputState();

		private Field hasPolledField;

		public GuideButtonComponent(int dwUserIndex) throws UnsatisfiedLinkError, Exception {
			super("Guide Button", new Component.Identifier.Button("Guide Button"));

			if (dwUserIndex < 0 || dwUserIndex > 3)
				throw new Exception("dwUserIndex must be a value between 0 and 3");
			this.dwUserIndex = dwUserIndex;

			final HMODULE hModule = com.sun.jna.platform.win32.Kernel32.INSTANCE
					.GetModuleHandle(XINPUT_LIBRARY_FILENAME);
			if (hModule == null)
				throw new UnsatisfiedLinkError("Could not load " + XINPUT_LIBRARY_FILENAME);

			final Kernel32 kernel32 = (Kernel32) Native.loadLibrary(Kernel32.class);
			function = Function.getFunction(kernel32.GetProcAddress(hModule, 100));

			hasPolledField = AbstractComponent.class.getDeclaredField("has_polled");
			hasPolledField.setAccessible(true);
		}

		@Override
		public boolean isRelative() {
			return false;
		}

		@Override
		protected float poll() {
			function.invokeInt(new Object[] { dwUserIndex, pState });

			try {
				hasPolledField.setBoolean(this, false);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			return BigInteger.valueOf(pState.gamepad.wButtons).testBit(10) ? 1.0f : 0.0f;
		}

	}

	private interface Kernel32 extends Library {

		public Pointer GetProcAddress(HMODULE hModule, long lpProcName);

	}

	public enum VirtualAxis {
		X, Y, Z, RX, RY, RZ, S0, S1
	}

	private static final String XBOX_360_CONTROLLER_NAME = "XBOX 360 For Windows (Controller)";

	public static final String XINPUT_LIBRARY_FILENAME = "xinput1_3.dll";

	private static Controller cachedController;

	private static Component[] cachedComponents;

	public static final int MAX_N_BUTTONS = 128;

	private static Profile profile;

	private static EnumMap<VirtualAxis, Integer> axis = new EnumMap<VirtualAxis, Integer>(VirtualAxis.class);

	public static EnumMap<VirtualAxis, Integer> getAxis() {
		return axis;
	}

	public static Component[] getComponents(Controller controller) {
		if (controller != cachedController) {
			cachedController = controller;
			cachedComponents = controller.getComponents();

			if (Main.isWindows() && XBOX_360_CONTROLLER_NAME.equals(controller.getName())) {

				final List<Controller> xbox360Controllers = new ArrayList<Controller>();
				for (Controller c : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
					if (XBOX_360_CONTROLLER_NAME.equals(c.getName()))
						xbox360Controllers.add(c);
				}
				final int dwUserIndex = xbox360Controllers.indexOf(controller);

				if (dwUserIndex <= 3) {
					try {
						final GuideButtonComponent guideButtonComponent = new GuideButtonComponent(dwUserIndex);
						cachedComponents = Arrays.copyOf(cachedComponents, cachedComponents.length + 1);
						cachedComponents[cachedComponents.length - 1] = guideButtonComponent;
					} catch (UnsatisfiedLinkError e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		return cachedComponents;
	}

	public static Profile getProfile() {
		return profile;
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

	public static boolean setProfile(Profile profile, Controller controller) {
		if (controller == null)
			return false;
		else {
			for (String s : profile.getComponentToModeActionMap().keySet()) {
				boolean componentFound = false;

				for (Component c : getComponents(controller)) {
					if (s.equals(c.getName())) {
						componentFound = true;
						break;
					}
				}

				if (!componentFound)
					return false;
			}

			for (Mode m : profile.getModes()) {
				for (String s : m.getComponentToActionsMap().keySet()) {
					boolean componentFound = false;

					for (Component c : getComponents(controller)) {
						if (s.equals(c.getName())) {
							componentFound = true;
							break;
						}
					}

					if (!componentFound)
						return false;
				}

				class ActionComparator implements Comparator<IAction> {
					@Override
					public int compare(IAction o1, IAction o2) {
						if (o1 instanceof IButtonToAction && o2 instanceof IButtonToAction) {
							final IButtonToAction buttonToAction1 = (IButtonToAction) o1;
							final IButtonToAction buttonToAction2 = (IButtonToAction) o2;

							final boolean o1IsLongPress = buttonToAction1.isLongPress();
							final boolean o2IsLongPress = buttonToAction2.isLongPress();

							if (o1IsLongPress && !o2IsLongPress)
								return -1;
							else if (!o1IsLongPress && o2IsLongPress)
								return 1;
							else
								return 0;
						} else
							return 0;
					}
				}

				for (List<IAction> actions : m.getComponentToActionsMap().values())
					Collections.sort(actions, new ActionComparator());
			}

			Input.profile = profile;
			return true;
		}
	}

	private Controller controller;
	private OutputThread outputThread;
	private boolean[] buttons;
	private int cursorDeltaX = 5;
	private int cursorDeltaY = 5;
	private int scrollClicks = 1;
	private final Set<Integer> downMouseButtons = new HashSet<Integer>();
	private final Set<Integer> downUpMouseButtons = new HashSet<Integer>();
	private final Set<KeyStroke> downKeyStrokes = new HashSet<KeyStroke>();
	private final Set<KeyStroke> downUpKeyStrokes = new HashSet<KeyStroke>();
	private final Set<Integer> onLockKeys = new HashSet<Integer>();
	private final Set<Integer> offLockKeys = new HashSet<Integer>();

	public Input(Controller controller) {
		this.controller = controller;

		for (VirtualAxis va : VirtualAxis.values())
			axis.put(va, 0);

		profile = new Profile();
	}

	public boolean[] getButtons() {
		return buttons;
	}

	public Controller getController() {
		return controller;
	}

	public int getCursorDeltaX() {
		return cursorDeltaX;
	}

	public int getCursorDeltaY() {
		return cursorDeltaY;
	}

	public Set<KeyStroke> getDownKeyStrokes() {
		return downKeyStrokes;
	}

	public Set<Integer> getDownMouseButtons() {
		return downMouseButtons;
	}

	public Set<KeyStroke> getDownUpKeyStrokes() {
		return downUpKeyStrokes;
	}

	public Set<Integer> getDownUpMouseButtons() {
		return downUpMouseButtons;
	}

	public Set<Integer> getOffLockKeys() {
		return offLockKeys;
	}

	public Set<Integer> getOnLockKeys() {
		return onLockKeys;
	}

	public OutputThread getOutputThread() {
		return outputThread;
	}

	public int getScrollClicks() {
		return scrollClicks;
	}

	public boolean poll() {
		if (!controller.poll())
			return false;

		for (Component c : getComponents(controller)) {
			final List<ButtonToModeAction> buttonToModeActions = profile.getComponentToModeActionMap().get(c.getName());
			if (buttonToModeActions != null) {
				for (ButtonToModeAction a : buttonToModeActions)
					a.doAction(this, c.getPollData());
			}

			final List<Mode> modes = profile.getModes();
			final Map<String, List<IAction>> componentToActionMap = profile.getActiveMode().getComponentToActionsMap();

			List<IAction> actions = componentToActionMap.get(c.getName());
			if (actions == null)
				actions = modes.get(0).getComponentToActionsMap().get(c.getName());

			if (actions != null) {
				for (IAction a : actions)
					a.doAction(this, c.getPollData());
			}
		}

		Main.updateOverlayAxisIndicators();

		return true;
	}

	public void reset() {
		profile.setActiveMode(0);
		ButtonToModeAction.getButtonToModeActionStack().clear();

		for (Mode m : profile.getModes()) {
			for (List<IAction> actions : m.getComponentToActionsMap().values()) {
				for (IAction a : actions) {
					if (a instanceof ButtonToCycleAction)
						((ButtonToCycleAction) a).reset();
				}
			}
		}
	}

	public void setAxis(VirtualAxis virtualAxis, float value) {
		value = Math.max(value, -1.0f);
		value = Math.min(value, 1.0f);

		setAxis(virtualAxis,
				(int) normalize(value, -1.0f, 1.0f, outputThread.getMinAxisValue(), outputThread.getMaxAxisValue()));
	}

	public void setAxis(VirtualAxis virtualAxis, int value) {
		value = Math.max(value, outputThread.getMinAxisValue());
		value = Math.min(value, outputThread.getMaxAxisValue());

		axis.put(virtualAxis, value);
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

	public void setCursorDeltaX(int cursorDeltaX) {
		this.cursorDeltaX = cursorDeltaX;
	}

	public void setCursorDeltaY(int cursorDeltaY) {
		this.cursorDeltaY = cursorDeltaY;
	}

	public void setnButtons(int nButtons) {
		buttons = new boolean[Math.min(outputThread.getnButtons(), MAX_N_BUTTONS)];
	}

	public void setOutputThread(OutputThread outputThread) {
		this.outputThread = outputThread;
	}

	public void setScrollClicks(int scrollClicks) {
		this.scrollClicks = scrollClicks;
	}

}

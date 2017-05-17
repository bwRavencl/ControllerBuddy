/* Copyright (C) 2017  Matteo Hausner
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HMODULE;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.IButtonToAction;
import de.bwravencl.controllerbuddy.input.action.IInitializationAction;
import de.bwravencl.controllerbuddy.input.action.IResetableAction;
import de.bwravencl.controllerbuddy.input.action.ISuspendableAction;
import de.bwravencl.controllerbuddy.input.xinput.XInputState;
import de.bwravencl.controllerbuddy.output.OutputThread;
import net.java.games.input.AbstractComponent;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;

public class Input {

	private static class GuideButtonComponent extends AbstractComponent {

		private final Function function;
		@SuppressWarnings("unused")
		private final Library xInput = Native.loadLibrary(XINPUT_LIBRARY_FILENAME, Library.class);
		private final int dwUserIndex;
		private final XInputState pState = new XInputState();
		private final Field hasPolledField;

		public GuideButtonComponent(final int dwUserIndex) throws UnsatisfiedLinkError, Exception {
			super("Guide Button", new Component.Identifier.Button("Guide Button"));

			if (dwUserIndex < 0 || dwUserIndex > 3)
				throw new Exception(getClass().getName() + ": dwUserIndex must be a value between 0 and 3");
			this.dwUserIndex = dwUserIndex;

			final HMODULE hModule = com.sun.jna.platform.win32.Kernel32.INSTANCE
					.GetModuleHandle(XINPUT_LIBRARY_FILENAME);
			if (hModule == null)
				throw new UnsatisfiedLinkError(getClass().getName() + ": Could not load " + XINPUT_LIBRARY_FILENAME);

			final Kernel32 kernel32 = Native.loadLibrary(Kernel32.class);
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
			} catch (final IllegalArgumentException e) {
				e.printStackTrace();
			} catch (final IllegalAccessException e) {
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

	private static final int LOW_BATTERY_WARNING = 10;
	private static final float ABORT_SUSPENSION_ACTION_DEADZONE = 0.25f;
	private static final String XBOX_360_CONTROLLER_NAME = "XBOX 360 For Windows (Controller)";
	private static final String DUAL_SHOCK_4_CONTROLLER_NAMES[] = { "Wireless Controller",
			"DUALSHOCK\u00AE4 USB Wireless Adapto" };
	public static final String XINPUT_LIBRARY_FILENAME = "xinput1_3.dll";

	private static Controller cachedController;
	private static Component[] cachedComponents;
	public static final int MAX_N_BUTTONS = 128;
	private static Profile profile;
	private static EnumMap<VirtualAxis, Integer> axis = new EnumMap<>(VirtualAxis.class);

	public static EnumMap<VirtualAxis, Integer> getAxis() {
		return axis;
	}

	public static Component[] getComponents(final Controller controller) {
		if (controller != cachedController) {
			cachedController = controller;
			cachedComponents = controller.getComponents();

			if (Main.isWindows())
				if (XBOX_360_CONTROLLER_NAME.equals(controller.getName())) {

					final List<Controller> xbox360Controllers = new ArrayList<>();
					for (final Controller c : ControllerEnvironment.getDefaultEnvironment().getControllers())
						if (XBOX_360_CONTROLLER_NAME.equals(c.getName()))
							xbox360Controllers.add(c);
					final int dwUserIndex = xbox360Controllers.indexOf(controller);

					if (dwUserIndex <= 3)
						try {
							final GuideButtonComponent guideButtonComponent = new GuideButtonComponent(dwUserIndex);
							cachedComponents = Arrays.copyOf(cachedComponents, cachedComponents.length + 1);
							cachedComponents[cachedComponents.length - 1] = guideButtonComponent;
						} catch (final UnsatisfiedLinkError e) {
							e.printStackTrace();
						} catch (final Exception e) {
							e.printStackTrace();
						}
				} else if (isDualShock4Controller(controller)) {
					final int touchpadButtonIndex = 18;
					final Component[] newCachedComponents = new Component[cachedComponents.length - 1];
					System.arraycopy(cachedComponents, 0, newCachedComponents, 0, touchpadButtonIndex);
					System.arraycopy(cachedComponents, touchpadButtonIndex + 1, newCachedComponents,
							touchpadButtonIndex, cachedComponents.length - touchpadButtonIndex - 1);
					cachedComponents = newCachedComponents;
				}
		}

		return cachedComponents;
	}

	public static List<Controller> getControllers() {
		final List<Controller> controllers = new ArrayList<>();

		for (final Controller c : ControllerEnvironment.getDefaultEnvironment().getControllers())
			if (c.getType() != Type.KEYBOARD && c.getType() != Type.MOUSE && c.getType() != Type.TRACKBALL
					&& c.getType() != Type.TRACKPAD && c.getType() != Type.UNKNOWN && !c.getName().startsWith("vJoy"))
				controllers.add(c);

		return controllers;
	}

	public static Profile getProfile() {
		return profile;
	}

	public static boolean isDualShock4Controller(final Controller controller) {
		if (controller != null)
			for (final String s : DUAL_SHOCK_4_CONTROLLER_NAMES)
				if (s.equals(controller.getName()))
					return true;

		return false;
	}

	public static float normalize(final float value, final float inMin, final float inMax, final float outMin,
			final float outMax) {
		final float newValue;
		final float oldRange = inMax - inMin;

		if (oldRange == 0.0f)
			newValue = outMin;
		else {
			final float newRange = outMax - outMin;
			newValue = (value - inMin) * newRange / oldRange + outMin;
		}

		return newValue;
	}

	public static boolean setProfile(final Profile profile, final Controller controller) {
		if (controller == null)
			return false;
		else {
			for (final String s : profile.getComponentToModeActionMap().keySet()) {
				boolean componentFound = false;

				for (final Component c : getComponents(controller))
					if (s.equals(c.getName())) {
						componentFound = true;
						break;
					}

				if (!componentFound)
					return false;
			}

			for (final Mode m : profile.getModes()) {
				for (final String s : m.getComponentToActionsMap().keySet()) {
					boolean componentFound = false;

					for (final Component c : getComponents(controller))
						if (s.equals(c.getName())) {
							componentFound = true;
							break;
						}

					if (!componentFound)
						return false;
				}

				class ActionComparator implements Comparator<IAction> {
					@Override
					public int compare(final IAction o1, final IAction o2) {
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

				for (final List<IAction> actions : m.getComponentToActionsMap().values())
					Collections.sort(actions, new ActionComparator());
			}

			Input.profile = profile;
			return true;
		}
	}

	private final Main main;
	private final Controller controller;
	private OutputThread outputThread;
	private boolean[] buttons;
	private volatile int cursorDeltaX = 5;
	private volatile int cursorDeltaY = 5;
	private volatile int scrollClicks = 1;
	private final Set<Integer> downMouseButtons = ConcurrentHashMap.newKeySet();
	private final Set<Integer> downUpMouseButtons = new HashSet<>();
	private final Set<KeyStroke> downKeyStrokes = new HashSet<>();
	private final Set<KeyStroke> downUpKeyStrokes = new HashSet<>();
	private final Set<Integer> onLockKeys = new HashSet<>();
	private final Set<Integer> offLockKeys = new HashSet<>();
	private HidDevice hidDevice;
	private boolean charging = true;
	private int batteryState;

	public Input(final Main main, final Controller controller) {
		this.main = main;
		this.controller = controller;

		for (final VirtualAxis va : VirtualAxis.values())
			axis.put(va, 0);

		profile = new Profile();

		if (Main.isWindows() && isDualShock4Controller(controller)) {
			final List<HidDeviceInfo> devices = PureJavaHidApi.enumerateDevices();
			HidDeviceInfo hidDeviceInfo = null;
			for (final HidDeviceInfo hi : devices) {
				final short productId = hi.getProductId();
				if (hi.getVendorId() == (short) 0x54C
						&& (productId == (short) 0x5C4 || productId == (short) 0x9CC || productId == (short) 0xBA0)) {
					hidDeviceInfo = hi;
					break;
				}
			}
			if (hidDeviceInfo != null)
				try {
					hidDevice = PureJavaHidApi.openDevice(hidDeviceInfo);
					hidDevice.setInputReportListener(new InputReportListener() {

						private static final int TOUCHPAD_MAX_DELTA = 150;
						private static final float TOUCHPAD_CURSOR_SENSITIVITY = 1.5f;
						private static final float TOUCHPAD_SCROLL_SENSITIVITY = 0.25f;

						private boolean prevTouchpadButtonDown;
						private boolean prevDown1;
						private boolean prevDown2;
						private int prevX1;
						private int prevY1;

						@Override
						public void onInputReport(final HidDevice source, final byte Id, final byte[] data,
								final int len) {
							final boolean touchpadButtonDown = (data[6] & 1 << 2 - 1) != 0;
							final boolean down1 = data[34] >> 7 != 0 ? false : true;
							final boolean down2 = data[38] >> 7 != 0 ? false : true;
							final int x1 = data[35] + (data[36] & 0xF) * 255;
							final int y1 = ((data[36] & 0xF0) >> 4) + data[37] * 16;
							final int dX1 = x1 - prevX1;
							final int dY1 = y1 - prevY1;

							if (touchpadButtonDown)
								downMouseButtons.add(down2 ? 2 : 1);
							else if (prevTouchpadButtonDown)
								downMouseButtons.clear();

							if (down1 && !prevDown1) {
								prevX1 = -1;
								prevY1 = -1;
							}

							if (!prevDown2 || touchpadButtonDown) {
								if (prevX1 > 0)
									if (Math.abs(dX1) < TOUCHPAD_MAX_DELTA)
										cursorDeltaX = (int) (dX1 * TOUCHPAD_CURSOR_SENSITIVITY);

								if (prevY1 > 0)
									if (Math.abs(dY1) < TOUCHPAD_MAX_DELTA)
										cursorDeltaY = (int) (dY1 * TOUCHPAD_CURSOR_SENSITIVITY);
							} else if (prevY1 > 0)
								if (Math.abs(dY1) < TOUCHPAD_MAX_DELTA)
									scrollClicks = (int) (-dY1 * TOUCHPAD_SCROLL_SENSITIVITY);

							prevTouchpadButtonDown = touchpadButtonDown;
							prevDown1 = down1;
							prevDown2 = down2;
							prevX1 = x1;
							prevY1 = y1;

							final boolean charging = (data[29] & 0x10) > 6;
							final int battery = Math.min((data[29] & 0x0F) * 100 / (charging ? 11 : 8), 100);

							setBatteryState(battery);
							setCharging(charging);
						}

					});
				} catch (final IOException e) {
					e.printStackTrace();
				}
		}

	}

	public void deInit() {
		if (hidDevice != null)
			hidDevice.close();
	}

	public int getBatteryState() {
		return batteryState;
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

	public void init() {
		for (final Mode m : profile.getModes())
			for (final List<IAction> actions : m.getComponentToActionsMap().values())
				for (final IAction a : actions)
					if (a instanceof IInitializationAction)
						((IInitializationAction) a).init(this);
	}

	public boolean isCharging() {
		return charging;
	}

	public boolean poll() {
		if (!controller.poll())
			return false;

		final List<Mode> modes = profile.getModes();
		final Map<String, List<IAction>> componentToActionMap = profile.getActiveMode().getComponentToActionsMap();

		for (final Component c : getComponents(controller)) {
			final float pollData = c.getPollData();

			if (Math.abs(pollData) <= ABORT_SUSPENSION_ACTION_DEADZONE) {
				final Iterator<Entry<ISuspendableAction, String>> it = ISuspendableAction.componentToSuspendedActionsMap
						.entrySet().iterator();
				while (it.hasNext())
					if (c.getName().equals(it.next().getValue()))
						it.remove();
			}

			List<IAction> actions = componentToActionMap.get(c.getName());
			if (actions == null) {
				final LinkedList<ButtonToModeAction> buttonToModeActionStack = ButtonToModeAction
						.getButtonToModeActionStack();
				for (int i = 1; i < buttonToModeActionStack.size(); i++) {
					actions = buttonToModeActionStack.get(i).getMode().getComponentToActionsMap().get(c.getName());

					if (actions != null)
						break;
				}
			}

			if (actions == null)
				actions = modes.get(0).getComponentToActionsMap().get(c.getName());

			if (actions != null)
				for (final IAction a : actions)
					a.doAction(this, pollData);
		}

		for (final Component c : getComponents(controller)) {
			final List<ButtonToModeAction> buttonToModeActions = profile.getComponentToModeActionMap().get(c.getName());
			if (buttonToModeActions != null)
				for (final ButtonToModeAction a : buttonToModeActions)
					a.doAction(this, c.getPollData());
		}

		Main.updateOverlayAxisIndicators();

		return true;
	}

	public void reset() {
		profile.setActiveMode(this, 0);
		ButtonToModeAction.getButtonToModeActionStack().clear();

		for (final Mode m : profile.getModes())
			for (final List<IAction> actions : m.getComponentToActionsMap().values())
				for (final IAction a : actions)
					if (a instanceof IResetableAction)
						((IResetableAction) a).reset();
	}

	public void setAxis(final VirtualAxis virtualAxis, float value) {
		value = Math.max(value, -1.0f);
		value = Math.min(value, 1.0f);

		setAxis(virtualAxis,
				(int) normalize(value, -1.0f, 1.0f, outputThread.getMinAxisValue(), outputThread.getMaxAxisValue()));
	}

	public void setAxis(final VirtualAxis virtualAxis, int value) {
		value = Math.max(value, outputThread.getMinAxisValue());
		value = Math.min(value, outputThread.getMaxAxisValue());

		axis.put(virtualAxis, value);
	}

	public void setBatteryState(final int batteryState) {
		if (this.batteryState != batteryState) {
			this.batteryState = batteryState;
			if (main != null) {
				main.updateTitleAndTooltip();

				if (batteryState == LOW_BATTERY_WARNING)
					main.displayLowBatteryWarning(batteryState);
			}
		}
	}

	public void setButtons(final int id, final boolean value) {
		if (id < buttons.length)
			buttons[id] = value;
	}

	public void setButtons(final int id, final float value) {
		if (value < 0.5f)
			setButtons(id, false);
		else
			setButtons(id, true);
	}

	public void setCharging(final boolean charging) {
		if (this.charging != charging) {
			this.charging = charging;
			main.updateTitleAndTooltip();
			main.displayChargingStateInfo(charging);
		}
	}

	public void setCursorDeltaX(final int cursorDeltaX) {
		this.cursorDeltaX = cursorDeltaX;
	}

	public void setCursorDeltaY(final int cursorDeltaY) {
		this.cursorDeltaY = cursorDeltaY;
	}

	public void setnButtons(final int nButtons) {
		buttons = new boolean[Math.min(outputThread.getnButtons(), MAX_N_BUTTONS)];
	}

	public void setOutputThread(final OutputThread outputThread) {
		this.outputThread = outputThread;
	}

	public void setScrollClicks(final int scrollClicks) {
		this.scrollClicks = scrollClicks;
	}

}

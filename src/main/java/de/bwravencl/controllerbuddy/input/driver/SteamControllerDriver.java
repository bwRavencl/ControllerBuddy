/* Copyright (C) 2022  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.input.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamController;
import com.codedisaster.steamworks.SteamControllerHandle;
import com.codedisaster.steamworks.SteamException;
import com.sun.jna.Platform;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;

public class SteamControllerDriver extends Driver {

	public static class SteamControllerDriverBuilder implements IDriverBuilder {

		@Override
		public Driver getIfAvailable(final Input input, final List<ControllerInfo> presentControllers,
				final ControllerInfo selectedController) {
			if (!Platform.isIntel())
				return null;

			String name;
			if (Main.isMac)
				name = GLFW.glfwGetGamepadName(selectedController.jid());
			else
				name = selectedController.name();

			if (!"Steam Virtual Gamepad".equals(name))
				return null;

			SteamController steamController = null;
			try {
				SteamAPI.loadLibraries();
				if (SteamAPI.init()) {
					steamController = new SteamController();
					if (steamController.init()) {
						final var controllerHandles = new SteamControllerHandle[SteamController.STEAM_CONTROLLER_MAX_COUNT];

						final var steamControllerHandles = new ArrayList<SteamControllerHandle>();
						for (var i = 0; i < steamController.getConnectedControllers(controllerHandles); i++)
							switch (steamController.getInputTypeForHandle(controllerHandles[i])) {
							case SteamController:
								steamControllerHandles.add(controllerHandles[i]);
								break;
							default:
								break;
							}

						final var numSteamControllers = steamControllerHandles.size();
						if (numSteamControllers > 1) {
							log.log(Level.WARNING,
									"Found more than one Steam Controller - extended Steam Controller support will be unavailable");
							return null;
						}
						if (numSteamControllers == 1)
							return new SteamControllerDriver(input, selectedController, steamController,
									steamControllerHandles.get(0));
					} else
						log.log(Level.WARNING, "Steam Controller initialization failed");
				} else
					log.log(Level.WARNING, "Steam API initialization failed");
			} catch (final SteamException e) {
				log.log(Level.WARNING, e.getMessage(), e);

				deInit(steamController);
			}

			return null;
		}
	}

	private static final short MAX_MOTOR_SPEED = Short.MAX_VALUE;

	private static final Logger log = Logger.getLogger(SteamControllerDriver.class.getName());

	private static final void deInit(final SteamController steamController) {
		if (steamController != null)
			steamController.shutdown();

		SteamAPI.shutdown();
	}

	private SteamController steamController;
	private SteamControllerHandle steamControllerHandle;

	private SteamControllerDriver(final Input input, final ControllerInfo controller,
			final SteamController steamController, final SteamControllerHandle steamControllerHandle) {
		super(input, controller);

		this.steamController = steamController;
		this.steamControllerHandle = steamControllerHandle;

		log.log(Level.INFO,
				Main.assembleControllerLoggingMessage(
						"Using Steam Controller with ID "
								+ steamController.getGamepadIndexForController(steamControllerHandle) + " as",
						controller));
	}

	@Override
	public void deInit(final boolean disconnected) {
		super.deInit(disconnected);

		deInit(steamController);
		steamController = null;
		steamControllerHandle = null;
	}

	private void rumble(final long duration, final short leftMotor, final short rightMotor) {
		if (steamControllerHandle == null)
			return;

		new Thread(() -> {
			if (steamControllerHandle != null)
				synchronized (steamControllerHandle) {
					steamController.triggerVibration(steamControllerHandle, leftMotor, leftMotor);
					try {
						Thread.sleep(duration);
					} catch (final InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					steamController.triggerVibration(steamControllerHandle, (short) 0, (short) 0);
				}
		}).start();
	}

	@Override
	public void rumbleLight() {
		rumble(60L, MAX_MOTOR_SPEED, (short) 0);
	}

	@Override
	public void rumbleStrong() {
		rumble(90L, (short) 0, MAX_MOTOR_SPEED);
	}
}

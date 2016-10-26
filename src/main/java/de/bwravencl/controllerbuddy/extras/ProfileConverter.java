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

package de.bwravencl.controllerbuddy.extras;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.AxisToButtonAction;
import de.bwravencl.controllerbuddy.input.action.AxisToKeyAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToButtonAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToKeyAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.ToKeyAction;
import de.bwravencl.controllerbuddy.json.InterfaceAdapter;

public class ProfileConverter {

	public static void main(final String[] args) {
		final Scanner reader = new Scanner(System.in);
		final File sourceFolder, destinationFolder;
		final Input input = new Input(null);

		System.out.print("Xbox 360 Controller profiles folder: ");
		try {
			sourceFolder = new File(reader.next());
			if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
				System.out.println("Error: Folder does not exist");
				return;
			}

			System.out.print("DualShock 4 Controller profiles folder: ");
			destinationFolder = new File(reader.next());
		} finally {
			reader.close();
		}

		for (final File inputFile : sourceFolder.listFiles()) {
			final String fileName = inputFile.getName();

			if (fileName.endsWith(".json")) {
				System.out.print("Converting: " + fileName);

				try {
					String jsonString = new String(Files.readAllBytes(inputFile.toPath()), StandardCharsets.UTF_8);
					Gson gson = new GsonBuilder().registerTypeAdapter(IAction.class, new InterfaceAdapter<>()).create();
					final Profile profile = gson.fromJson(jsonString, Profile.class);

					final Map<String, List<ButtonToModeAction>> newComponentToModeActionMap = new HashMap<>();

					for (final Entry<String, List<ButtonToModeAction>> e : profile.getComponentToModeActionMap()
							.entrySet()) {
						final String oldComponent = e.getKey();
						final String newComponent = mapComponent(oldComponent);

						newComponentToModeActionMap.put(newComponent, e.getValue());
					}

					profile.setComponentToModeActionMap(newComponentToModeActionMap);

					final Iterator<Mode> it = profile.getModes().iterator();
					while (it.hasNext()) {
						final Mode mode = it.next();

						final Map<String, List<IAction>> newComponentToModeActionsMap = new HashMap<>();
						for (final Entry<String, List<IAction>> e : mode.getComponentToActionsMap().entrySet()) {
							final String oldComponent = e.getKey();
							if (oldComponent.equals("Z-Achse")) {
								final List<IAction> l2Actions = new ArrayList<>();
								final List<IAction> r2Actions = new ArrayList<>();

								for (final IAction action : e.getValue()) {
									if (action instanceof AxisToButtonAction) {
										final AxisToButtonAction axisToButtonAction = (AxisToButtonAction) action;

										final ButtonToButtonAction buttonToButtonAction = new ButtonToButtonAction();
										buttonToButtonAction.setActivationValue(1.0f);
										buttonToButtonAction.setButtonId(axisToButtonAction.getButtonId());
										buttonToButtonAction.setLongPress(false);

										if (axisToButtonAction.getMaxAxisValue() < 0.0f)
											r2Actions.add(buttonToButtonAction);
										else if (axisToButtonAction.getMinAxisValue() > 0.0f)
											l2Actions.add(buttonToButtonAction);
									} else if (action instanceof ToKeyAction) {
										final AxisToKeyAction axisToKeyAction = (AxisToKeyAction) action;

										final ButtonToKeyAction buttonToKeyAction = new ButtonToKeyAction();
										buttonToKeyAction.setActivationValue(1.0f);
										buttonToKeyAction.setDownUp(axisToKeyAction.isDownUp());
										buttonToKeyAction.setKeystroke(axisToKeyAction.getKeystroke());
										buttonToKeyAction.setLongPress(false);

										if (axisToKeyAction.getMaxAxisValue() < 0.0f)
											r2Actions.add(buttonToKeyAction);
										else if (axisToKeyAction.getMinAxisValue() > 0.0f)
											l2Actions.add(buttonToKeyAction);
									} else
										System.out.println(
												"\nWarning " + oldComponent + " is mapped to a non-convertable action");
								}

								newComponentToModeActionsMap.put("Taste 6", l2Actions);
								newComponentToModeActionsMap.put("Taste 7", r2Actions);
							} else {
								final String newComponent = mapComponent(oldComponent);

								newComponentToModeActionsMap.put(newComponent, e.getValue());
							}
						}

						mode.setComponentToActionMap(newComponentToModeActionsMap);
					}

					input.reset();

					gson = new GsonBuilder().registerTypeAdapter(IAction.class, new InterfaceAdapter<>())
							.setPrettyPrinting().create();
					jsonString = gson.toJson(profile);

					final FileOutputStream fos = new FileOutputStream(
							destinationFolder.getPath() + File.separator + fileName);
					final Writer writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
					writer.write(jsonString);
					writer.flush();
					fos.flush();
					fos.close();

					System.out.println(" -> Done!");

				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static String mapComponent(final String oldComponent) {
		if (oldComponent.equals("Y-Rotation"))
			return "Z-Rotation";
		else if (oldComponent.equals("X-Rotation"))
			return "Z-Achse";
		else if (oldComponent.equals("Guide Button"))
			return "Taste 12";
		else if (!oldComponent.contains("Taste "))
			return oldComponent;

		final int splitIndex = oldComponent.lastIndexOf(' ') + 1;
		final String prefix = oldComponent.substring(0, splitIndex);
		final String suffix = oldComponent.substring(splitIndex);
		final int oldButtonNo = Integer.parseInt(suffix);

		int newButtonNo;
		switch (oldButtonNo) {
		case 0:
			newButtonNo = 1;
			break;
		case 1:
			newButtonNo = 2;
			break;
		case 2:
			newButtonNo = 0;
			break;
		case 6:
			newButtonNo = 8;
			break;
		case 7:
			newButtonNo = 9;
			break;
		case 8:
			newButtonNo = 10;
			break;
		case 9:
			newButtonNo = 11;
			break;
		default:
			return oldComponent;
		}

		return prefix + newButtonNo;
	}

}

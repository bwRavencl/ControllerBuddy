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

package de.bwravencl.controllerbuddy.gui;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.sun.jna.platform.win32.WinDef.UINT;
import com.trolltech.qt.core.QCoreApplication;

import de.bwravencl.controllerbuddy.Version;
import de.bwravencl.controllerbuddy.gui.mumbleoverlay.MumbleOverlay;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.output.ClientVJoyOutputThread;
import de.bwravencl.controllerbuddy.output.LocalVJoyOutputThread;
import de.bwravencl.controllerbuddy.output.OutputThread;
import de.bwravencl.controllerbuddy.output.ServerOutputThread;
import de.bwravencl.controllerbuddy.output.VJoyOutputThread;
import net.brockmatt.util.ResourceBundleUtil;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;

public final class Main {

	private class AddModeAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -4881923833724315489L;

		public AddModeAction() {
			putValue(NAME, rb.getString("ADD_MODE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("ADD_MODE_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final Mode mode = new Mode();
			Input.getProfile().getModes().add(mode);

			setUnsavedChangesTitle();
			updateModesPanel();
		}

	}

	private class ChangeMumbleDirectoryAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 8249400342495050289L;

		public ChangeMumbleDirectoryAction() {
			putValue(NAME, rb.getString("CHANGE_MUMBLE_DIRECTORY_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("CHANGE_MUMBLE_DIRECTORY_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser mumbleDirectoryFileChooser = new JFileChooser(
					preferences.get(PREFERENCES_MUMBLE_DIRECTORY, MumbleOverlay.getDefaultMumbleInstallationPath()));
			mumbleDirectoryFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (mumbleDirectoryFileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				final String path = mumbleDirectoryFileChooser.getSelectedFile().getAbsolutePath();
				final String helper32 = MumbleOverlay.getMumbleHelperFilePath(path, false);
				final String helper64 = MumbleOverlay.getMumbleHelperFilePath(path, true);

				if (helper32 != null && helper64 != null) {
					preferences.put(PREFERENCES_MUMBLE_DIRECTORY, path);
					mumbleDirectoryLabel1.setText(path);
				} else
					JOptionPane.showMessageDialog(frame,
							rb.getString("INVALID_MUMBLE_DIRECTORY_DIALOG_TEXT_PREFIX")
									+ MumbleOverlay.getDefaultMumbleInstallationPath()
									+ rb.getString("INVALID_MUMBLE_DIRECTORY_DIALOG_TEXT_SUFFIX"),
							rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			}
		}

	}

	private class ChangeVJoyDirectoryAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -7672382299595684105L;

		public ChangeVJoyDirectoryAction() {
			putValue(NAME, rb.getString("CHANGE_VJOY_DIRECTORY_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("CHANGE_VJOY_DIRECTORY_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser vJoyDirectoryFileChooser = new JFileChooser(
					preferences.get(PREFERENCES_VJOY_DIRECTORY, VJoyOutputThread.getDefaultInstallationPath()));
			vJoyDirectoryFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (vJoyDirectoryFileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				final String path = vJoyDirectoryFileChooser.getSelectedFile().getAbsolutePath();
				final File file = new File(VJoyOutputThread.getLibraryFilePath(path));

				if (file.exists()) {
					preferences.put(PREFERENCES_VJOY_DIRECTORY, path);
					vJoyDirectoryLabel1.setText(path);
				} else
					JOptionPane.showMessageDialog(frame,
							rb.getString("INVALID_VJOY_DIRECTORY_DIALOG_TEXT_PREFIX")
									+ VJoyOutputThread.getDefaultInstallationPath()
									+ rb.getString("INVALID_VJOY_DIRECTORY_DIALOG_TEXT_SUFFIX"),
							rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			}
		}

	}

	private class DisplayIndicatorAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 3316770144012465987L;

		private final VirtualAxis virtualAxis;

		public DisplayIndicatorAction(VirtualAxis virtualAxis) {
			this.virtualAxis = virtualAxis;

			putValue(NAME, rb.getString("DISPLAY_INDICATOR_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("DISPLAY_INDICATOR_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (((JCheckBox) e.getSource()).isSelected())
				Input.getProfile().getVirtualAxisToColorMap().put(virtualAxis, new Color(0, 0, 0, 128));
			else
				Input.getProfile().getVirtualAxisToColorMap().remove(virtualAxis);

			setUnsavedChangesTitle();
			updateOverlayPanel();
		}

	}

	private class EditComponentAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 8811608785278071903L;

		private final Component component;

		public EditComponentAction(Component component) {
			this.component = component;

			putValue(NAME, rb.getString("EDIT_COMPONENT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("EDIT_COMPONENT_ACTION_DESCRIPTION_PREFIX") + component.getName()
					+ rb.getString("EDIT_COMPONENT_ACTION_DESCRIPTION_SUFFIX"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final EditActionsDialog editComponentDialog = new EditActionsDialog(Main.this, component, input);
			editComponentDialog.setVisible(true);

			suspendControllerSettingsUpdate = false;
		}

	}

	private static class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

		private static final String PROPERTY_TYPE = "type";
		private static final String PROPERTY_DATA = "data";

		@Override
		public T deserialize(JsonElement elem, java.lang.reflect.Type interfaceType, JsonDeserializationContext context)
				throws JsonParseException {
			final JsonObject wrapper = (JsonObject) elem;
			final JsonElement typeName = get(wrapper, PROPERTY_TYPE);
			final JsonElement data = get(wrapper, PROPERTY_DATA);
			final java.lang.reflect.Type actualType = typeForName(typeName);

			return context.deserialize(data, actualType);
		}

		private JsonElement get(final JsonObject wrapper, String memberName) {
			final JsonElement elem = wrapper.get(memberName);
			if (elem == null)
				throw new JsonParseException(getClass().getName() + ": No member '" + memberName
						+ "' found in what was expected to be an interface wrapper");
			return elem;
		}

		@Override
		public JsonElement serialize(T object, java.lang.reflect.Type interfaceType, JsonSerializationContext context) {
			final JsonObject wrapper = new JsonObject();
			wrapper.addProperty(PROPERTY_TYPE, object.getClass().getName());
			wrapper.add(PROPERTY_DATA, context.serialize(object));

			return wrapper;
		}

		private java.lang.reflect.Type typeForName(final JsonElement typeElem) {
			try {
				return Class.forName(typeElem.getAsString());
			} catch (final ClassNotFoundException e) {
				throw new JsonParseException(getClass().getName() + ": " + e);
			}
		}

	}

	private class NewAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 5703987691203427504L;

		public NewAction() {
			putValue(NAME, rb.getString("NEW_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("NEW_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			newProfile();
		}

	}

	private class OpenAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -8932510785275935297L;

		public OpenAction() {
			putValue(NAME, rb.getString("OPEN_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("OPEN_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				final File file = fileChooser.getSelectedFile();

				if (!loadProfile(file))
					JOptionPane.showMessageDialog(frame, rb.getString("COULD_NOT_LOAD_PROFILE_DIALOG_TEXT"),
							rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			}
		}

	}

	private class QuitAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 8952460723177800923L;

		public QuitAction() {
			putValue(NAME, rb.getString("QUIT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("QUIT_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			stopAll();
			System.exit(0);
		}

	}

	private class RemoveModeAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -1056071724769862582L;

		private final Mode mode;

		public RemoveModeAction(Mode mode) {
			this.mode = mode;

			putValue(NAME, rb.getString("REMOVE_MODE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("REMOVE_MODE_ACTION_DESCRIPTION_PREFIX") + mode.getDescription()
					+ rb.getString("REMOVE_MODE_ACTION_DESCRIPTION_SUFFIX"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Input.getProfile().removeMode(mode);
			setUnsavedChangesTitle();
			updateModesPanel();
		}

	}

	private class SaveAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -8469921697479550983L;

		public SaveAction() {
			putValue(NAME, rb.getString("SAVE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("SAVE_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (currentFile != null)
				saveProfile(currentFile);
			else
				saveProfileAs();
		}

	}

	private class SaveAsAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -8469921697479550983L;

		public SaveAsAction() {
			putValue(NAME, rb.getString("SAVE_AS_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("SAVE_AS_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			saveProfileAs();
		}

	}

	private class SelectControllerAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -2043467156713598592L;

		private final Controller controller;

		public SelectControllerAction(Controller controller) {
			this.controller = controller;

			final String name = controller.getName();
			putValue(NAME, name);
			putValue(SHORT_DESCRIPTION, rb.getString("SELECT_CONTROLLER_ACTION_DESCRIPTION_PREFIX") + name
					+ rb.getString("SELECT_CONTROLLER_ACTION_DESCRIPTION_SUFFIX"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			selectedController = controller;
			newProfile();
			preferences.put(PREFERENCES_LAST_CONTROLLER, controller.getName());
		}

	}

	private class SelectIndicatorColorAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 3316770144012465987L;

		private final VirtualAxis virtualAxis;

		public SelectIndicatorColorAction(VirtualAxis virtualAxis) {
			this.virtualAxis = virtualAxis;

			putValue(NAME, rb.getString("CHANGE_INDICATOR_COLOR_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("CHANGE_INDICATOR_COLOR_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final Color newColor = JColorChooser.showDialog(frame, "Choose Background Color",
					Input.getProfile().getVirtualAxisToColorMap().get(virtualAxis));
			if (newColor != null)
				Input.getProfile().getVirtualAxisToColorMap().replace(virtualAxis, newColor);

			setUnsavedChangesTitle();
			updateOverlayPanel();
		}

	}

	private class SetHostAction extends AbstractAction implements FocusListener {

		/**
		 *
		 */
		private static final long serialVersionUID = -7674562782751876814L;

		private final JTextField hostTextField;

		public SetHostAction(JTextField hostTextField) {
			this.hostTextField = hostTextField;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setHost();
		}

		@Override
		public void focusGained(FocusEvent e) {
		}

		@Override
		public void focusLost(FocusEvent e) {
			setHost();
		}

		private void setHost() {
			final String host = hostTextField.getText();

			if (host != null && host.length() > 0)
				preferences.put(PREFERENCES_HOST, host);
			else
				hostTextField.setText(preferences.get(PREFERENCES_HOST, ClientVJoyOutputThread.DEFAULT_HOST));
		}

	}

	private class SetModeDescriptionAction extends AbstractAction implements DocumentListener {

		/**
		 *
		 */
		private static final long serialVersionUID = -6706537047137827688L;

		private final Mode mode;
		private final JTextField modeDescriptionTextField;

		public SetModeDescriptionAction(Mode mode, JTextField modeDescriptionTextField) {
			this.mode = mode;
			this.modeDescriptionTextField = modeDescriptionTextField;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setModeDescription();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			setModeDescription();
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			setModeDescription();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			setModeDescription();
		}

		private void setModeDescription() {
			final String description = modeDescriptionTextField.getText();

			if (description != null && description.length() > 0) {
				mode.setDescription(description);
				setUnsavedChangesTitle();
			}
		}

	}

	private class ShowAboutDialogAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -2578971543384483382L;

		public ShowAboutDialogAction() {
			putValue(NAME, rb.getString("SHOW_ABOUT_DIALOG_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("SHOW_ABOUT_DIALOG_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final ImageIcon icon = new ImageIcon(Main.class.getResource(Main.ICON_RESOURCE_PATHS[2]));
			JOptionPane.showMessageDialog(frame,
					rb.getString("ABOUT_DIALOG_TEXT_PREFIX") + Version.getVersion()
							+ rb.getString("ABOUT_DIALOG_TEXT_SUFFIX"),
					(String) getValue(NAME), JOptionPane.INFORMATION_MESSAGE, icon);
		}

	}

	private class ShowAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 8578159622754054457L;

		public ShowAction() {
			putValue(NAME, rb.getString("SHOW_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("SHOW_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final WindowEvent openEvent = new WindowEvent(frame, WindowEvent.WINDOW_OPENED);
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(openEvent);
			frame.setVisible(true);
			frame.setExtendedState(Frame.NORMAL);
		}

	}

	private class StartClientAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 3975574941559749481L;

		public StartClientAction() {
			putValue(NAME, rb.getString("START_CLIENT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("START_CLIENT_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			startClient();
		}

	}

	private class StartLocalAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -2003502124995392039L;

		public StartLocalAction() {
			putValue(NAME, rb.getString("START_LOCAL_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("START_LOCAL_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			startLocal();
		}

	}

	private class StartServerAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1758447420975631146L;

		public StartServerAction() {
			putValue(NAME, rb.getString("START_SERVER_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("START_SERVER_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			startServer();
		}

	}

	private class StopClientAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -2863419586328503426L;

		public StopClientAction() {
			putValue(NAME, rb.getString("STOP_CLIENT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("STOP_CLIENT_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			stopClient(true);
		}

	}

	private class StopLocalAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -4859431944733030332L;

		public StopLocalAction() {
			putValue(NAME, rb.getString("STOP_LOCAL_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("STOP_LOCAL_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			stopLocal(true);
		}

	}

	private class StopServerAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 6023207463370122769L;

		public StopServerAction() {
			putValue(NAME, rb.getString("STOP_SERVER_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("STOP_SERVER_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			stopServer(true);
		}

	}

	private static final int OUTPUT_TYPE_NONE = 0;
	private static final int OUTPUT_TYPE_LOCAL = 1;
	private static final int OUTPUT_TYPE_CLIENT = 2;
	private static final int OUTPUT_TYPE_SERVER = 3;
	public static final int DIALOG_BOUNDS_X = 100;
	public static final int DIALOG_BOUNDS_Y = 100;
	public static final int DIALOG_BOUNDS_WIDTH = 580;
	public static final int DIALOG_BOUNDS_HEIGHT = 640;
	public static final int DIALOG_BOUNDS_X_Y_OFFSET = 25;
	public static final Dimension BUTTON_DIMENSION = new Dimension(100, 25);
	private static final String OPTION_AUTOSTART = "autostart";
	private static final String OPTION_TRAY = "tray";
	private static final String OPTION_VERSION = "version";
	private static final String OPTION_AUTOSTART_VALUE_LOCAL = "local";
	private static final String OPTION_AUTOSTART_VALUE_CLIENT = "client";
	private static final String OPTION_AUTOSTART_VALUE_SERVER = "server";
	public static final String STRING_RESOURCE_BUNDLE_BASENAME = "strings";
	private static final String PREFERENCES_POLL_INTERVAL = "poll_interval";
	private static final String PREFERENCES_LAST_CONTROLLER = "last_controller";
	private static final String PREFERENCES_LAST_PROFILE = "last_profile";
	public static final String PREFERENCES_VJOY_DIRECTORY = "vjoy_directory";
	private static final String PREFERENCES_VJOY_DEVICE = "vjoy_device";
	private static final String PREFERENCES_HOST = "host";
	private static final String PREFERENCES_PORT = "port";
	private static final String PREFERENCES_TIMEOUT = "timeout";
	private static final String PREFERENCES_SHOW_OVERLAY = "show_overlay";
	private static final String PREFERENCES_USE_MUMBLE_OVERLAY = "use_mumble_overlay";
	public static final String PREFERENCES_MUMBLE_DIRECTORY = "mumble_directory";
	private static final String PREFERENCES_MUMBLE_OVERLAY_FPS = "mumble_overlay_fps";
	private static final long ASSIGNMENTS_PANEL_UPDATE_INTERVAL = 100L;
	private static final String[] ICON_RESOURCE_PATHS = { "/icon_16.png", "/icon_32.png", "/icon_64.png",
			"/icon_128.png" };
	private static final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(STRING_RESOURCE_BUNDLE_BASENAME,
			Locale.getDefault());
	private static final Map<VirtualAxis, JProgressBar> virtualAxisToProgressBarMap = new HashMap<>();
	private static JFrame overlayFrame;
	private static JPanel indicatorPanel;
	private final static JLabel labelCurrentMode = new JLabel();
	private static LocalVJoyOutputThread localThread;
	private static ClientVJoyOutputThread clientThread;
	private static ServerOutputThread serverThread;
	private static Dimension prevScreenSize;
	private static boolean mumbleOverlayActive;
	private static Boolean mumbleOverlayRedraw;

	public static boolean is64Bit() {
		return "64".equals(System.getProperty("sun.arch.data.model"));
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				final Options options = new Options();
				options.addOption(OPTION_AUTOSTART, true, rb.getString("AUTOSTART_OPTION_DESCRIPTION"));
				options.addOption(OPTION_TRAY, false, rb.getString("TRAY_OPTION_DESCRIPTION"));
				options.addOption(OPTION_VERSION, false, rb.getString("VERSION_OPTION_DESCRIPTION"));

				try {
					final CommandLine commandLine = new DefaultParser().parse(options, args);
					if (commandLine.hasOption(OPTION_VERSION))
						System.out.println(rb.getString("APPLICATION_NAME") + ' ' + Version.getVersion());
					else {
						final Main main = new Main();
						if (!commandLine.hasOption(OPTION_TRAY))
							main.frame.setVisible(true);
						if (commandLine.hasOption(OPTION_AUTOSTART)) {
							final String optionValue = commandLine.getOptionValue(OPTION_AUTOSTART);

							if (OPTION_AUTOSTART_VALUE_LOCAL.equals(optionValue))
								main.startLocal();
							else if (OPTION_AUTOSTART_VALUE_CLIENT.equals(optionValue))
								main.startClient();
							else if (OPTION_AUTOSTART_VALUE_SERVER.equals(optionValue))
								main.startServer();
						}
					}
				} catch (final ParseException e) {
					final HelpFormatter helpFormatter = new HelpFormatter();
					helpFormatter.printHelp("ControllerBuddy", options, true);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private static void setEnabledRecursive(java.awt.Component component, boolean enabled) {
		component.setEnabled(enabled);

		if (component instanceof Container) {
			for (final java.awt.Component child : ((Container) component).getComponents())
				setEnabledRecursive(child, enabled);
		}
	}

	public static void setOverlayText(String text) {
		labelCurrentMode.setText(text);
		mumbleOverlayRedraw = true;
	}

	public static void updateOverlayAxisIndicators() {
		if (overlayFrame != null && overlayFrame.isAlwaysOnTop()) {
			overlayFrame.setAlwaysOnTop(false);
			overlayFrame.setAlwaysOnTop(true);
		}

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (prevScreenSize == null || screenSize.width != prevScreenSize.width
				|| screenSize.height != prevScreenSize.height) {
			prevScreenSize = screenSize;
			updateOverlayLocation();
		}

		for (final VirtualAxis va : Input.VirtualAxis.values()) {
			if (virtualAxisToProgressBarMap.containsKey(va)) {
				OutputThread outputThread = null;
				if (localThread != null && localThread.isAlive())
					outputThread = localThread;
				else if (clientThread != null && clientThread.isAlive())
					outputThread = clientThread;
				else if (serverThread != null && serverThread.isAlive())
					outputThread = serverThread;

				if (outputThread != null) {
					final JProgressBar progressBar = virtualAxisToProgressBarMap.get(va);
					progressBar.setMinimum(-outputThread.getMaxAxisValue());
					progressBar.setMaximum(outputThread.getMinAxisValue());

					final int newValue = -Input.getAxis().get(va);
					if (progressBar.getValue() != newValue) {
						progressBar.setValue(newValue);
						mumbleOverlayRedraw = true;
					}
				}
			}
		}
	}

	private static void updateOverlayLocation() {
		if (overlayFrame != null) {
			overlayFrame.pack();
			final Rectangle rectangle = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			final int x = (int) rectangle.getMaxX() - overlayFrame.getWidth();
			final int y = (int) rectangle.getMaxY() - overlayFrame.getHeight();
			overlayFrame.setLocation(x, y);
		}
	}

	private Controller selectedController;
	private Input input;
	private int lastOutputType = OUTPUT_TYPE_NONE;
	private boolean suspendControllerSettingsUpdate = false;
	private final Preferences preferences = Preferences.userNodeForPackage(getClass());
	private final JFrame frame;
	private final OpenAction openAction = new OpenAction();
	private JRadioButtonMenuItem startLocalRadioButtonMenuItem;
	private JRadioButtonMenuItem stopLocalRadioButtonMenuItem;
	private JRadioButtonMenuItem startClientRadioButtonMenuItem;
	private JRadioButtonMenuItem stopClientRadioButtonMenuItem;
	private final JRadioButtonMenuItem startServerRadioButtonMenuItem;
	private final JRadioButtonMenuItem stopServerRadioButtonMenuItem;
	private MenuItem showMenuItem;
	private final JPanel modesPanel;
	private final JScrollPane modesScrollPane;
	private final JPanel modesListPanel;
	private final JPanel assignmentsPanel;
	private final JPanel overlayPanel;
	private final JPanel settingsPanel;
	private final JScrollPane indicatorsScrollPane;
	private final JPanel indicatorsListPanel;
	private JLabel vJoyDirectoryLabel1;
	private JTextField hostTextField;
	private JPanel mumbleOverlaySettingsPanel;
	private JPanel mumbleDirectoryPanel;
	private JPanel mumbleOverlayFpsPanel;
	private JLabel mumbleDirectoryLabel1;
	private final JLabel statusLabel = new JLabel(rb.getString("STATUS_READY"));
	private TrayIcon trayIcon;

	private final JFileChooser fileChooser = new JFileChooser() {
		/**
		 *
		 */
		private static final long serialVersionUID = -4669170626378955605L;

		@Override
		public void approveSelection() {
			final File file = getSelectedFile();
			if (file.exists() && getDialogType() == SAVE_DIALOG) {
				final int result = JOptionPane.showConfirmDialog(this,
						file.getName() + rb.getString("FILE_EXISTS_DIALOG_TEXT"),
						rb.getString("FILE_EXISTS_DIALOG_TITLE"), JOptionPane.YES_NO_CANCEL_OPTION);
				switch (result) {
				case JOptionPane.NO_OPTION:
					return;
				case JOptionPane.CLOSED_OPTION:
					return;
				case JOptionPane.CANCEL_OPTION:
					cancelSelection();
					return;
				default:
					break;
				}
			}
			super.approveSelection();
		}
	};

	private File currentFile;

	public Main() {
		frame = new JFrame();
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);

				if (showMenuItem != null)
					showMenuItem.setEnabled(true);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				super.windowDeiconified(e);

				if (showMenuItem != null)
					showMenuItem.setEnabled(false);
			}

			@Override
			public void windowIconified(WindowEvent e) {
				super.windowIconified(e);

				if (showMenuItem != null)
					showMenuItem.setEnabled(true);
			}

			@Override
			public void windowOpened(WindowEvent e) {
				super.windowOpened(e);

				if (showMenuItem != null)
					showMenuItem.setEnabled(false);
			}

		});
		setTitle(rb.getString("APPLICATION_NAME"));
		frame.setBounds(DIALOG_BOUNDS_X, DIALOG_BOUNDS_Y, DIALOG_BOUNDS_WIDTH, DIALOG_BOUNDS_HEIGHT);
		frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		final List<Image> icons = new ArrayList<>();
		for (final String s : ICON_RESOURCE_PATHS) {
			final ImageIcon icon = new ImageIcon(Main.class.getResource(s));
			icons.add(icon.getImage());
		}
		frame.setIconImages(icons);

		final JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		final JMenu fileMenu = new JMenu(rb.getString("FILE_MENU"));
		menuBar.add(fileMenu);
		fileMenu.add(new NewAction());
		fileMenu.add(openAction);
		fileMenu.add(new SaveAction());
		fileMenu.add(new SaveAsAction());
		fileMenu.add(new JSeparator());
		final QuitAction quitAction = new QuitAction();
		fileMenu.add(quitAction);

		final JMenu deviceMenu = new JMenu(rb.getString("DEVICE_MENU"));
		deviceMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuCanceled(MenuEvent e) {
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuSelected(MenuEvent e) {
				deviceMenu.removeAll();

				final Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

				for (final Controller c : controllers) {
					if (c.getType() != Type.KEYBOARD && c.getType() != Type.MOUSE && c.getType() != Type.TRACKBALL
							&& c.getType() != Type.TRACKPAD && c.getType() != Type.UNKNOWN
							&& !c.getName().startsWith("vJoy"))
						deviceMenu.add(new SelectControllerAction(c));
				}
			}
		});
		deviceMenu.setEnabled(true);
		menuBar.add(deviceMenu);

		if (isWindows()) {
			final JMenu localMenu = new JMenu(rb.getString("LOCAL_MENU"));
			menuBar.add(localMenu);

			final ButtonGroup buttonGroupLocalState = new ButtonGroup();

			startLocalRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("START_MENU_ITEM"));
			startLocalRadioButtonMenuItem.setAction(new StartLocalAction());
			buttonGroupLocalState.add(startLocalRadioButtonMenuItem);
			localMenu.add(startLocalRadioButtonMenuItem);

			stopLocalRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("STOP_MENU_ITEM"));
			stopLocalRadioButtonMenuItem.setAction(new StopLocalAction());
			stopLocalRadioButtonMenuItem.setSelected(true);
			stopLocalRadioButtonMenuItem.setEnabled(false);
			buttonGroupLocalState.add(stopLocalRadioButtonMenuItem);
			localMenu.add(stopLocalRadioButtonMenuItem);

			final JMenu clientMenu = new JMenu(rb.getString("CLIENT_MENU"));
			menuBar.add(clientMenu);

			final ButtonGroup buttonGroupClientState = new ButtonGroup();

			startClientRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("START_MENU_ITEM"));
			startClientRadioButtonMenuItem.setAction(new StartClientAction());
			buttonGroupClientState.add(startClientRadioButtonMenuItem);
			clientMenu.add(startClientRadioButtonMenuItem);

			stopClientRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("STOP_MENU_ITEM"));
			stopClientRadioButtonMenuItem.setAction(new StopClientAction());
			stopClientRadioButtonMenuItem.setSelected(true);
			stopClientRadioButtonMenuItem.setEnabled(false);
			buttonGroupClientState.add(stopClientRadioButtonMenuItem);
			clientMenu.add(stopClientRadioButtonMenuItem);
		}

		final JMenu serverMenu = new JMenu(rb.getString("SERVER_MENU"));
		menuBar.add(serverMenu);

		final ButtonGroup buttonGroupServerState = new ButtonGroup();

		startServerRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("START_MENU_ITEM"));
		startServerRadioButtonMenuItem.setAction(new StartServerAction());
		buttonGroupServerState.add(startServerRadioButtonMenuItem);
		serverMenu.add(startServerRadioButtonMenuItem);

		stopServerRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("STOP_MENU_ITEM"));
		stopServerRadioButtonMenuItem.setAction(new StopServerAction());
		stopServerRadioButtonMenuItem.setSelected(true);
		stopServerRadioButtonMenuItem.setEnabled(false);
		buttonGroupServerState.add(stopServerRadioButtonMenuItem);
		serverMenu.add(stopServerRadioButtonMenuItem);

		final JMenu helpMenu = new JMenu(rb.getString("HELP_MENU"));
		menuBar.add(helpMenu);
		helpMenu.add(new ShowAboutDialogAction());

		final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
		frame.getContentPane().add(tabbedPane);

		modesPanel = new JPanel(new BorderLayout());
		tabbedPane.addTab(rb.getString("MODES_TAB"), null, modesPanel, null);

		modesListPanel = new JPanel();
		modesListPanel.setLayout(new GridBagLayout());

		modesScrollPane = new JScrollPane();
		modesScrollPane
				.setViewportBorder(BorderFactory.createMatteBorder(10, 10, 0, 10, modesListPanel.getBackground()));
		modesPanel.add(modesScrollPane, BorderLayout.CENTER);

		final JPanel addModePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton addButton = new JButton(new AddModeAction());
		addButton.setPreferredSize(BUTTON_DIMENSION);
		addModePanel.add(addButton);
		modesPanel.add(addModePanel, BorderLayout.SOUTH);

		assignmentsPanel = new JPanel();
		assignmentsPanel.setLayout(new GridBagLayout());

		final JScrollPane assignmentsScrollPane = new JScrollPane();
		assignmentsScrollPane
				.setViewportBorder(BorderFactory.createMatteBorder(10, 10, 0, 10, assignmentsPanel.getBackground()));
		tabbedPane.addTab(rb.getString("ASSIGNMENTS_TAB"), null, assignmentsScrollPane, null);

		overlayPanel = new JPanel(new BorderLayout());
		tabbedPane.addTab(rb.getString("OVERLAY_TAB"), null, overlayPanel, null);

		indicatorsListPanel = new JPanel();
		indicatorsListPanel.setLayout(new GridBagLayout());

		indicatorsScrollPane = new JScrollPane();
		indicatorsScrollPane
				.setViewportBorder(BorderFactory.createMatteBorder(10, 10, 0, 10, indicatorsListPanel.getBackground()));
		overlayPanel.add(indicatorsScrollPane, BorderLayout.CENTER);

		settingsPanel = new JPanel();
		settingsPanel.setLayout(new GridBagLayout());

		final JScrollPane settingsScrollPane = new JScrollPane();
		settingsScrollPane.setViewportView(settingsPanel);
		tabbedPane.addTab(rb.getString("SETTINGS_TAB"), null, settingsScrollPane, null);

		final GridBagConstraints panelGridBagConstraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1,
				0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 5);

		final FlowLayout panelFlowLayout = new FlowLayout(FlowLayout.LEADING, 10, 10);

		final JPanel pollIntervalPanel = new JPanel(panelFlowLayout);
		settingsPanel.add(pollIntervalPanel, panelGridBagConstraints);

		final JLabel pollIntervalLabel = new JLabel(rb.getString("POLL_INTERVAL_LABEL"));
		pollIntervalLabel.setPreferredSize(new Dimension(120, 15));
		pollIntervalPanel.add(pollIntervalLabel);

		final JSpinner pollIntervalSpinner = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_POLL_INTERVAL, OutputThread.DEFAULT_POLL_INTERVAL), 10, 500, 1));
		final JSpinner.DefaultEditor pollIntervalSpinnerEditor = new JSpinner.NumberEditor(pollIntervalSpinner, "#");
		((DefaultFormatter) pollIntervalSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		pollIntervalSpinner.setEditor(pollIntervalSpinnerEditor);
		pollIntervalSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_POLL_INTERVAL, (int) ((JSpinner) e.getSource()).getValue());
			}
		});
		pollIntervalPanel.add(pollIntervalSpinner);

		if (isWindows()) {
			final JPanel vJoyDirectoryPanel = new JPanel(panelFlowLayout);
			settingsPanel.add(vJoyDirectoryPanel, panelGridBagConstraints);

			final JLabel vJoyDirectoryLabel = new JLabel(rb.getString("VJOY_DIRECTORY_LABEL"));
			vJoyDirectoryLabel.setPreferredSize(new Dimension(120, 15));
			vJoyDirectoryPanel.add(vJoyDirectoryLabel);

			vJoyDirectoryLabel1 = new JLabel(
					preferences.get(PREFERENCES_VJOY_DIRECTORY, VJoyOutputThread.getDefaultInstallationPath()));
			vJoyDirectoryPanel.add(vJoyDirectoryLabel1);

			final JButton vJoyDirectoryButton = new JButton(new ChangeVJoyDirectoryAction());
			vJoyDirectoryPanel.add(vJoyDirectoryButton);

			final JPanel vJoyDevicePanel = new JPanel(panelFlowLayout);
			settingsPanel.add(vJoyDevicePanel, panelGridBagConstraints);

			final JLabel vJoyDeviceLabel = new JLabel(rb.getString("VJOY_DEVICE_LABEL"));
			vJoyDeviceLabel.setPreferredSize(new Dimension(120, 15));
			vJoyDevicePanel.add(vJoyDeviceLabel);

			final JSpinner vJoyDeviceSpinner = new JSpinner(new SpinnerNumberModel(
					preferences.getInt(PREFERENCES_VJOY_DEVICE, VJoyOutputThread.DEFAULT_VJOY_DEVICE), 1, 16, 1));
			final JSpinner.DefaultEditor vJoyDeviceSpinnerEditor = new JSpinner.NumberEditor(vJoyDeviceSpinner, "#");
			((DefaultFormatter) vJoyDeviceSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
			vJoyDeviceSpinner.setEditor(vJoyDeviceSpinnerEditor);
			vJoyDeviceSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					preferences.putInt(PREFERENCES_VJOY_DEVICE, (int) ((JSpinner) e.getSource()).getValue());
				}
			});
			vJoyDevicePanel.add(vJoyDeviceSpinner);

			final JPanel hostPanel = new JPanel(panelFlowLayout);
			settingsPanel.add(hostPanel, panelGridBagConstraints);

			final JLabel hostLabel = new JLabel(rb.getString("HOST_LABEL"));
			hostLabel.setPreferredSize(new Dimension(120, 15));
			hostPanel.add(hostLabel);

			hostTextField = new JTextField(preferences.get(PREFERENCES_HOST, ClientVJoyOutputThread.DEFAULT_HOST), 10);
			final SetHostAction setHostAction = new SetHostAction(hostTextField);
			hostTextField.addActionListener(setHostAction);
			hostTextField.addFocusListener(setHostAction);
			hostPanel.add(hostTextField);
		}

		final JPanel portPanel = new JPanel(panelFlowLayout);
		settingsPanel.add(portPanel, panelGridBagConstraints);

		final JLabel portLabel = new JLabel(rb.getString("PORT_LABEL"));
		portLabel.setPreferredSize(new Dimension(120, 15));
		portPanel.add(portLabel);

		final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_PORT, ServerOutputThread.DEFAULT_PORT), 1024, 65535, 1));
		final JSpinner.DefaultEditor portSpinnerEditor = new JSpinner.NumberEditor(portSpinner, "#");
		((DefaultFormatter) portSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		portSpinner.setEditor(portSpinnerEditor);
		portSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_PORT, (int) ((JSpinner) e.getSource()).getValue());
			}
		});
		portPanel.add(portSpinner);

		final JPanel timeoutPanel = new JPanel(panelFlowLayout);
		settingsPanel.add(timeoutPanel, panelGridBagConstraints);

		final JLabel timeoutLabel = new JLabel(rb.getString("TIMEOUT_LABEL"));
		timeoutLabel.setPreferredSize(new Dimension(120, 15));
		timeoutPanel.add(timeoutLabel);

		final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_TIMEOUT, ServerOutputThread.DEFAULT_TIMEOUT), 10, 60000, 1));
		final JSpinner.DefaultEditor timeoutSpinnerEditor = new JSpinner.NumberEditor(timeoutSpinner, "#");
		((DefaultFormatter) timeoutSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		timeoutSpinner.setEditor(timeoutSpinnerEditor);
		timeoutSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_TIMEOUT, (int) ((JSpinner) e.getSource()).getValue());
			}
		});
		timeoutPanel.add(timeoutSpinner);

		if (Toolkit.getDefaultToolkit().isAlwaysOnTopSupported()
				|| preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, true)) {
			final JPanel overlaySettingsPanel = new JPanel(panelFlowLayout);
			settingsPanel.add(overlaySettingsPanel, panelGridBagConstraints);

			final JLabel overlayLabel = new JLabel(rb.getString("OVERLAY_LABEL"));
			overlayLabel.setPreferredSize(new Dimension(120, 15));
			overlaySettingsPanel.add(overlayLabel);

			final JCheckBox showOverlayCheckBox = new JCheckBox(rb.getString("SHOW_OVERLAY_CHECK_BOX"));
			showOverlayCheckBox.setSelected(preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, true));
			showOverlayCheckBox.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					final boolean showOverlay = ((JCheckBox) e.getSource()).isSelected();

					preferences.putBoolean(PREFERENCES_SHOW_OVERLAY, showOverlay);
					updateOverlaySettings();
				}
			});
			overlaySettingsPanel.add(showOverlayCheckBox);

			if (isWindows() && is64Bit()) {
				mumbleOverlaySettingsPanel = new JPanel(panelFlowLayout);
				settingsPanel.add(mumbleOverlaySettingsPanel, panelGridBagConstraints);

				final JLabel mumbleOverlayLabel = new JLabel(rb.getString("MUMBLE_OVERLAY_LABEL"));
				mumbleOverlayLabel.setPreferredSize(new Dimension(120, 15));
				mumbleOverlaySettingsPanel.add(mumbleOverlayLabel);

				final JCheckBox useMumbleOverlayCheckBox = new JCheckBox(rb.getString("USE_MUMBLE_OVERLAY_CHECK_BOX"));
				useMumbleOverlayCheckBox.setSelected(preferences.getBoolean(PREFERENCES_USE_MUMBLE_OVERLAY, true));
				useMumbleOverlayCheckBox.addChangeListener(new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent e) {
						preferences.putBoolean(PREFERENCES_USE_MUMBLE_OVERLAY,
								((JCheckBox) e.getSource()).isSelected());
					}
				});
				mumbleOverlaySettingsPanel.add(useMumbleOverlayCheckBox);

				mumbleDirectoryPanel = new JPanel(panelFlowLayout);
				settingsPanel.add(mumbleDirectoryPanel, panelGridBagConstraints);

				final JLabel mumbleDirectoryLabel = new JLabel(rb.getString("MUMBLE_DIRECTORY_LABEL"));
				mumbleDirectoryLabel.setPreferredSize(new Dimension(120, 15));
				mumbleDirectoryPanel.add(mumbleDirectoryLabel);

				mumbleDirectoryLabel1 = new JLabel(preferences.get(PREFERENCES_MUMBLE_DIRECTORY,
						MumbleOverlay.getDefaultMumbleInstallationPath()));
				mumbleDirectoryPanel.add(mumbleDirectoryLabel1);

				final JButton mumbleDirectoryButton = new JButton(new ChangeMumbleDirectoryAction());
				mumbleDirectoryPanel.add(mumbleDirectoryButton);

				mumbleOverlayFpsPanel = new JPanel(panelFlowLayout);
				settingsPanel.add(mumbleOverlayFpsPanel, panelGridBagConstraints);

				final JLabel mumbleOverlayFpsLabel = new JLabel(rb.getString("MUMBLE_OVERLAY_FPS_LABEL"));
				mumbleOverlayFpsLabel.setPreferredSize(new Dimension(120, 15));
				mumbleOverlayFpsPanel.add(mumbleOverlayFpsLabel);

				final JSpinner mumbleOverlayFpsSpinner = new JSpinner(new SpinnerNumberModel(
						preferences.getDouble(PREFERENCES_MUMBLE_OVERLAY_FPS, MumbleOverlay.DEFAULT_MUMBLE_OVERLAY_FPS),
						1.0, 60.0, 1.0));
				final JSpinner.DefaultEditor mumbleOverlayFpsSpinnerEditor = new JSpinner.NumberEditor(
						mumbleOverlayFpsSpinner, "#");
				((DefaultFormatter) mumbleOverlayFpsSpinnerEditor.getTextField().getFormatter())
						.setCommitsOnValidEdit(true);
				mumbleOverlayFpsSpinner.setEditor(mumbleOverlayFpsSpinnerEditor);
				mumbleOverlayFpsSpinner.addChangeListener(new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent e) {
						preferences.putDouble(PREFERENCES_MUMBLE_OVERLAY_FPS,
								(double) ((JSpinner) e.getSource()).getValue());
					}
				});
				mumbleOverlayFpsPanel.add(mumbleOverlayFpsSpinner);
			}
		}

		if (SystemTray.isSupported()) {
			final PopupMenu popupMenu = new PopupMenu();

			final ShowAction showAction = new ShowAction();
			showMenuItem = new MenuItem((String) showAction.getValue(Action.NAME));
			showMenuItem.addActionListener(showAction);
			popupMenu.add(showMenuItem);

			popupMenu.addSeparator();

			final MenuItem openMenuItem = new MenuItem((String) openAction.getValue(Action.NAME));
			openMenuItem.addActionListener(openAction);
			popupMenu.add(openMenuItem);

			popupMenu.addSeparator();

			final MenuItem quitMenuItem = new MenuItem((String) quitAction.getValue(Action.NAME));
			quitMenuItem.addActionListener(quitAction);
			popupMenu.add(quitMenuItem);

			trayIcon = new TrayIcon(frame.getIconImage());
			trayIcon.setToolTip(rb.getString("APPLICATION_NAME"));
			trayIcon.addActionListener(showAction);
			trayIcon.setPopupMenu(popupMenu);
			try {
				SystemTray.getSystemTray().add(trayIcon);
			} catch (final AWTException e) {
				e.printStackTrace();
			}
		}

		settingsPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		final Border outsideBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		final Border insideBorder = BorderFactory.createEmptyBorder(0, 5, 0, 5);
		statusLabel.setBorder(BorderFactory.createCompoundBorder(outsideBorder, insideBorder));
		frame.add(statusLabel, BorderLayout.SOUTH);

		final FileNameExtensionFilter filter = new FileNameExtensionFilter(rb.getString("PROFILE_FILE_DESCRIPTION"),
				rb.getString("PROFILE_FILE_EXTENSION"));
		fileChooser.setFileFilter(filter);
		fileChooser.setSelectedFile(new File(rb.getString("PROFILE_FILE_SUFFIX")));

		final String lastControllerName = preferences.get(PREFERENCES_LAST_CONTROLLER, null);

		for (final Controller c : ControllerEnvironment.getDefaultEnvironment().getControllers())
			if (c.getType() != Type.KEYBOARD && c.getType() != Type.MOUSE && c.getType() != Type.TRACKBALL
					&& c.getType() != Type.TRACKPAD && c.getType() != Type.UNKNOWN && !c.getName().startsWith("vJoy")) {
				final boolean lastControllerFound = c.getName().equals(lastControllerName);

				if (selectedController == null || lastControllerFound)
					selectedController = c;

				if (lastControllerFound)
					break;
			}

		if (selectedController == null) {
			JOptionPane.showMessageDialog(frame, rb.getString("NO_CONTROLLER_CONNECTED_DIALOG_TEXT"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		} else {
			newProfile();

			final String path = preferences.get(PREFERENCES_LAST_PROFILE, null);
			if (path != null) {
				if (!loadProfile(new File(path)))
					JOptionPane.showMessageDialog(frame, rb.getString("COULD_NOT_LOAD_PROFILE_DIALOG_TEXT"),
							rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			}

			final Thread updateAssignmentsPanelThread = new Thread() {

				@Override
				public void run() {
					while (true) {
						if (!suspendControllerSettingsUpdate
								&& assignmentsScrollPane.equals(tabbedPane.getSelectedComponent())
								&& frame.getState() != Frame.ICONIFIED)
							EventQueue.invokeLater(new Runnable() {

								@Override
								public void run() {
									assignmentsPanel.removeAll();

									final Controller controller = input.getController();
									if (controller != null) {
										controller.poll();

										for (final Component c : Input.getComponents(controller)) {
											final JPanel componentPanel = new JPanel(new GridBagLayout());
											assignmentsPanel.add(componentPanel,
													new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0,
															0.0, GridBagConstraints.FIRST_LINE_START,
															GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 5,
															5));

											final String name = c.getName();
											final float value = c.getPollData();

											final JLabel nameLabel = new JLabel();
											nameLabel.setPreferredSize(new Dimension(100, 15));

											final GridBagConstraints nameGridBagConstraints = new GridBagConstraints(0,
													0, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE,
													GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);

											final GridBagConstraints valueGridBagConstraints = new GridBagConstraints(2,
													0, 1, 1, 1.0, 1.0, GridBagConstraints.BASELINE,
													GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);

											if (c.isAnalog()) {
												nameLabel.setText(rb.getString("AXIS_LABEL") + name);
												componentPanel.add(nameLabel, nameGridBagConstraints);

												componentPanel.add(Box.createGlue(),
														new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1,
																0.0, 0.0, GridBagConstraints.BASELINE,
																GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

												final JProgressBar valueProgressBar = new JProgressBar(-100, 100);
												valueProgressBar.setValue((int) (value * 100.0f));
												componentPanel.add(valueProgressBar, valueGridBagConstraints);
											} else {
												nameLabel.setText(rb.getString("BUTTON_LABEL") + name);
												componentPanel.add(nameLabel, nameGridBagConstraints);

												componentPanel.add(Box.createGlue(),
														new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1,
																0.0, 0.0, GridBagConstraints.BASELINE,
																GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

												final JLabel valueLabel = new JLabel();
												final StringWriter sw = new StringWriter();
												if (value > 0.0f)
													sw.append(rb.getString("BUTTON_DOWN_LABEL"));
												else {
													sw.append(rb.getString("BUTTON_UP_LABEL"));
													valueLabel.setForeground(Color.LIGHT_GRAY);
												}
												sw.append(" (" + String.valueOf(value) + ')');
												valueLabel.setText(sw.toString());
												componentPanel.add(valueLabel, valueGridBagConstraints);
											}

											componentPanel.add(Box.createGlue(),
													new GridBagConstraints(3, GridBagConstraints.RELATIVE, 1, 1, 0.0,
															0.0, GridBagConstraints.BASELINE, GridBagConstraints.NONE,
															new Insets(0, 0, 0, 0), 0, 0));

											final JButton editButton = new JButton(new EditComponentAction(c));
											editButton.setPreferredSize(BUTTON_DIMENSION);
											editButton.addMouseListener(new MouseListener() {

												@Override
												public void mouseClicked(MouseEvent e) {
												}

												@Override
												public void mouseEntered(MouseEvent e) {
												}

												@Override
												public void mouseExited(MouseEvent e) {
												}

												@Override
												public void mousePressed(MouseEvent e) {
													suspendControllerSettingsUpdate = true;
												}

												@Override
												public void mouseReleased(MouseEvent e) {
												}
											});
											componentPanel.add(editButton,
													new GridBagConstraints(4, GridBagConstraints.RELATIVE, 1, 1, 0.0,
															0.0, GridBagConstraints.BASELINE, GridBagConstraints.NONE,
															new Insets(0, 0, 0, 0), 0, 0));
										}

										assignmentsPanel.add(Box.createGlue(),
												new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
														GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
														new Insets(0, 0, 0, 0), 0, 0));
									}

									setEnabledRecursive(assignmentsPanel, assignmentsPanel.isEnabled());
									assignmentsScrollPane.setViewportView(assignmentsPanel);
								}
							});

						try {
							Thread.sleep(ASSIGNMENTS_PANEL_UPDATE_INTERVAL);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			};
			updateAssignmentsPanelThread.start();
		}
	}

	private void deInitOverlay() {
		if (overlayFrame != null) {
			overlayFrame.setVisible(false);
			overlayFrame.remove(indicatorPanel);
		}

		virtualAxisToProgressBarMap.clear();
		if (isMumbleOverlayEnabled()) {
			mumbleOverlayActive = false;
			mumbleOverlayRedraw = true;
		}
	}

	public JFrame getFrame() {
		return frame;
	}

	public Preferences getPreferences() {
		return preferences;
	}

	private void initOverlay() {
		String longestDescription = "";
		for (final Mode m : Input.getProfile().getModes()) {
			final String description = m.getDescription();
			if (description.length() > longestDescription.length())
				longestDescription = description;
		}

		final FontMetrics fontMetrics = labelCurrentMode.getFontMetrics(labelCurrentMode.getFont());
		labelCurrentMode
				.setPreferredSize(new Dimension(fontMetrics.stringWidth(longestDescription), fontMetrics.getHeight()));
		labelCurrentMode.setForeground(Color.RED);
		labelCurrentMode.setHorizontalAlignment(SwingConstants.RIGHT);
		labelCurrentMode.setText(Input.getProfile().getActiveMode().getDescription());

		final boolean mumbleOverlayEnabled = isMumbleOverlayEnabled();

		if (overlayFrame == null) {
			overlayFrame = new JFrame();
			overlayFrame.setLayout(new BorderLayout());
			overlayFrame.setAlwaysOnTop(!mumbleOverlayEnabled);
			overlayFrame.setFocusableWindowState(false);
			overlayFrame.setUndecorated(true);
			overlayFrame.setBackground(new Color(255, 255, 255, 0));
			overlayFrame.add(labelCurrentMode, BorderLayout.PAGE_END);
		}

		indicatorPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		indicatorPanel.setBackground(new Color(255, 255, 255, 0));

		for (final VirtualAxis va : Input.VirtualAxis.values()) {
			final Map<VirtualAxis, Color> virtualAxisToColorMap = Input.getProfile().getVirtualAxisToColorMap();

			if (virtualAxisToColorMap.containsKey(va)) {
				final JProgressBar progressBar = new JProgressBar(SwingConstants.VERTICAL);
				progressBar.setPreferredSize(new Dimension(21, 149));
				progressBar.setBorder(
						BorderFactory.createDashedBorder(Color.BLACK, (float) progressBar.getPreferredSize().getWidth(),
								(float) progressBar.getPreferredSize().getWidth()));
				progressBar.setBackground(Color.LIGHT_GRAY);
				progressBar.setForeground(virtualAxisToColorMap.get(va));
				indicatorPanel.add(progressBar);
				virtualAxisToProgressBarMap.put(va, progressBar);
			}
		}

		overlayFrame.add(indicatorPanel);

		updateOverlayLocation();
		overlayFrame.setVisible(true);

		if (mumbleOverlayEnabled) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					while (QCoreApplication.instance() != null) {
						try {
							Thread.sleep(100L);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					}
					QCoreApplication.initialize(new String[0]);

					final MumbleOverlay mumbleOverlay = new MumbleOverlay(Main.this);

					try {
						if (mumbleOverlay.init()) {
							final BufferedImage bufferedImage = new BufferedImage(overlayFrame.getWidth(),
									overlayFrame.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
							final Graphics2D graphics = bufferedImage.createGraphics();

							mumbleOverlayActive = true;

							new Thread(new Runnable() {

								@Override
								public void run() {
									final int interval = (int) Math
											.round(1000.0 / preferences.getDouble(PREFERENCES_MUMBLE_OVERLAY_FPS,
													MumbleOverlay.DEFAULT_MUMBLE_OVERLAY_FPS));

									while (mumbleOverlayActive) {
										QCoreApplication.invokeLater(new Runnable() {

											@Override
											public void run() {
												synchronized (mumbleOverlayRedraw) {
													if (mumbleOverlayRedraw || mumbleOverlay.hasDirtyClient()) {
														overlayFrame.print(graphics);
														mumbleOverlay.render(bufferedImage);
														mumbleOverlayRedraw = false;
													}
												}
											}
										});

										try {
											Thread.sleep(interval);
										} catch (final InterruptedException e) {
											e.printStackTrace();
										}

									}
									graphics.dispose();
									QCoreApplication.invokeLater(new Runnable() {

										@Override
										public void run() {
											if (mumbleOverlay != null)
												mumbleOverlay.deInit();

											QCoreApplication.exit();
										}
									});
								}
							}).start();

							QCoreApplication.instance().exec();
						}
					} catch (final Exception e) {
						e.printStackTrace();
						JOptionPane.showMessageDialog(getFrame(),
								rb.getString("MUMBLE_OVERLAY_GENERAL_INITIALIZATION_ERROR_DIALOG_TEXT"),
								rb.getString("WARNING_DIALOG_TITLE"), JOptionPane.WARNING_MESSAGE);
					} finally {
						final QCoreApplication app = QCoreApplication.instance();
						if (app != null)
							app.dispose();
					}
				}
			}).start();
		}
	}

	private boolean isMumbleOverlayEnabled() {
		if (isWindows() && is64Bit() && preferences.getBoolean(PREFERENCES_USE_MUMBLE_OVERLAY, true))
			return true;
		else
			return false;
	}

	private boolean loadProfile(File file) {
		stopAll();

		boolean result = false;

		try {
			final String jsonString = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			final Gson gson = new GsonBuilder().registerTypeAdapter(IAction.class, new InterfaceAdapter<>()).create();

			try {
				final Profile profile = gson.fromJson(jsonString, Profile.class);

				result = Input.setProfile(profile, input.getController());
				if (result) {
					saveLastProfile(file);
					updateModesPanel();
					updateOverlayPanel();
					setTitle(file.getName() + rb.getString("MAIN_FRAME_TITLE_SUFFIX"));
					setStatusBarText(rb.getString("STATUS_PROFILE_LOADED") + file.getAbsolutePath());
					scheduleStatusBarText(rb.getString("STATUS_READY"));
					fileChooser.setSelectedFile(file);

					restartLast();
				}
			} catch (final JsonSyntaxException e) {
				e.printStackTrace();
			}

			return result;
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	private void newProfile() {
		stopAll();

		currentFile = null;
		input = new Input(selectedController);

		setTitle(rb.getString("MAIN_FRAME_TITLE_UNSAVED_PROFILE"));
		updateModesPanel();
		updateOverlayPanel();
		setStatusBarText(rb.getString("STATUS_READY"));
		fileChooser.setSelectedFile(new File(rb.getString("PROFILE_FILE_SUFFIX")));
	}

	public void restartLast() {
		switch (lastOutputType) {
		case OUTPUT_TYPE_LOCAL:
			startLocal();
			break;
		case OUTPUT_TYPE_CLIENT:
			startClient();
			break;
		case OUTPUT_TYPE_SERVER:
			startServer();
			break;
		case OUTPUT_TYPE_NONE:
		default:
			break;
		}
	}

	private void saveLastProfile(File file) {
		currentFile = file;
		preferences.put(PREFERENCES_LAST_PROFILE, file.getAbsolutePath());
	}

	private void saveProfile(File file) {
		input.reset();

		final String profileFileSuffix = rb.getString("PROFILE_FILE_SUFFIX");
		if (!file.getName().toLowerCase().endsWith(profileFileSuffix))
			file = new File(file.getAbsoluteFile() + profileFileSuffix);

		final Gson gson = new GsonBuilder().registerTypeAdapter(IAction.class, new InterfaceAdapter<>())
				.setPrettyPrinting().create();
		final String jsonString = gson.toJson(Input.getProfile());

		try (FileOutputStream fos = new FileOutputStream(file)) {
			final Writer writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
			writer.write(jsonString);
			writer.flush();
			fos.flush();
			fos.close();

			saveLastProfile(file);
			setTitle(file.getName() + rb.getString("MAIN_FRAME_TITLE_SUFFIX"));
			setStatusBarText(rb.getString("STATUS_PROFILE_SAVED") + file.getAbsolutePath());
			scheduleStatusBarText(rb.getString("STATUS_READY"));
		} catch (final IOException e1) {
			e1.printStackTrace();
		}
	}

	private void saveProfileAs() {
		fileChooser.setSelectedFile(currentFile);
		if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
			saveProfile(fileChooser.getSelectedFile());
		}
	}

	public void scheduleStatusBarText(String text) {
		class StatusBarTextTimerTask extends TimerTask {

			private final String newText;
			private final String originalText;

			public StatusBarTextTimerTask(String newText) {
				this.newText = newText;
				originalText = statusLabel.getText();
			}

			@Override
			public void run() {
				if (statusLabel.getText().equals(originalText))
					setStatusBarText(newText);
			}
		}

		new Timer().schedule(new StatusBarTextTimerTask(text), 5000L);
	}

	public void setStatusBarText(String text) {
		if (statusLabel != null)
			statusLabel.setText(text);
	}

	private void setTitle(String title) {
		frame.setTitle(title);
		if (trayIcon != null)
			trayIcon.setToolTip(title);
	}

	public void setUnsavedChangesTitle() {
		final String title = frame.getTitle();

		if (!title.startsWith(rb.getString("MAIN_FRAME_TITLE_UNSAVED_PROFILE"))
				&& !title.startsWith(rb.getString("MAIN_FRAME_TITLE_PREFIX")))
			setTitle(rb.getString("MAIN_FRAME_TITLE_PREFIX") + title);
	}

	public void startClient() {
		lastOutputType = OUTPUT_TYPE_CLIENT;
		startClientRadioButtonMenuItem.setSelected(true);
		startLocalRadioButtonMenuItem.setEnabled(false);
		startClientRadioButtonMenuItem.setEnabled(false);
		startServerRadioButtonMenuItem.setEnabled(false);
		stopClientRadioButtonMenuItem.setEnabled(true);
		clientThread = new ClientVJoyOutputThread(Main.this, input);
		clientThread.setvJoyDevice(
				new UINT(preferences.getInt(PREFERENCES_VJOY_DEVICE, VJoyOutputThread.DEFAULT_VJOY_DEVICE)));
		clientThread.setHost(hostTextField.getText());
		clientThread.setPort(preferences.getInt(PREFERENCES_PORT, ServerOutputThread.DEFAULT_PORT));
		clientThread.setTimeout(preferences.getInt(PREFERENCES_TIMEOUT, ServerOutputThread.DEFAULT_TIMEOUT));
		clientThread.start();

		if (preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, true))
			initOverlay();
	}

	public void startLocal() {
		lastOutputType = OUTPUT_TYPE_LOCAL;
		startLocalRadioButtonMenuItem.setSelected(true);
		startLocalRadioButtonMenuItem.setEnabled(false);
		startClientRadioButtonMenuItem.setEnabled(false);
		startServerRadioButtonMenuItem.setEnabled(false);
		stopLocalRadioButtonMenuItem.setEnabled(true);
		setEnabledRecursive(modesPanel, false);
		setEnabledRecursive(assignmentsPanel, false);
		setEnabledRecursive(overlayPanel, false);
		setEnabledRecursive(settingsPanel, false);
		localThread = new LocalVJoyOutputThread(Main.this, input);
		localThread.setvJoyDevice(
				new UINT(preferences.getInt(PREFERENCES_VJOY_DEVICE, VJoyOutputThread.DEFAULT_VJOY_DEVICE)));
		localThread
				.setPollInterval(preferences.getInt(PREFERENCES_POLL_INTERVAL, VJoyOutputThread.DEFAULT_POLL_INTERVAL));
		localThread.start();

		if (preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, true))
			initOverlay();
	}

	public void startServer() {
		lastOutputType = OUTPUT_TYPE_SERVER;
		startServerRadioButtonMenuItem.setSelected(true);
		if (isWindows()) {
			startLocalRadioButtonMenuItem.setEnabled(false);
			startClientRadioButtonMenuItem.setEnabled(false);
		}
		startServerRadioButtonMenuItem.setEnabled(false);
		stopServerRadioButtonMenuItem.setEnabled(true);
		setEnabledRecursive(modesPanel, false);
		setEnabledRecursive(assignmentsPanel, false);
		setEnabledRecursive(overlayPanel, false);
		setEnabledRecursive(settingsPanel, false);
		serverThread = new ServerOutputThread(Main.this, input);
		serverThread.setPort(preferences.getInt(PREFERENCES_PORT, ServerOutputThread.DEFAULT_PORT));
		serverThread.setTimeout(preferences.getInt(PREFERENCES_TIMEOUT, ServerOutputThread.DEFAULT_TIMEOUT));
		serverThread
				.setPollInterval(preferences.getInt(PREFERENCES_POLL_INTERVAL, VJoyOutputThread.DEFAULT_POLL_INTERVAL));
		serverThread.start();
	}

	private void stopAll() {
		if (isWindows()) {
			stopLocal(false);
			stopClient(false);
		}
		stopServer(false);

		while ((localThread != null && localThread.isAlive()) || (clientThread != null && clientThread.isAlive())
				|| (serverThread != null && serverThread.isAlive())) {
			try {
				Thread.sleep(100L);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void stopClient(boolean resetLastOutputType) {
		if (clientThread != null)
			clientThread.stopOutput();
		stopClientRadioButtonMenuItem.setSelected(true);
		stopClientRadioButtonMenuItem.setEnabled(false);
		startLocalRadioButtonMenuItem.setEnabled(true);
		startClientRadioButtonMenuItem.setEnabled(true);
		startServerRadioButtonMenuItem.setEnabled(true);
		if (resetLastOutputType)
			lastOutputType = OUTPUT_TYPE_NONE;

		deInitOverlay();
	}

	public void stopLocal(boolean resetLastOutputType) {
		if (localThread != null)
			localThread.stopOutput();

		stopLocalRadioButtonMenuItem.setSelected(true);
		stopLocalRadioButtonMenuItem.setEnabled(false);
		startLocalRadioButtonMenuItem.setEnabled(true);
		startClientRadioButtonMenuItem.setEnabled(true);
		startServerRadioButtonMenuItem.setEnabled(true);
		setEnabledRecursive(modesPanel, true);
		setEnabledRecursive(assignmentsPanel, true);
		setEnabledRecursive(overlayPanel, true);
		setEnabledRecursive(settingsPanel, true);
		updateOverlaySettings();

		if (resetLastOutputType)
			lastOutputType = OUTPUT_TYPE_NONE;

		deInitOverlay();
	}

	public void stopServer(boolean resetLastOutputType) {
		if (serverThread != null)
			serverThread.stopOutput();

		stopServerRadioButtonMenuItem.setSelected(true);
		stopServerRadioButtonMenuItem.setEnabled(false);

		if (isWindows()) {
			startLocalRadioButtonMenuItem.setEnabled(true);
			startClientRadioButtonMenuItem.setEnabled(true);
		}

		startServerRadioButtonMenuItem.setEnabled(true);
		setEnabledRecursive(modesPanel, true);
		setEnabledRecursive(assignmentsPanel, true);
		setEnabledRecursive(overlayPanel, true);
		setEnabledRecursive(settingsPanel, true);
		updateOverlaySettings();

		if (resetLastOutputType)
			lastOutputType = OUTPUT_TYPE_NONE;
	}

	private void updateModesPanel() {
		modesListPanel.removeAll();

		final List<Mode> modes = Input.getProfile().getModes();
		for (final Mode p : modes) {
			final JPanel modePanel = new JPanel(new GridBagLayout());
			modesListPanel.add(modePanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
					GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 5));

			final JLabel modeNoLabel = new JLabel(rb.getString("MODE_NO_LABEL_PREFIX") + modes.indexOf(p));
			modeNoLabel.setPreferredSize(new Dimension(100, 15));
			modePanel.add(modeNoLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			modePanel.add(Box.createGlue(), new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final JTextField descriptionTextField = new JTextField(p.getDescription(), 20);
			modePanel.add(descriptionTextField, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final SetModeDescriptionAction setModeDescriptionAction = new SetModeDescriptionAction(p,
					descriptionTextField);
			descriptionTextField.addActionListener(setModeDescriptionAction);
			descriptionTextField.getDocument().addDocumentListener(setModeDescriptionAction);

			modePanel.add(Box.createGlue(), new GridBagConstraints(3, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			if (Profile.isDefaultMode(p)) {
				descriptionTextField.setEditable(false);
				modePanel.add(Box.createHorizontalStrut(BUTTON_DIMENSION.width));
			} else {
				final JButton deleteButton = new JButton(new RemoveModeAction(p));
				deleteButton.setPreferredSize(BUTTON_DIMENSION);
				modePanel.add(deleteButton, new GridBagConstraints(4, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
						GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			}
		}

		modesListPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		modesScrollPane.setViewportView(modesListPanel);
	}

	private void updateOverlayPanel() {
		indicatorsListPanel.removeAll();

		for (final VirtualAxis va : Input.VirtualAxis.values()) {
			final JPanel indicatorPanel = new JPanel(new GridBagLayout());
			indicatorsListPanel.add(indicatorPanel,
					new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
							GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0),
							0, 5));

			final JLabel virtualAxisLabel = new JLabel(va.toString() + rb.getString("AXIS_LABEL_SUFFIX"));
			virtualAxisLabel.setPreferredSize(new Dimension(100, 15));
			indicatorPanel.add(virtualAxisLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final boolean enabled = Input.getProfile().getVirtualAxisToColorMap().containsKey(va);

			final JLabel colorLabel = new JLabel();
			if (enabled) {
				colorLabel.setOpaque(true);
				colorLabel.setBackground(Input.getProfile().getVirtualAxisToColorMap().get(va));
			} else
				colorLabel.setText(rb.getString("INDICATOR_DISABLED_LABEL"));
			colorLabel.setHorizontalAlignment(SwingConstants.CENTER);

			colorLabel.setPreferredSize(new Dimension(100, 15));
			colorLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			indicatorPanel.add(colorLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final JButton colorButton = new JButton(new SelectIndicatorColorAction(va));
			colorButton.setPreferredSize(BUTTON_DIMENSION);
			colorButton.setEnabled(enabled);
			indicatorPanel.add(colorButton, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final JCheckBox displayCheckBox = new JCheckBox(new DisplayIndicatorAction(va));
			displayCheckBox.setSelected(enabled);
			indicatorPanel.add(displayCheckBox, new GridBagConstraints(3, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		}

		indicatorsListPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		indicatorsScrollPane.setViewportView(indicatorsListPanel);
	}

	private void updateOverlaySettings() {
		final boolean showOverlay = preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, true);

		setEnabledRecursive(mumbleOverlaySettingsPanel, showOverlay);
		setEnabledRecursive(mumbleDirectoryPanel, showOverlay);
		setEnabledRecursive(mumbleOverlayFpsPanel, showOverlay);
	}

}

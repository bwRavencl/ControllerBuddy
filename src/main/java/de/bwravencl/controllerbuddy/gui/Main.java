/* Copyright (C) 2019  Matteo Hausner
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

import static de.bwravencl.controllerbuddy.gui.GuiUtils.invokeOnEventDispatchThreadIfRequired;
import static de.bwravencl.controllerbuddy.gui.GuiUtils.loadFrameLocation;
import static de.bwravencl.controllerbuddy.gui.GuiUtils.setEnabledRecursive;
import static org.lwjgl.glfw.GLFW.GLFW_DISCONNECTED;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_1;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_LAST;
import static org.lwjgl.glfw.GLFW.glfwGetGamepadName;
import static org.lwjgl.glfw.GLFW.glfwGetJoystickGUID;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwJoystickIsGamepad;
import static org.lwjgl.glfw.GLFW.glfwJoystickPresent;
import static org.lwjgl.glfw.GLFW.glfwSetJoystickCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.lwjgl.glfw.GLFWJoystickCallback;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.oracle.si.Singleton;
import com.oracle.si.Singleton.SingletonApp;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.WinDef.UINT;

import de.bwravencl.controllerbuddy.Version;
import de.bwravencl.controllerbuddy.gui.GuiUtils.FrameDragListener;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.OverlayAxis;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.json.ActionTypeAdapter;
import de.bwravencl.controllerbuddy.json.ModeAwareTypeAdapterFactory;
import de.bwravencl.controllerbuddy.output.ClientVJoyOutputThread;
import de.bwravencl.controllerbuddy.output.LocalVJoyOutputThread;
import de.bwravencl.controllerbuddy.output.OutputThread;
import de.bwravencl.controllerbuddy.output.ServerOutputThread;
import de.bwravencl.controllerbuddy.output.VJoyOutputThread;
import de.bwravencl.controllerbuddy.util.ResourceBundleUtil;

public final class Main implements SingletonApp {

	private class AddModeAction extends AbstractAction {

		private static final long serialVersionUID = -4881923833724315489L;

		private AddModeAction() {
			putValue(NAME, rb.getString("ADD_MODE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("ADD_MODE_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var mode = new Mode();
			input.getProfile().getModes().add(mode);

			setUnsavedChanges(true);
			updateModesPanel();
		}

	}

	private class ChangeVJoyDirectoryAction extends AbstractAction {

		private static final long serialVersionUID = -7672382299595684105L;

		private ChangeVJoyDirectoryAction() {
			putValue(NAME, rb.getString("CHANGE_VJOY_DIRECTORY_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("CHANGE_VJOY_DIRECTORY_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var vJoyDirectoryFileChooser = new JFileChooser(
					preferences.get(PREFERENCES_VJOY_DIRECTORY, VJoyOutputThread.getDefaultInstallationPath()));
			vJoyDirectoryFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (vJoyDirectoryFileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				final var path = vJoyDirectoryFileChooser.getSelectedFile().getAbsolutePath();
				final var file = new File(VJoyOutputThread.getLibraryFilePath(path));

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

		private static final long serialVersionUID = 3316770144012465987L;

		private final VirtualAxis virtualAxis;

		private DisplayIndicatorAction(final VirtualAxis virtualAxis) {
			this.virtualAxis = virtualAxis;

			putValue(NAME, rb.getString("DISPLAY_INDICATOR_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("DISPLAY_INDICATOR_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (((JCheckBox) e.getSource()).isSelected())
				input.getProfile().getVirtualAxisToOverlayAxisMap().put(virtualAxis, new OverlayAxis());
			else
				input.getProfile().getVirtualAxisToOverlayAxisMap().remove(virtualAxis);

			setUnsavedChanges(true);
			updateOverlayPanel();
		}

	}

	private class InvertIndicatorAction extends AbstractAction {

		private static final long serialVersionUID = 3316770144012465987L;

		private final VirtualAxis virtualAxis;

		private InvertIndicatorAction(final VirtualAxis virtualAxis) {
			this.virtualAxis = virtualAxis;

			putValue(NAME, rb.getString("INVERT_INDICATOR_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("INVERT_INDICATOR_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			input.getProfile().getVirtualAxisToOverlayAxisMap().get(virtualAxis).inverted = ((JCheckBox) e.getSource())
					.isSelected();

			setUnsavedChanges(true);
			updateOverlayPanel();
		}

	}

	private class NewAction extends AbstractAction {

		private static final long serialVersionUID = 5703987691203427504L;

		private NewAction() {
			putValue(NAME, rb.getString("NEW_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("NEW_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			newProfile();
		}

	}

	private class OpenAction extends AbstractAction {

		private static final long serialVersionUID = -8932510785275935297L;

		private OpenAction() {
			putValue(NAME, rb.getString("OPEN_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("OPEN_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
				loadProfile(fileChooser.getSelectedFile());
		}

	}

	private enum OutputType {
		NONE, LOCAL, CLIENT, SERVER
	}

	private static final class ProfileFileChooser extends JFileChooser {

		private static final long serialVersionUID = -4669170626378955605L;

		private ProfileFileChooser() {
			setFileFilter(new FileNameExtensionFilter(rb.getString("PROFILE_FILE_DESCRIPTION"),
					rb.getString("PROFILE_FILE_EXTENSION")));
			setSelectedFile(new File(rb.getString("PROFILE_FILE_SUFFIX")));
		}

		@Override
		public void approveSelection() {
			final var file = getSelectedFile();
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
	}

	private class QuitAction extends AbstractAction {

		private static final long serialVersionUID = 8952460723177800923L;

		private QuitAction() {
			putValue(NAME, rb.getString("QUIT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("QUIT_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			quit();
		}

	}

	private class RemoveModeAction extends AbstractAction {

		private static final long serialVersionUID = -1056071724769862582L;

		private final Mode mode;

		private RemoveModeAction(final Mode mode) {
			this.mode = mode;

			putValue(NAME, rb.getString("REMOVE_MODE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("REMOVE_MODE_ACTION_DESCRIPTION_PREFIX") + mode.getDescription()
					+ rb.getString("REMOVE_MODE_ACTION_DESCRIPTION_SUFFIX"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			input.getProfile().removeMode(input, mode);
			setUnsavedChanges(true);
			updateModesPanel();
		}

	}

	private class SaveAction extends AbstractAction {

		private static final long serialVersionUID = -8469921697479550983L;

		private SaveAction() {
			putValue(NAME, rb.getString("SAVE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("SAVE_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (currentFile != null)
				saveProfile(currentFile);
			else
				saveProfileAs();
		}

	}

	private class SaveAsAction extends AbstractAction {

		private static final long serialVersionUID = -8469921697479550983L;

		private SaveAsAction() {
			putValue(NAME, rb.getString("SAVE_AS_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("SAVE_AS_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			saveProfileAs();
		}

	}

	private class SelectControllerAction extends AbstractAction {

		private static final long serialVersionUID = -2043467156713598592L;

		private final int jid;

		private SelectControllerAction(final int jid) {
			this.jid = jid;

			final var name = glfwGetGamepadName(jid);
			putValue(NAME, name);
			putValue(SHORT_DESCRIPTION, rb.getString("SELECT_CONTROLLER_ACTION_DESCRIPTION_PREFIX") + name
					+ rb.getString("SELECT_CONTROLLER_ACTION_DESCRIPTION_SUFFIX"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			setSelectedJid(jid);
		}

	}

	private class SelectIndicatorColorAction extends AbstractAction {

		private static final long serialVersionUID = 3316770144012465987L;

		private final VirtualAxis virtualAxis;

		private SelectIndicatorColorAction(final VirtualAxis virtualAxis) {
			this.virtualAxis = virtualAxis;

			putValue(NAME, rb.getString("CHANGE_INDICATOR_COLOR_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("CHANGE_INDICATOR_COLOR_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var overlayAxis = input.getProfile().getVirtualAxisToOverlayAxisMap().get(virtualAxis);

			final var newColor = JColorChooser.showDialog(frame, "Choose Background Color", overlayAxis.color);
			if (newColor != null)
				overlayAxis.color = newColor;

			setUnsavedChanges(true);
			updateOverlayPanel();
		}

	}

	private class SetHostAction extends AbstractAction implements FocusListener {

		private static final long serialVersionUID = -7674562782751876814L;

		private final JTextField hostTextField;

		private SetHostAction(final JTextField hostTextField) {
			this.hostTextField = hostTextField;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			setHost();
		}

		@Override
		public void focusGained(final FocusEvent e) {
		}

		@Override
		public void focusLost(final FocusEvent e) {
			setHost();
		}

		private void setHost() {
			final var host = hostTextField.getText();

			if (host != null && host.length() > 0)
				preferences.put(PREFERENCES_HOST, host);
			else
				hostTextField.setText(preferences.get(PREFERENCES_HOST, ClientVJoyOutputThread.DEFAULT_HOST));
		}

	}

	private class SetModeDescriptionAction extends AbstractAction implements DocumentListener {

		private static final long serialVersionUID = -6706537047137827688L;

		private final Mode mode;
		private final JTextField modeDescriptionTextField;

		private SetModeDescriptionAction(final Mode mode, final JTextField modeDescriptionTextField) {
			this.mode = mode;
			this.modeDescriptionTextField = modeDescriptionTextField;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			setModeDescription();
		}

		@Override
		public void changedUpdate(final DocumentEvent e) {
			setModeDescription();
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			setModeDescription();
		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			setModeDescription();
		}

		private void setModeDescription() {
			final var description = modeDescriptionTextField.getText();

			if (description != null && description.length() > 0) {
				mode.setDescription(description);
				setUnsavedChanges(true);
			}
		}

	}

	private class ShowAboutDialogAction extends AbstractAction {

		private static final long serialVersionUID = -2578971543384483382L;

		private ShowAboutDialogAction() {
			putValue(NAME, rb.getString("SHOW_ABOUT_DIALOG_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("SHOW_ABOUT_DIALOG_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var icon = new ImageIcon(Main.class.getResource(Main.ICON_RESOURCE_PATHS[2]));
			JOptionPane.showMessageDialog(frame,
					rb.getString("ABOUT_DIALOG_TEXT_PREFIX") + Version.getVersion()
							+ rb.getString("ABOUT_DIALOG_TEXT_SUFFIX"),
					(String) getValue(NAME), JOptionPane.INFORMATION_MESSAGE, icon);
		}

	}

	private class ShowAction extends AbstractAction {

		private static final long serialVersionUID = 8578159622754054457L;

		private ShowAction() {
			putValue(NAME, rb.getString("SHOW_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("SHOW_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var openEvent = new WindowEvent(frame, WindowEvent.WINDOW_OPENED);
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(openEvent);
			frame.setVisible(true);
			frame.setExtendedState(Frame.NORMAL);
		}

	}

	private class StartClientAction extends AbstractAction {

		private static final long serialVersionUID = 3975574941559749481L;

		private StartClientAction() {
			putValue(NAME, rb.getString("START_CLIENT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("START_CLIENT_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			startClient();
		}

	}

	private class StartLocalAction extends AbstractAction {

		private static final long serialVersionUID = -2003502124995392039L;

		private StartLocalAction() {
			putValue(NAME, rb.getString("START_LOCAL_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("START_LOCAL_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			startLocal();
		}

	}

	private class StartServerAction extends AbstractAction {

		private static final long serialVersionUID = 1758447420975631146L;

		private StartServerAction() {
			putValue(NAME, rb.getString("START_SERVER_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("START_SERVER_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			startServer();
		}

	}

	private class StopClientAction extends AbstractAction {

		private static final long serialVersionUID = -2863419586328503426L;

		private StopClientAction() {
			putValue(NAME, rb.getString("STOP_CLIENT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("STOP_CLIENT_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			stopClient(true);
		}

	}

	private class StopLocalAction extends AbstractAction {

		private static final long serialVersionUID = -4859431944733030332L;

		private StopLocalAction() {
			putValue(NAME, rb.getString("STOP_LOCAL_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("STOP_LOCAL_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			stopLocal(true);
		}

	}

	private class StopServerAction extends AbstractAction {

		private static final long serialVersionUID = 6023207463370122769L;

		private StopServerAction() {
			putValue(NAME, rb.getString("STOP_SERVER_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("STOP_SERVER_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			stopServer(true);
		}

	}

	static {
		try {
			UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
		} catch (final UnsupportedLookAndFeelException e) {
			throw new RuntimeException(e);
		}
	}

	private static final Logger log = System.getLogger(Main.class.getName());
	public static final boolean windows = Platform.isWindows() && !Platform.isWindowsCE();
	private static final String SINGLETON_ID = Version.class.getPackageName();
	public static final String STRING_RESOURCE_BUNDLE_BASENAME = "strings";
	private static final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(STRING_RESOURCE_BUNDLE_BASENAME,
			Locale.getDefault());
	static final int DIALOG_BOUNDS_X = 100;
	static final int DIALOG_BOUNDS_Y = 100;
	static final int DIALOG_BOUNDS_WIDTH = 930;
	static final int DIALOG_BOUNDS_HEIGHT = 640;
	static final int DIALOG_BOUNDS_X_Y_OFFSET = 25;
	static final Dimension BUTTON_DIMENSION = new Dimension(110, 25);
	private static final String OPTION_AUTOSTART = "autostart";
	private static final String OPTION_TRAY = "tray";
	private static final String OPTION_VERSION = "version";
	private static final String OPTION_AUTOSTART_VALUE_LOCAL = "local";
	private static final String OPTION_AUTOSTART_VALUE_CLIENT = "client";
	private static final String OPTION_AUTOSTART_VALUE_SERVER = "server";
	private static final String PREFERENCES_POLL_INTERVAL = "poll_interval";
	private static final String PREFERENCES_LAST_CONTROLLER = "last_controller";
	private static final String PREFERENCES_LAST_PROFILE = "last_profile";
	public static final String PREFERENCES_VJOY_DIRECTORY = "vjoy_directory";
	private static final String PREFERENCES_VJOY_DEVICE = "vjoy_device";
	private static final String PREFERENCES_HOST = "host";
	private static final String PREFERENCES_PORT = "port";
	private static final String PREFERENCES_TIMEOUT = "timeout";
	private static final String PREFERENCES_SHOW_OVERLAY = "show_overlay";
	private static final String PREFERENCES_SHOW_VR_OVERLAY = "show_vr_overlay";
	private static final String PREFERENCES_PREVENT_POWER_SAVE_MODE = "prevent_power_save_mode";
	private static final long OVERLAY_POSITION_UPDATE_INTERVAL = 10000L;
	private static final String[] ICON_RESOURCE_PATHS = { "/icon_16.png", "/icon_32.png", "/icon_64.png",
			"/icon_128.png" };
	static final Color TRANSPARENT = new Color(255, 255, 255, 0);
	private static final int INVALID_JID = GLFW_JOYSTICK_1 - 1;

	private static int getExtendedKeyCodeForMenu(final AbstractButton button,
			final Set<Integer> alreadyAssignedKeyCodes) {
		var keyCode = KeyEvent.VK_UNDEFINED;

		final var text = button.getText();
		if (text != null && text.length() > 0) {
			var index = 0;
			do {
				keyCode = KeyEvent.getExtendedKeyCodeForChar(text.charAt(index));
				index++;
			} while (keyCode != KeyEvent.VK_UNDEFINED && !alreadyAssignedKeyCodes.add(keyCode));
		}

		return keyCode;
	}

	private static int getExtendedKeyCodeForMenuItem(final AbstractButton button) {
		final var action = button.getAction();
		if (action != null)
			if (action instanceof NewAction)
				return KeyEvent.VK_N;
			else if (action instanceof OpenAction)
				return KeyEvent.VK_O;
			else if (action instanceof SaveAction)
				return KeyEvent.VK_S;
			else if (action instanceof StartLocalAction || action instanceof StopLocalAction)
				return KeyEvent.VK_L;
			else if (action instanceof StartClientAction || action instanceof StopClientAction)
				return KeyEvent.VK_C;
			else if (action instanceof StartServerAction || action instanceof StopServerAction)
				return KeyEvent.VK_E;

		return KeyEvent.VK_UNDEFINED;
	}

	private static boolean isModalDialogShowing() {
		final var windows = Window.getWindows();
		if (windows != null)
			for (final Window w : windows)
				if (w.isShowing() && w instanceof Dialog && ((Dialog) w).isModal())
					return true;

		return false;
	}

	public static void main(final String[] args) {
		if (!Singleton.invoke(SINGLETON_ID, args))
			SwingUtilities.invokeLater(() -> {
				final var options = new Options();
				options.addOption(OPTION_AUTOSTART, true, rb.getString(
						Main.windows ? "AUTOSTART_OPTION_DESCRIPTION_WINDOWS" : "AUTOSTART_OPTION_DESCRIPTION"));
				options.addOption(OPTION_TRAY, false, rb.getString("TRAY_OPTION_DESCRIPTION"));
				options.addOption(OPTION_VERSION, false, rb.getString("VERSION_OPTION_DESCRIPTION"));

				try {
					final CommandLine commandLine = new DefaultParser().parse(options, args);
					if (commandLine.hasOption(OPTION_VERSION))
						System.out.println(rb.getString("APPLICATION_NAME") + " " + Version.getVersion());
					else {
						final var main = new Main();
						if (!commandLine.hasOption(OPTION_TRAY))
							main.frame.setVisible(true);
						if (commandLine.hasOption(OPTION_AUTOSTART)) {
							final var optionValue = commandLine.getOptionValue(OPTION_AUTOSTART);

							if (Main.windows)
								if (OPTION_AUTOSTART_VALUE_LOCAL.equals(optionValue)) {
									main.startLocal();
									return;
								} else if (OPTION_AUTOSTART_VALUE_CLIENT.equals(optionValue)) {
									main.startClient();
									return;
								}
							if (OPTION_AUTOSTART_VALUE_SERVER.equals(optionValue))
								main.startServer();
							else
								JOptionPane.showMessageDialog(main.frame,
										rb.getString("INVALID_VALUE_FOR_OPTION_AUTOSTART") + optionValue,
										rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
						}
					}
				} catch (final ParseException e) {
					final var helpFormatter = new HelpFormatter();
					helpFormatter.printHelp(rb.getString("APPLICATION_NAME"), options, true);
				}
			});
	}

	private static void waitForThreadToFinish(final Thread thread) {
		while (thread != null && thread.isAlive())
			try {
				Thread.sleep(100L);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
	}

	private final Preferences preferences = Preferences.userNodeForPackage(Version.class);
	private final Map<VirtualAxis, JProgressBar> virtualAxisToProgressBarMap = new HashMap<>();
	private volatile LocalVJoyOutputThread localThread;
	private volatile ClientVJoyOutputThread clientThread;
	private volatile ServerOutputThread serverThread;
	private int selectedJid = INVALID_JID;
	private Input input;
	private OutputType lastOutputType = OutputType.NONE;
	private final JFrame frame;
	private final OpenAction openAction = new OpenAction();
	private final JMenuBar menuBar = new JMenuBar();
	private final JMenu fileMenu = new JMenu(rb.getString("FILE_MENU"));
	private final JMenu deviceMenu = new JMenu(rb.getString("DEVICE_MENU"));
	private final JMenu localMenu = new JMenu(rb.getString("LOCAL_MENU"));
	private final JMenu clientMenu = new JMenu(rb.getString("CLIENT_MENU"));
	private final JMenu serverMenu = new JMenu(rb.getString("SERVER_MENU"));
	private final JMenuItem newMenuItem = fileMenu.add(new NewAction());
	private final JMenuItem openMenuItem = fileMenu.add(openAction);
	private final JMenuItem saveMenuItem = fileMenu.add(new SaveAction());
	private final JMenuItem saveAsMenuItem = fileMenu.add(new SaveAsAction());
	private JRadioButtonMenuItem startLocalRadioButtonMenuItem;
	private JRadioButtonMenuItem stopLocalRadioButtonMenuItem;
	private JRadioButtonMenuItem startClientRadioButtonMenuItem;
	private JRadioButtonMenuItem stopClientRadioButtonMenuItem;
	private final JRadioButtonMenuItem startServerRadioButtonMenuItem;
	private final JRadioButtonMenuItem stopServerRadioButtonMenuItem;
	private MenuItem showMenuItem;
	private final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
	private JPanel modesPanel;
	private JScrollPane modesScrollPane;
	private JPanel modesListPanel;
	private JPanel addModePanel;
	private JPanel overlayPanel;
	private AssignmentsComponent assignmentsComponent;
	private final JScrollPane settingsScrollPane = new JScrollPane();
	private final JPanel settingsPanel;
	private JScrollPane indicatorsScrollPane;
	private JPanel indicatorsListPanel;
	private TimerTask overlayTimerTask;
	private JLabel vJoyDirectoryLabel1;
	private JTextField hostTextField;
	private final JLabel statusLabel = new JLabel(rb.getString("STATUS_READY"));
	private TrayIcon trayIcon;
	private boolean unsavedChanges = false;
	private String loadedProfile = null;
	private File currentFile;
	private ServerSocket serverSocket;
	private volatile boolean scheduleOnScreenKeyboardModeSwitch;
	private final JLabel labelCurrentMode = new JLabel();
	private final JFileChooser fileChooser = new ProfileFileChooser();
	private final Timer timer = new Timer();
	private volatile OpenVrOverlay openVrOverlay;
	private FrameDragListener overlayFrameDragListener;
	private FlowLayout indicatorPanelFlowLayout;
	private JPanel indicatorPanel;
	private Rectangle prevMaxWindowBounds;
	private volatile JFrame overlayFrame;

	private final OnScreenKeyboard onScreenKeyboard = new OnScreenKeyboard(this);

	private Main() {
		Singleton.start(this, SINGLETON_ID);

		frame = new JFrame();
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) {
				super.windowClosing(e);

				if (showMenuItem != null)
					showMenuItem.setEnabled(true);
			}

			@Override
			public void windowDeiconified(final WindowEvent e) {
				super.windowDeiconified(e);

				if (showMenuItem != null)
					showMenuItem.setEnabled(false);
			}

			@Override
			public void windowIconified(final WindowEvent e) {
				super.windowIconified(e);

				if (showMenuItem != null)
					showMenuItem.setEnabled(true);
			}

			@Override
			public void windowOpened(final WindowEvent e) {
				super.windowOpened(e);

				if (showMenuItem != null)
					showMenuItem.setEnabled(false);
			}

		});

		frame.setBounds(DIALOG_BOUNDS_X, DIALOG_BOUNDS_Y, DIALOG_BOUNDS_WIDTH, DIALOG_BOUNDS_HEIGHT);
		frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		final var icons = new ArrayList<Image>();
		for (final var path : ICON_RESOURCE_PATHS) {
			final var icon = new ImageIcon(Main.class.getResource(path));
			icons.add(icon.getImage());
		}
		frame.setIconImages(icons);

		frame.setJMenuBar(menuBar);

		menuBar.add(fileMenu);
		final QuitAction quitAction = new QuitAction();
		fileMenu.add(quitAction);
		menuBar.add(deviceMenu);

		if (windows) {
			menuBar.add(localMenu, 2);

			final var buttonGroupLocalState = new ButtonGroup();
			startLocalRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("START_MENU_ITEM"));
			startLocalRadioButtonMenuItem.setAction(new StartLocalAction());
			buttonGroupLocalState.add(startLocalRadioButtonMenuItem);
			localMenu.add(startLocalRadioButtonMenuItem);

			stopLocalRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("STOP_MENU_ITEM"));
			stopLocalRadioButtonMenuItem.setAction(new StopLocalAction());
			buttonGroupLocalState.add(stopLocalRadioButtonMenuItem);
			localMenu.add(stopLocalRadioButtonMenuItem);

			menuBar.add(clientMenu);

			final var buttonGroupClientState = new ButtonGroup();

			startClientRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("START_MENU_ITEM"));
			startClientRadioButtonMenuItem.setAction(new StartClientAction());
			buttonGroupClientState.add(startClientRadioButtonMenuItem);
			clientMenu.add(startClientRadioButtonMenuItem);

			stopClientRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("STOP_MENU_ITEM"));
			stopClientRadioButtonMenuItem.setAction(new StopClientAction());
			buttonGroupClientState.add(stopClientRadioButtonMenuItem);
			clientMenu.add(stopClientRadioButtonMenuItem);
		}

		final var buttonGroupServerState = new ButtonGroup();
		startServerRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("START_MENU_ITEM"));
		startServerRadioButtonMenuItem.setAction(new StartServerAction());
		buttonGroupServerState.add(startServerRadioButtonMenuItem);
		serverMenu.add(startServerRadioButtonMenuItem);

		stopServerRadioButtonMenuItem = new JRadioButtonMenuItem(rb.getString("STOP_MENU_ITEM"));
		stopServerRadioButtonMenuItem.setAction(new StopServerAction());
		buttonGroupServerState.add(stopServerRadioButtonMenuItem);
		serverMenu.add(stopServerRadioButtonMenuItem);

		final var helpMenu = new JMenu(rb.getString("HELP_MENU"));
		menuBar.add(helpMenu);
		helpMenu.add(new ShowAboutDialogAction());

		frame.getContentPane().add(tabbedPane);

		settingsPanel = new JPanel();
		settingsPanel.setLayout(new GridBagLayout());

		settingsScrollPane.setViewportView(settingsPanel);
		tabbedPane.addTab(rb.getString("SETTINGS_TAB"), null, settingsScrollPane);

		final var panelGridBagConstraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 5);

		final var panelFlowLayout = new FlowLayout(FlowLayout.LEADING, 10, 10);

		final var pollIntervalPanel = new JPanel(panelFlowLayout);
		settingsPanel.add(pollIntervalPanel, panelGridBagConstraints);

		final var pollIntervalLabel = new JLabel(rb.getString("POLL_INTERVAL_LABEL"));
		pollIntervalLabel.setPreferredSize(new Dimension(120, 15));
		pollIntervalPanel.add(pollIntervalLabel);

		final var pollIntervalSpinner = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_POLL_INTERVAL, OutputThread.DEFAULT_POLL_INTERVAL), 10, 500, 1));
		final JSpinner.DefaultEditor pollIntervalSpinnerEditor = new JSpinner.NumberEditor(pollIntervalSpinner, "#");
		((DefaultFormatter) pollIntervalSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		pollIntervalSpinner.setEditor(pollIntervalSpinnerEditor);
		pollIntervalSpinner.addChangeListener(
				e -> preferences.putInt(PREFERENCES_POLL_INTERVAL, (int) ((JSpinner) e.getSource()).getValue()));
		pollIntervalPanel.add(pollIntervalSpinner);

		if (windows) {
			final var vJoyDirectoryPanel = new JPanel(panelFlowLayout);
			settingsPanel.add(vJoyDirectoryPanel, panelGridBagConstraints);

			final var vJoyDirectoryLabel = new JLabel(rb.getString("VJOY_DIRECTORY_LABEL"));
			vJoyDirectoryLabel.setPreferredSize(new Dimension(120, 15));
			vJoyDirectoryPanel.add(vJoyDirectoryLabel);

			vJoyDirectoryLabel1 = new JLabel(
					preferences.get(PREFERENCES_VJOY_DIRECTORY, VJoyOutputThread.getDefaultInstallationPath()));
			vJoyDirectoryPanel.add(vJoyDirectoryLabel1);

			final var vJoyDirectoryButton = new JButton(new ChangeVJoyDirectoryAction());
			vJoyDirectoryPanel.add(vJoyDirectoryButton);

			final var vJoyDevicePanel = new JPanel(panelFlowLayout);
			settingsPanel.add(vJoyDevicePanel, panelGridBagConstraints);

			final var vJoyDeviceLabel = new JLabel(rb.getString("VJOY_DEVICE_LABEL"));
			vJoyDeviceLabel.setPreferredSize(new Dimension(120, 15));
			vJoyDevicePanel.add(vJoyDeviceLabel);

			final var vJoyDeviceSpinner = new JSpinner(new SpinnerNumberModel(
					preferences.getInt(PREFERENCES_VJOY_DEVICE, VJoyOutputThread.DEFAULT_VJOY_DEVICE), 1, 16, 1));
			final JSpinner.DefaultEditor vJoyDeviceSpinnerEditor = new JSpinner.NumberEditor(vJoyDeviceSpinner, "#");
			((DefaultFormatter) vJoyDeviceSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
			vJoyDeviceSpinner.setEditor(vJoyDeviceSpinnerEditor);
			vJoyDeviceSpinner.addChangeListener(
					e -> preferences.putInt(PREFERENCES_VJOY_DEVICE, (int) ((JSpinner) e.getSource()).getValue()));
			vJoyDevicePanel.add(vJoyDeviceSpinner);

			final var hostPanel = new JPanel(panelFlowLayout);
			settingsPanel.add(hostPanel, panelGridBagConstraints);

			final var hostLabel = new JLabel(rb.getString("HOST_LABEL"));
			hostLabel.setPreferredSize(new Dimension(120, 15));
			hostPanel.add(hostLabel);

			hostTextField = new JTextField(preferences.get(PREFERENCES_HOST, ClientVJoyOutputThread.DEFAULT_HOST), 10);
			final var setHostAction = new SetHostAction(hostTextField);
			hostTextField.addActionListener(setHostAction);
			hostTextField.addFocusListener(setHostAction);
			hostPanel.add(hostTextField);
		}

		final var portPanel = new JPanel(panelFlowLayout);
		settingsPanel.add(portPanel, panelGridBagConstraints);

		final var portLabel = new JLabel(rb.getString("PORT_LABEL"));
		portLabel.setPreferredSize(new Dimension(120, 15));
		portPanel.add(portLabel);

		final var portSpinner = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_PORT, ServerOutputThread.DEFAULT_PORT), 1024, 65535, 1));
		final JSpinner.DefaultEditor portSpinnerEditor = new JSpinner.NumberEditor(portSpinner, "#");
		((DefaultFormatter) portSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		portSpinner.setEditor(portSpinnerEditor);
		portSpinner.addChangeListener(
				e -> preferences.putInt(PREFERENCES_PORT, (int) ((JSpinner) e.getSource()).getValue()));
		portPanel.add(portSpinner);

		final var timeoutPanel = new JPanel(panelFlowLayout);
		settingsPanel.add(timeoutPanel, panelGridBagConstraints);

		final var timeoutLabel = new JLabel(rb.getString("TIMEOUT_LABEL"));
		timeoutLabel.setPreferredSize(new Dimension(120, 15));
		timeoutPanel.add(timeoutLabel);

		final var timeoutSpinner = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_TIMEOUT, ServerOutputThread.DEFAULT_TIMEOUT), 10, 60000, 1));
		final JSpinner.DefaultEditor timeoutSpinnerEditor = new JSpinner.NumberEditor(timeoutSpinner, "#");
		((DefaultFormatter) timeoutSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		timeoutSpinner.setEditor(timeoutSpinnerEditor);
		timeoutSpinner.addChangeListener(
				e -> preferences.putInt(PREFERENCES_TIMEOUT, (int) ((JSpinner) e.getSource()).getValue()));
		timeoutPanel.add(timeoutSpinner);

		final var alwaysOnTopSupported = Toolkit.getDefaultToolkit().isAlwaysOnTopSupported();
		if (alwaysOnTopSupported || preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, alwaysOnTopSupported)) {
			final var overlaySettingsPanel = new JPanel(panelFlowLayout);
			settingsPanel.add(overlaySettingsPanel, panelGridBagConstraints);

			final var overlayLabel = new JLabel(rb.getString("OVERLAY_LABEL"));
			overlayLabel.setPreferredSize(new Dimension(120, 15));
			overlaySettingsPanel.add(overlayLabel);

			final var showOverlayCheckBox = new JCheckBox(rb.getString("SHOW_OVERLAY_CHECK_BOX"));
			showOverlayCheckBox.setSelected(preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, true));
			showOverlayCheckBox.addActionListener(e -> {
				final boolean showOverlay = ((JCheckBox) e.getSource()).isSelected();

				preferences.putBoolean(PREFERENCES_SHOW_OVERLAY, showOverlay);
			});
			overlaySettingsPanel.add(showOverlayCheckBox);
		}

		if (windows) {
			if (preferences.getBoolean(PREFERENCES_SHOW_VR_OVERLAY, true)) {
				final var vrOverlaySettingsPanel = new JPanel(panelFlowLayout);
				settingsPanel.add(vrOverlaySettingsPanel, panelGridBagConstraints);

				final var vrOverlayLabel = new JLabel(rb.getString("VR_OVERLAY_LABEL"));
				vrOverlayLabel.setPreferredSize(new Dimension(120, 15));
				vrOverlaySettingsPanel.add(vrOverlayLabel);

				final var showVrOverlayCheckBox = new JCheckBox(rb.getString("SHOW_VR_OVERLAY_CHECK_BOX"));
				showVrOverlayCheckBox.setSelected(preferences.getBoolean(PREFERENCES_SHOW_VR_OVERLAY, true));
				showVrOverlayCheckBox.addActionListener(e -> {
					final var showVrOverlay = ((JCheckBox) e.getSource()).isSelected();

					preferences.putBoolean(PREFERENCES_SHOW_VR_OVERLAY, showVrOverlay);
				});
				vrOverlaySettingsPanel.add(showVrOverlayCheckBox);
			}

			final var preventPowerSaveModeSettingsPanel = new JPanel(panelFlowLayout);
			settingsPanel.add(preventPowerSaveModeSettingsPanel, panelGridBagConstraints);

			final var preventPowerSaveModeLabel = new JLabel(rb.getString("POWER_SAVE_MODE_LABEL"));
			preventPowerSaveModeLabel.setPreferredSize(new Dimension(120, 15));
			preventPowerSaveModeSettingsPanel.add(preventPowerSaveModeLabel);

			final var preventPowerSaveModeCheckBox = new JCheckBox(rb.getString("PREVENT_POWER_SAVE_MODE_CHECK_BOX"));
			preventPowerSaveModeCheckBox.setSelected(preferences.getBoolean(PREFERENCES_PREVENT_POWER_SAVE_MODE, true));
			preventPowerSaveModeCheckBox.addActionListener(e -> {
				final var preventPowerSaveMode = ((JCheckBox) e.getSource()).isSelected();

				preferences.putBoolean(PREFERENCES_PREVENT_POWER_SAVE_MODE, preventPowerSaveMode);
			});
			preventPowerSaveModeSettingsPanel.add(preventPowerSaveModeCheckBox);
		}

		if (SystemTray.isSupported()) {
			final var popupMenu = new PopupMenu();

			final var showAction = new ShowAction();
			showMenuItem = new MenuItem((String) showAction.getValue(Action.NAME));
			showMenuItem.addActionListener(showAction);
			popupMenu.add(showMenuItem);

			popupMenu.addSeparator();

			final var openMenuItem = new MenuItem((String) openAction.getValue(Action.NAME));
			openMenuItem.addActionListener(openAction);
			popupMenu.add(openMenuItem);

			popupMenu.addSeparator();

			final var quitMenuItem = new MenuItem((String) quitAction.getValue(Action.NAME));
			quitMenuItem.addActionListener(quitAction);
			popupMenu.add(quitMenuItem);

			trayIcon = new TrayIcon(frame.getIconImage());
			trayIcon.addActionListener(showAction);
			trayIcon.setPopupMenu(popupMenu);
			try {
				SystemTray.getSystemTray().add(trayIcon);
			} catch (final AWTException e) {
				log.log(Logger.Level.ERROR, e.getMessage(), e);
			}
		}

		updateTitleAndTooltip();

		settingsPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		final var outsideBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		final var insideBorder = BorderFactory.createEmptyBorder(0, 5, 0, 5);
		statusLabel.setBorder(BorderFactory.createCompoundBorder(outsideBorder, insideBorder));
		frame.add(statusLabel, BorderLayout.SOUTH);

		final var glfwInitialized = glfwInit();
		if (!glfwInitialized)
			if (windows)
				JOptionPane.showMessageDialog(frame, rb.getString("COULD_NOT_INITIALIZE_GLFW_DIALOG_TEXT_WINDOWS"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			else {
				JOptionPane.showMessageDialog(frame, rb.getString("COULD_NOT_INITIALIZE_GLFW_DIALOG_TEXT"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				quit();
			}

		final var presentJids = new HashSet<Integer>();
		for (var jid = GLFW_JOYSTICK_1; jid <= GLFW_JOYSTICK_LAST; jid++)
			if (glfwJoystickPresent(jid) && glfwJoystickIsGamepad(jid))
				presentJids.add(jid);

		final var lastControllerGuid = preferences.get(PREFERENCES_LAST_CONTROLLER, null);
		for (final var jid : presentJids) {
			final var lastControllerFound = lastControllerGuid != null
					? lastControllerGuid.equals(glfwGetJoystickGUID(jid))
					: false;

			if (!isSelectedJidValid() || lastControllerFound)
				selectedJid = jid;

			if (lastControllerFound)
				break;
		}

		newProfile();

		onControllersChanged(true);

		glfwSetJoystickCallback(new GLFWJoystickCallback() {

			@Override
			public void invoke(final int jid, final int event) {
				final var disconnected = event == GLFW_DISCONNECTED;
				if (disconnected || glfwJoystickIsGamepad(jid)) {
					if (disconnected && selectedJid == jid)
						selectedJid = INVALID_JID;

					invokeOnEventDispatchThreadIfRequired(() -> onControllersChanged(false));
				}

			}
		});

		if (glfwInitialized && presentJids.isEmpty()) {
			if (windows)
				JOptionPane.showMessageDialog(frame, rb.getString("NO_CONTROLLER_CONNECTED_DIALOG_TEXT_WINDOWS"),
						rb.getString("INFORMATION_DIALOG_TITLE"), JOptionPane.INFORMATION_MESSAGE);
			else
				JOptionPane.showMessageDialog(frame, rb.getString("NO_CONTROLLER_CONNECTED_DIALOG_TEXT"),
						rb.getString("INFORMATION_DIALOG_TITLE"), JOptionPane.INFORMATION_MESSAGE);
		} else {
			final String path = preferences.get(PREFERENCES_LAST_PROFILE, null);
			if (path != null)
				loadProfile(new File(path));
		}
	}

	private void deInitOverlay() {
		if (openVrOverlay != null) {
			openVrOverlay.stop();
			openVrOverlay = null;
		}

		if (overlayFrame != null) {
			overlayFrame.dispose();
			overlayFrame = null;
		}

		virtualAxisToProgressBarMap.clear();

		onScreenKeyboard.setVisible(false);
	}

	public void displayChargingStateInfo(final boolean charging) {
		if (trayIcon != null && input != null)
			trayIcon.displayMessage(rb.getString("CHARGING_STATE_CAPTION"),
					(charging ? rb.getString("CHARGING_STATE_CHARGING_PREFIX")
							: rb.getString("CHARGING_STATE_DISCHARGING_PREFIX")) + input.getBatteryState()
							+ rb.getString("CHARGING_STATE_SUFFIX"),
					MessageType.INFO);
	}

	public void displayLowBatteryWarning(final int batteryCharge) {
		SwingUtilities.invokeLater(() -> {
			if (trayIcon != null)
				trayIcon.displayMessage(rb.getString("LOW_BATTERY_CAPTION"), batteryCharge + "%", MessageType.WARNING);
		});
	}

	public JFrame getFrame() {
		return frame;
	}

	Input getInput() {
		return input;
	}

	public OnScreenKeyboard getOnScreenKeyboard() {
		return onScreenKeyboard;
	}

	JFrame getOverlayFrame() {
		return overlayFrame;
	}

	public Preferences getPreferences() {
		return preferences;
	}

	public Timer getTimer() {
		return timer;
	}

	public void handleOnScreenKeyboardModeChange() {
		if (scheduleOnScreenKeyboardModeSwitch) {
			for (final var buttonToModeActions : input.getProfile().getButtonToModeActionsMap().values())
				for (final var buttonToModeAction : buttonToModeActions)
					if (OnScreenKeyboard.onScreenKeyboardMode.equals(buttonToModeAction.getMode(input))) {
						buttonToModeAction.doAction(input, Byte.MAX_VALUE);
						break;
					}

			scheduleOnScreenKeyboardModeSwitch = false;
		}
	}

	private void initOverlay() {
		if (!preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, Toolkit.getDefaultToolkit().isAlwaysOnTopSupported()))
			return;

		var longestDescription = "";
		for (final var mode : input.getProfile().getModes()) {
			final var description = mode.getDescription();
			if (description.length() > longestDescription.length())
				longestDescription = description;
		}

		final var fontMetrics = labelCurrentMode.getFontMetrics(labelCurrentMode.getFont());
		labelCurrentMode
				.setPreferredSize(new Dimension(fontMetrics.stringWidth(longestDescription), fontMetrics.getHeight()));
		labelCurrentMode.setForeground(Color.RED);
		labelCurrentMode.setText(input.getProfile().getActiveMode().getDescription());

		overlayFrame = new JFrame("Overlay");
		overlayFrame.setType(JFrame.Type.UTILITY);
		overlayFrame.setLayout(new BorderLayout());
		overlayFrame.setFocusableWindowState(false);
		overlayFrame.setUndecorated(true);
		overlayFrame.setBackground(TRANSPARENT);

		overlayFrame.add(labelCurrentMode, BorderLayout.PAGE_END);
		overlayFrame.setAlwaysOnTop(true);

		indicatorPanelFlowLayout = new FlowLayout();
		indicatorPanel = new JPanel(indicatorPanelFlowLayout);
		indicatorPanel.setBackground(TRANSPARENT);

		final var virtualAxisToOverlayAxisMap = input.getProfile().getVirtualAxisToOverlayAxisMap();
		for (final var virtualAxis : Input.VirtualAxis.values()) {
			final var overlayAxis = virtualAxisToOverlayAxisMap.get(virtualAxis);
			if (overlayAxis != null) {
				final var progressBar = new JProgressBar(SwingConstants.VERTICAL) {

					private static final long serialVersionUID = 8167193907929992395L;

					@Override
					public void setMaximum(final int n) {
						if (overlayAxis.inverted)
							super.setMinimum(-n);
						else
							super.setMaximum(n);
					}

					@Override
					public void setMinimum(final int n) {
						if (overlayAxis.inverted)
							super.setMaximum(-n);
						else
							super.setMinimum(n);
					}

					@Override
					public void setValue(final int n) {
						super.setValue(overlayAxis.inverted ? -n : n);
					}
				};
				progressBar.setPreferredSize(new Dimension(21, 149));
				progressBar.setBorder(
						BorderFactory.createDashedBorder(Color.BLACK, (float) progressBar.getPreferredSize().getWidth(),
								(float) progressBar.getPreferredSize().getWidth()));
				progressBar.setBackground(Color.LIGHT_GRAY);
				progressBar.setForeground(overlayAxis.color);
				progressBar.setValue(1);
				indicatorPanel.add(progressBar);
				virtualAxisToProgressBarMap.put(virtualAxis, progressBar);
			}
		}

		overlayFrame.add(indicatorPanel);

		overlayFrameDragListener = new FrameDragListener(this, overlayFrame) {

			@Override
			public void mouseDragged(final MouseEvent e) {
				super.mouseDragged(e);
				final var maxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
				updateOverlayAlignment(maxWindowBounds);
			}

		};
		overlayFrame.addMouseListener(overlayFrameDragListener);
		overlayFrame.addMouseMotionListener(overlayFrameDragListener);

		prevMaxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		updateOverlayLocation(prevMaxWindowBounds);

		overlayFrame.setVisible(true);
	}

	private void initVrOverlay() {
		if (!windows || !preferences.getBoolean(PREFERENCES_SHOW_VR_OVERLAY, true))
			return;

		try {
			openVrOverlay = new OpenVrOverlay(this);
		} catch (final Exception e) {
			openVrOverlay = null;
		}
	}

	private boolean isSelectedJidValid() {
		return selectedJid >= GLFW_JOYSTICK_1 && selectedJid <= GLFW_JOYSTICK_LAST;
	}

	private void loadProfile(final File file) {
		stopAll();

		var profileLoaded = false;

		try {
			final var jsonString = Files.readString(file.toPath());
			final var actionAdapter = new ActionTypeAdapter();
			final var gson = new GsonBuilder().registerTypeAdapterFactory(new ModeAwareTypeAdapterFactory())
					.registerTypeAdapter(IAction.class, actionAdapter).create();

			try {
				final var profile = gson.fromJson(jsonString, Profile.class);

				final var unknownActionClasses = actionAdapter.getUnknownActionClasses();
				if (!unknownActionClasses.isEmpty())
					JOptionPane.showMessageDialog(frame,
							rb.getString("UNKNOWN_ACTION_TYPES_DIALOG_TEXT") + String.join("\n", unknownActionClasses),
							rb.getString("WARNING_DIALOG_TITLE"), JOptionPane.WARNING_MESSAGE);

				profileLoaded = input.setProfile(profile, input.getJid());
				if (profileLoaded) {
					saveLastProfile(file);
					updateModesPanel();
					updateOverlayPanel();
					loadedProfile = file.getName();
					setUnsavedChanges(false);
					setStatusBarText(rb.getString("STATUS_PROFILE_LOADED") + file.getAbsolutePath());
					scheduleStatusBarText(rb.getString("STATUS_READY"));
					fileChooser.setSelectedFile(file);

					restartLast();
				}
			} catch (final JsonParseException e) {
				log.log(Logger.Level.ERROR, e.getMessage(), e);
			}
		} catch (final IOException e) {
			log.log(Logger.Level.ERROR, e.getMessage(), e);
		}

		if (!profileLoaded)
			JOptionPane.showMessageDialog(frame, rb.getString("COULD_NOT_LOAD_PROFILE_DIALOG_TEXT"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void newActivation(final String... args) {
		SwingUtilities
				.invokeLater(() -> JOptionPane.showMessageDialog(frame, rb.getString("ALREADY_RUNNING_DIALOG_TEXT"),
						rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
	}

	private void newProfile() {
		stopAll();

		currentFile = null;

		if (input != null)
			input.deInit();

		input = new Input(this, selectedJid);

		loadedProfile = null;
		updateTitleAndTooltip();
		updateModesPanel();
		updateOverlayPanel();
		setStatusBarText(rb.getString("STATUS_READY"));
		fileChooser.setSelectedFile(new File(rb.getString("PROFILE_FILE_SUFFIX")));
	}

	private void onControllersChanged(final boolean selectFirstTab) {
		final var presentJids = new HashSet<Integer>();
		for (var jid = GLFW_JOYSTICK_1; jid <= GLFW_JOYSTICK_LAST; jid++)
			if (glfwJoystickPresent(jid) && glfwJoystickIsGamepad(jid)) {
				presentJids.add(jid);

				if (!isSelectedJidValid())
					setSelectedJid(jid);
			}

		final var controllerConnected = !presentJids.isEmpty();
		if (!controllerConnected)
			selectedJid = INVALID_JID;

		final var previousSelectedTabIndex = tabbedPane.getSelectedIndex();
		fileMenu.remove(newMenuItem);
		fileMenu.remove(openMenuItem);
		fileMenu.remove(saveMenuItem);
		fileMenu.remove(saveAsMenuItem);
		if (fileMenu.getItemCount() > 1)
			fileMenu.remove(0);
		deviceMenu.removeAll();
		menuBar.remove(deviceMenu);
		menuBar.remove(localMenu);
		menuBar.remove(serverMenu);
		tabbedPane.remove(modesPanel);
		tabbedPane.remove(assignmentsComponent);
		tabbedPane.remove(overlayPanel);

		if (controllerConnected) {
			fileMenu.insert(newMenuItem, 0);
			fileMenu.insert(openMenuItem, 1);
			fileMenu.insert(saveMenuItem, 2);
			fileMenu.insert(saveAsMenuItem, 3);
			fileMenu.insertSeparator(4);

			for (final var jid : presentJids)
				deviceMenu.add(new SelectControllerAction(jid));
			menuBar.add(deviceMenu, 1);

			if (windows)
				menuBar.add(localMenu, 2);

			menuBar.add(serverMenu, windows ? 4 : 2);

			modesPanel = new JPanel(new BorderLayout());
			tabbedPane.insertTab(rb.getString("MODES_TAB"), null, modesPanel, null,
					tabbedPane.indexOfComponent(settingsScrollPane));

			modesListPanel = new JPanel();
			modesListPanel.setLayout(new GridBagLayout());

			modesScrollPane = new JScrollPane();
			modesScrollPane
					.setViewportBorder(BorderFactory.createMatteBorder(10, 10, 0, 10, modesListPanel.getBackground()));
			modesPanel.add(modesScrollPane, BorderLayout.CENTER);

			addModePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			final var addButton = new JButton(new AddModeAction());
			addButton.setPreferredSize(BUTTON_DIMENSION);
			addModePanel.add(addButton);
			modesPanel.add(addModePanel, BorderLayout.SOUTH);

			assignmentsComponent = new AssignmentsComponent(this);
			tabbedPane.insertTab(rb.getString("ASSIGNMENTS_TAB"), null, assignmentsComponent, null,
					tabbedPane.indexOfComponent(settingsScrollPane));

			overlayPanel = new JPanel(new BorderLayout());

			indicatorsListPanel = new JPanel();
			indicatorsListPanel.setLayout(new GridBagLayout());

			indicatorsScrollPane = new JScrollPane();
			indicatorsScrollPane.setViewportBorder(
					BorderFactory.createMatteBorder(10, 10, 0, 10, indicatorsListPanel.getBackground()));
			overlayPanel.add(indicatorsScrollPane, BorderLayout.CENTER);
			tabbedPane.insertTab(rb.getString("OVERLAY_TAB"), null, overlayPanel, null,
					tabbedPane.indexOfComponent(settingsScrollPane));
		}

		if (selectFirstTab || !controllerConnected)
			tabbedPane.setSelectedIndex(0);
		else if (previousSelectedTabIndex < tabbedPane.getTabCount())
			tabbedPane.setSelectedIndex(previousSelectedTabIndex);

		updateMenuShortcuts();
		updateModesPanel();
		updateOverlayPanel();
		updatePanelAccess();

		frame.getContentPane().invalidate();
		frame.getContentPane().repaint();
	}

	private void onOutputThreadsChanged() {
		final var localActive = localThread != null && localThread.isAlive();
		final var clientActive = clientThread != null && clientThread.isAlive();
		final var serverActive = serverThread != null && serverThread.isAlive();

		final var noneActive = !localActive && !clientActive && !serverActive;

		if (startLocalRadioButtonMenuItem != null) {
			startLocalRadioButtonMenuItem.setSelected(localActive);
			startLocalRadioButtonMenuItem.setEnabled(noneActive);
		}

		if (stopLocalRadioButtonMenuItem != null) {
			stopLocalRadioButtonMenuItem.setSelected(!localActive);
			stopLocalRadioButtonMenuItem.setEnabled(localActive);
		}

		if (startClientRadioButtonMenuItem != null) {
			startClientRadioButtonMenuItem.setSelected(clientActive);
			startClientRadioButtonMenuItem.setEnabled(noneActive);
		}

		if (stopClientRadioButtonMenuItem != null) {
			stopClientRadioButtonMenuItem.setSelected(!clientActive);
			stopClientRadioButtonMenuItem.setEnabled(clientActive);
		}

		if (startServerRadioButtonMenuItem != null) {
			startServerRadioButtonMenuItem.setSelected(serverActive);
			startServerRadioButtonMenuItem.setEnabled(noneActive);
		}

		if (stopServerRadioButtonMenuItem != null) {
			stopServerRadioButtonMenuItem.setSelected(!serverActive);
			stopServerRadioButtonMenuItem.setEnabled(serverActive);
		}

		updateMenuShortcuts();
		updatePanelAccess();
	}

	public boolean preventPowerSaveMode() {
		return preferences.getBoolean(PREFERENCES_PREVENT_POWER_SAVE_MODE, true);
	}

	public void quit() {
		if (serverSocket != null)
			try {
				serverSocket.close();
			} catch (final IOException e) {
				log.log(Logger.Level.ERROR, e.getMessage(), e);
			}

		if (input != null)
			input.deInit();

		stopAll();
		glfwTerminate();
		Singleton.stop();
		System.exit(0);
	}

	private void repaintOverlay() {
		if (overlayFrame != null) {
			overlayFrame.getContentPane().validate();
			overlayFrame.getContentPane().repaint();
		}

		if (onScreenKeyboard.isVisible()) {
			onScreenKeyboard.getContentPane().validate();
			onScreenKeyboard.getContentPane().repaint();
		}
	}

	public void restartLast() {
		switch (lastOutputType) {
		case LOCAL:
			startLocal();
			break;
		case CLIENT:
			startClient();
			break;
		case SERVER:
			startServer();
			break;
		case NONE:
			break;
		}
	}

	private void saveLastProfile(final File file) {
		currentFile = file;
		preferences.put(PREFERENCES_LAST_PROFILE, file.getAbsolutePath());
	}

	private void saveProfile(File file) {
		input.reset();

		final String profileFileSuffix = rb.getString("PROFILE_FILE_SUFFIX");
		if (!file.getName().toLowerCase(Locale.getDefault()).endsWith(profileFileSuffix))
			file = new File(file.getAbsoluteFile() + profileFileSuffix);

		final var gson = new GsonBuilder().registerTypeAdapterFactory(new ModeAwareTypeAdapterFactory())
				.registerTypeAdapter(IAction.class, new ActionTypeAdapter()).setPrettyPrinting().create();
		final var jsonString = gson.toJson(input.getProfile());
		try {
			Files.writeString(file.toPath(), jsonString);
			saveLastProfile(file);
			loadedProfile = file.getName();
			setUnsavedChanges(false);
			setStatusBarText(rb.getString("STATUS_PROFILE_SAVED") + file.getAbsolutePath());
			scheduleStatusBarText(rb.getString("STATUS_READY"));
		} catch (final IOException e) {
			log.log(Logger.Level.ERROR, e.getMessage(), e);
			JOptionPane.showMessageDialog(frame, rb.getString("COULD_NOT_SAVE_PROFILE"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void saveProfileAs() {
		fileChooser.setSelectedFile(currentFile);
		if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
			saveProfile(fileChooser.getSelectedFile());
	}

	public void scheduleStatusBarText(final String text) {
		class StatusBarTextTimerTask extends TimerTask {

			private final String newText;
			private final String originalText;

			public StatusBarTextTimerTask(final String newText) {
				this.newText = newText;
				originalText = statusLabel.getText();
			}

			@Override
			public void run() {
				SwingUtilities.invokeLater(() -> {
					if (statusLabel.getText().equals(originalText))
						setStatusBarText(newText);
				});
			}
		}

		timer.schedule(new StatusBarTextTimerTask(text), 5000L);
	}

	public void setOverlayText(final String text) {
		invokeOnEventDispatchThreadIfRequired(() -> labelCurrentMode.setText(text));
	}

	public void setSelectedJid(final int jid) {
		if (selectedJid == jid)
			return;

		selectedJid = jid;

		final var guid = glfwGetJoystickGUID(jid);
		if (guid != null)
			preferences.put(PREFERENCES_LAST_CONTROLLER, guid);

		stopAll();

		Profile previousProfile = null;
		if (input != null) {
			input.deInit();
			previousProfile = input.getProfile();
		}

		input = new Input(this, selectedJid);

		if (previousProfile != null)
			input.setProfile(previousProfile, selectedJid);
	}

	public void setStatusBarText(final String text) {
		if (statusLabel != null) {
			statusLabel.setText(text);
			repaintOverlay();
		}
	}

	void setUnsavedChanges(final boolean unsavedChanges) {
		this.unsavedChanges = unsavedChanges;
		updateTitleAndTooltip();
	}

	private void startClient() {
		lastOutputType = OutputType.CLIENT;
		clientThread = new ClientVJoyOutputThread(Main.this, input);
		clientThread.setvJoyDevice(
				new UINT(preferences.getInt(PREFERENCES_VJOY_DEVICE, VJoyOutputThread.DEFAULT_VJOY_DEVICE)));
		clientThread.setHost(hostTextField.getText());
		clientThread.setPort(preferences.getInt(PREFERENCES_PORT, ServerOutputThread.DEFAULT_PORT));
		clientThread.setTimeout(preferences.getInt(PREFERENCES_TIMEOUT, ServerOutputThread.DEFAULT_TIMEOUT));
		clientThread.start();

		onOutputThreadsChanged();
	}

	private void startLocal() {
		if (!isSelectedJidValid())
			return;

		lastOutputType = OutputType.LOCAL;
		localThread = new LocalVJoyOutputThread(Main.this, input);
		localThread.setvJoyDevice(
				new UINT(preferences.getInt(PREFERENCES_VJOY_DEVICE, VJoyOutputThread.DEFAULT_VJOY_DEVICE)));
		localThread.setPollInterval(preferences.getInt(PREFERENCES_POLL_INTERVAL, OutputThread.DEFAULT_POLL_INTERVAL));
		localThread.start();

		onOutputThreadsChanged();

		initOverlay();
		initVrOverlay();
		startOverlayTimerTask();
	}

	private void startOverlayTimerTask() {
		stopOverlayTimerTask();

		overlayTimerTask = new TimerTask() {

			@Override
			public void run() {
				SwingUtilities.invokeLater(() -> {
					if (!isModalDialogShowing()) {
						if (overlayFrame != null) {
							overlayFrame.setAlwaysOnTop(false);
							overlayFrame.setAlwaysOnTop(true);
						}

						if (onScreenKeyboard.isVisible()) {
							onScreenKeyboard.setAlwaysOnTop(false);
							onScreenKeyboard.setAlwaysOnTop(true);
						}
					}

					final var maxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
							.getMaximumWindowBounds();
					if (!maxWindowBounds.equals(prevMaxWindowBounds)) {
						prevMaxWindowBounds = maxWindowBounds;

						updateOverlayLocation(maxWindowBounds);
						onScreenKeyboard.updateLocation();
					}

					repaintOverlay();
				});
			}
		};

		timer.schedule(overlayTimerTask, OVERLAY_POSITION_UPDATE_INTERVAL, OVERLAY_POSITION_UPDATE_INTERVAL);
	}

	private void startServer() {
		if (!isSelectedJidValid())
			return;

		lastOutputType = OutputType.SERVER;
		serverThread = new ServerOutputThread(Main.this, input);
		serverThread.setPort(preferences.getInt(PREFERENCES_PORT, ServerOutputThread.DEFAULT_PORT));
		serverThread.setTimeout(preferences.getInt(PREFERENCES_TIMEOUT, ServerOutputThread.DEFAULT_TIMEOUT));
		serverThread.setPollInterval(preferences.getInt(PREFERENCES_POLL_INTERVAL, OutputThread.DEFAULT_POLL_INTERVAL));
		serverThread.start();

		onOutputThreadsChanged();

		initOverlay();
		startOverlayTimerTask();
	}

	public void stopAll() {
		if (windows) {
			stopLocal(false);
			stopClient(false);
		}
		stopServer(false);

		System.gc();
	}

	private void stopClient(final boolean resetLastOutputType) {
		if (clientThread != null)
			clientThread.stopOutput();

		if (resetLastOutputType)
			lastOutputType = OutputType.NONE;

		waitForThreadToFinish(clientThread);

		invokeOnEventDispatchThreadIfRequired(() -> {
			onOutputThreadsChanged();
		});
	}

	private void stopLocal(final boolean resetLastOutputType) {
		if (localThread != null)
			localThread.stopOutput();

		if (resetLastOutputType)
			lastOutputType = OutputType.NONE;

		invokeOnEventDispatchThreadIfRequired(() -> {
			stopOverlayTimerTask();
			deInitOverlay();
		});

		waitForOverlayDeInit();

		waitForThreadToFinish(localThread);

		invokeOnEventDispatchThreadIfRequired(() -> {
			onOutputThreadsChanged();
		});
	}

	private void stopOverlayTimerTask() {
		if (overlayTimerTask != null)
			overlayTimerTask.cancel();
	}

	private void stopServer(final boolean resetLastOutputType) {
		if (serverThread != null)
			serverThread.stopOutput();

		if (resetLastOutputType)
			lastOutputType = OutputType.NONE;

		invokeOnEventDispatchThreadIfRequired(() -> {
			stopOverlayTimerTask();
			deInitOverlay();
		});

		waitForOverlayDeInit();

		waitForThreadToFinish(serverThread);

		invokeOnEventDispatchThreadIfRequired(() -> {
			onOutputThreadsChanged();
		});
	}

	public void toggleOnScreenKeyboard() {
		if (localThread != null && localThread.isAlive() || clientThread != null && clientThread.isAlive()
				|| serverThread != null && serverThread.isAlive())
			SwingUtilities.invokeLater(() -> {
				onScreenKeyboard.setVisible(!onScreenKeyboard.isVisible());
				repaintOverlay();
			});
	}

	private void updateMenuShortcuts() {
		final var menuCount = menuBar.getMenuCount();
		final var alreadyAssignedMenuKeyCodes = new HashSet<Integer>(menuCount);

		for (var i = 0; i < menuCount; i++) {
			final var menu = menuBar.getMenu(i);
			menu.setMnemonic(getExtendedKeyCodeForMenu(menu, alreadyAssignedMenuKeyCodes));

			final var menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
			for (var j = 0; j < menu.getItemCount(); j++) {
				final var menuItem = menu.getItem(j);
				if (menuItem != null) {
					KeyStroke keyStroke = null;

					if (menuItem.isEnabled()) {
						final var keyCode = getExtendedKeyCodeForMenuItem(menuItem);

						if (keyCode != KeyEvent.VK_UNDEFINED)
							keyStroke = KeyStroke.getKeyStroke(keyCode, menuShortcutKeyMask);
					}

					menuItem.setAccelerator(keyStroke);
				}
			}
		}
	}

	void updateModesPanel() {
		if (modesListPanel == null)
			return;

		modesListPanel.removeAll();

		final var modes = input.getProfile().getModes();
		for (var i = 0; i < modes.size(); i++) {
			final var mode = modes.get(i);

			final var modePanel = new JPanel(new GridBagLayout());
			modesListPanel.add(modePanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
					GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 5));

			final var modeNoLabel = new JLabel(rb.getString("MODE_NO_LABEL_PREFIX") + (i + 1));
			modeNoLabel.setPreferredSize(new Dimension(100, 15));
			modePanel.add(modeNoLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			modePanel.add(Box.createGlue(), new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final var descriptionTextField = new JTextField(mode.getDescription(), 20);
			modePanel.add(descriptionTextField, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final var setModeDescriptionAction = new SetModeDescriptionAction(mode, descriptionTextField);
			descriptionTextField.addActionListener(setModeDescriptionAction);
			descriptionTextField.getDocument().addDocumentListener(setModeDescriptionAction);

			modePanel.add(Box.createGlue(), new GridBagConstraints(3, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			if (Profile.defaultMode.equals(mode) || OnScreenKeyboard.onScreenKeyboardMode.equals(mode)) {
				descriptionTextField.setEditable(false);
				modePanel.add(Box.createHorizontalStrut(BUTTON_DIMENSION.width));
			} else {
				final var deleteButton = new JButton(new RemoveModeAction(mode));
				deleteButton.setPreferredSize(BUTTON_DIMENSION);
				modePanel.add(deleteButton, new GridBagConstraints(4, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
						GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			}
		}

		modesListPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		modesScrollPane.setViewportView(modesListPanel);
	}

	private void updateOverlayAlignment(final Rectangle maxWindowBounds) {
		final var inLowerHalf = overlayFrame.getY() + overlayFrame.getHeight() / 2 < maxWindowBounds.height / 2;

		overlayFrame.remove(labelCurrentMode);
		overlayFrame.add(labelCurrentMode, inLowerHalf ? BorderLayout.PAGE_START : BorderLayout.PAGE_END);

		var alignment = SwingConstants.RIGHT;
		var flowLayoutAlignment = FlowLayout.RIGHT;
		if (overlayFrame.getX() + overlayFrame.getWidth() / 2 < maxWindowBounds.width / 2) {
			alignment = SwingConstants.LEFT;
			flowLayoutAlignment = FlowLayout.LEFT;
		}

		labelCurrentMode.setHorizontalAlignment(alignment);

		indicatorPanelFlowLayout.setAlignment(flowLayoutAlignment);
		indicatorPanel.invalidate();

		overlayFrame.setBackground(TRANSPARENT);
		overlayFrame.pack();
	}

	public void updateOverlayAxisIndicators() {
		for (final var virtualAxis : Input.VirtualAxis.values())
			if (virtualAxisToProgressBarMap.containsKey(virtualAxis)) {
				OutputThread outputThread = null;
				if (localThread != null && localThread.isAlive())
					outputThread = localThread;
				else if (clientThread != null && clientThread.isAlive())
					outputThread = clientThread;
				else if (serverThread != null && serverThread.isAlive())
					outputThread = serverThread;

				if (outputThread != null) {
					final var progressBar = virtualAxisToProgressBarMap.get(virtualAxis);
					progressBar.setMinimum(-outputThread.getMaxAxisValue());
					progressBar.setMaximum(outputThread.getMinAxisValue());

					final var newValue = -input.getAxes().get(virtualAxis);
					if (progressBar.getValue() != newValue)
						progressBar.setValue(newValue);
				}
			}
	}

	private void updateOverlayLocation(final Rectangle maxWindowBounds) {
		if (overlayFrame != null && overlayFrameDragListener != null && !overlayFrameDragListener.isDragging()) {
			overlayFrame.pack();
			final var x = maxWindowBounds.width - overlayFrame.getWidth();
			final var y = maxWindowBounds.height - overlayFrame.getHeight();
			final var defaultLocation = new Point(x, y);
			loadFrameLocation(preferences, overlayFrame, defaultLocation, maxWindowBounds);
			updateOverlayAlignment(maxWindowBounds);
		}
	}

	private void updateOverlayPanel() {
		if (indicatorsListPanel == null)
			return;

		indicatorsListPanel.removeAll();

		for (final var virtualAxis : Input.VirtualAxis.values()) {
			final var indicatorPanel = new JPanel(new GridBagLayout());
			indicatorsListPanel.add(indicatorPanel,
					new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
							GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0),
							0, 5));

			final var virtualAxisLabel = new JLabel(virtualAxis.toString() + rb.getString("AXIS_LABEL_SUFFIX"));
			virtualAxisLabel.setPreferredSize(new Dimension(100, 15));
			indicatorPanel.add(virtualAxisLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final var virtualAxisToOverlayAxisMap = input.getProfile().getVirtualAxisToOverlayAxisMap();
			final var overlayAxis = virtualAxisToOverlayAxisMap.get(virtualAxis);
			final var enabled = overlayAxis != null;

			final var colorLabel = new JLabel();
			if (enabled) {
				colorLabel.setOpaque(true);
				colorLabel.setBackground(overlayAxis.color);
			} else
				colorLabel.setText(rb.getString("INDICATOR_DISABLED_LABEL"));
			colorLabel.setHorizontalAlignment(SwingConstants.CENTER);

			colorLabel.setPreferredSize(new Dimension(100, 15));
			colorLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			indicatorPanel.add(colorLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final var colorButton = new JButton(new SelectIndicatorColorAction(virtualAxis));
			colorButton.setPreferredSize(BUTTON_DIMENSION);
			colorButton.setEnabled(enabled);
			indicatorPanel.add(colorButton, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final var invertedCheckBox = new JCheckBox(new InvertIndicatorAction(virtualAxis));
			invertedCheckBox.setSelected(enabled && overlayAxis.inverted);
			invertedCheckBox.setEnabled(enabled);
			indicatorPanel.add(invertedCheckBox, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final var displayCheckBox = new JCheckBox(new DisplayIndicatorAction(virtualAxis));
			displayCheckBox.setSelected(enabled);
			indicatorPanel.add(displayCheckBox, new GridBagConstraints(4, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		}

		indicatorsListPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		indicatorsScrollPane.setViewportView(indicatorsListPanel);
	}

	private void updatePanelAccess() {
		final var localActive = localThread != null && localThread.isAlive();
		final var serverActive = serverThread != null && serverThread.isAlive();

		final var panelsEnabled = !localActive && !serverActive;
		setEnabledRecursive(modesListPanel, panelsEnabled);
		setEnabledRecursive(addModePanel, panelsEnabled);
		if (assignmentsComponent != null)
			assignmentsComponent.setEnabled(panelsEnabled);
		setEnabledRecursive(indicatorsListPanel, panelsEnabled);
		setEnabledRecursive(settingsPanel, panelsEnabled);
	}

	public void updateTitleAndTooltip() {
		final var sb = new StringBuilder();

		if (!isSelectedJidValid())
			sb.append(rb.getString("APPLICATION_NAME"));
		else if (loadedProfile == null)
			sb.append(rb.getString("MAIN_FRAME_TITLE_UNSAVED_PROFILE"));
		else {
			if (unsavedChanges)
				sb.append(rb.getString("MAIN_FRAME_TITLE_PREFIX"));

			sb.append(loadedProfile);
			sb.append(rb.getString("MAIN_FRAME_TITLE_SUFFIX"));
		}

		frame.setTitle(sb.toString());

		if (trayIcon != null && input != null) {
			if (input.isDualShock4Controller())
				sb.append(rb.getString("BATTERY_TOOLTIP_PREFIX") + input.getBatteryState()
						+ (input.isCharging() ? rb.getString("BATTERY_TOOLTIP_CHARGING_SUFFIX")
								: rb.getString("BATTERY_TOOLTIP_DISCHARGING_SUFFIX")));

			trayIcon.setToolTip(sb.toString());
		}
	}

	private void waitForOverlayDeInit() {
		while (overlayFrame != null || openVrOverlay != null)
			try {
				Thread.sleep(100L);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
	}

}

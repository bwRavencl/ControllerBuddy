/* Copyright (C) 2018  Matteo Hausner
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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
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
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.System.Logger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
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
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
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
import com.google.gson.JsonParseException;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.WinDef.UINT;

import de.bwravencl.controllerbuddy.Version;
import de.bwravencl.controllerbuddy.gui.GuiUtils.FrameDragListener;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.json.ActionAdapter;
import de.bwravencl.controllerbuddy.output.ClientVJoyOutputThread;
import de.bwravencl.controllerbuddy.output.LocalVJoyOutputThread;
import de.bwravencl.controllerbuddy.output.OutputThread;
import de.bwravencl.controllerbuddy.output.ServerOutputThread;
import de.bwravencl.controllerbuddy.output.VJoyOutputThread;
import de.bwravencl.controllerbuddy.util.ResourceBundleUtil;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;

public final class Main {

	private class AddModeAction extends AbstractAction {

		private static final long serialVersionUID = -4881923833724315489L;

		private AddModeAction() {
			putValue(NAME, rb.getString("ADD_MODE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("ADD_MODE_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final Mode mode = new Mode();
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
				input.getProfile().getVirtualAxisToColorMap().put(virtualAxis, new Color(0, 0, 0, 128));
			else
				input.getProfile().getVirtualAxisToColorMap().remove(virtualAxis);

			setUnsavedChanges(true);
			updateOverlayPanel();
		}

	}

	private class EditComponentAction extends AbstractAction {

		private static final long serialVersionUID = 8811608785278071903L;

		private final Component component;

		private EditComponentAction(final Component component) {
			this.component = component;

			putValue(NAME, rb.getString("EDIT_COMPONENT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("EDIT_COMPONENT_ACTION_DESCRIPTION_PREFIX") + component.getName()
					+ rb.getString("EDIT_COMPONENT_ACTION_DESCRIPTION_SUFFIX"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final EditActionsDialog editComponentDialog = new EditActionsDialog(Main.this, component, input);
			editComponentDialog.setVisible(true);
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

	private static enum OutputType {
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

		private final Controller controller;

		private SelectControllerAction(final Controller controller) {
			this.controller = controller;

			final String name = controller.getName();
			putValue(NAME, name);
			putValue(SHORT_DESCRIPTION, rb.getString("SELECT_CONTROLLER_ACTION_DESCRIPTION_PREFIX") + name
					+ rb.getString("SELECT_CONTROLLER_ACTION_DESCRIPTION_SUFFIX"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			setSelectedController(controller);
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
			final Color newColor = JColorChooser.showDialog(frame, "Choose Background Color",
					input.getProfile().getVirtualAxisToColorMap().get(virtualAxis));
			if (newColor != null)
				input.getProfile().getVirtualAxisToColorMap().replace(virtualAxis, newColor);

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
			final String host = hostTextField.getText();

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
			final String description = modeDescriptionTextField.getText();

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
			final ImageIcon icon = new ImageIcon(Main.class.getResource(Main.ICON_RESOURCE_PATHS[2]));
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
			final WindowEvent openEvent = new WindowEvent(frame, WindowEvent.WINDOW_OPENED);
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

	private static final System.Logger log = System.getLogger(Main.class.getName());
	public static final String STRING_RESOURCE_BUNDLE_BASENAME = "strings";
	private static final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(STRING_RESOURCE_BUNDLE_BASENAME,
			Locale.getDefault());
	private static final int SINGLE_INSTANCE_PORT = 58008;
	static final int DIALOG_BOUNDS_X = 100;
	static final int DIALOG_BOUNDS_Y = 100;
	static final int DIALOG_BOUNDS_WIDTH = 580;
	static final int DIALOG_BOUNDS_HEIGHT = 640;
	static final int DIALOG_BOUNDS_X_Y_OFFSET = 25;
	static final Dimension BUTTON_DIMENSION = new Dimension(100, 25);
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
	private static final long ASSIGNMENTS_PANEL_UPDATE_INTERVAL = 100L;
	private static final long OVERLAY_POSITION_UPDATE_INTERVAL = 10000L;
	private static final String[] ICON_RESOURCE_PATHS = { "/icon_16.png", "/icon_32.png", "/icon_64.png",
			"/icon_128.png" };
	private static final String KEYBOARD_ICON_RESOURCE_PATH = "/keyboard.png";
	static final Color TRANSPARENT = new Color(255, 255, 255, 0);

	private static boolean isModalDialogShowing() {
		final Window[] windows = Window.getWindows();
		if (windows != null)
			for (final Window w : windows)
				if (w.isShowing() && w instanceof Dialog && ((Dialog) w).isModal())
					return true;

		return false;
	}

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(() -> {
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
				helpFormatter.printHelp(rb.getString("APPLICATION_NAME"), options, true);
			}
		});
	}

	private static void setEnabledRecursive(final java.awt.Component component, final boolean enabled) {
		if (component == null)
			return;

		component.setEnabled(enabled);

		if (component instanceof Container)
			for (final java.awt.Component child : ((Container) component).getComponents())
				setEnabledRecursive(child, enabled);
	}

	private final boolean windows = Platform.isWindows() && !Platform.isWindowsCE();
	private final Preferences preferences = Preferences.userNodeForPackage(Version.class);
	private final Map<VirtualAxis, JProgressBar> virtualAxisToProgressBarMap = new HashMap<>();
	private LocalVJoyOutputThread localThread;
	private ClientVJoyOutputThread clientThread;
	private ServerOutputThread serverThread;
	private Controller selectedController;
	private Input input;
	private OutputType lastOutputType = OutputType.NONE;
	private final JFrame frame;
	private final OpenAction openAction = new OpenAction();
	private JRadioButtonMenuItem startLocalRadioButtonMenuItem;
	private JRadioButtonMenuItem stopLocalRadioButtonMenuItem;
	private JRadioButtonMenuItem startClientRadioButtonMenuItem;
	private JRadioButtonMenuItem stopClientRadioButtonMenuItem;
	private JRadioButtonMenuItem startServerRadioButtonMenuItem;
	private JRadioButtonMenuItem stopServerRadioButtonMenuItem;
	private MenuItem showMenuItem;
	private final JPanel modesPanel;
	private final JScrollPane modesScrollPane;
	private final JPanel modesListPanel;
	private final JPanel assignmentsPanel;
	private final JPanel overlayPanel;
	private final JPanel settingsPanel;
	private final JScrollPane indicatorsScrollPane;
	private final JPanel indicatorsListPanel;
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
	private volatile JButton onScreenKeyboardButton;
	private final OnScreenKeyboard onScreenKeyboard = new OnScreenKeyboard(this);

	private Main() {
		frame = new JFrame();

		try {
			serverSocket = new ServerSocket(SINGLE_INSTANCE_PORT, 10, InetAddress.getLoopbackAddress());
		} catch (final IOException e) {
			JOptionPane.showMessageDialog(frame, rb.getString("ALREADY_RUNNING_DIALOG_TEXT"),
					rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			quit();
		}

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
		final List<Controller> controllers = Input.getControllers();
		final boolean controllerConnected = !controllers.isEmpty();
		if (controllerConnected) {
			fileMenu.add(openAction);
			fileMenu.add(new SaveAction());
			fileMenu.add(new SaveAsAction());
		}
		fileMenu.add(new JSeparator());
		final QuitAction quitAction = new QuitAction();
		fileMenu.add(quitAction);

		if (controllerConnected) {
			final JMenu deviceMenu = new JMenu(rb.getString("DEVICE_MENU"));
			menuBar.add(deviceMenu);
			deviceMenu.addMenuListener(new MenuListener() {

				@Override
				public void menuCanceled(final MenuEvent e) {
				}

				@Override
				public void menuDeselected(final MenuEvent e) {
				}

				@Override
				public void menuSelected(final MenuEvent e) {
					deviceMenu.removeAll();

					for (final Controller c : controllers)
						if (c.poll())
							deviceMenu.add(new SelectControllerAction(c));
				}
			});
			deviceMenu.setEnabled(true);
		}

		if (windows) {
			if (controllerConnected) {
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
			}

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

		if (controllerConnected) {
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
		}

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
		pollIntervalSpinner.addChangeListener(
				e -> preferences.putInt(PREFERENCES_POLL_INTERVAL, (int) ((JSpinner) e.getSource()).getValue()));
		pollIntervalPanel.add(pollIntervalSpinner);

		if (windows) {
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
			vJoyDeviceSpinner.addChangeListener(
					e -> preferences.putInt(PREFERENCES_VJOY_DEVICE, (int) ((JSpinner) e.getSource()).getValue()));
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
		portSpinner.addChangeListener(
				e -> preferences.putInt(PREFERENCES_PORT, (int) ((JSpinner) e.getSource()).getValue()));
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
		timeoutSpinner.addChangeListener(
				e -> preferences.putInt(PREFERENCES_TIMEOUT, (int) ((JSpinner) e.getSource()).getValue()));
		timeoutPanel.add(timeoutSpinner);

		final boolean alwaysOnTopSupported = Toolkit.getDefaultToolkit().isAlwaysOnTopSupported();
		if (alwaysOnTopSupported || preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, alwaysOnTopSupported)) {
			final JPanel overlaySettingsPanel = new JPanel(panelFlowLayout);
			settingsPanel.add(overlaySettingsPanel, panelGridBagConstraints);

			final JLabel overlayLabel = new JLabel(rb.getString("OVERLAY_LABEL"));
			overlayLabel.setPreferredSize(new Dimension(120, 15));
			overlaySettingsPanel.add(overlayLabel);

			final JCheckBox showOverlayCheckBox = new JCheckBox(rb.getString("SHOW_OVERLAY_CHECK_BOX"));
			showOverlayCheckBox.setSelected(preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, true));
			showOverlayCheckBox.addActionListener(e -> {
				final boolean showOverlay = ((JCheckBox) e.getSource()).isSelected();

				preferences.putBoolean(PREFERENCES_SHOW_OVERLAY, showOverlay);
			});
			overlaySettingsPanel.add(showOverlayCheckBox);
		}

		if (windows) {
			if (preferences.getBoolean(PREFERENCES_SHOW_VR_OVERLAY, true)) {
				final JPanel vrOverlaySettingsPanel = new JPanel(panelFlowLayout);
				settingsPanel.add(vrOverlaySettingsPanel, panelGridBagConstraints);

				final JLabel vrOverlayLabel = new JLabel(rb.getString("VR_OVERLAY_LABEL"));
				vrOverlayLabel.setPreferredSize(new Dimension(120, 15));
				vrOverlaySettingsPanel.add(vrOverlayLabel);

				final JCheckBox showVrOverlayCheckBox = new JCheckBox(rb.getString("SHOW_VR_OVERLAY_CHECK_BOX"));
				showVrOverlayCheckBox.setSelected(preferences.getBoolean(PREFERENCES_SHOW_VR_OVERLAY, true));
				showVrOverlayCheckBox.addActionListener(e -> {
					final boolean showVrOverlay = ((JCheckBox) e.getSource()).isSelected();

					preferences.putBoolean(PREFERENCES_SHOW_VR_OVERLAY, showVrOverlay);
				});
				vrOverlaySettingsPanel.add(showVrOverlayCheckBox);
			}

			final JPanel preventPowerSaveModeSettingsPanel = new JPanel(panelFlowLayout);
			settingsPanel.add(preventPowerSaveModeSettingsPanel, panelGridBagConstraints);

			final JLabel preventPowerSaveModeLabel = new JLabel(rb.getString("POWER_SAVE_MODE_LABEL"));
			preventPowerSaveModeLabel.setPreferredSize(new Dimension(120, 15));
			preventPowerSaveModeSettingsPanel.add(preventPowerSaveModeLabel);

			final JCheckBox preventPowerSaveModeCheckBox = new JCheckBox(
					rb.getString("PREVENT_POWER_SAVE_MODE_CHECK_BOX"));
			preventPowerSaveModeCheckBox.setSelected(preferences.getBoolean(PREFERENCES_PREVENT_POWER_SAVE_MODE, true));
			preventPowerSaveModeCheckBox.addActionListener(e -> {
				final boolean preventPowerSaveMode = ((JCheckBox) e.getSource()).isSelected();

				preferences.putBoolean(PREFERENCES_PREVENT_POWER_SAVE_MODE, preventPowerSaveMode);
			});
			preventPowerSaveModeSettingsPanel.add(preventPowerSaveModeCheckBox);
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

		final Border outsideBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		final Border insideBorder = BorderFactory.createEmptyBorder(0, 5, 0, 5);
		statusLabel.setBorder(BorderFactory.createCompoundBorder(outsideBorder, insideBorder));
		frame.add(statusLabel, BorderLayout.SOUTH);

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

		newProfile();

		if (input.getController() == null)
			JOptionPane.showMessageDialog(frame, rb.getString("NO_CONTROLLER_CONNECTED_DIALOG_TEXT"),
					rb.getString("INFORMATION_DIALOG_TITLE"), JOptionPane.INFORMATION_MESSAGE);
		else {
			final String path = preferences.get(PREFERENCES_LAST_PROFILE, null);
			if (path != null)
				loadProfile(new File(path));
		}

		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				SwingUtilities.invokeLater(() -> {
					if (frame.getState() == Frame.ICONIFIED
							|| !assignmentsScrollPane.equals(tabbedPane.getSelectedComponent()))
						return;

					assignmentsPanel.removeAll();

					final Controller controller = input.getController();
					if (controller != null && controller.poll()) {
						for (final Component c : input.getComponents(controller)) {
							final JPanel componentPanel = new JPanel(new GridBagLayout());
							assignmentsPanel.add(componentPanel,
									new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
											GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
											new Insets(0, 0, 0, 0), 5, 5));

							final String name = c.getName();
							final float value = c.getPollData();

							final JLabel nameLabel = new JLabel();
							nameLabel.setPreferredSize(new Dimension(100, 15));

							final GridBagConstraints nameGridBagConstraints = new GridBagConstraints(0, 0, 1, 1, 0.0,
									0.0, GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0),
									0, 0);

							final GridBagConstraints valueGridBagConstraints = new GridBagConstraints(2, 0, 1, 1, 1.0,
									1.0, GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0),
									0, 0);

							if (c.isAnalog()) {
								nameLabel.setText(rb.getString("AXIS_LABEL") + name);
								componentPanel.add(nameLabel, nameGridBagConstraints);

								componentPanel.add(Box.createGlue(),
										new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
												GridBagConstraints.BASELINE, GridBagConstraints.NONE,
												new Insets(0, 0, 0, 0), 0, 0));

								final JProgressBar valueProgressBar = new JProgressBar(-100, 100);
								valueProgressBar.setValue((int) (value * 100.0f));
								componentPanel.add(valueProgressBar, valueGridBagConstraints);
							} else {
								nameLabel.setText(rb.getString("BUTTON_LABEL") + name);
								componentPanel.add(nameLabel, nameGridBagConstraints);

								componentPanel.add(Box.createGlue(),
										new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
												GridBagConstraints.BASELINE, GridBagConstraints.NONE,
												new Insets(0, 0, 0, 0), 0, 0));

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
									new GridBagConstraints(3, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
											GridBagConstraints.BASELINE, GridBagConstraints.NONE,
											new Insets(0, 0, 0, 0), 0, 0));

							final JButton editButton = new JButton(new EditComponentAction(c));
							editButton.setPreferredSize(BUTTON_DIMENSION);
							componentPanel.add(editButton,
									new GridBagConstraints(4, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
											GridBagConstraints.BASELINE, GridBagConstraints.NONE,
											new Insets(0, 0, 0, 0), 0, 0));
						}

						assignmentsPanel.add(Box.createGlue(),
								new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
										GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
										new Insets(0, 0, 0, 0), 0, 0));
					}

					setEnabledRecursive(assignmentsPanel, assignmentsPanel.isEnabled());
					assignmentsScrollPane.setViewportView(assignmentsPanel);
				});
			}
		}, 0L, ASSIGNMENTS_PANEL_UPDATE_INTERVAL);
	}

	private void deInitOverlay() {
		if (openVrOverlay != null) {
			openVrOverlay.stop();
			openVrOverlay = null;
		}

		if (overlayFrame != null) {
			overlayFrame.dispose();
			overlayFrame = null;
			onScreenKeyboardButton = null;
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

	public OnScreenKeyboard getOnScreenKeyboard() {
		return onScreenKeyboard;
	}

	JButton getOnScreenKeyboardButton() {
		return onScreenKeyboardButton;
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
			for (final List<ButtonToModeAction> buttonToModeActions : input.getProfile().getComponentToModeActionMap()
					.values())
				for (final ButtonToModeAction buttonToModeAction : buttonToModeActions)
					if (OnScreenKeyboard.onScreenKeyboardMode.equals(buttonToModeAction.getMode(input))) {
						buttonToModeAction.doAction(input, buttonToModeAction.getActivationValue());
						break;
					}

			scheduleOnScreenKeyboardModeSwitch = false;
		}
	}

	private void initOverlay() {
		if (!preferences.getBoolean(PREFERENCES_SHOW_OVERLAY, Toolkit.getDefaultToolkit().isAlwaysOnTopSupported()))
			return;

		String longestDescription = "";
		for (final Mode m : input.getProfile().getModes()) {
			final String description = m.getDescription();
			if (description.length() > longestDescription.length())
				longestDescription = description;
		}

		final FontMetrics fontMetrics = labelCurrentMode.getFontMetrics(labelCurrentMode.getFont());
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
		if (input.getProfile().getModes().contains(OnScreenKeyboard.onScreenKeyboardMode)) {
			final Icon icon = new ImageIcon(Main.class.getResource(KEYBOARD_ICON_RESOURCE_PATH));
			onScreenKeyboardButton = new JButton(icon);
			onScreenKeyboardButton.addActionListener(e -> {
				scheduleOnScreenKeyboardModeSwitch = true;
			});
			onScreenKeyboardButton.setBorder(null);
			onScreenKeyboardButton.setFocusPainted(false);
			onScreenKeyboardButton.setContentAreaFilled(false);
			overlayFrame.add(onScreenKeyboardButton, BorderLayout.PAGE_START);
		}

		overlayFrame.add(labelCurrentMode, BorderLayout.PAGE_END);
		overlayFrame.setAlwaysOnTop(true);

		indicatorPanelFlowLayout = new FlowLayout();
		indicatorPanel = new JPanel(indicatorPanelFlowLayout);
		indicatorPanel.setBackground(TRANSPARENT);

		for (final VirtualAxis va : Input.VirtualAxis.values()) {
			final Map<VirtualAxis, Color> virtualAxisToColorMap = input.getProfile().getVirtualAxisToColorMap();

			if (virtualAxisToColorMap.containsKey(va)) {
				final JProgressBar progressBar = new JProgressBar(SwingConstants.VERTICAL);
				progressBar.setPreferredSize(new Dimension(21, 149));
				progressBar.setBorder(
						BorderFactory.createDashedBorder(Color.BLACK, (float) progressBar.getPreferredSize().getWidth(),
								(float) progressBar.getPreferredSize().getWidth()));
				progressBar.setBackground(Color.LIGHT_GRAY);
				progressBar.setForeground(virtualAxisToColorMap.get(va));
				progressBar.setValue(1);
				indicatorPanel.add(progressBar);
				virtualAxisToProgressBarMap.put(va, progressBar);
			}
		}

		overlayFrame.add(indicatorPanel);

		overlayFrameDragListener = new FrameDragListener(this, overlayFrame) {

			@Override
			public void mouseDragged(final MouseEvent e) {
				super.mouseDragged(e);
				final Rectangle maxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
						.getMaximumWindowBounds();
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

	public boolean isWindows() {
		return windows;
	}

	private void loadProfile(final File file) {
		stopAll();

		boolean profileLoaded = false;

		try {
			final String jsonString = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			final ActionAdapter actionAdapter = new ActionAdapter();
			final Gson gson = new GsonBuilder().registerTypeAdapter(IAction.class, actionAdapter).create();

			try {
				final Profile profile = gson.fromJson(jsonString, Profile.class);

				final Set<String> unknownActionClasses = actionAdapter.getUnknownActionClasses();
				if (!unknownActionClasses.isEmpty())
					JOptionPane.showMessageDialog(frame,
							rb.getString("UNKNOWN_ACTION_TYPES_DIALOG_TEXT") + String.join("\n", unknownActionClasses),
							rb.getString("WARNING_DIALOG_TITLE"), JOptionPane.WARNING_MESSAGE);

				profileLoaded = input.setProfile(profile, input.getController());
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

	private void newProfile() {
		stopAll();

		currentFile = null;

		if (input != null)
			input.deInit();

		input = new Input(this, selectedController);

		loadedProfile = null;
		updateTitleAndTooltip();
		updateModesPanel();
		updateOverlayPanel();
		setStatusBarText(rb.getString("STATUS_READY"));
		fileChooser.setSelectedFile(new File(rb.getString("PROFILE_FILE_SUFFIX")));
	}

	public boolean preventPowerSaveMode() {
		if (!windows)
			return false;

		return preferences.getBoolean(Main.PREFERENCES_PREVENT_POWER_SAVE_MODE, true);
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
		System.exit(0);
	}

	private void repaintOverlay() {
		if (overlayFrame == null)
			return;

		overlayFrame.getContentPane().validate();
		overlayFrame.getContentPane().repaint();

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

		final Gson gson = new GsonBuilder().registerTypeAdapter(IAction.class, new ActionAdapter()).setPrettyPrinting()
				.create();
		final String jsonString = gson.toJson(input.getProfile());

		try (FileOutputStream fos = new FileOutputStream(file)) {
			final Writer writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
			try {
				writer.write(jsonString);
				writer.flush();
				fos.flush();
				fos.close();
			} finally {
				writer.close();
			}

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

	void setOnScreenKeyboardButtonVisible(final boolean visible) {
		if (onScreenKeyboardButton == null)
			return;

		onScreenKeyboardButton.setVisible(visible);

		final Rectangle maxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		updateOverlayLocation(maxWindowBounds);
	}

	public void setOverlayText(final String text) {
		GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> labelCurrentMode.setText(text));
	}

	public void setSelectedController(final Controller controller) {
		selectedController = controller;
		newProfile();
		preferences.put(PREFERENCES_LAST_CONTROLLER, controller.getName());
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

	public void startClient() {
		lastOutputType = OutputType.CLIENT;
		startClientRadioButtonMenuItem.setSelected(true);
		if (startLocalRadioButtonMenuItem != null)
			startLocalRadioButtonMenuItem.setEnabled(false);
		startClientRadioButtonMenuItem.setEnabled(false);
		if (startServerRadioButtonMenuItem != null)
			startServerRadioButtonMenuItem.setEnabled(false);
		stopClientRadioButtonMenuItem.setEnabled(true);
		clientThread = new ClientVJoyOutputThread(Main.this, input);
		clientThread.setvJoyDevice(
				new UINT(preferences.getInt(PREFERENCES_VJOY_DEVICE, VJoyOutputThread.DEFAULT_VJOY_DEVICE)));
		clientThread.setHost(hostTextField.getText());
		clientThread.setPort(preferences.getInt(PREFERENCES_PORT, ServerOutputThread.DEFAULT_PORT));
		clientThread.setTimeout(preferences.getInt(PREFERENCES_TIMEOUT, ServerOutputThread.DEFAULT_TIMEOUT));
		clientThread.start();

		initOverlay();
		initVrOverlay();

		startOverlayTimerTask();
	}

	public void startLocal() {
		lastOutputType = OutputType.LOCAL;
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
		localThread.setPollInterval(preferences.getInt(PREFERENCES_POLL_INTERVAL, OutputThread.DEFAULT_POLL_INTERVAL));
		localThread.start();

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

					final Rectangle maxWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
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

	public void startServer() {
		lastOutputType = OutputType.SERVER;
		startServerRadioButtonMenuItem.setSelected(true);
		if (startLocalRadioButtonMenuItem != null)
			startLocalRadioButtonMenuItem.setEnabled(false);
		if (startClientRadioButtonMenuItem != null)
			startClientRadioButtonMenuItem.setEnabled(false);
		startServerRadioButtonMenuItem.setEnabled(false);
		stopServerRadioButtonMenuItem.setEnabled(true);
		setEnabledRecursive(modesPanel, false);
		setEnabledRecursive(assignmentsPanel, false);
		setEnabledRecursive(overlayPanel, false);
		setEnabledRecursive(settingsPanel, false);
		serverThread = new ServerOutputThread(Main.this, input);
		serverThread.setPort(preferences.getInt(PREFERENCES_PORT, ServerOutputThread.DEFAULT_PORT));
		serverThread.setTimeout(preferences.getInt(PREFERENCES_TIMEOUT, ServerOutputThread.DEFAULT_TIMEOUT));
		serverThread.setPollInterval(preferences.getInt(PREFERENCES_POLL_INTERVAL, OutputThread.DEFAULT_POLL_INTERVAL));
		serverThread.start();
	}

	private void stopAll() {
		if (windows) {
			stopLocal(false);
			stopClient(false);
		}
		stopServer(false);

		while (localThread != null && localThread.isAlive() || clientThread != null && clientThread.isAlive()
				|| serverThread != null && serverThread.isAlive())
			try {
				Thread.sleep(100L);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		System.gc();
	}

	public void stopClient(final boolean resetLastOutputType) {
		if (clientThread != null)
			clientThread.stopOutput();

		if (stopClientRadioButtonMenuItem != null) {
			stopClientRadioButtonMenuItem.setSelected(true);
			stopClientRadioButtonMenuItem.setEnabled(false);
		}

		if (startLocalRadioButtonMenuItem != null)
			startLocalRadioButtonMenuItem.setEnabled(true);

		if (startClientRadioButtonMenuItem != null)
			startClientRadioButtonMenuItem.setEnabled(true);

		if (startServerRadioButtonMenuItem != null)
			startServerRadioButtonMenuItem.setEnabled(true);

		if (resetLastOutputType)
			lastOutputType = OutputType.NONE;

		stopOverlayTimerTask();
		deInitOverlay();
	}

	public void stopLocal(final boolean resetLastOutputType) {
		if (localThread != null)
			localThread.stopOutput();

		if (stopLocalRadioButtonMenuItem != null) {
			stopLocalRadioButtonMenuItem.setSelected(true);
			stopLocalRadioButtonMenuItem.setEnabled(false);
		}

		if (startLocalRadioButtonMenuItem != null)
			startLocalRadioButtonMenuItem.setEnabled(true);

		if (startClientRadioButtonMenuItem != null)
			startClientRadioButtonMenuItem.setEnabled(true);

		if (startServerRadioButtonMenuItem != null)
			startServerRadioButtonMenuItem.setEnabled(true);

		setEnabledRecursive(modesPanel, true);
		setEnabledRecursive(assignmentsPanel, true);
		setEnabledRecursive(overlayPanel, true);
		setEnabledRecursive(settingsPanel, true);

		if (resetLastOutputType)
			lastOutputType = OutputType.NONE;

		stopOverlayTimerTask();
		deInitOverlay();
	}

	private void stopOverlayTimerTask() {
		if (overlayTimerTask != null)
			overlayTimerTask.cancel();
	}

	public void stopServer(final boolean resetLastOutputType) {
		if (serverThread != null)
			serverThread.stopOutput();

		if (stopLocalRadioButtonMenuItem != null) {
			stopLocalRadioButtonMenuItem.setSelected(true);
			stopLocalRadioButtonMenuItem.setEnabled(false);
		}

		if (windows) {
			if (startLocalRadioButtonMenuItem != null)
				startLocalRadioButtonMenuItem.setEnabled(true);

			if (startClientRadioButtonMenuItem != null)
				startClientRadioButtonMenuItem.setEnabled(true);
		}

		if (startServerRadioButtonMenuItem != null)
			startServerRadioButtonMenuItem.setEnabled(true);

		setEnabledRecursive(modesPanel, true);
		setEnabledRecursive(assignmentsPanel, true);
		setEnabledRecursive(overlayPanel, true);
		setEnabledRecursive(settingsPanel, true);

		if (resetLastOutputType)
			lastOutputType = OutputType.NONE;
	}

	public void toggleOnScreenKeyboard() {
		if (localThread != null && localThread.isAlive() || clientThread != null && clientThread.isAlive()
				|| serverThread != null && serverThread.isAlive())
			SwingUtilities.invokeLater(() -> {
				onScreenKeyboard.setVisible(!onScreenKeyboard.isVisible());
				repaintOverlay();
			});
	}

	void updateModesPanel() {
		modesListPanel.removeAll();

		final List<Mode> modes = input.getProfile().getModes();
		for (final Mode m : modes) {
			final JPanel modePanel = new JPanel(new GridBagLayout());
			modesListPanel.add(modePanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
					GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 5));

			final JLabel modeNoLabel = new JLabel(rb.getString("MODE_NO_LABEL_PREFIX") + modes.indexOf(m));
			modeNoLabel.setPreferredSize(new Dimension(100, 15));
			modePanel.add(modeNoLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			modePanel.add(Box.createGlue(), new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final JTextField descriptionTextField = new JTextField(m.getDescription(), 20);
			modePanel.add(descriptionTextField, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			final SetModeDescriptionAction setModeDescriptionAction = new SetModeDescriptionAction(m,
					descriptionTextField);
			descriptionTextField.addActionListener(setModeDescriptionAction);
			descriptionTextField.getDocument().addDocumentListener(setModeDescriptionAction);

			modePanel.add(Box.createGlue(), new GridBagConstraints(3, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
					GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			if (Profile.defaultMode.equals(m) || OnScreenKeyboard.onScreenKeyboardMode.equals(m)) {
				descriptionTextField.setEditable(false);
				modePanel.add(Box.createHorizontalStrut(BUTTON_DIMENSION.width));
			} else {
				final JButton deleteButton = new JButton(new RemoveModeAction(m));
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
		final boolean inLowerHalf = overlayFrame.getY() + overlayFrame.getHeight() / 2 < maxWindowBounds.height / 2;

		if (onScreenKeyboardButton != null) {
			overlayFrame.remove(onScreenKeyboardButton);
			overlayFrame.add(onScreenKeyboardButton, inLowerHalf ? BorderLayout.PAGE_END : BorderLayout.PAGE_START);
		}

		overlayFrame.remove(labelCurrentMode);
		overlayFrame.add(labelCurrentMode, inLowerHalf ? BorderLayout.PAGE_START : BorderLayout.PAGE_END);

		int alignment = SwingConstants.RIGHT;
		int flowLayoutAlignment = FlowLayout.RIGHT;
		if (overlayFrame.getX() + overlayFrame.getWidth() / 2 < maxWindowBounds.width / 2) {
			alignment = SwingConstants.LEFT;
			flowLayoutAlignment = FlowLayout.LEFT;
		}

		if (onScreenKeyboardButton != null)
			onScreenKeyboardButton.setHorizontalAlignment(alignment);
		labelCurrentMode.setHorizontalAlignment(alignment);

		indicatorPanelFlowLayout.setAlignment(flowLayoutAlignment);
		indicatorPanel.invalidate();

		overlayFrame.pack();
	}

	public void updateOverlayAxisIndicators() {
		for (final VirtualAxis va : Input.VirtualAxis.values())
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

					final int newValue = -input.getAxis().get(va);
					if (progressBar.getValue() != newValue)
						progressBar.setValue(newValue);
				}
			}
	}

	private void updateOverlayLocation(final Rectangle maxWindowBounds) {
		if (overlayFrame != null && overlayFrameDragListener != null && !overlayFrameDragListener.isDragging()) {
			overlayFrame.pack();
			final int x = maxWindowBounds.width - overlayFrame.getWidth();
			final int y = maxWindowBounds.height - overlayFrame.getHeight();
			final Point defaultLocation = new Point(x, y);
			GuiUtils.loadFrameLocation(preferences, overlayFrame, defaultLocation, maxWindowBounds);
			updateOverlayAlignment(maxWindowBounds);
		}
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

			final boolean enabled = input.getProfile().getVirtualAxisToColorMap().containsKey(va);

			final JLabel colorLabel = new JLabel();
			if (enabled) {
				colorLabel.setOpaque(true);
				colorLabel.setBackground(input.getProfile().getVirtualAxisToColorMap().get(va));
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

	public void updateTitleAndTooltip() {
		final StringBuilder sb = new StringBuilder();

		if (loadedProfile == null)
			sb.append(rb.getString("MAIN_FRAME_TITLE_UNSAVED_PROFILE"));
		else {
			if (unsavedChanges)
				sb.append(rb.getString("MAIN_FRAME_TITLE_PREFIX"));

			sb.append(loadedProfile);
			sb.append(rb.getString("MAIN_FRAME_TITLE_SUFFIX"));
		}

		frame.setTitle(sb.toString());

		if (trayIcon != null && input != null) {
			if (windows && Input.isDualShock4Controller(input.getController()))
				sb.append(rb.getString("BATTERY_TOOLTIP_PREFIX") + input.getBatteryState()
						+ (input.isCharging() ? rb.getString("BATTERY_TOOLTIP_CHARGING_SUFFIX")
								: rb.getString("BATTERY_TOOLTIP_DISCHARGING_SUFFIX")));

			trayIcon.setToolTip(sb.toString());
		}
	}

}

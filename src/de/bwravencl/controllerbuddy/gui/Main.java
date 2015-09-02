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

package de.bwravencl.controllerbuddy.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.output.ClientVJoyOutputThread;
import de.bwravencl.controllerbuddy.output.LocalVJoyOutputThread;
import de.bwravencl.controllerbuddy.output.ServerOutputThread;
import de.bwravencl.controllerbuddy.output.VJoyOutputThread;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JLabel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.swing.JSpinner;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sun.jna.platform.win32.WinDef.UINT;

import net.brockmatt.util.ResourceBundleUtil;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;

public class Main {

	public static final int DIALOG_BOUNDS_X = 100;
	public static final int DIALOG_BOUNDS_Y = 100;
	public static final int DIALOG_BOUNDS_WIDTH = 600;
	public static final int DIALOG_BOUNDS_HEIGHT = 600;
	public static final int DIALOG_BOUNDS_X_Y_OFFSET = 25;
	public static final Dimension BUTTON_DIMENSION = new Dimension(100, 25);
	public static final String STRING_RESOURCE_BUNDLE_BASENAME = "strings";
	public static final String[] ICON_RESOURCE_PATHS = { "/icon_16.png", "/icon_32.png", "/icon_64.png",
			"/icon_128.png" };

	public static final String PREFERENCES_LAST_CONTROLLER = "last_controller";
	public static final String PREFERENCES_LAST_PROFILE = "last_profile";
	public static final String PREFERENCES_VJOY_DIRECTORY = "vjoy_directory";
	public static final String PREFERENCES_VJOY_DEVICE = "vjoy_device";
	public static final String PREFERENCES_HOST = "host";
	public static final String PREFERENCES_PORT = "port";
	public static final String PREFERENCES_TIMEOUT = "timeout";
	public static final String PREFERENCES_UPDATE_RATE = "update_rate";

	private static final long ASSIGNMENTS_PANEL_UPDATE_RATE = 100L;

	private Controller selectedController;
	private Input input;
	private LocalVJoyOutputThread localThread;
	private ClientVJoyOutputThread clientThread;
	private ServerOutputThread serverThread;
	private boolean suspendControllerSettingsUpdate = false;
	private final Preferences preferences = Preferences.userNodeForPackage(getClass());
	private final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(STRING_RESOURCE_BUNDLE_BASENAME,
			Locale.getDefault());

	private final JFrame frame;
	private JRadioButtonMenuItem startLocalRadioButtonMenuItem;
	private JRadioButtonMenuItem stopLocalRadioButtonMenuItem;
	private JRadioButtonMenuItem startClientRadioButtonMenuItem;
	private JRadioButtonMenuItem stopClientRadioButtonMenuItem;
	private final JRadioButtonMenuItem startServerRadioButtonMenuItem;
	private final JRadioButtonMenuItem stopServerRadioButtonMenuItem;
	private final JPanel modesListPanel;
	private JLabel vJoyDirectoryLabel1;
	private JSpinner vJoyDeviceSpinner;
	private JTextField hostTextField;
	private final JSpinner portSpinner;
	private final JSpinner timeoutSpinner;
	private final JSpinner updateRateSpinner;
	private final JScrollPane modesScrollPane;
	private final JLabel statusLabel = new JLabel(rb.getString("STATUS_READY"));
	private final JFileChooser fileChooser = new JFileChooser();

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				try {
					Main main = new Main();
					main.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	public Main() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				stopLocal();
				stopServer();
			}
		});

		frame = new JFrame();
		frame.setTitle(rb.getString("APPLICATION_NAME"));
		frame.setBounds(DIALOG_BOUNDS_X, DIALOG_BOUNDS_Y, DIALOG_BOUNDS_WIDTH, DIALOG_BOUNDS_HEIGHT);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final List<Image> icons = new ArrayList<Image>();
		for (String s : ICON_RESOURCE_PATHS) {
			final ImageIcon icon = new ImageIcon(Main.class.getResource(s));
			icons.add(icon.getImage());
		}
		frame.setIconImages(icons);

		final JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		final JMenu fileMenu = new JMenu(rb.getString("FILE_MENU"));
		menuBar.add(fileMenu);
		fileMenu.add(new NewProfileAction());
		fileMenu.add(new OpenFileAction());
		fileMenu.add(new SaveFileAction());
		fileMenu.add(new JSeparator());
		fileMenu.add(new QuitAction());

		final JMenu deviceMenu = new JMenu(rb.getString("DEVICE_MENU"));
		deviceMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				deviceMenu.removeAll();

				final Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

				for (Controller c : controllers)
					if (c.getType() != Type.KEYBOARD && c.getType() != Type.MOUSE && c.getType() != Type.TRACKBALL
							&& c.getType() != Type.TRACKPAD && c.getType() != Type.UNKNOWN
							&& !c.getName().startsWith("vJoy"))
						deviceMenu.add(new SelectControllerAction(c));
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
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

		final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frame.getContentPane().add(tabbedPane);

		final JPanel modesPanel = new JPanel(new BorderLayout());
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

		final JPanel assignmentsPanel = new JPanel();
		assignmentsPanel.setLayout(new GridBagLayout());

		final JScrollPane assignmentsScrollPane = new JScrollPane();
		assignmentsScrollPane
				.setViewportBorder(BorderFactory.createMatteBorder(10, 10, 0, 10, assignmentsPanel.getBackground()));
		tabbedPane.addTab(rb.getString("ASSIGNMENTS_TAB"), null, assignmentsScrollPane, null);

		final JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new GridBagLayout());

		final JScrollPane settingsScrollPane = new JScrollPane();
		settingsScrollPane.setViewportView(settingsPanel);
		tabbedPane.addTab(rb.getString("SETTINGS_TAB"), null, settingsScrollPane, null);

		final GridBagConstraints panelGridBagConstraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1,
				0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 5);

		final FlowLayout panelFlowLayout = new FlowLayout(FlowLayout.LEADING, 10, 10);

		if (isWindows()) {
			final JPanel vJoyDirectoryPanel = new JPanel(panelFlowLayout);
			settingsPanel.add(vJoyDirectoryPanel, panelGridBagConstraints);

			final JLabel vJoyDirectoryLabel = new JLabel(rb.getString("VJOY_DIRECTORY_LABEL"));
			vJoyDirectoryLabel.setPreferredSize(new Dimension(100, 15));
			vJoyDirectoryPanel.add(vJoyDirectoryLabel);

			vJoyDirectoryLabel1 = new JLabel(
					preferences.get(PREFERENCES_VJOY_DIRECTORY, VJoyOutputThread.getDefaultInstallationPath()));
			vJoyDirectoryPanel.add(vJoyDirectoryLabel1);

			final JButton vJoyDirectoryButton = new JButton(new ChangeVJoyDirectoryAction());
			vJoyDirectoryPanel.add(vJoyDirectoryButton);

			final JPanel vJoyDevicePanel = new JPanel(panelFlowLayout);
			settingsPanel.add(vJoyDevicePanel, panelGridBagConstraints);

			final JLabel vJoyDeviceLabel = new JLabel(rb.getString("VJOY_DEVICE_LABEL"));
			vJoyDeviceLabel.setPreferredSize(new Dimension(100, 15));
			vJoyDevicePanel.add(vJoyDeviceLabel);

			vJoyDeviceSpinner = new JSpinner(new SpinnerNumberModel(
					preferences.getInt(PREFERENCES_VJOY_DEVICE, VJoyOutputThread.DEFAULT_VJOY_DEVICE), 1, 16, 1));
			vJoyDeviceSpinner.setEditor(new JSpinner.NumberEditor(vJoyDeviceSpinner, "#"));
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
			hostLabel.setPreferredSize(new Dimension(100, 15));
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
		portLabel.setPreferredSize(new Dimension(100, 15));
		portPanel.add(portLabel);

		portSpinner = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_PORT, ServerOutputThread.DEFAULT_PORT), 1024, 65535, 1));
		portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"));
		portSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_PORT, (int) ((JSpinner) e.getSource()).getValue());
			}
		});
		portPanel.add(portSpinner);

		final JPanel updateRatePanel = new JPanel(panelFlowLayout);
		settingsPanel.add(updateRatePanel, panelGridBagConstraints);

		final JLabel updateRateLabel = new JLabel(rb.getString("UPDATE_RATE_LABEL"));
		updateRateLabel.setPreferredSize(new Dimension(100, 15));
		updateRatePanel.add(updateRateLabel);

		updateRateSpinner = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_UPDATE_RATE, (int) ServerOutputThread.DEFAULT_UPDATE_RATE), 10, 500, 1));
		updateRateSpinner.setEditor(new JSpinner.NumberEditor(updateRateSpinner, "#"));
		updateRateSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_UPDATE_RATE, (int) ((JSpinner) e.getSource()).getValue());
			}
		});
		updateRatePanel.add(updateRateSpinner);

		final JPanel timeoutPanel = new JPanel(panelFlowLayout);
		settingsPanel.add(timeoutPanel, panelGridBagConstraints);

		final JLabel timeoutLabel = new JLabel(rb.getString("TIMEOUT_LABEL"));
		timeoutLabel.setPreferredSize(new Dimension(100, 15));
		timeoutPanel.add(timeoutLabel);

		timeoutSpinner = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_TIMEOUT, ServerOutputThread.DEFAULT_TIMEOUT), 10, 60000, 1));
		timeoutSpinner.setEditor(new JSpinner.NumberEditor(timeoutSpinner, "#"));
		timeoutSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_TIMEOUT, (int) ((JSpinner) e.getSource()).getValue());
			}
		});
		timeoutPanel.add(timeoutSpinner);

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

		for (Controller c : ControllerEnvironment.getDefaultEnvironment().getControllers())
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
			if (path != null)
				loadProfile(new File(path));

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

										for (Component c : controller.getComponents()) {
											final JPanel componentPanel = new JPanel(new GridBagLayout());
											assignmentsPanel.add(componentPanel,
													new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0,
															0.0, GridBagConstraints.FIRST_LINE_START,
															GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 5,
															0));

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
																1.0, 1.0, GridBagConstraints.BASELINE,
																GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

												final JProgressBar valueProgressBar = new JProgressBar(-100, 100);
												valueProgressBar.setValue((int) (value * 100.0f));
												componentPanel.add(valueProgressBar, valueGridBagConstraints);
											} else {
												nameLabel.setText(rb.getString("BUTTON_LABEL") + name);
												componentPanel.add(nameLabel, nameGridBagConstraints);

												componentPanel.add(Box.createGlue(),
														new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1,
																1.0, 1.0, GridBagConstraints.BASELINE,
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
													new GridBagConstraints(3, GridBagConstraints.RELATIVE, 1, 1, 1.0,
															1.0, GridBagConstraints.BASELINE, GridBagConstraints.NONE,
															new Insets(0, 0, 0, 0), 0, 0));

											final JButton editButton = new JButton(new EditComponentAction(c));
											editButton.setPreferredSize(BUTTON_DIMENSION);
											editButton.addMouseListener(new MouseListener() {

												@Override
												public void mouseReleased(MouseEvent e) {
												}

												@Override
												public void mousePressed(MouseEvent e) {
													suspendControllerSettingsUpdate = true;
												}

												@Override
												public void mouseExited(MouseEvent e) {
												}

												@Override
												public void mouseEntered(MouseEvent e) {
												}

												@Override
												public void mouseClicked(MouseEvent e) {
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

									assignmentsScrollPane.setViewportView(assignmentsPanel);
								}
							});

						try {
							Thread.sleep(ASSIGNMENTS_PANEL_UPDATE_RATE);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			};
			updateAssignmentsPanelThread.start();
		}
	}

	private void updateModesPanel() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				modesListPanel.removeAll();

				final List<Mode> modes = Input.getProfile().getModes();
				for (Mode p : modes) {
					final JPanel modePanel = new JPanel(new GridBagLayout());
					modesListPanel.add(modePanel,
							new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
									GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
									new Insets(0, 0, 0, 0), 0, 5));

					final JLabel modeNoLabel = new JLabel("Mode " + modes.indexOf(p));
					modeNoLabel.setPreferredSize(new Dimension(100, 15));
					modePanel.add(modeNoLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

					modePanel.add(Box.createGlue(), new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 1.0,
							1.0, GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

					final JTextField descriptionTextField = new JTextField(p.getDescription(), 20);
					modePanel.add(descriptionTextField, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0,
							GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

					final SetModeDescriptionAction setModeDescriptionAction = new SetModeDescriptionAction(p,
							descriptionTextField);
					descriptionTextField.addActionListener(setModeDescriptionAction);
					descriptionTextField.addFocusListener(setModeDescriptionAction);

					modePanel.add(Box.createGlue(), new GridBagConstraints(3, GridBagConstraints.RELATIVE, 1, 1, 1.0,
							1.0, GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

					final JButton deleteButton = new JButton(new RemoveModeAction(p));
					deleteButton.setPreferredSize(BUTTON_DIMENSION);
					modePanel.add(deleteButton, new GridBagConstraints(4, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
							GridBagConstraints.BASELINE, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

					if (Profile.isDefaultMode(p)) {
						descriptionTextField.setEditable(false);
						deleteButton.setEnabled(false);
					}

				}

				modesListPanel.add(Box.createGlue(),
						new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
								GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
								0));

				modesScrollPane.setViewportView(modesListPanel);
			}
		});
	}

	private void newProfile() {
		input = new Input(selectedController);

		frame.setTitle(rb.getString("MAIN_FRAME_TITLE_UNSAVED_PROFILE"));
		updateModesPanel();
		setStatusbarText(rb.getString("STATUS_READY"));
	}

	public void stopLocal() {
		if (localThread != null)
			localThread.stopFeeder();
		stopLocalRadioButtonMenuItem.setSelected(true);
		stopLocalRadioButtonMenuItem.setEnabled(false);
		startLocalRadioButtonMenuItem.setEnabled(true);
		startClientRadioButtonMenuItem.setEnabled(true);
		startServerRadioButtonMenuItem.setEnabled(true);
	}

	public void stopClient() {
		if (clientThread != null)
			clientThread.stopFeeder();
		stopClientRadioButtonMenuItem.setSelected(true);
		stopClientRadioButtonMenuItem.setEnabled(false);
		startLocalRadioButtonMenuItem.setEnabled(true);
		startClientRadioButtonMenuItem.setEnabled(true);
		startServerRadioButtonMenuItem.setEnabled(true);
	}

	public void stopServer() {
		if (serverThread != null)
			serverThread.closeSocket();
		stopServerRadioButtonMenuItem.setSelected(true);
		stopServerRadioButtonMenuItem.setEnabled(false);
		startLocalRadioButtonMenuItem.setEnabled(true);
		startClientRadioButtonMenuItem.setEnabled(true);
		startServerRadioButtonMenuItem.setEnabled(true);
	}

	private boolean loadProfile(File file) {
		boolean result = false;

		try {
			final String jsonString = new String(Files.readAllBytes(file.toPath()));
			final Gson gson = new GsonBuilder().registerTypeAdapter(IAction.class, new InterfaceAdapter<IAction>())
					.create();

			final Profile profile = gson.fromJson(jsonString, Profile.class);

			result = Input.setProfile(profile, input.getController());
			if (result)
				saveLastProfile(file);

			updateModesPanel();
			frame.setTitle(file.getName() + rb.getString("MAIN_FRAME_TITLE_SUFFIX"));
			setStatusbarText(rb.getString("STATUS_PROFILE_LOADED") + file.getAbsolutePath());

			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public Preferences getPreferences() {
		return preferences;
	}

	public JFrame getFrame() {
		return frame;
	}

	private void saveLastProfile(File file) {
		preferences.put(PREFERENCES_LAST_PROFILE, file.getAbsolutePath());
	}

	public void setStatusbarText(String text) {
		if (statusLabel != null)
			statusLabel.setText(text);
	}

	public void setUnsavedChangesTitle() {
		final String title = frame.getTitle();

		if (!title.startsWith(rb.getString("MAIN_FRAME_TITLE_UNSAVED_PROFILE"))
				&& !title.startsWith(rb.getString("MAIN_FRAME_TITLE_PREFIX")))
			frame.setTitle(rb.getString("MAIN_FRAME_TITLE_PREFIX") + title);
	}

	private class NewProfileAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5703987691203427504L;

		public NewProfileAction() {
			putValue(NAME, rb.getString("NEW_PROFILE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("NEW_PROFILE_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			newProfile();
		}

	}

	private class OpenFileAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8932510785275935297L;

		public OpenFileAction() {
			putValue(NAME, rb.getString("OPEN_PROFILE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("OPEN_PROFILE_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				final File file = fileChooser.getSelectedFile();

				if (!loadProfile(file))
					JOptionPane.showMessageDialog(frame, rb.getString("COULD_NOT_LOAD_PROFILE_DIALOG_TEXT"),
							rb.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			}
		}

	}

	private class SaveFileAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8469921697479550983L;

		public SaveFileAction() {
			putValue(NAME, rb.getString("SAVE_PROFILE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("SAVE_PROFILE_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				final String profileFileSuffix = rb.getString("PROFILE_FILE_SUFFIX");
				if (!file.getName().toLowerCase().endsWith(profileFileSuffix))
					file = new File(file.getAbsoluteFile() + profileFileSuffix);

				final Gson gson = new GsonBuilder().registerTypeAdapter(IAction.class, new InterfaceAdapter<IAction>())
						.create();
				final String jsonString = gson.toJson(Input.getProfile());

				try (FileOutputStream fos = new FileOutputStream(file)) {
					final Writer writer = new BufferedWriter(new OutputStreamWriter(fos));
					writer.write(jsonString);
					writer.flush();
					fos.flush();
					fos.close();

					saveLastProfile(file);
					frame.setTitle(file.getName() + rb.getString("MAIN_FRAME_TITLE_SUFFIX"));
					setStatusbarText(rb.getString("STATUS_PROFILE_SAVED") + file.getAbsolutePath());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
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

		public void actionPerformed(ActionEvent e) {
			Main.this.frame.dispose();
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

		public void actionPerformed(ActionEvent e) {
			startLocalRadioButtonMenuItem.setEnabled(false);
			startClientRadioButtonMenuItem.setEnabled(false);
			startServerRadioButtonMenuItem.setEnabled(false);
			stopLocalRadioButtonMenuItem.setEnabled(true);
			localThread = new LocalVJoyOutputThread(Main.this, input);
			localThread.setvJoyDevice(new UINT((int) vJoyDeviceSpinner.getValue()));
			localThread.setUpdateRate((int) updateRateSpinner.getValue());
			localThread.start();
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

		public void actionPerformed(ActionEvent e) {
			stopLocal();
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

		public void actionPerformed(ActionEvent e) {
			startLocalRadioButtonMenuItem.setEnabled(false);
			startClientRadioButtonMenuItem.setEnabled(false);
			startServerRadioButtonMenuItem.setEnabled(false);
			stopClientRadioButtonMenuItem.setEnabled(true);
			clientThread = new ClientVJoyOutputThread(Main.this, input);
			clientThread.setvJoyDevice(new UINT((int) vJoyDeviceSpinner.getValue()));
			clientThread.setHost(hostTextField.getText());
			clientThread.setPort((int) portSpinner.getValue());
			clientThread.setTimeout((int) timeoutSpinner.getValue());
			clientThread.start();
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

		public void actionPerformed(ActionEvent e) {
			stopClient();
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

		public void actionPerformed(ActionEvent e) {
			startLocalRadioButtonMenuItem.setEnabled(false);
			startClientRadioButtonMenuItem.setEnabled(false);
			startServerRadioButtonMenuItem.setEnabled(false);
			stopServerRadioButtonMenuItem.setEnabled(true);
			serverThread = new ServerOutputThread(Main.this, input);
			serverThread.setPort((int) portSpinner.getValue());
			serverThread.setTimeout((int) timeoutSpinner.getValue());
			serverThread.setUpdateRate((int) updateRateSpinner.getValue());
			serverThread.start();
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

		public void actionPerformed(ActionEvent e) {
			stopServer();
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

		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(frame, rb.getString("ABOUT_DIALOG_TEXT"), (String) getValue(NAME),
					JOptionPane.INFORMATION_MESSAGE);
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

		public void actionPerformed(ActionEvent e) {
			selectedController = controller;
			newProfile();
			preferences.put(PREFERENCES_LAST_CONTROLLER, controller.getName());
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

		public void actionPerformed(ActionEvent e) {
			final EditActionsDialog editComponentDialog = new EditActionsDialog(Main.this, component, input);
			editComponentDialog.setVisible(true);

			suspendControllerSettingsUpdate = false;
		}

	}

	private class SetModeDescriptionAction extends AbstractAction implements FocusListener {

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

		public void actionPerformed(ActionEvent e) {
			setModeDescription();
		}

		@Override
		public void focusGained(FocusEvent e) {
		}

		@Override
		public void focusLost(FocusEvent e) {
			setModeDescription();
		}

		private void setModeDescription() {
			final String description = modeDescriptionTextField.getText();

			if (description != null && description.length() > 0) {
				mode.setDescription(description);
				setUnsavedChangesTitle();
			} else
				modeDescriptionTextField.setText(mode.getDescription());
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

		public void actionPerformed(ActionEvent e) {
			Input.getProfile().removeMode(mode);
			setUnsavedChangesTitle();
			updateModesPanel();
		}

	}

	private class AddModeAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -4881923833724315489L;

		public AddModeAction() {
			putValue(NAME, rb.getString("ADD_MODE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("ADD_MODE_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			final Mode mode = new Mode();
			Input.getProfile().getModes().add(mode);

			setUnsavedChangesTitle();
			updateModesPanel();
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

	private class SetHostAction extends AbstractAction implements FocusListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = -7674562782751876814L;

		private final JTextField hostTextField;

		public SetHostAction(JTextField hostTextField) {
			this.hostTextField = hostTextField;
		}

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

	private class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

		private static final String PROPERTY_TYPE = "type";
		private static final String PROPERTY_DATA = "data";

		public JsonElement serialize(T object, java.lang.reflect.Type interfaceType, JsonSerializationContext context) {
			final JsonObject wrapper = new JsonObject();
			wrapper.addProperty(PROPERTY_TYPE, object.getClass().getName());
			wrapper.add(PROPERTY_DATA, context.serialize(object));

			return wrapper;
		}

		public T deserialize(JsonElement elem, java.lang.reflect.Type interfaceType, JsonDeserializationContext context)
				throws JsonParseException {
			final JsonObject wrapper = (JsonObject) elem;
			final JsonElement typeName = get(wrapper, PROPERTY_TYPE);
			final JsonElement data = get(wrapper, PROPERTY_DATA);
			final java.lang.reflect.Type actualType = typeForName(typeName);

			return context.deserialize(data, actualType);
		}

		private java.lang.reflect.Type typeForName(final JsonElement typeElem) {
			try {
				return Class.forName(typeElem.getAsString());
			} catch (ClassNotFoundException e) {
				throw new JsonParseException(e);
			}
		}

		private JsonElement get(final JsonObject wrapper, String memberName) {
			final JsonElement elem = wrapper.get(memberName);
			if (elem == null)
				throw new JsonParseException(
						"No member '" + memberName + "' found in what was expected to be an interface wrapper");
			return elem;
		}

	}

}

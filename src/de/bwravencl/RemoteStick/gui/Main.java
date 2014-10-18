package de.bwravencl.RemoteStick.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.UIManager;

import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.Mode;
import de.bwravencl.RemoteStick.input.Profile;
import de.bwravencl.RemoteStick.input.action.IAction;
import de.bwravencl.RemoteStick.net.ServerThread;

import javax.swing.ButtonGroup;
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
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.swing.JSpinner;
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

	private static final String PREFERENCES_LAST_CONTROLLER = "last_controller";
	private static final String PREFERENCES_LAST_PROFILE = "last_profile";
	private static final String PREFERENCES_PORT = "port";
	private static final String PREFERENCES_CLIENT_TIMEOUT = "client_timeout";
	private static final String PREFERENCES_UPDATE_RATE = "update_rate";

	private static final long ASSIGNMENTS_PANEL_UPDATE_RATE = 100L;

	private Controller selectedController;
	private Input input;
	private ServerThread serverThread;
	private boolean suspendControllerSettingsUpdate = false;
	private final Preferences preferences = Preferences.userNodeForPackage(this
			.getClass());
	private final ResourceBundle rb = new ResourceBundleUtil()
			.getResourceBundle(STRING_RESOURCE_BUNDLE_BASENAME,
					Locale.getDefault());

	private JFrame frmRemoteStickServer;
	private JTabbedPane tabbedPane;
	private JScrollPane scrollPaneAssignments;
	private JMenu mnController;
	private JPanel panelModeList;
	private JPanel panelAssignments;
	private JSpinner spinnerPort;
	private JSpinner spinnerClientTimeout;
	private JSpinner spinnerUpdateRate;
	private JScrollPane scrollPaneModes;
	private final JLabel lblStatus = new JLabel(rb.getString("STATUS_READY"));
	private final JFileChooser fileChooser = new JFileChooser();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main window = new Main();
					window.frmRemoteStickServer.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Main() {
		initialize();

		final String lastControllerName = preferences.get(
				PREFERENCES_LAST_CONTROLLER, null);

		for (Controller c : ControllerEnvironment.getDefaultEnvironment()
				.getControllers())
			if (c.getType() != Type.KEYBOARD && c.getType() != Type.MOUSE
					&& c.getType() != Type.TRACKBALL
					&& c.getType() != Type.TRACKPAD) {
				final boolean lastControllerFound = c.getName().equals(
						lastControllerName);

				if (selectedController == null || lastControllerFound)
					selectedController = c;

				if (lastControllerFound)
					break;
			}

		if (selectedController == null) {
			int option = JOptionPane
					.showConfirmDialog(
							frmRemoteStickServer,
							rb.getString("NO_CONTROLLER_CONNECTED_DIALOG_TEXT_PART_1")
									+ UIManager.getLookAndFeelDefaults().get(
											"OptionPane.okButtonText")
									+ rb.getString("NO_CONTROLLER_CONNECTED_DIALOG_TEXT_PART_2")
									+ UIManager.getLookAndFeelDefaults().get(
											"OptionPane.cancelButtonText")
									+ rb.getString("NO_CONTROLLER_CONNECTED_DIALOG_TEXT_PART_3"),
							rb.getString("ERROR_DIALOG_TITLE"),
							JOptionPane.OK_CANCEL_OPTION);

			if (option == JOptionPane.OK_OPTION) {
				final String javaBin = System.getProperty("java.home")
						+ File.separator + "bin" + File.separator + "java";
				try {
					final File jarFile = new File(Main.class
							.getProtectionDomain().getCodeSource()
							.getLocation().toURI());

					if (jarFile.getName().endsWith(".jar")) {
						final List<String> command = new ArrayList<String>();
						command.add(javaBin);
						command.add("-jar");
						command.add(jarFile.getPath());

						final ProcessBuilder builder = new ProcessBuilder(
								command);
						builder.start();
					}
				} catch (URISyntaxException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.exit(0);
		} else {
			newProfile();

			final String path = preferences.get(PREFERENCES_LAST_PROFILE, null);
			if (path != null)
				loadProfile(new File(path));

			final Thread updateAssignmentsPanelThread = new UpdateAssignmentsPanelThread();
			updateAssignmentsPanelThread.start();
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmRemoteStickServer = new JFrame();
		frmRemoteStickServer.setTitle(rb.getString("APPLICATION_NAME"));
		frmRemoteStickServer.setBounds(DIALOG_BOUNDS_X, DIALOG_BOUNDS_Y,
				DIALOG_BOUNDS_WIDTH, DIALOG_BOUNDS_HEIGHT);
		frmRemoteStickServer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final JMenuBar menuBar = new JMenuBar();
		frmRemoteStickServer.setJMenuBar(menuBar);

		final JMenu mnFile = new JMenu(rb.getString("FILE_MENU"));
		menuBar.add(mnFile);
		mnFile.add(new NewProfileAction());
		mnFile.add(new OpenFileAction());
		mnFile.add(new SaveFileAction());
		mnFile.add(new JSeparator());
		mnFile.add(new QuitAction());

		mnController = new JMenu(rb.getString("CONTROLLER_MENU"));
		mnController.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				mnController.removeAll();

				final Controller[] controllers = ControllerEnvironment
						.getDefaultEnvironment().getControllers();

				for (Controller c : controllers)
					if (c.getType() != Type.KEYBOARD
							&& c.getType() != Type.MOUSE
							&& c.getType() != Type.TRACKBALL
							&& c.getType() != Type.TRACKPAD)
						mnController.add(new SelectControllerAction(c));
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}
		});
		mnController.setEnabled(true);
		menuBar.add(mnController);

		final JMenu mnServer = new JMenu(rb.getString("SERVER_MENU"));
		menuBar.add(mnServer);

		final ButtonGroup buttonGroupServerState = new ButtonGroup();

		final JRadioButtonMenuItem rdbtnmntmRun = new JRadioButtonMenuItem(
				rb.getString("START_SERVER_MENU_ITEM"));
		rdbtnmntmRun.setAction(new StartServerAction());
		buttonGroupServerState.add(rdbtnmntmRun);
		mnServer.add(rdbtnmntmRun);

		final JMenu mnHelp = new JMenu(rb.getString("HELP_MENU"));
		menuBar.add(mnHelp);
		mnHelp.add(new ShowAboutDialogAction());

		final JRadioButtonMenuItem rdbtnmntmStop = new JRadioButtonMenuItem(
				rb.getString("STOP_SERVER_MENU_ITEM"));
		rdbtnmntmStop.setAction(new StopServerAction());
		rdbtnmntmStop.setSelected(true);
		buttonGroupServerState.add(rdbtnmntmStop);
		mnServer.add(rdbtnmntmStop);

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmRemoteStickServer.getContentPane().add(tabbedPane);

		final JPanel panelModes = new JPanel(new BorderLayout());
		tabbedPane.addTab(rb.getString("MODES_TAB"), null, panelModes, null);

		panelModeList = new JPanel();
		panelModeList.setLayout(new GridBagLayout());

		scrollPaneModes = new JScrollPane();
		scrollPaneModes.setViewportBorder(BorderFactory.createMatteBorder(10,
				10, 0, 10, panelModeList.getBackground()));
		panelModes.add(scrollPaneModes, BorderLayout.CENTER);

		final JPanel panelAddMode = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton addButton = new JButton(new AddModeAction());
		addButton.setPreferredSize(BUTTON_DIMENSION);
		panelAddMode.add(addButton);
		panelModes.add(panelAddMode, BorderLayout.SOUTH);

		panelAssignments = new JPanel();
		panelAssignments.setLayout(new GridBagLayout());

		scrollPaneAssignments = new JScrollPane();
		scrollPaneAssignments.setViewportBorder(BorderFactory
				.createMatteBorder(10, 10, 0, 10,
						panelAssignments.getBackground()));
		tabbedPane.addTab(rb.getString("ASSIGNMENTS_TAB"), null,
				scrollPaneAssignments, null);

		final JPanel panelServerSettings = new JPanel();
		panelServerSettings.setLayout(new GridBagLayout());

		final JScrollPane scrollPaneServerSettings = new JScrollPane();
		scrollPaneServerSettings.setViewportView(panelServerSettings);
		tabbedPane.addTab(rb.getString("SERVER_SETTINGS_TAB"), null,
				scrollPaneServerSettings, null);

		final GridBagConstraints panelGridBagConstraints = new GridBagConstraints(
				0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 5);

		final FlowLayout panelFlowLayout = new FlowLayout(FlowLayout.LEADING,
				10, 10);

		final JPanel panelPort = new JPanel(panelFlowLayout);
		panelServerSettings.add(panelPort, panelGridBagConstraints);

		final JLabel lblPort = new JLabel(rb.getString("PORT_LABEL"));
		lblPort.setPreferredSize(new Dimension(100, 15));
		panelPort.add(lblPort);

		spinnerPort = new JSpinner(new SpinnerNumberModel(preferences.getInt(
				PREFERENCES_PORT, ServerThread.DEFAULT_PORT), 1024, 65535, 1));
		spinnerPort.setEditor(new JSpinner.NumberEditor(spinnerPort, "#"));
		spinnerPort.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_PORT,
						(int) ((JSpinner) e.getSource()).getValue());
			}
		});
		panelPort.add(spinnerPort);

		final JPanel panelTimeout = new JPanel(panelFlowLayout);
		panelServerSettings.add(panelTimeout, panelGridBagConstraints);

		final JLabel lblClientTimeout = new JLabel(
				rb.getString("CLIENT_TIMEOUT_LABEL"));
		lblClientTimeout.setPreferredSize(new Dimension(100, 15));
		panelTimeout.add(lblClientTimeout);

		spinnerClientTimeout = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_CLIENT_TIMEOUT,
						ServerThread.DEFAULT_CLIENT_TIMEOUT), 10, 60000, 1));
		spinnerClientTimeout.setEditor(new JSpinner.NumberEditor(
				spinnerClientTimeout, "#"));
		spinnerClientTimeout.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_CLIENT_TIMEOUT,
						(int) ((JSpinner) e.getSource()).getValue());
			}
		});
		panelTimeout.add(spinnerClientTimeout);

		final JPanel panelUpdateRate = new JPanel(panelFlowLayout);
		panelServerSettings.add(panelUpdateRate, panelGridBagConstraints);

		final JLabel lblUpdateRate = new JLabel(
				rb.getString("UPDATE_RATE_LABEL"));
		lblUpdateRate.setPreferredSize(new Dimension(100, 15));
		panelUpdateRate.add(lblUpdateRate);

		spinnerUpdateRate = new JSpinner(new SpinnerNumberModel(
				preferences.getInt(PREFERENCES_UPDATE_RATE,
						(int) ServerThread.DEFAULT_UPDATE_RATE), 10, 500, 1));
		spinnerUpdateRate.setEditor(new JSpinner.NumberEditor(
				spinnerUpdateRate, "#"));
		spinnerUpdateRate.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				preferences.putInt(PREFERENCES_UPDATE_RATE,
						(int) ((JSpinner) e.getSource()).getValue());
			}
		});
		panelUpdateRate.add(spinnerUpdateRate);

		panelServerSettings.add(Box.createGlue(), new GridBagConstraints(0,
				GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));

		lblStatus.setBorder(BorderFactory
				.createEtchedBorder(EtchedBorder.RAISED));
		frmRemoteStickServer.add(lblStatus, BorderLayout.SOUTH);

		final FileNameExtensionFilter filter = new FileNameExtensionFilter(
				rb.getString("PROFILE_FILE_DESCRIPTION"),
				rb.getString("PROFILE_FILE_EXTENSION"));
		fileChooser.setFileFilter(filter);
		fileChooser.setSelectedFile(new File(rb
				.getString("PROFILE_FILE_SUFFIX")));
	}

	private void updateModesPanel() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				panelModeList.removeAll();

				final List<Mode> modes = Input.getProfile().getModes();
				for (Mode p : modes) {
					final JPanel panelMode = new JPanel(new GridBagLayout());
					panelModeList.add(panelMode, new GridBagConstraints(0,
							GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
							GridBagConstraints.FIRST_LINE_START,
							GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0,
									0), 0, 5));

					final JLabel lblModeNo = new JLabel("Mode "
							+ modes.indexOf(p));
					lblModeNo.setPreferredSize(new Dimension(100, 15));
					panelMode.add(lblModeNo, new GridBagConstraints(0, 0, 1, 1,
							0.0, 0.0, GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					panelMode.add(Box.createGlue(), new GridBagConstraints(1,
							GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
							GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					final JTextField textFieldDescription = new JTextField(p
							.getDescription(), 20);
					panelMode.add(textFieldDescription, new GridBagConstraints(
							2, 0, 1, 1, 1.0, 1.0, GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					final SetModeDescriptionAction setModeDescriptionAction = new SetModeDescriptionAction(
							p, textFieldDescription);
					textFieldDescription
							.addActionListener(setModeDescriptionAction);
					textFieldDescription
							.addFocusListener(setModeDescriptionAction);

					panelMode.add(Box.createGlue(), new GridBagConstraints(3,
							GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
							GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					final JButton deleteButton = new JButton(
							new RemoveModeAction(p));
					deleteButton.setPreferredSize(BUTTON_DIMENSION);
					panelMode.add(deleteButton, new GridBagConstraints(4,
							GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
							GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					if (Profile.isDefaultMode(p)) {
						textFieldDescription.setEditable(false);
						deleteButton.setEnabled(false);
					}

				}

				panelModeList.add(Box.createGlue(), new GridBagConstraints(0,
						GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
						GridBagConstraints.FIRST_LINE_START,
						GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

				scrollPaneModes.setViewportView(panelModeList);
			}
		});
	}

	private void newProfile() {
		input = new Input(selectedController);

		frmRemoteStickServer.setTitle(rb
				.getString("MAIN_FRAME_TITLE_UNSAVED_PROFILE"));
		updateModesPanel();
	}

	private void stopServer() {
		if (serverThread != null)
			serverThread.closeSocket();
	}

	private boolean loadProfile(File file) {
		boolean result = false;

		try {
			final String jsonString = new String(Files.readAllBytes(file
					.toPath()));
			final Gson gson = new GsonBuilder().registerTypeAdapter(
					IAction.class, new InterfaceAdapter<IAction>()).create();

			final Profile profile = gson.fromJson(jsonString, Profile.class);

			result = Input.setProfile(profile);
			if (result)
				saveLastProfile(file);

			updateModesPanel();
			frmRemoteStickServer.setTitle(file.getName()
					+ rb.getString("MAIN_FRAME_TITLE_SUFFIX"));
			setStatusbarText(rb.getString("STATUS_PROFILE_LOADED")
					+ file.getAbsolutePath());

			return result;
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return result;
	}

	private void saveLastProfile(File file) {
		preferences.put(PREFERENCES_LAST_PROFILE, file.getAbsolutePath());
	}

	public void setStatusbarText(String text) {
		lblStatus.setText(text);
	}

	private class NewProfileAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5703987691203427504L;

		public NewProfileAction() {
			putValue(NAME, rb.getString("NEW_PROFILE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION,
					rb.getString("NEW_PROFILE_ACTION_DESCRIPTION"));
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
			putValue(SHORT_DESCRIPTION,
					rb.getString("OPEN_PROFILE_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			if (fileChooser.showOpenDialog(frmRemoteStickServer) == JFileChooser.APPROVE_OPTION) {
				final File file = fileChooser.getSelectedFile();

				if (!loadProfile(file))
					JOptionPane.showMessageDialog(frmRemoteStickServer,
							rb.getString("COULD_NOT_LOAD_PROFILE_DIALOG_TEXT"),
							rb.getString("ERROR_DIALOG_TITLE"),
							JOptionPane.ERROR_MESSAGE);
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
			putValue(SHORT_DESCRIPTION,
					rb.getString("SAVE_PROFILE_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			if (fileChooser.showSaveDialog(frmRemoteStickServer) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				final String profileFileSuffix = rb
						.getString("PROFILE_FILE_SUFFIX");
				if (!file.getName().toLowerCase().endsWith(profileFileSuffix))
					file = new File(file.getAbsoluteFile() + profileFileSuffix);

				final Gson gson = new GsonBuilder().registerTypeAdapter(
						IAction.class, new InterfaceAdapter<IAction>())
						.create();
				final String jsonString = gson.toJson(Input.getProfile());

				try (FileOutputStream fos = new FileOutputStream(file)) {
					final Writer writer = new BufferedWriter(
							new OutputStreamWriter(fos));
					writer.write(jsonString);
					writer.flush();
					fos.flush();
					fos.close();

					saveLastProfile(file);
					setStatusbarText(rb.getString("STATUS_PROFILE_SAVED")
							+ file.getAbsolutePath());
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
			stopServer();
			System.exit(0);
		}

	}

	private class StartServerAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1758447420975631146L;

		public StartServerAction() {
			putValue(NAME, rb.getString("START_SERVER_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION,
					rb.getString("START_SERVER_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			serverThread = new ServerThread(Main.this, input);
			serverThread.setPort((int) spinnerPort.getValue());
			serverThread
					.setClientTimeout((int) spinnerClientTimeout.getValue());
			serverThread.setUpdateRate((int) spinnerUpdateRate.getValue());
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
			putValue(SHORT_DESCRIPTION,
					rb.getString("STOP_SERVER_ACTION_DESCRIPTION"));
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
			putValue(SHORT_DESCRIPTION,
					rb.getString("SHOW_ABOUT_DIALOG_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(frmRemoteStickServer,
					rb.getString("ABOUT_DIALOG_TEXT"), (String) getValue(NAME),
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
			putValue(
					SHORT_DESCRIPTION,
					rb.getString("SELECT_CONTROLLER_ACTION_DESCRIPTION_PREFIX")
							+ name
							+ rb.getString("SELECT_CONTROLLER_ACTION_DESCRIPTION_SUFFIX"));
		}

		public void actionPerformed(ActionEvent e) {
			selectedController = controller;
			newProfile();
			preferences.put(PREFERENCES_LAST_CONTROLLER, controller.getName());
		}

	}

	private class UpdateAssignmentsPanelThread extends Thread {

		@Override
		public void run() {
			super.run();

			while (true) {
				if (!suspendControllerSettingsUpdate
						&& scrollPaneAssignments.equals(tabbedPane
								.getSelectedComponent())
						&& frmRemoteStickServer.getState() != Frame.ICONIFIED)
					EventQueue.invokeLater(new Runnable() {

						@Override
						public void run() {

							panelAssignments.removeAll();

							final Controller controller = Input.getController();
							if (controller != null) {
								controller.poll();

								for (Component c : controller.getComponents()) {
									final JPanel panelComponent = new JPanel(
											new GridBagLayout());
									panelAssignments
											.add(panelComponent,
													new GridBagConstraints(
															0,
															GridBagConstraints.RELATIVE,
															1,
															1,
															0.0,
															0.0,
															GridBagConstraints.FIRST_LINE_START,
															GridBagConstraints.HORIZONTAL,
															new Insets(0, 0, 0,
																	0), 5, 0));

									final String name = c.getName();
									final float value = c.getPollData();

									final JLabel lblName = new JLabel();
									lblName.setPreferredSize(new Dimension(100,
											15));

									final GridBagConstraints nameGridBagConstraints = new GridBagConstraints(
											0, 0, 1, 1, 0.0, 0.0,
											GridBagConstraints.BASELINE,
											GridBagConstraints.NONE,
											new Insets(0, 0, 0, 0), 0, 0);

									final GridBagConstraints valueGridBagConstraints = new GridBagConstraints(
											2, 0, 1, 1, 1.0, 1.0,
											GridBagConstraints.BASELINE,
											GridBagConstraints.NONE,
											new Insets(0, 0, 0, 0), 0, 0);

									if (c.isAnalog()) {
										lblName.setText(rb
												.getString("AXIS_LABEL") + name);
										panelComponent.add(lblName,
												nameGridBagConstraints);

										panelComponent.add(
												Box.createGlue(),
												new GridBagConstraints(
														1,
														GridBagConstraints.RELATIVE,
														1,
														1,
														1.0,
														1.0,
														GridBagConstraints.BASELINE,
														GridBagConstraints.NONE,
														new Insets(0, 0, 0, 0),
														0, 0));

										final JProgressBar progressBarValue = new JProgressBar(
												-100, 100);
										progressBarValue
												.setValue((int) (value * 100.0f));
										panelComponent.add(progressBarValue,
												valueGridBagConstraints);
									} else {
										lblName.setText(rb
												.getString("BUTTON_LABEL")
												+ name);
										panelComponent.add(lblName,
												nameGridBagConstraints);

										panelComponent.add(
												Box.createGlue(),
												new GridBagConstraints(
														1,
														GridBagConstraints.RELATIVE,
														1,
														1,
														1.0,
														1.0,
														GridBagConstraints.BASELINE,
														GridBagConstraints.NONE,
														new Insets(0, 0, 0, 0),
														0, 0));

										final JLabel lblValue = new JLabel();
										if (value > 0.5f)
											lblValue.setText(rb
													.getString("BUTTON_DOWN_LABEL"));
										else {
											lblValue.setText(rb
													.getString("BUTTON_UP_LABEL"));
											lblValue.setForeground(Color.LIGHT_GRAY);
										}
										panelComponent.add(lblValue,
												valueGridBagConstraints);
									}

									panelComponent.add(
											Box.createGlue(),
											new GridBagConstraints(
													3,
													GridBagConstraints.RELATIVE,
													1,
													1,
													1.0,
													1.0,
													GridBagConstraints.BASELINE,
													GridBagConstraints.NONE,
													new Insets(0, 0, 0, 0), 0,
													0));

									final JButton editButton = new JButton(
											new EditComponentAction(c));
									editButton
											.setPreferredSize(BUTTON_DIMENSION);
									editButton
											.addMouseListener(new MouseListener() {

												@Override
												public void mouseReleased(
														MouseEvent e) {
												}

												@Override
												public void mousePressed(
														MouseEvent e) {
													suspendControllerSettingsUpdate = true;
												}

												@Override
												public void mouseExited(
														MouseEvent e) {
												}

												@Override
												public void mouseEntered(
														MouseEvent e) {
												}

												@Override
												public void mouseClicked(
														MouseEvent e) {
												}
											});
									panelComponent
											.add(editButton,
													new GridBagConstraints(
															4,
															GridBagConstraints.RELATIVE,
															1,
															1,
															0.0,
															0.0,
															GridBagConstraints.BASELINE,
															GridBagConstraints.NONE,
															new Insets(0, 0, 0,
																	0), 0, 0));
								}

								panelAssignments.add(
										Box.createGlue(),
										new GridBagConstraints(
												0,
												GridBagConstraints.RELATIVE,
												1,
												1,
												1.0,
												1.0,
												GridBagConstraints.FIRST_LINE_START,
												GridBagConstraints.NONE,
												new Insets(0, 0, 0, 0), 0, 0));
							}

							scrollPaneAssignments
									.setViewportView(panelAssignments);
						}
					});

				try {
					Thread.sleep(ASSIGNMENTS_PANEL_UPDATE_RATE);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
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
			putValue(
					SHORT_DESCRIPTION,
					rb.getString("EDIT_COMPONENT_ACTION_DESCRIPTION_PREFIX")
							+ component.getName()
							+ rb.getString("EDIT_COMPONENT_ACTION_DESCRIPTION_SUFFIX"));
		}

		public void actionPerformed(ActionEvent e) {
			final EditActionsDialog editComponentDialog = new EditActionsDialog(
					component, input);
			editComponentDialog.setVisible(true);

			suspendControllerSettingsUpdate = false;
		}

	}

	private class SetModeDescriptionAction extends AbstractAction implements
			FocusListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6706537047137827688L;

		private final Mode mode;
		private final JTextField modeDescriptionTextField;

		public SetModeDescriptionAction(Mode mode,
				JTextField modeDescriptionTextField) {
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

			if (description != null && description.length() > 0)
				mode.setDescription(description);
			else
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
			putValue(
					SHORT_DESCRIPTION,
					rb.getString("REMOVE_MODE_ACTION_DESCRIPTION_PREFIX")
							+ mode.getDescription()
							+ rb.getString("REMOVE_MODE_ACTION_DESCRIPTION_SUFFIX"));
		}

		public void actionPerformed(ActionEvent e) {
			Input.getProfile().getModes().remove(mode);
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
			putValue(SHORT_DESCRIPTION,
					rb.getString("ADD_MODE_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			final Mode mode = new Mode();
			Input.getProfile().getModes().add(mode);

			updateModesPanel();
		}

	}

	private class InterfaceAdapter<T> implements JsonSerializer<T>,
			JsonDeserializer<T> {

		public static final String PROPERTY_TYPE = "type";
		public static final String PROPERTY_DATA = "data";

		public JsonElement serialize(T object,
				java.lang.reflect.Type interfaceType,
				JsonSerializationContext context) {
			final JsonObject wrapper = new JsonObject();
			wrapper.addProperty(PROPERTY_TYPE, object.getClass().getName());
			wrapper.add(PROPERTY_DATA, context.serialize(object));

			return wrapper;
		}

		public T deserialize(JsonElement elem,
				java.lang.reflect.Type interfaceType,
				JsonDeserializationContext context) throws JsonParseException {
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
						"No member '"
								+ memberName
								+ "' found in what was expected to be an interface wrapper");
			return elem;
		}

	}

}

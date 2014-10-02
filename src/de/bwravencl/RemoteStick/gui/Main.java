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
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import de.bwravencl.RemoteStick.ServerThread;
import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.Profile;

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

import java.io.File;
import java.util.List;
import java.util.UUID;

import javax.swing.JSpinner;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;

import javax.swing.border.MatteBorder;

public class Main {

	public static final long ASSIGNMENTS_PANEL_UPDATE_RATE = 100L;

	private Input input;
	private ServerThread serverThread;

	private JFrame frmRemotestickserver;
	private JTabbedPane tabbedPane;
	private JScrollPane scrollPaneAssignments;
	private JMenu mnController;
	private JPanel panelProfileList;
	private JPanel panelAssignments;
	private JSpinner spinnerPort;
	private JSpinner spinnerClientTimeout;
	private JSpinner spinnerUpdateRate;
	private JScrollPane scrollPaneProfiles;

	private boolean suspendControllerSettingsUpdate = false;

	private final JFileChooser jFileChooser = new JFileChooser();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main window = new Main();
					window.frmRemotestickserver.setVisible(true);
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

		final Controller[] controllers = ControllerEnvironment
				.getDefaultEnvironment().getControllers();
		for (Controller c : controllers)
			if (c.getType() != Type.KEYBOARD && c.getType() != Type.MOUSE
					&& c.getType() != Type.TRACKBALL
					&& c.getType() != Type.TRACKPAD) {
				input = new Input(c);
				break;
			}

		updateProfilesPanel();

		final Thread updateAssignmentsPanelThread = new UpdateAssignmentsPanelThread();
		updateAssignmentsPanelThread.start();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmRemotestickserver = new JFrame();
		frmRemotestickserver.setTitle("RemoteStick Server");
		frmRemotestickserver.setBounds(100, 100, 650, 600);
		frmRemotestickserver.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final JMenuBar menuBar = new JMenuBar();
		frmRemotestickserver.setJMenuBar(menuBar);

		final JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		mnFile.add(new OpenFileAction());
		mnFile.add(new SaveFileAction());
		mnFile.add(new JSeparator());
		mnFile.add(new QuitAction());

		mnController = new JMenu("Controller");
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

		final JMenu mnServer = new JMenu("Server");
		menuBar.add(mnServer);

		final ButtonGroup buttonGroupServerState = new ButtonGroup();

		final JRadioButtonMenuItem rdbtnmntmRun = new JRadioButtonMenuItem(
				"Run");
		rdbtnmntmRun.setAction(new StartServerAction());
		buttonGroupServerState.add(rdbtnmntmRun);
		mnServer.add(rdbtnmntmRun);

		final JRadioButtonMenuItem rdbtnmntmStop = new JRadioButtonMenuItem(
				"Stop");
		rdbtnmntmStop.setAction(new StopServerAction());
		rdbtnmntmStop.setSelected(true);
		buttonGroupServerState.add(rdbtnmntmStop);
		mnServer.add(rdbtnmntmStop);
		frmRemotestickserver.getContentPane().setLayout(
				new BoxLayout(frmRemotestickserver.getContentPane(),
						BoxLayout.X_AXIS));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmRemotestickserver.getContentPane().add(tabbedPane);

		final JPanel panelProfiles = new JPanel(new BorderLayout());
		tabbedPane.addTab("Profiles", null, panelProfiles, null);

		panelProfileList = new JPanel();
		panelProfileList.setLayout(new GridBagLayout());

		scrollPaneProfiles = new JScrollPane();
		scrollPaneProfiles.setViewportBorder(BorderFactory.createMatteBorder(
				10, 10, 0, 10, panelProfileList.getBackground()));
		panelProfiles.add(scrollPaneProfiles, BorderLayout.CENTER);

		final JPanel panelAddProfile = new JPanel(new FlowLayout(
				FlowLayout.RIGHT));
		final JButton buttonAddProfile = new JButton(new AddProfileAction());
		panelAddProfile.add(buttonAddProfile);
		panelProfiles.add(panelAddProfile, BorderLayout.SOUTH);

		panelAssignments = new JPanel();
		panelAssignments.setLayout(new GridBagLayout());

		scrollPaneAssignments = new JScrollPane();
		scrollPaneAssignments.setViewportBorder(BorderFactory
				.createMatteBorder(10, 10, 0, 10,
						panelAssignments.getBackground()));
		tabbedPane.addTab("Assignments", null, scrollPaneAssignments, null);

		final JPanel panelServerSettings = new JPanel();
		panelServerSettings.setBorder(new MatteBorder(10, 10, 10, 10,
				panelServerSettings.getBackground()));
		tabbedPane.addTab("Server Settings", null, panelServerSettings, null);
		panelServerSettings.setLayout(new BoxLayout(panelServerSettings,
				BoxLayout.Y_AXIS));

		final JPanel panelPort = new JPanel(new FlowLayout(FlowLayout.LEFT, 10,
				0));
		panelServerSettings.add(panelPort);

		final JLabel lblPort = new JLabel("Port");
		panelPort.add(lblPort);

		spinnerPort = new JSpinner(new SpinnerNumberModel(
				ServerThread.DEFAULT_PORT, 1024, 65535, 1));
		panelPort.add(spinnerPort);

		final JPanel panelTimeout = new JPanel(new FlowLayout(FlowLayout.LEFT,
				10, 0));
		panelServerSettings.add(panelTimeout);

		final JLabel lblClientTimeout = new JLabel("Client Timeout");
		panelTimeout.add(lblClientTimeout);

		spinnerClientTimeout = new JSpinner(new SpinnerNumberModel(
				ServerThread.DEFAULT_CLIENT_TIMEOUT, 10, 60000, 1));
		panelTimeout.add(spinnerClientTimeout);

		final JPanel panelUpdateRate = new JPanel(new FlowLayout(
				FlowLayout.LEFT, 10, 0));
		panelServerSettings.add(panelUpdateRate);

		final JLabel lblUpdateRate = new JLabel("Update Rate");
		panelUpdateRate.add(lblUpdateRate);

		spinnerUpdateRate = new JSpinner(new SpinnerNumberModel(
				(int) ServerThread.DEFAULT_UPDATE_RATE, 1, 1000, 1));
		panelUpdateRate.add(spinnerUpdateRate);

		final FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Controller Profiles", "json");
		jFileChooser.setFileFilter(filter);
	}

	private void updateProfilesPanel() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				panelProfileList.removeAll();

				final List<Profile> profiles = input.getProfiles();
				for (Profile p : profiles) {
					final JPanel panelProfile = new JPanel(new GridBagLayout());
					panelProfileList.add(panelProfile, new GridBagConstraints(
							0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
							GridBagConstraints.FIRST_LINE_START,
							GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0,
									0), 0, 5));

					final JLabel lblProfileNo = new JLabel("Profile "
							+ profiles.indexOf(p));
					lblProfileNo.setPreferredSize(new Dimension(100, 15));
					panelProfile.add(lblProfileNo, new GridBagConstraints(0, 0,
							1, 1, 0.0, 0.0, GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					panelProfile.add(Box.createGlue(), new GridBagConstraints(
							1, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
							GridBagConstraints.BASELINE,
							GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
							0));

					final JTextField textFieldDescription = new JTextField(p
							.getDescription(), 20);
					panelProfile.add(textFieldDescription,
							new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0,
									GridBagConstraints.BASELINE,
									GridBagConstraints.NONE, new Insets(0, 0,
											0, 0), 0, 0));

					if (p.getUuid()
							.equals(UUID
									.fromString(Profile.DEFAULT_PROFILE_UUID_STRING))) {
						textFieldDescription.setEnabled(false);

						panelProfile.add(Box.createGlue(),
								new GridBagConstraints(3,
										GridBagConstraints.RELATIVE, 1, 1, 1.0,
										1.0, GridBagConstraints.BASELINE,
										GridBagConstraints.NONE, new Insets(0,
												0, 0, 0), 0, 0));

						panelProfile.add(Box.createGlue(),
								new GridBagConstraints(4,
										GridBagConstraints.RELATIVE, 1, 1, 1.0,
										1.0, GridBagConstraints.BASELINE,
										GridBagConstraints.NONE, new Insets(0,
												0, 0, 0), 0, 0));
					} else {
						final setProfileDescriptionAction setProfileDescriptionAction = new setProfileDescriptionAction(
								p, textFieldDescription);
						textFieldDescription
								.addActionListener(setProfileDescriptionAction);
						textFieldDescription
								.addFocusListener(setProfileDescriptionAction);

						panelProfile.add(Box.createGlue(),
								new GridBagConstraints(3,
										GridBagConstraints.RELATIVE, 1, 1, 1.0,
										1.0, GridBagConstraints.BASELINE,
										GridBagConstraints.NONE, new Insets(0,
												0, 0, 0), 0, 0));

						final JButton deleteProfileButton = new JButton(
								new DeleteProfileAction(p));
						panelProfile.add(deleteProfileButton,
								new GridBagConstraints(4,
										GridBagConstraints.RELATIVE, 1, 1, 0.0,
										0.0, GridBagConstraints.BASELINE,
										GridBagConstraints.NONE, new Insets(0,
												0, 0, 0), 0, 0));
					}
				}

				panelProfileList.add(Box.createGlue(), new GridBagConstraints(
						0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
						GridBagConstraints.FIRST_LINE_START,
						GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

				scrollPaneProfiles.setViewportView(panelProfileList);
			}
		});
	}

	private void stopServer() {
		if (serverThread != null)
			serverThread.stopServer();
	}

	private class OpenFileAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public OpenFileAction() {
			putValue(NAME, "Open");
			putValue(SHORT_DESCRIPTION,
					"Loads a controller configuration from a file");
		}

		public void actionPerformed(ActionEvent e) {
			if (jFileChooser.showOpenDialog(frmRemotestickserver) == JFileChooser.APPROVE_OPTION) {
				final File file = jFileChooser.getSelectedFile();
			}
		}
	}

	private class SaveFileAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public SaveFileAction() {
			putValue(NAME, "Save");
			putValue(SHORT_DESCRIPTION,
					"Saves the controller configuration to a file");
		}

		public void actionPerformed(ActionEvent e) {
			if (jFileChooser.showSaveDialog(frmRemotestickserver) == JFileChooser.APPROVE_OPTION) {
				final File file = jFileChooser.getSelectedFile();
			}
		}
	}

	private class QuitAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public QuitAction() {
			putValue(NAME, "Quit");
			putValue(SHORT_DESCRIPTION, "Quits the application");
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
		private static final long serialVersionUID = 1L;

		public StartServerAction() {
			putValue(NAME, "Start");
			putValue(SHORT_DESCRIPTION, "Starts the server");
		}

		public void actionPerformed(ActionEvent e) {
			serverThread = new ServerThread(input);
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
		private static final long serialVersionUID = 1L;

		public StopServerAction() {
			putValue(NAME, "Stop");
			putValue(SHORT_DESCRIPTION, "Stops the server");
		}

		public void actionPerformed(ActionEvent e) {
			stopServer();
		}
	}

	private class SelectControllerAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final Controller controller;

		public SelectControllerAction(Controller controller) {
			this.controller = controller;

			final String name = controller.getName();
			putValue(NAME, name);
			putValue(SHORT_DESCRIPTION, "Selects '" + name
					+ "' as the active controller");
		}

		public void actionPerformed(ActionEvent e) {
			input = new Input(controller);
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
						&& frmRemotestickserver.getState() != Frame.ICONIFIED)
					EventQueue.invokeLater(new Runnable() {

						@Override
						public void run() {

							panelAssignments.removeAll();

							final Controller controller = input.getController();
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
										lblName.setText("Axis: " + name);
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
										lblName.setText("Button: " + name);
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
											lblValue.setText("Down");
										else {
											lblValue.setText("Up");
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
							} else
								panelAssignments.add(new JLabel(
										"No active controller selected!"));

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
		private static final long serialVersionUID = 1L;

		private final Component component;

		public EditComponentAction(Component component) {
			this.component = component;

			putValue(NAME, "Edit");
			putValue(SHORT_DESCRIPTION,
					"Edit actions of the '" + component.getName()
							+ "' component");
		}

		public void actionPerformed(ActionEvent e) {
			final EditComponentDialog editComponentDialog = new EditComponentDialog(
					input, component);
			editComponentDialog.setVisible(true);

			suspendControllerSettingsUpdate = false;
		}
	}

	private class setProfileDescriptionAction extends AbstractAction implements
			FocusListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final Profile profile;
		private final JTextField profileDescriptionTextField;

		public setProfileDescriptionAction(Profile profile,
				JTextField profileDescriptionTextField) {
			this.profile = profile;
			this.profileDescriptionTextField = profileDescriptionTextField;

			putValue(NAME, "Set profile description");
			putValue(SHORT_DESCRIPTION, "Sets the description of a profile");
		}

		public void actionPerformed(ActionEvent e) {
			setProfileDescription();
		}

		@Override
		public void focusGained(FocusEvent e) {
		}

		@Override
		public void focusLost(FocusEvent e) {
			setProfileDescription();
		}

		private void setProfileDescription() {
			final String description = profileDescriptionTextField.getText();

			if (description != null && description.length() > 0)
				profile.setDescription(description);
			else
				profileDescriptionTextField.setText(profile.getDescription());
		}
	}

	private class DeleteProfileAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final Profile profile;

		public DeleteProfileAction(Profile profile) {
			this.profile = profile;

			putValue(NAME, "Delete");
			putValue(SHORT_DESCRIPTION,
					"Delete the '" + profile.getDescription() + "' profile");
		}

		public void actionPerformed(ActionEvent e) {
			input.getProfiles().remove(profile);
			updateProfilesPanel();
		}
	}

	private class AddProfileAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public AddProfileAction() {
			putValue(NAME, "Add Profile");
			putValue(SHORT_DESCRIPTION, "Add a new profile");
		}

		public void actionPerformed(ActionEvent e) {
			final Profile profile = new Profile();
			profile.setDescription("New Profile");
			input.getProfiles().add(profile);

			updateProfilesPanel();
		}
	}

}

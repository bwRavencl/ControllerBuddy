package de.bwravencl.RemoteStick.gui;

import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.BoxLayout;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import de.bwravencl.RemoteStick.ServerThread;

import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.GridLayout;

import javax.swing.JSpinner;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;

public class MainWindow {

	public static final long CONTROLLER_SETTINGS_PANEL_UPDATE_RATE = 100L;

	private ServerThread serverThread;
	private Controller selectedController;

	private JFrame frmRemotestickserver;
	private JMenu mnController;
	private JPanel panelControllerSettings;

	private final ButtonGroup buttonGroupServerState = new ButtonGroup();
	private final Action quitAction = new QuitAction();
	private final Action startServerAction = new StartServerAction();
	private final Action stopServerAction = new StopServerAction();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
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
	public MainWindow() {
		initialize();

		final Controller[] controllers = ControllerEnvironment
				.getDefaultEnvironment().getControllers();
		for (Controller c : controllers)
			if (c.getType() != Type.KEYBOARD && c.getType() != Type.MOUSE
					&& c.getType() != Type.TRACKBALL
					&& c.getType() != Type.TRACKPAD) {
				selectedController = c;
				break;
			}

		Thread queryConrollerThread = new QueryControllerThread();
		queryConrollerThread.start();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmRemotestickserver = new JFrame();
		frmRemotestickserver.setTitle("RemoteStick Server");
		frmRemotestickserver.setBounds(100, 100, 600, 600);
		frmRemotestickserver.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		frmRemotestickserver.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmOpen = new JMenuItem("Open");
		mnFile.add(mntmOpen);

		JMenuItem mntmSave = new JMenuItem("Save");
		mnFile.add(mntmSave);

		mnFile.add(new JSeparator());
		mnFile.add(quitAction);

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

		JMenu mnServer = new JMenu("Server");
		menuBar.add(mnServer);

		JRadioButtonMenuItem rdbtnmntmRun = new JRadioButtonMenuItem("Run");
		rdbtnmntmRun.setAction(startServerAction);
		buttonGroupServerState.add(rdbtnmntmRun);
		mnServer.add(rdbtnmntmRun);

		JRadioButtonMenuItem rdbtnmntmStop = new JRadioButtonMenuItem("Stop");
		rdbtnmntmStop.setAction(stopServerAction);
		rdbtnmntmStop.setSelected(true);
		buttonGroupServerState.add(rdbtnmntmStop);
		mnServer.add(rdbtnmntmStop);
		frmRemotestickserver.getContentPane().setLayout(
				new BoxLayout(frmRemotestickserver.getContentPane(),
						BoxLayout.X_AXIS));

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmRemotestickserver.getContentPane().add(tabbedPane);

		JScrollPane scrollPaneControllerSettings = new JScrollPane();
		tabbedPane.addTab("Controller Settings", null,
				scrollPaneControllerSettings, null);

		panelControllerSettings = new JPanel();
		scrollPaneControllerSettings.setViewportView(panelControllerSettings);
		panelControllerSettings.setLayout(new GridLayout(0, 3, 10, 5));

		JPanel panelServerSettings = new JPanel();
		tabbedPane.addTab("Server Settings", null, panelServerSettings, null);
		panelServerSettings.setLayout(new GridLayout(1, 2, 0, 0));

		JLabel lblPort = new JLabel("Port");
		panelServerSettings.add(lblPort);

		JSpinner spinnerPort = new JSpinner(new SpinnerNumberModel(
				ServerThread.DEFAULT_PORT, 1024, 65535, 1));
		panelServerSettings.add(spinnerPort);
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
			StopServer();
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
			serverThread = new ServerThread();
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
			StopServer();
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
			MainWindow.this.selectedController = controller;
		}
	}

	private class QueryControllerThread extends Thread {

		@Override
		public void run() {
			super.run();

			while (true) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {

						panelControllerSettings.removeAll();
						panelControllerSettings.add(new JLabel("Controller: "));

						if (selectedController != null) {
							panelControllerSettings.add(new JLabel(
									selectedController.getName()));
							panelControllerSettings.add(Box.createGlue());

							panelControllerSettings.add(new JSeparator());
							panelControllerSettings.add(new JSeparator());
							panelControllerSettings.add(new JSeparator());

							selectedController.poll();

							for (Component c : selectedController
									.getComponents()) {
								final String name = c.getName();
								final float value = c.getPollData();

								if (c.isAnalog()) {
									panelControllerSettings.add(new JLabel(
											"Axis: " + name));

									final JProgressBar jProgressBar = new JProgressBar(
											-100, 100);
									jProgressBar
											.setValue((int) (value * 100.0f));
									panelControllerSettings.add(jProgressBar);
								} else {
									panelControllerSettings.add(new JLabel(
											"Button: " + name));

									final JLabel jLabel = new JLabel();
									jLabel.setHorizontalAlignment(SwingConstants.CENTER);
									if (value > 0.5f)
										jLabel.setText("Down");
									else {
										jLabel.setText("Up");
										jLabel.setForeground(Color.LIGHT_GRAY);
									}
									panelControllerSettings.add(jLabel);
								}

								final JButton jButton = new JButton("Edit");
								panelControllerSettings.add(jButton);
							}
						} else
							panelControllerSettings.add(new JLabel(
									"No active controller selected!"));

						panelControllerSettings.validate();
					}
				});

				try {
					Thread.sleep(CONTROLLER_SETTINGS_PANEL_UPDATE_RATE);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void StopServer() {
		if (serverThread != null)
			serverThread.stopServer();
	}

}

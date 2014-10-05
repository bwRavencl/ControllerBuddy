package de.bwravencl.RemoteStick.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.Profile;
import de.bwravencl.RemoteStick.input.action.ButtonToProfileAction;
import de.bwravencl.RemoteStick.input.action.IAction;
import net.java.games.input.Component;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EditComponentDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String ACTION_CLASS_PREFIX = "de.bwravencl.RemoteStick.input.action.";
	public static final String[] AXIS_ACTION_CLASSES = {
			ACTION_CLASS_PREFIX + "AxisToAxisAction",
			ACTION_CLASS_PREFIX + "AxisToButtonAction",
			ACTION_CLASS_PREFIX + "AxisToKeyAction",
			ACTION_CLASS_PREFIX + "AxisToRelativeAxisAction",
			ACTION_CLASS_PREFIX + "AxisToScrollAction" };
	public static final String[] BUTTON_ACTION_CLASSES = {
			ACTION_CLASS_PREFIX + "ButtonToButtonAction",
			ACTION_CLASS_PREFIX + "ButtonToKeyAction",
			ACTION_CLASS_PREFIX + "ButtonToProfileAction",
			ACTION_CLASS_PREFIX + "ButtonToScrollAction" };

	private final JComboBox<Profile> comboBoxProfile;
	private final JButton btnAdd;
	private final JButton btnRemove;
	private final JList<AvailableAction> listAvailableActions = new JList<AvailableAction>();
	private final JList<IAction> listAssignedActions = new JList<IAction>();

	private final Input input;
	private final Component component;
	private final Map<String, ButtonToProfileAction> unsavedComponentToProfileActionMap;
	private final List<Profile> unsavedProfiles;
	private Profile selectedProfile;
	private AvailableAction selectedAvailableAction;
	private IAction selectedAssignedAction;

	/**
	 * Create the dialog.
	 */
	public EditComponentDialog(Input input, Component component) {
		this.input = input;
		this.component = component;

		unsavedComponentToProfileActionMap = new HashMap<String, ButtonToProfileAction>();
		for (Map.Entry<String, ButtonToProfileAction> e : input
				.getComponentToProfileActionMap().entrySet())
			try {
				unsavedComponentToProfileActionMap.put(e.getKey(),
						(ButtonToProfileAction) e.getValue().clone());
			} catch (CloneNotSupportedException e1) {
				e1.printStackTrace();
			}

		unsavedProfiles = new ArrayList<Profile>();
		for (Profile p : input.getProfiles())
			try {
				unsavedProfiles.add((Profile) p.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}

		setModal(true);
		setTitle("Component Editor '" + component.getName() + "'");
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());

		final JPanel panelProfile = new JPanel(new FlowLayout());
		getContentPane().add(panelProfile, BorderLayout.NORTH);

		final JLabel lblProfile = new JLabel("Profile");
		panelProfile.add(lblProfile);

		selectedProfile = unsavedProfiles.get(0);
		comboBoxProfile = new JComboBox<Profile>(
				unsavedProfiles.toArray(new Profile[unsavedProfiles.size()]));
		comboBoxProfile.addActionListener(new SelectProfileAction());
		panelProfile.add(comboBoxProfile);

		final JPanel panelActions = new JPanel(new GridBagLayout());
		panelActions.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panelActions, BorderLayout.CENTER);

		final JLabel lblAvailableActions = new JLabel("Available Actions");
		panelActions.add(lblAvailableActions, new GridBagConstraints(0, 0, 1,
				1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));

		listAvailableActions
				.addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						selectedAvailableAction = listAvailableActions
								.getSelectedValue();
						btnAdd.setEnabled(true);
					}
				});
		updateAvailableActions();
		panelActions.add(new JScrollPane(listAvailableActions),
				new GridBagConstraints(0, 1, 1, 5, 0.5, 1,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));

		btnAdd = new JButton(new AddActionAction());
		btnAdd.setEnabled(false);
		panelActions.add(btnAdd, new GridBagConstraints(1, 2, 1, 2, 0, 0.25,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
						0, 0, 0, 0), 0, 0));

		btnRemove = new JButton(new RemoveActionAction());
		btnRemove.setEnabled(false);
		panelActions.add(btnRemove, new GridBagConstraints(1, 4, 1, 2, 0, 0.25,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
						0, 0, 0, 0), 0, 0));

		final JLabel lblAssignedActions = new JLabel("Assigned Actions");
		panelActions.add(lblAssignedActions, new GridBagConstraints(2, 0, 1, 1,
				0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));

		listAssignedActions
				.addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						selectedAssignedAction = listAssignedActions
								.getSelectedValue();
						btnRemove.setEnabled(true);
					}
				});
		panelActions.add(new JScrollPane(listAssignedActions),
				new GridBagConstraints(2, 1, 1, 5, .5, 1.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));

		final JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		final JButton btnOK = new JButton(new OKAction());
		btnOK.setActionCommand("OK");
		buttonPane.add(btnOK);
		getRootPane().setDefaultButton(btnOK);

		final JButton btnCancel = new JButton(new CancelAction());
		btnCancel.setActionCommand("Cancel");
		buttonPane.add(btnCancel);

		updateAssignedActions();
	}

	private void updateAvailableActions() {
		boolean hasProfileAction = false;
		for (IAction a : getAssignedActions())
			if (a instanceof ButtonToProfileAction)
				hasProfileAction = true;

		final List<AvailableAction> availableActions = new ArrayList<AvailableAction>();

		for (String s : component.isAnalog() ? AXIS_ACTION_CLASSES
				: BUTTON_ACTION_CLASSES) {
			final AvailableAction availableAction = new AvailableAction(s);
			if (!hasProfileAction
					|| (hasProfileAction && !ButtonToProfileAction.class
							.getName().equals(availableAction.className)))
				availableActions.add(availableAction);
		}

		listAvailableActions.setListData(availableActions
				.toArray(new AvailableAction[availableActions.size()]));
	}

	private IAction[] getAssignedActions() {
		Set<IAction> assignedActions = selectedProfile
				.getComponentToActionMap().get(component.getName());
		if (assignedActions == null)
			assignedActions = new HashSet<IAction>();

		final ButtonToProfileAction buttonToProfileAction = unsavedComponentToProfileActionMap
				.get(component.getName());
		if (buttonToProfileAction != null)
			assignedActions.add(buttonToProfileAction);

		return (IAction[]) assignedActions.toArray(new IAction[assignedActions
				.size()]);
	}

	private void updateAssignedActions() {
		listAssignedActions.setListData(getAssignedActions());
		btnRemove.setEnabled(false);
	}

	void closeDialog() {
		setVisible(false);
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private class SelectProfileAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public SelectProfileAction() {
			putValue(NAME, "Select Profile");
			putValue(SHORT_DESCRIPTION, "Selects the profile to edit");
		}

		public void actionPerformed(ActionEvent e) {
			selectedProfile = (Profile) comboBoxProfile.getSelectedItem();
			updateAssignedActions();
		}
	}

	private class AddActionAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public AddActionAction() {
			putValue(NAME, "Add");
			putValue(SHORT_DESCRIPTION,
					"Add the selected action to the component");
		}

		public void actionPerformed(ActionEvent e) {
			try {
				final Class<?> clazz = Class
						.forName(selectedAvailableAction.className);
				final IAction action = (IAction) clazz.newInstance();

				if (action instanceof ButtonToProfileAction)
					unsavedComponentToProfileActionMap.put(component.getName(),
							(ButtonToProfileAction) action);
				else {
					final Map<String, HashSet<IAction>> componentToActionMap = selectedProfile
							.getComponentToActionMap();
					final String componentName = component.getName();

					if (componentToActionMap.get(componentName) == null)
						componentToActionMap.put(componentName,
								new HashSet<IAction>());

					componentToActionMap.get(componentName).add(action);
				}

				updateAvailableActions();
				updateAssignedActions();
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			}

		}
	}

	private class RemoveActionAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public RemoveActionAction() {
			putValue(NAME, "Remove");
			putValue(SHORT_DESCRIPTION,
					"Remove the selected action to the component");
		}

		public void actionPerformed(ActionEvent e) {
			if (selectedAssignedAction instanceof ButtonToProfileAction)
				unsavedComponentToProfileActionMap.remove(component.getName());
			else {
				final Map<String, HashSet<IAction>> componentToActionMap = selectedProfile
						.getComponentToActionMap();
				final HashSet<IAction> actions = componentToActionMap
						.get(component.getName());
				actions.remove(selectedAssignedAction);

				if (actions.size() == 0)
					componentToActionMap.remove(component.getName());
			}
			updateAvailableActions();
			updateAssignedActions();
		}
	}

	private class OKAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public OKAction() {
			putValue(NAME, "OK");
			putValue(SHORT_DESCRIPTION, "Apply changes");
		}

		public void actionPerformed(ActionEvent e) {
			input.setComponentToProfileActionMap(unsavedComponentToProfileActionMap);
			input.setProfiles(unsavedProfiles);

			closeDialog();
		}
	}

	private class CancelAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public CancelAction() {
			putValue(NAME, "Cancel");
			putValue(SHORT_DESCRIPTION, "Dismiss changes");
		}

		public void actionPerformed(ActionEvent e) {
			closeDialog();
		}
	}

	private class AvailableAction {

		private final String className;

		public AvailableAction(String className) {
			this.className = className;
		}

		@Override
		public String toString() {
			String description = null;

			try {
				final Class<?> clazz = Class.forName(className);
				description = clazz.newInstance().toString();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			return description;
		}

	}

}

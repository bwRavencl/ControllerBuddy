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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultFormatter;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.ScanCode;
import de.bwravencl.controllerbuddy.input.action.AxisToCursorAction.MouseAxis;
import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import net.brockmatt.util.ResourceBundleUtil;
import net.java.games.input.Component;

public class EditActionsDialog extends JDialog {

	private class AddActionAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -7713175853948284887L;

		public AddActionAction() {
			putValue(NAME, rb.getString("ADD_ACTION_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("ADD_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				final Class<?> clazz = Class.forName(selectedAvailableAction.className);
				final IAction action = (IAction) clazz.newInstance();

				if (action instanceof ButtonToModeAction) {
					if (unsavedProfile.getComponentToModeActionMap().get(component.getName()) == null)
						unsavedProfile.getComponentToModeActionMap().put(component.getName(), new ArrayList<>());

					unsavedProfile.getComponentToModeActionMap().get(component.getName())
							.add((ButtonToModeAction) action);
				} else {
					if (isComponentEditor()) {
						final Map<String, List<IAction>> componentToActionMap = selectedMode.getComponentToActionsMap();
						final String componentName = component.getName();

						if (componentToActionMap.get(componentName) == null)
							componentToActionMap.put(componentName, new ArrayList<>());

						componentToActionMap.get(componentName).add(action);
					} else
						cycleActions.add(action);
				}

				updateAvailableActions();
				updateAssignedActions();

				assignedActionsList.setSelectedIndex(assignedActionsList.getLastVisibleIndex()
						- (hasModeAction() && !(action instanceof ButtonToModeAction) ? 1 : 0));
			} catch (final ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (final InstantiationException e1) {
				e1.printStackTrace();
			} catch (final IllegalAccessException e1) {
				e1.printStackTrace();
			}

		}

	}

	private static class AvailableAction {

		private final String className;

		public AvailableAction(String className) {
			this.className = className;
		}

		@Override
		public String toString() {
			String description = "";

			try {
				final Class<?> clazz = Class.forName(className);
				description = clazz.newInstance().toString();
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
			} catch (final InstantiationException e) {
				e.printStackTrace();
			} catch (final IllegalAccessException e) {
				e.printStackTrace();
			}

			return description;
		}

	}

	private class CancelAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 8086810563127997199L;

		public CancelAction() {
			putValue(NAME, UIManager.getLookAndFeelDefaults().get("OptionPane.cancelButtonText"));
			putValue(SHORT_DESCRIPTION, rb.getString("CANCEL_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			closeDialog();
		}

	}

	private class EditActionsAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -6538021954760621595L;

		public EditActionsAction() {
			putValue(NAME, rb.getString("EDIT_ACTIONS_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("EDIT_ACTIONS_ACTION_DESCRIPTION_PREFIX")
					+ selectedAssignedAction.toString() + rb.getString("EDIT_ACTIONS_ACTION_DESCRIPTION_SUFFIX"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			final EditActionsDialog editComponentDialog = new EditActionsDialog(
					(ButtonToCycleAction) selectedAssignedAction, input);
			editComponentDialog.setVisible(true);
		}

	}

	private class JCheckBoxSetPropertyAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -33052386834598414L;

		private final Method setterMethod;

		public JCheckBoxSetPropertyAction(Method setterMethod) {
			this.setterMethod = setterMethod;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				setterMethod.invoke(selectedAssignedAction, ((JCheckBox) e.getSource()).isSelected());
			} catch (final IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (final IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (final InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}

	}

	private class JComboBoxSetPropertyAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1938012378184518954L;

		private final Method setterMethod;

		public JComboBoxSetPropertyAction(Method setterMethod) {
			this.setterMethod = setterMethod;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				setterMethod.invoke(selectedAssignedAction, ((JComboBox<?>) e.getSource()).getSelectedItem());
			} catch (final IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (final IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (final InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}

	}

	private class JListSetPropertyListSelectionListener implements ListSelectionListener {

		private final Method setterMethod;
		private final KeyStroke keyStroke;
		private final boolean modifiers;

		public JListSetPropertyListSelectionListener(Method setterMethod, KeyStroke keyStroke, boolean modifiers) {
			this.setterMethod = setterMethod;
			this.keyStroke = keyStroke;
			this.modifiers = modifiers;
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			try {
				final Set<Integer> scanCodes = new HashSet<>();

				for (final Object o : ((JList<?>) e.getSource()).getSelectedValuesList())
					scanCodes.add(ScanCode.nameToScanCodeMap.get(o));

				final Integer[] scanCodesArray = scanCodes.toArray(new Integer[scanCodes.size()]);

				if (modifiers)
					keyStroke.setModifierCodes(scanCodesArray);
				else
					keyStroke.setKeyCodes(scanCodesArray);

				setterMethod.invoke(selectedAssignedAction, keyStroke);
			} catch (final IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (final IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (final InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}

	}

	private class JSpinnerSetPropertyChangeListener implements ChangeListener {

		private final Method setterMethod;

		public JSpinnerSetPropertyChangeListener(Method setterMethod) {
			this.setterMethod = setterMethod;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			try {
				final Object value = (((JSpinner) e.getSource()).getValue());

				if (value instanceof Double)
					setterMethod.invoke(selectedAssignedAction, ((Double) value).floatValue());
				else
					setterMethod.invoke(selectedAssignedAction, value);
			} catch (final IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (final IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (final InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}

	}

	private class OKAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -6947022759101822700L;

		public OKAction() {
			putValue(NAME, UIManager.getLookAndFeelDefaults().get("OptionPane.okButtonText"));
			putValue(SHORT_DESCRIPTION, rb.getString("OK_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (isComponentEditor()) {
				Input.setProfile(unsavedProfile, input.getController());
				main.setUnsavedChangesTitle();
			} else
				cycleAction.setActions(cycleActions);

			closeDialog();
		}

	}

	private class RemoveActionAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = -5681740772832902238L;

		public RemoveActionAction() {
			putValue(NAME, rb.getString("REMOVE_ACTION_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("REMOVE_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (selectedAssignedAction instanceof ButtonToModeAction) {
				unsavedProfile.getComponentToModeActionMap().get(component.getName()).remove(selectedAssignedAction);
				if (unsavedProfile.getComponentToModeActionMap().get(component.getName()).isEmpty())
					unsavedProfile.getComponentToModeActionMap().remove(component.getName());
			} else {
				if (isComponentEditor()) {
					final Map<String, List<IAction>> componentToActionMap = selectedMode.getComponentToActionsMap();
					final List<IAction> actions = componentToActionMap.get(component.getName());
					actions.remove(selectedAssignedAction);

					if (actions.size() == 0)
						componentToActionMap.remove(component.getName());
				} else
					cycleActions.remove(selectedAssignedAction);
			}

			updateAvailableActions();
			updateAssignedActions();
		}

	}

	/**
	 *
	 */
	private static final long serialVersionUID = 8876286334367723566L;
	private static final String ACTION_CLASS_PREFIX = "de.bwravencl.controllerbuddy.input.action.";
	private static final String[] ACTION_CLASSES_AXIS = { ACTION_CLASS_PREFIX + "AxisToAxisAction",
			ACTION_CLASS_PREFIX + "AxisToButtonAction", ACTION_CLASS_PREFIX + "AxisToCursorAction",
			ACTION_CLASS_PREFIX + "AxisToKeyAction", ACTION_CLASS_PREFIX + "AxisToMouseButtonAction",
			ACTION_CLASS_PREFIX + "AxisToRelativeAxisAction", ACTION_CLASS_PREFIX + "AxisToScrollAction" };
	private static final String[] ACTION_CLASSES_BUTTON = { ACTION_CLASS_PREFIX + "ButtonToButtonAction",
			ACTION_CLASS_PREFIX + "ButtonToCycleAction", ACTION_CLASS_PREFIX + "ButtonToKeyAction",
			ACTION_CLASS_PREFIX + "ButtonToLockKeyAction", ACTION_CLASS_PREFIX + "ButtonToModeAction",
			ACTION_CLASS_PREFIX + "ButtonToMouseButtonAction", ACTION_CLASS_PREFIX + "ButtonToRelativeAxisReset",
			ACTION_CLASS_PREFIX + "ButtonToScrollAction" };
	private static final String[] ACTION_CLASSES_CYCLE_ACTION = { ACTION_CLASS_PREFIX + "ButtonToButtonAction",
			ACTION_CLASS_PREFIX + "ButtonToKeyAction", ACTION_CLASS_PREFIX + "ButtonToMouseButtonAction",
			ACTION_CLASS_PREFIX + "ButtonToRelativeAxisReset", ACTION_CLASS_PREFIX + "ButtonToScrollAction" };
	private static final String ACTION_PROPERTY_GETTER_PREFIX_DEFAULT = "get";
	private static final String ACTION_PROPERTY_GETTER_PREFIX_BOOLEAN = "is";
	private static final String ACTION_PROPERTY_SETTER_PREFIX = "set";
	private static final int DIALOG_BOUNDS_X = Main.DIALOG_BOUNDS_X + Main.DIALOG_BOUNDS_X_Y_OFFSET;
	private static final int DIALOG_BOUNDS_Y = Main.DIALOG_BOUNDS_Y + Main.DIALOG_BOUNDS_X_Y_OFFSET;
	private static final int DIALOG_BOUNDS_WIDTH = 950;
	private static final int DIALOG_BOUNDS_HEIGHT = 510;

	public static int getListModelIndex(ListModel<?> model, Object value) {
		if (value == null)
			return -1;

		if (model instanceof DefaultListModel)
			return ((DefaultListModel<?>) model).indexOf(value);

		for (int i = 0; i < model.getSize(); i++)
			if (value.equals(model.getElementAt(i)))
				return i;

		return -1;
	}

	private Main main;
	private Component component;
	private Input input;
	private Profile unsavedProfile;
	private ButtonToCycleAction cycleAction;
	private final List<IAction> cycleActions = new ArrayList<>();
	private Mode selectedMode;
	private AvailableAction selectedAvailableAction;
	private IAction selectedAssignedAction;
	private final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(Main.STRING_RESOURCE_BUNDLE_BASENAME,
			Locale.getDefault());
	private final JList<AvailableAction> availableActionsList = new JList<>();
	private final JList<IAction> assignedActionsList = new JList<>();

	public EditActionsDialog(ButtonToCycleAction cycleAction, Input input) {
		this.cycleAction = cycleAction;

		try {
			for (final IAction a : cycleAction.getActions())
				cycleActions.add((IAction) a.clone());

			preInit();

			setBounds(DIALOG_BOUNDS_X + Main.DIALOG_BOUNDS_X_Y_OFFSET, DIALOG_BOUNDS_Y + Main.DIALOG_BOUNDS_X_Y_OFFSET,
					DIALOG_BOUNDS_WIDTH, DIALOG_BOUNDS_HEIGHT);
			setTitle(cycleAction.toString() + rb.getString("EDIT_ACTIONS_DIALOG_TITLE_CYCLE_ACTION_EDITOR_SUFFIX"));

			init(input);
		} catch (final CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	public EditActionsDialog(Main main, Component component, Input input) {
		super(main.getFrame());
		this.main = main;
		this.component = component;

		try {
			unsavedProfile = (Profile) Input.getProfile().clone();

			preInit();

			setBounds(DIALOG_BOUNDS_X, DIALOG_BOUNDS_Y, DIALOG_BOUNDS_WIDTH, DIALOG_BOUNDS_HEIGHT);
			setTitle(rb.getString("EDIT_ACTIONS_DIALOG_TITLE_COMPONENT_EDITOR_PREFIX") + component.getName());

			final JPanel modePanel = new JPanel(new FlowLayout());
			getContentPane().add(modePanel, BorderLayout.NORTH);

			modePanel.add(new JLabel(rb.getString("MODE_LABEL")));

			final List<Mode> modes = unsavedProfile.getModes();
			selectedMode = modes.get(0);
			final JComboBox<Mode> modeComboBox = new JComboBox<>(modes.toArray(new Mode[modes.size()]));
			modeComboBox.addActionListener(new AbstractAction() {

				/**
				 *
				 */
				private static final long serialVersionUID = -9107064465015662054L;

				@Override
				public void actionPerformed(ActionEvent e) {
					selectedMode = (Mode) modeComboBox.getSelectedItem();
					updateAssignedActions();
					updateAvailableActions();
				}
			});
			modePanel.add(modeComboBox);

			init(input);
		} catch (final CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	void closeDialog() {
		setVisible(false);
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private IAction[] getAssignedActions() {
		final List<IAction> assignedActions;
		if (isComponentEditor())
			assignedActions = selectedMode.getComponentToActionsMap().get(component.getName());
		else
			assignedActions = cycleActions;

		final List<IAction> clonedAssignedActions = new ArrayList<>();
		if (assignedActions != null)
			clonedAssignedActions.addAll(assignedActions);

		if (isComponentEditor() && Profile.isDefaultMode(selectedMode)) {
			final List<ButtonToModeAction> buttonToModeActions = unsavedProfile.getComponentToModeActionMap()
					.get(component.getName());
			if (buttonToModeActions != null) {
				for (final ButtonToModeAction a : buttonToModeActions)
					clonedAssignedActions.add(a);
			}
		}

		return clonedAssignedActions.toArray(new IAction[clonedAssignedActions.size()]);
	}

	private boolean hasModeAction() {
		boolean hasModeAction = false;

		for (final IAction a : getAssignedActions())
			if (a instanceof ButtonToModeAction)
				hasModeAction = true;

		return hasModeAction;
	}

	private void init(Input input) {
		this.input = input;

		final JPanel actionsPanel = new JPanel(new GridBagLayout());
		actionsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(actionsPanel, BorderLayout.CENTER);

		actionsPanel.add(new JLabel(rb.getString("AVAILABLE_ACTIONS_LABEL")), new GridBagConstraints(0, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 25));

		final JButton addButton = new JButton(new AddActionAction());
		addButton.setPreferredSize(Main.BUTTON_DIMENSION);
		addButton.setEnabled(false);
		actionsPanel.add(addButton, new GridBagConstraints(1, 2, 1, 2, 0.0, 0.25, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		final JButton removeButton = new JButton(new RemoveActionAction());
		removeButton.setPreferredSize(Main.BUTTON_DIMENSION);
		removeButton.setEnabled(false);
		actionsPanel.add(removeButton, new GridBagConstraints(1, 4, 1, 2, 0.0, 0.25, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		availableActionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		availableActionsList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				selectedAvailableAction = availableActionsList.getSelectedValue();
				if (selectedAvailableAction == null)
					addButton.setEnabled(false);
				else
					addButton.setEnabled(true);
			}
		});
		updateAvailableActions();
		actionsPanel.add(new JScrollPane(availableActionsList), new GridBagConstraints(0, 1, 1, 5, 0.25, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		actionsPanel.add(new JLabel(rb.getString("ASSIGNED_ACTIONS_LABEL")), new GridBagConstraints(2, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 25));

		final JLabel propertiesLabel = new JLabel(rb.getString("PROPERTIES_LABEL"));
		propertiesLabel.setVisible(false);
		actionsPanel.add(propertiesLabel, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 25));

		final JScrollPane propertiesScrollPane = new JScrollPane();
		propertiesScrollPane.setVisible(false);
		actionsPanel.add(propertiesScrollPane, new GridBagConstraints(3, 1, 1, 5, 0.5, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		assignedActionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		assignedActionsList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				selectedAssignedAction = assignedActionsList.getSelectedValue();
				if (selectedAssignedAction == null)
					removeButton.setEnabled(false);
				else
					removeButton.setEnabled(true);

				EventQueue.invokeLater(new Runnable() {

					@Override
					public void run() {
						if (selectedAssignedAction == null) {
							propertiesLabel.setVisible(false);
							propertiesScrollPane.setVisible(false);
						} else {
							propertiesLabel.setVisible(true);

							final JPanel propertiesPanel = new JPanel(new GridBagLayout());

							for (final Method m : selectedAssignedAction.getClass().getMethods()) {
								final String methodDescription = m.toGenericString();
								String methodName = methodDescription.substring(0, methodDescription.indexOf('('));
								methodName = methodName.substring(methodName.lastIndexOf('.') + 1);

								if (methodName.startsWith(ACTION_PROPERTY_SETTER_PREFIX)) {
									final String propertyName = methodName
											.substring(methodName.indexOf(ACTION_PROPERTY_SETTER_PREFIX)
													+ ACTION_PROPERTY_SETTER_PREFIX.length());
									String parameterType = methodDescription.substring(
											methodDescription.indexOf('(') + 1, methodDescription.indexOf(')'));
									if (parameterType.contains("<"))
										parameterType = parameterType.substring(0, parameterType.indexOf('<'));

									final Class<?> clazz;
									try {
										clazz = Class.forName(parameterType);

										final Method getterMethod = selectedAssignedAction.getClass().getMethod(
												(clazz == Boolean.class ? ACTION_PROPERTY_GETTER_PREFIX_BOOLEAN
														: ACTION_PROPERTY_GETTER_PREFIX_DEFAULT) + propertyName);

										final JPanel propertyPanel = new JPanel(
												new FlowLayout(FlowLayout.LEADING, 10, 0));
										propertiesPanel.add(propertyPanel,
												new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
														GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
														new Insets(0, 0, 0, 0), 0, 10));

										final JLabel propertyNameLabel = new JLabel(propertyName);
										propertyNameLabel.setPreferredSize(new Dimension(100, 15));
										propertyPanel.add(propertyNameLabel);

										if (Boolean.class == clazz) {
											final JCheckBox checkBox = new JCheckBox(new JCheckBoxSetPropertyAction(m));
											if (!isComponentEditor() && "LongPress".equals(propertyName)) {
												m.invoke(selectedAssignedAction, false);
												checkBox.setSelected(false);
												checkBox.setEnabled(false);
											} else if (!isComponentEditor() && "DownUp".equals(propertyName)) {
												m.invoke(selectedAssignedAction, true);
												checkBox.setSelected(true);
												checkBox.setEnabled(false);
											} else
												checkBox.setSelected(
														(boolean) getterMethod.invoke(selectedAssignedAction));
											propertyPanel.add(checkBox);
										} else if (Integer.class == clazz) {
											final int value = (int) getterMethod.invoke(selectedAssignedAction);

											final SpinnerNumberModel model;
											if ("Clicks".equals(propertyName))
												model = new SpinnerNumberModel(value, 1, 20, 1);
											else if ("MouseButton".equals(propertyName))
												model = new SpinnerNumberModel(value, 1, 3, 1);
											else
												model = new SpinnerNumberModel(value, 0, Input.MAX_N_BUTTONS, 1);

											final JSpinner spinner = new JSpinner(model);
											final JComponent editor = spinner.getEditor();
											final JFormattedTextField textField = ((JSpinner.DefaultEditor) editor)
													.getTextField();
											textField.setColumns(2);
											final DefaultFormatter formatter = (DefaultFormatter) textField
													.getFormatter();
											formatter.setCommitsOnValidEdit(true);
											spinner.addChangeListener(new JSpinnerSetPropertyChangeListener(m));
											propertyPanel.add(spinner);
										} else if (Float.class == clazz) {
											final float value = (float) getterMethod.invoke(selectedAssignedAction);

											final SpinnerNumberModel model;
											if ("ActivationValue".equals(propertyName))
												model = new SpinnerNumberModel(value, 0.0, 1.0, 0.01);
											else if ("DeadZone".equals(propertyName))
												model = new SpinnerNumberModel(value, 0.0, 1.0, 0.01);
											else if ("Exponent".equals(propertyName))
												model = new SpinnerNumberModel(value, 1.0, 5.0, 0.1);
											else if ("MinAxisValue".equals(propertyName)
													|| "MaxAxisValue".equals(propertyName))
												model = new SpinnerNumberModel(value, -1.0, 1.0, 0.01);
											else if ("MaxCursorSpeed".equals(propertyName))
												model = new SpinnerNumberModel(value, 100.0, 10000.0, 1.0);
											else if ("MaxRelativeSpeed".equals(propertyName))
												model = new SpinnerNumberModel(value, 0.1, 100.0, 0.01);
											else
												model = new SpinnerNumberModel(value, -1.0, 1.0, 0.05);

											final JSpinner spinner = new JSpinner(model);
											final JComponent editor = spinner.getEditor();
											final JFormattedTextField textField = ((JSpinner.DefaultEditor) editor)
													.getTextField();
											textField.setColumns(4);
											final DefaultFormatter formatter = (DefaultFormatter) textField
													.getFormatter();
											formatter.setCommitsOnValidEdit(true);
											spinner.addChangeListener(new JSpinnerSetPropertyChangeListener(m));
											propertyPanel.add(spinner);
											if (!isComponentEditor() && "ActivationValue".equals(propertyName)) {
												final float parentActivationValue = cycleAction.getActivationValue();
												m.invoke(selectedAssignedAction, parentActivationValue);
												spinner.setValue(parentActivationValue);
												spinner.setEnabled(false);
											}
										} else if (Mode.class == clazz) {
											final JComboBox<Mode> comboBox = new JComboBox<>();
											for (final Mode p : Input.getProfile().getModes())
												if (!Profile.isDefaultMode(p))
													comboBox.addItem(p);
											comboBox.setAction(new JComboBoxSetPropertyAction(m));
											comboBox.setSelectedItem(getterMethod.invoke(selectedAssignedAction));
											propertyPanel.add(comboBox);
										} else if (VirtualAxis.class == clazz) {
											final JComboBox<VirtualAxis> comboBox = new JComboBox<>(
													VirtualAxis.values());
											comboBox.setAction(new JComboBoxSetPropertyAction(m));
											comboBox.setSelectedItem(getterMethod.invoke(selectedAssignedAction));
											propertyPanel.add(comboBox);
										} else if (MouseAxis.class == clazz) {
											final JComboBox<MouseAxis> comboBox = new JComboBox<>(MouseAxis.values());
											comboBox.setAction(new JComboBoxSetPropertyAction(m));
											comboBox.setSelectedItem(getterMethod.invoke(selectedAssignedAction));
											propertyPanel.add(comboBox);
										} else if (LockKey.class == clazz) {
											final JComboBox<LockKey> comboBox = new JComboBox<>(LockKey.LOCK_KEYS);
											comboBox.setAction(new JComboBoxSetPropertyAction(m));
											comboBox.setSelectedItem(getterMethod.invoke(selectedAssignedAction));
											propertyPanel.add(comboBox);
										} else if (KeyStroke.class == clazz) {
											final KeyStroke keyStroke = (KeyStroke) getterMethod
													.invoke(selectedAssignedAction);
											final Set<String> availableScanCodes = ScanCode.nameToScanCodeMap.keySet();

											final JPanel modifiersPanel = new JPanel();
											modifiersPanel
													.setLayout(new BoxLayout(modifiersPanel, BoxLayout.PAGE_AXIS));
											final JLabel modifiersLabel = new JLabel(rb.getString("MODIFIERS_LABEL"));
											modifiersLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
											modifiersPanel.add(modifiersLabel);
											modifiersPanel.add(Box.createVerticalStrut(5));
											final JList<String> modifierList = new JList<>(
													availableScanCodes.toArray(new String[availableScanCodes.size()]));
											modifierList.addListSelectionListener(
													new JListSetPropertyListSelectionListener(m, keyStroke, true));

											final List<String> addedModifiers = new ArrayList<>();
											for (final int c : keyStroke.getModifierCodes())
												addedModifiers.add(ScanCode.scanCodeToNameMap.get(c));
											for (final String s : addedModifiers) {
												final int index = getListModelIndex(modifierList.getModel(), s);
												if (index >= 0)
													modifierList.addSelectionInterval(index, index);
											}
											final JScrollPane modifiersScrollPane = new JScrollPane(modifierList);
											modifiersScrollPane.setPreferredSize(new Dimension(175, 200));
											modifiersPanel.add(modifiersScrollPane);
											propertyPanel.add(modifiersPanel);

											final JPanel keysPanel = new JPanel();
											keysPanel.setLayout(new BoxLayout(keysPanel, BoxLayout.PAGE_AXIS));
											final JLabel keysLabel = new JLabel(rb.getString("KEYS_LABEL"));
											keysLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
											keysPanel.add(keysLabel);
											keysPanel.add(Box.createVerticalStrut(5));
											final JList<String> keyList = new JList<>(
													availableScanCodes.toArray(new String[availableScanCodes.size()]));
											keyList.addListSelectionListener(
													new JListSetPropertyListSelectionListener(m, keyStroke, false));
											final List<String> addedKeys = new ArrayList<>();
											for (final int c : keyStroke.getKeyCodes())
												addedKeys.add(ScanCode.scanCodeToNameMap.get(c));
											for (final String s : addedKeys) {
												final int index = getListModelIndex(keyList.getModel(), s);
												if (index >= 0)
													keyList.addSelectionInterval(index, index);
											}
											final JScrollPane keysScrollPane = new JScrollPane(keyList);
											keysScrollPane.setPreferredSize(new Dimension(175, 200));
											keysPanel.add(keysScrollPane);
											propertyPanel.add(keysPanel);

										} else if (List.class == clazz) {
											final JButton editActionsButton = new JButton(new EditActionsAction());
											editActionsButton.setPreferredSize(Main.BUTTON_DIMENSION);
											propertyPanel.add(editActionsButton);
										} else
											throw new Exception(getClass().getName()
													+ ": GUI representation implementation missing for "
													+ clazz.getName());
									} catch (final Exception e) {
										e.printStackTrace();
									}
								}
							}

							propertiesPanel.add(Box.createGlue(),
									new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
											GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0),
											0, 0));

							propertiesScrollPane.setViewportView(propertiesPanel);
							propertiesScrollPane.setVisible(true);
						}
					}
				});
			}
		});
		actionsPanel.add(new JScrollPane(assignedActionsList), new GridBagConstraints(2, 1, 1, 5, 0.25, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		final JButton okButton = new JButton(new OKAction());
		okButton.setPreferredSize(Main.BUTTON_DIMENSION);
		buttonPanel.add(okButton);
		getRootPane().setDefaultButton(okButton);

		final JButton cancelButton = new JButton(new CancelAction());
		cancelButton.setPreferredSize(Main.BUTTON_DIMENSION);
		buttonPanel.add(cancelButton);

		updateAssignedActions();
	}

	private boolean isComponentEditor() {
		return component != null;
	}

	private void preInit() {
		setModal(true);
		getContentPane().setLayout(new BorderLayout());
	}

	private void updateAssignedActions() {
		assignedActionsList.setListData(getAssignedActions());
	}

	private void updateAvailableActions() {
		final List<AvailableAction> availableActions = new ArrayList<>();

		String[] actionClasses;
		if (isComponentEditor()) {
			if (component.isAnalog())
				actionClasses = ACTION_CLASSES_AXIS;
			else
				actionClasses = ACTION_CLASSES_BUTTON;
		} else
			actionClasses = ACTION_CLASSES_CYCLE_ACTION;

		for (final String s : actionClasses) {
			final AvailableAction availableAction = new AvailableAction(s);
			if (ButtonToModeAction.class.getName().equals(availableAction.className)) {
				if (unsavedProfile.getModes().size() > 1) {
					if (Profile.isDefaultMode(selectedMode)
							|| !availableAction.className.endsWith(ButtonToModeAction.class.getName()))
						availableActions.add(availableAction);
				}
			} else
				availableActions.add(availableAction);
		}

		availableActionsList.setListData(availableActions.toArray(new AvailableAction[availableActions.size()]));
	}

}

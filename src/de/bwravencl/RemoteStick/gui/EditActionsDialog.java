/* Copyright (C) 2014  Matteo Hausner
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

package de.bwravencl.RemoteStick.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.AbstractAction;
import javax.swing.Box;
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
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.bwravencl.RemoteStick.input.Input;
import de.bwravencl.RemoteStick.input.Input.VirtualAxis;
import de.bwravencl.RemoteStick.input.KeyStroke;
import de.bwravencl.RemoteStick.input.Mode;
import de.bwravencl.RemoteStick.input.Profile;
import de.bwravencl.RemoteStick.input.action.ButtonToModeAction;
import de.bwravencl.RemoteStick.input.action.ButtonToCycleAction;
import de.bwravencl.RemoteStick.input.action.AxisToCursorAction.MouseAxis;
import de.bwravencl.RemoteStick.input.action.IAction;
import net.brockmatt.util.ResourceBundleUtil;
import net.java.games.input.Component;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class EditActionsDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8876286334367723566L;

	private static final String ACTION_CLASS_PREFIX = "de.bwravencl.RemoteStick.input.action.";
	private static final String[] ACTION_CLASSES_AXIS = {
			ACTION_CLASS_PREFIX + "AxisToAxisAction",
			ACTION_CLASS_PREFIX + "AxisToButtonAction",
			ACTION_CLASS_PREFIX + "AxisToCursorAction",
			ACTION_CLASS_PREFIX + "AxisToKeyAction",
			ACTION_CLASS_PREFIX + "AxisToRelativeAxisAction",
			ACTION_CLASS_PREFIX + "AxisToScrollAction" };
	private static final String[] ACTION_CLASSES_BUTTON = {
			ACTION_CLASS_PREFIX + "ButtonToButtonAction",
			ACTION_CLASS_PREFIX + "ButtonToCycleAction",
			ACTION_CLASS_PREFIX + "ButtonToKeyAction",
			ACTION_CLASS_PREFIX + "ButtonToModeAction",
			ACTION_CLASS_PREFIX + "ButtonToScrollAction", };
	private static final String[] ACTION_CLASSES_CYCLE_ACTION = {
			ACTION_CLASS_PREFIX + "ButtonToButtonAction",
			ACTION_CLASS_PREFIX + "ButtonToKeyAction",
			ACTION_CLASS_PREFIX + "ButtonToScrollAction" };
	private static final String ACTION_PROPERTY_GETTER_PREFIX_DEFAULT = "get";
	private static final String ACTION_PROPERTY_GETTER_PREFIX_BOOLEAN = "is";
	private static final String ACTION_PROPERTY_SETTER_PREFIX = "set";

	private static final int DIALOG_BOUNDS_X = Main.DIALOG_BOUNDS_X
			+ Main.DIALOG_BOUNDS_X_Y_OFFSET;
	private static final int DIALOG_BOUNDS_Y = Main.DIALOG_BOUNDS_Y
			+ Main.DIALOG_BOUNDS_X_Y_OFFSET;
	private static final int DIALOG_BOUNDS_WIDTH = 950;
	private static final int DIALOG_BOUNDS_HEIGHT = 510;

	private Component component;
	private Input input;
	private Profile unsavedProfile;
	private ButtonToCycleAction cycleAction;
	private final List<IAction> cycleActions = new ArrayList<IAction>();
	private Mode selectedMode;
	private AvailableAction selectedAvailableAction;
	private IAction selectedAssignedAction;
	private final ResourceBundle rb = new ResourceBundleUtil()
			.getResourceBundle(Main.STRING_RESOURCE_BUNDLE_BASENAME,
					Locale.getDefault());

	private JComboBox<Mode> comboBoxMode;
	private final JList<AvailableAction> listAvailableActions = new JList<AvailableAction>();
	private JButton btnAdd;
	private JButton btnRemove;
	private final JList<IAction> listAssignedActions = new JList<IAction>();
	private JLabel lblProperties;
	private JScrollPane scrollPaneProperties;

	/**
	 * Create the dialog.
	 */
	public EditActionsDialog(Component component, Input input) {
		this.component = component;

		try {
			unsavedProfile = (Profile) Input.getProfile().clone();

			preInit();

			setBounds(DIALOG_BOUNDS_X, DIALOG_BOUNDS_Y, DIALOG_BOUNDS_WIDTH,
					DIALOG_BOUNDS_HEIGHT);
			setTitle(rb
					.getString("EDIT_ACTIONS_DIALOG_TITLE_COMPONENT_EDITOR_PREFIX")
					+ component.getName());

			final JPanel panelMode = new JPanel(new FlowLayout());
			getContentPane().add(panelMode, BorderLayout.NORTH);

			panelMode.add(new JLabel(rb.getString("MODE_LABEL")));

			final List<Mode> modes = unsavedProfile.getModes();
			selectedMode = modes.get(0);
			comboBoxMode = new JComboBox<Mode>(modes.toArray(new Mode[modes
					.size()]));
			comboBoxMode.addActionListener(new SelectModeAction());
			panelMode.add(comboBoxMode);

			init(input);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	public EditActionsDialog(ButtonToCycleAction cycleAction, Input input) {
		this.cycleAction = cycleAction;

		try {
			for (IAction a : cycleAction.getActions())
				cycleActions.add((IAction) a.clone());

			preInit();

			setBounds(DIALOG_BOUNDS_X + Main.DIALOG_BOUNDS_X_Y_OFFSET,
					DIALOG_BOUNDS_Y + Main.DIALOG_BOUNDS_X_Y_OFFSET,
					DIALOG_BOUNDS_WIDTH, DIALOG_BOUNDS_HEIGHT);
			setTitle(cycleAction.toString()
					+ rb.getString("EDIT_ACTIONS_DIALOG_TITLE_CYCLE_ACTION_EDITOR_SUFFIX"));

			init(input);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	private void preInit() {
		setModal(true);
		getContentPane().setLayout(new BorderLayout());
	}

	private void init(Input input) {
		this.input = input;

		final JPanel panelActions = new JPanel(new GridBagLayout());
		panelActions.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panelActions, BorderLayout.CENTER);

		panelActions.add(new JLabel(rb.getString("AVAILABLE_ACTIONS_LABEL")),
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 25));

		listAvailableActions
				.addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						selectedAvailableAction = listAvailableActions
								.getSelectedValue();
						if (selectedAvailableAction == null)
							btnAdd.setEnabled(false);
						else
							btnAdd.setEnabled(true);
					}
				});
		updateAvailableActions();
		panelActions.add(new JScrollPane(listAvailableActions),
				new GridBagConstraints(0, 1, 1, 5, 0.25, 1.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));

		btnAdd = new JButton(new AddActionAction());
		btnAdd.setPreferredSize(Main.BUTTON_DIMENSION);
		btnAdd.setEnabled(false);
		panelActions.add(btnAdd, new GridBagConstraints(1, 2, 1, 2, 0.0, 0.25,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
						0, 0, 0, 0), 0, 0));

		btnRemove = new JButton(new RemoveActionAction());
		btnRemove.setPreferredSize(Main.BUTTON_DIMENSION);
		btnRemove.setEnabled(false);
		panelActions.add(btnRemove, new GridBagConstraints(1, 4, 1, 2, 0.0,
				0.25, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));

		panelActions.add(new JLabel(rb.getString("ASSIGNED_ACTIONS_LABEL")),
				new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 25));

		listAssignedActions
				.addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						selectedAssignedAction = listAssignedActions
								.getSelectedValue();
						if (selectedAssignedAction == null)
							btnRemove.setEnabled(false);
						else
							btnRemove.setEnabled(true);

						EventQueue.invokeLater(new Runnable() {

							@Override
							public void run() {
								if (selectedAssignedAction == null) {
									lblProperties.setVisible(false);
									scrollPaneProperties.setVisible(false);
								} else {
									lblProperties.setVisible(true);

									final JPanel panelProperties = new JPanel(
											new GridBagLayout());

									for (Method m : selectedAssignedAction
											.getClass().getMethods()) {
										final String methodDescription = m
												.toGenericString();

										if (methodDescription
												.contains(ACTION_PROPERTY_SETTER_PREFIX)) {
											final String propertyName = methodDescription.substring(
													methodDescription
															.indexOf(ACTION_PROPERTY_SETTER_PREFIX)
															+ ACTION_PROPERTY_SETTER_PREFIX
																	.length(),
													methodDescription
															.indexOf('('));
											String parameterType = methodDescription.substring(
													methodDescription
															.indexOf('(') + 1,
													methodDescription
															.indexOf(')'));
											if (parameterType.contains("<"))
												parameterType = parameterType
														.substring(
																0,
																parameterType
																		.indexOf('<'));

											final Class<?> clazz;
											try {
												clazz = Class
														.forName(parameterType);

												final Method getterMethod = selectedAssignedAction
														.getClass()
														.getMethod(
																(clazz == Boolean.class ? ACTION_PROPERTY_GETTER_PREFIX_BOOLEAN
																		: ACTION_PROPERTY_GETTER_PREFIX_DEFAULT)
																		+ propertyName);

												final JPanel panelProperty = new JPanel(
														new FlowLayout(
																FlowLayout.LEADING,
																10, 0));
												panelProperties
														.add(panelProperty,
																new GridBagConstraints(
																		0,
																		GridBagConstraints.RELATIVE,
																		1,
																		1,
																		0.0,
																		0.0,
																		GridBagConstraints.FIRST_LINE_START,
																		GridBagConstraints.NONE,
																		new Insets(
																				0,
																				0,
																				0,
																				0),
																		0, 10));

												final JLabel lblPropertyName = new JLabel(
														propertyName);
												lblPropertyName
														.setPreferredSize(new Dimension(
																100, 15));
												panelProperty
														.add(lblPropertyName);

												if (Boolean.class == clazz) {
													final JCheckBox checkBox = new JCheckBox(
															new JCheckBoxSetPropertyAction(
																	m));
													checkBox.setSelected((boolean) getterMethod
															.invoke(selectedAssignedAction));
													panelProperty.add(checkBox);
												} else if (Integer.class == clazz) {
													int value = (int) getterMethod
															.invoke(selectedAssignedAction);

													final SpinnerNumberModel model;
													if ("Clicks"
															.equals(propertyName))
														model = new SpinnerNumberModel(
																value, 0, 20, 1);
													else
														model = new SpinnerNumberModel(
																value,
																0,
																input.getnButtons(),
																1);

													final JSpinner spinner = new JSpinner(
															model);
													final JComponent editor = spinner
															.getEditor();
													final JFormattedTextField textField = ((JSpinner.DefaultEditor) editor)
															.getTextField();
													textField.setColumns(2);
													spinner.addChangeListener(new JSpinnerSetPropertyChangeListener(
															m));
													panelProperty.add(spinner);
												} else if (Float.class == clazz) {
													float value = (float) getterMethod
															.invoke(selectedAssignedAction);

													final SpinnerNumberModel model;
													if ("DeadZone"
															.equals(propertyName))
														model = new SpinnerNumberModel(
																value, 0.0,
																1.0, 0.01);
													else if ("MaxSpeed"
															.equals(propertyName))
														model = new SpinnerNumberModel(
																value, 100.0,
																1000.0, 0.1);
													else
														model = new SpinnerNumberModel(
																value, -1.0,
																1.0, 0.05);

													final JSpinner spinner = new JSpinner(
															model);
													final JComponent editor = spinner
															.getEditor();
													final JFormattedTextField textField = ((JSpinner.DefaultEditor) editor)
															.getTextField();
													textField.setColumns(3);
													spinner.addChangeListener(new JSpinnerSetPropertyChangeListener(
															m));
													panelProperty.add(spinner);
												} else if (Mode.class == clazz) {
													final JComboBox<Mode> comboBox = new JComboBox<Mode>();
													for (Mode p : Input
															.getProfile()
															.getModes())
														if (!Profile
																.isDefaultMode(p))
															comboBox.addItem(p);
													comboBox.setAction(new JComboBoxSetPropertyAction(
															m));
													comboBox.setSelectedItem(getterMethod
															.invoke(selectedAssignedAction));
													panelProperty.add(comboBox);
												} else if (VirtualAxis.class == clazz) {
													final JComboBox<VirtualAxis> comboBox = new JComboBox<VirtualAxis>(
															VirtualAxis
																	.values());
													comboBox.setAction(new JComboBoxSetPropertyAction(
															m));
													comboBox.setSelectedItem(getterMethod
															.invoke(selectedAssignedAction));
													panelProperty.add(comboBox);
												} else if (MouseAxis.class == clazz) {
													final JComboBox<MouseAxis> comboBox = new JComboBox<MouseAxis>(
															MouseAxis.values());
													comboBox.setAction(new JComboBoxSetPropertyAction(
															m));
													comboBox.setSelectedItem(getterMethod
															.invoke(selectedAssignedAction));
													panelProperty.add(comboBox);
												} else if (KeyStroke.class == clazz) {
													final int length = KeyStroke.MODIFIER_CODES.length
															+ KeyStroke.KEY_CODES.length;
													final String[] availableCodes = new String[length];
													System.arraycopy(
															KeyStroke.MODIFIER_CODES,
															0,
															availableCodes,
															0,
															KeyStroke.MODIFIER_CODES.length);
													System.arraycopy(
															KeyStroke.KEY_CODES,
															0,
															availableCodes,
															KeyStroke.MODIFIER_CODES.length,
															KeyStroke.KEY_CODES.length);
													final JList<String> listCodes = new JList<String>(
															availableCodes);
													listCodes
															.addListSelectionListener(new JListSetPropertyListSelectionListener(
																	m));
													final KeyStroke keyStroke = (KeyStroke) getterMethod
															.invoke(selectedAssignedAction);
													final List<String> addedCodes = new ArrayList<String>();
													for (String s : keyStroke
															.getModifierCodes())
														addedCodes.add(s);
													for (String s : keyStroke
															.getKeyCodes())
														addedCodes.add(s);
													for (String s : addedCodes) {
														final int index = getListModelIndex(
																listCodes
																		.getModel(),
																s);
														if (index >= 0)
															listCodes
																	.addSelectionInterval(
																			index,
																			index);
													}
													final JScrollPane scrollPane = new JScrollPane(
															listCodes);
													scrollPane
															.setPreferredSize(new Dimension(
																	175, 200));
													panelProperty
															.add(scrollPane);
												} else if (List.class == clazz) {
													final JButton editActionsButton = new JButton(
															new EditActionsAction());
													editActionsButton
															.setPreferredSize(Main.BUTTON_DIMENSION);
													panelProperty
															.add(editActionsButton);
												} else
													throw new Exception(
															"GUI representation implementation missing for "
																	+ clazz.getName());
											} catch (Exception e) {
												e.printStackTrace();
											}
										}
									}

									panelProperties.add(
											Box.createGlue(),
											new GridBagConstraints(
													0,
													GridBagConstraints.RELATIVE,
													1, 1, 1.0, 1.0,
													GridBagConstraints.CENTER,
													GridBagConstraints.NONE,
													new Insets(0, 0, 0, 0), 0,
													0));

									scrollPaneProperties
											.setViewportView(panelProperties);
									scrollPaneProperties.setVisible(true);
								}
							}
						});
					}
				});
		panelActions.add(new JScrollPane(listAssignedActions),
				new GridBagConstraints(2, 1, 1, 5, 0.25, 1.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 0, 0, 0), 0, 0));

		lblProperties = new JLabel(rb.getString("ASSIGNED_ACTIONS_LABEL"));
		lblProperties.setVisible(false);
		panelActions.add(lblProperties, new GridBagConstraints(3, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 25));

		scrollPaneProperties = new JScrollPane();
		scrollPaneProperties.setVisible(false);
		panelActions.add(scrollPaneProperties, new GridBagConstraints(3, 1, 1,
				5, 0.5, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		final JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		final JButton btnOK = new JButton(new OKAction());
		btnOK.setPreferredSize(Main.BUTTON_DIMENSION);
		buttonPane.add(btnOK);
		getRootPane().setDefaultButton(btnOK);

		final JButton btnCancel = new JButton(new CancelAction());
		btnCancel.setPreferredSize(Main.BUTTON_DIMENSION);
		buttonPane.add(btnCancel);

		updateAssignedActions();
	}

	private boolean isComponentEditor() {
		return component != null;
	}

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

	private boolean hasModeAction() {
		boolean hasModeAction = false;

		for (IAction a : getAssignedActions())
			if (a instanceof ButtonToModeAction)
				hasModeAction = true;

		return hasModeAction;
	}

	private void updateAvailableActions() {

		final List<AvailableAction> availableActions = new ArrayList<AvailableAction>();

		String[] actionClasses;
		if (isComponentEditor()) {
			if (component.isAnalog())
				actionClasses = ACTION_CLASSES_AXIS;
			else
				actionClasses = ACTION_CLASSES_BUTTON;
		} else
			actionClasses = ACTION_CLASSES_CYCLE_ACTION;

		for (String s : actionClasses) {
			final AvailableAction availableAction = new AvailableAction(s);
			if (ButtonToModeAction.class.getName().equals(
					availableAction.className)) {
				if (unsavedProfile.getModes().size() > 1 && !hasModeAction())
					availableActions.add(availableAction);
			} else
				availableActions.add(availableAction);
		}

		listAvailableActions.setListData(availableActions
				.toArray(new AvailableAction[availableActions.size()]));
	}

	private IAction[] getAssignedActions() {
		final List<IAction> assignedActions;
		if (isComponentEditor())
			assignedActions = selectedMode.getComponentToActionMap().get(
					component.getName());
		else
			assignedActions = cycleActions;

		final List<IAction> clonedAssignedActions = new ArrayList<IAction>();
		if (assignedActions != null)
			clonedAssignedActions.addAll(assignedActions);

		if (isComponentEditor()) {
			final ButtonToModeAction buttonToModeAction = unsavedProfile
					.getComponentToModeActionMap().get(component.getName());
			if (buttonToModeAction != null)
				clonedAssignedActions.add(buttonToModeAction);
		}

		return (IAction[]) clonedAssignedActions
				.toArray(new IAction[clonedAssignedActions.size()]);
	}

	private void updateAssignedActions() {
		listAssignedActions.setListData(getAssignedActions());
	}

	void closeDialog() {
		setVisible(false);
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private class SelectModeAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -800704681422511116L;

		public void actionPerformed(ActionEvent e) {
			selectedMode = (Mode) comboBoxMode.getSelectedItem();
			updateAssignedActions();
		}

	}

	private class AddActionAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -7713175853948284887L;

		public AddActionAction() {
			putValue(NAME, rb.getString("ADD_ACTION_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION,
					rb.getString("ADD_ACTION_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			try {
				final Class<?> clazz = Class
						.forName(selectedAvailableAction.className);
				final IAction action = (IAction) clazz.newInstance();

				if (action instanceof ButtonToModeAction)
					unsavedProfile.getComponentToModeActionMap().put(
							component.getName(), (ButtonToModeAction) action);
				else {
					if (isComponentEditor()) {
						final Map<String, List<IAction>> componentToActionMap = selectedMode
								.getComponentToActionMap();
						final String componentName = component.getName();

						if (componentToActionMap.get(componentName) == null)
							componentToActionMap.put(componentName,
									new ArrayList<IAction>());

						componentToActionMap.get(componentName).add(action);
					} else
						cycleActions.add(action);
				}

				updateAvailableActions();
				updateAssignedActions();

				listAssignedActions.setSelectedIndex(listAssignedActions
						.getLastVisibleIndex()
						- (hasModeAction()
								&& !(action instanceof ButtonToModeAction) ? 1
								: 0));
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
		private static final long serialVersionUID = -5681740772832902238L;

		public RemoveActionAction() {
			putValue(NAME, rb.getString("REMOVE_ACTION_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION,
					rb.getString("REMOVE_ACTION_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			if (selectedAssignedAction instanceof ButtonToModeAction)
				unsavedProfile.getComponentToModeActionMap().remove(
						component.getName());
			else {
				if (isComponentEditor()) {
					final Map<String, List<IAction>> componentToActionMap = selectedMode
							.getComponentToActionMap();
					final List<IAction> actions = componentToActionMap
							.get(component.getName());
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

	private class JCheckBoxSetPropertyAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -33052386834598414L;

		private final Method setterMethod;

		public JCheckBoxSetPropertyAction(Method setterMethod) {
			this.setterMethod = setterMethod;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				setterMethod.invoke(selectedAssignedAction,
						((JCheckBox) e.getSource()).isSelected());
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
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
				Object value = (((JSpinner) e.getSource()).getValue());

				if (value instanceof Double)
					setterMethod.invoke(selectedAssignedAction,
							((Double) value).floatValue());
				else
					setterMethod.invoke(selectedAssignedAction, value);
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
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

		public void actionPerformed(ActionEvent e) {
			try {
				setterMethod.invoke(selectedAssignedAction,
						((JComboBox<?>) e.getSource()).getSelectedItem());
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}

	}

	private class JListSetPropertyListSelectionListener implements
			ListSelectionListener {

		private final Method setterMethod;

		public JListSetPropertyListSelectionListener(Method setterMethod) {
			this.setterMethod = setterMethod;
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			try {
				final List<String> modifierCodes = new ArrayList<String>();
				final List<String> keyCodes = new ArrayList<String>();

				for (Object o : ((JList<?>) e.getSource())
						.getSelectedValuesList()) {
					if (Arrays.asList(KeyStroke.MODIFIER_CODES).contains(o))
						modifierCodes.add((String) o);
					else
						keyCodes.add((String) o);
				}

				final KeyStroke keyStroke = new KeyStroke();
				keyStroke.setModifierCodes(modifierCodes
						.toArray(new String[modifierCodes.size()]));
				keyStroke.setKeyCodes(keyCodes.toArray(new String[keyCodes
						.size()]));

				setterMethod.invoke(selectedAssignedAction, keyStroke);
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}

	}

	private class EditActionsAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6538021954760621595L;

		public EditActionsAction() {
			putValue(NAME, rb.getString("EDIT_ACTIONS_ACTION_NAME"));
			putValue(
					SHORT_DESCRIPTION,
					rb.getString("EDIT_ACTIONS_ACTION_DESCRIPTION_PREFIX")
							+ selectedAssignedAction.toString()
							+ rb.getString("EDIT_ACTIONS_ACTION_DESCRIPTION_SUFFIX"));
		}

		public void actionPerformed(ActionEvent e) {
			final EditActionsDialog editComponentDialog = new EditActionsDialog(
					(ButtonToCycleAction) selectedAssignedAction, input);
			editComponentDialog.setVisible(true);
		}

	}

	private class OKAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6947022759101822700L;

		public OKAction() {
			putValue(
					NAME,
					UIManager.getLookAndFeelDefaults().get(
							"OptionPane.okButtonText"));
			putValue(SHORT_DESCRIPTION, rb.getString("OK_ACTION_DESCRIPTION"));
		}

		public void actionPerformed(ActionEvent e) {
			if (isComponentEditor())
				Input.setProfile(unsavedProfile);
			else
				cycleAction.setActions(cycleActions);

			closeDialog();
		}

	}

	private class CancelAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 8086810563127997199L;

		public CancelAction() {
			putValue(
					NAME,
					UIManager.getLookAndFeelDefaults().get(
							"OptionPane.cancelButtonText"));
			putValue(SHORT_DESCRIPTION,
					rb.getString("CANCEL_ACTION_DESCRIPTION"));
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

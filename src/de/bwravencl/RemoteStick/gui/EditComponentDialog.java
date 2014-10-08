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
import de.bwravencl.RemoteStick.input.action.CursorAction.MouseAxis;
import de.bwravencl.RemoteStick.input.action.IAction;
import net.java.games.input.Component;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
			ACTION_CLASS_PREFIX + "AxisToScrollAction",
			ACTION_CLASS_PREFIX + "CursorAction" };
	public static final String[] BUTTON_ACTION_CLASSES = {
			ACTION_CLASS_PREFIX + "ButtonToButtonAction",
			ACTION_CLASS_PREFIX + "ButtonToKeyAction",
			ACTION_CLASS_PREFIX + "ButtonToModeAction",
			ACTION_CLASS_PREFIX + "ButtonToScrollAction" };

	public static final String ACTION_PROPERTY_GETTER_PREFIX_DEFAULT = "get";
	public static final String ACTION_PROPERTY_GETTER_PREFIX_BOOLEAN = "is";
	public static final String ACTION_PROPERTY_SETTER_PREFIX = "set";

	private final JComboBox<Mode> comboBoxMode;
	private final JList<AvailableAction> listAvailableActions = new JList<AvailableAction>();
	private final JButton btnAdd;
	private final JButton btnRemove;
	private final JList<IAction> listAssignedActions = new JList<IAction>();
	private final JLabel lblProperties;
	private final JScrollPane scrollPaneProperties;

	private final Component component;
	private Profile unsavedProfile;
	private Mode selectedMode;
	private AvailableAction selectedAvailableAction;
	private IAction selectedAssignedAction;

	/**
	 * Create the dialog.
	 */
	public EditComponentDialog(Input input, Component component) {
		this.component = component;

		try {
			unsavedProfile = (Profile) Input.getProfile().clone();
		} catch (CloneNotSupportedException e1) {
			e1.printStackTrace();
		}

		setModal(true);
		setTitle("Component Editor - " + component.getName());
		setBounds(100, 100, 800, 400);
		getContentPane().setLayout(new BorderLayout());

		final JPanel panelMode = new JPanel(new FlowLayout());
		getContentPane().add(panelMode, BorderLayout.NORTH);

		panelMode.add(new JLabel("Mode"));

		final List<Mode> modes = unsavedProfile.getModes();
		selectedMode = modes.get(0);
		comboBoxMode = new JComboBox<Mode>(
				modes.toArray(new Mode[modes.size()]));
		comboBoxMode.addActionListener(new SelectModeAction());
		panelMode.add(comboBoxMode);

		final JPanel panelActions = new JPanel(new GridBagLayout());
		panelActions.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panelActions, BorderLayout.CENTER);

		panelActions.add(new JLabel("Available Actions"),
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

		panelActions.add(new JLabel("Assigned Actions"),
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
											final String parameterType = methodDescription.substring(
													methodDescription
															.indexOf('(') + 1,
													methodDescription
															.indexOf(')'));

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
																value, 0.0,
																20.0, 0.1);
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
																	100, 100));
													panelProperty
															.add(scrollPane);
												} else {
													System.out.println("Error: "
															+ clazz.getName()
															+ " GUI element not implemented!");
												}
											} catch (ClassNotFoundException e) {
												e.printStackTrace();
											} catch (IllegalAccessException e) {
												e.printStackTrace();
											} catch (IllegalArgumentException e) {
												e.printStackTrace();
											} catch (InvocationTargetException e) {
												e.printStackTrace();
											} catch (NoSuchMethodException e) {
												e.printStackTrace();
											} catch (SecurityException e) {
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

		lblProperties = new JLabel("Properties");
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

	public int getListModelIndex(ListModel<?> model, Object value) {
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

		for (String s : component.isAnalog() ? AXIS_ACTION_CLASSES
				: BUTTON_ACTION_CLASSES) {
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
		final List<IAction> assignedActions = selectedMode
				.getComponentToActionMap().get(component.getName());

		final List<IAction> clonedAssignedActions = new ArrayList<IAction>();
		if (assignedActions != null)
			clonedAssignedActions.addAll(assignedActions);

		final ButtonToModeAction buttonToModeAction = unsavedProfile
				.getComponentToModeActionMap().get(component.getName());
		if (buttonToModeAction != null)
			clonedAssignedActions.add(buttonToModeAction);

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
		private static final long serialVersionUID = 1L;

		public SelectModeAction() {
			putValue(NAME, "Select Mode");
			putValue(SHORT_DESCRIPTION, "Selects the mode to edit");
		}

		public void actionPerformed(ActionEvent e) {
			selectedMode = (Mode) comboBoxMode.getSelectedItem();
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

				if (action instanceof ButtonToModeAction)
					unsavedProfile.getComponentToModeActionMap().put(
							component.getName(), (ButtonToModeAction) action);
				else {
					final Map<String, List<IAction>> componentToActionMap = selectedMode
							.getComponentToActionMap();
					final String componentName = component.getName();

					if (componentToActionMap.get(componentName) == null)
						componentToActionMap.put(componentName,
								new ArrayList<IAction>());

					componentToActionMap.get(componentName).add(action);
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
		private static final long serialVersionUID = 1L;

		public RemoveActionAction() {
			putValue(NAME, "Remove");
			putValue(SHORT_DESCRIPTION,
					"Remove the selected action to the component");
		}

		public void actionPerformed(ActionEvent e) {
			if (selectedAssignedAction instanceof ButtonToModeAction)
				unsavedProfile.getComponentToModeActionMap().remove(
						component.getName());
			else {
				final Map<String, List<IAction>> componentToActionMap = selectedMode
						.getComponentToActionMap();
				final List<IAction> actions = componentToActionMap
						.get(component.getName());
				actions.remove(selectedAssignedAction);

				if (actions.size() == 0)
					componentToActionMap.remove(component.getName());
			}

			updateAvailableActions();
			updateAssignedActions();
		}

	}

	private class JCheckBoxSetPropertyAction extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

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
		private static final long serialVersionUID = 1L;

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
			Input.setProfile(unsavedProfile);

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

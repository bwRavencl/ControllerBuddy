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

import static de.bwravencl.controllerbuddy.gui.Main.BUTTON_DIMENSION;
import static de.bwravencl.controllerbuddy.gui.Main.DIALOG_BOUNDS_X_Y_OFFSET;
import static de.bwravencl.controllerbuddy.gui.Main.STRING_RESOURCE_BUNDLE_BASENAME;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.lang.System.Logger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

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

import de.bwravencl.controllerbuddy.input.DirectInputKeyCode;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Mode.Component;
import de.bwravencl.controllerbuddy.input.Mode.Component.ComponentType;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.AxisToAxisAction;
import de.bwravencl.controllerbuddy.input.action.AxisToButtonAction;
import de.bwravencl.controllerbuddy.input.action.AxisToCursorAction;
import de.bwravencl.controllerbuddy.input.action.AxisToCursorAction.MouseAxis;
import de.bwravencl.controllerbuddy.input.action.AxisToKeyAction;
import de.bwravencl.controllerbuddy.input.action.AxisToMouseButtonAction;
import de.bwravencl.controllerbuddy.input.action.AxisToRelativeAxisAction;
import de.bwravencl.controllerbuddy.input.action.AxisToScrollAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToButtonAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToKeyAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToLockKeyAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToMouseButtonAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToPressOnScreenKeyboardKeyAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToRelativeAxisReset;
import de.bwravencl.controllerbuddy.input.action.ButtonToScrollAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToSelectOnScreenKeyboardKeyAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToSelectOnScreenKeyboardKeyAction.Direction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.NullAction;
import de.bwravencl.controllerbuddy.util.ResourceBundleUtil;

@SuppressWarnings("serial")
public class EditActionsDialog extends JDialog {

	private class AddActionAction extends AbstractAction {

		private static final long serialVersionUID = -7713175853948284887L;

		private AddActionAction() {
			putValue(NAME, rb.getString("ADD_ACTION_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("ADD_ACTION_ACTION_DESCRIPTION"));
		}

		@SuppressWarnings("unchecked")
		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				final var action = getActionClassInstance(selectedAvailableAction.clazz);

				if (action instanceof ButtonToModeAction) {
					final var buttonToModeActionsMap = unsavedProfile.getButtonToModeActionsMap();
					if (!buttonToModeActionsMap.containsKey(component.index))
						buttonToModeActionsMap.put(component.index, new ArrayList<>());

					buttonToModeActionsMap.get(component.index).add((ButtonToModeAction) action);
				} else if (isComponentEditor()) {
					final var componentToActionMap = (Map<Integer, List<IAction<?>>>) selectedMode
							.getComponentToActionsMap(component.type);

					if (!componentToActionMap.containsKey(component.index))
						componentToActionMap.put(component.index, new ArrayList<>());

					componentToActionMap.get(component.index).add(action);
				} else
					cycleActions.add((IAction<Byte>) action);

				updateAvailableActions();
				updateAssignedActions();

				assignedActionsList.setSelectedIndex(assignedActionsList.getLastVisibleIndex()
						- (hasModeAction() && !(action instanceof ButtonToModeAction) ? 1 : 0));
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e1) {
				log.log(Logger.Level.ERROR, e1.getMessage(), e1);
			}
		}

	}

	private class AvailableAction {

		private final Class<?> clazz;

		private AvailableAction(final Class<?> clazz) {
			this.clazz = clazz;
		}

		@Override
		public String toString() {
			String description = "";

			try {
				final IAction<?> action = getActionClassInstance(clazz);
				description = action.toString();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				log.log(Logger.Level.ERROR, e.getMessage(), e);
			}

			return description;
		}

	}

	private class CancelAction extends AbstractAction {

		private static final long serialVersionUID = 8086810563127997199L;

		private CancelAction() {
			putValue(NAME, UIManager.getLookAndFeelDefaults().get("OptionPane.cancelButtonText"));
			putValue(SHORT_DESCRIPTION, rb.getString("CANCEL_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			closeDialog();
		}

	}

	private class EditActionsAction extends AbstractAction {

		private static final long serialVersionUID = -6538021954760621595L;

		private EditActionsAction() {
			putValue(NAME, rb.getString("EDIT_ACTIONS_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("EDIT_ACTIONS_ACTION_DESCRIPTION_PREFIX")
					+ selectedAssignedAction.toString() + rb.getString("EDIT_ACTIONS_ACTION_DESCRIPTION_SUFFIX"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final EditActionsDialog editComponentDialog = new EditActionsDialog(
					(ButtonToCycleAction) selectedAssignedAction);
			editComponentDialog.setVisible(true);
		}

	}

	private class JCheckBoxSetPropertyAction extends AbstractAction {

		private static final long serialVersionUID = -33052386834598414L;

		private final Method setterMethod;

		private JCheckBoxSetPropertyAction(final Method setterMethod) {
			this.setterMethod = setterMethod;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				setterMethod.invoke(selectedAssignedAction, ((JCheckBox) e.getSource()).isSelected());
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				log.log(Logger.Level.ERROR, e1.getMessage(), e1);
			}
		}

	}

	private class JComboBoxSetPropertyAction extends AbstractAction {

		private static final long serialVersionUID = 1938012378184518954L;

		private final Method setterMethod;

		private JComboBoxSetPropertyAction(final Method setterMethod) {
			this.setterMethod = setterMethod;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				setterMethod.invoke(selectedAssignedAction, ((JComboBox<?>) e.getSource()).getSelectedItem());
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				log.log(Logger.Level.ERROR, e1.getMessage(), e1);
			}
		}

	}

	private class JListSetPropertyListSelectionListener implements ListSelectionListener {

		private final Method setterMethod;
		private final KeyStroke keyStroke;
		private final boolean modifiers;

		private JListSetPropertyListSelectionListener(final Method setterMethod, final KeyStroke keyStroke,
				final boolean modifiers) {
			this.setterMethod = setterMethod;
			this.keyStroke = keyStroke;
			this.modifiers = modifiers;
		}

		@Override
		public void valueChanged(final ListSelectionEvent e) {
			try {
				final Set<Integer> scanCodes = new HashSet<>();

				for (final Object o : ((JList<?>) e.getSource()).getSelectedValuesList())
					scanCodes.add(DirectInputKeyCode.nameToKeyCodeMap.get(o));

				final Integer[] scanCodesArray = scanCodes.toArray(new Integer[scanCodes.size()]);

				if (modifiers)
					keyStroke.setModifierCodes(scanCodesArray);
				else
					keyStroke.setKeyCodes(scanCodesArray);

				setterMethod.invoke(selectedAssignedAction, keyStroke);
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				log.log(Logger.Level.ERROR, e1.getMessage(), e1);
			}
		}

	}

	private class JSpinnerSetPropertyChangeListener implements ChangeListener {

		private final Method setterMethod;

		private JSpinnerSetPropertyChangeListener(final Method setterMethod) {
			this.setterMethod = setterMethod;
		}

		@Override
		public void stateChanged(final ChangeEvent e) {
			try {
				final Object value = ((JSpinner) e.getSource()).getValue();

				setterMethod.invoke(selectedAssignedAction,
						value instanceof Double ? ((Double) value).floatValue() : value);
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				log.log(Logger.Level.ERROR, e1.getMessage(), e1);
			}
		}

	}

	private class OKAction extends AbstractAction {

		private static final long serialVersionUID = -6947022759101822700L;

		private OKAction() {
			putValue(NAME, UIManager.getLookAndFeelDefaults().get("OptionPane.okButtonText"));
			putValue(SHORT_DESCRIPTION, rb.getString("OK_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (isComponentEditor()) {

				boolean requiresOnScreenKeyboardMode = false;
				outer: for (final List<ButtonToModeAction> buttonToModeActions : unsavedProfile
						.getButtonToModeActionsMap().values())
					for (final ButtonToModeAction a : buttonToModeActions)
						if (a.targetsOnScreenKeyboardMode()) {
							requiresOnScreenKeyboardMode = true;
							break outer;
						}

				if (requiresOnScreenKeyboardMode
						&& !unsavedProfile.getModes().contains(OnScreenKeyboard.onScreenKeyboardMode))
					unsavedProfile.getModes().add(OnScreenKeyboard.onScreenKeyboardMode);
				else if (!requiresOnScreenKeyboardMode
						&& unsavedProfile.getModes().contains(OnScreenKeyboard.onScreenKeyboardMode))
					unsavedProfile.getModes().remove(OnScreenKeyboard.onScreenKeyboardMode);

				input.setProfile(unsavedProfile, input.getJid());
				main.updateModesPanel();
				main.setUnsavedChanges(true);
			} else
				cycleAction.setActions(cycleActions);

			closeDialog();
		}

	}

	private class RemoveActionAction extends AbstractAction {

		private static final long serialVersionUID = -5681740772832902238L;

		private RemoveActionAction() {
			putValue(NAME, rb.getString("REMOVE_ACTION_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, rb.getString("REMOVE_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (selectedAssignedAction instanceof ButtonToModeAction) {
				final var buttonToModeActionsMap = unsavedProfile.getButtonToModeActionsMap();
				buttonToModeActionsMap.get(component.index).remove(selectedAssignedAction);
				if (buttonToModeActionsMap.get(component.index).isEmpty())
					buttonToModeActionsMap.remove(component.index);
			} else if (isComponentEditor()) {
				final var componentToActionMap = selectedMode.getComponentToActionsMap(component.type);
				@SuppressWarnings("unchecked")
				final var actions = (List<IAction<?>>) componentToActionMap.get(component.index);
				actions.remove(selectedAssignedAction);

				if (actions.isEmpty())
					componentToActionMap.remove(component.index);
			} else
				cycleActions.remove(selectedAssignedAction);

			updateAvailableActions();
			updateAssignedActions();
		}

	}

	private static final Logger log = System.getLogger(EditActionsDialog.class.getName());

	private static final Class<?>[] AXIS_ACTION_CLASSES = { AxisToAxisAction.class, AxisToButtonAction.class,
			AxisToCursorAction.class, AxisToKeyAction.class, AxisToMouseButtonAction.class,
			AxisToRelativeAxisAction.class, AxisToScrollAction.class, NullAction.class };

	private static final Class<?>[] BUTTON_ACTION_CLASSES = { ButtonToButtonAction.class, ButtonToCycleAction.class,
			ButtonToKeyAction.class, ButtonToLockKeyAction.class, ButtonToModeAction.class,
			ButtonToMouseButtonAction.class, ButtonToRelativeAxisReset.class, ButtonToScrollAction.class,
			NullAction.class };
	private static final Class<?>[] CYCLE_ACTION_CLASSES = { ButtonToButtonAction.class, ButtonToKeyAction.class,
			ButtonToMouseButtonAction.class, ButtonToRelativeAxisReset.class, ButtonToScrollAction.class,
			NullAction.class };
	private static final Class<?>[] ON_SCREEN_KEYBOARD_ACTION_CLASSES = { ButtonToPressOnScreenKeyboardKeyAction.class,
			ButtonToSelectOnScreenKeyboardKeyAction.class };
	private static final String ACTION_PROPERTY_GETTER_PREFIX_DEFAULT = "get";

	private static final String ACTION_PROPERTY_GETTER_PREFIX_BOOLEAN = "is";
	private static final String ACTION_PROPERTY_SETTER_PREFIX = "set";
	private static final int DIALOG_BOUNDS_X = Main.DIALOG_BOUNDS_X + DIALOG_BOUNDS_X_Y_OFFSET;
	private static final int DIALOG_BOUNDS_Y = Main.DIALOG_BOUNDS_Y + DIALOG_BOUNDS_X_Y_OFFSET;
	private static final int DIALOG_BOUNDS_WIDTH = 950;
	private static final int DIALOG_BOUNDS_HEIGHT = 510;

	public static int getListModelIndex(final ListModel<?> model, final Object value) {
		if (value == null)
			return -1;

		if (model instanceof DefaultListModel)
			return ((DefaultListModel<?>) model).indexOf(value);

		for (var i = 0; i < model.getSize(); i++)
			if (value.equals(model.getElementAt(i)))
				return i;

		return -1;
	}

	private Main main;
	private Component component;
	private Input input;
	private Profile unsavedProfile;
	private ButtonToCycleAction cycleAction;
	private final List<IAction<Byte>> cycleActions = new ArrayList<>();
	private Mode selectedMode;
	private AvailableAction selectedAvailableAction;
	private IAction<?> selectedAssignedAction;
	private final ResourceBundle rb = new ResourceBundleUtil().getResourceBundle(STRING_RESOURCE_BUNDLE_BASENAME,
			Locale.getDefault());
	private final JList<AvailableAction> availableActionsList = new JList<>();
	private final JList<IAction<?>> assignedActionsList = new JList<>();

	@SuppressWarnings("unchecked")
	private EditActionsDialog(final ButtonToCycleAction cycleAction) {
		this.cycleAction = cycleAction;

		try {
			for (final var action : cycleAction.getActions())
				cycleActions.add((IAction<Byte>) action.clone());

			preInit();

			setBounds(DIALOG_BOUNDS_X + DIALOG_BOUNDS_X_Y_OFFSET, DIALOG_BOUNDS_Y + DIALOG_BOUNDS_X_Y_OFFSET,
					DIALOG_BOUNDS_WIDTH, DIALOG_BOUNDS_HEIGHT);
			setTitle(cycleAction.toString() + rb.getString("EDIT_ACTIONS_DIALOG_TITLE_CYCLE_ACTION_EDITOR_SUFFIX"));

			init();
		} catch (final CloneNotSupportedException e) {
			log.log(Logger.Level.ERROR, e.getMessage(), e);
		}
	}

	EditActionsDialog(final Main main, final Component component, final String name) {
		super(main.getFrame());
		this.main = main;
		this.component = component;
		input = main.getInput();

		try {
			unsavedProfile = (Profile) input.getProfile().clone();

			preInit();

			setBounds(DIALOG_BOUNDS_X, DIALOG_BOUNDS_Y, DIALOG_BOUNDS_WIDTH, DIALOG_BOUNDS_HEIGHT);
			setTitle(rb.getString("EDIT_ACTIONS_DIALOG_TITLE_COMPONENT_EDITOR_PREFIX") + name);

			final JPanel modePanel = new JPanel(new FlowLayout());
			getContentPane().add(modePanel, BorderLayout.NORTH);

			modePanel.add(new JLabel(rb.getString("MODE_LABEL")));

			final List<Mode> modes = unsavedProfile.getModes();
			selectedMode = modes.get(0);
			final JComboBox<Mode> modeComboBox = new JComboBox<>(modes.toArray(new Mode[modes.size()]));
			modeComboBox.addActionListener(new AbstractAction() {

				private static final long serialVersionUID = -9107064465015662054L;

				@Override
				public void actionPerformed(final ActionEvent e) {
					selectedMode = (Mode) modeComboBox.getSelectedItem();
					updateAssignedActions();
					updateAvailableActions();
				}
			});
			modePanel.add(modeComboBox);

			init();
		} catch (final CloneNotSupportedException e) {
			log.log(Logger.Level.ERROR, e.getMessage(), e);
		}
	}

	private void closeDialog() {
		setVisible(false);
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private IAction<?> getActionClassInstance(final Class<?> clazz)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		if (!IAction.class.isAssignableFrom(clazz))
			throw new IllegalArgumentException(
					"Class '" + clazz.getName() + "' does not implement '" + IAction.class.getSimpleName() + "'");

		if (clazz == ButtonToModeAction.class)
			return new ButtonToModeAction(input);
		else
			return (IAction<?>) clazz.getConstructor().newInstance();
	}

	@SuppressWarnings("unchecked")
	private IAction<?>[] getAssignedActions() {
		final var clonedAssignedActions = new ArrayList<IAction<?>>();

		if (isComponentEditor()) {
			final var componentActions = selectedMode.getComponentToActionsMap(component.type).get(component.index);
			if (componentActions != null)
				clonedAssignedActions.addAll((Collection<? extends IAction<?>>) componentActions);
		} else if (cycleActions != null)
			clonedAssignedActions.addAll(cycleActions);

		if (isComponentEditor() && component.type == ComponentType.BUTTON && Profile.defaultMode.equals(selectedMode)) {
			final var buttonToModeActions = unsavedProfile.getButtonToModeActionsMap().get(component.index);
			if (buttonToModeActions != null)
				for (final var action : buttonToModeActions)
					clonedAssignedActions.add(action);
		}

		return clonedAssignedActions.toArray(new IAction[clonedAssignedActions.size()]);
	}

	private boolean hasModeAction() {
		boolean hasModeAction = false;

		for (final var action : getAssignedActions())
			if (action instanceof ButtonToModeAction)
				hasModeAction = true;

		return hasModeAction;
	}

	private void init() {
		final JPanel actionsPanel = new JPanel(new GridBagLayout());
		actionsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(actionsPanel, BorderLayout.CENTER);

		actionsPanel.add(new JLabel(rb.getString("AVAILABLE_ACTIONS_LABEL")), new GridBagConstraints(0, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 25));

		final JButton addButton = new JButton(new AddActionAction());
		addButton.setPreferredSize(BUTTON_DIMENSION);
		addButton.setEnabled(false);
		actionsPanel.add(addButton, new GridBagConstraints(1, 2, 1, 2, 0.0, 0.25, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		final JButton removeButton = new JButton(new RemoveActionAction());
		removeButton.setPreferredSize(BUTTON_DIMENSION);
		removeButton.setEnabled(false);
		actionsPanel.add(removeButton, new GridBagConstraints(1, 4, 1, 2, 0.0, 0.25, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		availableActionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		availableActionsList.addListSelectionListener(e -> {
			selectedAvailableAction = availableActionsList.getSelectedValue();
			if (selectedAvailableAction == null)
				addButton.setEnabled(false);
			else
				addButton.setEnabled(true);
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
		assignedActionsList.addListSelectionListener(e -> {
			selectedAssignedAction = assignedActionsList.getSelectedValue();
			if (selectedAssignedAction == null)
				removeButton.setEnabled(false);
			else
				removeButton.setEnabled(true);

			JPanel propertiesPanel = null;
			if (selectedAssignedAction != null)
				for (final Method m : selectedAssignedAction.getClass().getMethods()) {
					final String methodName = m.getName();

					if (!methodName.startsWith(ACTION_PROPERTY_SETTER_PREFIX))
						continue;

					final Class<?>[] parameterTypes = m.getParameterTypes();
					if (parameterTypes.length != 1)
						continue;

					if (propertiesPanel == null)
						propertiesPanel = new JPanel(new GridBagLayout());

					final Class<?> clazz = parameterTypes[0];
					final String propertyName = methodName.substring(
							methodName.indexOf(ACTION_PROPERTY_SETTER_PREFIX) + ACTION_PROPERTY_SETTER_PREFIX.length());
					try {
						final Method getterMethod = selectedAssignedAction.getClass().getMethod(
								(clazz == boolean.class ? ACTION_PROPERTY_GETTER_PREFIX_BOOLEAN
										: ACTION_PROPERTY_GETTER_PREFIX_DEFAULT) + propertyName,
								"Mode".equals(propertyName) ? new Class<?>[] { Input.class } : new Class<?>[] {});

						final JPanel propertyPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 0));
						propertiesPanel.add(propertyPanel,
								new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0,
										GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
										new Insets(0, 0, 0, 0), 0, 10));

						String spacedPropertyName = propertyName.codePoints()
								.mapToObj((c) -> (Character.isUpperCase(c) ? " " : "") + (char) c)
								.collect(Collectors.joining());
						spacedPropertyName = spacedPropertyName.startsWith(" ") ? spacedPropertyName.substring(1)
								: spacedPropertyName;
						final JLabel propertyNameLabel = new JLabel(spacedPropertyName);
						propertyNameLabel.setPreferredSize(new Dimension(100, 15));
						propertyPanel.add(propertyNameLabel);
						if (clazz == boolean.class) {
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
								checkBox.setSelected((boolean) getterMethod.invoke(selectedAssignedAction));
							propertyPanel.add(checkBox);
						} else if (clazz == int.class) {
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
							final JFormattedTextField textField1 = ((JSpinner.DefaultEditor) editor).getTextField();
							textField1.setColumns(2);
							final DefaultFormatter formatter1 = (DefaultFormatter) textField1.getFormatter();
							formatter1.setCommitsOnValidEdit(true);
							spinner.addChangeListener(new JSpinnerSetPropertyChangeListener(m));
							propertyPanel.add(spinner);
						} else if (clazz == float.class) {
							final float value = (float) getterMethod.invoke(selectedAssignedAction);

							final SpinnerNumberModel model;
							if ("DeadZone".equals(propertyName))
								model = new SpinnerNumberModel(value, 0.0, 1.0, 0.01);
							else if ("Exponent".equals(propertyName))
								model = new SpinnerNumberModel(value, 1.0, 5.0, 0.1);
							else if ("MinAxisValue".equals(propertyName) || "MaxAxisValue".equals(propertyName))
								model = new SpinnerNumberModel(value, -1.0, 1.0, 0.01);
							else if ("MaxCursorSpeed".equals(propertyName))
								model = new SpinnerNumberModel(value, 100.0, 10000.0, 1.0);
							else if ("MaxRelativeSpeed".equals(propertyName))
								model = new SpinnerNumberModel(value, 0.1, 100.0, 0.01);
							else
								model = new SpinnerNumberModel(value, -1.0, 1.0, 0.05);

							final JSpinner spinner = new JSpinner(model);
							final JComponent editor = spinner.getEditor();
							final JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
							textField.setColumns(4);
							final DefaultFormatter formatter = (DefaultFormatter) textField.getFormatter();
							formatter.setCommitsOnValidEdit(true);
							spinner.addChangeListener(new JSpinnerSetPropertyChangeListener(m));
							propertyPanel.add(spinner);
						} else if (clazz == Mode.class) {
							final JComboBox<Mode> comboBox = new JComboBox<>();
							if (!input.getProfile().getModes().contains(OnScreenKeyboard.onScreenKeyboardMode))
								comboBox.addItem(OnScreenKeyboard.onScreenKeyboardMode);
							for (final Mode p : input.getProfile().getModes())
								if (!Profile.defaultMode.equals(p))
									comboBox.addItem(p);
							comboBox.setAction(new JComboBoxSetPropertyAction(m));
							comboBox.setSelectedItem(getterMethod.invoke(selectedAssignedAction, input));
							propertyPanel.add(comboBox);
						} else if (VirtualAxis.class == clazz) {
							final JComboBox<VirtualAxis> comboBox = new JComboBox<>(VirtualAxis.values());
							comboBox.setAction(new JComboBoxSetPropertyAction(m));
							comboBox.setSelectedItem(getterMethod.invoke(selectedAssignedAction));
							propertyPanel.add(comboBox);
						} else if (clazz == MouseAxis.class) {
							final JComboBox<MouseAxis> comboBox = new JComboBox<>(MouseAxis.values());
							comboBox.setAction(new JComboBoxSetPropertyAction(m));
							comboBox.setSelectedItem(getterMethod.invoke(selectedAssignedAction));
							propertyPanel.add(comboBox);
						} else if (clazz == Direction.class) {
							final JComboBox<Direction> comboBox = new JComboBox<>(Direction.values());
							comboBox.setAction(new JComboBoxSetPropertyAction(m));
							comboBox.setSelectedItem(getterMethod.invoke(selectedAssignedAction));
							propertyPanel.add(comboBox);
						} else if (clazz == LockKey.class) {
							final JComboBox<LockKey> comboBox = new JComboBox<>(LockKey.LOCK_KEYS);
							comboBox.setAction(new JComboBoxSetPropertyAction(m));
							comboBox.setSelectedItem(getterMethod.invoke(selectedAssignedAction));
							propertyPanel.add(comboBox);
						} else if (clazz == KeyStroke.class) {
							final KeyStroke keyStroke = (KeyStroke) getterMethod.invoke(selectedAssignedAction);
							final Set<String> availableScanCodes = DirectInputKeyCode.nameToKeyCodeMap.keySet();

							final JPanel modifiersPanel = new JPanel();
							modifiersPanel.setLayout(new BoxLayout(modifiersPanel, BoxLayout.PAGE_AXIS));
							final JLabel modifiersLabel = new JLabel(rb.getString("MODIFIERS_LABEL"));
							modifiersLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
							modifiersPanel.add(modifiersLabel);
							modifiersPanel.add(Box.createVerticalStrut(5));
							final JList<String> modifierList = new JList<>(
									availableScanCodes.toArray(new String[availableScanCodes.size()]));
							modifierList.addListSelectionListener(
									new JListSetPropertyListSelectionListener(m, keyStroke, true));

							final List<String> addedModifiers = new ArrayList<>();
							for (final int c1 : keyStroke.getModifierCodes())
								addedModifiers.add(DirectInputKeyCode.keyCodeToNameMap.get(c1));
							for (final String s1 : addedModifiers) {
								final int index1 = getListModelIndex(modifierList.getModel(), s1);
								if (index1 >= 0)
									modifierList.addSelectionInterval(index1, index1);
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
							for (final int c2 : keyStroke.getKeyCodes())
								addedKeys.add(DirectInputKeyCode.keyCodeToNameMap.get(c2));
							for (final String s2 : addedKeys) {
								final int index2 = getListModelIndex(keyList.getModel(), s2);
								if (index2 >= 0)
									keyList.addSelectionInterval(index2, index2);
							}
							final JScrollPane keysScrollPane = new JScrollPane(keyList);
							keysScrollPane.setPreferredSize(new Dimension(175, 200));
							keysPanel.add(keysScrollPane);
							propertyPanel.add(keysPanel);

						} else if (clazz == List.class) {
							final JButton editActionsButton = new JButton(new EditActionsAction());
							editActionsButton.setPreferredSize(BUTTON_DIMENSION);
							propertyPanel.add(editActionsButton);
						} else
							throw new UnsupportedOperationException(getClass().getName()
									+ ": GUI representation implementation missing for " + clazz.getName());
					} catch (final NoSuchMethodException | SecurityException | IllegalAccessException
							| IllegalArgumentException | InvocationTargetException e1) {
						log.log(Logger.Level.ERROR, e1.getMessage(), e1);
					}
				}

			final boolean anyPropertiesFound = propertiesPanel != null;

			if (anyPropertiesFound) {
				propertiesPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0,
						1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

				propertiesScrollPane.setViewportView(propertiesPanel);
			}
			propertiesLabel.setVisible(anyPropertiesFound);
			propertiesScrollPane.setVisible(anyPropertiesFound);
		});
		actionsPanel.add(new JScrollPane(assignedActionsList), new GridBagConstraints(2, 1, 1, 5, 0.25, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		final JButton okButton = new JButton(new OKAction());
		okButton.setPreferredSize(BUTTON_DIMENSION);
		buttonPanel.add(okButton);
		getRootPane().setDefaultButton(okButton);

		final JButton cancelButton = new JButton(new CancelAction());
		cancelButton.setPreferredSize(BUTTON_DIMENSION);
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

		Class<?>[] actionClasses;
		if (isComponentEditor()) {
			if (component.type == ComponentType.AXIS)
				actionClasses = AXIS_ACTION_CLASSES;
			else if (OnScreenKeyboard.onScreenKeyboardMode.equals(selectedMode))
				actionClasses = ON_SCREEN_KEYBOARD_ACTION_CLASSES;
			else
				actionClasses = BUTTON_ACTION_CLASSES;
		} else
			actionClasses = CYCLE_ACTION_CLASSES;

		for (final Class<?> c : actionClasses) {
			final AvailableAction availableAction = new AvailableAction(c);

			if (ButtonToModeAction.class.equals(availableAction.clazz) && !Profile.defaultMode.equals(selectedMode))
				continue;
			else
				availableActions.add(availableAction);
		}

		availableActionsList.setListData(availableActions.toArray(new AvailableAction[availableActions.size()]));
	}

}

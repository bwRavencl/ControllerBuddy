/* Copyright (C) 2020  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.gui;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Mode.Component;
import de.bwravencl.controllerbuddy.input.Mode.Component.ComponentType;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.ToButtonAction;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import io.github.classgraph.ClassGraph;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("exports")
public final class EditActionsDialog extends JDialog {

	@Serial
	private static final long serialVersionUID = 5007388251349678609L;

	private static final Logger log = Logger.getLogger(EditActionsDialog.class.getName());

	private static final int DIALOG_BOUNDS_WIDTH = 1015;
	private static final int DIALOG_BOUNDS_HEIGHT = 665;
	private static final int DIALOG_BOUNDS_PARENT_OFFSET = 25;

	private static final double ACTIONS_LIST_WEIGHT_X = .245d;

	private static final List<Class<?>> axisActionClasses;
	private static final List<Class<?>> buttonActionClasses;
	private static final List<Class<?>> cycleActionClasses;
	private static final List<Class<?>> onScreenKeyboardActionClasses;

	static {
		final List<Class<?>> mutableAxisActionClasses = new ArrayList<>();
		final List<Class<?>> mutableButtonActionClasses = new ArrayList<>();
		final List<Class<?>> mutableCycleActionClasses = new ArrayList<>();
		final List<Class<?>> mutableOnScreenKeyboardActionClasses = new ArrayList<>();

		try (final var scanResult = new ClassGraph().acceptPackages(IAction.class.getPackageName()).enableClassInfo()
				.enableAnnotationInfo().scan()) {
			final var classInfoList = scanResult.getClassesWithAnnotation(Action.class.getName());
			classInfoList.sort((c1, c2) -> {
				final var a1 = c1.loadClass().getAnnotation(Action.class);
				final var a2 = c2.loadClass().getAnnotation(Action.class);

				return a1.order() - a2.order();
			});

			classInfoList.forEach(classInfo -> {
				final var actionClass = classInfo.loadClass();
				final var annotation = actionClass.getAnnotation(Action.class);
				final var category = annotation.category();

				if (category == ActionCategory.ALL || category == ActionCategory.AXIS) {
					mutableAxisActionClasses.add(actionClass);
				}
				if (category == ActionCategory.ALL || category == ActionCategory.BUTTON
						|| category == ActionCategory.BUTTON_AND_CYCLES) {
					mutableButtonActionClasses.add(actionClass);
				}
				if (category == ActionCategory.ALL || category == ActionCategory.BUTTON_AND_CYCLES) {
					mutableCycleActionClasses.add(actionClass);
				}
				if (category == ActionCategory.ALL || category == ActionCategory.ON_SCREEN_KEYBOARD_MODE) {
					mutableOnScreenKeyboardActionClasses.add(actionClass);
				}
			});
		}

		axisActionClasses = Collections.unmodifiableList(mutableAxisActionClasses);
		buttonActionClasses = Collections.unmodifiableList(mutableButtonActionClasses);
		cycleActionClasses = Collections.unmodifiableList(mutableCycleActionClasses);
		onScreenKeyboardActionClasses = Collections.unmodifiableList(mutableOnScreenKeyboardActionClasses);
	}

	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final List<IAction<Byte>> cycleActions = new ArrayList<>();

	private final JList<AvailableAction> availableActionsList = new JList<>();
	private final JList<AssignedAction> assignedActionsList = new JList<>();

	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final Profile unsavedProfile;

	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final Main main;

	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private Component component;

	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private Input input;

	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private ButtonToCycleAction cycleAction;

	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private Mode selectedMode;

	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private AvailableAction selectedAvailableAction;

	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private AssignedAction selectedAssignedAction;

	private JButton pasteButton;

	@SuppressWarnings("unchecked")
	public EditActionsDialog(@SuppressWarnings("exports") final EditActionsDialog parentDialog,
			@SuppressWarnings("exports") final ButtonToCycleAction cycleAction) {
		super(parentDialog);

		main = parentDialog.main;
		this.cycleAction = cycleAction;

		try {
			for (final var action : cycleAction.getActions()) {
				cycleActions.add((IAction<Byte>) action.clone());
			}
		} catch (final CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}

		unsavedProfile = parentDialog.unsavedProfile;

		preInit(parentDialog);
		setTitle(MessageFormat.format(Main.strings.getString("EDIT_ACTIONS_DIALOG_TITLE_CYCLE_ACTION_EDITOR"),
				IAction.getLabel(cycleAction.getClass())));

		init();
	}

	EditActionsDialog(final Main main, final Component component, final String name) {
		super(main.getFrame());

		this.main = main;
		this.component = component;
		input = main.getInput();

		try {
			unsavedProfile = (Profile) input.getProfile().clone();
		} catch (final CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}

		preInit(main.getFrame());
		setTitle(MessageFormat.format(Main.strings.getString("EDIT_ACTIONS_DIALOG_TITLE_COMPONENT_EDITOR"), name));

		final var modes = unsavedProfile.getModes();
		selectedMode = modes.getFirst();
		GuiUtils.addModePanel(getContentPane(), modes, new AbstractAction() {

			@Serial
			private static final long serialVersionUID = -9107064465015662054L;

			@SuppressWarnings("unchecked")
			@Override
			public void actionPerformed(final ActionEvent e) {
				selectedMode = (Mode) ((JComboBox<Mode>) e.getSource()).getSelectedItem();
				updateAssignedActions();
				updateAvailableActions();
			}
		});

		init();
	}

	private static JButton addActionButton(final javax.swing.Action action, final JPanel parentPanel,
			final Dimension dimension) {
		final var button = new JButton(action);
		button.setEnabled(false);
		addComponentToPanelFixedSize(button, parentPanel, dimension);

		return button;
	}

	private static void addComponentToPanelFixedSize(final javax.swing.JComponent component, final JPanel parentPanel,
			final Dimension dimension) {
		component.setPreferredSize(dimension);
		component.setMinimumSize(dimension);
		component.setMaximumSize(dimension);

		component.setAlignmentX(CENTER_ALIGNMENT);
		component.setAlignmentY(CENTER_ALIGNMENT);

		parentPanel.add(component);
	}

	private static Map<Field, ActionProperty> getFieldToActionPropertiesMap(final Class<?> actionClass) {
		if (!IAction.class.isAssignableFrom(actionClass)) {
			throw new IllegalArgumentException(
					"Parameter actionClass does not implement " + IAction.class.getSimpleName());
		}

		final var propertyMap = new HashMap<Field, ActionProperty>();

		for (final var field : actionClass.getDeclaredFields()) {
			final var annotation = field.getAnnotation(ActionProperty.class);
			if (annotation != null) {
				propertyMap.put(field, annotation);
			}
		}

		final var parentClass = actionClass.getSuperclass();
		if (parentClass != Object.class) {
			final var parentClassPropertyMap = getFieldToActionPropertiesMap(parentClass);
			propertyMap.putAll(parentClassPropertyMap);
		}

		return propertyMap;
	}

	@SuppressWarnings("unchecked")
	private void addAction(final IAction<?> action) {
		try {
			if (action instanceof final ButtonToModeAction buttonToModeAction) {
				final var buttonToModeActionsMap = unsavedProfile.getButtonToModeActionsMap();
				if (!buttonToModeActionsMap.containsKey(component.getIndex())) {
					buttonToModeActionsMap.put(component.getIndex(), new ArrayList<>());
				}

				buttonToModeActionsMap.get(component.getIndex()).add(buttonToModeAction);
			} else if (isCycleEditor()) {
				cycleActions.add((IAction<Byte>) action);
			} else {
				final var componentToActionMap = (Map<Integer, List<IAction<?>>>) selectedMode
						.getComponentToActionsMap(component.getType());

				if (!componentToActionMap.containsKey(component.getIndex())) {
					componentToActionMap.put(component.getIndex(), new ArrayList<>());
				}

				componentToActionMap.get(component.getIndex()).add(action);
			}

			updateAvailableActions();
			updateAssignedActions();

			final var hasModeAction = Arrays.stream(getAssignedActions())
					.anyMatch(assignedAction -> assignedAction.action instanceof ButtonToModeAction);

			assignedActionsList.setSelectedIndex(assignedActionsList.getLastVisibleIndex()
					- (hasModeAction && !(action instanceof ButtonToModeAction) ? 1 : 0));
		} catch (final IllegalArgumentException | SecurityException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	private void closeDialog() {
		setVisible(false);
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private IAction<?> getActionClassInstance(final Class<?> actionClass)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		if (!IAction.class.isAssignableFrom(actionClass)) {
			throw new IllegalArgumentException(
					"Class '" + actionClass.getName() + "' does not implement '" + IAction.class.getSimpleName() + "'");
		}

		final var action = (IAction<?>) actionClass.getConstructor().newInstance();

		switch (action) {
		case final ButtonToModeAction buttonToModeAction -> {
			final var modes = input.getProfile().getModes();
			final var defaultMode = modes.size() > 1 ? modes.get(1) : OnScreenKeyboard.onScreenKeyboardMode;
			buttonToModeAction.setMode(defaultMode);
		}
		case final ToButtonAction<?> toButtonAction -> {
			final var maxButtonId = unsavedProfile.getModes().stream()
					.flatMapToInt(mode -> Stream
							.concat(mode.getAxisToActionsMap().values().stream(),
									mode.getButtonToActionsMap().values().stream())
							.flatMapToInt(actions -> actions.stream().flatMapToInt(action1 -> {
								if (action1 instanceof final ToButtonAction<?> toButtonAction1) {
									return IntStream.of(toButtonAction1.getButtonId());
								}

								return IntStream.empty();
							})))
					.max().orElse(-1);

			final var buttonId = Math.min(maxButtonId + 1, Input.MAX_N_BUTTONS - 1);
			toButtonAction.setButtonId(buttonId);
		}
		default -> {
		}
		}

		return action;
	}

	private List<Class<?>> getAllowedActionClasses() {
		if (isCycleEditor()) {
			return cycleActionClasses;
		} else if (OnScreenKeyboard.onScreenKeyboardMode.equals(selectedMode)) {
			return onScreenKeyboardActionClasses;
		} else if (component.getType() == ComponentType.AXIS) {
			return axisActionClasses;
		} else if (Profile.defaultMode.equals(selectedMode)) {
			return buttonActionClasses;
		}

		return buttonActionClasses.stream().filter(clazz -> clazz != ButtonToModeAction.class).toList();
	}

	@SuppressWarnings("unchecked")
	private AssignedAction[] getAssignedActions() {
		final var assignedActions = new ArrayList<AssignedAction>();

		final var cycleEditor = isCycleEditor();

		if (cycleEditor && cycleActions != null) {
			cycleActions.forEach(action -> assignedActions.add(new AssignedAction(action)));
		} else if (component != null) {
			final var componentActions = selectedMode.getComponentToActionsMap(component.getType())
					.get(component.getIndex());
			if (componentActions != null) {
				((Collection<? extends IAction<?>>) componentActions)
						.forEach(action -> assignedActions.add(new AssignedAction(action)));
			}
		}

		if (!cycleEditor && component.getType() == ComponentType.BUTTON && Profile.defaultMode.equals(selectedMode)) {
			final var buttonToModeActions = unsavedProfile.getButtonToModeActionsMap().get(component.getIndex());
			if (buttonToModeActions != null) {
				buttonToModeActions.forEach(action -> assignedActions.add(new AssignedAction(action)));
			}
		}

		return assignedActions.toArray(AssignedAction[]::new);
	}

	@SuppressWarnings("exports")
	public Input getInput() {
		return input;
	}

	private void init() {
		final var actionsPanel = new JPanel(new GridBagLayout());
		actionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		getContentPane().add(actionsPanel, BorderLayout.CENTER);

		actionsPanel.add(new JLabel(Main.strings.getString("AVAILABLE_ACTIONS_LABEL")), new GridBagConstraints(0, 0, 1,
				1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 25));

		final var actionButtonsPanel = new JPanel();
		actionButtonsPanel.setLayout(new BoxLayout(actionButtonsPanel, BoxLayout.Y_AXIS));
		actionsPanel.add(actionButtonsPanel, new GridBagConstraints(1, 3, 1, 0, 0d, 1d, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 5, 0, 5), 0, 0));

		actionButtonsPanel.add(Box.createVerticalGlue());
		final var addButton = addActionButton(new AddActionAction(), actionButtonsPanel, Main.BUTTON_DIMENSION);
		actionButtonsPanel.add(Box.createVerticalStrut(Main.BUTTON_DIMENSION_HEIGHT));
		final var removeButton = addActionButton(new RemoveActionAction(), actionButtonsPanel, Main.BUTTON_DIMENSION);
		actionButtonsPanel.add(Box.createVerticalGlue());

		final var copyPastePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		final var titledBorder = BorderFactory.createTitledBorder(Main.strings.getString("CLIPBOARD_BORDER_TITLE"));
		final var emptyBorder = (EmptyBorder) BorderFactory.createEmptyBorder(5, 0, 5, 0);
		final var border = BorderFactory.createCompoundBorder(titledBorder, emptyBorder);
		copyPastePanel.setBorder(border);
		final var insideBorderInsets = emptyBorder.getBorderInsets();
		addComponentToPanelFixedSize(copyPastePanel, actionButtonsPanel,
				new Dimension(Main.BUTTON_DIMENSION.width,
						Main.BUTTON_DIMENSION_HEIGHT + titledBorder.getMinimumSize(copyPastePanel).height
								+ insideBorderInsets.top + insideBorderInsets.bottom));

		final var copyButton = addActionButton(new CopyActionAction(), copyPastePanel, Main.SQUARE_BUTTON_DIMENSION);
		pasteButton = addActionButton(new PasteActionAction(), copyPastePanel, Main.SQUARE_BUTTON_DIMENSION);

		availableActionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		availableActionsList.addListSelectionListener(_ -> {
			selectedAvailableAction = availableActionsList.getSelectedValue();
			addButton.setEnabled(selectedAvailableAction != null);
		});
		updateAvailableActions();

		actionsPanel.add(GuiUtils.wrapComponentInScrollPane(availableActionsList),
				new GridBagConstraints(0, 1, 1, 5, ACTIONS_LIST_WEIGHT_X, 1d, GridBagConstraints.CENTER,
						GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		actionsPanel.add(new JLabel(Main.strings.getString("ASSIGNED_ACTIONS_LABEL")), new GridBagConstraints(2, 0, 1,
				1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 25));

		final var propertiesLabel = new JLabel(Main.strings.getString("PROPERTIES_LABEL"));
		propertiesLabel.setVisible(false);
		actionsPanel.add(propertiesLabel, new GridBagConstraints(3, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 25));

		final var propertiesScrollPane = new JScrollPane();
		propertiesScrollPane.setVisible(false);
		actionsPanel.add(propertiesScrollPane, new GridBagConstraints(3, 1, 1, 5, 1d, 1d, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		assignedActionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		assignedActionsList.addListSelectionListener(_ -> {
			selectedAssignedAction = assignedActionsList.getSelectedValue();

			final var buttonsEnabled = selectedAssignedAction != null;
			removeButton.setEnabled(buttonsEnabled);
			copyButton.setEnabled(buttonsEnabled);

			JPanel propertiesPanel = null;
			if (selectedAssignedAction != null) {
				final var actionClass = selectedAssignedAction.action.getClass();
				final var fieldToActionPropertyMap = getFieldToActionPropertiesMap(actionClass);
				final var sortedEntries = fieldToActionPropertyMap.entrySet().stream().sorted((entry1, entry2) -> {
					final var action1 = entry1.getValue();
					final var action2 = entry2.getValue();

					return action1.order() - action2.order();
				}).toList();

				for (final var entry : sortedEntries) {
					final var field = entry.getKey();
					final var annotation = entry.getValue();

					if (propertiesPanel == null) {
						propertiesPanel = new JPanel(new GridBagLayout());
					}

					final var propertyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
					propertiesPanel.add(propertyPanel,
							new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d,
									GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
									new Insets(5, 5, 5, 5), 0, 10));

					final var propertyNameLabel = new JLabel(Main.strings.getString(annotation.label()));
					propertyNameLabel.setPreferredSize(new Dimension(155, 15));
					propertyPanel.add(propertyNameLabel);

					try {
						final var editorBuilderClass = annotation.editorBuilder();

						var fieldName = annotation.overrideFieldName();
						if (fieldName.isEmpty()) {
							fieldName = field.getName();
						}

						var fieldType = annotation.overrideFieldType();
						if (fieldType == Void.class) {
							fieldType = field.getType();
						}

						final var constructor = editorBuilderClass.getDeclaredConstructor(EditActionsDialog.class,
								IAction.class, String.class, Class.class);
						final var editorBuilder = constructor.newInstance(this, selectedAssignedAction.action,
								fieldName, fieldType);

						editorBuilder.buildEditor(propertyPanel);
					} catch (final NoSuchMethodException | SecurityException | InstantiationException
							| IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
						throw new RuntimeException(e1);
					}
				}
			}

			final var anyPropertiesFound = propertiesPanel != null;
			if (anyPropertiesFound) {
				propertiesPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1d,
						1d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

				propertiesScrollPane.setViewportView(propertiesPanel);
			}
			propertiesLabel.setVisible(anyPropertiesFound);
			propertiesScrollPane.setVisible(anyPropertiesFound);
			revalidate();
		});
		actionsPanel.add(GuiUtils.wrapComponentInScrollPane(assignedActionsList),
				new GridBagConstraints(2, 1, 1, 5, ACTIONS_LIST_WEIGHT_X, 1d, GridBagConstraints.CENTER,
						GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		final var okCancelButtonPanel = new JPanel();
		okCancelButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		getContentPane().add(okCancelButtonPanel, BorderLayout.SOUTH);

		final var okButton = new JButton(new OKAction());
		okButton.setPreferredSize(Main.BUTTON_DIMENSION);
		okCancelButtonPanel.add(okButton);
		getRootPane().setDefaultButton(okButton);

		final var cancelButton = new JButton(new CancelAction());
		cancelButton.setPreferredSize(Main.BUTTON_DIMENSION);
		okCancelButtonPanel.add(cancelButton);

		updateAssignedActions();
	}

	public boolean isCycleEditor() {
		return component == null;
	}

	private void preInit(final java.awt.Component parentComponent) {
		setModal(true);
		getContentPane().setLayout(new BorderLayout());

		final var bounds = parentComponent.getBounds();
		bounds.x += DIALOG_BOUNDS_PARENT_OFFSET;
		bounds.y += DIALOG_BOUNDS_PARENT_OFFSET;
		bounds.width = DIALOG_BOUNDS_WIDTH;
		bounds.height = DIALOG_BOUNDS_HEIGHT;
		setBounds(bounds);
	}

	@Serial
	private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(EditActionsDialog.class.getName());
	}

	private void updateAssignedActions() {
		assignedActionsList.setListData(getAssignedActions());
	}

	private void updateAvailableActions() {
		Objects.requireNonNull(pasteButton, "Field pasteButton must not be null");

		final var availableActions = new ArrayList<AvailableAction>();

		for (final var actionClass : getAllowedActionClasses()) {
			final var availableAction = new AvailableAction(actionClass);

			if (ButtonToModeAction.class.equals(availableAction.actionClass)
					&& !Profile.defaultMode.equals(selectedMode)) {
				continue;
			}
			availableActions.add(availableAction);
		}

		availableActionsList.setListData(availableActions.toArray(AvailableAction[]::new));

		updatePasteButton();
	}

	private void updatePasteButton() {
		final var clipboardAction = main.getClipboardAction();
		final var pasteAllowed = clipboardAction != null
				&& getAllowedActionClasses().contains(clipboardAction.getClass());

		pasteButton.setEnabled(pasteAllowed);
	}

	@Serial
	private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(EditActionsDialog.class.getName());
	}

	@SuppressWarnings("unused")
	private record AssignedAction(IAction<?> action) {

		@Override
		public String toString() {
			return IAction.getLabel(action.getClass());
		}
	}

	@SuppressWarnings("unused")
	private record AvailableAction(Class<?> actionClass) {

		@Override
		public String toString() {
			return IAction.getLabel(actionClass);
		}
	}

	private final class AddActionAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -7713175853948284887L;

		private AddActionAction() {
			putValue(NAME, Main.strings.getString("ADD_ACTION_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, Main.strings.getString("ADD_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				addAction(getActionClassInstance(selectedAvailableAction.actionClass));
			} catch (final InstantiationException | IllegalAccessException | InvocationTargetException
					| NoSuchMethodException e1) {
				log.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}

	private final class CancelAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 8086810563127997199L;

		private CancelAction() {
			putValue(NAME, UIManager.getString("OptionPane.cancelButtonText"));
			putValue(SHORT_DESCRIPTION, Main.strings.getString("CANCEL_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			closeDialog();
		}
	}

	private final class CopyActionAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -6630683334825900710L;

		private CopyActionAction() {
			putValue(NAME, "🗐");
			putValue(SHORT_DESCRIPTION, Main.strings.getString("COPY_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				main.setClipboardAction((IAction<?>) selectedAssignedAction.action.clone());
				updatePasteButton();
			} catch (final CloneNotSupportedException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	private final class OKAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -6947022759101822700L;

		private OKAction() {
			putValue(NAME, UIManager.getString("OptionPane.okButtonText"));
			putValue(SHORT_DESCRIPTION, Main.strings.getString("OK_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (isCycleEditor()) {
				cycleAction.setActions(cycleActions);
			} else {
				var requiresOnScreenKeyboardMode = false;
				outer: for (final List<ButtonToModeAction> buttonToModeActions : unsavedProfile
						.getButtonToModeActionsMap().values()) {
					for (final ButtonToModeAction a : buttonToModeActions) {
						if (a.targetsOnScreenKeyboardMode()) {
							requiresOnScreenKeyboardMode = true;
							break outer;
						}
					}
				}

				if (requiresOnScreenKeyboardMode
						&& !unsavedProfile.getModes().contains(OnScreenKeyboard.onScreenKeyboardMode)) {
					unsavedProfile.getModes().add(OnScreenKeyboard.onScreenKeyboardMode);
				} else if (!requiresOnScreenKeyboardMode) {
					unsavedProfile.getModes().remove(OnScreenKeyboard.onScreenKeyboardMode);
				}

				input.setProfile(unsavedProfile);
				main.updateModesPanel(false);
				main.updateVisualizationPanel();
				main.setUnsavedChanges(true);
			}

			closeDialog();
		}
	}

	private final class PasteActionAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -6630683334825900710L;

		private PasteActionAction() {
			putValue(NAME, "📋");
			putValue(SHORT_DESCRIPTION, Main.strings.getString("PASTE_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				addAction((IAction<?>) main.getClipboardAction().clone());
			} catch (final CloneNotSupportedException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	private final class RemoveActionAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -5681740772832902238L;

		private RemoveActionAction() {
			putValue(NAME, Main.strings.getString("REMOVE_ACTION_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, Main.strings.getString("REMOVE_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (selectedAssignedAction.action instanceof final ButtonToModeAction buttonToModeAction) {
				final var buttonToModeActionsMap = unsavedProfile.getButtonToModeActionsMap();
				buttonToModeActionsMap.get(component.getIndex()).remove(buttonToModeAction);
				if (buttonToModeActionsMap.get(component.getIndex()).isEmpty()) {
					buttonToModeActionsMap.remove(component.getIndex());
				}
			} else if (isCycleEditor()) {
				cycleActions.remove(selectedAssignedAction.action);
			} else {
				final var componentToActionMap = selectedMode.getComponentToActionsMap(component.getType());
				@SuppressWarnings("unchecked")
				final var actions = (List<IAction<?>>) componentToActionMap.get(component.getIndex());
				actions.remove(selectedAssignedAction.action);

				if (actions.isEmpty()) {
					componentToActionMap.remove(component.getIndex());
				}
			}

			updateAvailableActions();
			updateAssignedActions();
		}
	}
}

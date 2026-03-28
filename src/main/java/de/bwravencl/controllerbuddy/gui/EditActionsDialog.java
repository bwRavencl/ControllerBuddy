/*
 * Copyright (C) 2014 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.gui;

import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.Mode.Component;
import de.bwravencl.controllerbuddy.input.Mode.Component.ComponentType;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.action.ButtonToCycleAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.ToAxisAction;
import de.bwravencl.controllerbuddy.input.action.ToButtonAction;
import de.bwravencl.controllerbuddy.input.action.annotation.Action;
import de.bwravencl.controllerbuddy.input.action.annotation.Action.ActionCategory;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import io.github.classgraph.ClassGraph;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.io.IOException;
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
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import org.lwjgl.sdl.SDLGamepad;

/// A modal dialog for editing the actions assigned to a controller component
/// (axis or button) or to a cycle action. Provides lists of available and
/// assigned actions, along with a property editor for configuring individual
/// action parameters and a help panel describing each action.
///
/// This dialog supports two modes of operation:
/// - **Component editor**: edits actions for a specific controller component
/// within a profile.
/// - **Cycle editor**: edits the sub-actions within a [ButtonToCycleAction].
@SuppressWarnings("exports")
public final class EditActionsDialog extends JDialog {

	/// Horizontal weight for the actions list columns in the grid layout.
	private static final double ACTIONS_LIST_WEIGHT_X = 0.2;

	/// Action classes available for axis components.
	private static final List<Class<?>> AXIS_ACTION_CLASSES;

	/// Action classes available for button components.
	private static final List<Class<?>> BUTTON_ACTION_CLASSES;

	/// Action classes available inside a cycle action editor.
	private static final List<Class<?>> CYCLE_ACTION_CLASSES;

	/// Default height of the dialog bounds in pixels.
	private static final int DIALOG_BOUNDS_HEIGHT = 690;

	/// Pixel offset applied to the dialog position relative to its parent.
	private static final int DIALOG_BOUNDS_PARENT_OFFSET = 25;

	/// Default width of the dialog bounds in pixels.
	private static final int DIALOG_BOUNDS_WIDTH = 1005;

	/// Cache mapping action classes to their field-to-annotation maps.
	private static final Map<Class<?>, Map<Field, ActionProperty>> FIELD_ACTION_PROPERTY_MAP_CACHE = new HashMap<>();

	/// Custom cursor shown when hovering over a property label that has help text.
	private static final Cursor HELP_CURSOR;

	/// Hot-spot point for the custom help cursor image.
	private static final Point HELP_CURSOR_HOT_SPOT = new Point(0, 0);

	/// Classpath resource path for the help cursor GIF image.
	private static final String HELP_GIF_RESOURCE_PATH = "/help.gif";

	/// Dimension for icon labels.
	private static final Dimension ICON_LABEL_DIMENSION = new Dimension(24, 24);

	private static final Logger LOGGER = Logger.getLogger(EditActionsDialog.class.getName());

	/// Action classes available for on-screen keyboard mode components.
	private static final List<Class<?>> ON_SCREEN_KEYBOARD_ACTION_CLASSES;

	/// Action classes available for trigger (half-axis) components.
	private static final List<Class<?>> TRIGGER_ACTION_CLASSES;

	@Serial
	private static final long serialVersionUID = 5007388251349678609L;

	static {
		final List<Class<?>> mutableAxisActionClasses = new ArrayList<>();
		final List<Class<?>> mutableTriggerActionClasses = new ArrayList<>();
		final List<Class<?>> mutableButtonActionClasses = new ArrayList<>();
		final List<Class<?>> mutableCycleActionClasses = new ArrayList<>();
		final List<Class<?>> mutableOnScreenKeyboardActionClasses = new ArrayList<>();

		try (final var scanResult = new ClassGraph().acceptPackages(IAction.class.getPackageName()).enableClassInfo()
				.enableAnnotationInfo().scan()) {
			final var classInfoList = scanResult.getClassesWithAnnotation(Action.class.getName());
			classInfoList.sort((classInfo1, classInfo2) -> {
				final var actionAnnotation1 = classInfo1.loadClass().getAnnotation(Action.class);
				final var actionAnnotation2 = classInfo2.loadClass().getAnnotation(Action.class);

				return actionAnnotation1.order() - actionAnnotation2.order();
			});

			classInfoList.forEach(classInfo -> {
				final var actionClass = classInfo.loadClass();
				final var actionAnnotation = actionClass.getAnnotation(Action.class);
				final var category = actionAnnotation.category();

				if (category == ActionCategory.ALL || category == ActionCategory.AXIS
						|| category == ActionCategory.AXIS_AND_TRIGGER) {
					mutableAxisActionClasses.add(actionClass);
				}
				if (category == ActionCategory.ALL || category == ActionCategory.AXIS_AND_TRIGGER) {
					mutableTriggerActionClasses.add(actionClass);
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

		AXIS_ACTION_CLASSES = Collections.unmodifiableList(mutableAxisActionClasses);
		TRIGGER_ACTION_CLASSES = Collections.unmodifiableList(mutableTriggerActionClasses);
		BUTTON_ACTION_CLASSES = Collections.unmodifiableList(mutableButtonActionClasses);
		CYCLE_ACTION_CLASSES = Collections.unmodifiableList(mutableCycleActionClasses);
		ON_SCREEN_KEYBOARD_ACTION_CLASSES = Collections.unmodifiableList(mutableOnScreenKeyboardActionClasses);

		final var inputStream = EditActionsDialog.class.getResourceAsStream(HELP_GIF_RESOURCE_PATH);
		if (inputStream == null) {
			throw new RuntimeException("Resource not found: " + HELP_GIF_RESOURCE_PATH);
		}

		try {
			final var bufferedImage = ImageIO.read(inputStream);

			HELP_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(bufferedImage, HELP_CURSOR_HOT_SPOT, "help");
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/// The list displaying currently assigned actions.
	private final JList<IAction<?>> assignedActionsList = new ViewportWidthTrackingJList<>() {

		private static final int PLACEHOLDER_MESSAGE_MARGIN = 5;

		@Serial
		private static final long serialVersionUID = -3862365536659647863L;

		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);

			if (getModel().getSize() == 0) {
				final var g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2d.setColor(UIManager.getColor("Label.disabledForeground"));

				final var fontMetrics = g2d.getFontMetrics();
				final var text = Main.STRINGS.getString("NO_ASSIGNED_ACTIONS_PLACEHOLDER");

				if (fontMetrics.stringWidth(text) > getWidth() - PLACEHOLDER_MESSAGE_MARGIN * 2) {
					return;
				}

				final var x = (getWidth() - fontMetrics.stringWidth(text)) / 2;
				final var y = (getHeight() - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();

				g2d.drawString(text, x, y);
			}
		}
	};

	/// The list displaying available action types that can be added.
	private final JList<Class<?>> availableActionsList = new ViewportWidthTrackingJList<>();

	/// The list of sub-actions being edited in cycle-editor mode.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final List<IAction<Boolean>> cycleActions = new ArrayList<>();

	/// The editor pane used to display help text for the selected action.
	private final JEditorPane helpEditorPane = GuiUtils.createHtmlViewerEditorPane();

	/// The main application instance.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final Main main;

	/// A working copy of the profile modified by this dialog before saving.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private final Profile unsavedProfile;

	/// The controller component whose actions are being edited, or `null` in
	/// cycle-editor mode.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private Component component;

	/// The cycle action whose sub-actions are being edited, or `null` in
	/// component-editor mode.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private ButtonToCycleAction cycleAction;

	/// The input instance associated with the current editing session.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private Input input;

	/// The paste button, enabled only when the clipboard holds a compatible action.
	private JButton pasteActionButton;

	/// The label shown above the properties panel when a property editor is
	/// visible.
	private JLabel propertiesLabel;

	/// The panel containing property editors for the selected action.
	private JPanel propertiesPanel;

	/// The scroll pane wrapping the properties panel.
	private JScrollPane propertiesScrollPane;

	/// The action currently selected in the assigned-actions list.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private IAction<?> selectedAssignedAction;

	/// The action type currently selected in the available-actions list.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private Class<?> selectedAvailableActionClass;

	/// The mode currently selected in the mode combo box.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private Mode selectedMode;

	/// Creates a cycle editor dialog for editing the sub-actions of a
	/// [ButtonToCycleAction].
	///
	/// @param parentDialog the parent [EditActionsDialog] that owns this cycle
	/// editor
	/// @param cycleAction the cycle action whose sub-actions are to be edited
	@SuppressWarnings("unchecked")
	public EditActionsDialog(@SuppressWarnings("exports") final EditActionsDialog parentDialog,
			@SuppressWarnings("exports") final ButtonToCycleAction cycleAction) {
		super(parentDialog);

		main = parentDialog.main;
		this.cycleAction = cycleAction;

		try {
			for (final var action : cycleAction.getActions()) {
				cycleActions.add((IAction<Boolean>) action.clone());
			}
		} catch (final CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}

		unsavedProfile = parentDialog.unsavedProfile;

		preInit(parentDialog);
		setTitle(MessageFormat.format(Main.STRINGS.getString("EDIT_ACTIONS_DIALOG_TITLE_CYCLE_ACTION_EDITOR"),
				IAction.getLabel(cycleAction.getClass())));

		init();
	}

	/// Creates a component editor dialog for editing the actions assigned to a
	/// specific controller component within a profile.
	///
	/// @param main the main application instance owning this dialog
	/// @param component the controller component whose actions are to be edited
	/// @param name the display name of the component, used in the dialog title
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
		setTitle(MessageFormat.format(Main.STRINGS.getString("EDIT_ACTIONS_DIALOG_TITLE_COMPONENT_EDITOR"), name));

		final var modes = unsavedProfile.getModes();
		selectedMode = modes.getFirst();
		GuiUtils.addModePanel(getContentPane(), modes, new AbstractAction() {

			@Serial
			private static final long serialVersionUID = -9107064465015662054L;

			@SuppressWarnings("unchecked")
			@Override
			public void actionPerformed(final ActionEvent e) {
				selectedMode = (Mode) ((JComboBox<Mode>) e.getSource()).getSelectedItem();
				updateAvailableActions();
				updateAssignedActions();
			}
		});

		init();
	}

	/// Applies the selection or default background and foreground colors from the
	/// given [JList] to the specified component.
	///
	/// @param component the component whose colors are updated
	/// @param list the list providing selection and default colors
	/// @param isSelected whether the item is currently selected
	private static void applyListColors(final java.awt.Component component, final JList<?> list,
			final boolean isSelected) {
		if (isSelected) {
			component.setBackground(list.getSelectionBackground());
			component.setForeground(list.getSelectionForeground());
		} else {
			component.setBackground(list.getBackground());
			component.setForeground(list.getForeground());
		}
	}

	/// Collects all fields of the given class that are annotated with
	/// [ActionProperty] and adds them to the provided map.
	///
	/// Fields already present in the map are not overwritten, preserving entries
	/// from subclasses over those from superclasses.
	///
	/// @param clazz the class whose declared fields are inspected
	/// @param map the map to populate with field-to-annotation entries
	private static void collectFields(final Class<?> clazz, final Map<Field, ActionProperty> map) {
		for (final var field : clazz.getDeclaredFields()) {
			final var annotation = field.getAnnotation(ActionProperty.class);
			if (annotation != null) {
				map.putIfAbsent(field, annotation);
			}
		}
	}

	/// Returns the smallest non-negative integer not present in the given stream,
	/// or the next integer after the maximum present value, capped at `maxValue`.
	///
	/// Returns `0` if the stream is empty. If the sequence is contiguous from the
	/// first value, returns the value immediately following the last element,
	/// capped at `maxValue`.
	///
	/// @param numbers the stream of integers to examine; values above `maxValue`
	/// are ignored
	/// @param maxValue the inclusive upper bound for the returned value
	/// @return the first missing or next integer in the range 0 to maxValue
	static int findFirstMissingOrNext(final IntStream numbers, final int maxValue) {
		final var it = numbers.filter(n -> n <= maxValue).distinct().sorted().iterator();

		if (!it.hasNext()) {
			return 0;
		}

		var expected = it.nextInt();
		if (expected > maxValue) {
			return maxValue;
		}

		while (it.hasNext()) {
			final var current = it.nextInt();
			if (current != expected + 1) {
				final var candidate = expected + 1;
				return Math.min(candidate, maxValue);
			}
			expected = current;
		}

		return Math.min(expected + 1, maxValue);
	}

	/// Returns a map of fields to their [ActionProperty] annotations for the given
	/// action class.
	/// Results are cached for performance. The map includes fields from the class
	/// itself and all superclasses that implement [IAction].
	///
	/// @param actionClass the action class to inspect; must implement [IAction]
	/// @return an unmodifiable map of fields to their action property annotations
	/// @throws IllegalArgumentException if the provided class does not implement
	/// [IAction]
	public static Map<Field, ActionProperty> getFieldToActionPropertiesMap(final Class<?> actionClass) {
		if (!IAction.class.isAssignableFrom(actionClass)) {
			throw new IllegalArgumentException(
					"Parameter actionClass does not implement " + IAction.class.getSimpleName());
		}

		synchronized (FIELD_ACTION_PROPERTY_MAP_CACHE) {
			return FIELD_ACTION_PROPERTY_MAP_CACHE.computeIfAbsent(actionClass, clazz -> {
				final var propertyMap = new HashMap<Field, ActionProperty>();

				collectFields(clazz, propertyMap);

				var currentClass = clazz.getSuperclass();
				while (currentClass != null && currentClass != Object.class
						&& IAction.class.isAssignableFrom(currentClass)) {
					final var cached = FIELD_ACTION_PROPERTY_MAP_CACHE.get(currentClass);
					if (cached != null) {
						propertyMap.putAll(cached);
						break;
					}

					collectFields(currentClass, propertyMap);

					currentClass = currentClass.getSuperclass();
				}

				return Map.copyOf(propertyMap);
			});
		}
	}

	/// Adds the given action to the appropriate data structure and refreshes the
	/// dialog's action lists.
	///
	/// Depending on the action type and editor mode, the action is appended to the
	/// button-to-mode actions map, the cycle sub-action list, or the
	/// component-to-actions map of the currently selected mode. The assigned-
	/// and available-actions lists are then refreshed, and the appropriate row is
	/// selected in the assigned-actions list.
	///
	/// @param action the action instance to add
	@SuppressWarnings("unchecked")
	private void addAction(final IAction<?> action) {
		if (action instanceof final ButtonToModeAction buttonToModeAction) {
			final var buttonToModeActionsMap = unsavedProfile.getButtonToModeActionsMap();
			if (!buttonToModeActionsMap.containsKey(component.index())) {
				buttonToModeActionsMap.put(component.index(), new ArrayList<>());
			}

			buttonToModeActionsMap.get(component.index()).add(buttonToModeAction);
		} else if (isCycleEditor()) {
			cycleActions.add((IAction<Boolean>) action);
		} else {
			final var componentToActionMap = (Map<Integer, List<IAction<?>>>) selectedMode
					.getComponentToActionsMap(component.type());

			if (!componentToActionMap.containsKey(component.index())) {
				componentToActionMap.put(component.index(), new ArrayList<>());
			}

			componentToActionMap.get(component.index()).add(action);
		}

		updateAvailableActions();
		updateAssignedActions();

		final var hasModeAction = Arrays.stream(getAssignedActions())
				.anyMatch(assignedAction -> assignedAction instanceof ButtonToModeAction);

		assignedActionsList.setSelectedIndex(assignedActionsList.getLastVisibleIndex()
				- (hasModeAction && !(action instanceof ButtonToModeAction) ? 1 : 0));
	}

	/// Hides the dialog and dispatches a window-closing event to trigger any
	/// registered window listeners.
	private void closeDialog() {
		setVisible(false);
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	/// Creates and returns a new instance of the given action class, initializing
	/// smart defaults for [ButtonToModeAction], [ToAxisAction], and
	/// [ToButtonAction]
	/// based on the current profile state.
	///
	/// For [ButtonToModeAction], sets the target mode to the second profile mode or
	/// the on-screen keyboard mode as a fallback. For [ToAxisAction], assigns the
	/// first unused virtual axis ordinal. For [ToButtonAction], assigns the first
	/// unused button ID.
	///
	/// @param actionClass the action class to instantiate; must implement [IAction]
	/// @return the newly created and initialized action instance
	/// @throws IllegalAccessException if the constructor is not accessible
	/// @throws InstantiationException if the class represents an abstract class
	/// @throws InvocationTargetException if the constructor throws an exception
	/// @throws NoSuchMethodException if no no-arg constructor exists
	@SuppressWarnings("EnumOrdinal")
	private IAction<?> getActionClassInstance(final Class<?> actionClass)
			throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		if (!IAction.class.isAssignableFrom(actionClass)) {
			throw new IllegalArgumentException(
					"Class '" + actionClass.getName() + "' does not implement '" + IAction.class.getSimpleName() + "'");
		}

		final var action = (IAction<?>) actionClass.getConstructor().newInstance();

		switch (action) {
		case final ButtonToModeAction buttonToModeAction -> {
			final var modes = input.getProfile().getModes();
			final var defaultMode = modes.size() > 1 ? modes.get(1) : OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE;
			buttonToModeAction.setMode(defaultMode);
		}
		case final ToAxisAction<?> toAxisAction -> {
			final var virtualAxisIndex = findFirstMissingOrNext(
					unsavedProfile.getModes().stream().flatMapToInt(mode -> mode.getAxisToActionsMap().values().stream()
							.flatMapToInt(actions -> actions.stream().mapMultiToInt((action1, downstream) -> {
								if (action1 instanceof final ToAxisAction<?> toAxisAction1) {
									downstream.accept(toAxisAction1.getVirtualAxis().ordinal());
								}
							}))),
					VirtualAxis.values().length - 1);

			toAxisAction.setVirtualAxis(VirtualAxis.values()[virtualAxisIndex]);
		}
		case final ToButtonAction<?> toButtonAction -> {
			final var buttonId = findFirstMissingOrNext(unsavedProfile.getModes().stream()
					.flatMapToInt(mode -> Stream
							.of(mode.getAxisToActionsMap().values(), mode.getButtonToActionsMap().values(),
									List.of((List<? extends IAction<?>>) cycleActions))
							.flatMap(Collection::stream).flatMap(List::stream).mapMultiToInt((action1, downstream) -> {
								if (action1 instanceof final ButtonToCycleAction buttonToCycleAction) {
									buttonToCycleAction.getActions().forEach(action2 -> {
										if (action2 instanceof final ToButtonAction<?> toButtonAction1) {
											downstream.accept(toButtonAction1.getButtonId());
										}
									});
								} else if (action1 instanceof final ToButtonAction<?> toButtonAction1) {
									downstream.accept(toButtonAction1.getButtonId());
								}
							})),
					Input.MAX_N_BUTTONS - 1);

			toButtonAction.setButtonId(buttonId);
		}
		default -> {
		}
		}

		return action;
	}

	/// Returns the list of action classes that may be assigned to the current
	/// component or cycle, based on the editor mode and the selected mode and
	/// component type.
	///
	/// Cycle editors return cycle-compatible classes. On-screen keyboard mode
	/// returns the keyboard-specific class list. Trigger axes return
	/// trigger-compatible classes. Axis components return axis-compatible classes.
	/// Button components in the default mode return the full button class list;
	/// other modes exclude [ButtonToModeAction].
	///
	/// @return the list of allowed action classes for the current context
	private List<Class<?>> getAllowedActionClasses() {
		if (isCycleEditor()) {
			return CYCLE_ACTION_CLASSES;
		} else if (OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.equals(selectedMode)) {
			return ON_SCREEN_KEYBOARD_ACTION_CLASSES;
		} else if (component.type() == ComponentType.AXIS) {
			final var componentIndex = component.index();
			if (componentIndex == SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER
					|| componentIndex == SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER) {
				return TRIGGER_ACTION_CLASSES;
			}
			return AXIS_ACTION_CLASSES;
		} else if (Profile.DEFAULT_MODE.equals(selectedMode)) {
			return BUTTON_ACTION_CLASSES;
		}

		return BUTTON_ACTION_CLASSES.stream().filter(clazz -> clazz != ButtonToModeAction.class).toList();
	}

	/// Returns an array of [IAction]s representing all actions currently assigned
	/// to the component or cycle being edited.
	///
	/// In cycle-editor mode, adds each action from the local cycle action list.
	/// In component-editor mode, adds actions from the selected mode's
	/// component-to-actions map and, for button components in the default mode,
	/// also includes any [ButtonToModeAction] entries from the profile.
	///
	/// @return an array of assigned actions
	@SuppressWarnings("unchecked")
	private IAction<?>[] getAssignedActions() {
		final var assignedActions = new ArrayList<IAction<?>>();

		final var cycleEditor = isCycleEditor();

		if (cycleEditor && cycleActions != null) {
			assignedActions.addAll(cycleActions);
		} else if (component != null) {
			final var componentActions = selectedMode.getComponentToActionsMap(component.type()).get(component.index());
			if (componentActions != null) {
				assignedActions.addAll(((Collection<? extends IAction<?>>) componentActions));
			}
		}

		if (!cycleEditor && component.type() == ComponentType.BUTTON && Profile.DEFAULT_MODE.equals(selectedMode)) {
			final var buttonToModeActions = unsavedProfile.getButtonToModeActionsMap().get(component.index());
			if (buttonToModeActions != null) {
				assignedActions.addAll(buttonToModeActions);
			}
		}

		return assignedActions.toArray(IAction<?>[]::new);
	}

	/// Returns the [Input] instance associated with this dialog.
	///
	/// @return the input instance, or `null` if this is a cycle editor
	@SuppressWarnings("exports")
	public Input getInput() {
		return input;
	}

	/// Builds and populates the dialog's content pane with the actions panel,
	/// available-actions list, assigned-actions list, action buttons, clipboard
	/// controls, properties panel, help panel, OK, and Cancel buttons.
	///
	/// This method must be called after [#preInit].
	private void init() {
		final var actionsPanel = new JPanel(new GridBagLayout());
		actionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		getContentPane().add(actionsPanel, BorderLayout.CENTER);

		actionsPanel.add(new JLabel(Main.STRINGS.getString("AVAILABLE_ACTIONS_LABEL")), new GridBagConstraints(0, 0, 1,
				1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));

		availableActionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final var cellBorder = BorderFactory.createEmptyBorder(0, 0, 0, 5);
		availableActionsList.setCellRenderer((list, value, _, isSelected, _) -> {
			final var iconLabel = new IconLabel(value, list, isSelected);
			iconLabel.setBorder(cellBorder);
			return iconLabel;
		});

		actionsPanel.add(GuiUtils.wrapComponentInScrollPane(availableActionsList),
				new GridBagConstraints(0, 1, 1, 2, ACTIONS_LIST_WEIGHT_X, 1d, GridBagConstraints.CENTER,
						GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

		final var addActionButton = new JButton(new AddActionAction());
		addActionButton.setPreferredSize(Main.BUTTON_DIMENSION);
		addActionButton.setEnabled(false);
		actionsPanel.add(addActionButton, new GridBagConstraints(0, 3, 1, 1, 0d, 0d, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));

		final var clipboardPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 0));
		final var titledBorder = BorderFactory.createTitledBorder(Main.STRINGS.getString("CLIPBOARD_BORDER_TITLE"));
		final var emptyBorder = (EmptyBorder) BorderFactory.createEmptyBorder(5, 0, 5, 0);
		final var border = BorderFactory.createCompoundBorder(titledBorder, emptyBorder);
		clipboardPanel.setBorder(border);

		availableActionsList.addListSelectionListener(_ -> {
			selectedAvailableActionClass = availableActionsList.getSelectedValue();
			addActionButton.setEnabled(selectedAvailableActionClass != null);
			updateHelp(selectedAvailableActionClass);
		});

		assignedActionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		assignedActionsList.setCellRenderer((list, value, _, isSelected, _) -> {
			final var panel = new JPanel(new BorderLayout());
			panel.setBorder(cellBorder);
			applyListColors(panel, list, isSelected);

			final var iconLabel = new IconLabel(value.getClass(), list, isSelected);
			panel.add(iconLabel, BorderLayout.NORTH);

			final var description = value.getDescription(input);
			if (!Objects.equals(iconLabel.getTitle(), description)) {
				final var descriptionLabel = new JLabel(description);
				descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0,
						ICON_LABEL_DIMENSION.width + IconLabel.ICON_LABEL_SPACING, 0, 0));
				applyListColors(descriptionLabel, list, isSelected);
				final var defaultForegroundColor = descriptionLabel.getForeground();
				final var dimmedForegroundColor = new Color(defaultForegroundColor.getRed(),
						defaultForegroundColor.getGreen(), defaultForegroundColor.getBlue(), 192);
				descriptionLabel.setForeground(dimmedForegroundColor);
				panel.add(descriptionLabel, BorderLayout.SOUTH);
			}

			return panel;
		});

		actionsPanel.add(GuiUtils.wrapComponentInScrollPane(assignedActionsList),
				new GridBagConstraints(1, 1, 1, 1, ACTIONS_LIST_WEIGHT_X, 1d, GridBagConstraints.CENTER,
						GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

		actionsPanel.add(new JLabel(Main.STRINGS.getString("ASSIGNED_ACTIONS_LABEL")), new GridBagConstraints(1, 0, 1,
				1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));

		final var cutActionButton = new JButton(new CutActionAction());
		cutActionButton.setEnabled(false);
		cutActionButton.setPreferredSize(Main.SQUARE_BUTTON_DIMENSION);
		cutActionButton.setMinimumSize(Main.SQUARE_BUTTON_DIMENSION);
		cutActionButton.setMaximumSize(Main.SQUARE_BUTTON_DIMENSION);
		cutActionButton.setAlignmentX(CENTER_ALIGNMENT);
		cutActionButton.setAlignmentY(CENTER_ALIGNMENT);
		clipboardPanel.add(cutActionButton);

		final var copyActionButton = new JButton(new CopyActionAction());
		copyActionButton.setEnabled(false);
		copyActionButton.setPreferredSize(Main.SQUARE_BUTTON_DIMENSION);
		copyActionButton.setMinimumSize(Main.SQUARE_BUTTON_DIMENSION);
		copyActionButton.setMaximumSize(Main.SQUARE_BUTTON_DIMENSION);
		copyActionButton.setAlignmentX(CENTER_ALIGNMENT);
		copyActionButton.setAlignmentY(CENTER_ALIGNMENT);
		clipboardPanel.add(copyActionButton);

		pasteActionButton = new JButton(new PasteActionAction());
		pasteActionButton.setEnabled(false);
		pasteActionButton.setPreferredSize(Main.SQUARE_BUTTON_DIMENSION);
		pasteActionButton.setMinimumSize(Main.SQUARE_BUTTON_DIMENSION);
		pasteActionButton.setMaximumSize(Main.SQUARE_BUTTON_DIMENSION);
		pasteActionButton.setAlignmentX(CENTER_ALIGNMENT);
		pasteActionButton.setAlignmentY(CENTER_ALIGNMENT);
		clipboardPanel.add(pasteActionButton);

		actionsPanel.add(clipboardPanel, new GridBagConstraints(1, 2, 1, 1, 0d, 0d, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		final var removeActionButton = new JButton(new RemoveActionAction());
		removeActionButton.setPreferredSize(Main.BUTTON_DIMENSION);
		removeActionButton.setEnabled(false);
		actionsPanel.add(removeActionButton, new GridBagConstraints(1, 3, 1, 1, 0d, 0d, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));

		assignedActionsList.addListSelectionListener(_ -> {
			selectedAssignedAction = assignedActionsList.getSelectedValue();

			final var notNull = selectedAssignedAction != null;
			removeActionButton.setEnabled(notNull);
			cutActionButton.setEnabled(notNull);
			copyActionButton.setEnabled(notNull);

			updateProperties();
			updateHelp(notNull ? selectedAssignedAction.getClass() : null);
		});

		final var helpScrollPane = GuiUtils.wrapComponentInScrollPane(helpEditorPane);
		actionsPanel.add(helpScrollPane, new GridBagConstraints(0, 4, 2, 1, 0d, 0.5, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		final var helpScrollPaneBorder = BorderFactory.createTitledBorder(Main.STRINGS.getString("HELP_BORDER_TITLE"));
		helpScrollPane.setBorder(helpScrollPaneBorder);
		final var helpScrollPaneBorderInsets = helpScrollPaneBorder.getBorder().getBorderInsets(helpScrollPane);

		propertiesLabel = new JLabel(Main.STRINGS.getString("PROPERTIES_LABEL"));
		propertiesLabel.setVisible(false);
		actionsPanel.add(propertiesLabel, new GridBagConstraints(2, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));

		propertiesScrollPane = new JScrollPane();
		propertiesScrollPane.setVisible(false);
		actionsPanel.add(propertiesScrollPane,
				new GridBagConstraints(2, 1, 1, 5, 1d, 1d, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(5, 5, helpScrollPaneBorderInsets.top + helpScrollPaneBorderInsets.bottom, 0), 0, 0));

		final var okCancelButtonPanel = new JPanel();
		okCancelButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		final var okButton = new JButton(new OKAction());
		okButton.setPreferredSize(Main.BUTTON_DIMENSION);
		okCancelButtonPanel.add(okButton);
		getRootPane().setDefaultButton(okButton);

		final var cancelButton = new JButton(new CancelAction());
		cancelButton.setPreferredSize(Main.BUTTON_DIMENSION);
		okCancelButtonPanel.add(cancelButton);

		getContentPane().add(okCancelButtonPanel, BorderLayout.SOUTH);

		updateAvailableActions();
		updateAssignedActions();
		updateHelp(null);
	}

	/// Determines whether this dialog is operating as a cycle editor (editing
	/// sub-actions of a [ButtonToCycleAction]) rather than a component editor.
	///
	/// @return `true` if this dialog is a cycle editor, `false` if it is a
	/// component editor
	public boolean isCycleEditor() {
		return component == null;
	}

	/// Performs common pre-initialization shared by all constructors: sets modal,
	/// configures the content pane layout, and positions and sizes the dialog
	/// relative to the given parent component.
	///
	/// @param parentComponent the component used to derive the dialog's initial
	/// position and as the offset reference
	private void preInit(final java.awt.Component parentComponent) {
		setModal(true);
		getContentPane().setLayout(new BorderLayout());

		final var bounds = parentComponent.getBounds();
		bounds.x += DIALOG_BOUNDS_PARENT_OFFSET;
		bounds.y += DIALOG_BOUNDS_PARENT_OFFSET;
		bounds.width = DIALOG_BOUNDS_WIDTH;
		bounds.height = DIALOG_BOUNDS_HEIGHT;

		GuiUtils.setBoundsWithMinimum(this, bounds);
	}

	/// Prevents deserialization.
	///
	/// @param ignoredStream the object input stream (unused)
	/// @throws NotSerializableException always
	@Serial
	private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(EditActionsDialog.class.getName());
	}

	/// Removes the currently selected assigned action from the component or cycle.
	///
	/// Deletes the action selected in the assigned-actions list from the
	/// appropriate data structure: the button-to-mode actions map for
	/// [ButtonToModeAction] instances, the cycle sub-action list in cycle-editor
	/// mode, or the component-to-actions map in component-editor mode. After
	/// removal, it refreshes both the available- and assigned-actions lists.
	private void removeAction() {
		if (selectedAssignedAction instanceof final ButtonToModeAction buttonToModeAction) {
			final var buttonToModeActionsMap = unsavedProfile.getButtonToModeActionsMap();
			buttonToModeActionsMap.get(component.index()).remove(buttonToModeAction);
			if (buttonToModeActionsMap.get(component.index()).isEmpty()) {
				buttonToModeActionsMap.remove(component.index());
			}
		} else if (isCycleEditor()) {
			cycleActions.remove(selectedAssignedAction);
		} else {
			final var componentToActionMap = selectedMode.getComponentToActionsMap(component.type());
			@SuppressWarnings("unchecked")
			final var actions = (List<IAction<?>>) componentToActionMap.get(component.index());
			actions.remove(selectedAssignedAction);

			if (actions.isEmpty()) {
				componentToActionMap.remove(component.index());
			}
		}

		updateAvailableActions();
		updateAssignedActions();
	}

	/// Repaints the assigned-actions list to reflect changes in action properties.
	public void repaintAssignedActionsList() {
		assignedActionsList.repaint();
	}

	/// Refreshes the assigned-actions list to reflect the current state of the
	/// edited component or cycle.
	private void updateAssignedActions() {
		assignedActionsList.setListData(getAssignedActions());
	}

	/// Refreshes the available-actions list and the paste button based on the
	/// current context.
	///
	/// Rebuilds the list from the allowed action classes for the current mode and
	/// component type, excluding [ButtonToModeAction] when not in the default mode,
	/// and then updates the enabled state of the paste button.
	private void updateAvailableActions() {
		Objects.requireNonNull(pasteActionButton, "Field pasteButton must not be null");

		final var availableActions = getAllowedActionClasses().stream()
				.filter(actionClass -> !ButtonToModeAction.class.equals(actionClass)
						|| Profile.DEFAULT_MODE.equals(selectedMode))
				.toArray(Class<?>[]::new);

		availableActionsList.setListData(availableActions);

		updatePasteButton();
	}

	/// Updates the help panel to show the title and description for the given
	/// action class, or a default "no selection" message if `actionClass` is
	/// `null`.
	///
	/// @param actionClass the action class whose help text is displayed, or `null`
	/// to show the default help text
	private void updateHelp(final Class<?> actionClass) {
		final String title;
		String descriptionLabel = null;

		if (actionClass != null) {
			title = IAction.getLabel(actionClass);
			final var actionAnnotation = actionClass.getAnnotation(Action.class);
			if (actionAnnotation != null) {
				descriptionLabel = actionAnnotation.description();
			}
		} else {
			title = Main.STRINGS.getString("NO_SELECTION_HELP_TITLE");
			descriptionLabel = "NO_SELECTION_HELP_DESCRIPTION";
		}

		updateHelp(title, descriptionLabel);
	}

	/// Updates the help panel with the given title and a description looked up from
	/// the resource bundle via `descriptionLabel`.
	///
	/// If `descriptionLabel` is `null`, blank, or not found in the resource bundle,
	/// falls back to the "no help available" description string. The HTML content
	/// of the help editor pane is rebuilt, and the caret is reset to the top.
	///
	/// @param title the heading text displayed in the help panel
	/// @param descriptionLabel the resource bundle key for the description text, or
	/// `null` to use the fallback
	private void updateHelp(final String title, final String descriptionLabel) {
		String description = null;
		if (descriptionLabel != null && !descriptionLabel.isBlank()) {
			try {
				description = Main.STRINGS.getString(descriptionLabel);
			} catch (final MissingResourceException _) {
				// handled below
			}
		}

		if ( description == null || description.isBlank()) {
			description = Main.STRINGS.getString("NO_HELP_AVAILABLE_HELP_DESCRIPTION");
		}

		helpEditorPane.setText(
				"<html>" + (title != null && !title.isBlank() ? "<h3 style='margin-top: 0'>" + title + "</h3>" : "")
						+ description + "</html>");
		helpEditorPane.setCaretPosition(0);
	}

	/// Updates the enabled state of the paste button based on whether the
	/// clipboard contains an action that is allowed in the current context.
	///
	/// The paste button is enabled only when the application clipboard holds a
	/// non-`null` action whose class is present in the list returned by
	/// [#getAllowedActionClasses].
	private void updatePasteButton() {
		final var clipboardAction = main.getClipboardAction();
		final var pasteAllowed = clipboardAction != null
				&& getAllowedActionClasses().contains(clipboardAction.getClass());

		pasteActionButton.setEnabled(pasteAllowed);
	}

	/// Updates the properties panel to reflect the currently selected assigned
	/// action. Rebuilds the property editors for each annotated field of the
	/// selected action or hides the panel if no action is selected.
	public void updateProperties() {
		Objects.requireNonNull(propertiesLabel, "Field propertiesLabel must not be null");
		Objects.requireNonNull(propertiesScrollPane, "Field propertiesScrollPane must not be null");

		if (propertiesPanel != null) {
			propertiesPanel.removeAll();
			propertiesPanel = null;
		}

		if (selectedAssignedAction != null) {
			final var actionClass = selectedAssignedAction.getClass();
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
				propertiesPanel.add(propertyPanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d,
						GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 10));

				final var propertyIcon = annotation.icon();
				final var propertyTitle = Main.STRINGS.getString(annotation.title());
				final var propertyDescriptionLabel = annotation.description();

				final var propertyNameLabel = new JLabel(propertyTitle);
				propertyNameLabel.setPreferredSize(new Dimension(155, 15));
				if (propertyDescriptionLabel != null && !propertyDescriptionLabel.isBlank()) {
					propertyNameLabel.setCursor(HELP_CURSOR);
					propertyNameLabel.addMouseListener(new MouseAdapter() {

						private final Font originalFont = propertyNameLabel.getFont();

						@Override
						public void mouseClicked(final MouseEvent e) {
							updateHelp(propertyIcon + " " + propertyTitle, propertyDescriptionLabel);
						}

						@Override
						public void mouseEntered(final MouseEvent e) {
							setUnderlineEnabled(true);
						}

						@Override
						public void mouseExited(final MouseEvent e) {
							setUnderlineEnabled(false);
						}

						/// Sets whether the property label is displayed with an underline font.
						///
						/// @param enabled `true` to apply underline, `false` to restore the original
						/// font
						private void setUnderlineEnabled(final boolean enabled) {
							final Font newFont;
							if (enabled) {
								final var newAttributes = new HashMap<TextAttribute, Object>(
										originalFont.getAttributes());
								newAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
								newFont = originalFont.deriveFont(newAttributes);
							} else {
								newFont = originalFont;
							}

							propertyNameLabel.setFont(newFont);
						}
					});
				}
				propertyPanel.add(new IconLabel(propertyIcon, propertyTitle, propertyNameLabel));

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
					final var editorBuilder = constructor.newInstance(this, selectedAssignedAction, fieldName,
							fieldType);

					editorBuilder.buildEditor(propertyPanel);
				} catch (final IllegalAccessException | InstantiationException | InvocationTargetException
						| NoSuchMethodException e1) {
					throw new RuntimeException(e1);
				}
			}
		}

		final var anyPropertiesFound = propertiesPanel != null;
		if (anyPropertiesFound) {
			propertiesPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1d, 1d,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

			propertiesScrollPane.setViewportView(propertiesPanel);
		}
		propertiesLabel.setVisible(anyPropertiesFound);
		propertiesScrollPane.setVisible(anyPropertiesFound);
		revalidate();
	}

	/// Prevents serialization.
	///
	/// @param ignoredStream the object output stream (unused)
	/// @throws NotSerializableException always
	@Serial
	private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
		throw new NotSerializableException(EditActionsDialog.class.getName());
	}

	/// A panel that displays an icon alongside a title label in a horizontal
	/// layout.
	///
	/// This component is used to render action entries in the edit-actions dialog,
	/// showing a fixed-size icon on the left followed by a title. It supports both
	/// custom icon and title strings as well as construction from an action class
	/// annotation, adapting its colors to match the list selection state.
	private static final class IconLabel extends JPanel {

		/// Horizontal spacing in pixels between the icon and the title label.
		private static final int ICON_LABEL_SPACING = 5;

		@Serial
		private static final long serialVersionUID = 3892761470881313182L;

		/// The label displaying the icon.
		private final JLabel iconLabel;

		/// The label displaying the title text.
		private final JLabel titleLabel;

		/// Creates a new [IconLabel] with the specified icon, title, and optional title
		/// label.
		///
		/// If `titleLabel` is `null`, a new [JLabel] is created. The panel uses a
		/// horizontal [BoxLayout] with the icon sized to [#ICON_LABEL_DIMENSION],
		/// followed by a small strut and the title label.
		///
		/// @param icon the icon text to display
		/// @param title the title text to display
		/// @param titleLabel an existing label to reuse for the title, or `null` to
		/// create a new one
		private IconLabel(final String icon, final String title, JLabel titleLabel) {
			if (titleLabel == null) {
				titleLabel = new JLabel();
			}

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setOpaque(true);

			iconLabel = new JLabel(icon);
			iconLabel.setPreferredSize(ICON_LABEL_DIMENSION);
			iconLabel.setMinimumSize(ICON_LABEL_DIMENSION);
			iconLabel.setMaximumSize(ICON_LABEL_DIMENSION);
			iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
			add(iconLabel);

			add(Box.createHorizontalStrut(5));

			this.titleLabel = titleLabel;
			this.titleLabel.setText(title);
			add(this.titleLabel);
		}

		/// Creates a new [IconLabel] from an action class annotation, styled for list
		/// rendering.
		///
		/// Extracts the icon and title from the [Action] annotation on the given class
		/// and sets the foreground and background colors based on the list selection
		/// state.
		///
		/// @param actionClass the action class whose [Action] annotation provides the
		/// icon and title
		/// @param list the list component providing selection colors
		/// @param isSelected whether the item is currently selected in the list
		private IconLabel(final Class<?> actionClass, final JList<?> list, final boolean isSelected) {
			final var annotation = actionClass.getAnnotation(Action.class);
			this(annotation.icon(), Main.STRINGS.getString(annotation.title()), null);

			applyListColors(this, list, isSelected);
		}

		/// Returns the title text displayed by this icon label.
		///
		/// @return the title text
		private String getTitle() {
			return titleLabel.getText();
		}

		/// Sets the foreground color of the panel and propagates it to the icon and
		/// title labels.
		///
		/// The `null` checks are necessary because this method may be called during
		/// superclass initialization before the labels are assigned.
		///
		/// @param fg the foreground color to set
		@Override
		public void setForeground(final Color fg) {
			super.setForeground(fg);

			if (iconLabel != null) {
				iconLabel.setForeground(fg);
			}

			if (titleLabel != null) {
				titleLabel.setForeground(fg);
			}
		}
	}

	/// A [JList] subclass that always tracks the viewport width of its enclosing
	/// scroll pane.
	///
	/// Overrides [JList#getScrollableTracksViewportWidth()] to return `true`,
	/// ensuring the list stretches horizontally to fill the available viewport
	/// width rather than scrolling.
	///
	/// @param <E> the type of elements in the list
	private static class ViewportWidthTrackingJList<E> extends JList<E> {

		@Serial
		private static final long serialVersionUID = -5416293197170067165L;

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}
	}

	/// Adds a new instance of the currently selected available action to the
	/// component or cycle.
	///
	/// When performed, instantiates the action class selected in the
	/// available-actions list and delegates to the enclosing dialog's add-action
	/// logic.
	private final class AddActionAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -7713175853948284887L;

		/// Creates a new [AddActionAction] and initializes its name and description.
		private AddActionAction() {
			putValue(NAME, Main.STRINGS.getString("ADD_ACTION_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, Main.STRINGS.getString("ADD_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				addAction(getActionClassInstance(selectedAvailableActionClass));
			} catch (final InstantiationException | IllegalAccessException | InvocationTargetException
					| NoSuchMethodException e1) {
				LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}

	/// Closes the dialog without saving any changes.
	///
	/// When performed, discards all unsaved edits and closes the
	/// [EditActionsDialog] by calling the enclosing dialog's close logic.
	private final class CancelAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 8086810563127997199L;

		/// Creates a new [CancelAction] and initializes its name and description.
		private CancelAction() {
			putValue(NAME, UIManager.getString("OptionPane.cancelButtonText"));
			putValue(SHORT_DESCRIPTION, Main.STRINGS.getString("CANCEL_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			closeDialog();
		}
	}

	/// Copies the currently selected assigned action to the clipboard.
	///
	/// When performed, clones the action selected in the assigned-actions list and
	/// stores the clone in the application clipboard via [Main], enabling it to be
	/// pasted into another component or mode.
	private class CopyActionAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -6630683334825900710L;

		/// Creates a new [CopyActionAction] and initializes its name and description.
		private CopyActionAction() {
			putValue(NAME, "🗐");
			putValue(SHORT_DESCRIPTION, Main.STRINGS.getString("COPY_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				main.setClipboardAction((IAction<?>) selectedAssignedAction.clone());
				updatePasteButton();
			} catch (final CloneNotSupportedException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	/// Cuts the currently selected assigned action to the clipboard.
	///
	/// When performed, clones the action selected in the assigned-actions list,
	/// stores the clone in the application clipboard via [Main], enabling it to be
	/// pasted into another component or mode, and removes the original from the
	/// assigned actions.
	private final class CutActionAction extends CopyActionAction {

		@Serial
		private static final long serialVersionUID = -6630683334825900710L;

		/// Creates a new [CutActionAction] and initializes its name and description.
		private CutActionAction() {
			putValue(NAME, "✂");
			putValue(SHORT_DESCRIPTION, Main.STRINGS.getString("CUT_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			super.actionPerformed(e);
			removeAction();
		}
	}

	/// Saves all pending changes and closes the dialog.
	///
	/// When performed in cycle-editor mode, commits the edited sub-action list back
	/// to the [ButtonToCycleAction]. In component-editor mode, applies the updated
	/// profile to the [Input], refreshes the relevant UI panels in [Main], and
	/// marks the profile as having unsaved changes before closing.
	private final class OKAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -6947022759101822700L;

		/// Creates a new [OKAction] and initializes its name and description.
		private OKAction() {
			putValue(NAME, UIManager.getString("OptionPane.okButtonText"));
			putValue(SHORT_DESCRIPTION, Main.STRINGS.getString("OK_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (isCycleEditor()) {
				cycleAction.setActions(cycleActions);
			} else {
				var requiresOnScreenKeyboardMode = false;
				outer: for (final var buttonToModeActions : unsavedProfile.getButtonToModeActionsMap().values()) {
					for (final var buttonToModeAction : buttonToModeActions) {
						if (buttonToModeAction.targetsOnScreenKeyboardMode()) {
							requiresOnScreenKeyboardMode = true;
							break outer;
						}
					}
				}

				if (requiresOnScreenKeyboardMode
						&& !unsavedProfile.getModes().contains(OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE)) {
					unsavedProfile.getModes().add(OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE);
				} else if (!requiresOnScreenKeyboardMode) {
					unsavedProfile.getModes().remove(OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE);
				}

				input.setProfile(unsavedProfile);
				main.updateModesPanel(false);
				main.updateOverlayPanel();
				main.updateVisualizationPanel();
				main.setUnsavedChanges(true);
			}

			closeDialog();
		}
	}

	/// Pastes a cloned copy of the clipboard action into the current component or
	/// cycle.
	///
	/// When performed, retrieves the action stored in the application clipboard via
	/// [Main], clones it, and delegates to the enclosing dialog's add-action logic,
	/// so the copy is appended to the assigned-actions list.
	private final class PasteActionAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -6630683334825900710L;

		/// Creates a new [PasteActionAction] and initializes its name and description.
		private PasteActionAction() {
			putValue(NAME, "📋");
			putValue(SHORT_DESCRIPTION, Main.STRINGS.getString("PASTE_ACTION_ACTION_DESCRIPTION"));
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

	/// Removes the currently selected assigned action from the component or cycle.
	///
	/// Delegates to [#removeAction] which deletes the action selected in the
	/// assigned-actions list.
	private final class RemoveActionAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -5681740772832902238L;

		/// Creates a new [RemoveActionAction] and initializes its name and description.
		private RemoveActionAction() {
			putValue(NAME, Main.STRINGS.getString("REMOVE_ACTION_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, Main.STRINGS.getString("REMOVE_ACTION_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			removeAction();
		}
	}
}

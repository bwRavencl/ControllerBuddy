/*
 * Copyright (C) 2019 Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action.gui;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.ScanCode;
import de.bwravencl.controllerbuddy.input.action.IAction;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/// Editor builder for keystroke properties, rendering two filterable checkbox
/// lists for selecting modifier and key scan codes.
///
/// Displays a visualization of the resulting key combination that updates
/// in real time as the user selects or deselects scan codes. Both lists
/// support prefix and wildcard filtering to quickly locate specific keys.
public final class KeystrokeEditorBuilder extends EditorBuilder {

	/// Width in pixels of the key list scroll pane.
	private static final int KEY_LIST_SCROLL_PANE_WIDTH = 120;

	/// Preferred size for key list scroll panes.
	private static final Dimension KEY_LIST_SCROLL_PANE_DIMENSION = new Dimension(KEY_LIST_SCROLL_PANE_WIDTH, 200);

	private static final Logger LOGGER = Logger.getLogger(KeystrokeEditorBuilder.class.getName());

	/// Label showing the `+` separator between modifier and key lists.
	private final JLabel plusLabel = new JLabel("+");

	/// Panel displaying a live visualization of the currently selected keystroke.
	private final JPanel visualizationPanel = new JPanel();

	/// List of selectable normal (non-modifier) key scan codes.
	private CheckboxJList<?> keyList;

	/// List of selectable modifier scan codes.
	private CheckboxJList<?> modifierList;

	/// Constructs a keystroke editor builder for the specified action property.
	///
	/// @param editActionsDialog the parent dialog hosting the editor
	/// @param action the action whose keystroke property is being edited
	/// @param fieldName the name of the property field
	/// @param fieldType the type of the property field
	/// @throws IllegalAccessException if the property cannot be accessed
	/// @throws InvocationTargetException if the property getter throws an exception
	/// @throws NoSuchMethodException if the property getter method is not found
	public KeystrokeEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	/// Adds a label for each scan code in the list to the given panel.
	///
	/// If the list is empty, a horizontal strut of the standard list width is
	/// added instead, preserving layout spacing. Non-breaking spaces replace
	/// regular spaces in the label text, so word-wrapping does not occur.
	///
	/// @param scanCodes the list of scan codes whose names are displayed
	/// @param panel the panel to which the labels are added
	private static void addScanCodeLabels(final List<?> scanCodes, final JPanel panel) {
		if (scanCodes.isEmpty()) {
			panel.add(Box.createHorizontalStrut(KEY_LIST_SCROLL_PANE_WIDTH));
			return;
		}

		scanCodes.stream().map(scanCode -> scanCode.toString().replace(" ", "\u00A0")).forEach(text -> {
			final var scanCodeLabel = new JLabel(text);
			scanCodeLabel
					.setPreferredSize(new Dimension(KEY_LIST_SCROLL_PANE_WIDTH, scanCodeLabel.getMinimumSize().height));
			scanCodeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			panel.add(scanCodeLabel);
		});
	}

	/// Returns the index of the given value within the list model.
	///
	/// Uses [DefaultListModel#indexOf] when possible for efficiency, otherwise
	/// performs a linear scan. Returns -1 if the value is `null` or not found.
	///
	/// @param model the list model to search
	/// @param value the value to locate in the model
	/// @return the zero-based index of the value, or -1 if not found
	private static int getListModelIndex(final ListModel<?> model, final Object value) {
		if (value == null) {
			return -1;
		}

		if (model instanceof final DefaultListModel<?> defaultListModel) {
			return defaultListModel.indexOf(value);
		}

		for (var i = 0; i < model.getSize(); i++) {
			if (value.equals(model.getElementAt(i))) {
				return i;
			}
		}

		return -1;
	}

	/// Builds a labeled, filterable checkbox list for selecting scan codes and
	/// adds it to the keystroke panel at the given layout constraints.
	///
	/// The list is populated with all known scan code names and is wired to a
	/// selection listener that updates the [KeyStroke] property and refreshes
	/// the visualization whenever the selection changes.
	///
	/// @param labelKey the resource bundle key for the list header label
	/// @param keyStroke the keystroke whose property is updated on selection change
	/// @param keystrokePanel the panel to which the list panel is added
	/// @param constraints the layout constraint string passed to the panel
	/// @param scanCodeConsumer the consumer that receives the updated scan code
	/// array
	/// @return the constructed [CheckboxJList]
	private CheckboxJList<String> buildCheckboxList(final String labelKey, final KeyStroke keyStroke,
			final JPanel keystrokePanel, final String constraints, final Consumer<ScanCode[]> scanCodeConsumer) {
		final var listData = ScanCode.NAME_TO_SCAN_CODE_MAP.keySet().toArray(String[]::new);

		final var checkboxList = new CheckboxJList<>(listData);
		checkboxList.addListSelectionListener(
				new JListSetPropertyListSelectionListener(setterMethod, keyStroke, scanCodeConsumer));

		final var checkboxListCellRenderer = new CheckboxListCellRenderer<>(checkboxList);
		checkboxList.setCellRenderer(checkboxListCellRenderer);

		final var borderColor = UIManager.getColor("Component.borderColor");

		final var listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor),
				BorderFactory.createEmptyBorder(2, 0, 0, 0)));

		final var label = new JLabel(Main.STRINGS.getString(labelKey));
		label.setAlignmentX(Component.CENTER_ALIGNMENT);

		listPanel.add(label);

		listPanel.add(Box.createVerticalStrut(5));

		final var utilityPanel = new JPanel();
		final var boxLayout = new BoxLayout(utilityPanel, BoxLayout.X_AXIS);
		utilityPanel.setLayout(boxLayout);
		utilityPanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));

		final var deselectAllButton = new JButton(new DeselectAllAction(checkboxList.getSelectionModel()));
		deselectAllButton.setFont(deselectAllButton.getFont().deriveFont(9f));
		final var deselectButtonSize = new Dimension(14, 14);
		deselectAllButton.setPreferredSize(deselectButtonSize);
		deselectAllButton.setMaximumSize(deselectButtonSize);
		utilityPanel.add(deselectAllButton);

		utilityPanel.add(Box.createHorizontalStrut(6));

		final var filterTextField = new FilterTextField(checkboxListCellRenderer);
		utilityPanel.add(filterTextField);

		final var clearFilterButton = new JButton();
		final var clearFilterAction = new ClearFilterAction(clearFilterButton, filterTextField);
		clearFilterButton.setAction(clearFilterAction);
		clearFilterButton.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 1, 1, borderColor), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
		utilityPanel.add(clearFilterButton);

		listPanel.add(utilityPanel);

		listPanel.add(Box.createVerticalStrut(5));

		final var scrollPane = GuiUtils.wrapComponentInScrollPane(checkboxList, KEY_LIST_SCROLL_PANE_DIMENSION);
		scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor));
		listPanel.add(scrollPane);

		keystrokePanel.add(listPanel, constraints);

		return checkboxList;
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var keystrokePanel = new JPanel(new BorderLayout(5, 5));
		parentPanel.add(keystrokePanel);

		final var keyStroke = (KeyStroke) initialValue;
		modifierList = buildCheckboxList("MODIFIERS_LABEL", keyStroke, keystrokePanel, BorderLayout.WEST,
				keyStroke::setModifierCodes);
		keyList = buildCheckboxList("KEYS_LABEL", keyStroke, keystrokePanel, BorderLayout.EAST, keyStroke::setKeyCodes);

		plusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		plusLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

		visualizationPanel.setLayout(new BoxLayout(visualizationPanel, BoxLayout.X_AXIS));
		keystrokePanel.add(visualizationPanel, BorderLayout.SOUTH);

		initListSelection(modifierList, keyStroke.getModifierCodes());
		initListSelection(keyList, keyStroke.getKeyCodes());

		updateUpdateKeyStrokeVisualization();
	}

	/// Selects the list items that correspond to the given scan codes and scrolls
	/// to the first selected item.
	///
	/// @param list the list in which to apply the selection
	/// @param scanCodes the scan codes that should be selected
	private void initListSelection(final JList<?> list, final ScanCode[] scanCodes) {
		Arrays.stream(scanCodes).map(ScanCode::name).forEach(scanCodeName -> {
			final var index = getListModelIndex(list.getModel(), scanCodeName);
			if (index >= 0) {
				list.addSelectionInterval(index, index);
			}
		});

		final var minSelectionIndex = list.getMinSelectionIndex();
		if (minSelectionIndex >= 0) {
			list.ensureIndexIsVisible(minSelectionIndex);
		}
	}

	/// Rebuilds the keystroke visualization panel to reflect the current modifier
	/// and key selections.
	///
	/// Clears the visualization panel and repopulates it with labels for the
	/// currently selected modifier and key scan codes, separated by a "+" label
	/// when both modifiers and keys are selected.
	private void updateUpdateKeyStrokeVisualization() {
		visualizationPanel.removeAll();

		final var selectedModifiersList = modifierList != null ? modifierList.getSelectedValuesList() : List.of();

		final var selectedKeysList = keyList != null ? keyList.getSelectedValuesList() : List.of();

		final var modifierVisualizationPanel = new JPanel();
		modifierVisualizationPanel.setLayout(new BoxLayout(modifierVisualizationPanel, BoxLayout.Y_AXIS));
		addScanCodeLabels(selectedModifiersList, modifierVisualizationPanel);
		visualizationPanel.add(modifierVisualizationPanel);

		visualizationPanel.add(Box.createHorizontalGlue());
		visualizationPanel.add(Box.createHorizontalStrut(5));

		if (!selectedModifiersList.isEmpty() && !selectedKeysList.isEmpty()) {
			visualizationPanel.add(plusLabel);
		} else {
			visualizationPanel.add(Box.createHorizontalStrut(plusLabel.getMinimumSize().width));
		}

		visualizationPanel.add(Box.createHorizontalStrut(5));
		visualizationPanel.add(Box.createHorizontalGlue());

		final var keyVisualizationPanel = new JPanel();
		keyVisualizationPanel.setLayout(new BoxLayout(keyVisualizationPanel, BoxLayout.Y_AXIS));
		addScanCodeLabels(selectedKeysList, keyVisualizationPanel);
		visualizationPanel.add(keyVisualizationPanel);

		visualizationPanel.revalidate();
	}

	/// A [JList] variant that uses a toggle-select model, rendering each item as a
	/// checkbox so that clicking an already-selected row deselects it.
	///
	/// The default mouse-motion listener that would cause drag-selection is
	/// removed to prevent unintended multi-selection when the user moves the
	/// pointer while clicking.
	///
	/// @param <E> the element type of the list
	private static final class CheckboxJList<E> extends JList<E> {

		@Serial
		private static final long serialVersionUID = 5413881551745215922L;

		/// Constructs a checkbox list populated with the given items and installs a
		/// toggle-select model.
		///
		/// @param listData the initial items to display in the list
		private CheckboxJList(final E[] listData) {
			super(listData);

			for (final var mouseMotionListener : getMouseMotionListeners()) {
				if (mouseMotionListener instanceof ListSelectionListener) {
					removeMouseMotionListener(mouseMotionListener);
				}
			}

			setSelectionModel(new DefaultListSelectionModel() {

				@Serial
				private static final long serialVersionUID = 8997996268575032389L;

				@Override
				public void setSelectionInterval(final int index0, final int index1) {
					if (isSelectedIndex(index0)) {
						removeSelectionInterval(index0, index1);
					} else {
						addSelectionInterval(index0, index1);
					}
				}

				@Override
				public void setValueIsAdjusting(final boolean isAdjusting) {
				}
			});
		}
	}

	/// Cell renderer that displays each list item as a [JCheckBox] and hides items
	/// that do not match the active filter string.
	///
	/// Supports prefix matching (default) and wildcard matching when the filter
	/// starts with `*`, in which case the remainder is matched as a substring.
	/// Items that do not match are replaced with an invisible glue component, so
	/// the list layout is not disrupted.
	///
	/// @param <E> the element type of the list being rendered
	private static final class CheckboxListCellRenderer<E> extends JCheckBox implements ListCellRenderer<E> {

		/// Pattern matching a wildcard filter string starting with `*`.
		private static final Pattern WILDCARD_FILTER_PATTERN = Pattern.compile("^\\*(.+)");

		@Serial
		private static final long serialVersionUID = -7958791166718006570L;

		/// The list this renderer is attached to.
		private final JList<? extends E> list;

		/// The current filter string applied when rendering cells.
		private String filter;

		/// Constructs a renderer that tracks the given list for orientation, font,
		/// and color settings.
		///
		/// @param list the list this renderer is attached to
		private CheckboxListCellRenderer(final JList<? extends E> list) {
			this.list = list;
		}

		@Override
		public Component getListCellRendererComponent(final JList<? extends E> list, final E value, final int index,
				final boolean isSelected, final boolean cellHasFocus) {
			if (filter != null && !filter.isEmpty()) {
				final var valueString = value.toString().toLowerCase(Locale.ROOT);

				var match = false;

				final var wildcardMatcher = WILDCARD_FILTER_PATTERN.matcher(filter);
				if (wildcardMatcher.matches()) {
					match = valueString.contains(wildcardMatcher.group(1));
				} else {
					match = valueString.startsWith(filter);
				}

				if (!match) {
					return Box.createVerticalGlue();
				}
			}

			setComponentOrientation(list.getComponentOrientation());

			setFont(list.getFont());
			setText(String.valueOf(value));

			setBackground(list.getBackground());
			setForeground(list.getForeground());

			setSelected(isSelected);
			setEnabled(list.isEnabled());

			return this;
		}

		/// Sets the filter string used to hide non-matching list items.
		///
		/// The text is normalized to lower case before storing. If the new value
		/// equals the current filter, no update is performed; otherwise the filter
		/// is updated, and a property change is fired on the list to trigger a repaint.
		///
		/// @param text the filter string, or `null` to clear the filter
		private void setFilter(String text) {
			if (text != null) {
				text = text.toLowerCase(Locale.ROOT);
			}

			if (Objects.equals(filter, text)) {
				return;
			}

			filter = text;
			list.firePropertyChange("fixedCellHeight", 0, -1);
		}
	}

	/// Action that clears the text in the associated [FilterTextField] and returns
	/// focus to it.
	///
	/// Listens to document changes on the filter field and keeps the clear button
	/// visible only while the filter contains text, hiding it automatically when
	/// the field is empty.
	private static final class ClearFilterAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -1551195858919523623L;

		/// The button that is shown or hidden depending on whether a filter is active.
		private final JButton clearFilterButton;

		/// The text field whose content is cleared when this action is performed.
		private final FilterTextField filterTextField;

		/// Constructs the action, wires up a document listener to keep the clear
		/// button visibility in sync with the filter text field content.
		///
		/// @param clearFilterButton the button whose visibility this action controls
		/// @param filterTextField the text field whose content is cleared when the
		/// action is performed
		private ClearFilterAction(final JButton clearFilterButton, final FilterTextField filterTextField) {
			this.clearFilterButton = clearFilterButton;
			this.filterTextField = filterTextField;

			putValue(NAME, "⨯");
			putValue(SHORT_DESCRIPTION, Main.STRINGS.getString("CLEAR_FILTER_ACTION_DESCRIPTION"));

			filterTextField.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void changedUpdate(final DocumentEvent e) {
					updateState();
				}

				@Override
				public void insertUpdate(final DocumentEvent e) {
					updateState();
				}

				@Override
				public void removeUpdate(final DocumentEvent e) {
					updateState();
				}
			});

			updateState();
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			filterTextField.setText(null);
			filterTextField.grabFocus();
		}

		/// Updates the visibility of the clear button based on whether the filter is
		/// active.
		private void updateState() {
			clearFilterButton.setVisible(filterTextField.isFilterActive());
		}
	}

	/// Action that clears all selected items in the associated
	/// [ListSelectionModel].
	///
	/// Listens to selection changes and disables itself automatically when the
	/// selection is already empty, so the button is only enabled when there is
	/// something to deselect.
	private static final class DeselectAllAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -5034928593330512532L;

		/// The selection model that is cleared when this action is performed.
		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final ListSelectionModel listSelectionModel;

		/// Constructs the action, registers a selection listener to keep the enabled
		/// state in sync, and performs an initial state update.
		///
		/// @param listSelectionModel the selection model to clear when the action is
		/// performed
		private DeselectAllAction(final ListSelectionModel listSelectionModel) {
			this.listSelectionModel = listSelectionModel;

			putValue(NAME, "⛶");
			putValue(SHORT_DESCRIPTION, Main.STRINGS.getString("DESELECT_ALL_ACTION_DESCRIPTION"));

			listSelectionModel.addListSelectionListener((_) -> updateState());
			updateState();
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			listSelectionModel.clearSelection();
		}

		/// Prevents deserialization.
		///
		/// @param ignoredStream the object input stream (unused)
		/// @throws NotSerializableException always
		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(DeselectAllAction.class.getName());
		}

		/// Updates the enabled state based on whether the selection model has any
		/// selection.
		private void updateState() {
			setEnabled(!listSelectionModel.isSelectionEmpty());
		}

		/// Prevents serialization.
		///
		/// @param ignoredStream the object output stream (unused)
		/// @throws NotSerializableException always
		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(DeselectAllAction.class.getName());
		}
	}

	/// A [JTextField] that forwards its content to a [CheckboxListCellRenderer] as
	/// a filter string and renders an italic placeholder when the field is empty.
	///
	/// Each document change immediately updates the renderer filter, causing the
	/// list to repaint and hide items that do not match the current input.
	private static final class FilterTextField extends JTextField {

		@Serial
		private static final long serialVersionUID = -7998118986240546988L;

		/// Constructs the text field and registers a document listener that forwards
		/// each text change to the given renderer as a filter.
		///
		/// @param checkboxListCellRenderer the renderer whose filter is updated on
		/// every
		/// document change
		private FilterTextField(final CheckboxListCellRenderer<?> checkboxListCellRenderer) {
			super(1);

			getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void changedUpdate(final DocumentEvent e) {
					filter();
				}

				/// Forwards the current text field content to the renderer as a filter.
				private void filter() {
					checkboxListCellRenderer.setFilter(getText());
				}

				@Override
				public void insertUpdate(final DocumentEvent e) {
					filter();
				}

				@Override
				public void removeUpdate(final DocumentEvent e) {
					filter();
				}
			});
		}

		/// Returns whether the filter text field contains any text.
		///
		/// @return `true` if the field is non-empty, `false` otherwise
		private boolean isFilterActive() {
			final var text = getText();

			return text != null && !text.isEmpty();
		}

		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);

			if (isFilterActive()) {
				return;
			}

			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(getDisabledTextColor());
			g.setFont(g.getFont().deriveFont(Font.ITALIC));
			g.drawString(Main.STRINGS.getString("FILTER_PLACEHOLDER"), getInsets().left,
					g.getFontMetrics().getMaxAscent() + getInsets().top);
		}
	}

	/// Selection listener that converts the selected list items to [ScanCode]
	/// values, updates the [KeyStroke] property, and refreshes the keystroke
	/// visualization.
	///
	/// Invokes the action's setter method with the updated [KeyStroke] each time
	/// the user changes the selection in the modifier or key list, keeping the
	/// action property and the visualization panel in sync.
	private final class JListSetPropertyListSelectionListener implements ListSelectionListener {

		/// The keystroke being edited by this listener.
		private final KeyStroke keyStroke;

		/// Consumer that receives the updated array of scan codes.
		private final Consumer<ScanCode[]> scanCodeConsumer;

		/// The setter method to invoke with the updated keystroke.
		private final Method setterMethod;

		/// Constructs the listener with the setter method, keystroke, and scan code
		/// consumer.
		///
		/// @param setterMethod the setter method to invoke with the updated keystroke
		/// @param keyStroke the keystroke object that is updated and passed to the
		/// setter
		/// @param scanCodeConsumer the consumer that receives the new scan code array
		/// on
		/// each selection change
		private JListSetPropertyListSelectionListener(final Method setterMethod, final KeyStroke keyStroke,
				final Consumer<ScanCode[]> scanCodeConsumer) {
			this.setterMethod = setterMethod;
			this.keyStroke = keyStroke;
			this.scanCodeConsumer = scanCodeConsumer;
		}

		@Override
		public void valueChanged(final ListSelectionEvent e) {
			try {
				final Set<ScanCode> scanCodes = new HashSet<>();

				// noinspection SuspiciousMethodCalls
				((JList<?>) e.getSource()).getSelectedValuesList()
						.forEach(object -> scanCodes.add(ScanCode.NAME_TO_SCAN_CODE_MAP.get(object)));

				scanCodeConsumer.accept(scanCodes.toArray(ScanCode[]::new));

				setterMethod.invoke(action, keyStroke);

				updateUpdateKeyStrokeVisualization();
			} catch (final IllegalAccessException | InvocationTargetException e1) {
				LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}
}

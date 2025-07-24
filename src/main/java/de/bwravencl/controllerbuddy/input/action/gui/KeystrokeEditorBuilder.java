/* Copyright (C) 2019  Matteo Hausner
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
import java.util.Collections;
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

public final class KeystrokeEditorBuilder extends EditorBuilder {

	private static final int KEY_LIST_SCROLL_PANE_WIDTH = 120;

	private static final Dimension KEY_LIST_SCROLL_PANE_DIMENSION = new Dimension(KEY_LIST_SCROLL_PANE_WIDTH, 200);

	private static final Logger LOGGER = Logger.getLogger(KeystrokeEditorBuilder.class.getName());

	private final JLabel plusLabel = new JLabel("+");

	private final JPanel visualizationPanel = new JPanel();

	private CheckboxJList<?> keyList;

	private CheckboxJList<?> modifierList;

	public KeystrokeEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws SecurityException, NoSuchMethodException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	private static void addScanCodeLabels(final List<?> scanCodes, final JPanel panel) {
		if (scanCodes.isEmpty()) {
			panel.add(Box.createHorizontalStrut(KEY_LIST_SCROLL_PANE_WIDTH));
			return;
		}

		scanCodes.stream().map(scanCode -> scanCode.toString().replaceAll(" ", "\u00A0")).forEach(text -> {
			final var scanCodeLabel = new JLabel(text);
			scanCodeLabel
					.setPreferredSize(new Dimension(KEY_LIST_SCROLL_PANE_WIDTH, scanCodeLabel.getMinimumSize().height));
			scanCodeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			panel.add(scanCodeLabel);
		});
	}

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

	private void updateUpdateKeyStrokeVisualization() {
		visualizationPanel.removeAll();

		final var selectedModifiersList = modifierList != null ? modifierList.getSelectedValuesList()
				: Collections.emptyList();

		final var selectedKeysList = keyList != null ? keyList.getSelectedValuesList() : Collections.emptyList();

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

	private static final class CheckboxJList<E> extends JList<E> {

		@Serial
		private static final long serialVersionUID = 5413881551745215922L;

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

	private static final class CheckboxListCellRenderer<E> extends JCheckBox implements ListCellRenderer<E> {

		private static final Pattern WILDCARD_FILTER_PATTERN = Pattern.compile("^\\*(.+)");

		@Serial
		private static final long serialVersionUID = -7958791166718006570L;

		private final JList<? extends E> list;

		private String filter;

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

	private static final class ClearFilterAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -1551195858919523623L;

		private final JButton clearFilterButton;

		private final FilterTextField filterTextField;

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

		private void updateState() {
			clearFilterButton.setVisible(filterTextField.isFilterActive());
		}
	}

	private static final class DeselectAllAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -5034928593330512532L;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final ListSelectionModel listSelectionModel;

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

		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(DeselectAllAction.class.getName());
		}

		private void updateState() {
			setEnabled(!listSelectionModel.isSelectionEmpty());
		}

		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(DeselectAllAction.class.getName());
		}
	}

	private static class FilterTextField extends JTextField {

		@Serial
		private static final long serialVersionUID = -7998118986240546988L;

		private FilterTextField(final CheckboxListCellRenderer<?> checkboxListCellRenderer) {
			super(1);

			getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void changedUpdate(final DocumentEvent e) {
					filter();
				}

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

	private final class JListSetPropertyListSelectionListener implements ListSelectionListener {

		private final KeyStroke keyStroke;

		private final Consumer<ScanCode[]> scanCodeConsumer;

		private final Method setterMethod;

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
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}
}

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
import java.awt.FlowLayout;
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class KeystrokeEditorBuilder extends EditorBuilder {

	private static final Logger log = Logger.getLogger(KeystrokeEditorBuilder.class.getName());

	private static final int KEY_LIST_SCROLL_PANE_WIDTH = 110;
	private static final Dimension KEY_LIST_SCROLL_PANE_DIMENSION = new Dimension(KEY_LIST_SCROLL_PANE_WIDTH, 200);

	private final JPanel visualizationPanel = new JPanel();
	private final JLabel plusLabel = new JLabel("+");
	private CheckboxJList<?> modifierList;
	private CheckboxJList<?> keyList;

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
		final var checkboxList = new CheckboxJList<>(ScanCode.nameToScanCodeMap.keySet().toArray(String[]::new));
		checkboxList.addListSelectionListener(
				new JListSetPropertyListSelectionListener(setterMethod, keyStroke, scanCodeConsumer));

		final var borderColor = UIManager.getColor("Component.borderColor");

		final var listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor),
				BorderFactory.createEmptyBorder(2, 0, 0, 0)));

		final var label = new JLabel(Main.strings.getString(labelKey));
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		listPanel.add(label);

		listPanel.add(Box.createVerticalStrut(5));

		final var deselectAllPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		final var deselectAllButton = new JButton(new DeselectAllAction(checkboxList.getSelectionModel()));
		deselectAllButton.setFont(deselectAllButton.getFont().deriveFont(11f));
		deselectAllButton.setPreferredSize(new Dimension(15, 15));
		deselectAllPanel.add(deselectAllButton);
		listPanel.add(deselectAllPanel);

		listPanel.add(Box.createVerticalStrut(5));

		final var scrollPane = GuiUtils.wrapComponentInScrollPane(checkboxList, KEY_LIST_SCROLL_PANE_DIMENSION);
		scrollPane.setBorder(new MatteBorder(1, 0, 0, 0, borderColor));
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

			setCellRenderer(new CheckboxListCellRenderer<>());

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

		private static final class CheckboxListCellRenderer<E> extends JCheckBox implements ListCellRenderer<E> {

			@Serial
			private static final long serialVersionUID = -7958791166718006570L;

			@Override
			public Component getListCellRendererComponent(final JList<? extends E> list, final E value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				setComponentOrientation(list.getComponentOrientation());

				setFont(list.getFont());
				setText(String.valueOf(value));

				setBackground(list.getBackground());
				setForeground(list.getForeground());

				setSelected(isSelected);
				setEnabled(list.isEnabled());

				return this;
			}
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
			putValue(SHORT_DESCRIPTION, Main.strings.getString("DESELECT_ALL_ACTION_DESCRIPTION"));

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

	private final class JListSetPropertyListSelectionListener implements ListSelectionListener {

		private final Method setterMethod;
		private final KeyStroke keyStroke;
		private final Consumer<ScanCode[]> scanCodeConsumer;

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
						.forEach(object -> scanCodes.add(ScanCode.nameToScanCodeMap.get(object)));

				scanCodeConsumer.accept(scanCodes.toArray(ScanCode[]::new));

				setterMethod.invoke(action, keyStroke);

				updateUpdateKeyStrokeVisualization();
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				log.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}
}

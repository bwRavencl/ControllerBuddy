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
import java.awt.EventQueue;
import java.awt.Insets;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public final class KeystrokeEditorBuilder extends EditorBuilder {

	private static final Logger log = Logger.getLogger(KeystrokeEditorBuilder.class.getName());

	private static final Dimension KEY_LIST_SCROLL_PANE_DIMENSION = new Dimension(110, 200);

	private final KeyStrokeTextPane modifiersTextArea = new KeyStrokeTextPane();
	private final KeyStrokeTextPane keysTextArea = new KeyStrokeTextPane();
	private final JLabel plusLabel = new JLabel("+");
	private CheckboxJList<?> modifierList;
	private CheckboxJList<?> keyList;

	public KeystrokeEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws SecurityException, NoSuchMethodException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
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

	@SuppressWarnings("DuplicatedCode")
	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var keystrokePanel = new JPanel(new BorderLayout(5, 5));
		parentPanel.add(keystrokePanel);

		final var keyStroke = (KeyStroke) initialValue;

		final var availableScanCodes = ScanCode.nameToScanCodeMap.keySet();

		final var modifiersPanel = new JPanel();
		modifiersPanel.setLayout(new BoxLayout(modifiersPanel, BoxLayout.PAGE_AXIS));
		final var modifiersLabel = new JLabel(Main.strings.getString("MODIFIERS_LABEL"));
		modifiersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		modifiersPanel.add(modifiersLabel);
		modifiersPanel.add(Box.createVerticalStrut(5));
		modifierList = new CheckboxJList<>(availableScanCodes.toArray(String[]::new));
		modifierList.addListSelectionListener(new JListSetPropertyListSelectionListener(setterMethod, keyStroke, true));

		modifiersPanel.add(GuiUtils.wrapComponentInScrollPane(modifierList, KEY_LIST_SCROLL_PANE_DIMENSION));

		modifiersPanel.add(Box.createVerticalStrut(5));
		modifiersPanel.add(modifiersTextArea);

		keystrokePanel.add(modifiersPanel, BorderLayout.WEST);

		final var keysPanel = new JPanel();
		keysPanel.setLayout(new BoxLayout(keysPanel, BoxLayout.PAGE_AXIS));
		final var keysLabel = new JLabel(Main.strings.getString("KEYS_LABEL"));
		keysLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		keysPanel.add(keysLabel);
		keysPanel.add(Box.createVerticalStrut(5));
		keyList = new CheckboxJList<>(availableScanCodes.toArray(String[]::new));
		keyList.addListSelectionListener(new JListSetPropertyListSelectionListener(setterMethod, keyStroke, false));

		keysPanel.add(GuiUtils.wrapComponentInScrollPane(keyList, KEY_LIST_SCROLL_PANE_DIMENSION));

		keysPanel.add(Box.createVerticalStrut(5));
		keysPanel.add(keysTextArea);

		keystrokePanel.add(keysPanel, BorderLayout.EAST);

		final var plusPanel = new JPanel();
		plusPanel.setLayout(new BoxLayout(plusPanel, BoxLayout.PAGE_AXIS));
		plusPanel.setPreferredSize(new Dimension(10, plusPanel.getPreferredSize().height));
		plusPanel.add(Box.createVerticalStrut(
				modifiersPanel.getPreferredSize().height - modifiersTextArea.getPreferredSize().height));
		plusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		plusLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
		plusPanel.add(plusLabel);
		keystrokePanel.add(plusPanel, BorderLayout.CENTER);

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
		final var selectedModifiersList = modifierList != null ? modifierList.getSelectedValuesList()
				: Collections.emptyList();
		modifiersTextArea.setScanCodes(selectedModifiersList);

		final var selectedKeysList = keyList != null ? keyList.getSelectedValuesList() : Collections.emptyList();
		keysTextArea.setScanCodes(selectedKeysList);

		plusLabel.setVisible(!selectedModifiersList.isEmpty() && !selectedKeysList.isEmpty());
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

	private static final class KeyStrokeTextPane extends JTextPane {

		@Serial
		private static final long serialVersionUID = 4814218567890032503L;

		private KeyStrokeTextPane() {
			setEditable(false);
			setFocusable(false);
			setMargin(new Insets(0, 5, 0, 5));
			setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);

			final var styledDocument = getStyledDocument();
			final var attributeSet = new SimpleAttributeSet();
			StyleConstants.setAlignment(attributeSet, StyleConstants.ALIGN_CENTER);
			styledDocument.setParagraphAttributes(0, styledDocument.getLength(), attributeSet, false);

			getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void changedUpdate(final DocumentEvent e) {
					updateSize();
				}

				@Override
				public void insertUpdate(final DocumentEvent e) {
					updateSize();
				}

				@Override
				public void removeUpdate(final DocumentEvent e) {
					updateSize();
				}

				private void updateSize() {
					repaint();
					EventQueue.invokeLater(() -> {
						setPreferredSize(new Dimension(getMinimumSize().width, getMinimumSize().height));
						revalidate();
					});
				}
			});
		}

		private void setScanCodes(final List<?> scanCodes) {
			setText(scanCodes.stream().map(scanCode -> scanCode.toString().replaceAll(" ", "\u00A0"))
					.collect(Collectors.joining(" + ")));
		}
	}

	private final class JListSetPropertyListSelectionListener implements ListSelectionListener {

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
				final Set<ScanCode> scanCodes = new HashSet<>();

				// noinspection SuspiciousMethodCalls
				((JList<?>) e.getSource()).getSelectedValuesList()
						.forEach(object -> scanCodes.add(ScanCode.nameToScanCodeMap.get(object)));

				final var scanCodesArray = scanCodes.toArray(ScanCode[]::new);

				if (modifiers) {
					keyStroke.setModifierCodes(scanCodesArray);
				} else {
					keyStroke.setKeyCodes(scanCodesArray);
				}

				setterMethod.invoke(action, keyStroke);

				updateUpdateKeyStrokeVisualization();
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				log.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}
}

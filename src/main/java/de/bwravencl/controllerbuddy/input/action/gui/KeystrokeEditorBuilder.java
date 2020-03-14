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

package de.bwravencl.controllerbuddy.input.action.gui;

import static de.bwravencl.controllerbuddy.gui.Main.strings;

import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.bwravencl.controllerbuddy.gui.EditActionsDialog;
import de.bwravencl.controllerbuddy.input.DirectInputKeyCode;
import de.bwravencl.controllerbuddy.input.KeyStroke;
import de.bwravencl.controllerbuddy.input.action.IAction;

public final class KeystrokeEditorBuilder extends EditorBuilder {

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
				final Set<Integer> scanCodes = new HashSet<>();

				for (final Object o : ((JList<?>) e.getSource()).getSelectedValuesList())
					scanCodes.add(DirectInputKeyCode.nameToKeyCodeMap.get(o));

				final Integer[] scanCodesArray = scanCodes.toArray(new Integer[scanCodes.size()]);

				if (modifiers)
					keyStroke.setModifierCodes(scanCodesArray);
				else
					keyStroke.setKeyCodes(scanCodesArray);

				setterMethod.invoke(action, keyStroke);
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				log.log(Level.SEVERE, e1.getMessage(), e1);
			}
		}
	}

	private static final Logger log = Logger.getLogger(KeystrokeEditorBuilder.class.getName());

	private static int getListModelIndex(final ListModel<?> model, final Object value) {
		if (value == null)
			return -1;

		if (model instanceof DefaultListModel)
			return ((DefaultListModel<?>) model).indexOf(value);

		for (var i = 0; i < model.getSize(); i++)
			if (value.equals(model.getElementAt(i)))
				return i;

		return -1;
	}

	public KeystrokeEditorBuilder(final EditActionsDialog editActionsDialog, final IAction<?> action,
			final String fieldName, final Class<?> fieldType) throws NoSuchFieldException, SecurityException,
			NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(editActionsDialog, action, fieldName, fieldType);
	}

	@Override
	public void buildEditor(final JPanel parentPanel) {
		final var keyStroke = (KeyStroke) initialValue;

		final var availableScanCodes = DirectInputKeyCode.nameToKeyCodeMap.keySet();

		final var modifiersPanel = new JPanel();
		modifiersPanel.setLayout(new BoxLayout(modifiersPanel, BoxLayout.PAGE_AXIS));
		final var modifiersLabel = new JLabel(strings.getString("MODIFIERS_LABEL"));
		modifiersLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
		modifiersPanel.add(modifiersLabel);
		modifiersPanel.add(Box.createVerticalStrut(5));
		final var modifierList = new JList<>(availableScanCodes.toArray(new String[availableScanCodes.size()]));
		modifierList.addListSelectionListener(new JListSetPropertyListSelectionListener(setterMethod, keyStroke, true));

		final var addedModifiers = new ArrayList<String>();
		for (final int c1 : keyStroke.getModifierCodes())
			addedModifiers.add(DirectInputKeyCode.keyCodeToNameMap.get(c1));
		for (final var s1 : addedModifiers) {
			final var index1 = getListModelIndex(modifierList.getModel(), s1);
			if (index1 >= 0)
				modifierList.addSelectionInterval(index1, index1);
		}

		final var modifiersScrollPane = new JScrollPane(modifierList);
		modifiersScrollPane.setPreferredSize(new Dimension(130, 200));
		modifiersPanel.add(modifiersScrollPane);
		parentPanel.add(modifiersPanel);

		final var keysPanel = new JPanel();
		keysPanel.setLayout(new BoxLayout(keysPanel, BoxLayout.PAGE_AXIS));
		final var keysLabel = new JLabel(strings.getString("KEYS_LABEL"));
		keysLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
		keysPanel.add(keysLabel);
		keysPanel.add(Box.createVerticalStrut(5));
		final var keyList = new JList<>(availableScanCodes.toArray(new String[availableScanCodes.size()]));
		keyList.addListSelectionListener(new JListSetPropertyListSelectionListener(setterMethod, keyStroke, false));

		final var addedKeys = new ArrayList<String>();
		for (final int c2 : keyStroke.getKeyCodes())
			addedKeys.add(DirectInputKeyCode.keyCodeToNameMap.get(c2));
		for (final var s2 : addedKeys) {
			final var index2 = getListModelIndex(keyList.getModel(), s2);
			if (index2 >= 0)
				keyList.addSelectionInterval(index2, index2);
		}

		final var keysScrollPane = new JScrollPane(keyList);
		keysScrollPane.setPreferredSize(new Dimension(130, 200));
		keysPanel.add(keysScrollPane);
		parentPanel.add(keysPanel);
	}
}

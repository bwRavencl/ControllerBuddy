/* Copyright (C) 2026  Matteo Hausner
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

import java.lang.reflect.Method;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class KeystrokeEditorBuilderTest {

	private static Method getListModelIndexMethod;

	private static int getListModelIndex(final ListModel<?> model, final Object value) throws Exception {
		return (int) getListModelIndexMethod.invoke(null, model, value);
	}

	@BeforeAll
	static void setUp() throws Exception {
		getListModelIndexMethod = KeystrokeEditorBuilder.class.getDeclaredMethod("getListModelIndex", ListModel.class,
				Object.class);
		getListModelIndexMethod.setAccessible(true);
	}

	@Nested
	@DisplayName("getListModelIndex()")
	class GetListModelIndexTests {

		@Test
		@DisplayName("finds element in DefaultListModel by value")
		void findsInDefaultListModel() throws Exception {
			final var model = new DefaultListModel<String>();
			model.addElement("Alpha");
			model.addElement("Beta");
			model.addElement("Gamma");
			Assertions.assertEquals(1, getListModelIndex(model, "Beta"));
		}

		@Test
		@DisplayName("finds element in non-DefaultListModel via linear scan")
		void findsInGenericListModel() throws Exception {
			// Use an anonymous ListModel that is not a DefaultListModel
			final var items = new String[] { "X", "Y", "Z" };
			final ListModel<String> model = new ListModel<>() {

				@Override
				public void addListDataListener(final javax.swing.event.ListDataListener l) {
				}

				@Override
				public String getElementAt(final int index) {
					return items[index];
				}

				@Override
				public int getSize() {
					return items.length;
				}

				@Override
				public void removeListDataListener(final javax.swing.event.ListDataListener l) {
				}
			};
			Assertions.assertEquals(2, getListModelIndex(model, "Z"));
		}

		@Test
		@DisplayName("returns -1 for empty model")
		void returnsMinusOneForEmptyModel() throws Exception {
			final var model = new DefaultListModel<String>();
			Assertions.assertEquals(-1, getListModelIndex(model, "anything"));
		}

		@Test
		@DisplayName("returns -1 when element is not in the model")
		void returnsMinusOneForMissing() throws Exception {
			final var model = new DefaultListModel<String>();
			model.addElement("Alpha");
			Assertions.assertEquals(-1, getListModelIndex(model, "Omega"));
		}

		@Test
		@DisplayName("returns -1 for null value")
		void returnsMinusOneForNull() throws Exception {
			final var model = new DefaultListModel<String>();
			model.addElement("A");
			Assertions.assertEquals(-1, getListModelIndex(model, null));
		}
	}
}

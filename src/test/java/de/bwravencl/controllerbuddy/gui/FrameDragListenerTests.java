/*
 * Copyright (C) 2026 Matteo Hausner
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

import java.awt.Point;
import javax.swing.JFrame;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@NullMarked
@ExtendWith(MockitoExtension.class)
final class FrameDragListenerTests {

	@Mock
	JFrame mockFrame;

	@Mock
	Main mockMain;

	private FrameDragListener listener;

	@Test
	@DisplayName("isDragging() returns true after mousePressed()")
	void isDraggingAfterMousePressed() {
		final var mockEvent = Mockito.mock(java.awt.event.MouseEvent.class);
		Mockito.when(mockEvent.getPoint()).thenReturn(new Point(10, 20));
		listener.mousePressed(mockEvent);
		Assertions.assertTrue(listener.isDragging());
	}

	@Test
	@DisplayName("isDragging() returns false before any mouse event")
	void isNotDraggingInitially() {
		Assertions.assertFalse(listener.isDragging());
	}

	@BeforeEach
	void setUp() {
		listener = new FrameDragListener(mockMain, mockFrame);
	}
}

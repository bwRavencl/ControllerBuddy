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

import javax.swing.event.ChangeListener;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@NullMarked
@ExtendWith(MockitoExtension.class)
final class ClampingSpinnerNumberModelTests {

	@Mock
	ChangeListener mockChangeListener;

	@Test
	@DisplayName("clamps value above maximum to maximum")
	void clampsValueAboveMaximum() {
		final var model = new ClampingSpinnerNumberModel(150, 1, 100, 1, null);
		Assertions.assertEquals(100, model.getValue());
	}

	@Test
	@DisplayName("clamps value below minimum to minimum")
	void clampsValueBelowMinimum() {
		final var model = new ClampingSpinnerNumberModel(-5, 1, 100, 1, null);
		Assertions.assertEquals(1, model.getValue());
	}

	@Test
	@DisplayName("constructor accepts null changeListener without throwing")
	void constructorAcceptsNullChangeListener() {
		Assertions.assertDoesNotThrow(() -> new ClampingSpinnerNumberModel(50, 1, 100, 1, null));
	}

	@Test
	@DisplayName("constructor does not notify changeListener when value is within range")
	void constructorDoesNotNotifyListenerWhenValueInRange() {
		new ClampingSpinnerNumberModel(50, 1, 100, 1, mockChangeListener);
		Mockito.verifyNoInteractions(mockChangeListener);
	}

	@Test
	@DisplayName("constructor notifies changeListener immediately when value is clamped")
	void constructorNotifiesListenerWhenValueClamped() {
		new ClampingSpinnerNumberModel(150, 1, 100, 1, mockChangeListener);
		Mockito.verify(mockChangeListener).stateChanged(Mockito.any());
	}

	@Test
	@DisplayName("constructor registers changeListener for subsequent setValue() calls")
	void constructorRegistersListenerForSubsequentChanges() {
		final var model = new ClampingSpinnerNumberModel(50, 1, 100, 1, mockChangeListener);
		model.setValue(60);
		Mockito.verify(mockChangeListener).stateChanged(Mockito.any());
	}

	@Test
	@DisplayName("does not clamp when maximum is null")
	void doesNotClampWhenMaximumIsNull() {
		final var model = new ClampingSpinnerNumberModel(1000, 1, null, 1, null);
		Assertions.assertEquals(1000, model.getValue());
	}

	@Test
	@DisplayName("does not clamp when minimum is null")
	void doesNotClampWhenMinimumIsNull() {
		final var model = new ClampingSpinnerNumberModel(-1000, null, 100, 1, null);
		Assertions.assertEquals(-1000, model.getValue());
	}

	@Test
	@DisplayName("does not clamp when value equals maximum")
	void doesNotClampWhenValueEqualsMaximum() {
		final var model = new ClampingSpinnerNumberModel(100, 1, 100, 1, null);
		Assertions.assertEquals(100, model.getValue());
	}

	@Test
	@DisplayName("does not clamp when value equals minimum")
	void doesNotClampWhenValueEqualsMinimum() {
		final var model = new ClampingSpinnerNumberModel(1, 1, 100, 1, null);
		Assertions.assertEquals(1, model.getValue());
	}

	@Test
	@DisplayName("keeps value unchanged when within range")
	void keepsValueUnchangedWhenWithinRange() {
		final var model = new ClampingSpinnerNumberModel(50, 1, 100, 1, null);
		Assertions.assertEquals(50, model.getValue());
	}

	@Test
	@DisplayName("setValue() clamps value above maximum")
	void setValueClampsValueAboveMaximum() {
		final var model = new ClampingSpinnerNumberModel(50, 1, 100, 1, null);
		model.setValue(500);
		Assertions.assertEquals(100, model.getValue());
	}

	@Test
	@DisplayName("setValue() clamps value below minimum")
	void setValueClampsValueBelowMinimum() {
		final var model = new ClampingSpinnerNumberModel(50, 1, 100, 1, null);
		model.setValue(-500);
		Assertions.assertEquals(1, model.getValue());
	}

	@Test
	@DisplayName("setValue() delegates non-Number values to superclass, which rejects them")
	void setValueDelegatesNonNumberValuesToSuperclass() {
		final var model = new ClampingSpinnerNumberModel(50, 1, 100, 1, null);
		Assertions.assertThrows(IllegalArgumentException.class, () -> model.setValue("not a number"));
	}

	@Test
	@DisplayName("setValue() accepts value within range unchanged")
	void setValueKeepsValueWithinRangeUnchanged() {
		final var model = new ClampingSpinnerNumberModel(50, 1, 100, 1, null);
		model.setValue(75);
		Assertions.assertEquals(75, model.getValue());
	}
}

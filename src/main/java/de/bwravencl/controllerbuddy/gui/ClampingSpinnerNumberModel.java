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

import java.io.Serial;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// A [SpinnerNumberModel] that clamps out-of-range values to the nearest bound
/// instead of throwing an [IllegalArgumentException].
///
/// This is useful when a value bound to the model - e.g. one loaded from a
/// file - may fall outside the configured minimum/maximum range. Rather than
/// failing construction, the value is silently clamped, and if clamping
/// occurred, the supplied [ChangeListener] is notified immediately so callers
/// can keep any external state in sync with the corrected value.
@NullMarked
public final class ClampingSpinnerNumberModel extends SpinnerNumberModel {

	@Serial
	private static final long serialVersionUID = 5592669679820882529L;

	/// Creates a clamping model for arbitrary [Number]/[Comparable] types.
	///
	/// The `value` is clamped against `minimum` and `maximum` before the superclass
	/// constructor runs, so the model is never in an invalid state.
	/// If `changeListener` is non-`null`, it is registered.
	/// Additionally, if clamping actually changed the value, the listener is
	/// notified immediately via [#fireStateChanged], so it observes the corrected
	/// value right away instead of only on the next user-driven change.
	///
	/// @param value the initial value
	/// @param minimum the minimum allowed value (inclusive) or `null`
	/// @param maximum the maximum allowed value (inclusive) or `null`
	/// @param stepSize the step size
	/// @param changeListener a listener to register immediately, notified
	/// synchronously if `value` had to be clamped; may be `null`
	public ClampingSpinnerNumberModel(final Number value, final @Nullable Comparable<?> minimum,
			final @Nullable Comparable<?> maximum, final Number stepSize,
			final @Nullable ChangeListener changeListener) {
		super(clamp(value, minimum, maximum), minimum, maximum, stepSize);

		if (changeListener != null) {
			addChangeListener(changeListener);
		}

		if (!getValue().equals(value)) {
			fireStateChanged();
		}
	}

	/// Clamps `value` to lie within `[minimum, maximum]`.
	///
	/// @param value the value to clamp
	/// @param minimum the minimum allowed value (inclusive) or `null`
	/// @param maximum the maximum allowed value (inclusive) or `null`
	/// @return `minimum` if `value` is less than `minimum`, `maximum` if `value` is
	/// greater than `maximum`, otherwise `value` unchanged
	@SuppressWarnings("unchecked")
	private static Number clamp(final Number value, final @Nullable Comparable<?> minimum,
			final @Nullable Comparable<?> maximum) {
		final var minimumObject = (Comparable<Object>) minimum;
		final var maximumObject = (Comparable<Object>) maximum;

		if (minimumObject != null && minimumObject.compareTo(value) > 0) {
			return (Number) minimumObject;
		}
		if (maximumObject != null && maximumObject.compareTo(value) < 0) {
			return (Number) maximumObject;
		}
		return value;
	}

	/// Sets the current value, clamping it to `[minimum, maximum]` first if it is a
	/// [Number]. Non-[Number] values are delegated to the superclass unchanged.
	///
	/// @param value the new value
	@Override
	public void setValue(final Object value) {
		if (value instanceof final Number number) {
			super.setValue(clamp(number, getMinimum(), getMaximum()));
		} else {
			super.setValue(value);
		}
	}
}

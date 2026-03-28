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

package de.bwravencl.controllerbuddy.input;

import de.bwravencl.controllerbuddy.gui.Main;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/// Represents a keyboard keystroke consisting of modifier keys and regular
/// keys.
///
/// Supports serialization using legacy key code sets for backward
/// compatibility, converting between [Scancode]-based and integer-based
/// representations as needed.
public final class Keystroke implements Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = 3572153768203547877L;

	/// The primary scancodes making up the keystroke.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private Scancode[] keyCodes;

	/// The modifier scancodes (e.g., Shift, Ctrl) accompanying the keystroke.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private Scancode[] modifierCodes;

	/// Constructs an empty keystroke with no key codes or modifier codes.
	public Keystroke() {
		this(new Scancode[0], new Scancode[0]);
	}

	/// Constructs a keystroke with the specified key codes and modifier codes.
	///
	/// @param keyCodes the primary scancodes for this keystroke
	/// @param modifierCodes the modifier scancodes (e.g., Shift, Ctrl) for this
	/// keystroke
	public Keystroke(final Scancode[] keyCodes, final Scancode[] modifierCodes) {
		this.keyCodes = keyCodes;
		this.modifierCodes = modifierCodes;
	}

	/// Creates a deep copy of this keystroke, including copies of the key code and
	/// modifier code arrays.
	///
	/// @return a cloned Keystroke instance
	@Override
	public Object clone() throws CloneNotSupportedException {
		final var keystroke = (Keystroke) super.clone();

		final var clonedKeyCodes = new Scancode[keyCodes.length];
		System.arraycopy(keyCodes, 0, clonedKeyCodes, 0, keyCodes.length);
		keystroke.keyCodes = clonedKeyCodes;

		final var clonedModifierCodes = new Scancode[modifierCodes.length];
		System.arraycopy(modifierCodes, 0, clonedModifierCodes, 0, modifierCodes.length);
		keystroke.modifierCodes = clonedModifierCodes;

		return keystroke;
	}

	/// Checks equality based on key codes and modifier codes arrays.
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof final Keystroke keystroke && Arrays.equals(keyCodes, keystroke.keyCodes)
				&& Arrays.equals(modifierCodes, keystroke.modifierCodes);
	}

	/// Returns the primary scancodes for this keystroke.
	///
	/// @return the key codes array
	public Scancode[] getKeyCodes() {
		return keyCodes;
	}

	/// Returns the modifier scancodes for this keystroke.
	///
	/// @return the modifier codes array
	public Scancode[] getModifierCodes() {
		return modifierCodes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(Arrays.hashCode(keyCodes), Arrays.hashCode(modifierCodes));
	}

	/// Deserializes this keystroke from the given stream, reading the modifier
	/// and key code sets and mapping each stored integer key code to its
	/// corresponding [Scancode].
	///
	/// @param stream the object input stream to read from
	/// @throws ClassNotFoundException if the class of a serialized object cannot be
	/// found
	/// @throws IOException if an I/O error occurs during deserialization
	@Serial
	private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
		@SuppressWarnings("unchecked")
		final var modifierCodesKeyCodes = (Set<Integer>) stream.readObject();
		modifierCodes = modifierCodesKeyCodes.stream().map(Scancode.KEY_CODE_TO_SCAN_CODE_MAP::get)
				.toArray(Scancode[]::new);

		@SuppressWarnings("unchecked")
		final var keyCodesKeyCodes = (Set<Integer>) stream.readObject();
		keyCodes = keyCodesKeyCodes.stream().map(Scancode.KEY_CODE_TO_SCAN_CODE_MAP::get).toArray(Scancode[]::new);
	}

	/// Sets the primary scancodes for this keystroke.
	///
	/// @param keyCodes the key codes to set
	public void setKeyCodes(final Scancode[] keyCodes) {
		this.keyCodes = keyCodes;
	}

	/// Sets the modifier scancodes for this keystroke.
	///
	/// @param modifierCodes the modifier codes to set
	public void setModifierCodes(final Scancode[] modifierCodes) {
		this.modifierCodes = modifierCodes;
	}

	/// Returns a human-readable representation of this keystroke, joining modifier
	/// and key names with " + ".
	///
	/// @return the formatted keystroke string, or the localized "NOTHING" string
	/// if empty
	@Override
	public String toString() {
		final var collectedKeyCodes = new ArrayList<>(Arrays.asList(modifierCodes));
		collectedKeyCodes.addAll(Arrays.asList(keyCodes));
		if (collectedKeyCodes.isEmpty()) {
			return Main.STRINGS.getString("NOTHING");
		}

		return collectedKeyCodes.stream().map(Scancode::name).collect(Collectors.joining(" + "));
	}

	/// Serializes this keystroke to the given stream, writing the modifier codes
	/// and key codes as sets of integer key code values.
	///
	/// @param stream the object output stream to write to
	/// @throws IOException if an I/O error occurs during serialization
	@Serial
	private void writeObject(final ObjectOutputStream stream) throws IOException {
		stream.writeObject(Arrays.stream(modifierCodes).map(Scancode::keyCode).collect(Collectors.toSet()));
		stream.writeObject(Arrays.stream(keyCodes).map(Scancode::keyCode).collect(Collectors.toSet()));
	}
}

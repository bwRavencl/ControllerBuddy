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
/// compatibility, converting between [ScanCode]-based and integer-based
/// representations as needed.
public final class KeyStroke implements Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = 3572153768203547877L;

	/// The primary scan codes making up the keystroke.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private ScanCode[] keyCodes;

	/// The modifier scan codes (e.g., Shift, Ctrl) accompanying the keystroke.
	@SuppressWarnings({ "serial", "RedundantSuppression" })
	private ScanCode[] modifierCodes;

	/// Constructs an empty keystroke with no key codes or modifier codes.
	public KeyStroke() {
		this(new ScanCode[0], new ScanCode[0]);
	}

	/// Constructs a keystroke with the specified key codes and modifier codes.
	///
	/// @param keyCodes the primary scan codes for this keystroke
	/// @param modifierCodes the modifier scan codes (e.g., Shift, Ctrl) for this
	/// keystroke
	public KeyStroke(final ScanCode[] keyCodes, final ScanCode[] modifierCodes) {
		this.keyCodes = keyCodes;
		this.modifierCodes = modifierCodes;
	}

	/// Creates a deep copy of this keystroke, including copies of the key code and
	/// modifier code arrays.
	///
	/// @return a cloned KeyStroke instance
	@Override
	public Object clone() throws CloneNotSupportedException {
		final var keyStroke = (KeyStroke) super.clone();

		final var clonedKeyCodes = new ScanCode[keyCodes.length];
		System.arraycopy(keyCodes, 0, clonedKeyCodes, 0, keyCodes.length);
		keyStroke.keyCodes = clonedKeyCodes;

		final var clonedModifierCodes = new ScanCode[modifierCodes.length];
		System.arraycopy(modifierCodes, 0, clonedModifierCodes, 0, modifierCodes.length);
		keyStroke.modifierCodes = clonedModifierCodes;

		return keyStroke;
	}

	/// Checks equality based on key codes and modifier codes arrays.
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof final KeyStroke keyStroke && Arrays.equals(keyCodes, keyStroke.keyCodes)
				&& Arrays.equals(modifierCodes, keyStroke.modifierCodes);
	}

	/// Returns the primary scan codes for this keystroke.
	///
	/// @return the key codes array
	public ScanCode[] getKeyCodes() {
		return keyCodes;
	}

	/// Returns the modifier scan codes for this keystroke.
	///
	/// @return the modifier codes array
	public ScanCode[] getModifierCodes() {
		return modifierCodes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(Arrays.hashCode(keyCodes), Arrays.hashCode(modifierCodes));
	}

	/// Deserializes this keystroke from the given stream, reading the modifier
	/// and key code sets and mapping each stored integer key code to its
	/// corresponding [ScanCode].
	///
	/// @param stream the object input stream to read from
	/// @throws ClassNotFoundException if the class of a serialized object cannot be
	/// found
	/// @throws IOException if an I/O error occurs during deserialization
	@Serial
	private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
		@SuppressWarnings("unchecked")
		final var modifierCodesKeyCodes = (Set<Integer>) stream.readObject();
		modifierCodes = modifierCodesKeyCodes.stream().map(ScanCode.KEY_CODE_TO_SCAN_CODE_MAP::get)
				.toArray(ScanCode[]::new);

		@SuppressWarnings("unchecked")
		final var keyCodesKeyCodes = (Set<Integer>) stream.readObject();
		keyCodes = keyCodesKeyCodes.stream().map(ScanCode.KEY_CODE_TO_SCAN_CODE_MAP::get).toArray(ScanCode[]::new);
	}

	/// Sets the primary scan codes for this keystroke.
	///
	/// @param keyCodes the key codes to set
	public void setKeyCodes(final ScanCode[] keyCodes) {
		this.keyCodes = keyCodes;
	}

	/// Sets the modifier scan codes for this keystroke.
	///
	/// @param modifierCodes the modifier codes to set
	public void setModifierCodes(final ScanCode[] modifierCodes) {
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

		return collectedKeyCodes.stream().map(ScanCode::name).collect(Collectors.joining(" + "));
	}

	/// Serializes this keystroke to the given stream, writing the modifier codes
	/// and key codes as sets of integer key code values.
	///
	/// @param stream the object output stream to write to
	/// @throws IOException if an I/O error occurs during serialization
	@Serial
	private void writeObject(final ObjectOutputStream stream) throws IOException {
		stream.writeObject(Arrays.stream(modifierCodes).map(ScanCode::keyCode).collect(Collectors.toSet()));
		stream.writeObject(Arrays.stream(keyCodes).map(ScanCode::keyCode).collect(Collectors.toSet()));
	}
}

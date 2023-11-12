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

public final class KeyStroke implements Cloneable, Serializable {

    @Serial
    private static final long serialVersionUID = 3572153768203547877L;

    @SuppressWarnings({"serial", "RedundantSuppression"})
    private ScanCode[] keyCodes;

    @SuppressWarnings({"serial", "RedundantSuppression"})
    private ScanCode[] modifierCodes;

    public KeyStroke() {
        this(new ScanCode[0], new ScanCode[0]);
    }

    public KeyStroke(final ScanCode[] keyCodes, final ScanCode[] modifierCodes) {
        this.keyCodes = keyCodes;
        this.modifierCodes = modifierCodes;
    }

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

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof final KeyStroke keyStroke
                && Arrays.equals(keyCodes, keyStroke.keyCodes)
                && Arrays.equals(modifierCodes, keyStroke.modifierCodes);
    }

    public ScanCode[] getKeyCodes() {
        return keyCodes;
    }

    public ScanCode[] getModifierCodes() {
        return modifierCodes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(keyCodes), Arrays.hashCode(modifierCodes));
    }

    @Serial
    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
        @SuppressWarnings("unchecked")
        final var modifierCodesKeyCodes = (Set<Integer>) stream.readObject();
        modifierCodes = modifierCodesKeyCodes.stream()
                .map(ScanCode.keyCodeToScanCodeMap::get)
                .toArray(ScanCode[]::new);

        @SuppressWarnings("unchecked")
        final var keyCodesKeyCodes = (Set<Integer>) stream.readObject();
        keyCodes = keyCodesKeyCodes.stream()
                .map(ScanCode.keyCodeToScanCodeMap::get)
                .toArray(ScanCode[]::new);
    }

    public void setKeyCodes(final ScanCode[] keyCodes) {
        this.keyCodes = keyCodes;
    }

    public void setModifierCodes(final ScanCode[] modifierCodes) {
        this.modifierCodes = modifierCodes;
    }

    @Override
    public String toString() {
        final var collectedKeyCodes = new ArrayList<>(Arrays.asList(modifierCodes));
        collectedKeyCodes.addAll(Arrays.asList(keyCodes));
        if (collectedKeyCodes.isEmpty()) {
            return Main.strings.getString("NOTHING");
        }

        return collectedKeyCodes.stream().map(ScanCode::name).collect(Collectors.joining(" + "));
    }

    @Serial
    private void writeObject(final ObjectOutputStream stream) throws IOException {
        stream.writeObject(Arrays.stream(modifierCodes).map(ScanCode::keyCode).collect(Collectors.toSet()));
        stream.writeObject(Arrays.stream(keyCodes).map(ScanCode::keyCode).collect(Collectors.toSet()));
    }
}

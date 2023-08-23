/* Copyright (C) 2019  Matteo Hausner
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

import java.awt.Color;

public final class OverlayAxis implements Cloneable {

    public Color color;

    public boolean inverted;

    public OverlayAxis() {
        this(Color.BLACK, false);
    }

    private OverlayAxis(final Color color, final boolean inverted) {
        this.color = color;
        this.inverted = inverted;
    }

    @Override
    public Object clone() {
        return new OverlayAxis(new Color(color.getRGB(), true), inverted);
    }
}

/* Copyright (C) 2014  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ClicksEditorBuilder;
import java.lang.constant.Constable;
import java.text.MessageFormat;
import java.util.Locale;

public abstract class ToScrollAction<V extends Constable> extends InvertableAction<V> {

	private static final int DEFAULT_CLICKS = 10;

	@ActionProperty(title = "CLICKS_TITLE", description = "CLICKS_DESCRIPTION", editorBuilder = ClicksEditorBuilder.class, order = 10)
	int clicks = DEFAULT_CLICKS;

	transient float remainingD = 0f;

	public int getClicks() {
		return clicks;
	}

	@Override
	public String getDescription(final Input input) {
		if (!isDescriptionEmpty()) {
			return super.getDescription(input);
		}

		return MessageFormat.format(Main.STRINGS.getString("SCROLL_DIRECTION"),
				Main.STRINGS.getString(invert ? "DIRECTION_DOWN" : "DIRECTION_UP").toLowerCase(Locale.ROOT));
	}

	void scroll(final Input input, float d) {
		d = invert ? -d : d;
		d += remainingD;

		final var roundedD = Math.round(d);
		if (roundedD == 0) {
			remainingD = d;
			return;
		}

		input.setScrollClicks(roundedD);
		remainingD = d - roundedD;
	}

	public void setClicks(final int clicks) {
		this.clicks = clicks;
	}
}

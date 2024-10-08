/* Copyright (C) 2022  Matteo Hausner
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

package de.bwravencl.controllerbuddy.input.driver;

import de.bwravencl.controllerbuddy.gui.Main.ControllerInfo;
import de.bwravencl.controllerbuddy.input.Input;
import java.util.List;
import java.util.Optional;

public interface IDriverBuilder extends Comparable<IDriverBuilder> {

	@Override
	default int compareTo(final IDriverBuilder o) {
		return Integer.compare(getOrder(), o.getOrder());
	}

	Optional<Driver> getIfAvailable(final Input input, final List<ControllerInfo> presentControllers,
			final ControllerInfo selectedController);

	default int getOrder() {
		return 0;
	}
}

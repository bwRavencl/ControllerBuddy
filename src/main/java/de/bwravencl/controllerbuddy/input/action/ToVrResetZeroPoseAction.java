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

package de.bwravencl.controllerbuddy.input.action;

import de.bwravencl.controllerbuddy.gui.Main;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.action.annotation.ActionProperty;
import de.bwravencl.controllerbuddy.input.action.gui.ActivationEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.LongPressEditorBuilder;
import de.bwravencl.controllerbuddy.input.action.gui.VrCoordinateSystemEditorBuilder;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRChaperone;

public abstract class ToVrResetZeroPoseAction<V extends Number> implements IActivatableAction<V>, ILongPressAction<V> {

	private transient Activatable activatable;

	@ActionProperty(label = "ACTIVATION", editorBuilder = ActivationEditorBuilder.class, order = 11)
	private Activation activation = Activation.REPEAT;

	@ActionProperty(label = "LONG_PRESS", editorBuilder = LongPressEditorBuilder.class, order = 400)
	private boolean longPress = DEFAULT_LONG_PRESS;

	@ActionProperty(label = "VR_COORDINATE_SYSTEM", editorBuilder = VrCoordinateSystemEditorBuilder.class, order = 10)
	private VrCoordinateSystem vrCoordinateSystem = VrCoordinateSystem.SEATED;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public Activatable getActivatable() {
		return activatable;
	}

	@Override
	public Activation getActivation() {
		return activation;
	}

	@Override
	public String getDescription(final Input input) {
		return Main.strings.getString("TO_VR_RESET_ZERO_POSE_ACTION");
	}

	public VrCoordinateSystem getVrCoordinateSystem() {
		return vrCoordinateSystem;
	}

	void handleAction(final boolean hot, final Input input) {
		if (!input.getMain().isOpenVrOverlayActive()) {
			return;
		}

		if (activatable == Activatable.ALWAYS) {
			VRChaperone.VRChaperone_ResetZeroPose(vrCoordinateSystem.value);
			return;
		}

		switch (activation) {
		case REPEAT -> {
			if (hot) {
				VRChaperone.VRChaperone_ResetZeroPose(vrCoordinateSystem.value);
			}
		}
		case SINGLE_IMMEDIATELY -> {
			if (!hot) {
				activatable = Activatable.YES;
			} else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				VRChaperone.VRChaperone_ResetZeroPose(vrCoordinateSystem.value);
			}
		}
		case SINGLE_ON_RELEASE -> {
			if (hot) {
				if (activatable == Activatable.NO) {
					activatable = Activatable.YES;
				} else if (activatable == Activatable.DENIED_BY_OTHER_ACTION) {
					activatable = Activatable.NO;
				}
			} else if (activatable == Activatable.YES) {
				activatable = Activatable.NO;
				VRChaperone.VRChaperone_ResetZeroPose(vrCoordinateSystem.value);
			}
		}
		}
	}

	@Override
	public boolean isLongPress() {
		return longPress;
	}

	@Override
	public void setActivatable(final Activatable activatable) {
		this.activatable = activatable;
	}

	@Override
	public void setActivation(final Activation activation) {
		this.activation = activation;
	}

	@Override
	public void setLongPress(final boolean longPress) {
		this.longPress = longPress;
	}

	public void setVrCoordinateSystem(final VrCoordinateSystem vrCoordinateSystem) {
		this.vrCoordinateSystem = vrCoordinateSystem;
	}

	public enum VrCoordinateSystem {

		SEATED(VR.ETrackingUniverseOrigin_TrackingUniverseSeated, "VR_COORDINATE_SYSTEM_SEATED"),
		STANDING(VR.ETrackingUniverseOrigin_TrackingUniverseStanding, "VR_COORDINATE_SYSTEM_STANDING"),
		RAW_AND_UNCALIBRATED(VR.ETrackingUniverseOrigin_TrackingUniverseRawAndUncalibrated,
				"VR_COORDINATE_SYSTEM_RAW_AND_UNCALIBRATED");

		private final String label;

		private final int value;

		VrCoordinateSystem(final int value, final String labelKey) {
			this.value = value;
			label = Main.strings.getString(labelKey);
		}

		@Override
		public String toString() {
			return label;
		}
	}
}

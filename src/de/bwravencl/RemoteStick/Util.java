package de.bwravencl.RemoteStick;

public class Util {

	public static float normalize(float value, float inMin, float inMax,
			float outMin, float outMax) {
		final float newValue;
		final float oldRange = (inMax - inMin);

		if (oldRange == 0)
			newValue = outMin;
		else {
			float newRange = (outMax - outMin);
			newValue = (((value - inMin) * newRange) / oldRange) + outMin;
		}

		return newValue;
	}

}

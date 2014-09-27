package de.bwravencl.RemoteStick;

public class Util {
	// normalizes a value of a range [inMin, inMax] to a value of the range
	// [outMin, outMax]
	public static float normalize(float value, float inMin, float inMax,
			float outMin, float outMax) {
		/*
		 * return (outMax - outMin) / (inMax - inMin) * (value - inMax) +
		 * (outMax - outMin);
		 */

		float newValue;
		float oldRange = (inMax - inMin);
		if (oldRange == 0)
			newValue = outMin;
		else {
			float newRange = (outMax - outMin);
			newValue = (((value - inMin) * newRange) / oldRange) + outMin;
		}
		
		return newValue;
	}
}

/* Copyright (C) 2015  Matteo Hausner
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.bwravencl.RemoteStick.vjoy;

import com.sun.jna.Native;

public class VJoy {

	public static String getDefaultLibraryPath() {
		final String arch = System.getProperty("sun.arch.data.model");

		final String archFolder;
		if ("64".equals(arch))
			archFolder = "x64";
		else
			archFolder = "x86";

		return System.getenv("ProgramFiles") + "\\vJoy\\" + archFolder;
	}

	public static IVjoyInterface loadLibrary() throws Exception {
		System.setProperty("jna.library.path", VJoy.getDefaultLibraryPath());

		try {
			return (IVjoyInterface) Native.loadLibrary("vJoyInterface", IVjoyInterface.class);
		} catch (UnsatisfiedLinkError e) {
			throw new Exception(e);
		}
	}

}

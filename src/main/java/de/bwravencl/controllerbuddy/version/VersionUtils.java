package de.bwravencl.controllerbuddy.version;

import java.util.Optional;

public class VersionUtils {

	public static Optional<Integer> compareVersions(final String version) throws IllegalArgumentException {
		if (version == null)
			return Optional.empty();

		final var versionParts = getVersionParts(version);
		if (versionParts.length < 2)
			return Optional.empty();

		final var currentVersionParts = getVersionParts(Version.VERSION);
		for (int i = 0; i < 2; i++)
			try {
				if (Integer.parseInt(versionParts[i]) < Integer.parseInt(currentVersionParts[i]))
					return Optional.of(-1);
				if (Integer.parseInt(versionParts[i]) > Integer.parseInt(currentVersionParts[i]))
					return Optional.of(1);
			} catch (final NumberFormatException e) {
				return Optional.empty();
			}

		return Optional.of(0);
	}

	public static String getMajorAndMinorVersion() {
		return Version.VERSION.substring(0, Version.VERSION.lastIndexOf('.'));
	}

	private static String[] getVersionParts(final String version) {
		return version.split("\\.");
	}

}

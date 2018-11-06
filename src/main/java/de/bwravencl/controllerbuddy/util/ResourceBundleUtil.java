/* Copyright (C) 2018  Matteo Hausner
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

package de.bwravencl.controllerbuddy.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceBundleUtil {

	private static final class BundleLocale {
		private final String baseName;
		private final Locale locale;

		private BundleLocale(final String baseName, final Locale locale) {
			this.baseName = baseName;
			this.locale = locale;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final BundleLocale other = (BundleLocale) obj;
			if (baseName == null) {
				if (other.baseName != null)
					return false;
			} else if (!baseName.equals(other.baseName))
				return false;
			if (locale == null) {
				if (other.locale != null)
					return false;
			} else if (!locale.equals(other.locale))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (baseName == null ? 0 : baseName.hashCode());
			result = prime * result + (locale == null ? 0 : locale.hashCode());
			return result;
		}

	}

	private static final class ReflectiveResourceBundle extends ResourceBundle {
		private final Map<String, String> messages;
		private final Enumeration<String> keys;

		private ReflectiveResourceBundle(final Map<String, String> messages) {
			this.messages = messages;
			keys = Collections.enumeration(messages.keySet());
		}

		@Override
		public Enumeration<String> getKeys() {
			return keys;
		}

		@Override
		protected Object handleGetObject(final String key) {
			return messages.get(key);
		}
	}

	private static final int MAX_RECURSION = 5;

	private static final Pattern keyPattern = Pattern.compile("\\$\\{([\\w\\.\\-]+)\\}");

	private final ConcurrentMap<BundleLocale, ResourceBundle> cache = new ConcurrentHashMap<>();

	public ResourceBundle getResourceBundle(final String baseName, final Locale locale) {
		final BundleLocale bundleLocale = new BundleLocale(baseName, locale);

		ReflectiveResourceBundle bundle = (ReflectiveResourceBundle) cache.get(bundleLocale);
		if (bundle == null) {
			final ResourceBundle fetchedBundle = ResourceBundle.getBundle(baseName, locale);
			final Map<String, String> messages = new HashMap<>();
			final Enumeration<String> keys = fetchedBundle.getKeys();

			while (keys.hasMoreElements()) {
				final String key = keys.nextElement();
				messages.put(key, translateMessage(fetchedBundle, key, MAX_RECURSION));
			}

			bundle = new ReflectiveResourceBundle(messages);
			cache.put(bundleLocale, bundle);
		}

		return bundle;
	}

	private String translateMessage(final ResourceBundle bundle, final String key, final int iteration) {
		final String message = bundle.getString(key);

		final Matcher matcher = keyPattern.matcher(message);
		final StringBuffer sb = new StringBuffer();
		while (matcher.find() && iteration > 0)
			matcher.appendReplacement(sb, translateMessage(bundle, matcher.group(1), iteration - 1));

		matcher.appendTail(sb);

		return sb.toString();
	}

}

/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.components;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Hold version information, and parsing methods
 * 
 * @author Ben Skinner
 * @since 1.12.0
 *
 */
public class Version implements Serializable {

	private static final Logger LOGGER = Logger.getLogger(Version.class.getName());

	private static final long serialVersionUID = 1L;

	/**
	 * The fields for setting the version. Backwards compatability should be
	 * maintained between revision increments and minor verions, but is not
	 * guaranteed between major version increments.
	 */
	public static final int VERSION_MAJOR = 2;
	public static final int VERSION_MINOR = 3;
	public static final int VERSION_REVISION = 0;

	private final int major;
	private final int minor;
	private final int revision;

	private static final String SEPARATOR = ".";

	// Track the minor versions
	public static final Version V_2_0_0 = new Version(2, 0, 0);
	public static final Version V_2_1_0 = new Version(2, 1, 0);
	public static final Version V_2_2_0 = new Version(2, 2, 0);

	public Version(final int major, final int minor, final int revision) {
		this.major = major;
		this.minor = minor;
		this.revision = revision;
	}

	/**
	 * Get the current software version
	 * 
	 * @return
	 */
	public static @NonNull Version currentVersion() {
		return new Version(VERSION_MAJOR, VERSION_MINOR, VERSION_REVISION);
	}

	/**
	 * Parse the given string to a version. The string should have three integers
	 * separated by dots - e.g. 1.11.5. Convenience method.
	 * 
	 * @param s the string to parse
	 * @return
	 */
	public static Version fromString(@NonNull final String s) {
		return parseString(s);
	}

	/**
	 * Parse the given string to a version. The string should have three integers
	 * separated by dots - e.g. 1.11.5
	 * 
	 * @param s
	 * @return
	 */
	public static Version parseString(@NonNull final String s) {
		String[] parts = s.split("\\" + SEPARATOR);
		if (parts.length == 3) {
			return new Version(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]),
					Integer.valueOf(parts[2]));
		}
		throw new IllegalArgumentException("Input string is not a version format");
	}

	/**
	 * Test if the given version is older than this version
	 * 
	 * @param v
	 * @return
	 */
	public boolean isOlderThan(@NonNull final Version v) {
		if (this.major < v.getMajor()) {
			return true;
		}

		if (this.major == v.getMajor() && this.minor < v.getMinor()) {
			return true;
		}

		if (this.major == v.getMajor() && this.minor == v.getMinor()
				&& this.revision < v.getRevision()) {
			return true;
		}
		return false;
	}

	public boolean isNewerThan(@NonNull final Version v) {
		return v.isOlderThan(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + major;
		result = prime * result + minor;
		result = prime * result + revision;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Version other = (Version) obj;
		if (major != other.major)
			return false;
		if (minor != other.minor)
			return false;
		if (revision != other.revision)
			return false;
		return true;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getRevision() {
		return revision;
	}

	@Override
	public String toString() {
		return major + SEPARATOR + minor + SEPARATOR + revision;
	}

	/**
	 * Check a version string to see if the program will be able to open a dataset.
	 * The major version must be the same. Minor and bugfixing revision versions are
	 * not checked.
	 * 
	 * @param version
	 * @return true if the version is supported, false otherwise
	 */
	public static boolean versionIsSupported(@NonNull Version version) {

		// major version MUST be the same
		if (version.getMajor() != VERSION_MAJOR)
			return false;

		return true;
	}

	private void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {

		in.defaultReadObject();

		if (!versionIsSupported(this)) {
			throw new UnsupportedVersionException(this);
		}
	}

	/**
	 * Throw if the version being deserialised is not supported
	 * 
	 * @author Ben Skinner
	 *
	 */
	public static class UnsupportedVersionException extends IOException {

		private static final long serialVersionUID = 1L;

		private Version detectedVersion = null;

		public UnsupportedVersionException(@Nullable Version v) {
			super("Incompatible version detected");
			detectedVersion = v;
		}

		public Version getDetectedVersion() {
			return detectedVersion;
		}
	}

}

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
 * @author bms41
 * @since 1.12.0
 *
 */
public class Version implements Serializable {

	private static final Logger LOGGER = Logger.getLogger(Version.class.getName());

	private static final long serialVersionUID = 1L;

	/**
	 * The fields for setting the version. Version will be stored in
	 * AnalysisDatasets. Backwards compatability should be maintained between bugfix
	 * increments, but is not guaranteed between revision or major version
	 * increments.
	 */
	public static final int VERSION_MAJOR = 2;
	public static final int VERSION_MINOR = 0;
	public static final int VERSION_REVISION = 0;

	private final int major;
	private final int minor;
	private final int revision;

	private static final String SEPARATOR = ".";

	// Some versions to compare features against
	public static final Version v_1_13_0 = new Version(1, 13, 0);
	public static final Version v_1_13_1 = new Version(1, 13, 1);
	public static final Version v_1_13_2 = new Version(1, 13, 2);
	public static final Version v_1_13_3 = new Version(1, 13, 3);
	public static final Version v_1_13_4 = new Version(1, 13, 4);
	public static final Version v_1_13_5 = new Version(1, 13, 5);
	public static final Version v_1_13_6 = new Version(1, 13, 6);
	public static final Version v_1_13_7 = new Version(1, 13, 7);
	public static final Version v_1_13_8 = new Version(1, 13, 8);
	public static final Version v_1_14_0 = new Version(1, 14, 0);
	public static final Version v_1_15_0 = new Version(1, 15, 0);
	public static final Version v_1_15_1 = new Version(1, 15, 1);
	public static final Version v_1_15_2 = new Version(1, 15, 2);
	public static final Version v_1_15_3 = new Version(1, 15, 3);
	public static final Version v_1_15_4 = new Version(1, 15, 4);
	public static final Version v_1_16_0 = new Version(1, 16, 0);
	public static final Version v_1_17_0 = new Version(1, 17, 0);
	public static final Version v_1_17_1 = new Version(1, 17, 1);
	public static final Version v_1_18_0 = new Version(1, 18, 0);
	public static final Version v_1_18_1 = new Version(1, 18, 1);
	public static final Version v_1_19_0 = new Version(1, 19, 0);

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
	public static Version currentVersion() {
		return new Version(VERSION_MAJOR, VERSION_MINOR, VERSION_REVISION);
//        try {
//        	LOGGER.config("Reading properties file: "+"project.properties");
//        	InputStream is = Version.class.getResourceAsStream("project.properties");
//            Properties p = new Properties();
//			p.load(is);
//	        String version = p.getProperty("version");
//	        return fromString(version);
//		} catch (IOException | NullPointerException e) {
//			LOGGER.config("Cannot read properties file: "+e.getMessage());
//			Version v = new Version(VERSION_MAJOR, VERSION_MINOR, VERSION_REVISION);
//			LOGGER.config("Using default version: "+v.toString());
//			return v;
//		}
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
			return new Version(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]), Integer.valueOf(parts[2]));
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

		if (this.major == v.getMajor() && this.minor == v.getMinor() && this.revision < v.getRevision()) {
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

		if (version == null)
			return false;

		// major version MUST be the same
		if (version.getMajor() != VERSION_MAJOR)
			return false;

		// Minor versions are compatible from version 1.13.6 onwards
		if (version.isOlderThan(v_1_13_0))
			return false;

		return true;
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

		in.defaultReadObject();

		if (!versionIsSupported(this)) {
			throw new UnsupportedVersionException(this);
		}
	}

	/**
	 * Throw if the version being deserialised is not supported
	 * 
	 * @author ben
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

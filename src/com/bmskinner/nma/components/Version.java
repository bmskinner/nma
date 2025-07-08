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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.core.NuclearMorphologyAnalysis;

/**
 * Hold version information, and parsing methods
 * 
 * @author Ben Skinner
 * @since 1.12.0
 *
 */
public class Version {

	private static final Logger LOGGER = Logger.getLogger(Version.class.getName());

	private static final String VERSION_STRING = readVersionTemplateFile();
	private static final Version CURRENT_VERSION = parseString(VERSION_STRING);

	private static final String EMPTY_STRING = "";

	private final int major;
	private final int minor;
	private final int revision;
	private final String suffix; // alpha suffix etc

	private static final String SEPARATOR = ".";

	// Track the minor versions
	public static final Version V_2_0_0 = new Version(2, 0, 0);
	public static final Version V_2_1_0 = new Version(2, 1, 0);
	public static final Version V_2_2_0 = new Version(2, 2, 0);
	public static final Version V_2_3_0 = new Version(2, 3, 0);

	/**
	 * Create a version
	 * @param major
	 * @param minor
	 * @param revision
	 */
	public Version(final int major, final int minor, final int revision, final String suffix) {
		this.major = major;
		this.minor = minor;
		this.revision = revision;
		this.suffix = suffix;
	}

	/**
	 * Create a version
	 * 
	 * @param major
	 * @param minor
	 * @param revision
	 */
	public Version(final int major, final int minor, final int revision) {
		this(major, minor, revision, "");
	}

	/**
	 * Get the current software version
	 * 
	 * @return
	 */
	public static @NonNull Version currentVersion() {
//		return new Version(VERSION_MAJOR, VERSION_MINOR, VERSION_REVISION);
		return CURRENT_VERSION;
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
		final String[] parts = s.split("\\" + SEPARATOR);
		if (parts.length == 3)
			return new Version(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]),
					Integer.valueOf(parts[2]));
		if (parts.length == 4)
			return new Version(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]),
					Integer.valueOf(parts[2]), parts[3]);
		throw new IllegalArgumentException("Input string %s is not a version format".formatted(s));
	}

	/**
	 * Test if the given version is older than this version
	 * 
	 * @param v
	 * @return
	 */
	public boolean isOlderThan(@NonNull final Version v) {

		if (this.equals(v))
			return false;

		if (this.major < v.getMajor())
			return true;

		if (this.major > v.getMajor())
			return false;

		// Major version must be equal
		if (this.minor < v.getMinor())
			return true;

		if (this.minor > v.getMinor())
			return false;

		// Minor version must be equal
		if (this.revision < v.getRevision())
			return true;

		if (this.revision > v.getRevision())
			return false;

		// Revision must be equal
		if (this.hasSuffix() && !v.hasSuffix())
			return true; // only pre-release has a suffix

		if (v.hasSuffix() && !this.hasSuffix())
			return false; // only pre-release has a suffix

		// Both have a suffix; alphabetical comparison
		return this.suffix.compareTo(v.suffix) < 1;
	}

	/**
	 * Test if the given version is older than the current software version
	 * @param v
	 * @return
	 */
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
		result = prime * result + suffix.hashCode();
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
		final Version other = (Version) obj;
		if (major != other.major)
			return false;
		if (minor != other.minor)
			return false;
		if (revision != other.revision)
			return false;
		if (!suffix.equals(other.suffix))
			return false;
		return true;
	}

	/**
	 * Get the major version number. This indicates a substantial change in
	 * functionality and backwards compatibility with saved files is not maintained.
	 * 
	 * @return the major version number
	 */
	public int getMajor() {
		return major;
	}

	/**
	 * Get the minor version number. This indicates a change in functionality.
	 * Backwards compatibility with saved files is maintained.
	 * 
	 * @return the minor version number
	 */
	public int getMinor() {
		return minor;
	}

	/**
	 * Get the revision version number. This a bugfix only with no new functionality.
	 * Backwards compatibility with saved files is maintained.
	 * 
	 * @return the revision version number
	 */
	public int getRevision() {
		return revision;
	}

	/**
	 * Get the version suffix
	 * 
	 * @return the suffix or the empty string if not present
	 */
	public String getSuffix() {
		return suffix;
	}

	public boolean hasSuffix() {
		return !suffix.equals(EMPTY_STRING);
	}

	@Override
	public String toString() {
		if (suffix.equals(""))
			return major + SEPARATOR + minor + SEPARATOR + revision;
		return major + SEPARATOR + minor + SEPARATOR + revision + SEPARATOR + suffix;
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
		return version.getMajor() == CURRENT_VERSION.major;
	}

	/**
	 * Read the res/version.template to get the current version string as written
	 * from the pom
	 * 
	 * @return
	 */
	private static String readVersionTemplateFile() {
		String version = EMPTY_STRING;
		try (InputStream fstream = NuclearMorphologyAnalysis.class.getClassLoader()
				.getResourceAsStream("version.template");
				BufferedReader br = new BufferedReader(new InputStreamReader(fstream, StandardCharsets.UTF_8));) {

			String strLine;
			while ((strLine = br.readLine()) != null) {
				version += strLine;
			}
		} catch (final IOException e) {
			LOGGER.log(Level.SEVERE, "Cannot read version information", e);
		}
		return version;
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

		public @Nullable Version getDetectedVersion() {
			return detectedVersion;
		}
	}

}

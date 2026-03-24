package com.bmskinner.nma.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.ThreadManager;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Check for updates to the program on the Bitbucket repository
 * 
 * @author Ben Skinner
 * @since 1.15.0
 */
public class UpdateChecker {

	private static final Logger LOGGER = Logger.getLogger(UpdateChecker.class.getName());

	/** The standard naming patterns for a release */
	private static final String VERSION_PATTERN = "(\\d+\\.\\d+\\.\\d+)";

	private UpdateChecker() {
	}

	/**
	 * Control how much information is written to the logging panel when an update
	 * check is run. The default is to be quiet - i.e. only alert if there is an
	 * update available.
	 * 
	 */
	public static enum UpdateVerbosity {
		/** Only log important information. Normal launch behaviour */
		QUIET,

		/** Log all information. When update check manually requested */
		LOUD
	}

	/**
	 * Check for updates in a background thread and report outcome to logger.
	 * 
	 */
	public static void runUpdateCheck(UpdateVerbosity verbosity) {
		final Runnable updateCheckRunnable = () -> {
			if (verbosity.equals(UpdateVerbosity.LOUD)) {
				LOGGER.info("Checking for updates at %s".formatted(GlobalOptions.getInstance().getString(GlobalOptions.DEFAULT_UPDATE_URL_KEY)));
			}
			final Version latestVersion = UpdateChecker.fetchLatestVersionOnRemote();
			LOGGER.fine("Checked for updates, latest version at remote is %s".formatted(latestVersion));
			if (latestVersion.isNewerThan(Version.currentVersion())) {
				LOGGER.info("New version %s available".formatted(latestVersion));
			} else if(verbosity.equals(UpdateVerbosity.LOUD)) {
				LOGGER.info("Running %s. No newer version found; latest version at remote is %s. "
						.formatted(Version.currentVersion(), latestVersion));
				
			}
		};
		ThreadManager.getInstance().submit(updateCheckRunnable);
	}

	/**
	 * Get the latest version of the software found in the repository
	 * 
	 * @return the latest version found. Will be the current version on error.
	 */
	public static Version fetchLatestVersionOnRemote() {
		Version latestVersion = new Version(1, 13, 0); // an arbitrarily old version to start age checking from
		try {

			final Pattern p = Pattern.compile(VERSION_PATTERN);

			final String jsonString = downloadJson();

			final String tag = new JsonParser().parse(jsonString).getAsJsonObject().toString();

			final Matcher m = p.matcher(tag);
			if (m.find()) {
				final String vString = m.group(1);
				final Version v = Version.fromString(vString);
				if (v.isNewerThan(latestVersion)) {
					latestVersion = v;
				}
			}

		} catch (IOException | JsonSyntaxException e) {
			LOGGER.fine("Unable to fetch latest version from %s; using current version"
					.formatted(GlobalOptions.getInstance().getString(GlobalOptions.DEFAULT_UPDATE_URL_KEY)));
		}
		return latestVersion;
	}

	/**
	 * Check if an updated version of the software is available in the default
	 * repository.
	 * 
	 * @return true if an update is found, false on error, timeout or if this is the
	 *         latest version
	 */
	public static boolean isUpdateAvailable(Version testVersion) {

		boolean isLaterVersion = false;

		final Version latestVersion = fetchLatestVersionOnRemote();
		isLaterVersion = latestVersion.isNewerThan(testVersion);

		if (isLaterVersion) {
			LOGGER.fine("Found later version: " + latestVersion);
		} else {
			LOGGER.fine("Up to date: " + latestVersion);
		}
		return isLaterVersion;
	}

	private static String downloadJson() throws IOException {

		String result = "";

		final URL url = new URL(GlobalOptions.getInstance().getString(GlobalOptions.DEFAULT_UPDATE_URL_KEY));

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));) {

			final StringBuffer buffer = new StringBuffer();
			int read;
			final char[] chars = new char[1024];
			while ((read = reader.read(chars)) != -1) {
				buffer.append(chars, 0, read);
			}

			result = buffer.toString();
		} catch (final IOException e) {
			LOGGER.log(Level.FINE, "Unable to connect to remote server: %s".formatted(e.getMessage()), e);
			result = "Unable to connect";
		}
		return result;
	}

}

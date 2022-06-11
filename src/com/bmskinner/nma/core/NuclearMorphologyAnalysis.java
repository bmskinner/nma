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
package com.bmskinner.nma.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.XMLWriter;
import com.bmskinner.nma.logging.Loggable;

/**
 * This is the main class that runs the program.
 * 
 * @author bms41
 * @since 1.13.7
 *
 */
public class NuclearMorphologyAnalysis {

	private static NuclearMorphologyAnalysis instance = null;

	public CommandLineParser clp = null;

	/** Initialise the logger for the project namespace */
	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);

	static {

		// Create a config folder in the user home dir if needed
		File logFolder = Io.getLogDir();

		if (!logFolder.exists()) {
			logFolder.mkdirs();
		}

		try {
			LogManager.getLogManager().readConfiguration(
					NuclearMorphologyAnalysis.class.getClassLoader()
							.getResourceAsStream("logging.properties"));
		} catch (SecurityException | IOException e) {
			LOGGER.log(Level.SEVERE, "Unable to make log manager", e);
		}
	}

	public static NuclearMorphologyAnalysis getInstance() {
		if (instance == null) {
			String[] args = {};
			instance = new NuclearMorphologyAnalysis(args);
		}
		return instance;
	}

	/**
	 * Private constructor used when launching as a standalone program
	 * 
	 * @param args
	 */
	private NuclearMorphologyAnalysis(String[] args) {
		configureLogging();
		configureSettingsFiles();
		clp = new CommandLineParser(args);
	}

	/**
	 * Main entry when launching jar
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		instance = new NuclearMorphologyAnalysis(args);
	}

	/**
	 * Log the program status handlers and files and configure the logging options
	 * 
	 */
	private void configureLogging() {

		try {
			LOGGER.config("Log file location: " + Io.getLogDir().getAbsolutePath());

			GlobalOptions.getInstance().setString(GlobalOptions.LOG_DIRECTORY_KEY,
					Io.getLogDir().getAbsolutePath());

			LOGGER.config("OS: " + System.getProperty("os.name") + ", version "
					+ System.getProperty("os.version")
					+ ", " + System.getProperty("os.arch"));
			LOGGER.config(
					"JVM: " + System.getProperty("java.vendor") + ", version "
							+ System.getProperty("java.version"));
			LOGGER.config("NMA version: " + Version.currentVersion());

			// First invokation of the thread manager will log available resources
			ThreadManager.getInstance();

			// Clean up log files from older versions
			// 1.x.x stored logs in ~/.nma, rather than ~/.nma/logs
			removeV1Logs();

		} catch (SecurityException e) {
			LOGGER.log(Level.SEVERE, "Error initialising logger", e);
		}
	}

	private void removeV1Logs() {
		try {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					String lowercaseName = name.toLowerCase();
					return (lowercaseName.endsWith(".log"));
				}
			};

			for (File oldLog : Io.getConfigDir().listFiles(filter)) {
				Files.deleteIfExists(oldLog.toPath());
				LOGGER.fine("Removed v1 log file: " + oldLog.getName());
			}
		} catch (IOException e) {
			LOGGER.log(Loggable.STACK, "Unable to delete old v1 log file", e);
		}
	}

	/**
	 * Ensure all default settings files are present by creating if needed from
	 * inbuilt defaults
	 */
	private void configureSettingsFiles() {

		if (!Io.getRulesetDir().exists())
			try {
				Files.createDirectories(Io.getRulesetDir().toPath());
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Error creating ruleset dir", e);
			}

		ensureRuleSetFileExists(RuleSetCollection.mouseSpermRuleSetCollection(), "Mouse sperm.xml");
		ensureRuleSetFileExists(RuleSetCollection.pigSpermRuleSetCollection(), "Pig sperm.xml");
		ensureRuleSetFileExists(RuleSetCollection.roundRuleSetCollection(), "Round.xml");

	}

	/**
	 * Check if a file exists for the given ruleset. If not, create it.
	 * 
	 * @param rsc
	 * @param fileName
	 */
	private void ensureRuleSetFileExists(RuleSetCollection rsc, String fileName) {
		File ruleFile = new File(Io.getRulesetDir(), fileName);

		if (!ruleFile.exists()) {
			// create as needed
			LOGGER.config("Creating default ruleset: " + ruleFile.getAbsolutePath());
			try {
				XMLWriter.writeXML(rsc.toXmlElement(), ruleFile);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Unable to create default ruleset", e);
			}
		}
	}
}

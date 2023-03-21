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

import java.awt.EventQueue;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.UIManager;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.main.DockableMainWindow;
import com.bmskinner.nma.io.ConfigFileReader;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.XMLWriter;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.pipelines.BasicAnalysisPipeline;
import com.bmskinner.nma.pipelines.ExportDataPipeline;
import com.bmskinner.nma.pipelines.SavedOptionsAnalysisPipeline;

import ij.IJ;
import ij.Prefs;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

/**
 * This is the main class that runs the program.
 * 
 * @author bms41
 * @since 1.13.7
 *
 */
public class NuclearMorphologyAnalysis {

	private static NuclearMorphologyAnalysis instance = null;
	public DockableMainWindow mw = null;

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

	/**
	 * Get the current instance of NMA. Used for e.g screenshotting
	 * 
	 * @return
	 */
	public static NuclearMorphologyAnalysis getInstance() {
		if (instance == null) {
			instance = new NuclearMorphologyAnalysis(new CommandOptions());
		}
		return instance;
	}

	/**
	 * Private constructor used when launching as a standalone program
	 * 
	 * @param args
	 */
	private NuclearMorphologyAnalysis(CommandOptions opt) {
		configureLogging();
		configureSettingsFiles();
	}

	/**
	 * Main entry when launching jar
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		ArgumentParser parser = ArgumentParsers.newFor("Nuclear_Morphology_Analysis")
				.build()
				.defaultHelp(true)
				.description("Analyse nuclear morphometric data");
		parser.addArgument("-d", "--directory")
				.type(Arguments.fileType().verifyIsDirectory().verifyCanRead())
				.help("Directory of images to analyse");
		parser.addArgument("-o", "--options")
				.type(Arguments.fileType().verifyIsFile().verifyCanRead())
				.help("File of analysis options to use (.xml)");
		parser.addArgument("-f", "--file")
				.type(Arguments.fileType().verifyIsFile().verifyCanRead())
				.help("File with existing data (.nmd)");

		CommandOptions opt = new CommandOptions();

		try {
			parser.parseArgs(args, opt);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}

		instance = new NuclearMorphologyAnalysis(opt);

		// load the config file
		new ConfigFileReader();
		int ijThreads = GlobalOptions.getInstance().getInt(GlobalOptions.NUM_IMAGEJ_THREADS_KEY);
		Prefs.setThreads(ijThreads);
		LOGGER.finer(
				() -> "Internal ImageJ PluginFilter thread count set to %s".formatted(ijThreads));

		// No arguments provided, launch the GUI
		if (!opt.hasOptions()) {
			instance.runWithGUI();
			return;
		}

		// Arguments given, run headless
		if (opt.folder != null) {
			instance.runHeadless(opt.folder, opt.options);
		}

		if (opt.nmd != null)
			instance.runExport(opt.nmd);

	}

	/**
	 * Log the program status handlers and files and configure the logging options
	 * 
	 */
	private void configureLogging() {

		try {
			LOGGER.config(
					() -> "Log file location: %s".formatted(Io.getLogDir().getAbsolutePath()));

			GlobalOptions.getInstance().setString(GlobalOptions.LOG_DIRECTORY_KEY,
					Io.getLogDir().getAbsolutePath());

			LOGGER.config(() -> "OS: %s, version %s, %s"
					.formatted(System.getProperty("os.name"),
							System.getProperty("os.version"),
							System.getProperty("os.arch")));
			LOGGER.config(() -> "JVM: %s, version %s".formatted(System.getProperty("java.vendor"),
					System.getProperty("java.version")));
			LOGGER.config(() -> "NMA version: %s".formatted(Version.currentVersion()));

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
				LOGGER.log(Level.SEVERE,
						"Error creating ruleset directory: %s".formatted(e.getMessage()), e);
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
				LOGGER.log(Level.SEVERE,
						"Error creating default ruleset: %s".formatted(e.getMessage()), e);
			}
		}
	}

	private void runExport(File nmdFile) {

		if (!nmdFile.exists()) {
			LOGGER.warning(
					() -> "The file '%s' does not exist".formatted(nmdFile.getAbsolutePath()));
			System.exit(1);
		}
		LOGGER.info("Exporting data from file");
		try {
			new ExportDataPipeline(nmdFile);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error running pipeline: %s".formatted(e.getMessage()), e);
		}
	}

	/**
	 * Run in headless mode, specifying a folder of images, and a file of options
	 * 
	 * @param folder  the folder of images
	 * @param options
	 */
	private void runHeadless(final File folder, final File options) {
		LOGGER.config("Running headless");
		if (folder != null) {
			LOGGER.info("Running on folder: " + folder.getAbsolutePath());

			if (!folder.isDirectory()) {
				LOGGER.warning("A directory is required in the '-folder' argument");
				return;
			}
			try {
				if (options != null) {
					LOGGER.info("Running with saved options: " + options.getAbsolutePath());
					new SavedOptionsAnalysisPipeline(folder, options).call();
				} else {
					LOGGER.info(
							"No analysis options provided, using defaults and assuming these are mouse sperm");
					new BasicAnalysisPipeline(folder);
				}

			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error running pipeline: %s".formatted(e.getMessage()), e);
			}
		}
	}

	/**
	 * Load the program user interface
	 */
	private void runWithGUI() {

		try {
			Runnable r = new RunWithGui();
			EventQueue.invokeLater(r);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error loading GUI: %s".formatted(e.getMessage()), e);
		}
	}

	/**
	 * Runnable launcher that can be sent to the EDT
	 * 
	 * @author Ben Skinner
	 * @since 1.18.0
	 *
	 */
	private class RunWithGui implements Runnable {

		@Override
		public void run() {
			IJ.setBackgroundColor(0, 0, 0); // default background is black
			try {
				String lAndF = UIManager.getSystemLookAndFeelClassName();
				UIManager.setLookAndFeel(lAndF);
				LOGGER.config("Set UI look and feel to " + UIManager.getLookAndFeel().getName());

			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Unable to set look and feel", e);
			}

			// Ensure singleton instances created
			UserActionController.getInstance();
			DatasetListManager dlm = DatasetListManager.getInstance();
			UIController.getInstance().addDatasetAddedListener(dlm);

			mw = new DockableMainWindow();
			mw.setVisible(true);
		}

	}

	/**
	 * Store the options provided to the program via the command line
	 * 
	 * @author ben
	 * @since 2.1.0
	 *
	 */
	private static class CommandOptions {
		@Arg(dest = "directory")
		public File folder;

		@Arg(dest = "options")
		public File options;

		@Arg(dest = "file")
		public File nmd;

		public CommandOptions() {
		}

		public boolean hasOptions() {
			return folder != null || options != null || nmd != null;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (folder != null)
				sb.append(folder.getAbsolutePath() + Io.NEWLINE);
			if (options != null)
				sb.append(options.getAbsolutePath() + Io.NEWLINE);
			if (nmd != null)
				sb.append(nmd.getAbsolutePath() + Io.NEWLINE);
			return sb.toString();
		}
	}
}

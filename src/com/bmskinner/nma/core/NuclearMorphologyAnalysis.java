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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.UIManager;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.main.DockableMainWindow;
import com.bmskinner.nma.io.ConfigFileReader;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.XMLReader;
import com.bmskinner.nma.io.XMLReader.XMLReadingException;
import com.bmskinner.nma.io.XMLWriter;
import com.bmskinner.nma.pipelines.BasicAnalysisPipeline;
import com.bmskinner.nma.pipelines.ExportDataPipeline;
import com.bmskinner.nma.pipelines.MergeFilesPipeline;
import com.bmskinner.nma.pipelines.ModifyDataPipeline;
import com.bmskinner.nma.pipelines.SavedOptionsAnalysisPipeline;

import ij.IJ;
import ij.Prefs;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

/**
 * This is the main class that runs the program.
 * 
 * @author Ben Skinner
 * @since 1.13.7
 *
 */
public class NuclearMorphologyAnalysis {

	private static NuclearMorphologyAnalysis instance = null;
	public static DockableMainWindow mw = null;

	/** Initialise the logger for the project namespace */
	private static final Logger LOGGER = Logger.getLogger(NuclearMorphologyAnalysis.class.getName());

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
			instance = new NuclearMorphologyAnalysis();
		}
		return instance;
	}

	/**
	 * Private constructor used when launching as a standalone program
	 * 
	 * @param args
	 */
	private NuclearMorphologyAnalysis() {
		configureLogging();
		configureSettingsFiles();
	}

	/**
	 * Main entry when launching jar
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// If the program is launched without arguments, we launch the GUI
		// To do this, use a basic parser and check there are no arguments
		ArgumentParser uiParser = ArgumentParsers
				.newFor("Nuclear_Morphology_Analysis_" + Version.currentVersion()
						+ "_standalone.jar")
				.addHelp(false)
				.build();

		try {
			Map<String, Object> argMap = new HashMap<>();
			uiParser.parseArgs(args, argMap);
			if (argMap.isEmpty()) {
				loadConfigAndLaunch(new CommandOptions()); // options will be empty
				return;
			}
		} catch (ArgumentParserException e) {
			// ignore errors and move to the next parser
		}

		// If arguments were passed, we want to handle them with a dedicated parser
		ArgumentParser parser = ArgumentParsers
				.newFor("Nuclear_Morphology_Analysis_" + Version.currentVersion()
						+ "_standalone.jar")
				.build()
				.version(Version.currentVersion().toString())
				.defaultHelp(true)
				.description("Analyse nuclear morphometric data from microscope images");

		parser.addArgument("-v", "--version")
				.action(Arguments.version())
				.help("print the program version and exit");

		// Each type of functionality should have a separate command. Options can be
		// provided for each command specifically
		Subparsers subparsers = parser.addSubparsers()
				.title("subcommands")
				.description("valid subcommands")
				.metavar("COMMAND")
				.dest("runMode")
				.help("run <subcommand> -h for full options");

		// Sub parser for detecting nuclei in images
		createDetectParser(subparsers);

		// Sub parser for exporting data from an nmd
		createExportParser(subparsers);

		// Sub parser for modifying data in an nmd
		createModifyParser(subparsers);

		// Sub parser for merging nmd files
		createMergeParser(subparsers);

		// Store any options
		CommandOptions opt = new CommandOptions();

		try {
			parser.parseArgs(args, opt);

		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		} catch (IllegalArgumentException e) {
			System.err.println(Arrays.toString(args));
			System.err.println(opt.toString());
			e.printStackTrace();
			System.exit(1);
		}

		loadConfigAndLaunch(opt);

	}

	private static void createDetectParser(Subparsers subparsers) {
		Subparser analyseParser = subparsers.addParser("detect")
				.help("Analyse images using a saved options file");

		analyseParser.addArgument("-d", "--directory")
				.type(Arguments.fileType().verifyIsDirectory().verifyCanRead())
				.dest("directory")
				.required(true)
				.help("Directory of images to analyse");
		analyseParser.addArgument("-o", "--options")
				.type(Arguments.fileType().verifyIsFile().verifyCanRead())
				.dest("options")
				.help("File of analysis options to use (.xml)");
	}

	private static void createExportParser(Subparsers subparsers) {

		Subparser exportParser = subparsers.addParser("export")
				.help("Export data from an nmd file");

		exportParser.addArgument("-f", "--file")
				.type(Arguments.fileType().verifyIsFile().verifyCanRead())
				.required(true)
				.dest("file")
				.help("File with existing data (.nmd)");

		exportParser.addArgument("--measurements")
				.action(Arguments.storeTrue())
				.dest("measurements")
				.help("Export nuclear measurements");

		exportParser.addArgument("--profiles")
				.action(Arguments.storeTrue())
				.dest("profiles")
				.help("Export full nuclear profiles");

		exportParser.addArgument("--outlines")
				.action(Arguments.storeTrue())
				.dest("outlines")
				.help("Export full nuclear outlines");

		exportParser.addArgument("--cell-locations")
				.action(Arguments.storeTrue())
				.dest("cell-locations")
				.help("Export the cell centre-of-mass locations in their source images");

		exportParser.addArgument("--consensus")
				.action(Arguments.storeTrue())
				.dest("consensus")
				.help("Export consensus nucleus as SVG");

		exportParser.addArgument("--signals")
				.action(Arguments.storeTrue())
				.dest("signals")
				.help("Export nuclear signal measurements");

		exportParser.addArgument("--single-cell-images")
				.action(Arguments.storeTrue())
				.dest("single-cell-images")
				.help("Export each cell in a cropped image");

		exportParser.addArgument("--shells")
				.action(Arguments.storeTrue())
				.dest("shells")
				.help("Export nuclear signal shells");

		exportParser.addArgument("--analysis-options")
				.action(Arguments.storeTrue())
				.dest("analysis-options")
				.help("Export the analysis options for this dataset");

		exportParser.addArgument("--rulesets")
				.action(Arguments.storeTrue())
				.dest("rulesets")
				.help("Export the landmark rulesets for this dataset");

		exportParser.addArgument("--all")
				.action(Arguments.storeTrue())
				.dest("all")
				.help("Export all the above from the dataset except for single cell images");

	}

	private static void createModifyParser(Subparsers subparsers) {
		Subparser parser = subparsers.addParser("modify")
				.help("Modify data in an existing nmd file");

		parser.addArgument("-f", "--file")
				.type(Arguments.fileType().verifyIsFile().verifyCanRead())
				.dest("file")
				.help("File with existing data (.nmd)");

		parser.addArgument("--cluster-file")
				.type(Arguments.fileType().verifyIsFile().verifyCanRead())
				.dest("cluster-file")
				.help("File with clusters to import (tab-separated cell UUID and integer cluster number)");

		parser.addArgument("-o", "--options")
				.type(Arguments.fileType().verifyIsFile().verifyCanRead())
				.dest("options")
				.help("File with analysis options to run on this file");
	}

	private static void createMergeParser(Subparsers subparsers) {
		Subparser parser = subparsers.addParser("merge")
				.help("Merge existing nmd files");

		parser.addArgument("--output")
				.type(Arguments.fileType().verifyNotExists().verifyCanCreate())
				.dest("output")
				.help("Output nmd file for merged data");

		parser.addArgument("--input")
				.type(Arguments.fileType().verifyIsFile().verifyCanRead())
				.nargs("*")
				.dest("merge-sources")
				.help("Files to be merged into one nmd");

	}

	/**
	 * Given the input commands, run the appropriate action
	 * 
	 * @param opt
	 */
	private static void loadConfigAndLaunch(CommandOptions opt) {
		instance = new NuclearMorphologyAnalysis();
		
		ConfigFileReader.readConfigFile();
		
		int ijThreads = GlobalOptions.getInstance().getInt(GlobalOptions.NUM_IMAGEJ_THREADS_KEY);
		Prefs.setThreads(ijThreads);
		LOGGER.finer(
				() -> "Internal ImageJ PluginFilter thread count set to %s".formatted(ijThreads));

		if ("detect".equals(opt.runMode)) {
			// Arguments given, run headless
			instance.runHeadlessDetect(opt);
			return;
		}

		if ("export".equals(opt.runMode)) {
			instance.runHeadlessExport(opt);
			return;
		}

		if ("modify".equals(opt.runMode)) {
			instance.runHeadlessModify(opt);
			return;
		}

		if ("merge".equals(opt.runMode)) {
			instance.runHeadlessMerge(opt);
			return;
		}

		// No arguments provided, launch the GUI
		instance.runWithGUI();
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

			// Fix for issue 170 - requires a . in version string
			String javaVersion = System.getProperty("java.version");
			if (!javaVersion.contains("."))
				System.setProperty("java.version", javaVersion + ".0");

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
			FilenameFilter filter = (dir, name) -> {
				String lowercaseName = name.toLowerCase();
				return (lowercaseName.endsWith(".log"));
			};

			for (File oldLog : Io.getConfigDir().listFiles(filter)) {
				Files.deleteIfExists(oldLog.toPath());
				LOGGER.fine("Removed v1 log file: " + oldLog.getName());
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Unable to delete old v1 log file", e);
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

		// Check the default rulesets. If any have changed in this version,
		// create an update. If this version does not change anything,
		// do not create anything
		checkIfDefaultRulesetChanged(RuleSetCollection.mouseSpermRuleSetCollection(),
				"Mouse sperm." + Version.currentVersion() + ".xml");
		checkIfDefaultRulesetChanged(RuleSetCollection.pigSpermRuleSetCollection(),
				"Pig sperm." + Version.currentVersion() + ".xml");

		checkIfDefaultRulesetChanged(RuleSetCollection.roundRuleSetCollection(),
				"Round." + Version.currentVersion() + ".xml");

	}

	private void checkIfDefaultRulesetChanged(RuleSetCollection rsc, String fileName) {
		// Find rulesets with the same name

		boolean isPresent = false;
		for (File f : Io.getRulesetDir().listFiles()) {
			try {
				RuleSetCollection present = XMLReader.readRulesetCollection(f);

				if (rsc.equals(present)) {
					isPresent = true;
					LOGGER.fine(() -> "Found existing ruleset with unchanged optons for %s"
							.formatted(rsc.getName()));
					break;
				}

			} catch (XMLReadingException | ComponentCreationException e) {
				LOGGER.log(Level.SEVERE,
						"Error reading ruleset: %s".formatted(e.getMessage()), e);
			}
		}

		// create default rulesets for this version of NMA if needed
		if (!isPresent) {
			LOGGER.fine(
					() -> "Ruleset for %s has changed in this version, adding"
							.formatted(rsc.getName()));
			ensureRuleSetFileExists(rsc, fileName);
		}
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

	/**
	 * Run in headless mode, specifying an nmd file and what to export
	 * 
	 * @param opt the options
	 */
	private void runHeadlessExport(final CommandOptions opt) {
		try {
			LOGGER.info("Running export function");
			new ExportDataPipeline(opt);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error running pipeline: %s".formatted(e.getMessage()), e);
		}
	}

	/**
	 * Run in headless mode, specifying a folder of images, and a file of options
	 * 
	 * @param opt the options
	 */

	private void runHeadlessDetect(final CommandOptions opt) {

		LOGGER.info("Running on folder: " + opt.directory.getAbsolutePath());

		try {
			if (opt.options != null) {
				LOGGER.info("Running with saved options: " + opt.options.getAbsolutePath());
				new SavedOptionsAnalysisPipeline(opt.directory, opt.options).call();
			} else {
				LOGGER.info(
						"No analysis options provided, using defaults and assuming these are mouse sperm");
				new BasicAnalysisPipeline(opt.directory);
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error running pipeline: %s".formatted(e.getMessage()), e);
		}

	}

	/**
	 * Run in headless mode, analysing the data in an nmd
	 * 
	 * @param opt the options
	 */

	private void runHeadlessModify(final CommandOptions opt) {

		try {
			LOGGER.info("Running analysis function");
			new ModifyDataPipeline(opt);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error running pipeline: %s".formatted(e.getMessage()), e);
		}

	}

	/**
	 * Run in headless mode, merging nmd files
	 * 
	 * @param opt the options
	 */

	private void runHeadlessMerge(final CommandOptions opt) {

		try {
			LOGGER.info("Running merge function");
			new MergeFilesPipeline(opt);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error running pipeline: %s".formatted(e.getMessage()), e);
		}

	}

	/**
	 * Load the program user interface. This is public so we can create the UI for
	 * screenshotting.
	 */

	public void runWithGUI() {

		if (mw == null) {
			try {
				Runnable r = new RunWithGui();
				EventQueue.invokeLater(r);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error loading GUI: %s".formatted(e.getMessage()), e);
			}
		} else {
			LOGGER.fine("Existing GUI instance");
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

			// We need to create the JFrame after the look and feel has been set
			mw = new DockableMainWindow();
			mw.setVisible(true);
		}

	}

}

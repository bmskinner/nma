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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.main.DockableMainWindow;
import com.bmskinner.nma.io.ConfigFileReader;
import com.bmskinner.nma.pipelines.BasicAnalysisPipeline;
import com.bmskinner.nma.pipelines.ExportDataPipeline;
import com.bmskinner.nma.pipelines.SavedOptionsAnalysisPipeline;

import ij.IJ;
import ij.Prefs;

/**
 * Handle arguments passed to the program via the command line.
 * 
 * @author bms41
 * @since 1.13.7
 *
 */
public class CommandLineParser {

	private static final Logger LOGGER = Logger.getLogger(CommandLineParser.class.getName());

	public DockableMainWindow mw = null;

	/**
	 * Construct with an array of parameters for the program to interpret
	 * 
	 * @param arr
	 */
	public CommandLineParser(String[] arr) {
		execute(arr);
	}

	/**
	 * Execute the commands provided
	 * 
	 * @param arr
	 */
	private void execute(String[] arr) {

		// load the config file
		new ConfigFileReader();
		int ijThreads = GlobalOptions.getInstance().getInt(GlobalOptions.NUM_IMAGEJ_THREADS_KEY);
		Prefs.setThreads(ijThreads);
		LOGGER.config("Internal ImageJ PluginFilter thread count set to " + ijThreads);

		// No arguments provided, launch the GUI
		if (arr == null || arr.length == 0) {
			runWithGUI();
			return;
		}

		// Arguments given, run headless

		Map<String, File> commands = new HashMap<>();
		for (String s : arr) {
			LOGGER.info("Argument: " + s);

			if (s.startsWith("-h")) {
				LOGGER.config("Arguments:");
				LOGGER.config("\t-folder=<image_folder>");
				LOGGER.config("\t-options=<xml_options>");
				LOGGER.config("\t-nmd=<nmd_file>");
				System.exit(0);
			}

			if (s.startsWith("-folder=")) {
				String path = s.replace("-folder=", "").replace("\"", "");
				commands.put("folder", new File(path));
			}

			if (s.startsWith("-options=")) {
				String path = s.replace("-options=", "").replace("\"", "");
				commands.put("options", new File(path));
			}

			// Provide an nmd, just export the stats
			if (s.startsWith("-nmd=")) {
				String nmdFile = s.replace("-nmd=", "").replace("\"", "");
				commands.put("nmd", new File(nmdFile));
			}
		}

		LOGGER.info("Parsed arguments");

		if (commands.containsKey("folder")) {
			LOGGER.info("Running pipeline analysis");
			runHeadless(commands.get("folder"), commands.get("options"));
		}

		if (commands.containsKey("nmd")) {
			LOGGER.info("Running export");
			runExport(commands.get("nmd"));
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
			LOGGER.log(Level.SEVERE, "Error running pipeline", e);
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
				LOGGER.log(Level.SEVERE, "Error in pipeline", e);
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
			LOGGER.log(Level.SEVERE, "Error loading GUI", e);
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
}

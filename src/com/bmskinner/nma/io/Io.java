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
package com.bmskinner.nma.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bmskinner.nma.logging.Loggable;

/**
 * Top level interface for IO operations. Track the common file extensions and
 * provide access to the separate import and export operations.
 * 
 * @author bms41
 * @since 1.13.8
 *
 */
public interface Io {

	String NEWLINE = System.getProperty("line.separator");
	String TAB = "\t";
	String DOT = ".";
	String COMMA = ",";
	String PIPE = "|";
	String SPACE = " ";
	String NA = "NA";

	String TAB_FILE_EXTENSION = ".txt";
	String TAB_FILE_EXTENSION_NODOT = "txt";

	String PNG_FILE_EXTENSION_NODOT = "png";
	String PNG_FILE_EXTENSION = DOT + PNG_FILE_EXTENSION_NODOT;

	String SVG_FILE_EXTENSION_NODOT = "svg";
	String SVG_FILE_EXTENSION = DOT + SVG_FILE_EXTENSION_NODOT;

	String NMD_FILE_EXTENSION_NODOT = "nmd";
	String BACKUP_FILE_EXTENSION_NODOT = "bak";
	String NMD_FILE_EXTENSION = DOT + NMD_FILE_EXTENSION_NODOT;
	String BACKUP_FILE_EXTENSION = DOT + BACKUP_FILE_EXTENSION_NODOT;
	String LOG_FILE_EXTENSION = ".log";
	String LOCK_FILE_EXTENSION = ".lck";

	/** Locations of saved cells */
	String LOC_FILE_EXTENSION = "cell";

	/** Backup files made in nmd conversions */
	String BAK_FILE_EXTENSION = ".bak";

	/** Workspace file extension */
	String WRK_FILE_EXTENSION_NODOT = "wrk";
	String WRK_FILE_EXTENSION = DOT + WRK_FILE_EXTENSION_NODOT;

	String XML_FILE_EXTENSION_NODOT = "xml";
	String XML_FILE_EXTENSION = DOT + XML_FILE_EXTENSION_NODOT;

	String TIFF_FILE_EXTENSION_NODOT = "tiff";
	String TIFF_FILE_EXTENSION = DOT + TIFF_FILE_EXTENSION_NODOT;

	String TEXT_FILE_EXTENSION_NODOT = "txt";
	String TEXT_FILE_EXTENSION = DOT + TEXT_FILE_EXTENSION_NODOT;

	String INVALID_FILE_ERROR = "File is not valid for importing";
	String CHANNEL_BELOW_ZERO_ERROR = "Channel cannot be less than 0";

	/** The folder to write and store logs and configuration */
	String CONFIG_FOLDER_NAME = ".nma";

	String RULESET_FOLDER_NAME = "rulesets";

	String LOG_FOLDER_NAME = "logs";

	/** The file to write and store configuration */
	String CONFIG_FILE_NAME = "config.ini";

	/**
	 * Get the directory that the program is being run from
	 * 
	 * @return the program directory
	 */
	static File getProgramDir() {

		try {
			// Get the location of the jar file
			File dir = new File(Importer.class.getProtectionDomain().getCodeSource().getLocation()
					.toURI().getPath());
			// Difference in path between standalone and jar
			if (dir.getAbsolutePath().endsWith(".jar") || dir.getAbsolutePath().endsWith(".exe"))
				dir = dir.getParentFile();
			return dir;
		} catch (URISyntaxException e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING,
					"Error getting program dir");
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Loggable.STACK, e.getMessage(), e);
			return null;
		}

	}

	/**
	 * Get the configuration directory
	 * 
	 * @return
	 */
	static File getConfigDir() {
		return new File(System.getProperty("user.home"), CONFIG_FOLDER_NAME);
	}

	/**
	 * Get the rulesets directory
	 * 
	 * @return
	 */
	static File getRulesetDir() {
		File dir = getConfigDir();
		return new File(dir, RULESET_FOLDER_NAME);
	}

	/**
	 * Get the rulesets directory
	 * 
	 * @return
	 */
	static File getLogDir() {
		File dir = getConfigDir();
		return new File(dir, LOG_FOLDER_NAME);
	}

	/**
	 * Get the config file read at launch
	 * 
	 * @return
	 */
	static File getConfigFile() {
		File dir = getConfigDir();
		return new File(dir, CONFIG_FILE_NAME);
	}

	/**
	 * Get the most recent log file read at launch
	 * 
	 * @return
	 */
	static File getLogFile() {
		File dir = getLogDir();

		File[] files = dir.listFiles((d, name) -> name.endsWith("0.log"));

		if (files == null || files.length == 0)
			return null;

		return files[0];
	}

	/**
	 * Static methods for exporting data
	 * 
	 * @author ben
	 *
	 */
	public static class Exporter {

		/**
		 * Write a string to a given file
		 * 
		 * @param s
		 * @param f
		 */
		public static boolean writeString(final String s, final File f) {

			if (f == null) {
				throw new IllegalArgumentException("File cannot be null");
			}

			try (PrintWriter out = new PrintWriter(f)) {

				out.println(s);

			} catch (FileNotFoundException e) {
				// No action
				return false;
			}
			return true;
		}
	}

	interface Importer extends Io {

		/**
		 * Replace the old file extension in the given file and return a new file
		 * 
		 * @param f
		 * @param oldExt
		 * @param newExt
		 * @return
		 */
		static File replaceFileExtension(final File f, final String oldExt, final String newExt) {

			if (!f.getName().endsWith(oldExt)) {
				throw new IllegalArgumentException("Old extension not found");
			}
			String newFileName = f.getAbsolutePath().replace(oldExt, newExt);
			return new File(newFileName);

		}

		/**
		 * Test if the given folder contains at least 1 image file that can be imported.
		 * Importable image files are defined in ImageImporter.IMPORTABLE_FILE_TYPES
		 * 
		 * @param folder the folder to test
		 * @return
		 */
		static boolean containsImportableImageFiles(final File folder) {
			if (folder == null)
				return false;
			if (!folder.exists())
				return false;
			if (!folder.isDirectory())
				return false;
			for (String fileType : ImageImporter.IMPORTABLE_FILE_TYPES) {
				for (File f : folder.listFiles()) {
					if (f.getName().endsWith(fileType))
						return true;
				}
			}
			return false;
		}

		/**
		 * Test if the given file can be imported
		 * 
		 * @param f
		 * @return
		 */
		static boolean isSuitableImportFile(final File f) {
			if (f == null)
				return false;
			if (!f.exists())
				return false;
			if (f.isDirectory())
				return false;
			if (!f.isFile())
				return false;
			return true;
		}

		/**
		 * Complement to isSuitableImportFile; explains why that failed
		 * 
		 * @param f
		 * @return
		 */
		static String whyIsUnsuitableImportFile(final File f) {
			if (f == null)
				return "File variable is null";
			if (!f.exists())
				return f.getAbsolutePath() + " does not exist";
			if (f.isDirectory())
				return f.getAbsolutePath() + " is a directory";
			if (!f.isFile())
				return f.getAbsolutePath() + " is not a file";
			return "";
		}
	}

}

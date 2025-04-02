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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nma.io.XMLReader.XMLReadingException;

/**
 * Read the config file and assign values to the global options of the program
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
public class ConfigFileReader {

	private static final Logger LOGGER = Logger.getLogger(ConfigFileReader.class.getName());

	private ConfigFileReader() {
		// static access only
	}
	
	/**
	 * Read the NMA config file into the Global Options
	 */
	public static void readConfigFile() {
		try {
			File ini = Io.getConfigFile();
			LOGGER.config("Configuration file read from: %s".formatted(ini.getAbsolutePath()));

			if (ini.exists()) {
				// Read the properties
				Properties properties = new Properties();
				properties.load(new FileInputStream(ini));
				assignGlobalOptions(properties);
			} else {
				LOGGER.config("Config file does not exist; creating with default values");
				writeGlobalOptionsToConfigFile();
			}

		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error reading config ini file", e);
		}

	}
	
	public static void writeGlobalOptionsToConfigFile() throws FileNotFoundException, IOException {
		Properties properties = createPropertiesFromGlobalOptions();
		properties.store(new FileOutputStream(Io.getConfigFile()), null);
	}
	
	public record RulesetEntry(File file, RuleSetCollection rsc) {
		@Override
		public String toString() {
			return rsc.getName() + " (" + rsc.getRulesetVersion() + ")";
		}
	}

	public static RulesetEntry[] getAvailableRulesets() {
		LOGGER.finer("Reading available rulesets from disk");
		File[] files = Io.getRulesetDir()
				.listFiles((d, s) -> s.toLowerCase().endsWith(Io.XML_FILE_EXTENSION));

		return Arrays.stream(files)
				.map(f -> {
					try {
						return new RulesetEntry(f,
								XMLReader.readRulesetCollection(f));
					} catch (XMLReadingException | ComponentCreationException e) {
						LOGGER.log(Level.SEVERE,
								"Unable to read ruleset from file: %s".formatted(e.getMessage()),
								e);
						return new RulesetEntry(null, null);
					}
				})
				.toArray(RulesetEntry[]::new);
	}
	
	private static Properties createPropertiesFromGlobalOptions() {
		Properties properties = new Properties();

		GlobalOptions op = GlobalOptions.getInstance();

		properties.setProperty(GlobalOptions.DEFAULT_DIR_KEY, op.getDefaultDir().getAbsolutePath());
		properties.setProperty(GlobalOptions.DEFAULT_RULESET_KEY,
				GlobalOptions.DEFAULT_RULESET);
		properties.setProperty(GlobalOptions.DEFAULT_IMAGE_SCALE_KEY,
				String.valueOf(op.getImageScale()));
		properties.setProperty(GlobalOptions.DEFAULT_DISPLAY_SCALE_KEY,
				String.valueOf(op.getDisplayScale().name()));
		properties.setProperty(GlobalOptions.DEFAULT_FILL_CONSENSUS_KEY,
				String.valueOf(op.isFillConsensus()));
		properties.setProperty(GlobalOptions.DEFAULT_USE_ANTIALIASING_KEY,
				String.valueOf(op.isAntiAlias()));
		properties.setProperty(GlobalOptions.DEFAULT_SWATCH_KEY,
				String.valueOf(op.getSwatch().name()));
		properties.setProperty(GlobalOptions.REFOLD_OVERRIDE_KEY,
				String.valueOf(op.getBoolean(GlobalOptions.REFOLD_OVERRIDE_KEY)));
		properties.setProperty(GlobalOptions.IS_DEBUG_INTERFACE_KEY,
				String.valueOf(op.getBoolean(GlobalOptions.IS_DEBUG_INTERFACE_KEY)));
		properties.setProperty(GlobalOptions.IS_GLCM_INTERFACE_KEY,
				String.valueOf(op.getBoolean(GlobalOptions.IS_GLCM_INTERFACE_KEY)));
		properties.setProperty(GlobalOptions.ALLOW_UPDATE_CHECK_KEY,
				String.valueOf(op.getBoolean(GlobalOptions.ALLOW_UPDATE_CHECK_KEY)));
		properties.setProperty(GlobalOptions.IS_SINGLE_THREADED_DETECTION,
				String.valueOf(op.getBoolean(GlobalOptions.IS_SINGLE_THREADED_DETECTION)));
		properties.setProperty(GlobalOptions.WARN_LOW_JVM_MEMORY_FRACTION,
				String.valueOf(op.getBoolean(GlobalOptions.WARN_LOW_JVM_MEMORY_FRACTION)));
		return properties;

	}

	private static void assignGlobalOptions(Properties properties) {

		GlobalOptions op = GlobalOptions.getInstance();

		for (String key : properties.stringPropertyNames()) {

			String value = properties.getProperty(key);

			LOGGER.config(() -> "Assigning global option %s: %s".formatted(key, value));

			if (GlobalOptions.DEFAULT_DIR_KEY.equals(key))
				op.setDefaultDir(new File(value));

			if (GlobalOptions.DEFAULT_RULESET_KEY.equals(key))
				op.setString(GlobalOptions.DEFAULT_RULESET_KEY, value);

			if (GlobalOptions.DEFAULT_IMAGE_SCALE_KEY.equals(key))
				op.setImageScale(Double.valueOf(value));

			if (GlobalOptions.DEFAULT_DISPLAY_SCALE_KEY.equals(key))
				op.setDisplayScale(MeasurementScale.valueOf(value));

			if (GlobalOptions.DEFAULT_FILL_CONSENSUS_KEY.equals(key))
				op.setFillConsensus(Boolean.valueOf(value));

			if (GlobalOptions.DEFAULT_USE_ANTIALIASING_KEY.equals(key))
				op.setAntiAlias(Boolean.valueOf(value));

			if (GlobalOptions.DEFAULT_SWATCH_KEY.equals(key))
				op.setSwatch(ColourSwatch.valueOf(value));

			if (GlobalOptions.REFOLD_OVERRIDE_KEY.equals(key))
				op.setBoolean(GlobalOptions.REFOLD_OVERRIDE_KEY, Boolean.valueOf(value));

			if (GlobalOptions.IS_DEBUG_INTERFACE_KEY.equals(key))
				op.setBoolean(GlobalOptions.IS_DEBUG_INTERFACE_KEY, Boolean.valueOf(value));

			if (GlobalOptions.IS_GLCM_INTERFACE_KEY.equals(key))
				op.setBoolean(GlobalOptions.IS_GLCM_INTERFACE_KEY, Boolean.valueOf(value));

			if (GlobalOptions.NUM_IMAGEJ_THREADS_KEY.equals(key))
				op.setInt(GlobalOptions.NUM_IMAGEJ_THREADS_KEY, Integer.valueOf(value));

			if (GlobalOptions.ALLOW_UPDATE_CHECK_KEY.equals(key))
				op.setBoolean(GlobalOptions.ALLOW_UPDATE_CHECK_KEY, Boolean.valueOf(value));

			if (GlobalOptions.IS_SINGLE_THREADED_DETECTION.equals(key))
				op.setBoolean(GlobalOptions.IS_SINGLE_THREADED_DETECTION, Boolean.valueOf(value));
			
			if (GlobalOptions.WARN_LOW_JVM_MEMORY_FRACTION.equals(key))
				op.setBoolean(GlobalOptions.WARN_LOW_JVM_MEMORY_FRACTION, Boolean.valueOf(value));

		}
	}

}

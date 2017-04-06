/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Read the config file and assign values to the global options of the program  
 * @author bms41
 * @since 1.13.4
 *
 */
public class PropertiesReader implements Loggable {
	
	public static final String INI_FILE = "config.ini";
	
	private static final String DEFAULT_DIR_KEY   = "DEFAULT_DIR";
	private static final String DEFAULT_IMAGE_SCALE_KEY = "DEFAULT_IMAGE_SCALE";
	private static final String DEFAULT_DISPLAY_SCALE_KEY = "DEFAULT_DISPLAY_SCALE";
	private static final String DEFAULT_FILL_CONSENSUS_KEY = "FILL_CONSENSUS";
	private static final String DEFAULT_USE_ANTIALIASING_KEY = "USE_ANTIALIASING";
	private static final String DEFAULT_SWATCH_KEY = "DEFAULT_COLOUR_SWATCH";

	public PropertiesReader() {
		try {

			// Get the location of the jar file
			File dir =  Importer.getProgramDir();
			
			File ini = new File(dir, INI_FILE);
			System.out.println("ini: "+ini.getAbsolutePath());
			
			if(ini.exists()){
				// Read the properties
				Properties properties = new Properties();
	
				properties.load(new FileInputStream(ini));
	
				assignOptions(properties);
			} else {
				log("No ini file: creating default");
				Properties properties = createDefaultProperties();
				properties.store(new FileOutputStream(ini), null);
			}

		} catch (IOException e) {
			stack("Error reading ini file", e);
		}

	}
	
	private Properties createDefaultProperties(){
		Properties properties = new Properties();
		
		GlobalOptions op = GlobalOptions.getInstance();
		
		properties.setProperty(DEFAULT_DIR_KEY, op.getDefaultDir().getAbsolutePath());
		properties.setProperty(DEFAULT_IMAGE_SCALE_KEY, String.valueOf(op.getImageScale()));
		properties.setProperty(DEFAULT_DISPLAY_SCALE_KEY, String.valueOf(op.getScale().name()));
		properties.setProperty(DEFAULT_FILL_CONSENSUS_KEY, String.valueOf(op.isFillConsensus()));
		properties.setProperty(DEFAULT_USE_ANTIALIASING_KEY, String.valueOf(op.isAntiAlias()));
		properties.setProperty(DEFAULT_SWATCH_KEY, String.valueOf(op.getSwatch().name()));
		
		return properties;
		
	}
	
	private void assignOptions(Properties properties){
		
		GlobalOptions op = GlobalOptions.getInstance();
		
		for(String key : properties.stringPropertyNames()) {

			
			String value = properties.getProperty(key);
			
			if(DEFAULT_DIR_KEY.equals(key)){
				op.setDefaultDir( new File(value));
			}
			
			if(DEFAULT_IMAGE_SCALE_KEY.equals(key)){
				op.setImageScale(Double.valueOf(value));
			}
			
			if(DEFAULT_DISPLAY_SCALE_KEY.equals(key)){
				op.setScale(MeasurementScale.valueOf(value));
			}
			
			if(DEFAULT_FILL_CONSENSUS_KEY.equals(key)){
				op.setFillConsensus(Boolean.valueOf(value));
			}
			
			if(DEFAULT_USE_ANTIALIASING_KEY.equals(key)){
				op.setAntiAlias(Boolean.valueOf(value));
			}
			
			if(DEFAULT_SWATCH_KEY.equals(key)){
				op.setSwatch(ColourSwatch.valueOf(value));
			}

		}
	}

}

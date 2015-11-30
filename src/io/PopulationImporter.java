/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



//import utility.Logger;
import analysis.AnalysisDataset;
import components.CellCollection;
import components.nuclei.Nucleus;

public class PopulationImporter {
		
	public static AnalysisDataset readDataset(File inputFile, Logger programLogger) throws Exception {

		if(!inputFile.exists()){
			throw new IllegalArgumentException("Requested file does not exist");
		}

		AnalysisDataset dataset = null;

		FileInputStream fis = null;
		ObjectInputStream ois = null;
				
		try {
			fis = new FileInputStream(inputFile.getAbsolutePath());

			ois = new ObjectInputStream(fis);
			
			dataset = (AnalysisDataset) ois.readObject();
			
			// Replace existing save file path with the path to the file that has been opened
			
			if(!dataset.getSavePath().equals(inputFile)){
				programLogger.log(Level.FINE, "File path has changed: attempting to relocate images");

				dataset.setSavePath(inputFile);

				// Check if the image folders are present in the correct relative directories
				// If so, update the CellCollection image paths

				//			should be /ImageDir/AnalysisDir/dataset.nmd

				File expectedAnalysisDirectory = inputFile.getParentFile();
				File expectedImageDirectory = expectedAnalysisDirectory.getParentFile();
				programLogger.log(Level.FINE, "Searching "+expectedImageDirectory.getAbsolutePath());

				if(expectedAnalysisDirectory.exists() && expectedImageDirectory.exists()){

					programLogger.log(Level.FINE, "Updating dataset image paths");
					dataset.getCollection().updateSourceFolder(expectedImageDirectory);
					
					programLogger.log(Level.FINE, "Updating child dataset image paths");
					for(AnalysisDataset child : dataset.getAllChildDatasets()){
						child.getCollection().updateSourceFolder(expectedImageDirectory);
					}
					
					programLogger.log(Level.FINE, "Updated all images");

				} else {
					programLogger.log(Level.FINE, "Unable to locate image directory and/or analysis directory");
				}
			}

		} catch (FileNotFoundException e1) {
			programLogger.log(Level.SEVERE, "File not found: "+inputFile.getAbsolutePath(), e1);
		} catch (IOException e1) {
			programLogger.log(Level.SEVERE, "File IO error: "+inputFile.getAbsolutePath(), e1);
		} catch (ClassNotFoundException e1) {
			programLogger.log(Level.SEVERE, "Class not found error: "+inputFile.getAbsolutePath(), e1);
		} finally {
			ois.close();
			fis.close();
		}
		return dataset;
	}


}
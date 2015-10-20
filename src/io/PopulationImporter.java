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

public class PopulationImporter {
	
//	private static Logger programLogger;
//	private static Logger fileLogger;


	public static CellCollection readPopulation(File inputFile, Logger programLogger){
		
		if(!inputFile.exists()){
			programLogger.log(Level.INFO, "Requested file does not exist");
			throw new IllegalArgumentException("Requested file does not exist");
		}

		CellCollection collection = null;

		FileInputStream fis;
		try {
			fis = new FileInputStream(inputFile.getAbsolutePath());

			ObjectInputStream ois = new ObjectInputStream(fis);

			programLogger.log(Level.INFO, "Reading file...");
			List<Object> inputList = new ArrayList<Object>(0);

			try{
				Object inputObject = ois.readObject();
				while (inputObject != null){
					inputList.add(inputObject);
					inputObject = ois.readObject();
				}
			} catch (Exception e) { // exception occurs on reaching EOF

				programLogger.log(Level.INFO, "OK");
				collection = (CellCollection) inputList.get(0);

				programLogger.log(Level.INFO, "File imported");

			} finally {
				ois.close();
				fis.close();
			}
		} catch (FileNotFoundException e1) {
			programLogger.log(Level.SEVERE, "File not found: "+inputFile.getAbsolutePath(), e1);
		} catch (IOException e1) {
			programLogger.log(Level.SEVERE, "File IO error: "+inputFile.getAbsolutePath(), e1);
		}
		return collection;
	}
	
	public static AnalysisDataset readDataset(File inputFile, Logger programLogger){

		if(!inputFile.exists()){
			throw new IllegalArgumentException("Requested file does not exist");
		}

//		logger = new Logger(new File(inputFile.getParent()), "PopulationImporter");

		AnalysisDataset dataset = null;

		FileInputStream fis;
		try {
			fis = new FileInputStream(inputFile.getAbsolutePath());

			ObjectInputStream ois = new ObjectInputStream(fis);

			List<Object> inputList = new ArrayList<Object>(0);

			try{
				Object inputObject = ois.readObject();
				while (inputObject != null){
					inputList.add(inputObject);
					inputObject = ois.readObject();
				}
			} catch (Exception e) { // exception occurs on reaching EOF

				dataset = (AnalysisDataset) inputList.get(0);

			} finally {
				ois.close();
				fis.close();
			}
		} catch (FileNotFoundException e1) {
			programLogger.log(Level.SEVERE, "File not found: "+inputFile.getAbsolutePath(), e1);
		} catch (IOException e1) {
			programLogger.log(Level.SEVERE, "File IO error: "+inputFile.getAbsolutePath(), e1);
		}
		return dataset;
	}


}
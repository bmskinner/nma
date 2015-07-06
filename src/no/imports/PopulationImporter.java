package no.imports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import utility.Logger;
import no.analysis.AnalysisDataset;
import no.collections.NucleusCollection;
import no.gui.MainWindow;

public class PopulationImporter {
	
	private static Logger logger;


	public static NucleusCollection readPopulation(File inputFile, MainWindow mw){
		
		if(!inputFile.exists()){
			mw.log("Requested file does not exist");
			throw new IllegalArgumentException("Requested file does not exist");
		}
		
		logger = new Logger(new File(inputFile.getParent()), "PopulationImporter");

		NucleusCollection collection = null;

		FileInputStream fis;
		try {
			fis = new FileInputStream(inputFile.getAbsolutePath());

			ObjectInputStream ois = new ObjectInputStream(fis);

			mw.logc("Reading file...");
			List<Object> inputList = new ArrayList<Object>(0);

			try{
				Object inputObject = ois.readObject();
				while (inputObject != null){
					inputList.add(inputObject);
					inputObject = ois.readObject();
				}
			} catch (Exception e) { // exception occurs on reaching EOF

				mw.log("OK");
				collection = (NucleusCollection) inputList.get(0);

				mw.log("File imported");

			} finally {
				ois.close();
				fis.close();
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return collection;
	}
	
	public static AnalysisDataset readDataset(File inputFile){

		if(!inputFile.exists()){
			throw new IllegalArgumentException("Requested file does not exist");
		}

		logger = new Logger(new File(inputFile.getParent()), "PopulationImporter");

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
			logger.log("File not found: "+inputFile.getAbsolutePath()+" : "+e1.getMessage(), Logger.ERROR);
		} catch (IOException e1) {
			logger.log("File IO error: "+e1.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e1.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		}
		return dataset;
	}


}
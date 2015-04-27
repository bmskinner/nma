package no.imports;

import ij.IJ;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import no.collections.INuclearCollection;
import no.collections.NucleusCollection;
import no.nuclei.INuclearFunctions;

public class PopulationImporter {


	public static INuclearCollection readPopulation(File inputFile){
		
		if(!inputFile.exists()){
			throw new IllegalArgumentException("Requested file does not exist");
		}

		NucleusCollection collection = null;

		FileInputStream fis;
		try {
			fis = new FileInputStream(inputFile.getAbsolutePath());

			ObjectInputStream ois = new ObjectInputStream(fis);

			// the original output order for the population
			//	    output.writeObject(collection.getFolder());
			//		output.writeObject(collection.getOutputFolderName());
			//		output.writeObject(collection.getType());
			//		output.writeObject(collection.getNuclei());

			IJ.log("Reading file...");
			List<Object> inputList = new ArrayList<Object>(0);

			try{
				Object inputObject = ois.readObject();
				while (inputObject != null){
					inputList.add(inputObject);
					inputObject = ois.readObject();
				}
			} catch (Exception e) { // exception occurs on reaching EOF

				IJ.log("Read "+inputList.size()+" items");

				File folder = (File) inputList.get(0);
				String outputFolder = (String) inputList.get(1);
				String type = (String)inputList.get(2);
				collection = new NucleusCollection(folder, outputFolder, type);

				@SuppressWarnings("unchecked")
				List<INuclearFunctions> list = (List<INuclearFunctions>) inputList.get(3);

				for(INuclearFunctions n : list){
					collection.addNucleus(n);
//					IJ.log("Found "+n.getClass().getSimpleName());
				}

				IJ.log("File imported");

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


}
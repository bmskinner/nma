package no.export;

import ij.IJ;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import no.collections.INuclearCollection;
import no.nuclei.INuclearFunctions;

public class PopulationExporter {

	public static void savePopulation(INuclearCollection collection){

		File saveFile = new File(collection.getFolder()+"."+collection.getType()+".sav");

		try{
			//use buffering
			OutputStream file = new FileOutputStream(saveFile);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			
			IJ.log("    Saving data to file...");

			try{

				for(INuclearFunctions n : collection.getNuclei()){
					IJ.log("      Next nucleus...");
					output.writeObject(n);
				}
				IJ.log("    Save complete");

			} catch(IOException e){
				IJ.log("    Unable to save nucleus: "+e.getMessage());
				for(StackTraceElement el : e.getStackTrace()){
					IJ.log(el.toString());
				}
				
			} finally{
				output.close();
			}

		} catch(Exception e){
			IJ.log("    Error in saving: "+e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		}
	}
}

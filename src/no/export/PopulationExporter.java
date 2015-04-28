package no.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import no.collections.INuclearCollection;
import no.utility.Logger;

public class PopulationExporter {
	
	private static Logger logger;

	public static boolean savePopulation(INuclearCollection collection){

		logger = new Logger(collection.getDebugFile(), "PopulationExporter");

		try{

			File saveFile = new File(collection.getOutputFolder()+File.separator+collection.getType()+".sav");
			if(saveFile.exists()){
				saveFile.delete();
			}
			logger.log("Saving to "+saveFile.getAbsolutePath());

			try{
				//use buffering
				OutputStream file = new FileOutputStream(saveFile);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutputStream output = new ObjectOutputStream(buffer);

				try{

					output.writeObject(collection.getFolder());
					output.writeObject(collection.getOutputFolderName());
					output.writeObject(collection.getType());
					output.writeObject(collection.getNuclei());

					logger.log("Save complete");

				} catch(IOException e){
					logger.log("    Unable to save nuclei: "+e.getMessage(), Logger.ERROR);
					for(StackTraceElement el : e.getStackTrace()){
						logger.log(el.toString(), Logger.STACK);
					}

				} finally{
					output.close();
					buffer.close();
					file.close();
				}

			} catch(Exception e){
				logger.log("Error saving: "+e.getMessage(), Logger.ERROR);
				for(StackTraceElement el : e.getStackTrace()){
					logger.log(el.toString(), Logger.STACK);
				}
			}
		} catch(Exception e){
			logger.log("Error saving: "+e.getMessage(), Logger.ERROR);
			return false;
		}
		return true;
	}
}

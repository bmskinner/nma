package no.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import no.analysis.AnalysisDataset;
import no.collections.NucleusCollection;
import no.utility.Logger;

public class PopulationExporter {
	
	private static Logger logger;

	public static boolean savePopulation(NucleusCollection collection){

		logger = new Logger(collection.getDebugFile(), "PopulationExporter");

		try{

			// Since we're creating a save format, go with nmb: Nuclear Morphology Binary
			File saveFile = new File(collection.getOutputFolder()+File.separator+collection.getType()+".nmb");
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

					output.writeObject(collection);
			

					logger.log("Save complete");

				} catch(IOException e){
					logger.log("    Unable to save nuclei: "+e.getMessage(), Logger.ERROR);
					for(StackTraceElement el : e.getStackTrace()){
						logger.log(el.toString(), Logger.STACK);
					}
					throw new Exception("Individual nucleus error: "+e.getMessage());

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
				return false;
			}
			
		} catch(Exception e){
			logger.log("Error saving: "+e.getMessage(), Logger.ERROR);
			return false;
		}
		return true;
	}
	
	public static boolean saveAnalysisDataset(AnalysisDataset dataset, File saveFile){
		logger = new Logger(dataset.getDebugFile(), "PopulationExporter");

		try{
			// Since we're creating a save format, go with nmd: Nuclear Morphology Dataset
			logger.log("Saving dataset to "+saveFile.getAbsolutePath());

			try{
				//use buffering
				OutputStream file = new FileOutputStream(saveFile);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutputStream output = new ObjectOutputStream(buffer);

				try{

					output.writeObject(dataset);
					logger.log("Save complete");

				} catch(IOException e){
					logger.log("Unable to save dataset: "+e.getMessage(), Logger.ERROR);
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
				return false;
			}
			
		} catch(Exception e){
			logger.log("Error saving: "+e.getMessage(), Logger.ERROR);
			return false;
		}
		return true;
	}
	
	public static boolean saveAnalysisDataset(AnalysisDataset dataset){

		logger = new Logger(dataset.getDebugFile(), "PopulationExporter");

		try{

			// Since we're creating a save format, go with nmb: Nuclear Morphology Binary
			File saveFile = dataset.getSavePath();
			if(saveFile.exists()){
				saveFile.delete();
			}
			saveAnalysisDataset(dataset, saveFile);
						
		} catch(Exception e){
			logger.log("Error saving: "+e.getMessage(), Logger.ERROR);
			return false;
		}
		return true;
	}
}

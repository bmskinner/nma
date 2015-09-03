package no.export;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import utility.Constants;
import utility.Logger;
import no.analysis.AnalysisDataset;
import no.collections.CellCollection;
import no.nuclei.Nucleus;

public class PopulationExporter {
	
	private static Logger logger;

	public static boolean savePopulation(CellCollection collection){

		logger = new Logger(collection.getDebugFile(), "PopulationExporter");

		try{

			
			File saveFile = new File(collection.getOutputFolder()+File.separator+collection.getType()+Constants.SAVE_FILE_EXTENSION);
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
					logger.error("Unable to save nuclei", e);
					throw new Exception("Individual nucleus error: "+e.getMessage());

				} finally{
					output.close();
					buffer.close();
					file.close();
				}

			} catch(Exception e){
				logger.error("Error saving", e);
				return false;
			}
			
		} catch(Exception e){
			logger.error("Error saving", e);
			return false;
		}
		return true;
	}
	
	/**
	 * Save the given dataset to the given file
	 * @param dataset the dataset
	 * @param saveFile the file to save as
	 * @return
	 */
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
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Save the given dataset to it's preferred save path
	 * @param dataset the dataset
	 * @return ok or not
	 */
	public static boolean saveAnalysisDataset(AnalysisDataset dataset){

		logger = new Logger(dataset.getDebugFile(), "PopulationExporter");

		try{

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
	
	public static boolean extractNucleiToFolder(AnalysisDataset dataset, File exportFolder){
		logger = new Logger(dataset.getDebugFile(), "PopulationExporter");

		try{

			logger.log("Extracting nuclei to "+exportFolder.getAbsolutePath());

			for(Nucleus n : dataset.getCollection().getNuclei()){

				// get the path to the enlarged image
				File imagePath = new File(n.getEnlargedImagePath());

				// trim the name back to image name and number
				String imageName = n.getImageName();
				if(imageName.endsWith(".tiff")){
					imageName = imageName.replace(".tiff", "");
				}
				
				File newPath = new File(exportFolder+File.separator+n.getImageName()+"-"+n.getNucleusNumber()+".tiff");

				if(imagePath.exists()){		
					
					copyFile(imagePath, newPath);

				}
			}

		}catch(Exception e){
			logger.error("Error extracting", e);
			return false;
		}
		return true;

	}
	
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
}

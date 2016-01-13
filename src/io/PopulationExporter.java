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


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import utility.Constants;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import components.CellCollection;
import components.nuclei.Nucleus;

public class PopulationExporter extends AnalysisWorker {
	
	private File saveFile = null;
	private boolean useHDF5 = false;
	
	public PopulationExporter(AnalysisDataset dataset, File saveFile, Logger programLogger) {
		super(dataset, programLogger);		
		this.saveFile = saveFile;
//		this.useHDF5 = useHDF5;
		this.setProgressTotal(1);
	}
	
	public PopulationExporter(AnalysisDataset dataset, Logger programLogger) {
		super(dataset, programLogger);
		CellCollection collection = dataset.getCollection();
		this.saveFile = new File(collection.getOutputFolder()+File.separator+collection.getType()+Constants.SAVE_FILE_EXTENSION);
		this.setProgressTotal(1);
//		this.useHDF5 = useHDF5;
//		this.setProgress(0);
		
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {
//		publish(0);
		
		if(useHDF5){
			saveAnalysisDatasetToHDF5(getDataset());
			return true;
		} else {

			if(saveAnalysisDataset(getDataset(), saveFile)){
//				publish(1);
				log(Level.FINEST, "Save was sucessful");
				return true;
			} else{
				log(Level.WARNING, "Save was unsucessful");
				return false;
			}
		}
		
	}
	
	/**
	 * Save the given dataset to the given file
	 * @param dataset the dataset
	 * @param saveFile the file to save as
	 * @return
	 */
	public static boolean saveAnalysisDataset(AnalysisDataset dataset, File saveFile){

		try{
			// Since we're creating a save format, go with nmd: Nuclear Morphology Dataset
			log(Level.INFO, "Saving dataset to "+saveFile.getAbsolutePath());

			try{
				//use buffering
				OutputStream file = new FileOutputStream(saveFile);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutputStream output = new ObjectOutputStream(buffer);

				try{

					output.writeObject(dataset);
					log(Level.INFO, "Save complete");

				} catch(IOException e){
					logError("Unable to save dataset", e);

				} finally{
					output.close();
					buffer.close();
					file.close();
				}

			} catch(Exception e){
				logError("Error saving dataset", e);
				return false;
			}
			
		} catch(Exception e){
			logError("Error saving dataset", e);
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

		File saveFile = dataset.getSavePath();
		if(saveFile.exists()){
			saveFile.delete();
		}

		return saveAnalysisDataset(dataset, saveFile);

	}
	
	public static boolean extractNucleiToFolder(AnalysisDataset dataset, File exportFolder){

		try{

			log(Level.INFO, "Extracting nuclei to "+exportFolder.getAbsolutePath());

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
			logError("Error extracting nuclei", e);
			return false;
		}
		return true;

	}
	
	/**
	 * Directly copy the source file to the destination file
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
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
	
	public static void saveAnalysisDatasetToHDF5(AnalysisDataset dataset){

		/* TODO: the basic approach show below does not work;
		 * The HDF5 writer cannot handle the Maps within a Java object.
		 * Each object will need to be unpacked.
		 */
		
		
//		File saveFile = new File(dataset.getSavePath().getAbsolutePath()+".hdf5");
//		if(saveFile.exists()){
//			saveFile.delete();
//		}
//		IHDF5Writer writer = HDF5FactoryProvider.get().open(saveFile);

//		HDF5CompoundType<AnalysisDataset> type = writer.compound().getInferredAnonType(AnalysisDataset.class);

//		writer.compound().write("ds_name", type, dataset);
//		writer.close();


	} 


}

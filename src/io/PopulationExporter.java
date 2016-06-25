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
package io;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;

public class PopulationExporter extends AnalysisWorker {
	
	private File   saveFile = null;
//	private boolean useHDF5 = false;
	
	public PopulationExporter(AnalysisDataset dataset, File saveFile) {
		super(dataset);		
		this.saveFile = saveFile;
	}
		
	@Override
	protected Boolean doInBackground() {
		
		try{
		
//			if(useHDF5){
//				saveAnalysisDatasetToHDF5(getDataset());
//				return true;
//			} 

			if(saveAnalysisDataset(getDataset(), saveFile)){
				finest("Save was sucessful");        
				return true;
				
			} else{
				warn("Save was unsucessful");
				return false;
			}
		
		} catch(Exception e){
			error("Unable to save dataset", e);
			return false;
		}
		
		
	}
	
	/**
	 * Save the given dataset to the given file
	 * @param dataset the dataset
	 * @param saveFile the file to save as
	 * @return
	 */
	public boolean saveAnalysisDataset(AnalysisDataset dataset, File saveFile){

		boolean ok = true;
		try{
			// Since we're creating a save format, go with nmd: Nuclear Morphology Dataset
			fine("Saving dataset to "+saveFile.getAbsolutePath());

			//use buffering
			OutputStream file         = new FileOutputStream(saveFile);
			OutputStream buffer       = new BufferedOutputStream(file);
			ObjectOutputStream output = new ObjectOutputStream(buffer);

			try{

				output.writeObject(dataset);

			} catch(IOException e){
				error("IO error saving dataset", e);
				ok =  false;
			} catch(Exception e1){
				error("Unexpected exception saving dataset to: "+saveFile.getAbsolutePath(), e1);
				ok =  false;
			} catch(StackOverflowError e){
				error("StackOverflow saving dataset to: "+saveFile.getAbsolutePath(), e);
				ok =  false;
			} finally{
				output.close();
				buffer.close();
				file.close();
			}
			
			// This line is not always reached when saving multiple datasets
			fine("Save complete");
						
			if(!ok){
				return false;
			}
			
			
			
		} catch(FileNotFoundException e){
			warn("File not found when saving dataset");
			return false;
		} catch (IOException e2) {
			error("IO error saving dataset", e2);
			return false;
		}
		return true;
	}
	
	/**
	 * Save the given dataset to it's preferred save path
	 * @param dataset the dataset
	 * @return ok or not
	 */
	public boolean saveAnalysisDataset(AnalysisDataset dataset){

		File saveFile = dataset.getSavePath();
		if(saveFile.exists()){
			saveFile.delete();
		}

		return saveAnalysisDataset(dataset, saveFile);

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

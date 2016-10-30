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

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Set;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import components.active.DefaultAnalysisDataset;
import components.generic.IPoint;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.NucleusType;
import io.DatasetConverter.DatasetConversionException;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.IAnalysisDataset;
import utility.Constants;
import utility.Version;

public class PopulationImportWorker extends AnalysisWorker {
	
	private final File file;
	private IAnalysisDataset dataset = null; // the active dataset of an AnalysisWorker is private and immutable, so have a new field here
	
	public PopulationImportWorker(final File f){
		super(null);
		
		if(f==null){
			throw new IllegalArgumentException("File cannot be null");
		}
		
		if( ! f.exists()){
			throw new IllegalArgumentException("File does not exist");
		}
		
		if( f.isDirectory()){
			throw new IllegalArgumentException("File is a directory");
		}
		
		if( ! f.isFile()){
			throw new IllegalArgumentException("File has non-normal attributes or is not a file");
		}
		
		if( ! f.getName().endsWith(Constants.SAVE_FILE_EXTENSION)){
			throw new IllegalArgumentException("File is not nmd format or has been renamed");
		}

		
		this.file = f;
		this.setProgressTotal(1);
	}
	
	public IAnalysisDataset getLoadedDataset() throws UnloadableDatasetException {
		
		if(this.dataset==null){
			throw new UnloadableDatasetException("No dataset loaded");
		}
		return this.dataset;
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {
		finest("Beginning background work");
		fireCooldown();
		try {
		
			
			try {
				dataset = readDataset(file);
			} catch (UnloadableDatasetException e){
				warn(e.getMessage());
				warn("Dataset version may be too old");
				fine("Error reading dataset", e);
				return false;
			}
						
			fine("Read dataset");
			
			Version v = dataset.getVersion();
	
			
			if(checkVersion( v )){

				fine("Version check OK");
				dataset.setRoot(true);

				
				// update the log file to the same folder as the dataset
				File logFile = new File(file.getParent()
						+File.separator
						+file.getName().replace(Constants.SAVE_FILE_EXTENSION, Constants.LOG_FILE_EXTENSION));
				
				dataset.setDebugFile(logFile);
				fine("Updated log file location");

				// If rodent sperm, check if the TOP_VERTICAL and BOTTOM_VERTICAL 
				// points have been set, and if not, add them
				if(dataset.getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
					
					if(! dataset.getCollection()
							.getProfileCollection()
							.hasBorderTag(Tag.TOP_VERTICAL)  ){
						
						fine("TOP_ and BOTTOM_VERTICAL not assigned; calculating");
						dataset.getCollection().getProfileManager().calculateTopAndBottomVerticals();
						fine("Calculating TOP and BOTTOM for child datasets");
						for(IAnalysisDataset child : dataset.getAllChildDatasets()){
							child.getCollection().getProfileManager().calculateTopAndBottomVerticals();
						}
						
					}
					
				}
				
				// Correct signal border locations from older versions for all imported datasets
				if(v.isOlderThan( new Version(1, 13, 2))){	
					updateSignals();
				}
				
				

				// Generate vertically rotated nuclei for all imported datasets
				dataset.getCollection().updateVerticalNuclei();
				for(IAnalysisDataset child : dataset.getAllChildDatasets()){
					child.getCollection().updateVerticalNuclei();
				}
				
				
				return true;
				
			} else {
				warn("Unable to open dataset version: "+ dataset.getVersion());
				return false;
			}
			
			
		} catch (IllegalArgumentException e){
			warn("Unable to open file: "+e.getMessage());
			return false;
		}
	}
	
	private void updateSignals(){
		log("Updating signal positions for old dataset");
		updateSignalPositions(dataset);
		for(IAnalysisDataset child : dataset.getAllChildDatasets()){
			updateSignalPositions(child);
		}
		
		if(dataset.hasMergeSources()){
			for(IAnalysisDataset source : dataset.getAllMergeSources()){
				updateSignalPositions(source);
			}
		}
	}
	

	/**
	 * In older versions of the program, signal border positions were stored differently 
	 * to the CoM. This needs correcting, as it causes errors in rotating signals.
	 * The CoM is relative to the nucleus, but the border list is relative to the image.
	 * Adjust the border to bring it back in line with the CoM.
	 * @param dataset
	 */
	private void updateSignalPositions(IAnalysisDataset dataset){
		dataset.getCollection().getNuclei().parallelStream().forEach( n -> {
			
			if(n.getSignalCollection().hasSignal()){
				
				for(UUID id : n.getSignalCollection().getSignalGroupIDs()){
					
					n.getSignalCollection().getSignals(id).parallelStream().forEach( s -> {
						
						if( ! s.containsPoint(s.getCentreOfMass())){
						
							for(int i=0; i<s.getBorderLength();i++){
								IPoint offset = s.getBorderPoint(i).offset(-n.getPosition()[0], -n.getPosition()[1]);
//								s.updateBorderPoint(i, offset);
							}
						}
						
					});
				}
				
			}
			
		});
	}

	
	/**
	 * Check a version string to see if the program will be able to open a 
	 * dataset. The major version must be the same, while the revision of the
	 * dataset must be equal to or greater than the program revision. Bugfixing
	 * versions are not checked for.
	 * @param version
	 * @return a pass or fail
	 */
	public boolean checkVersion(Version version){

		
		if(version==null){ // allow for debugging, but warn
			warn("No version info found: functions may not work as expected");
			return true;
		}
				
		
		// major version MUST be the same
		if(version.getMajor()!=Constants.VERSION_MAJOR){
			warn("Major version difference");
			return false;
		}
		// dataset revision should be equal or greater to program
		if(version.getMinor()<Constants.VERSION_MINOR){
			warn("Dataset was created with an older version of the program");
			warn("Some functionality may not work as expected");
		}
		return true;
	}
	
	private IAnalysisDataset readDataset(File inputFile) throws UnloadableDatasetException  {

		
		finest("Checking input file");
		

		IAnalysisDataset dataset = null;
		FileInputStream fis     = null;
		ObjectInputStream ois   = null;
				
		try {
			
			finest("Attempting to read file");
			
			fis = new FileInputStream(inputFile.getAbsolutePath());
			finest("Created file stream");
			ois = new ObjectInputStream(fis);
			finest("Created object stream");
			
			
			finest("Attempting to read object");
			dataset = (IAnalysisDataset) ois.readObject();
			finest("Read object as analysis dataset");	
			
			
		} catch(NullPointerException e1){
			fine("NPE Error reading dataset", e1);
			throw new UnloadableDatasetException("Cannot load dataset due to "+e1.getClass().getSimpleName(), e1);
			
		} catch(Exception e1){
			fine("Error reading dataset", e1);
			throw new UnloadableDatasetException("Cannot load dataset due to "+e1.getClass().getSimpleName(), e1);
			
		} catch(StackOverflowError e){
			
			throw new UnloadableDatasetException("Stack overflow loading dataset", e);
			
		} finally {
			finest("Closing file stream");
			try {
				ois.close();
				fis.close();
			} catch(Exception e){
				fine("Error closing file stream", e);
				throw new UnloadableDatasetException("Cannot load dataset due to "+e.getClass().getSimpleName(), e);
			}
		}
		
		// Replace existing save file path with the path to the file that has been opened
		finest("Checking file path");
		if(!dataset.getSavePath().equals(inputFile)){
			updateSavePath(inputFile, dataset);
		}
		
		// convert old files if needed
		
		if(dataset instanceof AnalysisDataset){
			
			log("Old style dataset detected");
			
			try {

				DatasetConverter conv = new DatasetConverter(dataset);

				IAnalysisDataset converted = conv.convert();
				
				dataset = converted;

				log("Conversion successful");
			} catch (DatasetConversionException e){
				warn("Unable to convert to new format.");
				warn("Displaying as old format.");
				fine("Error in converter", e);
			}
		}
		
//		if(dataset instanceof DefaultAnalysisDataset){
//			
//			log("New style dataset detected");
//			
//			for(IAnalysisDataset child : dataset.getAllChildDatasets()){
//				child.getCollection().createProfileCollection();
////				child.getCollection().getProfileManager().recalculateProfileAggregates();
//			}
//		}
		
		
		
		finest("Returning opened dataset");
		return dataset;
	}
	
	/**
	 * Check if the image folders are present in the correct relative directories
	 * If so, update the CellCollection image paths
	 * should be /ImageDir/AnalysisDir/dataset.nmd
	 * @param inputFile the file being opened
	 * @param dataset the dataset being opened
	 */
	private void updateSavePath(File inputFile, IAnalysisDataset dataset) {
		
		fine("File path has changed: attempting to relocate images");
		
		
		/*
		 * The expected folder structure for an analysis is as follows:
		 * 
		 *   -- ImageDir/
		 *    | -- DateTimeDir/
		 *    |  | -- dataset.nmd
		 *    |  | -- dataset.log
		 *    | -- Image1.tiff
		 *    | -- ImageN.tiff
		 * 
		 */

		dataset.setSavePath(inputFile);
		
		if(!dataset.hasMergeSources()){

			// This should be /ImageDir/DateTimeDir/
			File expectedAnalysisDirectory = inputFile.getParentFile();

			// This should be /ImageDir/
			File expectedImageDirectory = expectedAnalysisDirectory.getParentFile();
			
			try {
				dataset.updateSourceImageDirectory(expectedImageDirectory);
			} catch (IllegalArgumentException e){
				warn("Cannot update save path: "+e.getMessage());
			}
			
			fine("Checking if signal folders need updating");
			updateSignalFolders(dataset);

		}else {
			warn("Dataset is a merge");
			warn("Unable to find single source image directory");
		}
	}
	
	private void updateSignalFolders(IAnalysisDataset dataset){
		if(dataset.getCollection().getSignalManager().hasSignals()){
			fine("Updating signal locations");
			
			Set<UUID> signalGroups = dataset.getCollection().getSignalGroupIDs();
			
			for(UUID signalID : signalGroups){
				
				// Get the new folder of images
				File newsignalDir = getSignalDirectory(dataset, signalID);
				
				if(newsignalDir != null){

					fine("Updating signal group to "+newsignalDir);

					// Update the folder
					dataset.getCollection().getSignalManager().updateSignalSourceFolder(signalID, newsignalDir);

					for(IAnalysisDataset child : dataset.getAllChildDatasets()){
						child.getCollection().getSignalManager().updateSignalSourceFolder(signalID, newsignalDir);

					}
				} else {
					warn("Cannot update signal folder for group");
				}

			}
		}
	}
	
	private File getSignalDirectory(IAnalysisDataset dataset, UUID signalID){
		
		String signalName = dataset.getCollection().getSignalGroup(signalID).getGroupName();
		
		JOptionPane.showMessageDialog(null, "Choose the folder with images for signal group "+signalName);
		
		JFileChooser fc = new JFileChooser(dataset.getSavePath().getParentFile());
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int returnVal = fc.showOpenDialog(fc);
		if (returnVal != 0)	{
			return null;
		}
		
		File file = fc.getSelectedFile();

		fine("Selected folder: "+file.getAbsolutePath());
		return file;
	}
	
	public class UnloadableDatasetException extends Exception {
		private static final long serialVersionUID = 1L;
		public UnloadableDatasetException() { super(); }
		public UnloadableDatasetException(String message) { super(message); }
		public UnloadableDatasetException(String message, Throwable cause) { super(message, cause); }
		public UnloadableDatasetException(Throwable cause) { super(cause); }
	}
	


}

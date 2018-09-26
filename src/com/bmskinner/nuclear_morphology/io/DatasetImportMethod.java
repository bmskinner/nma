/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.components.AnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.generic.Version.UnsupportedVersionException;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.DatasetConverter.DatasetConversionException;
import com.bmskinner.nuclear_morphology.io.Io.Importer;

/**
 * Method to read a dataset from file
 * 
 * @author ben
 * @since 1.13.4
 *
 */

@SuppressWarnings("deprecation")
public class DatasetImportMethod extends AbstractAnalysisMethod implements Importer {

    private final File       file;
    private IAnalysisDataset dataset      = null;
    private boolean          wasConverted = false;
    public static final int WAS_CONVERTED_BOOL = 0;
    
    /**
     * Store a map of signal image locations if necessary 
     */
    private Optional<Map<UUID, File>> signalFileMap = Optional.empty();

    /**
     * Construct with a file to be read
     * 
     * @param f the saved dataset file
     */
    public DatasetImportMethod(final File f) {
        super();

        if (!Importer.isSuitableImportFile(f))
            throw new IllegalArgumentException(INVALID_FILE_ERROR);

        if (! (f.getName().endsWith(SAVE_FILE_EXTENSION) || f.getName().endsWith(BACKUP_FILE_EXTENSION)) )
            throw new IllegalArgumentException("File is not nmd or bak format or has been renamed");

        this.file = f;
    }
        
    /**
     * Call with an existing map of signal ids to dairectories of images. Designed for unit
     * testing.
     * @param f
     * @param signalFiles a map of signal group to folder of signals
     */
    public DatasetImportMethod(final File f, final Map<UUID, File> signalFiles) {
        this(f);
        signalFileMap = Optional.of(signalFiles);
    }

    @Override
    public IAnalysisResult call() throws Exception {

    	try{ 
    		run(); 
    	} catch (UnsupportedVersionException e) {
    		throw(e);
    	}  

        if (dataset == null)
            throw new UnloadableDatasetException(String.format("Could not load file '%s'", file.getAbsolutePath()));

        DefaultAnalysisResult r = new DefaultAnalysisResult(dataset);
        r.setBoolean(WAS_CONVERTED_BOOL, wasConverted);
        return r;
    }

    private void run() throws Exception {

        try {
            // Clean up old log lock files. Legacy.
            cleanLockFilesInDir(file.getParentFile());

            try {
                // Deserialise whatever is in the file
                dataset = readDataset(file);
                fireIndeterminateState();
            } catch (UnsupportedVersionException e) {
            	warn("Version "+e.getMessage()+" not supported");
            	if(e.getDetectedVersion().isNewerThan(Version.currentVersion()))
            		warn(String.format("Dataset version %s is from a newer software version; upgrade to view", e.getDetectedVersion()));
            	if(e.getDetectedVersion().isOlderThan(Version.currentVersion()))
            		warn(String.format("Dataset version %s is too old to read in this software", e.getDetectedVersion()));
            	throw(e);
                
            } catch (UnloadableDatasetException e) {
                warn(e.getMessage());
                stack("Error reading dataset", e);
            }
            
            if(dataset==null)
                return; // Exception will be thrown in call() method
            
            updateDataset();
            
            validateDataset();

        } catch (IllegalArgumentException e) {
            warn("Unable to open file '" + file.getAbsolutePath() + "': " + e.getMessage());
            stack("Error opening file", e);
        }
    }
    
    /**
     * Handle any upgrades or conversions needed
     */
    private void updateDataset(){
        // Replace existing save file path with the path to the file that has
        // been opened
        if (!dataset.getSavePath().equals(file)) {
            fine("Old save path: " + dataset.getSavePath().getAbsolutePath());
            fine("Input file: " + file.getAbsolutePath());
            updateSavePath(file, dataset);
        }

        DatasetConverter conv = new DatasetConverter(dataset);

        // convert old files if needed
        if (GlobalOptions.getInstance().isConvertDatasets()) {        	
             try {
            	 dataset = conv.convert();
            	 wasConverted = conv.shouldSave();
             } catch (DatasetConversionException e) {
                 warn("Unable to convert to new format.");
                 warn("Displaying as old format.");
                 stack("Error in converter", e);
             }
        }

        Version v = dataset.getVersion();

        if (Version.versionIsSupported(v)) {
            dataset.setRoot(true);

            File exportFolder = dataset.getCollection().getOutputFolder();
            if (!exportFolder.exists()) {
                // the nmd has probably been copied from another computer
                // update to the current file path
                exportFolder = file.getParentFile();
                dataset.getCollection().setOutputFolder(exportFolder);
                fine("Updated output folder to " + exportFolder);
            }

//            File logFile = null;
//            if(file.getName().endsWith(SAVE_FILE_EXTENSION))
//                logFile = Importer.replaceFileExtension(file, SAVE_FILE_EXTENSION, LOG_FILE_EXTENSION);
            
            if(file.getName().endsWith(BACKUP_FILE_EXTENSION)){
//                logFile = Importer.replaceFileExtension(file, BACKUP_FILE_EXTENSION, LOG_FILE_EXTENSION);
                dataset.setSavePath(Importer.replaceFileExtension(file, BACKUP_FILE_EXTENSION, SAVE_FILE_EXTENSION));
            }

            // If rodent sperm, check if the TOP_VERTICAL and
            // BOTTOM_VERTICAL
            // points have been set, and if not, add them
            if (dataset.getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)) {

                if (!dataset.getCollection().getProfileCollection().hasBorderTag(Tag.TOP_VERTICAL)) {
                    dataset.getCollection().getProfileManager().calculateTopAndBottomVerticals();
                    for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
                        child.getCollection().getProfileManager().calculateTopAndBottomVerticals();
                    }
                }
            }

            // Generate vertically rotated nuclei for all imported datasets
            try {
                dataset.getCollection().updateVerticalNuclei();
                for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
                    child.getCollection().updateVerticalNuclei();
                }

            } catch (Exception e) {
                warn("Error updating vertical nuclei");
                stack("Error updating vertical nuclei", e);
            }

        } else {
            warn("Unable to open dataset version: " + dataset.getVersion());
        }
    }
    
    /**
     * Check the dataset has valid segments and profiles
     */
    private void validateDataset() {
    	 // Check the validity of the loaded dataset
        DatasetValidator dv = new DatasetValidator();
        if (!dv.validate(dataset)) {
            for (String s : dv.getErrors()) {
                warn(s);
            }
            warn("Dataset is corrupted");
            warn("Saving child dataset info");
            new CellFileExporter().exportCellLocations(dataset);

            warn("Curated cells saved");
            warn("Redetect cells and import the ." + Importer.LOC_FILE_EXTENSION + " file");
        }
    }

    /**
     * Older version of the program did not always close log handlers properly,
     * so lck files may have proliferated. Kill them with fire.
     * 
     * @param dir the directory to clean
     */
    private void cleanLockFilesInDir(File dir) {

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                return (lowercaseName.endsWith(".lck"));
            }
        };

        File[] files = dir.listFiles(filter);

        if (files == null)
            return;

        for (File lockFile : files){
            lockFile.delete();
        }
    }

    private IAnalysisDataset readDataset(File inputFile) throws UnloadableDatasetException, UnsupportedVersionException {
        IAnalysisDataset dataset = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = new FileInputStream(inputFile.getAbsolutePath());
            
            CountedInputStream cis = new CountedInputStream(fis);
            BufferedInputStream bis = new BufferedInputStream(cis);
            
            cis.addCountListener( (l) ->{
            	fireProgressEvent(l);
            });
            
            // This was needed when classes changed packages between versions
            ois = new PackageReplacementObjectInputStream(bis);
            dataset = (IAnalysisDataset) ois.readObject();


        } catch (UnsupportedVersionException e1) {
                
        	throw(e1);

        } catch (ClassNotFoundException e1) {
        	warn("Missing class: "+e1.getMessage());
        	stack("Class not found reading '" + file.getAbsolutePath() + "': ", e1);
        	throw new UnloadableDatasetException("Missing class "+e1.getMessage());
        } catch (NullPointerException e1) {
            // holdover from deserialisation woes when migrating packages
            stack("NPE Error reading '" + file.getAbsolutePath() + "': ", e1);
            throw new UnloadableDatasetException("Cannot load dataset due to " + e1.getClass().getSimpleName(), e1);

        } catch (OptionalDataException e1) {
            /*
             * Exception indicating the failure of an object read operation due
             * to unread primitive data, or the end of data belonging to a
             * serialized object in the stream.
             */
            if (e1.eof) {

                /*
                 * An attempt was made to read past the end of data consumable
                 * by a class-defined readObject or readExternal method. In this
                 * case, the OptionalDataException's eof field is set to true
                 */
                stack("Unexpected end of data '" + file.getAbsolutePath() + "'", e1);
            } else {

                /*
                 * An attempt was made to read an object when the next element
                 * in the stream is primitive data. In this case, the
                 * OptionalDataException's length field is set to the number of
                 * bytes of primitive data immediately readable from the stream,
                 * and the eof field is set to false
                 */
                stack(file.getAbsolutePath() + ": " + e1.length + " remaining in buffer", e1);
            }
            throw new UnloadableDatasetException(
                    "Cannot load '" + file.getAbsolutePath() + "' due to unexpected end of file", e1);

        } catch (Exception e1) {
            // Is there anything else left that could go wrong? Probably.
            stack("Error reading '" + file.getAbsolutePath() + "'", e1);
            throw new UnloadableDatasetException(
                    "Cannot load '" + file.getAbsolutePath() + "' due to " + e1.getClass().getSimpleName(), e1);

        } catch (StackOverflowError e) {
            // From when a recursive loop was entered building segments.
            throw new UnloadableDatasetException("Stack overflow loading '" + file.getAbsolutePath() + "'", e);

        } finally {

            try {
            	if(ois!=null)
            		ois.close();
            	
            	if(fis!=null)
            		fis.close();
            } catch (Exception e) {
                stack("Error closing file stream", e);
                throw new UnloadableDatasetException(
                        "Cannot load '" + file.getAbsolutePath() + "' due to " + e.getClass().getSimpleName(), e);
            }
        }
        return dataset;
    }

//    private IAnalysisDataset upgradeDatasetVersion(IAnalysisDataset dataset) {
//        log("Old format detected");
//
//        try {
//
//            DatasetConverter conv = new DatasetConverter(dataset);
//
//            IAnalysisDataset converted = conv.convert();
//
//            dataset = converted;
//
//            log("Conversion successful");
//            wasConverted = true;
//        } catch (DatasetConversionException e) {
//            warn("Unable to convert to new format.");
//            warn("Displaying as old format.");
//            stack("Error in converter", e);
//        }
//        return dataset;
//    }

    /**
     * Check if the image folders are present in the correct relative
     * directories If so, update the ICellCollection image paths should be
     * /ImageDir/AnalysisDir/dataset.nmd
     * 
     * @param inputFile the file being opened
     * @param dataset the dataset being opened
     * @param signalFileMap an optional parameter with the mapping of signal groups to files
     */
    private void updateSavePath(@NonNull final File inputFile, @NonNull final IAnalysisDataset dataset) {

        fine("File path has changed: attempting to relocate images");

        // Check if the original image paths are still correct/
        // If not, proceed with the relocate below

        /*
         * The expected folder structure for an analysis is as follows:
         * 
         * -- ImageDir/ 
         * | -- DateTimeDir/ 
         * | | -- dataset.nmd
         * | | -- dataset.log
         * | -- Image1.tiff 
         * | -- ImageN.tiff
         * 
         */

        dataset.setSavePath(inputFile);

        if (dataset.hasMergeSources()) {
        	warn("Dataset is a merge");
            warn("Unable to find single source image directory");
            return;
        }

        // This should be /ImageDir/DateTimeDir/
        File expectedAnalysisDirectory = inputFile.getParentFile();

        // This should be /ImageDir/
        File expectedImageDirectory = expectedAnalysisDirectory.getParentFile();

        try {
        	dataset.updateSourceImageDirectory(expectedImageDirectory);
        } catch (IllegalArgumentException e) {
        	fine("Cannot update image file paths: " + e.getMessage());
        	fine("Nucleus images will not be displayed");
        }

        fine("Checking if signal folders need updating");
        if(!signalFileMap.isPresent()){
        	Map<UUID, File> map = new HashMap<>();
        	for (UUID id : dataset.getCollection().getSignalGroupIDs()) {
        		Optional<ISignalGroup> group = dataset.getCollection().getSignalGroup(id);
        		INuclearSignalOptions signalOptions = dataset.getAnalysisOptions().get().getNuclearSignalOptions(id);
        		if(group.isPresent() && signalOptions.getFolder().exists()) {
        			map.put(id, signalOptions.getFolder());
        		} else {
        			map.put(id, FileSelector.getSignalDirectory(dataset, id));
        		}        		
        	}
        	signalFileMap = Optional.of(map);
        }

        updateSignalFolders(dataset, signalFileMap.get());
    }

    /**
     * Update the source folders for signal groups in a dataset using the given map
     * @param dataset
     * @param newSignalMap
     */
    private void updateSignalFolders(IAnalysisDataset dataset, Map<UUID, File> newSignalMap) {
    	if (!dataset.getCollection().getSignalManager().hasSignals())
    		return;

    	fine("Updating signal locations");
    	Set<UUID> signalGroups = dataset.getCollection().getSignalGroupIDs();

    	for (UUID signalID : signalGroups) {

    		// Get the new folder of images
    		File newsignalDir = newSignalMap.get(signalID);
    		if(newsignalDir== null) {
    			warn("Cannot update signal folder for group");
    			continue;
    		}

    		fine("Updating signal group to " + newsignalDir);
    		// Update the folder
    		dataset.getCollection().getSignalManager().updateSignalSourceFolder(signalID, newsignalDir);
    		dataset.getAnalysisOptions().get().getNuclearSignalOptions(signalID).setFolder(newsignalDir);

    		for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
    			child.getCollection().getSignalManager().updateSignalSourceFolder(signalID, newsignalDir);
    			child.getAnalysisOptions().get().getNuclearSignalOptions(signalID).setFolder(newsignalDir);
    		}
    	}
    }


    public class UnloadableDatasetException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnloadableDatasetException() {
            super();
        }
        public UnloadableDatasetException(String message) {
            super(message);
        }
        public UnloadableDatasetException(String message, Throwable cause) {
            super(message, cause);
        }
        public UnloadableDatasetException(Throwable cause) {
            super(cause);
        }
    }

}

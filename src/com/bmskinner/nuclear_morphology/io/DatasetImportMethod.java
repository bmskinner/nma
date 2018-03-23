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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.AnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.generic.Version.UnsupportedVersionException;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.DatasetConverter.DatasetConversionException;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;

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
    private IAnalysisDataset dataset      = null; // the active dataset of an
                                                  // AnalysisWorker is private
                                                  // and immutable, so have a
                                                  // new field here
    private boolean          wasConverted = false;

    public static final int WAS_CONVERTED_BOOL = 0; // the IAnalysisResult
                                                    // boolean index for
                                                    // conversion state
    
    
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

        if (!Importer.isSuitableImportFile(f)) {
            throw new IllegalArgumentException(INVALID_FILE_ERROR);
        }

        if (!f.getName().endsWith(SAVE_FILE_EXTENSION)) {
            throw new IllegalArgumentException("File is not nmd format or has been renamed");
        }

        this.file = f;
    }
    
    /**
     * Call with an existing map of signal ids to directories of images. Designed for unit
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

        if (dataset == null) {
            throw new UnloadableDatasetException("Could not load file '" + file.getAbsolutePath() + "'");
        }

        DefaultAnalysisResult r = new DefaultAnalysisResult(dataset);
        r.setBoolean(WAS_CONVERTED_BOOL, wasConverted);
        return r;
    }

    private void run() throws Exception {

        try {

            // Clean up old log lock files
        	// Legacy.
            cleanLockFilesInDir(file.getParentFile());

            try {
                dataset = readDataset(file);
            } catch (UnsupportedVersionException e) {
            	warn("Version "+e.getMessage()+" not supported");
            	warn("Dataset is too old");
            	throw(e);
                
            } catch (UnloadableDatasetException e) {
                warn(e.getMessage());
                stack("Error reading dataset", e);
            }

            fine("Read dataset");

            Version v = dataset.getVersion();

            if (checkVersion(v)) {

                fine("Version check OK");
                dataset.setRoot(true);

                File exportFolder = dataset.getCollection().getOutputFolder();
                if (!exportFolder.exists()) {
                    // the nmd has probably been copied from another computer
                    // update to the current file path
                    exportFolder = file.getParentFile();
                    dataset.getCollection().setOutputFolder(exportFolder);
                    log("Updated output folder to " + exportFolder);
                }

                File logFile = Importer.replaceFileExtension(file, SAVE_FILE_EXTENSION, LOG_FILE_EXTENSION);

                dataset.setDebugFile(logFile);
                fine("Updated log file location");

                // If rodent sperm, check if the TOP_VERTICAL and
                // BOTTOM_VERTICAL
                // points have been set, and if not, add them
                if (dataset.getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)) {

                    if (!dataset.getCollection().getProfileCollection().hasBorderTag(Tag.TOP_VERTICAL)) {

                        fine("TOP_ and BOTTOM_VERTICAL not assigned; calculating");
                        dataset.getCollection().getProfileManager().calculateTopAndBottomVerticals();
                        fine("Calculating TOP and BOTTOM for child datasets");
                        for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
                            child.getCollection().getProfileManager().calculateTopAndBottomVerticals();
                        }

                    }
                    fine("Finished calculating verticals");
                }

                // Correct signal border locations from older versions for all
                // imported datasets
                if (v.isOlderThan(Version.v_1_13_2)) {
                    fine("Updating signal locations for pre-1.13.2 dataset");
                    updateSignals();
                }

                // Generate vertically rotated nuclei for all imported datasets

                try {
                    fine("Updating vertical nuclei");
                    dataset.getCollection().updateVerticalNuclei();
                    for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
                        child.getCollection().updateVerticalNuclei();
                    }

                } catch (Exception e) {
                    warn("Error updating vertical nuclei");
                    stack("Error updating vertical nuclei", e);
                }

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

//                    JOptionPane.showMessageDialog(null,
//                            "Corruption in dataset " + dataset.getName() + ".\n"
//                                    + "Curated and remapped cell locations have been saved.\n"
//                                    + "Please redetect nuclei and apply the saved .cell file.");
                }

            } else {
                warn("Unable to open dataset version: " + dataset.getVersion());
            }

        } catch (IllegalArgumentException e) {
            warn("Unable to open file '" + file.getAbsolutePath() + "': " + e.getMessage());
            stack("Error opening file", e);
        }
    }

    /**
     * Older version of the program did not always close log handlers properly,
     * so lck files may have proliferated. Kill them with fire.
     * 
     * @param dir
     *            the directory to clean
     */
    private void cleanLockFilesInDir(File dir) {

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                return (lowercaseName.endsWith(".lck"));
            }
        };

        File[] files = dir.listFiles(filter);

        if (files == null) {
            return;
        }

        for (File lockFile : files) {

            lockFile.delete();

        }

    }

    private void updateSignals() {
        log("Updating signal positions for old dataset");
        updateSignalPositions(dataset);
        for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
            updateSignalPositions(child);
        }

        if (dataset.hasMergeSources()) {
            for (IAnalysisDataset source : dataset.getAllMergeSources()) {
                updateSignalPositions(source);
            }
        }
    }

    /**
     * In older versions of the program, signal border positions were stored
     * differently to the CoM. This needs correcting, as it causes errors in
     * rotating signals. The CoM is relative to the nucleus, but the border list
     * is relative to the image. Adjust the border to bring it back in line with
     * the CoM.
     * 
     * @param dataset
     */
    private void updateSignalPositions(IAnalysisDataset dataset) {
        dataset.getCollection().getNuclei().parallelStream().forEach(n -> {

            if (n.getSignalCollection().hasSignal()) {

                for (UUID id : n.getSignalCollection().getSignalGroupIDs()) {

                    n.getSignalCollection().getSignals(id).parallelStream().forEach(s -> {

                        if (!s.containsPoint(s.getCentreOfMass())) {

                            for (int i = 0; i < s.getBorderLength(); i++) {
                                try {
                                    s.getBorderPoint(i).offset(-n.getPosition()[0], -n.getPosition()[1]);
                                } catch (UnavailableBorderPointException e) {
                                    stack("Could not offset border point", e);
                                }
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
     * 
     * @param version
     * @return a pass or fail
     */
    private boolean checkVersion(Version version) {

        if (version == null) { // allow for debugging, but warn
            warn("No version info found: functions may not work as expected");
            return true;
        }

        // major version MUST be the same
        if (version.getMajor() != Version.VERSION_MAJOR) {
            warn("Major version difference");
            return false;
        }

        // dataset revision should be equal or greater to program
        if (version.getMinor() < Version.VERSION_MINOR) {
            warn("Dataset was created with an older version of the program");
            warn("Some functionality may not work as expected");
        }
        return true;
    }

    private IAnalysisDataset readDataset(File inputFile) throws UnloadableDatasetException, UnsupportedVersionException {

        finest("Checking input file");

        IAnalysisDataset dataset = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {

            finest("Attempting to read file");

            fis = new FileInputStream(inputFile.getAbsolutePath());
            finest("Created file stream");
            
            // This was needed when classes changed packages between versions
            ois = new PackageReplacementObjectInputStream(fis);
            finest("Created object stream");

            dataset = (IAnalysisDataset) ois.readObject();
            finest("Read object as analysis dataset");

        } catch (UnsupportedVersionException e1) {
                
        	throw(e1);

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

        // Replace existing save file path with the path to the file that has
        // been opened
        if (!dataset.getSavePath().equals(inputFile)) {
            log("Old save path: " + dataset.getSavePath().getAbsolutePath());
            log("Input file: " + inputFile.getAbsolutePath());
            updateSavePath(inputFile, dataset);
        }

        // convert old files if needed
        if (GlobalOptions.getInstance().isConvertDatasets()) {
            if (dataset instanceof AnalysisDataset) {
                dataset = upgradeDatasetVersion(dataset);
            }
        }

        finest("Returning opened dataset");
        return dataset;
    }

    private IAnalysisDataset upgradeDatasetVersion(IAnalysisDataset dataset) {
        log("Old format detected");

        try {

            DatasetConverter conv = new DatasetConverter(dataset);

            IAnalysisDataset converted = conv.convert();

            dataset = converted;

            log("Conversion successful");
            wasConverted = true;
        } catch (DatasetConversionException e) {
            warn("Unable to convert to new format.");
            warn("Displaying as old format.");
            stack("Error in converter", e);
        }
        return dataset;
    }

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
         * | | -- dataset.nmd | 
         * | -- dataset.log
         * | -- Image1.tiff 
         * | -- ImageN.tiff
         * 
         */

        dataset.setSavePath(inputFile);

        if (!dataset.hasMergeSources()) {

            // This should be /ImageDir/DateTimeDir/
            File expectedAnalysisDirectory = inputFile.getParentFile();

            // This should be /ImageDir/
            File expectedImageDirectory = expectedAnalysisDirectory.getParentFile();

            try {
                dataset.updateSourceImageDirectory(expectedImageDirectory);
            } catch (IllegalArgumentException e) {
                warn("Cannot update image file paths: " + e.getMessage());
                warn("Nucleus images will not be displayed");
            }

            fine("Checking if signal folders need updating");
            if(!signalFileMap.isPresent()){
                Map<UUID, File> map = new HashMap<>();
                for (UUID id : dataset.getCollection().getSignalGroupIDs()) {
                    map.put(id, FileSelector.getSignalDirectory(dataset, id));
                }
                signalFileMap = Optional.of(map);
            }
            
            updateSignalFolders(dataset, signalFileMap.get());

        } else {
            warn("Dataset is a merge");
            warn("Unable to find single source image directory");
        }
    }

    private void updateSignalFolders(IAnalysisDataset dataset, Map<UUID, File> newsignalMap) {
        if (dataset.getCollection().getSignalManager().hasSignals()) {
            fine("Updating signal locations");

            Set<UUID> signalGroups = dataset.getCollection().getSignalGroupIDs();

            for (UUID signalID : signalGroups) {

                // Get the new folder of images
                File newsignalDir = newsignalMap.get(signalID);

                if (newsignalDir != null) {

                    fine("Updating signal group to " + newsignalDir);

                    // Update the folder
                    dataset.getCollection().getSignalManager().updateSignalSourceFolder(signalID, newsignalDir);

                    for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
                        child.getCollection().getSignalManager().updateSignalSourceFolder(signalID, newsignalDir);

                    }
                } else {
                    warn("Cannot update signal folder for group");
                }

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

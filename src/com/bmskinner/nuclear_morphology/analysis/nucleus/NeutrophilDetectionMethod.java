/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.NeutrophilFinder;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This is a cell detection method for neutrophils, separate from the
 * fluorescence nucleus method. It mostly duplicates the NucleusDetectionMethod
 * though.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class NeutrophilDetectionMethod extends AbstractAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final String spacerString = "---------";

    private final String outputFolder;

    private Finder<Collection<ICell>> finder;

    private final File folder;

    private final IAnalysisOptions analysisOptions;

    private Map<File, ICellCollection> collectionMap = new HashMap<>();

    List<IAnalysisDataset> datasets;

    /**
     * Construct a detector on the given folder, and output the results to the
     * given output folder
     * 
     * @param outputFolder the name of the folder for results
     * @param programLogger the logger to the log panel
     * @param debugFile the dataset log file
     * @param options the options to detect with
     */
    public NeutrophilDetectionMethod(@NonNull String outputFolder, @NonNull IAnalysisOptions options) {
        super();

        if (outputFolder == null || options == null)
            throw new IllegalArgumentException("Must have output folder name and input options");

        this.outputFolder = outputFolder;
        this.analysisOptions = options;
        
        if(!options.getDetectionOptions(IAnalysisOptions.NUCLEUS).isPresent())
        	throw new IllegalArgumentException("Input options does not have nucleus options");
        
        folder = options.getDetectionOptions(IAnalysisOptions.NUCLEUS).get().getFolder();
        finder = new NeutrophilFinder(options);

    }

    @Override
    public IAnalysisResult call() throws Exception {

        run();
        IAnalysisResult r = new DefaultAnalysisResult(datasets);
        return r;
    }

    public void run() {

        try {

            countTotalImagesToAnalyse();

            LOGGER.info("Running neutrophil detector");
            processFolder(analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).get().getFolder());

            LOGGER.fine("Detected nuclei in "
                    + analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).get().getFolder().getAbsolutePath());

            LOGGER.fine("Creating cell collections");

            List<ICellCollection> folderCollection = this.getNucleiCollections();

            // Run the analysis pipeline

            LOGGER.fine("Analysing collections");

            datasets = analysePopulations(folderCollection);

            LOGGER.fine("Analysis complete; return collections");

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error in processing folder", e);
        }

    }

    private void countTotalImagesToAnalyse() {
     LOGGER.info("Calculating number of images to analyse");
    	File folder = analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).get().getFolder();
    	int totalImages = countSuitableImages(folder);
    	fireProgressEvent(new ProgressEvent(this, ProgressEvent.SET_TOTAL_PROGRESS, totalImages));
     LOGGER.info("Analysing " + totalImages + " images");
    }

    public List<IAnalysisDataset> getDatasets() {
        return this.datasets;
    }

    public List<IAnalysisDataset> analysePopulations(List<ICellCollection> folderCollection) {

        LOGGER.info("Creating cell collections");

        List<IAnalysisDataset> result = new ArrayList<IAnalysisDataset>();

        for (ICellCollection collection : folderCollection) {

            IAnalysisDataset dataset = new DefaultAnalysisDataset(collection);
            dataset.setAnalysisOptions(analysisOptions);
            dataset.setRoot(true);

            File folder = collection.getFolder();
            LOGGER.info("Analysing: " + folder.getName());

            try {

                ICellCollection failedNuclei = new DefaultCellCollection(folder, collection.getOutputFolderName(),
                        collection.getName() + " - failed", collection.getNucleusType());

                // LOGGER.info("Filtering collection...");
                // boolean ok = new CollectionFilterer().run(collection,
                // failedNuclei); // put fails into failedNuclei, remove from r
                // if(ok){
                // LOGGER.info("Filtered OK");
                // } else {
                // LOGGER.info("Filtering error");
                // }

                /*
                 * Keep the failed nuclei - they can be manually assessed later
                 */

                if (analysisOptions.isKeepFailedCollections()) {
                    LOGGER.info("Keeping failed nuclei as new collection");
                    IAnalysisDataset failed = new DefaultAnalysisDataset(failedNuclei);
                    IAnalysisOptions failedOptions = OptionsFactory.makeAnalysisOptions(analysisOptions);
                    failedOptions.setNucleusType(NucleusType.ROUND);
                    failed.setAnalysisOptions(failedOptions);
                    failed.setRoot(true);
                    result.add(failed);
                }

                LOGGER.info(spacerString);

                LOGGER.info("Population: " + collection.getName());
                LOGGER.info("Passed: " + collection.size() + " nuclei");
                LOGGER.info("Failed: " + failedNuclei.size() + " nuclei");

                LOGGER.info(spacerString);

                result.add(dataset);

            } catch (Exception e) {
                LOGGER.warning("Cannot create collection: " + e.getMessage());
                LOGGER.log(Loggable.STACK, "Error in nucleus detection", e);
            }

            //

        }
        return result;
    }

    /**
     * Add a NucleusCollection to the group, using the source folder name as a
     * key.
     *
     * @param file
     *            a folder to be analysed
     * @param collection
     *            the collection of nuclei found
     */
    public void addNucleusCollection(File file, ICellCollection collection) {
        this.collectionMap.put(file, collection);
    }

    /**
     * Get the Map of NucleusCollections to the folder from which they came. Any
     * folders with no nuclei are removed before returning.
     *
     * @return a Map of a folder to its nuclei
     */
    public List<ICellCollection> getNucleiCollections() {
        // remove any empty collections before returning

        LOGGER.fine("Getting all collections");

        List<File> toRemove = new ArrayList<File>(0);

        LOGGER.fine("Testing nucleus counts");

        Set<File> keys = collectionMap.keySet();
        for (File key : keys) {
            ICellCollection collection = collectionMap.get(key);
            if (collection.size() == 0) {
                LOGGER.fine("Removing collection " + key.toString());
                toRemove.add(key);
            }
        }

        LOGGER.fine("Got collections to remove");

        Iterator<File> iter = toRemove.iterator();
        while (iter.hasNext()) {
            collectionMap.remove(iter.next());
        }

        LOGGER.fine("Removed collections");

        List<ICellCollection> result = new ArrayList<ICellCollection>();
        for (ICellCollection c : collectionMap.values()) {
            result.add(c);
        }
        return result;

    }

    /**
     * Count the number of images in the given folder that are suitable for
     * analysis. Rcursive over subfolders.
     * 
     * @param folder
     *            the folder to count
     * @return the number of analysable image files
     */
    public static int countSuitableImages(File folder) {
        if (folder == null) {
            throw new IllegalArgumentException("Folder cannot be null");
        }

        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            return 0;
        }

        int result = 0;

        for (File file : listOfFiles) {

            boolean ok = ImageImporter.fileIsImportable(file);

            if (ok) {
                result++;

            } else {
                if (file.isDirectory()) { // recurse over any sub folders
                    result += countSuitableImages(file);
                }
            }
        }
        return result;
    }

    /**
     * Go through the input folder. Check if each file is suitable for analysis,
     * and if so, call the analyser.
     *
     * @param folder
     *            the folder of images to be analysed
     */
    protected void processFolder(File folder) {

        if (folder == null) {
            throw new IllegalArgumentException("Folder cannot be null");
        }

        LOGGER.finest( "Processing folder " + folder.getAbsolutePath());
        File[] arrOfFiles = folder.listFiles();
        if (arrOfFiles == null) {
            return;
        }

        ICellCollection folderCollection = new DefaultCellCollection(folder, outputFolder, folder.getName(),
                analysisOptions.getNucleusType());

        collectionMap.put(folder, folderCollection);

        LOGGER.finest( "Invoking recursive detection task");

        for (File f : arrOfFiles) {
            if (f.isDirectory()) {
                processFolder(f); // recurse over each folder
            } else {
                analyseFile(f, folderCollection);
            }
        }

    } // end function

    protected void analyseFile(File file, ICellCollection collection) {

        LOGGER.finest( "Analysing file " + file.getAbsolutePath());
        boolean ok = ImageImporter.fileIsImportable(file);

        if (!ok) {
            return;
        }

        try {

            // put folder creation here so we don't make folders we won't use
            // (e.g. empty directory analysed)
            makeFolder(folder);

            LOGGER.info("File:  " + file.getName());
            Collection<ICell> cells = finder.findInImage(file);

            if (cells.isEmpty()) {
                LOGGER.info("  No cells detected in image");
            } else {

                for (ICell cell : cells) {
                    collection.addCell(cell);
                    LOGGER.info("  Added nucleus " + cell.getNucleus().getNucleusNumber());
                }
                LOGGER.info("  Added " + cells.size() + " nuclei");
            }

        } catch (Exception e) {
            LOGGER.warning("Error processing file");
            LOGGER.log(Loggable.STACK, "Error in image processing: " + e.getMessage(), e);
        }

        fireProgressEvent();

    }

    /**
     * Create the output folder for the analysis if required
     *
     * @param folder
     *            the folder in which to create the analysis folder
     * @return a File containing the created folder
     */
    protected File makeFolder(File folder) {
        File output = new File(folder.getAbsolutePath() + File.separator + this.outputFolder);
        if (!output.exists()) {
            try {
                output.mkdir();
            } catch (Exception e) {
                LOGGER.log(Loggable.STACK, "Failed to create directory", e);
            }
        }
        return output;
    }

}

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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.AnalysisMethodException;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.FinderDisplayType;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.FluorescentNucleusFinder;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The method for finding nuclei in fluorescence images
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class NucleusDetectionMethod extends AbstractAnalysisMethod {
		
	private static final Logger LOGGER = Logger.getLogger(NucleusDetectionMethod.class.getName());

    private final File outputFolder;

    private final IAnalysisOptions templateOptions;

    /** Map a folder of images to the detected cell collection */
    private Map<File, ICellCollection> collectionGroup = new HashMap<>();

    private final List<IAnalysisDataset> datasets = new ArrayList<>();

    /**
     * Construct a detector on the given folder, and output the results to a new
     * folder in the image directory, with the given name. The source image directory
     * is taken from the nucleus detection options
     * 
     * @param outputFolder the name of the folder to store results
     * @param options the options to detect with
     * @throws AnalysisMethodException 
     */
    public NucleusDetectionMethod(@NonNull String outputFolder, @NonNull IAnalysisOptions options) throws AnalysisMethodException {
        this(new File(options.getDetectionOptions(CellularComponent.NUCLEUS).get()
        		.getString(HashOptions.DETECTION_FOLDER), outputFolder), options);
    }
    
    /**
     * Construct a detector with the given options and output the results to the
     * given output folder
     * 
     * @param outputFolder the folder to save the results into
     * @param options the options to detect with
     * @throws AnalysisMethodException 
     */
    public NucleusDetectionMethod(@NonNull File outputFolder, @NonNull IAnalysisOptions options) throws AnalysisMethodException {
    	// We need the parent of the output folder to exist so 
    	// the folder can be created
    	if(outputFolder.getParentFile()==null || !outputFolder.getParentFile().exists())
    		throw new AnalysisMethodException("Output parent folder does not exist: "+outputFolder.getAbsolutePath());
       
    	Optional<HashOptions> op = options.getDetectionOptions(CellularComponent.NUCLEUS);
    	if(!op.isPresent()){
    		throw new AnalysisMethodException("No nucleus detection options present");
    	}
    	
    	this.outputFolder = outputFolder;
        this.templateOptions = options;
    }

    @Override
    public IAnalysisResult call() throws Exception {
        run();
        return new DefaultAnalysisResult(datasets);
    }

    public void run() throws AnalysisMethodException {
    	int i = getTotalImagesToAnalyse();
    	if(i==0) {
    		throw new AnalysisMethodException("No analysable images");
    	}

    	LOGGER.info("Running nucleus detector");

    	// Existence checked in constructor
    	Optional<HashOptions> op = templateOptions.getDetectionOptions(CellularComponent.NUCLEUS);

    	// Detect the nuclei in the folders selected
    	File filePath = op.get().getFile(HashOptions.DETECTION_FOLDER);
    	processFolder(filePath);

    	LOGGER.fine("Detected nuclei in "+ filePath.getAbsolutePath());

    	if(Thread.interrupted())
    		return;

    	List<IAnalysisDataset> analysedDatasets = analysePopulations();
    	
    	if(analysedDatasets.isEmpty())
    		throw new AnalysisMethodException("No datasets returned");

    	datasets.addAll(analysedDatasets);
    	LOGGER.fine("Nucleus detection complete for "+analysedDatasets.size()+" folders");
    }

    private int getTotalImagesToAnalyse() {

        LOGGER.info("Counting images to analyse");
        Optional<HashOptions> op = templateOptions.getDetectionOptions(CellularComponent.NUCLEUS);
        if(!op.isPresent())
        	return 0;
        
        File folder = new File(op.get().getString(HashOptions.DETECTION_FOLDER));
        int totalImages = countSuitableImages(folder);
        fireUpdateProgressTotalLength(totalImages);
        LOGGER.info(String.format("Analysing %d images", totalImages));
        return totalImages;
    }

    /**
     * Get the datasets identified in this method
     * @return
     */
    public List<IAnalysisDataset> getDatasets() {
        return this.datasets;
    }

    private List<IAnalysisDataset> analysePopulations() {

        LOGGER.fine("Creating cell collections");
        LOGGER.fine(templateOptions.toString());
        List<IAnalysisDataset> foundDatasets = new ArrayList<>();

        for (final Entry<File, ICellCollection> entry : collectionGroup.entrySet()) {
        	// Only keep collections containing nuclei
        	ICellCollection collection = entry.getValue();
        	if(collection.isEmpty())
        		continue;

        	File folder = entry.getKey();
            IAnalysisDataset dataset = new DefaultAnalysisDataset(collection, new File(outputFolder, collection.getName()+Io.SAVE_FILE_EXTENSION));
            
            // Ensure the actual folder of images is set in the analysis options, not a root folder
            IAnalysisOptions datasetOptions = templateOptions.duplicate();
            datasetOptions.getDetectionOptions(CellularComponent.NUCLEUS).get().setString(HashOptions.DETECTION_FOLDER, folder.getAbsolutePath());
            dataset.setAnalysisOptions(datasetOptions);

            LOGGER.info("Analysing " + collection.getName());

            try {
                collection.clear(MeasurementScale.PIXELS);
                collection.clear(MeasurementScale.MICRONS);
                LOGGER.info("Found " + collection.size() + " nuclei");
                foundDatasets.add(dataset);

            } catch (Exception e) {
                LOGGER.warning("Cannot create collection: " + e.getMessage());
                LOGGER.log(Loggable.STACK, "Error in nucleus detection", e);
            }
        }
        return foundDatasets;
    }

    /**
     * Go through the input folder. Check if each file is suitable for analysis,
     * and if so, call the analyser.
     *
     * @param folder the folder of images to be analysed
     */
    protected void processFolder(@NonNull final File folder) {
        LOGGER.fine("Searching "+folder.getAbsolutePath());
        File[] arr = folder.listFiles();
        if (arr == null)
            return;
        if(Thread.interrupted())
            return;

        // Recurse over all folders in the supplied folder
        for (File f : arr) {
            if (f.isDirectory())
                processFolder(f);
        }
        
        if(!containsImageFiles(folder))
        	return;

        ICellCollection fc = new DefaultCellCollection(templateOptions.getRuleSetCollection(), 
        		folder.getName(), UUID.randomUUID());

        collectionGroup.put(folder, fc);

        final Finder<Collection<ICell>> finder = new FluorescentNucleusFinder(templateOptions, FinderDisplayType.PIPELINE);
        finder.addProgressListener(this);

        try {
            final Collection<ICell> cells = finder.findInFolder(folder);
            if (!cells.isEmpty() && !outputFolder.exists()) 
            	outputFolder.mkdir();
            fc.addAll(cells);
            LOGGER.fine((()->"Detected "+cells.size()+" nuclei in "+folder.getAbsolutePath()));
        } catch (ImageImportException e) {
            LOGGER.log(Loggable.STACK, "Error searching folder", e);
        }

    }
    
    /**
     * Test if the given folder has any image files that can be analysed
     * @param folder the folder to test
     * @return
     */
    protected boolean containsImageFiles(@NonNull final File folder) {
    	if(!folder.isDirectory())
    		return false;
    	File[] arr = folder.listFiles();
        if (arr == null)
            return false;
        for(File f: arr)
        	if(ImageImporter.fileIsImportable(f))
        		return true;
        return false;
    }
    
    /**
     * Count the number of images in the given folder that are suitable for
     * analysis. Rcursive over subfolders.
     * 
     * @param folder the folder to count
     * @return the number of analysable image files in this folder or subfolders
     */
    private static int countSuitableImages(@NonNull final File folder) {
        final File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return 0;
        int result = 0;

        for (File file : listOfFiles) {
            boolean ok = ImageImporter.fileIsImportable(file);
            if (ok) {
                result++;
            } else {
                if (file.isDirectory())// recurse over any sub folders
                    result += countSuitableImages(file);
            }
        }
        return result;
    }

}

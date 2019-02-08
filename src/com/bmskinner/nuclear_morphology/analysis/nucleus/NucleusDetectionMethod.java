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
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.FluorescentNucleusFinder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * The method for finding nuclei in fluorescence images
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class NucleusDetectionMethod extends AbstractAnalysisMethod {

    private final File outputFolder;

    private final IAnalysisOptions templateOptions;

    private Map<File, ICellCollection> collectionGroup = new HashMap<>();

    private final List<IAnalysisDataset> datasets = new ArrayList<>();

    /**
     * Construct a detector on the given folder, and output the results to a new
     * folder in the image directory, with the given name
     * 
     * @param outputFolder the name of the folder for results
     * @param options the options to detect with
     */
    public NucleusDetectionMethod(@NonNull String outputFolder, @NonNull IAnalysisOptions options) {
        this.outputFolder = new File(options.getDetectionOptions(IAnalysisOptions.NUCLEUS).get().getFolder(),outputFolder);
        this.templateOptions = options;
    }
    
    /**
     * Construct a detector on the given folder, and output the results to the
     * given output folder
     * 
     * @param outputFolder the folder to save the results into
     * @param options the options to detect with
     */
    public NucleusDetectionMethod(@NonNull File outputFolder, @NonNull IAnalysisOptions options) {
        this.outputFolder = outputFolder;
        this.templateOptions = options;
    }

    @Override
    public IAnalysisResult call() throws Exception {
        run();
        return new DefaultAnalysisResult(datasets);
    }

    public void run() {

        try {
            int i = getTotalImagesToAnalyse();
            if(i==0) return;

            log("Running nucleus detector");
            
            Optional<? extends IDetectionOptions> op = templateOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS);
            if(!op.isPresent()){
            	warn("No nucleus detection options present");
            	return;
            }
            
            // Detect the nuclei in the folders selected
            processFolder(op.get().getFolder());

            fine("Detected nuclei in "+ op.get().getFolder().getAbsolutePath());
            
            if(Thread.interrupted())
                return;

            // Get the collections containing nuclei
            List<ICellCollection> folderCollection = collectionGroup.entrySet().stream()
            		.filter(e->e.getValue().size()>0)
            		.map(e->e.getValue())
            		.collect(Collectors.toList());

            datasets.addAll(analysePopulations(folderCollection));

            fine("Analysis complete; return collections");

        } catch (Exception e) {
            stack("Error processing folder", e);
        }
    }

    private int getTotalImagesToAnalyse() {

        log("Counting images to analyse");
        Optional<? extends IDetectionOptions> op = templateOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS);
        if(!op.isPresent())
        	return 0;
        
        File folder = op.get().getFolder();
        int totalImages = countSuitableImages(folder);
        fireUpdateProgressTotalLength(totalImages);
        log(String.format("Analysing %d images", totalImages));
        return totalImages;
    }

    /**
     * Get the datasets identified in this method
     * @return
     */
    public List<IAnalysisDataset> getDatasets() {
        return this.datasets;
    }

    private List<IAnalysisDataset> analysePopulations(List<ICellCollection> folderCollection) {

        log("Creating cell collections");

        List<IAnalysisDataset> foundDatasets = new ArrayList<>();

        for (final ICellCollection collection : folderCollection) {
        	
        	File folder = collection.getFolder();
            IAnalysisDataset dataset = new DefaultAnalysisDataset(collection);
            
            // Ensure the actual folder of images is set in the analysis options, not a root folder
            IAnalysisOptions datasetOptions = templateOptions.duplicate();
            datasetOptions.getDetectionOptions(CellularComponent.NUCLEUS).get().setFolder(folder);
            dataset.setAnalysisOptions(datasetOptions);
            
            dataset.setRoot(true);
            dataset.setSavePath(new File(outputFolder, dataset.getName()+Io.SAVE_FILE_EXTENSION));

            log("Analysing " + collection.getName());

            try {
                collection.clear(MeasurementScale.PIXELS);
                collection.clear(MeasurementScale.MICRONS);
                log("Found " + collection.size() + " nuclei");
                foundDatasets.add(dataset);

            } catch (Exception e) {
                warn("Cannot create collection: " + e.getMessage());
                stack("Error in nucleus detection", e);
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

        ICellCollection folderCollection = new DefaultCellCollection(folder, outputFolder.getName(), folder.getName(),
                templateOptions.getNucleusType());

        collectionGroup.put(folder, folderCollection);

        final Finder<Collection<ICell>> finder = new FluorescentNucleusFinder(templateOptions);
        finder.addProgressListener(this);

        try {
            final Collection<ICell> cells = finder.findInFolder(folder);
            if (!cells.isEmpty() && !outputFolder.exists()) 
            	outputFolder.mkdir();
            folderCollection.addAll(cells);
            fine("Detected "+cells.size()+" nuclei in "+folder.getAbsolutePath());
        } catch (ImageImportException e) {
            stack("Error searching folder", e);
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

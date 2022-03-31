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
package com.bmskinner.nuclear_morphology.analysis.signals;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImageCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.UncomparableMeshImageException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.signals.SignalManager;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.warping.SignalWarpingRunSettings;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * Warps signals from nuclei in a collection onto a target nucleus using meshes.
 * This is implemented as a SwingWorker that returns the merged overlay of all
 * the signals in the collection
 * 
 * @author ben
 * @since 1.13.6
 *
 */
public class SignalWarper extends SwingWorker<ImageProcessor, Integer> {
	
	private static final Logger LOGGER = Logger.getLogger(SignalWarper.class.getName());
	
	public static final boolean STRAIGHTEN_MESH = true;
	public static final boolean REGULAR_MESH = false;
		
	public static final int DEFAULT_MIN_SIGNAL_THRESHOLD = 0;
    
    /** The options for the analysis */
    private SignalWarpingRunSettings warpingOptions;
            
    /** The number of cell images to be merged */
    private int totalCells;
    
    /** The mesh images are warped onto */
    private final Mesh meshConsensus;
        
    /**
     * Construct with settings object.
     * @param warpingOptions
     */
    public SignalWarper(@NonNull final SignalWarpingRunSettings warpingOptions) {

        if (warpingOptions == null)
            throw new IllegalArgumentException("Must have options");

        this.warpingOptions = warpingOptions;

        // Count the number of cells to include
        SignalManager m = warpingOptions.templateDataset().getCollection().getSignalManager();
        List<ICell> cells = warpingOptions.getBoolean(SignalWarpingRunSettings.IS_ONLY_CELLS_WITH_SIGNALS_KEY) 
        		? m.getCellsWithNuclearSignals(warpingOptions.signalId(), true) 
        		: warpingOptions.templateDataset().getCollection().getCells();
        totalCells = cells.size();
        LOGGER.fine(String.format("Created signal warper for %s signal group %s with %s cells, min threshold %s ",
        		warpingOptions.templateDataset().getName(),
        		warpingOptions.signalId(), 
        		totalCells, 
        		warpingOptions.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY)));
        
        try {
        	
        	Nucleus target = warpingOptions.targetDataset()
        			.getCollection().getConsensus(); // Issue here when using '.duplicate()' - causes image sizing issue
            
    		// Create the consensus mesh to warp each cell onto
    		meshConsensus = new DefaultMesh(target);
        } catch (MeshCreationException | MissingLandmarkException e2) {
    		LOGGER.log(Loggable.STACK, "Error creating mesh", e2);
    		throw new IllegalArgumentException("Could not create mesh", e2);
    	}
    }

    @Override
    protected ImageProcessor doInBackground() throws Exception {

    	LOGGER.finer( "Running warper");

        List<ImageProcessor> warpedImages =  generateImages();
        return ImageFilterer.addByteImages(warpedImages);
    }
    
    public SignalWarpingRunSettings getOptions() {
    	return warpingOptions;
    }

    @Override
    protected void process(List<Integer> chunks) {
        for (Integer i : chunks) {
            int percent = (int) ((double) i / (double) totalCells * 100);
            if (percent >= 0 && percent <= 100)
                setProgress(percent);
        }
    }

    @Override
    public void done() {
        try {
            if (this.get() != null) {
                firePropertyChange("Finished", getProgress(), IAnalysisWorker.FINISHED);
            } else {
                firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
            }
        } catch (InterruptedException e) {
            LOGGER.log(Loggable.STACK, "Interruption error in worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        } catch (ExecutionException e) {
            LOGGER.log(Loggable.STACK, "Execution error in worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        }
    }

    /**
     * Create the warped images for all selected nuclei in the dataset
     * @throws MissingOptionException 
     * 
     */
    private List<ImageProcessor> generateImages() throws MissingOptionException {
    	LOGGER.finer( "Generating warped images for " + warpingOptions.templateDataset().getName());
    	final List<ImageProcessor> warpedImages = Collections.synchronizedList(new ArrayList<>());
    	
    	List<ICell> cells = getCells(warpingOptions.getBoolean(SignalWarpingRunSettings.IS_ONLY_CELLS_WITH_SIGNALS_KEY));
    	
    	for(ICell c : cells) {
    		for(Nucleus n : c.getNuclei()) {
    			LOGGER.finer( "Drawing signals for " + n.getNameAndNumber());
    			ImageProcessor nImage = generateNucleusImage(n);
    			warpedImages.add(nImage);
    			publish(warpedImages.size());
    		}
    	}
    	return warpedImages;
    }
    
    /**
     * The empty processor to return if a warp fails. Finds the image dimensions 
     * for the warped images - used for blank images if the warping fails
     * @return
     */
    private ImageProcessor createEmptyProcessor() {
    	Rectangle r = meshConsensus.toPath().getBounds();
    	return ImageFilterer.createBlackByteProcessor(r.width, r.height);
    }

	/**
	 * Create the warped image for a nucleus
	 * @param n the nucleus to warp
	 * @return the warped image
	 * @throws MissingOptionException 
	 */
	private ImageProcessor generateNucleusImage(@NonNull Nucleus n) throws MissingOptionException {

		try {
			Mesh cellMesh = new DefaultMesh(n, meshConsensus);

			ImageProcessor ip = getNucleusImageProcessor(n);
		    
		    if(warpingOptions.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY)>0)
	    		ip = new ImageFilterer(ip)
	    		.setBlackLevel(warpingOptions.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY))
	    		.toProcessor();
		    
		    if(warpingOptions.getBoolean(SignalWarpingRunSettings.IS_BINARISE_SIGNALS_KEY))
		    	ip.threshold(warpingOptions.getInt(SignalWarpingRunSettings.MIN_THRESHOLD_KEY));
		    
		    if(warpingOptions.getBoolean(SignalWarpingRunSettings.IS_NORMALISE_TO_COUNTERSTAIN_KEY)) {
		    	ip = new ImageFilterer(ip)
		    	.normaliseToCounterStain(ImageImporter.importFullImageTo8bit(n))
		    	.toProcessor();
		    	
		    	// The actual floating point values may not be visible to the human eye
		    	// Rescale the values to lie in the 0-255 range
		    	ip = ImageFilterer.rescaleImageIntensity(ip);
		    }

		    // Create a mesh coordinate image from the nucleus
		    MeshImage meshImage = new DefaultMeshImage(cellMesh, ip);

		    // Draw the mesh image onto the consensus mesh.
		    LOGGER.finer( "Warping image onto consensus mesh");
		   return meshImage.drawImage(meshConsensus);

		} catch (IllegalArgumentException | MeshCreationException | UncomparableMeshImageException | MeshImageCreationException | UnloadableImageException e) {
		    LOGGER.log(Loggable.STACK, e.getMessage(), e);
		    return createEmptyProcessor();
		}
		
	}
		
	/**
	 * Fetch the appropriate image to warp for the given nucleus
	 * @param n the nucleus to warp
	 * @return the nucleus image
	 * @throws MissingOptionException 
	 */
	private ImageProcessor getNucleusImageProcessor(@NonNull Nucleus n) throws MissingOptionException {

		try {
			// Get the image with the signal
			ImageProcessor ip;
			if(n.getSignalCollection().hasSignal(warpingOptions.signalId())){ // if there is no signal, getImage will throw exception
				ip = n.getSignalCollection().getImage(warpingOptions.signalId());
				ip.invert(); // image is imported as white background. Need black background.
			} else {
				// We need to get the file in which no signals were detected
				// This is not stored in a nucleus, so combine the expected file name 
				// with the source folder				
				HashOptions signalOptions = getSignalOptions(n);

				if(signalOptions!=null) {
					File imageFolder = signalOptions.getFile(HashOptions.DETECTION_FOLDER);
					File imageFile   = new File(imageFolder, n.getSourceFileName());
					ip = new ImageImporter(imageFile).importImage(signalOptions.getInt(HashOptions.CHANNEL));

				} else {
					return createEmptyProcessor();
				}
			}
			return ip;
		} catch (UnloadableImageException |ImageImportException e) {
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
			return createEmptyProcessor();
		}
	}
	
	/**
	 * Get the nuclear signal detection options, accounting for whether
	 * the dataset is merged or not merged.
	 * @param n the nucleus to fetch options for
	 * @return the signal options if present, otherwise null
	 * @throws MissingOptionException 
	 */
	private HashOptions getSignalOptions(@NonNull Nucleus n) throws MissingOptionException {
		
		// If merged datasets are being warped, the imageFolder will not
		// be correct, since the analysis options are mostly blank. We need
		// to find the correct source dataset, and take the analysis options
		// from that dataset.
		if(warpingOptions.templateDataset().hasMergeSources()) {

			return warpingOptions.templateDataset()
			.getAllMergeSources().stream()
			.filter(d->d.getCollection().contains(n))
			.findFirst().get()
			.getAnalysisOptions().get()
			.getNuclearSignalOptions(warpingOptions.signalId()).orElseThrow(MissingOptionException::new);			
		} else {
			
			Optional<IAnalysisOptions> analysisOptions = warpingOptions.templateDataset()
					.getAnalysisOptions();

			if(analysisOptions.isPresent()) {
				return analysisOptions.get()
						.getNuclearSignalOptions(warpingOptions.signalId()).orElseThrow(MissingOptionException::new);

			}
		}

		return null;
	}
	
    /**
     * Get the cells to be used for the warping
     * 
     * @param withSignalsOnly
     * @return
     */
    private List<ICell> getCells(boolean withSignalsOnly) {

        SignalManager m = warpingOptions.templateDataset().getCollection().getSignalManager();
        List<ICell> cells;
        if (withSignalsOnly) {
            LOGGER.finer( "Only fetching cells with signals");
            cells = m.getCellsWithNuclearSignals(warpingOptions.signalId(), true);
        } else {
            LOGGER.finer( "Fetching all cells");
            cells = warpingOptions.templateDataset().getCollection().getCells();

        }
        return cells;
    }

}

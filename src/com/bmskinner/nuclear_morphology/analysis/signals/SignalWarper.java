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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Warps signals from nuclei in a collection onto a target nucleus using meshes.
 * This is implemneted as a SwingWorker that returns the merged overlay of all
 * the signals in the collection
 * 
 * @author ben
 * @since 1.13.6
 *
 */
public class SignalWarper extends SwingWorker<ImageProcessor, Integer> implements Loggable {
	
	public static final boolean STRAIGHTEN_MESH = true;
	public static final boolean REGULAR_MESH = false;
	
	/** Pixels with values lower than this will not be included in the warped image */
	public static final String MIN_SIGNAL_THRESHOLD_KEY = "Min signal threshold";
	
	 /** Straighten the meshes */
	public static final String IS_STRAIGHTEN_MESH_KEY   = "Straighten mesh";
	
	 /** Only warp the cell images with detected signals */
	public static final String JUST_CELLS_WITH_SIGNAL_KEY   = "Cells withs signals only";
	
	public static final int DEFAULT_MIN_SIGNAL_THRESHOLD = 0;

    private IAnalysisDataset sourceDataset;
    private Nucleus          target;
    private UUID             signalGroup;
    
    /** The options for the analysis */
    private HashOptions warpingOptions;
        
    /** The warped images to be merged */
    private final List<ImageProcessor> warpedImages = new ArrayList<>();
    
    /** The number of cell images to be merged */
    private int totalCells;
    
    /**
     * Constructor
     * 
     * @param source the dataset with signals to be warped
     * @param target the nucleus to warp signals onto
     * @param signalGroup the signal group id to be warped
     * @param cellsWithSignals if true, only cells with defined signals will be included
     * @param straighten if true, the signals will be warped onto a straightened mesh
     */
    public SignalWarper(@NonNull final IAnalysisDataset source, @NonNull final Nucleus target, @NonNull final UUID signalGroup, @NonNull HashOptions warpingOptions) {

        if (source == null)
            throw new IllegalArgumentException("Must have source dataset");
        if (target == null)
            throw new IllegalArgumentException("Must have target nucleus");
        if (warpingOptions == null)
            throw new IllegalArgumentException("Must have options");

        this.sourceDataset = source;
        this.target = target;
        this.signalGroup = signalGroup;
        this.warpingOptions = warpingOptions;

        // Count the number of cells to include
        SignalManager m = sourceDataset.getCollection().getSignalManager();
        Set<ICell> cells = warpingOptions.getBoolean(JUST_CELLS_WITH_SIGNAL_KEY) ? m.getCellsWithNuclearSignals(signalGroup, true) : sourceDataset.getCollection().getCells();
        totalCells = cells.size();
        fine(String.format("Created signal warper for %s signal group %s with %s cells, min threshold %s ",
        		sourceDataset.getName(), signalGroup, totalCells, warpingOptions.getInt(MIN_SIGNAL_THRESHOLD_KEY)));
    }

    @Override
    protected ImageProcessor doInBackground() throws Exception {

        try {
            finer("Running warper");
            generateImages();

        } catch (Exception e) {
            warn("Error in warper");
            stack("Error in signal warper", e);
            return null;
        }
        
        return mergeWarpedImages();
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
                finest("Firing trigger for sucessful task");
                firePropertyChange("Finished", getProgress(), IAnalysisWorker.FINISHED);
            } else {
                finest("Firing trigger for failed task");
                firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
            }
        } catch (InterruptedException e) {
            error("Interruption error in worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        } catch (ExecutionException e) {
            error("Execution error in worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        }
    }

    private void generateImages() {
        finer("Generating warped images for " + sourceDataset.getName());

        Mesh<Nucleus> meshConsensus;
        try {
            meshConsensus = new DefaultMesh<Nucleus>(target);
        } catch (MeshCreationException e2) {
            stack("Error creating mesh", e2);
            return;
        }

        Rectangle r = meshConsensus.toPath().getBounds();
        Set<ICell> cells = getCells(warpingOptions.getBoolean(JUST_CELLS_WITH_SIGNAL_KEY));

        int cellNumber = 0;

        for (ICell cell : cells) {

            for (Nucleus n : cell.getNuclei()) {
                finer("Drawing signals for " + n.getNameAndNumber());

                generateNucleusImage(meshConsensus, r.width, r.height, n);
    		    publish(cellNumber++);
            }
        }
    }

	private void generateNucleusImage(@NonNull Mesh<Nucleus> meshConsensus, int w, int h, @NonNull Nucleus n) {

		try {
			Mesh<Nucleus> cellMesh = new DefaultMesh<>(n, meshConsensus);

		    // Get the image with the signal
		    ImageProcessor ip;
		    if(n.getSignalCollection().hasSignal(signalGroup)){ // if there is no signal, getImage will throw exception
		    	ip = n.getSignalCollection().getImage(signalGroup);
		    	ip.invert();
		    } else {
		    	// We need to get the file in which no signals were detected
		    	// This is not stored in a nucleus, so combine the expected file name with the source folder
		    	INuclearSignalOptions signalOptions = sourceDataset.getAnalysisOptions().get().getNuclearSignalOptions(signalGroup);
		    	File imageFolder = signalOptions.getFolder();
		    	File imageFile   = new File(imageFolder, n.getSourceFileName());
		    	ip = new ImageImporter(imageFile).importImage(signalOptions.getChannel());
		    }
		    
		    if(warpingOptions.getInt(MIN_SIGNAL_THRESHOLD_KEY)>0)
	    		ip = new ImageFilterer(ip).setBlackLevel(warpingOptions.getInt(MIN_SIGNAL_THRESHOLD_KEY)).toProcessor();

		    MeshImage<Nucleus> meshImage = new DefaultMeshImage<>(cellMesh, ip);

		    // Draw NucleusMeshImage onto consensus mesh.
		    finer("Warping image onto consensus mesh");
		    warpedImages.add(meshImage.drawImage(meshConsensus));

		} catch (IllegalArgumentException e) {
		    fine(e.getMessage());
		    warpedImages.add(ImageFilterer.createBlackByteProcessor(w, h));
		} catch (UnloadableImageException | ImageImportException e) {
			fine(String.format("Unable to load signal image for signal group %s in nucleus %s ",
					 signalGroup, n.getNameAndNumber()));
		    warpedImages.add(ImageFilterer.createBlackByteProcessor(w, h));
		} catch (MeshCreationException e1) {
			fine("Error creating mesh");
		    warpedImages.add(ImageFilterer.createBlackByteProcessor(w, h));
		} catch (UncomparableMeshImageException | MeshImageCreationException e) {
			fine("Cannot make mesh for " + n.getNameAndNumber());
			 warpedImages.add(ImageFilterer.createBlackByteProcessor(w, h));
		}
	}
	
	/**
	 * Merge the warped images to a final image
	 */
	private ImageProcessor mergeWarpedImages() {
		
//		ImageStack st = new ImageStack(warpedImages.get(0).getWidth(), warpedImages.get(0).getHeight());
//		for(ImageProcessor ip : warpedImages)
//			st.addSlice(ip);
//		ImagePlus img = new ImagePlus("Stack", st);
//		img.show();
		
//		ImageProcessor fp = new FloatProcessor(st.getWidth(), st.getHeight());
		
		ImageProcessor mergedImage = ImageFilterer.addByteImages(warpedImages);
		
//		ImagePlus img2 = new ImagePlus("Added", mergedImage);
//		img2.show();
		
//		ImageProcessor scaledImage = ImageFilterer.rescaleImageIntensity(mergedImage);
		
//		ImagePlus img3 = new ImagePlus("Scaled", scaledImage);
//		img3.show();
		
		return mergedImage;
//		return ImageFilterer.rescaleImageIntensity(mergedImage);
	}


    /**
     * Get the cells to be used for the warping
     * 
     * @param withSignalsOnly
     * @return
     */
    private Set<ICell> getCells(boolean withSignalsOnly) {

        SignalManager m = sourceDataset.getCollection().getSignalManager();
        Set<ICell> cells;
        if (withSignalsOnly) {
            finer("Only fetching cells with signals");
            cells = m.getCellsWithNuclearSignals(signalGroup, true);
        } else {
            finer("Fetching all cells");
            cells = sourceDataset.getCollection().getCells();

        }
        return cells;
    }

}

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

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.AbstractFinder;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * Implementation of the Finder interface for detecting nuclear signals.
 * It generates the step-by-step images for display in the image prober
 * UI, calling a SignalDetector to find the actual signals 
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class SignalFinder extends AbstractFinder<List<INuclearSignal>> {
	
	private static final Logger LOGGER = Logger.getLogger(SignalFinder.class.getName());

    private final HashOptions     signalOptions;
    private final ICellCollection collection;

    /**
     * Create a signal detector for a dataset using the given options
     * @param analysisOptions the dataset analysis options
     * @param signalOptions the signal group analysis options
     * @param collection the cell collection to detect within
     */
    public SignalFinder(@NonNull IAnalysisOptions analysisOptions, @NonNull HashOptions signalOptions, @NonNull ICellCollection collection) {
        super(analysisOptions);
        this.signalOptions = signalOptions;
        this.collection = collection;
    }

    @Override
    public List<INuclearSignal> findInFolder(@NonNull File folder) throws ImageImportException {

        List<INuclearSignal> list = new ArrayList<>();

        if (folder.listFiles() == null)
            return list;

        for(File f : folder.listFiles()) {
        	if (ImageImporter.fileIsImportable(f)) {
        		try {
        			list.addAll(findInImage(f));
        		} catch (ImageImportException e) {
        			LOGGER.log(Loggable.STACK, "Error searching image", e);
        		}
        	}
        }

        return list;
    }

    @Override
    public List<INuclearSignal> findInImage(@NonNull File imageFile) throws ImageImportException {

        List<INuclearSignal> list = new ArrayList<>();

        // We need to find all possible signals so we can highlight which
        // ones pass filters, so we must relax the size thresholds.
        // Store them in a new options object to preserve the original.
        HashOptions testOptions = signalOptions.duplicate();
        testOptions.setInt(HashOptions.MIN_SIZE_PIXELS, 5);
        testOptions.setDouble(HashOptions.SIGNAL_MAX_FRACTION, 1d);
        SignalDetector detector = new SignalDetector(testOptions);

        ImageStack stack = new ImageImporter(imageFile).importToStack();

        // Find the processor number in the stack to use
        int stackNumber = ImageImporter.rgbToStack(signalOptions.getInt(HashOptions.CHANNEL));

        // Ignore incorrect channel selections
        if (stack.getSize() < stackNumber) {
            LOGGER.fine("Channel not present in image");
            return list;
        }

        LOGGER.fine("Converting image");
        // Get the greyscale processor for the signal channel
        ImageProcessor greyProcessor = stack.getProcessor(stackNumber);

        // Convert to an RGB processor for annotation
        ImageProcessor ip = new ImageConverter(greyProcessor).convertToRGBGreyscale().invert().toProcessor();

        ImageProcessor ap = ip.duplicate();

        ImageAnnotator in = new ImageAnnotator(ip);
        ImageAnnotator an = new ImageAnnotator(ap);

        // The given image file may not be the same image that the nucleus was
        // detected in.
        // Take the image name only, and add onto the DAPI folder name.
        // This requires that the signal file name is identical to the dapi file
        // name

        String imageName = imageFile.getName();
        File dapiFolder = options.getDetectionOptions(CellularComponent.NUCLEUS).get().getFile(HashOptions.DETECTION_FOLDER);

        File dapiFile = new File(dapiFolder, imageName);

        Set<Nucleus> nuclei = collection.getNuclei(dapiFile);

        LOGGER.fine("Detecting signals in " + nuclei.size() + " nuclei");

        for (Nucleus n : nuclei) {
            try {
                // The detector also creates and adds the signals currently
                List<INuclearSignal> temp = detector.detectSignal(imageFile, n);

                if (hasDetectionListeners()) {
                    LOGGER.fine("Drawing signals for " + n.getNameAndNumber());
                    drawSignals(n, temp, in, false);
                    drawSignals(n, temp, an, true);
                }

                for (INuclearSignal s : temp) {
                    if (isValid(s, n)) {
                        list.add(s);
                    }
                }

            } catch (Exception e) {
                LOGGER.log(Loggable.STACK, "Error in detector", e);
            }
        }

        if (hasDetectionListeners()) {
            // annotate detected signals onto the imagefile
            fireDetectionEvent(ip.duplicate(), "Detected objects");

            // annotate detected signals onto the imagefile
            fireDetectionEvent(ap.duplicate(), "Annotated objects");
        }
        return list;
    }

    /**
     * Find nuclear signals within the given image, for the given nucleus
     * @param imageFile the image to search
     * @param n the nucleus the signals should belong to
     * @return
     * @throws ImageImportException
     */
    public List<INuclearSignal> findInImage(@NonNull File imageFile, @NonNull Nucleus n) throws ImageImportException {

    	SignalDetector sd = new SignalDetector(signalOptions);

        List<INuclearSignal> list = new ArrayList<>();
        try {
            // The detector also creates the signals currently
            List<INuclearSignal> temp = sd.detectSignal(imageFile, n);
            for (INuclearSignal s : temp)
                if (isValid(s, n)) 
                    list.add(s);
        } catch (IllegalArgumentException e) {
        	LOGGER.warning("Unable to find images in image "+imageFile.getAbsolutePath()+": "+e.getMessage());
        	LOGGER.log(Loggable.STACK, "Error in detector with image "+imageFile.getAbsolutePath(), e);
        }
        return list;
    }

    /**
     * Draw the signals for the given nucleus on an annotator
     * @param n the nucleus to annotate
     * @param list the  signals in the nucleus to be annotated
     * @param an the annotator
     * @param annotateStats should the stats be drawn on the image
     */
    protected void drawSignals(@NonNull Nucleus n, @NonNull List<INuclearSignal> list, @NonNull ImageAnnotator an, boolean annotateStats) {

        an.drawBorder(n, Color.BLUE);
        for (INuclearSignal s : list) {
            Color color = isValid(s, n) ? Color.ORANGE : Color.RED;
            an.drawBorder(s, color);
            if (annotateStats) {
                an.annotateSignalStats(n, s, Color.YELLOW, Color.BLUE);
            }
        }
    }

    /**
     * Test if the given signal passes the options criteria
     * @param s the signal
     * @param n the nucleus the signal belongs to
     * @return
     */
    private boolean isValid(@NonNull INuclearSignal s, @NonNull Nucleus n) {
        return (s.getMeasurement(Measurement.AREA) >= signalOptions.getInt(HashOptions.MIN_SIZE_PIXELS)
        		&& s.getMeasurement(Measurement.AREA) <= (signalOptions.getDouble(HashOptions.SIGNAL_MAX_FRACTION) * n.getMeasurement(Measurement.AREA)));
    }

}

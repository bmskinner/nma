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
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.AbstractFinder;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * Implementation of the Finder interface for detecting nuclear signals
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class SignalFinder extends AbstractFinder<List<INuclearSignal>> {

    private SignalDetector              detector;
    final private INuclearSignalOptions signalOptions;
    final private ICellCollection       collection;

    /**
     * Create a signal detector for a dataset using the given options
     * @param analysisOptions the dataset analysis options
     * @param signalOptions the signal group analysis options
     * @param collection the cell collection to detect within
     */
    public SignalFinder(@NonNull IAnalysisOptions analysisOptions, @NonNull INuclearSignalOptions signalOptions, @NonNull ICellCollection collection) {
        super(analysisOptions);
        this.signalOptions = signalOptions;
        this.collection = collection;

        INuclearSignalOptions testOptions = (INuclearSignalOptions) signalOptions.duplicate();
        testOptions.setMinSize(5);
        testOptions.setMaxFraction(1d);

        detector = new SignalDetector(testOptions, testOptions.getChannel());
    }

    @Override
    public List<INuclearSignal> findInFolder(@NonNull File folder) throws ImageImportException {

        if (folder == null)
            throw new IllegalArgumentException("Folder cannot be null");

        List<INuclearSignal> list = new ArrayList<>();

        if (folder.listFiles() == null)
            return list;

        Stream.of(folder.listFiles()).forEach(f -> {
            if (!f.isDirectory()) {

                if (ImageImporter.fileIsImportable(f)) {
                    try {
                        list.addAll(findInImage(f));
                    } catch (ImageImportException e) {
                        stack("Error searching image", e);
                    }
                }
            }
        });

        return list;
    }

    @Override
    public List<INuclearSignal> findInImage(@NonNull File imageFile) throws ImageImportException {

        List<INuclearSignal> list = new ArrayList<>();

        INuclearSignalOptions testOptions = (INuclearSignalOptions) signalOptions.duplicate();
        testOptions.setMinSize(5);
        testOptions.setMaxFraction(1d);
        detector = new SignalDetector(testOptions, testOptions.getChannel());

        ImageStack stack = new ImageImporter(imageFile).importToStack();

        // Find the processor number in the stack to use
        int stackNumber = ImageImporter.rgbToStack(signalOptions.getChannel());

        // Ignore incorrect channel selections
        if (stack.getSize() < stackNumber) {
            fine("Channel not present in image");
            return list;
        }

        fine("Converting image");
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
        File dapiFolder = collection.getFolder();

        File dapiFile = new File(dapiFolder, imageName);

        Set<Nucleus> nuclei = collection.getNuclei(dapiFile);

        fine("Detecting signals in " + nuclei.size() + " nuclei");

        for (Nucleus n : nuclei) {
            try {
                // The detector also creates and adds the signals currently
                List<INuclearSignal> temp = detector.detectSignal(imageFile, stack, n);

                if (hasDetectionListeners()) {
                    fine("Drawing signals for " + n.getNameAndNumber());
                    drawSignals(n, temp, in, false);
                    drawSignals(n, temp, an, true);
                }

                for (INuclearSignal s : temp) {
                    if (checkSignal(s, n)) {
                        list.add(s);
                    }
                }

            } catch (Exception e) {
                error("Error in detector", e);
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

        detector = new SignalDetector(signalOptions, signalOptions.getChannel());

        List<INuclearSignal> list = new ArrayList<>();
        try {
        	ImageStack stack = new ImageImporter(imageFile).importToStack();

            // The detector also creates the signals currently
            List<INuclearSignal> temp = detector.detectSignal(imageFile, stack, n);
            for (INuclearSignal s : temp)
                if (checkSignal(s, n)) 
                    list.add(s);
        } catch (IllegalArgumentException | ImageImportException e) {
        	warn("Unable to find images in image "+imageFile.getAbsolutePath()+": "+e.getMessage());
            fine("Error in detector with image "+imageFile.getAbsolutePath(), e);
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

        an.annotateBorder(n, Color.BLUE);
        for (INuclearSignal s : list) {
            Color color = checkSignal(s, n) ? Color.ORANGE : Color.RED;
            an.annotateBorder(s, color);
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
    private boolean checkSignal(@NonNull INuclearSignal s, @NonNull Nucleus n) {
        if (s.getStatistic(PlottableStatistic.AREA) < signalOptions.getMinSize())
            return false;
        if (s.getStatistic( PlottableStatistic.AREA) > (signalOptions.getMaxFraction() * n.getStatistic(PlottableStatistic.AREA)))
            return false;
        return true;
    }

}

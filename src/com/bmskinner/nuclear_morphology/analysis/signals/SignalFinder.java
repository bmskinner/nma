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


package com.bmskinner.nuclear_morphology.analysis.signals;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.AbstractFinder;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions;
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

    public SignalFinder(IAnalysisOptions op, INuclearSignalOptions signalOptions, ICellCollection collection) {
        super(op);
        this.signalOptions = signalOptions;
        this.collection = collection;

        IMutableNuclearSignalOptions testOptions = (IMutableNuclearSignalOptions) signalOptions.duplicate();
        testOptions.setMinSize(5);
        testOptions.setMaxFraction(1d);

        detector = new SignalDetector(testOptions, testOptions.getChannel());
    }

    @Override
    public List<INuclearSignal> findInFolder(File folder) throws ImageImportException, ComponentCreationException {

        if (folder == null) {
            throw new IllegalArgumentException("Folder cannot be null");
        }

        List<INuclearSignal> list = new ArrayList<>();

        if (folder.listFiles() == null) {
            return list;
        }

        Stream.of(folder.listFiles()).parallel().forEach(f -> {
            if (!f.isDirectory()) {

                if (ImageImporter.fileIsImportable(f)) {
                    try {
                        list.addAll(findInImage(f));
                    } catch (ImageImportException | ComponentCreationException e) {
                        stack("Error searching image", e);
                    }
                }
            }
        });

        return list;
    }

    @Override
    public List<INuclearSignal> findInImage(File imageFile) throws ImageImportException, ComponentCreationException {

        List<INuclearSignal> list = new ArrayList<>();

        IMutableNuclearSignalOptions testOptions = (IMutableNuclearSignalOptions) signalOptions.duplicate();
        testOptions.setMinSize(5);
        testOptions.setMaxFraction(1d);
        detector = new SignalDetector(testOptions, testOptions.getChannel());

        ImageStack stack = new ImageImporter(imageFile).importToStack();

        // Find the processor number in the stack to use
        int stackNumber = ImageImporter.rgbToStack(signalOptions.getChannel());

        // Ignore incorrect channel selections
        if (stack.getSize() < stackNumber) {
            fine("Channel not available");
            return list;
        }

        fine("Converting image");
        // Get the greyscale processor for the signal channel
        ImageProcessor greyProcessor = stack.getProcessor(stackNumber);

        // Convert to an RGB processor for annotation
        ImageProcessor ip = new ImageConverter(greyProcessor).convertToGreyscale().invert().toProcessor();

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

    public List<INuclearSignal> findInImage(File imageFile, Nucleus n) throws ImageImportException {

        detector = new SignalDetector(signalOptions, signalOptions.getChannel());

        List<INuclearSignal> list = new ArrayList<>();
        ImageStack stack = new ImageImporter(imageFile).importToStack();

        try {
            // The detector also creates the signals currently
            List<INuclearSignal> temp = detector.detectSignal(imageFile, stack, n);

            for (INuclearSignal s : temp) {
                if (checkSignal(s, n)) {
                    list.add(s);
                }
            }

        } catch (Exception e) {
            error("Error in detector", e);
        }

        return list;

    }

    protected void drawSignals(Nucleus n, List<INuclearSignal> list, ImageAnnotator an, boolean annotate) {

        an.annotateBorder(n, Color.BLUE);

        for (INuclearSignal s : list) {

            Color color = checkSignal(s, n) ? Color.ORANGE : Color.RED;
            an.annotateBorder(s, color);

            if (annotate) {
                an.annotateSignalStats(n, s, Color.YELLOW, Color.BLUE);
            }

        }

    }

    private boolean checkSignal(INuclearSignal s, Nucleus n) {

        if (s.getStatistic(PlottableStatistic.AREA) < signalOptions.getMinSize()) {

            return false;
        }

        if (s.getStatistic(
                PlottableStatistic.AREA) > (signalOptions.getMaxFraction() * n.getStatistic(PlottableStatistic.AREA))) {

            return false;
        }
        return true;
    }

}

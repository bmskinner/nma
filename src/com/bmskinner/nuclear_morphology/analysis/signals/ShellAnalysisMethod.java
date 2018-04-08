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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellCounter.CountType;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

import ij.ImageStack;

/**
 * Detect signal proportions within concentric shells of a nucleus
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class ShellAnalysisMethod extends SingleDatasetAnalysisMethod {

	public static final int MINIMUM_AREA_PER_SHELL = 100;
	public static final double MINIMUM_CIRCULARITY = 0.3;
	
    private final int shells;

    private int totalPixels = 0;

    private static Map<UUID, ShellCounter> counters = new HashMap<UUID, ShellCounter>(0);

    public ShellAnalysisMethod(IAnalysisDataset dataset, int shells) {
        super(dataset);
        this.shells = shells;

    }

    @Override
    public IAnalysisResult call() throws Exception {

        // Set the progress total
        ProgressEvent e = new ProgressEvent(this, ProgressEvent.SET_TOTAL_PROGRESS, dataset.getCollection().size() - 1);
        fireProgressEvent(e);
        run();
        IAnalysisResult r = new DefaultAnalysisResult(dataset);

        return r;
    }

    protected void run() {

        ICellCollection collection = dataset.getCollection();

        if (!collection.getSignalManager().hasSignals()) {
            fine("No signals in population");
            return;

        }

        log("Performing shell analysis with " + shells + " shells...");

        try {
            counters = new HashMap<UUID, ShellCounter>(0);

            for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

                if (signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)) {
                    continue;
                }
                counters.put(signalGroup, new ShellCounter(shells));
            }

            // make the shells and measure the values
            for (Nucleus n : collection.getNuclei()) {

                analyseNucleus(n);
                fireProgressEvent();
            }

            ProgressEvent e = new ProgressEvent(this, ProgressEvent.SET_INDETERMINATE, 0);
            fireProgressEvent(e);

            // get stats
            createResults();

            log("Shell analysis complete");
        } catch (Exception e) {
            warn("Error in shell analysis");
            stack("Error in shell analysis", e);
            return;
        }
        return;
    }

    private void analyseNucleus(Nucleus n) {

        ICellCollection collection = dataset.getCollection();

        ShellDetector shellDetector;
        try {
            shellDetector = new ShellDetector(n, shells);
        } catch (ShellAnalysisException e1) {
            warn("Unable to make shells for " + n.getNameAndNumber());
            stack("Error in shell detector", e1);
            return;
        }

        try {

            ImageStack nucleusStack = new ImageImporter(n.getSourceFile()).importToStack();

            for (UUID signalGroup : n.getSignalCollection().getSignalGroupIDs()) {

                if (signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)) {
                    continue;
                }

                if (collection.getSignalManager().hasSignals(signalGroup)) {
                    List<INuclearSignal> signals = n.getSignalCollection().getSignals(signalGroup);
                    if (signals.isEmpty()) {
                        fine("No signals in signal group " + signalGroup + "in nucleus");
                        continue;
                    }

                    File sourceFile = n.getSignalCollection().getSourceFile(signalGroup);

                    if (sourceFile == null) {
                        warn("Cannot find signal image for " + n.getNameAndNumber());
                        continue;
                    }
                    ImageStack signalStack = new ImageImporter(sourceFile).importToStack();

                    ShellCounter counter = counters.get(signalGroup);

                    for (INuclearSignal s : signals) {

                        try {

                            double[] signalInSignals = shellDetector.findProportionPerShell(s);
                            int[] countsInSignals = shellDetector.findPixelCountPerShell(s);

                            int[] countsInNucleus = shellDetector.findPixelIntensityPerShell(signalStack,
                                    s.getChannel());
                            double[] signalInNucleus = shellDetector.findProportionPerShell(signalStack,
                                    s.getChannel());

                            int[] dapiIntensities = shellDetector.findPixelIntensityPerShell(nucleusStack,
                                    n.getChannel());

                            double[] normalisedSignals = shellDetector.normalise(signalInSignals, dapiIntensities);
                            double[] normalisedNucleus = shellDetector.normalise(signalInNucleus, dapiIntensities);

                            counter.addSignalValues(signalInSignals, normalisedSignals, countsInSignals);
                            counter.addNucleusValues(signalInNucleus, normalisedNucleus, countsInNucleus);

                            totalPixels += counter.getPixelCounts(CountType.SIGNAL)
                            		.stream().mapToInt(i->i.intValue()).sum();
  
                        } catch (ShellAnalysisException e) {
                            warn("Error in shell analysis");
                            stack("Error in signal in shell analysis", e);
                        }
                    } // end for signals
                } // end if signals
            }
        } catch (ImageImportException e) {

            warn("Cannot import image source file");
            stack("Error importing file", e);
        }

    }

    private void createResults() {
        // get stats and export
        ICellCollection collection = dataset.getCollection();

        boolean addRandom = false;

        for (UUID group : counters.keySet()) {
            if (collection.getSignalManager().hasSignals(group)) {

                addRandom = true;
                ShellCounter channelCounter = counters.get(group);

                DefaultShellResult result = new DefaultShellResult(shells);

                for (CountType type : CountType.values()) {
                    result.setRawMeans(type, channelCounter.getRawMeans(type))
                            .setNormalisedMeans(type, channelCounter.getNormalisedMeans(type))
                            .setRawStandardErrors(type, channelCounter.getRawStandardErrors(type))
                            .setNormalisedStandardErrors(type, channelCounter.getNormalisedStandardErrors(type))
                            .setRawChiResult(type, channelCounter.getRawChiSquare(type),
                                    channelCounter.getRawPValue(type))
                            .setNormalisedChiResult(type, channelCounter.getNormalisedChiSquare(type),
                                    channelCounter.getNormalisedPValue(type));
                }

                dataset.getCollection().getSignalGroup(group).get().setShellResult(result);

            }
        }

        if (addRandom) {
            addRandomSignal();
        }
    }

    private void addRandomSignal() {

        ICellCollection collection = dataset.getCollection();

        // Create a random sample distibution
        if (collection.hasConsensus()) {

            ISignalGroup random = new SignalGroup();
            random.setGroupName("Random distribution");
            random.setFolder(new File(""));

            dataset.getCollection().addSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID, random);

            // Calculate random positions of pixels
            
            int iterations = totalPixels < 100000 ? totalPixels : 100000; // stop stupidly long calculations 
            fine("Creating random sample of " + iterations + " pixels");

            ShellRandomDistributionCreator sr = new ShellRandomDistributionCreator(collection.getConsensus(), shells,
                    iterations);

            double[] c = sr.getProportions();
            double[] err = new double[c.length];
            Arrays.fill(err, 0);

            List<Double> list = DoubleStream.of(c).boxed().collect(Collectors.toList());
			List<Double> errList = DoubleStream.of(err).boxed().collect(Collectors.toList());

			IShellResult randomResult = new DefaultShellResult(shells).setRawMeans(CountType.SIGNAL, list)
			        .setRawMeans(CountType.NUCLEUS, list).setNormalisedMeans(CountType.SIGNAL, list)
			        .setNormalisedMeans(CountType.NUCLEUS, list).setRawStandardErrors(CountType.SIGNAL, errList)
			        .setRawStandardErrors(CountType.NUCLEUS, errList)
			        .setNormalisedStandardErrors(CountType.SIGNAL, errList)
			        .setNormalisedStandardErrors(CountType.NUCLEUS, errList);

			dataset.getCollection().getSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID).get()
			        .setShellResult(randomResult);
        } else {
            warn("Cannot create simulated dataset, no consensus");
        }

    }

}

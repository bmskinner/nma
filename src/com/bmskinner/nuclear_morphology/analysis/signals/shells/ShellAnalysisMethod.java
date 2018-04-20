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


package com.bmskinner.nuclear_morphology.analysis.signals.shells;

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
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.KeyedShellResult;
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

    private static Map<UUID, KeyedShellResult> counters = new HashMap<UUID, KeyedShellResult>(0);
//    private static Map<UUID, ShellCounter> counters = new HashMap<UUID, ShellCounter>(0);

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

        if (!collection.getSignalManager().hasSignals()) 
            return;

        log("Performing shell analysis with " + shells + " shells...");

        try {
            counters = new HashMap<>(0);

            for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

                if (signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID))
                    continue;
                counters.put(signalGroup, new KeyedShellResult(shells));
            }

            // make the shells and measure the values
            for (ICell c : collection.getCells()) {
                new CellAnalysis(c).analyse();
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

    
    /**
     * Abstracts the business of creating shell detectors.
     * @author bms41
     * @since 1.13.8
     *
     */
    private class CellAnalysis {
        private ShellDetector shellDetector;
        final ICell c;
        
        public CellAnalysis(ICell c){
            this.c = c;
        }
        
        public void analyse(){
            for(Nucleus n : c.getNuclei()){
                analyseNucleus(n);
                fireProgressEvent();
            }
        }


        private void analyseNucleus(Nucleus n) {

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

                    if (signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID))
                        continue;
                    analyseSignalGroup(n, signalGroup);  
                }
            } catch (ImageImportException e) {

                warn("Cannot import image source file");
                stack("Error importing file", e);
            }

        }

        private void analyseSignalGroup(Nucleus n, UUID signalGroup) throws ImageImportException {
            ICellCollection collection = dataset.getCollection();
            if (!collection.getSignalManager().hasSignals(signalGroup))
                return;


            List<INuclearSignal> signals = n.getSignalCollection().getSignals(signalGroup);
            if (signals.isEmpty())
                return;

            File sourceFile = n.getSignalCollection().getSourceFile(signalGroup);

            if (sourceFile == null) 
                return;

            ImageStack nucleusStack = new ImageImporter(n.getSourceFile()).importToStack();
            ImageStack signalStack = new ImageImporter(sourceFile).importToStack();

            KeyedShellResult counter = counters.get(signalGroup);

            int channel = n.getSignalCollection().getSourceChannel(signalGroup);

            long[] totalSignalIntensity  = shellDetector.findPixelIntensityPerShell(signalStack, channel);
            long[] totalCounterIntensity = shellDetector.findPixelIntensityPerShell(nucleusStack, n.getChannel());

            counter.addShellData(CountType.COUNTERSTAIN, c, n, totalCounterIntensity); // the counterstain within the nucleus
            counter.addShellData(CountType.SIGNAL, c, n, totalSignalIntensity); // the pixels within the whole nucleus

            for (INuclearSignal s : signals) {
                long[] countsInSignals = shellDetector.findPixelCountPerShell(s);
                counter.addShellData(CountType.SIGNAL, c, n, s, countsInSignals); // the pixels within the signal
            }
        }
    }

    private void createResults() {
        // get stats and export
        ICellCollection collection = dataset.getCollection();

        boolean addRandom = false;

        for (UUID group : counters.keySet()) {
            if (collection.getSignalManager().hasSignals(group)) {

                addRandom = true;
                KeyedShellResult channelCounter = counters.get(group);

//                DefaultShellResult result = new DefaultShellResult(shells);
//
//                for (CountType type : CountType.values()) {
//                    result.setRawMeans(type, channelCounter.getRawMeans(type))
//                            .setNormalisedMeans(type, channelCounter.getNormalisedMeans(type))
//                            .setRawStandardErrors(type, channelCounter.getRawStandardErrors(type))
//                            .setNormalisedStandardErrors(type, channelCounter.getNormalisedStandardErrors(type))
//                            .setRawChiResult(type, channelCounter.getRawChiSquare(type),
//                                    channelCounter.getRawPValue(type))
//                            .setNormalisedChiResult(type, channelCounter.getNormalisedChiSquare(type),
//                                    channelCounter.getNormalisedPValue(type));
//                }

                dataset.getCollection().getSignalGroup(group).get().setShellResult(channelCounter);

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

            ShellRandomDistributionCreator sr = new ShellRandomDistributionCreator(collection.getConsensus(), shells,
                    iterations);

            double[] c = sr.getProportions();
            double[] err = new double[c.length];
            Arrays.fill(err, 0);

            List<Double> list = DoubleStream.of(c).boxed().collect(Collectors.toList());
			List<Double> errList = DoubleStream.of(err).boxed().collect(Collectors.toList());

			IShellResult randomResult = new DefaultShellResult(shells).setRawMeans(CountType.SIGNAL, list)
			        .setRawMeans(CountType.COUNTERSTAIN, list).setNormalisedMeans(CountType.SIGNAL, list)
			        .setNormalisedMeans(CountType.COUNTERSTAIN, list).setRawStandardErrors(CountType.SIGNAL, errList)
			        .setRawStandardErrors(CountType.COUNTERSTAIN, errList)
			        .setNormalisedStandardErrors(CountType.SIGNAL, errList)
			        .setNormalisedStandardErrors(CountType.COUNTERSTAIN, errList);

			dataset.getCollection().getSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID).get()
			        .setShellResult(randomResult);
        } else {
            warn("Cannot create simulated dataset, no consensus");
        }

    }

}

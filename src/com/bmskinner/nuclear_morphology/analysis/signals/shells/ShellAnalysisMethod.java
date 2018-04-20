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

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
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
import com.bmskinner.nuclear_morphology.logging.Loggable;

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

                if (signalGroup.equals(IShellResult.RANDOM_SIGNAL_ID))
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

                    if (signalGroup.equals(IShellResult.RANDOM_SIGNAL_ID))
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
            addRandom |= collection.getSignalManager().hasSignals(group);
            if (collection.getSignalManager().hasSignals(group)) {
                KeyedShellResult channelCounter = counters.get(group);
                dataset.getCollection().getSignalGroup(group).get().setShellResult(channelCounter);
            }
        }

        if (addRandom)
            addRandomSignal();
    }

    private void addRandomSignal() {

        ICellCollection collection = dataset.getCollection();

        // Create a random sample distribution
        if (collection.hasConsensus()) {

            ISignalGroup random = new SignalGroup();
            random.setGroupName("Random distribution");
            random.setFolder(new File(""));

            dataset.getCollection().addSignalGroup(IShellResult.RANDOM_SIGNAL_ID, random);

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

			dataset.getCollection().getSignalGroup(IShellResult.RANDOM_SIGNAL_ID).get()
			        .setShellResult(randomResult);
        } else {
            warn("Cannot create simulated dataset, no consensus");
        }

    }
    
    private static class ShellRandomDistributionCreator {
        /**
         * Store the shell as a key, and the number of signals measured as a value
         */
        private Map<Integer, Integer> map = new HashMap<Integer, Integer>();

        public static final int DEFAULT_ITERATIONS = 10000;

        public ShellRandomDistributionCreator(CellularComponent template, int shellCount, int iterations) {

            if (shellCount <= 1)
                throw new IllegalArgumentException("Shell count must be > 1");
            // Make a list of random points

            double xCen = template.getBounds().getCenterX();
            double yCen = template.getBounds().getCenterY();
            double xMin = template.getBounds().getMinX();
            double xMax = template.getBounds().getMaxX();
            double yMin = template.getBounds().getMinY();
            double yMax = template.getBounds().getMaxY();

            List<IPoint> list = new ArrayList<IPoint>();
            for (int i = 0; i < iterations; i++) {
                list.add(createRandomPoint(template));
            }

            // Find the shell for these points in the template
            ShellDetector detector;
            try {
                detector = new ShellDetector(template, shellCount);

                // initialise the map
                for (int i = -1; i < shellCount; i++) {
                    map.put(i, 0);
                }

                for (IPoint p : list) {
                    int shell = detector.findShell(p);

                    int count = map.get(shell);
                    map.put(shell, ++count);
                }

            } catch (ShellAnalysisException e) {
//                error("Simulation failed", e);
            }

//            int neg1 = -1;
//
//            if (map.get(neg1) > 0) {
//                fine("Unable to map " + map.get(neg1) + " points");
//            }

        }

        /**
         * Get the number of signals measured in the given shell
         * 
         * @param shell
         * @return
         */
        public int getCount(int shell) {
            if (!map.containsKey(shell)) {
                return 0;
            }
            return map.get(shell);
        }

        /**
         * Get the total number of hits excluding unmapped points
         * 
         * @return
         */
        private int getTotalCount() {
            int result = 0;
            for (int i : map.keySet()) {
                if (i == -1) {
                    continue;
                }
                result += map.get(i);
            }
            return result;
        }

        /**
         * Get the proportion of total signal in the given shell
         * 
         * @param shell
         * @return
         */
        public double getProportion(int shell) {
            if (!map.containsKey(shell)) {
                return 0;
            }
            int total = getTotalCount();

            int count = map.get(shell);

            double prop = (double) count / (double) total;

            return prop;
        }

        public double[] getProportions() {

            int shells = map.size() - 1;

            double[] result = new double[shells];

            for (int i = 0; i < shells; i++) {
                result[i] = getProportion(i);
            }
            return result;
        }

        public int[] getCounts() {

            int shells = map.size() - 1;

            int[] result = new int[shells];

            for (int i = 0; i < shells; i++) {
                result[i] = getCount(i);
            }
            return result;
        }

        /**
         * Create a random point that lies within the template
         * 
         * @param template
         * @return
         */
        private IPoint createRandomPoint(CellularComponent template) {

            Rectangle2D r = template.getBounds();

            // Make a random position in the rectangle
            // nextDouble is exclusive of the top value,
            // so add 1 to make it inclusive
            double rx = ThreadLocalRandom.current().nextDouble(r.getX(), r.getWidth() + 1);
            double ry = ThreadLocalRandom.current().nextDouble(r.getY(), r.getHeight() + 1);

            IPoint p = IPoint.makeNew(rx, ry);

            if (template.containsPoint(p)) {
                return p;
            } else {
                return createRandomPoint(template);
            }

        }
    }
    
    public class ShellAnalysisException extends Exception {
        private static final long serialVersionUID = 1L;

        public ShellAnalysisException() {
            super();
        }

        public ShellAnalysisException(String message) {
            super(message);
        }

        public ShellAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }

        public ShellAnalysisException(Throwable cause) {
            super(cause);
        }
    }

}

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

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.KeyedShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.RandomShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IShellOptions;
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

	public static final int MINIMUM_AREA_PER_SHELL = 50;
	public static final double MINIMUM_CIRCULARITY = 0.1;
	
	private final IShellOptions options;
	
	private ICellCollection collection;

    private final Map<UUID, KeyedShellResult> counters = new HashMap<>();

    public ShellAnalysisMethod(@NonNull final IAnalysisDataset dataset, @NonNull final IShellOptions o) {
        super(dataset);
        options = o;
    }

    @Override
    public IAnalysisResult call() throws Exception {
        // Set the progress total
        ProgressEvent e = new ProgressEvent(this, ProgressEvent.SET_TOTAL_PROGRESS, dataset.getCollection().size() - 1);
        fireProgressEvent(e);
        run();  
        return new DefaultAnalysisResult(dataset);
    }

    protected void run() {
       
        
    	// If all cells are not suitable for shell analysis, prefiltering may have created a child collection that
    	// can be analysed. Check for this before running.
//    	Optional<IAnalysisDataset> optionalSuitable = dataset.getChildDatasets().stream().filter(d->d.getName().equals("Suitable_for_shell_analysis")).findAny();
//    	collection = optionalSuitable.isPresent() ? optionalSuitable.get().getCollection() : dataset.getCollection();
    	collection = dataset.getCollection();
    	if (!collection.getSignalManager().hasSignals()) 
             return;
    	 
    	log("Created shell analysis for collection "+collection.getName());

        log(String.format("Performing %s shell analysis with %s shells on dataset %s...", 
        		options.getErosionMethod(), options.getShellNumber(), collection.getName()));

        try {

            for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

                if (signalGroup.equals(IShellResult.RANDOM_SIGNAL_ID))
                    continue;
                counters.put(signalGroup, new KeyedShellResult( options.getShellNumber(), options.getErosionMethod()));
                
                // Assign the options to each signal group
                dataset.getAnalysisOptions().get().getNuclearSignalOptions(signalGroup).setShellOptions(options);
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


        private void analyseNucleus(@NonNull final Nucleus n) {

            try {
                shellDetector = new ShellDetector(n,  options.getShellNumber(), options.getErosionMethod(), true);
            } catch (ShellAnalysisException e1) {
                warn("Unable to make shells for " + n.getNameAndNumber());
                stack("Error in shell detector", e1);
                return;
            }

            try {

                ImageStack nucleusStack = new ImageImporter(n.getSourceFile()).importToStack();

                for (UUID signalGroup : n.getSignalCollection().getSignalGroupIds()) {

                    if (signalGroup.equals(IShellResult.RANDOM_SIGNAL_ID))
                        continue;
                    analyseSignalGroup(n, signalGroup);  
                }
            } catch (ImageImportException e) {

                warn("Cannot import image source file");
                stack("Error importing file", e);
            }

        }

        private void analyseSignalGroup(@NonNull final Nucleus n, @NonNull final UUID signalGroup) throws ImageImportException {
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

            int signalChannel = n.getSignalCollection().getSourceChannel(signalGroup);

            long[] totalSignalIntensity  = shellDetector.findPixelIntensities(signalStack, signalChannel);
            long[] totalCounterIntensity = shellDetector.findPixelIntensities(n);

            counter.addShellData(CountType.COUNTERSTAIN, c, n, totalCounterIntensity); // the counterstain within the nucleus
            counter.addShellData(CountType.SIGNAL, c, n, totalSignalIntensity); // the pixels within the whole nucleus
            
            for (INuclearSignal s : signals) {
                long[] countsInSignals = shellDetector.findPixelIntensities(s);
                counter.addShellData(CountType.SIGNAL, c, n, s, countsInSignals); // the pixels within the signal
            }
        }
    }

    private void createResults() {
        // get stats and export
        boolean addRandom = false;

        for (UUID group : counters.keySet()) {
            addRandom |= collection.getSignalManager().hasSignals(group);
            if (collection.getSignalManager().hasSignals(group)) {
                KeyedShellResult channelCounter = counters.get(group);
                collection.getSignalGroup(group).get().setShellResult(channelCounter);
            }
        }

        if (addRandom)
            addRandomSignal();
    }

    private void addRandomSignal() {

        // Create a random sample distribution
        if (collection.hasConsensus()) {

            ISignalGroup random = new SignalGroup("Random distribution");
            random.setFolder(new File(""));
            random.setGroupColour(Color.LIGHT_GRAY);

            collection.addSignalGroup(IShellResult.RANDOM_SIGNAL_ID, random);

            // Calculate random positions of pixels
            RandomDistribution sr = new RandomDistribution(collection.getConsensus(), RandomDistribution.DEFAULT_ITERATIONS);

            long[] c = sr.getCounts();
            
			RandomShellResult randomResult = new RandomShellResult( options.getShellNumber(), options.getErosionMethod(), c);
			
			collection.getSignalGroup(IShellResult.RANDOM_SIGNAL_ID).get()
			        .setShellResult(randomResult);
        } else {
            warn("Cannot create simulated dataset, no consensus");
        }

    }
    
    private class RandomDistribution {
        
        private long[] counts;

        public static final int DEFAULT_ITERATIONS = 10000;

        public RandomDistribution(@NonNull CellularComponent template, int iterations) {
                        
            if(iterations<=0)
                throw new IllegalArgumentException("Must have at least one iteration");
            
            counts = new long[ options.getShellNumber()];
            for(int i=0; i< options.getShellNumber(); i++){
                counts[i] = 0;
            }

            List<IPoint> list = new ArrayList<IPoint>();
            for (int i = 0; i < iterations; i++) {
                list.add(createRandomPoint(template));
            }
            

            // Find the shell for these points in the template
            try {
                ShellDetector detector = new ShellDetector(template,  options.getShellNumber(), options.getErosionMethod(), true);
                for (IPoint p : list) {
                    int shell = detector.findShell(p);
                    if(shell>=0) // -1 for point not found
                        counts[shell]++;
                }

            } catch (ShellAnalysisException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }

        }

        public long[] getCounts() {
            return counts;
        }

        /**
         * Create a random point that lies within the template
         * 
         * @param template
         * @return
         */
        private IPoint createRandomPoint(@NonNull CellularComponent template) {

            Rectangle2D r = template.getBounds();

            // Make a random position in the rectangle
            // nextDouble is exclusive of the top value,
            // so add 1 to make it inclusive
            double rx = ThreadLocalRandom.current().nextDouble(r.getX(), r.getWidth() + 1);
            double ry = ThreadLocalRandom.current().nextDouble(r.getY(), r.getHeight() + 1);

            IPoint p = IPoint.makeNew(rx, ry);

            if (template.containsPoint(p))
                return p;
			return createRandomPoint(template);
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

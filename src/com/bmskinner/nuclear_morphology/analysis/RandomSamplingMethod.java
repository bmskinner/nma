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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class RandomSamplingMethod extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);
	
	private List<Double>       magnitudes = new ArrayList<>();
    private int                iterations;
    private PlottableStatistic stat;
    
    // the number of cells in the first subset
    private int                first;
    
 // the number of cells in the second subset
    private int                second;
    
    /**
     * Constructor
     * @param dataset the dataset to investigate
     * @param stat the stat to measure
     * @param iterations the number of iterations to run
     * @param first the size of the first subgroup 
     * @param second the size of the second subgroup 
     */
    public RandomSamplingMethod(IAnalysisDataset dataset, PlottableStatistic stat, int iterations, int first, int second) {
        super(dataset);
        this.stat = stat;
        this.iterations = iterations;
        this.first = first;
        this.second = second;

    }
    

    @Override
    public IAnalysisResult call() throws Exception {

        run();
        return new RandomSamplingResult(dataset, magnitudes);
    }
    
    public void run() throws Exception {

        // for each iteration
        LOGGER.fine("Beginning sampling");
        for (int i = 0; i < iterations; i++) {
            LOGGER.finest( "Sample " + i);
            // make a new collection randomly sampled to teh correct proportion
            ICellCollection[] collections = makeRandomSampledCollection(first, second);
            LOGGER.finest( "Made collection");

            // get the stat magnitude
            double value1 = collections[0].getMedian(stat, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
            double value2 = collections[1].getMedian(stat, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);

            double magnitude = value2 / value1;
            LOGGER.finest( "Found value");
            // add to a list
            magnitudes.add(magnitude);
                fireProgressEvent();
        }

    }

    private ICellCollection[] makeRandomSampledCollection(int firstSize, int secondSize) throws Exception {
        ICellCollection[] result = new ICellCollection[2];

        ICellCollection first = new VirtualCellCollection(dataset, "first");
        ICellCollection second = new VirtualCellCollection(dataset, "second");
        LOGGER.finer( "Created new collections");

        List<ICell> cells = new ArrayList<>(dataset.getCollection().getCells());
        Collections.shuffle(cells);
        LOGGER.finer( "Shuffled cells");

        for (int i = 0; i < firstSize; i++) {
            first.addCell(cells.get(i));
        }
        LOGGER.finer( "Added first set");
        for (int i = firstSize; i < firstSize + secondSize; i++) {
            second.addCell(cells.get(i));
        }
        LOGGER.finer( "Added second set");
        
        if(stat.equals(PlottableStatistic.VARIABILITY)) {
        	first.createProfileCollection();
        	second.createProfileCollection();
        }
        
        result[0] = first;
        result[1] = second;

        return result;

    }
    
    public class RandomSamplingResult extends DefaultAnalysisResult {
    	private List<Double> values = new ArrayList<>();
		public RandomSamplingResult(IAnalysisDataset d, List<Double> values) {
			super(d);
			this.values = values;
		}
		
		public List<Double> getValues(){
			return values;
		}
    	
    }

}

package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

public class RandomSamplingMethod extends AbstractAnalysisMethod {
	
	private List<Double>       magnitudes = new ArrayList<Double>();
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
        fine("Beginning sampling");
        for (int i = 0; i < iterations; i++) {
            finest("Sample " + i);
            // make a new collection randomly sampled to teh correct proportion
            ICellCollection[] collections = makeRandomSampledCollection(first, second);
            finest("Made collection");

            // get the stat magnitude
            double value1 = collections[0].getMedianStatistic(stat, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
            double value2 = collections[1].getMedianStatistic(stat, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);

            double magnitude = value2 / value1;
            finest("Found value");
            // add to a list
            magnitudes.add(magnitude);
                fireProgressEvent();
        }

    }

    private ICellCollection[] makeRandomSampledCollection(int firstSize, int secondSize) throws Exception {
        ICellCollection[] result = new ICellCollection[2];

        ICellCollection first = new VirtualCellCollection(dataset, "first");
        ICellCollection second = new VirtualCellCollection(dataset, "second");
        finer("Created new collections");

        List<ICell> cells = new ArrayList<>(dataset.getCollection().getCells());
        Collections.shuffle(cells);
        finer("Shuffled cells");

        for (int i = 0; i < firstSize; i++) {
            first.addCell(cells.get(i));
        }
        finer("Added first set");
        for (int i = firstSize; i < firstSize + secondSize; i++) {
            second.addCell(cells.get(i));
        }
        finer("Added second set");
        result[0] = first;
        result[1] = second;

        return result;

    }
    
    public class RandomSamplingResult extends DefaultAnalysisResult {
    	private List<Double> values = new ArrayList<Double>();
		public RandomSamplingResult(IAnalysisDataset d, List<Double> values) {
			super(d);
			this.values = values;
		}
		
		public List<Double> getValues(){
			return values;
		}
    	
    }

}

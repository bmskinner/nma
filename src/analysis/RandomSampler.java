package analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import components.Cell;
import components.CellCollection;
import components.generic.MeasurementScale;
import ij.IJ;
import stats.NucleusStatistic;

/**
 * The purpose of this class is to validate observed differences in nuclear statistics
 * within a dataset. The proportions are taken, and a random sampling of the input dataset
 * is made multiple times. The observed magnitude changes can then be compared to a randomly
 * sampled frequency distribution.
 * @author ben
 *
 */
public class RandomSampler {
	
	private AnalysisDataset dataset;
	private List<Double> magnitudes = new ArrayList<Double>();
	
	public RandomSampler(AnalysisDataset dataset){
		this.dataset = dataset;
	}
		
	public List<Double> run(NucleusStatistic stat, int iterations, int first, int second) throws Exception{
		
		// for each iteration
		for(int i=0; i<iterations; i++){
			// make a new collection randomly sampled to teh correct proportion
			List<CellCollection> collections = makeRandomSampledCollection(first, second);

			// get the stat magnitude
			double value1 =  collections.get(0).getMedianStatistic(stat, MeasurementScale.PIXELS);
			double value2 =  collections.get(1).getMedianStatistic(stat, MeasurementScale.PIXELS);
			double magnitude = value2 / value1;

			// add to a list
			magnitudes.add(magnitude);
			
//			IJ.log("Iteration "+i+": "+magnitude);
		}
		
		// generate the summary chart
		return magnitudes;
		
	}
	
	private List<CellCollection> makeRandomSampledCollection(int firstSize, int secondSize){
		List<CellCollection> result = new ArrayList<CellCollection>();
		
		CellCollection first  = new CellCollection(dataset, "first");
		CellCollection second = new CellCollection(dataset, "second");
		
		List<Cell> cells = dataset.getCollection().getCells();
		Collections.shuffle(cells);
		
		for(int i=0; i<firstSize; i++){
			first.addCell(new Cell(cells.get(i)));
		}
		for(int i=firstSize; i<firstSize+secondSize; i++){
			second.addCell(new Cell(cells.get(i)));
		}
		result.add(first);
		result.add(second);
		
		return result;
		
	}

}

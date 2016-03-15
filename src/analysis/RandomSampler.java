package analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class RandomSampler extends AnalysisWorker {
	
	private List<Double> magnitudes = new ArrayList<Double>();
	private int iterations;
	private NucleusStatistic stat;
	private int first;
	private int second;
	
	public RandomSampler(AnalysisDataset dataset, Logger logger, NucleusStatistic stat, int iterations, int first, int second){
		super(dataset);
		this.stat = stat;
		this.iterations = iterations;
		this.first = first;
		this.second = second;
		
		this.setProgressTotal(iterations);
		log(Level.FINE,"Created sampler for "+stat);
	}
	

	@Override
	protected Boolean doInBackground() throws Exception {
		boolean result = false;
		try {
			log(Level.FINE,"Running sampler");
			generateSamples();
			result = true;
			
		} catch (Exception e){
			result = false;
		}
		return result;
	}
	
	public List<Double> getResults(){
		return magnitudes;
	}
		
	public void generateSamples() throws Exception{
		
		// for each iteration
		log(Level.FINE,"Beginning sampling");
		for(int i=0; i<iterations; i++){
			log(Level.FINEST,"Sample "+i);
			// make a new collection randomly sampled to teh correct proportion
			List<CellCollection> collections = makeRandomSampledCollection(first, second);
			log(Level.FINEST,"Made collection");
			// get the stat magnitude
			double value1 =  collections.get(0).getMedianStatistic(stat, MeasurementScale.PIXELS);
			double value2 =  collections.get(1).getMedianStatistic(stat, MeasurementScale.PIXELS);
			
			// Always take the smaller as a proportion of the larger
			double magnitude = value1 > value2 ? value2/value1 : value1/value2; 
			
//			double magnitude = value2 / value1;
			log(Level.FINEST, "Found value");
			// add to a list
			magnitudes.add(magnitude);
			
			if( i%10==0){
				publish(i);
				System.gc(); // Suggest a clean up
			}
			
			// Release memory for collection
			collections = null;
			
		}	
		
	}
	
	private List<CellCollection> makeRandomSampledCollection(int firstSize, int secondSize) throws Exception{
		List<CellCollection> result = new ArrayList<CellCollection>();
		
		CellCollection first  = new CellCollection( this.getDataset(), "first");
		CellCollection second = new CellCollection( this.getDataset(), "second");
		log(Level.FINEST,"Created new collections");
		
		List<Cell> cells = this.getDataset().getCollection().getCells();
		Collections.shuffle(cells);
		log(Level.FINEST,"Shuffled cells");
		
		for(int i=0; i<firstSize; i++){
			first.addCell(new Cell(cells.get(i)));
		}
		log(Level.FINEST,"Added first set");
		for(int i=firstSize; i<firstSize+secondSize; i++){
			second.addCell(new Cell(cells.get(i)));
		}
		log(Level.FINEST,"Added second set");
		result.add(first);
		result.add(second);
		
		return result;
		
	
	}

}

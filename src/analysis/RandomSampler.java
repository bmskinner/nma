package analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import components.Cell;
import components.CellCollection;
import components.ICellCollection;
import components.active.VirtualCellCollection;
import components.generic.MeasurementScale;
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
	private int first; // the number of cells in the first subset
	private int second;// the number of cells in the second subset
	
	public RandomSampler(IAnalysisDataset dataset, NucleusStatistic stat, int iterations, int first, int second){
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
		fine("Beginning sampling");
		for(int i=0; i<iterations; i++){
			finest("Sample "+i);
			// make a new collection randomly sampled to teh correct proportion
			ICellCollection[]  collections = makeRandomSampledCollection(first, second);
			finest("Made collection");
			
			// get the stat magnitude
			double value1 =  collections[0].getMedianStatistic(stat, MeasurementScale.PIXELS);
			double value2 =  collections[1].getMedianStatistic(stat, MeasurementScale.PIXELS);
						
			double magnitude = value2 / value1;
			finest("Found value");
			// add to a list
			magnitudes.add(magnitude);
			
			if( i%10==0){
				publish(i);
			}			
		}	
		
	}
	
	private ICellCollection[] makeRandomSampledCollection(int firstSize, int secondSize) throws Exception{
		ICellCollection[] result = new ICellCollection[2];
		
		ICellCollection first  = new VirtualCellCollection( this.getDataset(), "first");
		ICellCollection second = new VirtualCellCollection( this.getDataset(), "second");
		finer("Created new collections");
		
		List<Cell> cells = new ArrayList(this.getDataset().getCollection().getCells());
		Collections.shuffle(cells);
		finer("Shuffled cells");
		
		for(int i=0; i<firstSize; i++){
			first.addCell(cells.get(i));
		}
		finer("Added first set");
		for(int i=firstSize; i<firstSize+secondSize; i++){
			second.addCell(cells.get(i));
		}
		finer("Added second set");
		result[0] = first;
		result[1] = second;
		
		return result;
		
	
	}

}

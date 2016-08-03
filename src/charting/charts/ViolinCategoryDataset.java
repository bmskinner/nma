package charting.charts;

import java.util.List;

import org.jfree.data.KeyedObjects2D;
import org.jfree.data.statistics.DefaultMultiValueCategoryDataset;

import stats.Stats;

/**
 * This provides dataset support for a violin plot, which has a box and whisker
 * plot plus a surrounding probability density function.
 * 
 * Each series needs to hold the boxplot quartiles, min, max, as well as a list of 
 * probabilities to render between min and max with even spacing
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ViolinCategoryDataset extends DefaultMultiValueCategoryDataset {
	
	private KeyedObjects2D boxplotData;
	
	public ViolinCategoryDataset(){
		
		super();
		boxplotData = new KeyedObjects2D();
		
	}
	
	public void addBoxplot(List<Number> values, Comparable rowKey, Comparable columnKey){
		if (values == null) {
            throw new IllegalArgumentException("Null 'values' argument.");
        }
        if (rowKey == null) {
            throw new IllegalArgumentException("Null 'rowKey' argument.");
        }
        if (columnKey == null) {
            throw new IllegalArgumentException("Null 'columnKey' argument.");
        }
        
        BoxplotData b = new BoxplotData(values);
        boxplotData.addObject(b, rowKey, columnKey);
        fireDatasetChanged();
	}
	
	public BoxplotData getBoxplot(Comparable rowKey, Comparable columnKey){
		return (BoxplotData) boxplotData.getObject(rowKey, columnKey);
	}
	
	
	
	/**
	 * Hold the values needed to construct a boxplot
	 * @author bms41
	 *
	 */
	public class BoxplotData {
		
		private Number min;
		private Number max;
		private Number q1;
		private Number q3;
		private Number mean;
		private Number median;
		
		
		public BoxplotData(List<Number> values){
			
			min    = Stats.min(values);
			max    = Stats.max(values);
			q1     = Stats.quartile(values, 25);
			q3     = Stats.quartile(values, 75);
			mean   = Stats.mean(values);
			median = Stats.quartile(values, 50);
			
		}


		public Number getMin() {
			return min;
		}


		public Number getMax() {
			return max;
		}


		public Number getQ1() {
			return q1;
		}


		public Number getQ3() {
			return q3;
		}


		public Number getMean() {
			return mean;
		}


		public Number getMedian() {
			return median;
		}
		
		public Number getRange(){
			return max.doubleValue() - min.doubleValue();
		}
		
		
	}
	
	

}

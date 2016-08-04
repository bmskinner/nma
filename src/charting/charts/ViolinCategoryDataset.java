package charting.charts;

import java.util.List;

import logging.Loggable;

import org.jfree.data.KeyedObjects2D;
import org.jfree.data.Range;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
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
public class ViolinCategoryDataset extends DefaultBoxAndWhiskerCategoryDataset implements Loggable {
	
	private KeyedObjects2D pdfData;
	private KeyedObjects2D ranges; // hold the min and max for each set of values for pdf step calculation
		
	public ViolinCategoryDataset(){
		
		super();	
		pdfData = new KeyedObjects2D();
		ranges  = new KeyedObjects2D();
	}
	
	@Override
	public void add(List list, Comparable rowKey, Comparable columnKey){
		
		if (list == null) {
            throw new IllegalArgumentException("Null 'list' argument.");
        }
        if (rowKey == null) {
            throw new IllegalArgumentException("Null 'rowKey' argument.");
        }
        if (columnKey == null) {
            throw new IllegalArgumentException("Null 'columnKey' argument.");
        }
        
		Number min = Stats.min(list);
		Number max = Stats.max(list);
		Range r = new Range(min.doubleValue(), max.doubleValue());
		ranges.addObject(r, rowKey, columnKey);
		super.add(list, rowKey, columnKey);
		
	}
	
	
	public void addProbabilities(List<Number> values, Comparable rowKey, Comparable columnKey){
		if (values == null) {
            throw new IllegalArgumentException("Null 'values' argument.");
        }
        if (rowKey == null) {
            throw new IllegalArgumentException("Null 'rowKey' argument.");
        }
        if (columnKey == null) {
            throw new IllegalArgumentException("Null 'columnKey' argument.");
        }
        
        pdfData.addObject(values, rowKey, columnKey);
                
        fireDatasetChanged();
	}
	
	public List<Number> getPdfValues(Comparable rowKey, Comparable columnKey){
		return (List<Number>) pdfData.getObject(rowKey, columnKey);
	}
	
	public List<Number> getPdfValues(int row, int column){
		return (List<Number>) pdfData.getObject(row, column);
	}
	
	public double getMax(Comparable rowKey, Comparable columnKey){
		Range r = (Range) ranges.getObject(rowKey, columnKey);
		if(r == null){
			log("Error: range is null");
			return 0;
		}
		return r.getUpperBound();
	}
	
	public double getMax(int row, int column){
		Range r = (Range) ranges.getObject(row, column);
		if(r == null){
			log("Error: range is null");
			return 0;
		}
		return r.getUpperBound();
	}
	
	
	public double getMin(Comparable rowKey, Comparable columnKey){
		
		Range r = (Range) ranges.getObject(rowKey, columnKey);
		if(r == null){
			log("Error: range is null");
			return 0;
		}
		return r.getLowerBound();
	}
	
	public double getMin(int row, int column){
		Range r = (Range) ranges.getObject(row, column);
		if(r == null){
			log("Error: range is null");
			return 0;
		}
		return r.getLowerBound();
	}
	
	
//	/**
//	 * Hold the values needed to construct a boxplot
//	 * @author bms41
//	 *
//	 */
//	public class BoxplotData {
//		
//		private Number min;
//		private Number max;
//		private Number q1;
//		private Number q3;
//		private Number mean;
//		private Number median;
//		
//		
//		public BoxplotData(List<Number> values){
//			
//			min    = Stats.min(values);
//			max    = Stats.max(values);
//			q1     = Stats.quartile(values, 25);
//			q3     = Stats.quartile(values, 75);
//			mean   = Stats.mean(values);
//			median = Stats.quartile(values, 50);
//			
//		}
//
//
//		public Number getMin() {
//			return min;
//		}
//
//
//		public Number getMax() {
//			return max;
//		}
//
//
//		public Number getQ1() {
//			return q1;
//		}
//
//
//		public Number getQ3() {
//			return q3;
//		}
//
//
//		public Number getMean() {
//			return mean;
//		}
//
//
//		public Number getMedian() {
//			return median;
//		}
//		
//		public Number getRange(){
//			return max.doubleValue() - min.doubleValue();
//		}
//		
//		public String toString(){
//			StringBuilder b = new StringBuilder();
//			b.append(min);
//			b.append(" - ");
//			b.append(q1);
//			b.append(" - ");
//			b.append(median);
//			b.append(" - ");
//			b.append(q3);
//			b.append(" - ");
//			b.append(max);
//			return b.toString();
//		}
//		
//	}
	
	

}

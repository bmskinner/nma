package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.KeyedObjects2D;
import org.jfree.data.Range;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import com.bmskinner.nuclear_morphology.logging.Loggable;

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
public class ViolinCategoryDataset extends ExportableBoxAndWhiskerCategoryDataset implements Loggable {
	
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
        
//		Number min = Stats.min(list);
//		Number max = Stats.max(list);
//		Range r = new Range(min.doubleValue(), max.doubleValue());
//		ranges.addObject(r, rowKey, columnKey);
		super.add(list, rowKey, columnKey);
		
	}
	
	public boolean hasProbabilities(Comparable<?> r, Comparable<?> c){

		List<Number> values = (List<Number>) pdfData.getObject( (Comparable) r, (Comparable) c);
		if(values != null){
			return values.size()>0;
		} else {
			return false;
		}
	}
	
	public boolean hasProbabilities(int r, int c){

		@SuppressWarnings("unchecked")
		List<Number> values = (List<Number>) pdfData.getObject( r,  c);
		if(values != null){
			return values.size()>0;
		} else {
			return false;
		}
	}
	
	public boolean hasProbabilities(){
		
		
		int total = 0;
		for( Object c : ranges.getColumnKeys()){
			
			for( Object r : ranges.getRowKeys()){
				
				List<Number> values = (List<Number>) pdfData.getObject( (Comparable) r, (Comparable) c);
				if(values != null){
					total +=values.size();
				}
			}
			
		}
		return total>0;
	}
	
	public void addProbabilityRange(Range r, Comparable<?> rowKey, Comparable<?> columnKey){
		ranges.addObject(r, rowKey, columnKey);
	}
	
	/**
	 * Fetch the RangeAxis range covering all probabilities in the dataset
	 * @return
	 */
	public Range getProbabiltyRange(){
		
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		for( Object c : ranges.getColumnKeys()){
			
			for( Object r : ranges.getRowKeys()){
				
				Range range = (Range) ranges.getObject( (Comparable<?>) r, (Comparable<?>) c);
				if(range != null){
					if( range.getLowerBound()<min){
						min = range.getLowerBound();
					}

					if( range.getUpperBound()>max){
						max = range.getUpperBound();
					}
				}
				
			}
			
		}
		
		if(min==Double.MAX_VALUE || max==Double.MIN_VALUE){
			return null;
		}
		
		double range = max - min;
		return new Range(min - (range /10), max+ (range /10)); // add 10% to each end for space
	}
	
	
	public void addProbabilities(List<Number> values, Comparable<?> rowKey, Comparable<?> columnKey){
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
	
	public List<Number> getPdfValues(Comparable<?> rowKey, Comparable<?> columnKey){
		return (List<Number>) pdfData.getObject(rowKey, columnKey);
	}
	
	public List<Number> getPdfValues(int row, int column){
		return (List<Number>) pdfData.getObject(row, column);
	}
	
	public double getMax(Comparable<?> rowKey, Comparable<?> columnKey){
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
	
	
	public double getMin(Comparable<?> rowKey, Comparable<?> columnKey){
		
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
	
	@Override
	public List<?> getOutliers(int row, int column){
		return new ArrayList<Object>(); // don't display outliers on violin plots
		
	}
}

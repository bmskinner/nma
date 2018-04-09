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


package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

import org.jfree.data.KeyedObjects2D;
import org.jfree.data.Range;

import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This provides dataset support for a violin plot, which has a box and whisker
 * plot plus a surrounding probability density function.
 * 
 * Each series needs to hold the boxplot quartiles, min, max, as well as a list
 * of probabilities to render between min and max with even spacing
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ViolinCategoryDataset extends ExportableBoxAndWhiskerCategoryDataset implements Loggable {

    private KeyedObjects2D pdfData;
    private KeyedObjects2D ranges; // hold the min and max for each set of
                                   // values for pdf step calculation
    private double maxPdfValue = Double.NaN;

    public ViolinCategoryDataset() {

        super();
        pdfData = new KeyedObjects2D();
        ranges = new KeyedObjects2D();
    }

    @Override
    public void add(List list, Comparable rowKey, Comparable columnKey) {

        if (list == null)
            throw new IllegalArgumentException("Null 'list' argument.");
        if (rowKey == null)
            throw new IllegalArgumentException("Null 'rowKey' argument.");
        if (columnKey == null)
            throw new IllegalArgumentException("Null 'columnKey' argument.");
        super.add(list, rowKey, columnKey);
    }

    public boolean hasProbabilities(Comparable<?> r, Comparable<?> c) {
    	double[] values = (double[]) pdfData.getObject((Comparable) r, (Comparable) c);
        return values!=null && values.length>0;
    }

    public boolean hasProbabilities(int r, int c) {
    	if(pdfData==null)
    		return false;

        double[] values = (double[]) pdfData.getObject(r, c);
        return values!=null && values.length>0;
    }

    public boolean hasProbabilities() {

        int total = 0;
        for (Object c : ranges.getColumnKeys()) {
            for (Object r : ranges.getRowKeys()) {
                double[] values = (double[]) pdfData.getObject((Comparable) r, (Comparable) c);
                if (values != null)
                    total += values.length;
            }
        }
        return total > 0;
    }

    public void addProbabilityRange(Range r, Comparable<?> rowKey, Comparable<?> columnKey) {
        ranges.addObject(r, rowKey, columnKey);
    }

    /**
     * Fetch the RangeAxis range covering all probabilities in the dataset
     * 
     * @return
     */
    public Range getProbabiltyRange() {

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (Object c : ranges.getColumnKeys()) {

            for (Object r : ranges.getRowKeys()) {

                Range range = (Range) ranges.getObject((Comparable<?>) r, (Comparable<?>) c);
                if (range != null) {
                    if (range.getLowerBound() < min) {
                        min = range.getLowerBound();
                    }

                    if (range.getUpperBound() > max) {
                        max = range.getUpperBound();
                    }
                }

            }

        }

        if (min == Double.MAX_VALUE || max == -Double.MAX_VALUE)
            return null;

        double range = max - min;
        return new Range(min - (range / 10), max + (range / 10)); // add 10% to
                                                                  // each end
                                                                  // for space
    }

    public void addProbabilities(double[] values, Comparable<?> rowKey, Comparable<?> columnKey) {
        if (values == null)
            throw new IllegalArgumentException("Null 'values' argument.");
        if (rowKey == null)
            throw new IllegalArgumentException("Null 'rowKey' argument.");
        if (columnKey == null)
            throw new IllegalArgumentException("Null 'columnKey' argument.");

        pdfData.addObject(values, rowKey, columnKey);
        maxPdfValue = getMaxPdfValue();
        fireDatasetChanged();
    }

    /**
     * Get the probability values for the given row and column
     * @param rowKey
     * @param columnKey
     * @return
     */
    public double[] getPdfValues(Comparable<?> rowKey, Comparable<?> columnKey) {
        return (double[]) pdfData.getObject(rowKey, columnKey);
    }

    /**
     * Get the probability values for the given row and column
     * @param rowKey
     * @param columnKey
     * @return
     */
    public double[] getPdfValues(int row, int column) {
        return (double[]) pdfData.getObject(row, column);
    }
    
    /**
     * Get the maximum probability value across all rows and columns
     * @return
     */
    public double getMaxPdfValue(){
    	if(!Double.isNaN(maxPdfValue))
    		return maxPdfValue;
    	
    	maxPdfValue = -Double.MAX_VALUE;
    	for (int c=0; c<pdfData.getColumnCount(); c++) {
    		for (int r=0; r<pdfData.getRowCount(); r++) {
    			double[] data = (double[]) pdfData.getObject(r, c);
    			double d = DoubleStream.of(data).max().orElse(Double.NaN);
    			maxPdfValue = d>maxPdfValue?d:maxPdfValue;
    		}
    	}
    	return maxPdfValue;
    }

    public double getMax(Comparable<?> rowKey, Comparable<?> columnKey) {
        Range r = (Range) ranges.getObject(rowKey, columnKey);
        if (r == null)
            return 0;
        return r.getUpperBound();
    }

    public double getMax(int row, int column) {
    	if(column>=ranges.getColumnCount())
    		return 0;
    	if(row>=ranges.getRowCount())
    		return 0;
        Range r = (Range) ranges.getObject(row, column);
        if (r == null)
            return 0;
        return r.getUpperBound();
    }

    public double getMin(Comparable<?> rowKey, Comparable<?> columnKey) {

        Range r = (Range) ranges.getObject(rowKey, columnKey);
        if (r == null)
            return 0;
        return r.getLowerBound();
    }

    public double getMin(int row, int column) {
    	if(column>=ranges.getColumnCount())
    		return 0;
    	if(row>=ranges.getRowCount())
    		return 0;
        Range r = (Range) ranges.getObject(row, column);
        if (r == null)
            return 0;
        return r.getLowerBound();
    }

    @Override
    public List<?> getOutliers(int row, int column) {
        return new ArrayList<Object>(); // don't display outliers on violin
                                        // plots
    }
    
    @Override
    public double getRangeLowerBound(boolean includeInterval){
    	return getRangeBounds(includeInterval).getLowerBound();
    }
    
    @Override
    public double getRangeUpperBound(boolean includeInterval){
    	return getRangeBounds(includeInterval).getUpperBound();
    }
    
    @Override
    public Range getRangeBounds(boolean includeInterval){
    	Range r = super.getRangeBounds(includeInterval);
    	return Range.combine(r, getProbabiltyRange());
    }
}

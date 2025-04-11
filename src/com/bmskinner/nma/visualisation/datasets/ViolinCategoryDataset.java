package com.bmskinner.nma.visualisation.datasets;

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.data.KeyedObjects2D;
import org.jfree.data.Range;

import weka.estimators.KernelEstimator;

/**
 * This provides dataset support for a violin plot, which has a box and whisker
 * plot plus a surrounding probability density function.
 * 
 * Each series needs to hold the boxplot quartiles, min, max, as well as a list
 * of probabilities to render between min and max with even spacing
 * 
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public class ViolinCategoryDataset extends ExportableBoxAndWhiskerCategoryDataset {

	private static final int STEP_COUNT = 100;

	private KeyedObjects2D pdfData;
	private KeyedObjects2D ranges; // hold the min and max for each set of
									// values for pdf step calculation
	private double maxPdfValue = Double.NaN;

	/**
	 * Default constructor. Create an empty dataset.
	 * 
	 */
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
		calculateProbabilities(list, rowKey, columnKey);
	}

	/**
	 * Test if the given data has probabilities associated
	 * 
	 * @param r the row key
	 * @param c the column key
	 * @return
	 */
	public boolean hasProbabilities(@NonNull Comparable<?> r, @NonNull Comparable<?> c) {
		double[] values = (double[]) pdfData.getObject(r, c);
		return values != null && values.length > 0;
	}

	/**
	 * Test if the given data has probabilities associated
	 * 
	 * @param r the row index
	 * @param c the column index
	 * @return
	 */
	public boolean hasProbabilities(int r, int c) {
		if (pdfData == null)
			return false;

		double[] values = (double[]) pdfData.getObject(r, c);
		return values != null && values.length > 0;
	}

	/**
	 * Test if any row or column in the dataset data has probabilities associated
	 * 
	 * @return
	 */
	public boolean hasProbabilities() {

		int total = 0;
		for (Object c : ranges.getColumnKeys()) {
			for (Object r : ranges.getRowKeys()) {
				double[] values = (double[]) pdfData.getObject((Comparable<?>) r,
						(Comparable<?>) c);
				if (values != null)
					total += values.length;
			}
		}
		return total > 0;
	}

	/**
	 * Add the given range for probabilities
	 * 
	 * @param r
	 * @param rowKey
	 * @param columnKey
	 */
	private void addProbabilityRange(@NonNull Range r, @NonNull Comparable<?> rowKey,
			@NonNull Comparable<?> columnKey) {
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

	/**
	 * Get the probability values for the given row and column
	 * 
	 * @param rowKey
	 * @param columnKey
	 * @return
	 */
	public double[] getPdfValues(@NonNull Comparable<?> rowKey, @NonNull Comparable<?> columnKey) {
		return (double[]) pdfData.getObject(rowKey, columnKey);
	}

	/**
	 * Get the probability values for the given row and column
	 * 
	 * @param rowKey
	 * @param columnKey
	 * @return
	 */
	public double[] getPdfValues(int row, int column) {
		return (double[]) pdfData.getObject(row, column);
	}

	private double calculateMaxPdfValue() {
		maxPdfValue = -Double.MAX_VALUE;
		for (int c = 0; c < pdfData.getColumnCount(); c++) {
			for (int r = 0; r < pdfData.getRowCount(); r++) {
				double[] arr = (double[]) pdfData.getObject(r, c);
				if (arr != null) {
					double d = DoubleStream.of(arr).max().orElse(Double.NaN);
					maxPdfValue = d > maxPdfValue ? d : maxPdfValue;
				}
			}
		}
		return maxPdfValue;
	}

	/**
	 * Get the maximum probability value across all rows and columns
	 * 
	 * @return
	 */
	public double getMaxPdfValue() {
		if (!Double.isNaN(maxPdfValue))
			return maxPdfValue;

		return calculateMaxPdfValue();
	}

	/**
	 * Get the maximum value in the dataset
	 * 
	 * @param rowKey
	 * @param columnKey
	 * @return
	 */
	public double getMax(Comparable<?> rowKey, Comparable<?> columnKey) {
		Range r = (Range) ranges.getObject(rowKey, columnKey);
		if (r == null)
			return 0;
		return r.getUpperBound();
	}

	/**
	 * Get the minimum value in the dataset
	 * 
	 * @param rowKey
	 * @param columnKey
	 * @return
	 */
	public double getMax(int row, int column) {
		if (column >= ranges.getColumnCount())
			return 0;
		if (row >= ranges.getRowCount())
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
		if (column >= ranges.getColumnCount())
			return 0;
		if (row >= ranges.getRowCount())
			return 0;
		Range r = (Range) ranges.getObject(row, column);
		if (r == null)
			return 0;
		return r.getLowerBound();
	}

	@Override
	public List<?> getOutliers(int row, int column) {
		return new ArrayList<>(); // don't display outliers on violin
									// plots
	}

	@Override
	public double getRangeLowerBound(boolean includeInterval) {
		return getRangeBounds(includeInterval).getLowerBound();
	}

	@Override
	public double getRangeUpperBound(boolean includeInterval) {
		return getRangeBounds(includeInterval).getUpperBound();
	}

	@Override
	public Range getRangeBounds(boolean includeInterval) {
		Range r = super.getRangeBounds(includeInterval);
		return Range.combine(r, getProbabiltyRange());
	}

	protected void addProbabilities(double[] values, Comparable<?> rowKey,
			Comparable<?> columnKey) {
		pdfData.addObject(values, rowKey, columnKey);
		calculateMaxPdfValue();
		fireDatasetChanged();
	}

	/**
	 * Calculate the probabilities at set intervals along the range of the data. The
	 * intervals by default are {@code range / STEP_SIZE}.
	 * 
	 * @param list
	 * @param rowKey
	 * @param colKey
	 */
	protected synchronized void calculateProbabilities(List<Number> list, Comparable<?> rowKey,
			Comparable<?> colKey) {

		if (list == null)
			throw new IllegalArgumentException("Null 'values' argument.");
		if (rowKey == null)
			throw new IllegalArgumentException("Null 'rowKey' argument.");
		if (colKey == null)
			throw new IllegalArgumentException("Null 'columnKey' argument.");

		double[] pdfValues = new double[STEP_COUNT + 1];

		if (list.isEmpty()) {
			Range r = new Range(0, 0);
			addProbabilityRange(r, rowKey, colKey);
			addProbabilities(pdfValues, rowKey, colKey);
			return;
		}

		double total = list.stream().mapToDouble(Number::doubleValue).sum();
		double min = list.stream().mapToDouble(Number::doubleValue).min().orElse(0);
		double max = list.stream().mapToDouble(Number::doubleValue).max().orElse(0);

		// If all values are the same, min==max, and there will be a step error
		// calculating values between them for pdf
		if (list.size() > 2 && total > 0 && min < max) { // don't bother with a dataset of a single
															// cell, or if the stat is not present

			double stepSize = (max - min) / STEP_COUNT;

			KernelEstimator est = createProbabililtyKernel(list, 0.001);

			for (int i = 0; i < STEP_COUNT; i++) {
				double v = min + (stepSize * i);
				pdfValues[i] = est.getProbability(v);
			}
			// ensure last value in the array is at yMax; allows the renderer to have a flat
			// top
			pdfValues[STEP_COUNT] = est.getProbability(max);

			Range r = new Range(min, max);
			addProbabilityRange(r, rowKey, colKey);
		}
		addProbabilities(pdfValues, rowKey, colKey);

	}

	/**
	 * Create a probability kernel estimator for an array of values
	 * 
	 * @param values   the array of values
	 * @param binWidth the precision of the KernelEstimator
	 * @return
	 */
	public static KernelEstimator createProbabililtyKernel(List<Number> values, double binWidth) {
		KernelEstimator est = new KernelEstimator(binWidth);
		for (Number d : values) {
			est.addValue(d.doubleValue(), 1);
		}
		return est;
	}
}

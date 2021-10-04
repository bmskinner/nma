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
package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jfree.chart.util.PublicCloneable;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * A dataset for charting that stores values as floats rather than doubles.
 * Modified from the JFreeChart DefaultXYDataset
 *
 */
public class FloatXYDataset extends AbstractXYDataset 
    implements XYDataset, PublicCloneable {

        /**
         * Storage for the series keys.  This list must be kept in sync with the
         * seriesList.
         */
        private List seriesKeys;
        private List<Integer> datasetIndexes;
        
        /** 
         * Storage for the series in the dataset.  We use a list because the
         * order of the series is significant.  This list must be kept in sync 
         * with the seriesKeys list.
         */ 
        private List seriesList;

        /**
         * Creates a new <code>FloatXYDataset</code> instance, initially 
         * containing no data.
         */
        public FloatXYDataset() {
        	seriesKeys     = new ArrayList(); 
        	datasetIndexes = new ArrayList<>(); 
            seriesList     = new ArrayList();    
        }

        /**
         * Returns the number of series in the dataset.
         *
         * @return The series count.
         */
        @Override
		public int getSeriesCount() {
            return this.seriesList.size();
        }

        /**
         * Returns the key for a series.  
         *
         * @param series  the series index (in the range <code>0</code> to 
         *     <code>getSeriesCount() - 1</code>).
         *
         * @return The key for the series.
         * 
         * @throws IllegalArgumentException if <code>series</code> is not in the 
         *     specified range.
         */
        @Override
		public Comparable<?> getSeriesKey(int series) {
            if (series < 0 || series >= getSeriesCount())
                throw new IllegalArgumentException("Series index out of bounds");
            return (Comparable<?>) this.seriesKeys.get(series);
        }
        
        public int getDatasetIndex(Comparable<?> seriesKey) {
            return datasetIndexes.get(indexOf(seriesKey));
        }

        /**
         * Returns the index of the series with the specified key, or -1 if there 
         * is no such series in the dataset.
         * 
         * @param seriesKey  the series key (<code>null</code> permitted).
         * 
         * @return The index, or -1.
         */
        @Override
		public int indexOf(Comparable seriesKey) {
            return this.seriesKeys.indexOf(seriesKey);
        }

        /**
         * Returns the order of the domain (x-) values in the dataset.  In this
         * implementation, we cannot guarantee that the x-values are ordered, so 
         * this method returns <code>DomainOrder.NONE</code>.
         * 
         * @return <code>DomainOrder.NONE</code>.
         */
        @Override
		public DomainOrder getDomainOrder() {
            return DomainOrder.NONE;
        }

        /**
         * Returns the number of items in the specified series.
         * 
         * @param series  the series index (in the range <code>0</code> to 
         *     <code>getSeriesCount() - 1</code>).
         * 
         * @return The item count.
         * 
         * @throws IllegalArgumentException if <code>series</code> is not in the 
         *     specified range.
         */
        @Override
		public int getItemCount(int series) {
            if (series < 0 || series >= getSeriesCount())
                throw new IllegalArgumentException("Series index out of bounds");
            float[][] seriesArray = (float[][]) this.seriesList.get(series);
            return seriesArray[0].length;
        }

        /**
         * Returns the x-value for an item within a series.
         * 
         * @param series  the series index (in the range <code>0</code> to 
         *     <code>getSeriesCount() - 1</code>).
         * @param item  the item index (in the range <code>0</code> to 
         *     <code>getItemCount(series)</code>).
         *     
         * @return The x-value.
         * 
         * @throws ArrayIndexOutOfBoundsException if <code>series</code> is not 
         *     within the specified range.
         * @throws ArrayIndexOutOfBoundsException if <code>item</code> is not 
         *     within the specified range.
         * 
         * @see #getX(int, int)
         */
        @Override
		public double getXValue(int series, int item) {
            float[][] seriesData = (float[][]) this.seriesList.get(series);
            return seriesData[0][item];
        }

        /**
         * Returns the x-value for an item within a series.
         * 
         * @param series  the series index (in the range <code>0</code> to 
         *     <code>getSeriesCount() - 1</code>).
         * @param item  the item index (in the range <code>0</code> to 
         *     <code>getItemCount(series)</code>).
         *     
         * @return The x-value.
         * 
         * @throws ArrayIndexOutOfBoundsException if <code>series</code> is not 
         *     within the specified range.
         * @throws ArrayIndexOutOfBoundsException if <code>item</code> is not 
         *     within the specified range.
         * 
         * @see #getXValue(int, int)
         */
        @Override
		public Number getX(int series, int item) {
            return new Double(getXValue(series, item));
        }

        /**
         * Returns the y-value for an item within a series.
         * 
         * @param series  the series index (in the range <code>0</code> to 
         *     <code>getSeriesCount() - 1</code>).
         * @param item  the item index (in the range <code>0</code> to 
         *     <code>getItemCount(series)</code>).
         *     
         * @return The y-value.
         * 
         * @throws ArrayIndexOutOfBoundsException if <code>series</code> is not 
         *     within the specified range.
         * @throws ArrayIndexOutOfBoundsException if <code>item</code> is not 
         *     within the specified range.
         * 
         * @see #getY(int, int)
         */
        @Override
		public double getYValue(int series, int item) {
            float[][] seriesData = (float[][]) this.seriesList.get(series);
            return seriesData[1][item];
        }

        /**
         * Returns the y-value for an item within a series.
         * 
         * @param series  the series index (in the range <code>0</code> to 
         *     <code>getSeriesCount() - 1</code>).
         * @param item  the item index (in the range <code>0</code> to 
         *     <code>getItemCount(series)</code>).
         *     
         * @return The y-value.
         * 
         * @throws ArrayIndexOutOfBoundsException if <code>series</code> is not 
         *     within the specified range.
         * @throws ArrayIndexOutOfBoundsException if <code>item</code> is not 
         *     within the specified range.
         *     
         * @see #getX(int, int)
         */
        @Override
		public Number getY(int series, int item) {
            return new Double(getYValue(series, item));
        }

        /**
         * Adds a series or if a series with the same key already exists replaces
         * the data for that series, then sends a {@link DatasetChangeEvent} to 
         * all registered listeners.
         * 
         * @param seriesKey  the series key (<code>null</code> not permitted).
         * @param data  the data (must be an array with length 2, containing two 
         *     arrays of equal length, the first containing the x-values and the
         *     second containing the y-values). 
         */
        public void addSeries(Comparable<?> seriesKey, float[][] data, int datasetIndex) {
            if (seriesKey == null)
                throw new IllegalArgumentException("The 'seriesKey' cannot be null.");
            if (data == null)
                throw new IllegalArgumentException("The 'data' is null.");
            if (data.length != 2)
                throw new IllegalArgumentException("The 'data' array must have length == 2.");
            if (data[0].length != data[1].length)
                throw new IllegalArgumentException("The 'data' array must contain two arrays with equal length.");
            int seriesIndex = indexOf(seriesKey);
            if (seriesIndex == -1) {  // add a new series
                this.seriesKeys.add(seriesKey);
                this.seriesList.add(data);
                this.datasetIndexes.add(datasetIndex);
            }
            else {  // replace an existing series
                this.seriesList.remove(seriesIndex);
                this.seriesList.add(seriesIndex, data);
            }
            notifyListeners(new DatasetChangeEvent(this, this));
        }

        /**
         * Removes a series from the dataset, then sends a 
         * {@link DatasetChangeEvent} to all registered listeners.
         * 
         * @param seriesKey  the series key (<code>null</code> not permitted).
         * 
         */
        public void removeSeries(Comparable seriesKey) {
            int seriesIndex = indexOf(seriesKey);
            if (seriesIndex >= 0) {
                this.seriesKeys.remove(seriesIndex);
                this.seriesList.remove(seriesIndex);
                notifyListeners(new DatasetChangeEvent(this, this));
            }
        }

        /**
         * Tests this <code>DefaultXYDataset</code> instance for equality with an
         * arbitrary object.  This method returns <code>true</code> if and only if:
         * <ul>
         * <li><code>obj</code> is not <code>null</code>;</li>
         * <li><code>obj</code> is an instance of 
         *         <code>DefaultXYDataset</code>;</li>
         * <li>both datasets have the same number of series, each containing 
         *         exactly the same values.</li>
         * </ul>
         * 
         * @param obj  the object (<code>null</code> permitted).
         * 
         * @return A boolean.
         */
        @Override
		public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof FloatXYDataset))
                return false;

            FloatXYDataset that = (FloatXYDataset) obj;
            if (!this.seriesKeys.equals(that.seriesKeys))
                return false;
            for (int i = 0; i < this.seriesList.size(); i++) {
                float[][] d1 = (float[][]) this.seriesList.get(i);
                float[][] d2 = (float[][]) that.seriesList.get(i);
                float[] d1x = d1[0];
                float[] d2x = d2[0];
                if (!Arrays.equals(d1x, d2x)) {
                    return false;
                }
                float[] d1y = d1[1];
                float[] d2y = d2[1];            
                if (!Arrays.equals(d1y, d2y)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns a hash code for this instance.
         * 
         * @return A hash code.
         */
        @Override
		public int hashCode() {
            int result;
            result = this.seriesKeys.hashCode();
            result = 29 * result + this.seriesList.hashCode();
            return result;
        }

        /**
         * Creates an independent copy of this dataset.
         * 
         * @return The cloned dataset.
         * 
         * @throws CloneNotSupportedException if there is a problem cloning the
         *     dataset (for instance, if a non-cloneable object is used for a
         *     series key).
         */
        @Override
		public Object clone() throws CloneNotSupportedException {
            FloatXYDataset clone = (FloatXYDataset) super.clone();
            clone.seriesKeys = new ArrayList(this.seriesKeys);
            clone.seriesList = new ArrayList(this.seriesList.size());
            clone.datasetIndexes = new ArrayList(seriesList);
            for (int i = 0; i < this.seriesList.size(); i++) {
                float[][] data = (float[][]) this.seriesList.get(i);
                float[] x = data[0];
                float[] y = data[1];
                float[] xx = new float[x.length];
                float[] yy = new float[y.length];
                System.arraycopy(x, 0, xx, 0, x.length);
                System.arraycopy(y, 0, yy, 0, y.length);
                clone.seriesList.add(i, new float[][] {xx, yy});
            }
            return clone;
        }

    }

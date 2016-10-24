/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package components.generic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import analysis.profiles.ProfileException;
import stats.Quartile;
import logging.Loggable;

/**
 * This is for testing a replacement of the profile aggregate
 * using arrays instead of collections
 * @author bms41
 *
 */
public class BetterProfileAggregate implements Loggable, Serializable {
	
	private static final long serialVersionUID = 1L;
	private final double[][] aggregate; // the values samples per profile
	private final int length; // the length of the aggregate (the median array length of a population usually)
	private final int profileCount;
	private transient int counter = 0; // track the number of profiles added to the aggregate

	public BetterProfileAggregate(final int length, final int profileCount){
		
		this.length = length;
		this.profileCount = profileCount;
		
		aggregate = new double[length][profileCount];

	}
	
		
	public void addValues(final Profile profile) throws ProfileException {
		
		if(counter>=profileCount){
			throw new ProfileException("Aggregate is full");
		}
		
		/*
		 * Make the profile the desired length, sample
		 * each point and add it to the aggregate
		 */
		
		Profile interpolated = profile.interpolate(length);
		for(int i=0; i<length; i++){
			double d = interpolated.get(i);
			aggregate[i][counter] = d;
			
		}
		
		counter++;
		
	}
	
	public int length(){
		return length;
	}
	
	public Profile getMedian(){
		return calculateQuartile(Quartile.MEDIAN);
	}

	public Profile getQuartile(double quartile){
		
		return calculateQuartile(quartile);
	}
	
	/**
	 * Get the angle values at the given position in the aggregate.
	 * @param position the position to search. Must be between 0 and the length of the aggregate.
	 * @return an unsorted array of the values at the given position
	 */
	public double[] getValuesAtPosition(int position) {
		if(position < 0 || position > length ){
			throw new IllegalArgumentException("Desired position is out of range: "+position);
		}
		return getValuesAtIndex(position);
	}
	
	/**
	 * Get the angle values at the given position in the aggregate. If the requested
	 * position is not an integer, the closest integer index values are returned
	 * @param position the position to search. Must be between 0 and the length of the aggregate.
	 * @return an unsorted array of the values at the given position
	 */
	public double[] getValuesAtPosition(double position) {
		if(position < 0 || position > length ){
			throw new IllegalArgumentException("Desired x-position is out of range: "+position);
		}
		
		// Choose the best position to return
		int index = (int) Math.round(position);
		return getValuesAtIndex(index);
	}
	
	/**
	 * Get the x-axis positions of the centre of each bin.
	 * @return the Profile of positions
	 */
	public Profile getXPositions(){
		double[] result = new double[length];
		
		double profileIncrement = 100d / (double) length;
		// start counting half a bin below zero
		// this sets the value to the bin centre
		double x = -profileIncrement/2;
		
		// add the bin size for each positions
		for(int i=0;i<length;i++){
			x += profileIncrement;
			result[i] = x;
		}
		return new Profile(result);
	}
	
	public List<Double> getXKeyset(){
		List<Double> result = new ArrayList<Double>(length);
		for(int i=0;i<length;i++){
			double profilePosition = (double) i / (double) length;
			result.add(profilePosition);
		}

		return result;
	}
	
	
	/*
	 * 
	 * PRIVATE METHODS
	 * 
	 */
	
	/**
	 * Get the values from each profile at the given position in the aggregate
	 * @param i
	 * @return
	 */
	private double[] getValuesAtIndex(int i){

		double[] values = new double[profileCount];

		for(int n=0; n<profileCount; n++){
			values[n] = aggregate[i][n];
		}

		return values;
	}
	
	private Profile calculateQuartile(double quartile) {
		double[] medians = new double[length];
		
		for(int i=0; i<length; i++){
			
			double[] values = getValuesAtIndex(i);
			
			double q = new Quartile(values, quartile).doubleValue();
			
			medians[i] = q;
			
		}
		
		return new Profile(medians);

	}

}

/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.components.generic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.transform.stream.StreamSource;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Quartile;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter.ArrayConversionException;

/**
 * This is for testing a replacement of the profile aggregate
 * using arrays instead of collections. Not serializable.
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultProfileAggregate implements Loggable, IProfileAggregate {

	private final float[][] aggregate; // the values samples per profile
	private final int length; // the length of the aggregate (the median array length of a population usually)
	private final int profileCount;
	
	private int counter = 0; // track the number of profiles added to the aggregate
	
//	private AggregateCache cache = new AggregateCache();

	public DefaultProfileAggregate(final int length, final int profileCount){
		if(profileCount==0){
			throw new IllegalArgumentException("Must have >0 profiles in aggregate");
		}
		this.length = length;
		this.profileCount = profileCount;
		
		aggregate = new float[length][profileCount];

	}
	
		
	public void addValues(final IProfile profile) throws ProfileException {
		
		if(counter>=profileCount){
			throw new ProfileException("Aggregate is full");
		}
		
		/*
		 * Make the profile the desired length, sample
		 * each point and add it to the aggregate
		 */
		
		IProfile interpolated = profile.interpolate(length);
		for(int i=0; i<length; i++){
			float d = (float) interpolated.get(i);
			aggregate[i][counter] = d;
			
		}
		
		counter++;
		
	}
	
	public int length(){
		return length;
	}
	
	public IProfile getMedian(){
		return calculateQuartile(Quartile.MEDIAN);
	}

	public IProfile getQuartile(float quartile){
		
		return calculateQuartile( (int) quartile);
	}
	
	/**
	 * Get the angle values at the given position in the aggregate.
	 * @param position the position to search. Must be between 0 and the length of the aggregate.
	 * @return an unsorted array of the values at the given position
	 */
	public float[] getValuesAtPosition(int position) {
		if(position < 0 || position > length ){
			throw new IllegalArgumentException("Desired position is out of range: "+position);
		}
		return getValuesAtIndex(position);
	}
	
	/**
	 * Get the angle values at the given position in the aggregate. If the requested
	 * position is not an integer, the closest integer index values are returned
	 * @param position the position to search. Must be between 0 and 1.
	 * @return an unsorted array of the values at the given position
	 */
	public double[] getValuesAtPosition(double position) {
		if(position < 0 || position > 1 ){
			throw new IllegalArgumentException("Desired x-position is out of range: "+position);
		}
		
		double indexPosition =  (double) this.length * position;
		
		// Choose the best position to return
		int index = (int) Math.round(indexPosition);
//		log("xposition "+position+": index "+index);
		
		
		float[] result = getValuesAtIndex(index);
		
		try {
			return new ArrayConverter(result).toDoubleArray();
		} catch (ArrayConversionException e) {
			stack("Error getting values from aggregate", e);
			return null;
		}
	}
	
	/**
	 * Get the x-axis positions of the centre of each bin.
	 * @return the Profile of positions
	 */
	public IProfile getXPositions(){
		float[] result = new float[length];
		
		float profileIncrement = 100f / (float) length;
		// start counting half a bin below zero
		// this sets the value to the bin centre
		float x = -profileIncrement/2;
		
		// add the bin size for each positions
		for(int i=0;i<length;i++){
			x += profileIncrement;
			result[i] = x;
		}
		return new FloatProfile(result);
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
	private float[] getValuesAtIndex(int i){

		float[] values = new float[profileCount];

		for(int n=0; n<profileCount; n++){
			values[n] = aggregate[i][n];
		}

		return values;
	}
	
	/**
	 * Calculate the profile for the given quartile
	 * @param quartile
	 * @return
	 */
	private IProfile calculateQuartile(int quartile) {
		
//		if(cache.hasProfile(quartile)){
////			log("Aggregate cache used");
//			return cache.getProfile(quartile);
//		}
		
		float[] medians = new float[length];
		
		

		
		for(int i=0; i<length; i++){
			
			float[] values = getValuesAtIndex(i);
			
			medians[i] = Quartile.quartile(values, quartile);
						
//			medians[i] = new Quartile(values, quartile).floatValue();
			
		}
		
		IProfile profile = new FloatProfile(medians);
//		cache.setProfile(quartile, profile);
//		log("Aggregate cache set");
		return profile;

	}


	@Override
	public double getBinSize() {
		return 0;
	}


	@Override
	public IProfile getQuartile(double quartile) throws ProfileException {
		return getQuartile( (float) quartile);
	}
	
	/**
	 * Cache the profiles from an aggregate at various quartiles.
	 * @author bms41
	 *
	 */
//	private class AggregateCache {
//		
//		private Map<Float, IProfile> cache = new HashMap<Float, IProfile>(5);
//
//		public AggregateCache(){}
//
//		/**
//		 * Set the stored profile
//		 * @param tag
//		 * @param profile
//		 */
//		public void setProfile(Float tag, IProfile profile){			  
//			cache.put(tag, profile);
//		}
//
//		/**
//		 * Get the given profile from the cache, or null if not present
//		 * @param tag
//		 * @param quartile
//		 * @return
//		 */
//		public IProfile getProfile(Float tag){
//			return cache.get(tag);
//		}
//
//		/**
//		 * Check if the given profile is in the cache
//		 * @param tag
//		 * @param quartile
//		 * @return
//		 */
//		public boolean hasProfile(Float tag){
//			return cache.containsKey(tag);
//		}
//
//		/**
//		 * Empty the cache - all values must be recalculated
//		 */
//		public void clear(){
//			cache = new HashMap<Float, IProfile>(5);
//		}
//	}

}

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
package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.List;

import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.rules.Rule.RuleType;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Quartile;

/**
 * This is a testbed for rule based identification of indexes in
 * a profile. Ideally, the rules can be saved in the description of
 * a nucleus, saving hard-coding of identification for new nucleus types
 * @author bms41
 *
 */
public class ProfileIndexFinder implements Loggable {
	
	
	public static final int NO_RULESETS    = -2;
	public static final int NO_INDEX_FOUND = -1;
	
	/**
	 * Get the indexes in the profile that match the given RuleSet
	 * @param p
	 * @param r
	 * @return
	 */
	public BooleanProfile getMatchingIndexes(final IProfile p, final RuleSet r){
		
		if(p==null){
			throw new IllegalArgumentException("Profile is null");
		}
		if(r==null){
			throw new IllegalArgumentException("Ruleset is null");
		}
		
		return isApplicable(p, r);
	}
	
	/**
	 * Get the indexes in the profile that match the given Rule
	 * @param p
	 * @param r
	 * @return
	 */
	public BooleanProfile getMatchingIndexes(final IProfile p, final Rule r){
		if(p==null){
			throw new IllegalArgumentException("Profile is null");
		}
		if(r==null){
			throw new IllegalArgumentException("Rule is null");
		}
		BooleanProfile result = new BooleanProfile(p, true);
		return isApplicable(p, r, result);
	}
	
	
	/**
	 * Count the indexes in the profile that match the given RuleSet
	 * @param p
	 * @param r
	 * @return
	 */
	public int countMatchingIndexes(final IProfile p, final RuleSet r){
		
		BooleanProfile matchingIndexes = getMatchingIndexes(p, r);
		
		int count = 0;
		for(int i=0;i<p.size();i++){
			
			if(matchingIndexes.get(i)){
				count++;
			}
		}
		return count;
		
	}
	
	/**
	 * Use the provided RuleSet to identify an index within a 
	 * profile. Returns the first matching index in the
	 * profile. On error or no hit, return -1
	 * @param p the profile
	 * @param r the ruleset to use for identification
	 * @return
	 */
	public int identifyIndex(final IProfile p, final RuleSet r){
		
		BooleanProfile matchingIndexes = getMatchingIndexes(p, r);
		
		for(int i=0;i<p.size();i++){
			
			if(matchingIndexes.get(i)){
				return i;
			}
		}
		return NO_INDEX_FOUND;
	}
	
	/**
	 * Use the provided RuleSets to identify an index within a 
	 * profile. Returns the first matching index in the
	 * profile. On error or no hit, return -1. Note that this ignores
	 * the RuleSet's ProfileType preference, and works directly on the given
	 * profile
	 * @param p the profile
	 * @param list the rulesets to use for identification
	 * @return
	 */
	public int identifyIndex(final IProfile p, final List<RuleSet> list){
		
		if(list==null || list.size()==0){
			// no rule set
			return NO_RULESETS;
		}
		
		BooleanProfile indexes = new BooleanProfile(p, true);
		
		for(RuleSet r : list){
			BooleanProfile matchingIndexes = getMatchingIndexes(p, r);
			indexes = indexes.and(matchingIndexes);
		}
				
		for(int i=0;i<p.size();i++){
			
			if(indexes.get(i)){
				return i;
			}
		}
		return NO_INDEX_FOUND;
	}
		
	/**
	 * Identify the index for the median profile of the collection based on the 
	 * internal RuleSets for the given border tag
	 * @param collection
	 * @param tag
	 * @return -2 if the RuleSet list is empty; -1 if the index is not found; else the index
	 */
	public int identifyIndex(final ICellCollection collection, final Tag tag){
		
		List<RuleSet> list = collection.getRuleSetCollection().getRuleSets(tag);
		
		if(list==null || list.size()==0){
			// no rule set
			return NO_RULESETS;
		}
		return identifyIndex(collection, list);
		
	}
	
	/**
	 * Identify the index for the median profile of the collection based on the 
	 * given RuleSets
	 * @param collection
	 * @param list
	 * @return -2 if the RuleSet list is empty; -1 if the index is not found; else the index
	 */
	public int identifyIndex(final ICellCollection collection, final List<RuleSet> list){
		
		if(list==null || list.isEmpty()){
			return NO_RULESETS; // no rule sets
		}
		
		
//		// Make a 'true' profile
//		BooleanProfile indexes = new BooleanProfile(collection
//					.getProfileCollection(ProfileType.ANGLE)
//					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN), true);
//		
//		
//		for(RuleSet r : list){
//			
//			// Get the correct profile for the RuleSet
//			Profile p = collection
//					.getProfileCollection(r.getType())
//					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
//			
//			// Apply the rule, and update the result profile
//			BooleanProfile matchingIndexes = getMatchingIndexes(p, r);
//			indexes = indexes.and(matchingIndexes);
//			
//		}
		BooleanProfile indexes = getMatchingProfile(collection, list);
		
		
		// Find the first true in the result profile
		for(int i=0;i<indexes.size();i++){

			if(indexes.get(i)){
				return i;
			}
		}
		return NO_INDEX_FOUND;
		
	}
	
	public BooleanProfile getMatchingProfile(final ICellCollection collection, final List<RuleSet> list){
		// Make a 'true' profile
		BooleanProfile indexes;
		try {
			indexes = new BooleanProfile(collection
					.getProfileCollection()
					.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN), true);
			
		} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
			fine("Cannot get matching profile", e);
			return new BooleanProfile(collection
					.getProfileCollection().length(), false);
		}


		for(RuleSet r : list){

			// Get the correct profile for the RuleSet
			IProfile p;
			try {
				p = collection
						.getProfileCollection()
						.getProfile(r.getType(), Tag.REFERENCE_POINT, Quartile.MEDIAN);
			} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
				fine("Cannot get matching profile", e);
				return new BooleanProfile(collection
						.getProfileCollection().length(), false);
			}

			// Apply the rule, and update the result profile
			BooleanProfile matchingIndexes = getMatchingIndexes(p, r);
			indexes = indexes.and(matchingIndexes);

		}
		return indexes;
	}
	
	/**
	 * Test a profile for the applicability of a ruleset
	 * @param p
	 * @param r
	 * @return
	 */
	private BooleanProfile isApplicable(final IProfile p, final RuleSet r){
		
		BooleanProfile result = new BooleanProfile(p, true);
		for(Rule rule : r.getRules()){
//			BooleanProfile b = isApplicable(p, rule, result);
			result = isApplicable(p, rule, result);
//			result = result.and(b);
		}
		return result;
	}
	
	/**
	 * Test a profile for the applicability of a rule in a ruleset
	 * @param p the profile to test
	 * @param r the rule to test
	 * @param existing the existing profile of valid indexes on which the rule will be applied
	 * @return
	 */
	private BooleanProfile isApplicable(final IProfile p, final Rule r, BooleanProfile limits){
		
		RuleType type = r.getType();
		
		switch(type){
		
			case IS_LOCAL_MINIMUM:
				return findLocalMinima(p, limits, r.getBooleanValue(), r.getValue(1));
			case IS_LOCAL_MAXIMUM:
				return findLocalMaxima(p, limits, r.getBooleanValue(), r.getValue(1));
				
			case IS_MINIMUM:
				return findMinimum(p, limits, r.getBooleanValue());
			case IS_MAXIMUM:
				return findMaximum(p, limits, r.getBooleanValue());
				
			case INDEX_IS_LESS_THAN:
				return findIndexLessThan(p, limits,  r.getValue());
			case INDEX_IS_MORE_THAN:
				return findIndexMoreThan(p, limits,  r.getValue());
				
			case VALUE_IS_LESS_THAN:
				return findValueLessThan(p, limits,  r.getValue());
			case VALUE_IS_MORE_THAN:
				return findValueMoreThan(p, limits,  r.getValue());
				
			case IS_CONSTANT_REGION:
				return findConstantRegion(p, limits, r.getValue(0), r.getValue(1), r.getValue(2));
				
			case FIRST_TRUE:
				return findFirstTrue(limits, r.getBooleanValue());	
			case LAST_TRUE:
				return findLastTrue(limits, r.getBooleanValue());	
				
			case INDEX_IS_WITHIN_FRACTION_OF:
				return findIndexWithinFractionOf(limits, r.getValue());
				
			case INDEX_IS_OUTSIDE_FRACTION_OF:
				return findIndexOutsideFractionOf(limits, r.getValue());
				
			case INVERT:
				return limits.invert();
				
			default:
				return new BooleanProfile(p);
		
		}
		
	}
	
	
	/**
	 * Get indexes that are within the given fraction of a profile from any true value in the input limits
	 * @param b the limits
	 * @param fraction the fraction of the profile from 0-1 either side of true values that will be true
	 * @return a boolean profile with each true widened by the fractional amount
	 */
	private BooleanProfile findIndexWithinFractionOf(final BooleanProfile b, final double fraction){
		BooleanProfile result = new BooleanProfile(b.size(), false); 
		
		int range = (int) Math.round((double) b.size() * fraction);
		
		for(int i=0;i<b.size();i++){
			if(b.get(i)){
				
				for(int j=i-range; j<i+range; j++){
					result.set(j, true);
				}
				
			}
		}	
		return result;
	}
	
	/**
	 * Get indexes that are within the given fraction of a profile from any true value in the input limits
	 * @param b the limits
	 * @param fraction the fraction of the profile from 0-1 either side of true values that will be true
	 * @return a boolean profile with each true widened by the fractional amount
	 */
	private BooleanProfile findIndexOutsideFractionOf(final BooleanProfile b, final double fraction){
		BooleanProfile result = findIndexWithinFractionOf(b, fraction); 
		
		return result.invert();
	}
	
	/**
	 * Find constant regions within the profile.
	 * @param p the profile to test
	 * @param value the value to remain at
	 * @param window the window size in indexes
	 * @param epsilon the maximum distance from value allowed
	 * @return
	 */
	private BooleanProfile findConstantRegion(final IProfile p, final BooleanProfile limits, final double value, final double window, final double epsilon){ 
		
		BooleanProfile result = new BooleanProfile(p); // hard code the smoothing window size for now
		

	    int[] verticalPoints = p.getConsistentRegionBounds(value, epsilon, (int) window);
	    if(verticalPoints[0]!=-1 && verticalPoints[1]!=-1){
	    	
	    	for(int i=verticalPoints[0]; i<=verticalPoints[1]; i++){
	    		result.set(i, true);
	    	}

	    }
	    result = result.and(limits);
		return result;
	}
	
	/**
	 * Find the first true value in a BooleanProfile
	 * @param b the limits to test within
	 * @param v find the first true value [true], or true values that are not the first true value [false]
	 * @return
	 */
	private BooleanProfile findFirstTrue(final BooleanProfile b, boolean v){
		
		BooleanProfile result = new BooleanProfile(b.size(), false); 
		boolean foundFirst = false;
		
		for(int i=0;i<b.size();i++){
			
			if(v){
				if(b.get(i)){
					result.set(i, true);
					return result;
				}
			} else {
				if(b.get(i)){
					
					if(foundFirst){
						result.set(i, true);
					} else {
						result.set(i, false);
					}
						
					foundFirst = true;
					
					
				}
				
				
			}
		}	
		return result;
	}
	
	/**
	 * Find the last true value in a BooleanProfile
	 * @param b the limits to test within
	 * @param v find the last true value [true], or true values that are not the last true value [false]
	 * @param p
	 * @return
	 */
	private BooleanProfile findLastTrue(final BooleanProfile b, boolean v){
		
		BooleanProfile result = new BooleanProfile(b.size(), false); 

		
		int maxTrueIndex = NO_INDEX_FOUND;
		for(int i=0;i<b.size();i++){
			if(b.get(i)){
				maxTrueIndex=i;
				
			}
		}	

		if(v){
			if(maxTrueIndex>-1){
				result.set(maxTrueIndex, true);
			}
			
		} else {
			
			if(maxTrueIndex>-1){
				
				for(int i=0;i<b.size();i++){
					if(b.get(i) && i!= maxTrueIndex){
						result.set(i, true);
						
					}
				}
				
			}
			
			
		}
		
		
		return result;
	}
	
	/**
	 * Find local minima within the profile.
	 * @param p the profile to test
	 * @param include if false, will find indexes that are NOT local minima
	 * @param window the size of the smoothing window
	 * @return
	 */
	private BooleanProfile findLocalMinima(final IProfile p, BooleanProfile limits, boolean include, double window){ 
		
		BooleanProfile result = p.getLocalMinima((int) window); // hard code the smoothing window size for now
		result = result.and(limits);
		
		if( ! include){
			result = result.invert();
		}
		return result;
	}
	
	/**
	 * Find local maxima within the profile.
	 * @param p the profile to test
	 * @param include if false, will find indexes that are NOT local maxima
	 * @param window the size of the smoothing window
	 * @return
	 */
	private BooleanProfile findLocalMaxima(final IProfile p, BooleanProfile limits, boolean include, double window){ 
		
		BooleanProfile result = p.getLocalMaxima((int) window); // hard code the smoothing window size for now
		result = result.and(limits);
		
		if( ! include){
			result = result.invert();
		}
		return result;
	}
	
	
	/**
	 * Find the index of the minimum value in a profile
	 * @param p
	 * @return
	 */
	private BooleanProfile findMinimum(final IProfile p, BooleanProfile limits, boolean b){
		
		int index = p.getIndexOfMin(limits);
		
		BooleanProfile result;
		
		if(b){
			
			result = new BooleanProfile(p, false);
			result.set(index, true);
			
		} else {
			result = new BooleanProfile(p, true);
			result.set(index, false);
		}
		

		return result;
	}
	
	/**
	 * Find the index of the maximum value in a profile
	 * @param p
	 * @return
	 */
	private BooleanProfile findMaximum(final IProfile p, BooleanProfile limits, boolean b){
		
		int index = p.getIndexOfMax(limits);
		
		BooleanProfile result;
		
		if(b){
			
			result = new BooleanProfile(p, false);
			result.set(index, true);
			
		} else {
			result = new BooleanProfile(p, true);
			result.set(index, false);
		}
		
		return result;
	}
	
	/**
	 * Make a boolean profile where the indexes are less than the
	 * given value
	 * @param p
	 * @param fileIndex
	 * @return
	 */
	private BooleanProfile findIndexLessThan(final IProfile p, BooleanProfile limits, double proportion){
		
		int index = (int) Math.ceil(  (double) p.size() * proportion);
		
		BooleanProfile result = new BooleanProfile(p);
		
		for(int i=0;i<index;i++){
			result.set(i, true);
		}
		
		result = result.and(limits);
		return result;
		
	}
	
	/**
	 * Make a boolean profile where the indexes are more than the
	 * given value
	 * @param p
	 * @param fileIndex
	 * @return
	 */
	private BooleanProfile findIndexMoreThan(final IProfile p, BooleanProfile limits, double proportion){
		
		int index = (int) Math.floor(  (double) p.size() * proportion);
		
		BooleanProfile result = new BooleanProfile(p);
		
		for(int i=index;i<result.size();i++){
			result.set(i, true);
		}
		result = result.and(limits);
		return result;
		
	}
	
	/**
	 * Make a boolean profile where the values are less than the
	 * given value
	 * @param p
	 * @param fileIndex
	 * @return
	 */
	private BooleanProfile findValueLessThan(final IProfile p, BooleanProfile limits, double value){
		
		BooleanProfile result = new BooleanProfile(p);

		for(int i=0;i<result.size();i++){
			
			if(p.get(i) < value){
				result.set(i, true);
			}
		}
		result = result.and(limits);
		return result;
		
	}
	
	/**
	 * Make a boolean profile where the values are more than the
	 * given value
	 * @param p
	 * @param fileIndex
	 * @return
	 */
	private BooleanProfile findValueMoreThan(final IProfile p, BooleanProfile limits, double value){
		
		BooleanProfile result = new BooleanProfile(p);

		for(int i=0;i<result.size();i++){
			
			if(p.get(i) > value){
				result.set(i, true);
			}
		}
		result = result.and(limits);
		return result;
		
	}
	
		
	
}

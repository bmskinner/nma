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
package analysis.profiles;

import java.util.List;

import utility.Constants;
import analysis.profiles.Rule.RuleType;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileType;
import logging.Loggable;

/**
 * This is a testbed for rule based identification of indexes in
 * a profile. Ideally, the rules can be saved in the description of
 * a nucleus, saving hard-coding of identification for new nucleus types
 * @author bms41
 *
 */
public class ProfileIndexFinder implements Loggable {
	
	
	/**
	 * Get the indexes in the profile that match the given RuleSet
	 * @param p
	 * @param r
	 * @return
	 */
	public BooleanProfile getMatchingIndexes(final Profile p, final RuleSet r){
		return isApplicable(p, r);
	}
	
	
	/**
	 * Count the indexes in the profile that match the given RuleSet
	 * @param p
	 * @param r
	 * @return
	 */
	public int countMatchingIndexes(final Profile p, final RuleSet r){
		
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
	public int identifyIndex(final Profile p, final RuleSet r){
		
		BooleanProfile matchingIndexes = getMatchingIndexes(p, r);
		
		for(int i=0;i<p.size();i++){
			
			if(matchingIndexes.get(i)){
				return i;
			}
		}
		return -1;
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
	public int identifyIndex(final Profile p, final List<RuleSet> list){
		
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
		return -1;
	}
	
	/**
	 * Identify the index for the median profile of the collection based on the 
	 * internal RuleSets for the given border tag
	 * @param collection
	 * @param tag
	 * @return
	 */
	public int identifyIndex(final CellCollection collection, final BorderTag tag){
		
		List<RuleSet> list = collection.getRuleSetCollection().getRuleSets(tag);
		return identifyIndex(collection, list);
		
	}
	
	/**
	 * Identify the index for the median profile of the collection based on the 
	 * given RuleSets
	 * @param collection
	 * @param list
	 * @return
	 */
	public int identifyIndex(final CellCollection collection, final List<RuleSet> list){
		
		if(list.size()==0){
			return -1;
		}
		
		BooleanProfile indexes = new BooleanProfile(collection
					.getProfileCollection(ProfileType.REGULAR)
					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN), true);
		
		
		for(RuleSet r : list){
			
			// Get the correct profile for the RuleSet
			Profile p = collection
					.getProfileCollection(r.getType())
					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			
			BooleanProfile matchingIndexes = getMatchingIndexes(p, r);
			indexes = indexes.and(matchingIndexes);
			
		}
		
		for(int i=0;i<indexes.size();i++){

			if(indexes.get(i)){
				return i;
			}
		}
		return -1;
		
	}
	
	/**
	 * Test a profile for the applicability of a ruleset
	 * @param p
	 * @param r
	 * @return
	 */
	private BooleanProfile isApplicable(final Profile p, final RuleSet r){
		
		BooleanProfile result = new BooleanProfile(p, true);
		for(Rule rule : r.getRules()){
			BooleanProfile b = isApplicable(p, rule, result);
			result = result.and(b);
		}
		return result;
	}
	
	/**
	 * Test a profile for the applicability of a rule in a ruleset
	 * @param p
	 * @param r
	 * @param existing the existing profile of valid indexes on which the rule will be applied
	 * @return
	 */
	private BooleanProfile isApplicable(final Profile p, final Rule r, BooleanProfile limits){
		
		RuleType type = r.getType();
		
		switch(type){
		
			case IS_LOCAL_MINIMUM:
				return findLocalMinima(p, r.getBooleanValue(), r.getValue(1));
			case IS_LOCAL_MAXIMUM:
				return findLocalMaxima(p, r.getBooleanValue(), r.getValue(1));
				
			case IS_MINIMUM:
				return findMinimum(p, limits);
			case IS_MAXIMUM:
				return findMaximum(p, limits);
				
			case INDEX_IS_LESS_THAN:
				return findIndexLessThan(p, r.getValue());
			case INDEX_IS_MORE_THAN:
				return findIndexMoreThan(p, r.getValue());
				
			case VALUE_IS_LESS_THAN:
				return findValueLessThan(p, r.getValue());
			case VALUE_IS_MORE_THAN:
				return findValueMoreThan(p, r.getValue());
				
			case IS_CONSTANT_REGION:
				return findConstantRegion(p, r.getValue(0), r.getValue(1), r.getValue(2));
				
			case FIRST_TRUE:
				return findFirstTrue(p, limits);	
			case LAST_TRUE:
				return findLastTrue(p, limits);	
				
			default:
				return new BooleanProfile(p);
		
		}
		
	}
	
	/**
	 * Find constant regions within the profile.
	 * @param p the profile to test
	 * @param value the value to remain at
	 * @param window the window size in indexes
	 * @param epsilon the maximum distance from value allowed
	 * @return
	 */
	private BooleanProfile findConstantRegion(final Profile p, final double value, final double window, final double epsilon){ 
		
		BooleanProfile result = new BooleanProfile(p); // hard code the smoothing window size for now
		

	    int[] verticalPoints = p.getConsistentRegionBounds(value, epsilon, (int) window);
	    if(verticalPoints[0]!=-1 && verticalPoints[1]!=-1){
	    	
	    	for(int i=verticalPoints[0]; i<=verticalPoints[1]; i++){
	    		result.set(i, true);
	    	}

	    }
	
		return result;
	}
	
	/**
	 * Find the first true value in a BooleanProfile
	 * @param p
	 * @return
	 */
	private BooleanProfile findFirstTrue(final Profile p, final BooleanProfile b){
		
		BooleanProfile result = new BooleanProfile(p); // hard code the smoothing window size for now
		
		for(int i=0;i<p.size();i++){
			if(b.get(i)){
				result.set(i, true);
				return result;
			}
		}	
		return result;
	}
	
	/**
	 * Find the last true value in a BooleanProfile
	 * @param p
	 * @return
	 */
	private BooleanProfile findLastTrue(final Profile p, final BooleanProfile b){
		
		BooleanProfile result = new BooleanProfile(p); // hard code the smoothing window size for now
		
		int maxTrueIndex = -1;
		for(int i=0;i<p.size();i++){
			if(b.get(i)){
				maxTrueIndex=i;
				
			}
		}	
		
		if(maxTrueIndex>-1){
			result.set(maxTrueIndex, true);
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
	private BooleanProfile findLocalMinima(final Profile p, boolean include, double window){ 
		
		BooleanProfile result = p.getLocalMinima((int) window); // hard code the smoothing window size for now
		
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
	private BooleanProfile findLocalMaxima(final Profile p, boolean include, double window){ 
		
		BooleanProfile result = p.getLocalMaxima((int) window); // hard code the smoothing window size for now
		
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
	private BooleanProfile findMinimum(final Profile p, BooleanProfile limits){
		
		int index = p.getIndexOfMin(limits);
		
		BooleanProfile result = new BooleanProfile(p);
		
		result.set(index, true);
		return result;
	}
	
	/**
	 * Find the index of the maximum value in a profile
	 * @param p
	 * @return
	 */
	private BooleanProfile findMaximum(final Profile p, BooleanProfile limits){
		
		int index = p.getIndexOfMax(limits);
		
		BooleanProfile result = new BooleanProfile(p);
		
		result.set(index, true);
		return result;
	}
	
	/**
	 * Make a boolean profile where the indexes are less than the
	 * given value
	 * @param p
	 * @param index
	 * @return
	 */
	private BooleanProfile findIndexLessThan(final Profile p, double proportion){
		
		int index = (int) Math.ceil(  (double) p.size() * proportion);
		
		BooleanProfile result = new BooleanProfile(p);
		
		for(int i=0;i<index;i++){
			result.set(i, true);
		}
		return result;
		
	}
	
	/**
	 * Make a boolean profile where the indexes are more than the
	 * given value
	 * @param p
	 * @param index
	 * @return
	 */
	private BooleanProfile findIndexMoreThan(final Profile p, double proportion){
		
		int index = (int) Math.floor(  (double) p.size() * proportion);
		
		BooleanProfile result = new BooleanProfile(p);
		
		for(int i=index;i<result.size();i++){
			result.set(i, true);
		}
		return result;
		
	}
	
	/**
	 * Make a boolean profile where the values are less than the
	 * given value
	 * @param p
	 * @param index
	 * @return
	 */
	private BooleanProfile findValueLessThan(final Profile p, double value){
		
		BooleanProfile result = new BooleanProfile(p);

		for(int i=0;i<result.size();i++){
			
			if(p.get(i) < value){
				result.set(i, true);
			}
		}
		return result;
		
	}
	
	/**
	 * Make a boolean profile where the values are more than the
	 * given value
	 * @param p
	 * @param index
	 * @return
	 */
	private BooleanProfile findValueMoreThan(final Profile p, double value){
		
		BooleanProfile result = new BooleanProfile(p);

		for(int i=0;i<result.size();i++){
			
			if(p.get(i) > value){
				result.set(i, true);
			}
		}
		return result;
		
	}
	
		
	
}

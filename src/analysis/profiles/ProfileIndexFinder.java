package analysis.profiles;

import analysis.profiles.Rule.RuleType;
import components.generic.BooleanProfile;
import components.generic.Profile;
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
				return findLocalMinima(p, r.getBooleanValue());
			case IS_LOCAL_MAXIMUM:
				return findLocalMaxima(p, r.getBooleanValue());
				
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
				
			default:
				return new BooleanProfile(p);
		
		}
		
	}
	
	/**
	 * Find local minima within the profile.
	 * @param p the profile to test
	 * @param include if false, will find indexes that are NOT local minima
	 * @return
	 */
	private BooleanProfile findLocalMinima(final Profile p, boolean include){ 
		
		BooleanProfile result = p.getLocalMinima(5); // hard code the smoothing window size for now
		
		if( ! include){
			result = result.invert();
		}
		return result;
	}
	
	/**
	 * Find local maxima within the profile.
	 * @param p the profile to test
	 * @param include if false, will find indexes that are NOT local maxima
	 * @return
	 */
	private BooleanProfile findLocalMaxima(final Profile p, boolean include){ 
		
		BooleanProfile result = p.getLocalMaxima(5); // hard code the smoothing window size for now
		
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

package analysis;

import java.util.ArrayList;
import java.util.List;

import components.generic.BooleanProfile;
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
public class ProfileRulesetInderxer implements Loggable {
	
	/**
	 * Create a RuleSet that describes how to find the RP in 
	 * mouse sperm nuclear profiles
	 * @return
	 */
	public RuleSet createMouseSpermRPRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.REGULAR);
		
		return builder.isMinimum().build();
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
		
		BooleanProfile matchingIndexes = isApplicable(p, r);
		
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
			BooleanProfile b = isApplicable(p, rule);
			result = result.and(b);
		}
		return result;
	}
	
	/**
	 * Test a profile for the applicability of a rule in a ruleset
	 * @param p
	 * @param r
	 * @return
	 */
	private BooleanProfile isApplicable(final Profile p, final Rule r){
		
		RuleType type = r.getType();
		
		switch(type){
		
			case IS_LOCAL_MINIMUM:
				return p.getLocalMinima(5);
			case IS_LOCAL_MAXIMUM:
				return p.getLocalMaxima(5);
				
			case IS_MINIMUM:
				return findMinimum(p);
			case IS_MAXIMUM:
				return findMaximum(p);
				
			case INDEX_IS_LESS_THAN:
				return findIndexLessThan(p, r.getIntValue());
			case INDEX_IS_MORE_THAN:
				return findIndexMoreThan(p, r.getIntValue());
				
			case VALUE_IS_LESS_THAN:
				return findValueLessThan(p, r.getValue());
			case VALUE_IS_MORE_THAN:
				return findValueMoreThan(p, r.getValue());
				
			default:
				return new BooleanProfile(p);
		
		}
		
	}
	
	private BooleanProfile findMinimum(final Profile p){
		
		int index = p.getIndexOfMin();
		
		BooleanProfile result = new BooleanProfile(p);
		
		result.set(index, true);
		return result;
	}
	
	private BooleanProfile findMaximum(final Profile p){
		
		int index = p.getIndexOfMax();
		
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
	private BooleanProfile findIndexLessThan(final Profile p, int index){
		
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
	private BooleanProfile findIndexMoreThan(final Profile p, int index){
		
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
	
	
	/**
	 * Use readable instructions to create a ruleset
	 * describing how to find a feature in a profile
	 * @author bms41
	 *
	 */
	public class RuleSetBuilder {
		
		List<Rule> rules;
		ProfileType type;
		
		/**
		 * Construct a builder for the given profile type
		 * @param type
		 */
		public RuleSetBuilder(final ProfileType type){
			rules = new ArrayList<Rule>();
			this.type = type;
		}
		
		
		/**
		 * Add an arbitrary rule
		 * @param r
		 * @return
		 */
		public RuleSetBuilder addRule(final Rule r){
			rules.add(r);
			return this;
		}
		
		/**
		 * Will find the lowest value in the profile
		 * @return
		 */
		public RuleSetBuilder isMinimum(){
			Rule r = new Rule(RuleType.IS_MINIMUM, 1d);
			rules.add(r);
			return this;
		}
		
		/**
		 * Will find the highest value in the profile
		 * @return
		 */
		public RuleSetBuilder isMaximum(){
			Rule r = new Rule(RuleType.IS_MAXIMUM, 1d);
			rules.add(r);
			return this;
		}
		
		/**
		 * The index must be a local minimum
		 * @return
		 */
		public RuleSetBuilder isLocalMinimum(){
			Rule r = new Rule(RuleType.IS_LOCAL_MINIMUM, 1d);
			rules.add(r);
			return this;
		}
		
		/**
		 * The index must not be a local minimum
		 * @return
		 */
		public RuleSetBuilder isNotLocalMinimum(){
			Rule r = new Rule(RuleType.IS_LOCAL_MINIMUM, 0d);
			rules.add(r);
			return this;
		}
		
		/**
		 * The index must be a local maximum
		 * @return
		 */
		public RuleSetBuilder isLocalMaximum(){
			Rule r = new Rule(RuleType.IS_LOCAL_MAXIMUM, 1d);
			rules.add(r);
			return this;
		}
		
		/**
		 * The index must not be a local maximum
		 * @return
		 */
		public RuleSetBuilder isNotLocalMaximum(){
			Rule r = new Rule(RuleType.IS_LOCAL_MAXIMUM, 0d);
			rules.add(r);
			return this;
		}
		
		/**
		 * The index found must be less than the given proportional
		 * distance through the profile (0-1)
		 * @param d
		 * @return
		 */
		public RuleSetBuilder indexIsLessThan(final double d){
			if(d<0 || d>1){
				warn("Invalid rule: proportion "+d);
			}
			Rule r = new Rule(RuleType.INDEX_IS_LESS_THAN, d);
			rules.add(r);
			return this;
		}
		
		/**
		 * The index found must be less than the given proportional
		 * distance through the profile (0-1)
		 * @param d
		 * @return
		 */
		public RuleSetBuilder indexIsMoreThan(final double d){
			if(d<0 || d>1){
				warn("Invalid rule: proportion "+d);
			}
			Rule r = new Rule(RuleType.INDEX_IS_MORE_THAN, d);
			rules.add(r);
			return this;
		}
		
		/**
		 * The value found must be less than the given value
		 * @param d
		 * @return
		 */
		public RuleSetBuilder valueIsLessThan(final double d){

			Rule r = new Rule(RuleType.VALUE_IS_LESS_THAN, d);
			rules.add(r);
			return this;
		}
		
		/**
		 * The value found must be less than the given value
		 * @param d
		 * @return
		 */
		public RuleSetBuilder valueIsMoreThan(final double d){

			Rule r = new Rule(RuleType.VALUE_IS_MORE_THAN, d);
			rules.add(r);
			return this;
		}
		
		
		/**
		 * Create the RuleSet
		 * @return
		 */
		public RuleSet build(){
			RuleSet r = new RuleSet(type);
			for(Rule rule : rules){
				r.addRule(rule);
			}
			return r;
		}
		
	}
	
	/**
	 * A collection of rules that can be followed to
	 * identify a feature in a proflile
	 * @author bms41
	 *
	 */
	public class RuleSet {

		// combine rules with AND conditions
		List<Rule> rules;
		ProfileType type; // the type of profile to which the rules apply

		public RuleSet(final ProfileType type){
			rules = new ArrayList<Rule>();
			this.type = type;
		}

		public void addRule(final Rule r){
			rules.add(r);
		}
		
		public List<Rule> getRules(){
			return rules;
		}
		
		public String toString(){
			StringBuilder b = new StringBuilder();
			b.append(type+"\n");
			for(Rule r : rules){
				b.append(r.toString()+"\n");
			}
			return b.toString();
		}
	}
	
	/**
	 * An instruction for finding an index in a profile
	 * @author bms41
	 *
	 */
	public class Rule {

		final RuleType type;
		final double  value;

		public Rule(RuleType type, double value){

			this.type = type;
			this.value = value;
		}

		public Rule(RuleType type, boolean value){

			this.type = type;
			this.value = value ? 1d : 0d;
		} 

		public double getValue(){
			return value;
		}

		public boolean getBooleanValue(){
			if(value==1d){
				return true;
			} else {
				return false;
			}
		}

		public int getIntValue(){
			return (int) value;
		}
		
		public RuleType getType(){
			return type;
		}
		
		public String toString(){
			return type+" : "+value;
		}
	}


	/**
	 * A type of instruction to follow
	 * @author bms41
	 *
	 */
	public enum RuleType{

		IS_MINIMUM,
		IS_MAXIMUM,
		
		IS_LOCAL_MINIMUM,
		IS_LOCAL_MAXIMUM,

		VALUE_IS_LESS_THAN,
		VALUE_IS_MORE_THAN,
		
		INDEX_IS_LESS_THAN,
		INDEX_IS_MORE_THAN;

	}

}

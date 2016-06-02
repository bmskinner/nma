package analysis.profiles;

import java.util.ArrayList;
import java.util.List;

import analysis.profiles.Rule.RuleType;
import logging.Loggable;
import components.generic.ProfileType;

/**
 * Use readable instructions to create a ruleset
 * describing how to find a feature in a profile
 * @author bms41
 *
 */
public class RuleSetBuilder implements Loggable {
	
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
	 * Will find the lowest remaining value in the profile
	 * after previous rules have been applied
	 * @return
	 */
	public RuleSetBuilder isMinimum(){
		Rule r = new Rule(RuleType.IS_MINIMUM, 1d);
		rules.add(r);
		return this;
	}
	
	/**
	 * Will find the highest remaining value in the profile
	 * after previous rules have been applied
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

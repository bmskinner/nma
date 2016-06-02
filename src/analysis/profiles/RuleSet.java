package analysis.profiles;

import java.util.ArrayList;
import java.util.List;

import components.generic.ProfileType;

/**
 * A collection of rules that can be followed to
 * identify a feature in a proflile. Rules will be applied
 * sequentially: if rule n limits indexes, rule n+1 will only
 * be applied to those remaining indexes
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
	
	/*
	 * STATIC METHODS FOR BUILT-IN RULESETS
	 */
	
	/**
	 * Create a RuleSet that describes how to find the RP in 
	 * mouse sperm nuclear profiles
	 * @return
	 */
	public static RuleSet mouseSpermRPRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.REGULAR);
		
		return builder
				.isMinimum()
				.build();
	}
	
	/**
	 * Create a RuleSet that describes how to find the OP in 
	 * mouse sperm nuclear profiles
	 * @return
	 */
	public static RuleSet mouseSpermOPRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.REGULAR);
		
		return builder
			.isLocalMinimum()
			.indexIsMoreThan(0.2) // assumes the profile is indexes on the RP
			.indexIsLessThan(0.6)
			.isMinimum()
			.build();
		
	}
	
	/**
	 * Create a RuleSet that describes how to find the RP in 
	 * pig sperm nuclear profiles
	 * @return
	 */
	public static RuleSet pigSpermRPRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.REGULAR);
		
		return builder
				.isLocalMaximum()
				.valueIsMoreThan(180)
				.isMaximum()
				.build();
	}
	
	/**
	 * Create a RuleSet that describes how to find the RP in 
	 * round nucleus profiles
	 * @return
	 */
	public static RuleSet roundRPRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.DISTANCE);
		
		return builder
			.isMaximum()
			.build();
	}
	
	
}
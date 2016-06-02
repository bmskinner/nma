package analysis.profiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import components.generic.BorderTag;
import components.nuclear.NucleusType;

/**
 * This holds the rulesets for identifying each of the BorderTags
 * in a profile. Multiple RuleSets can be combined for each BorderTag,
 * allowing multiple ProfileTypes to be used. Designed to be stored within
 * a cell collection
 * @author bms41
 *
 */
public class RuleSetCollection {
	
	private Map<BorderTag, List<RuleSet>> map = new HashMap<BorderTag, List<RuleSet>>();
	
	/**
	 * Create a new empty collection
	 */
	public RuleSetCollection(){
		for(BorderTag tag : BorderTag.values()){
			clearRuleSets(tag);
		}
	}
	
	/**
	 * Remove all the RuleSets for the given tag
	 * @param tag
	 */
	public void clearRuleSets(BorderTag tag){
		map.put(tag, new ArrayList<RuleSet>());
	}
	
	/**
	 * Add a ruleset for the given tag
	 * @param tag
	 * @param r
	 */
	public void addRuleSet(BorderTag tag, RuleSet r){
		map.get(tag).add(r);
	}
	
	/**
	 * Get the rulesets for the given tag
	 * @param tag
	 * @param r
	 */
	public List<RuleSet> getRuleSets(BorderTag tag){
		return map.get(tag);
	}
	
	public String toString(){
		StringBuilder b = new StringBuilder();
		b.append("RuleSets:\n");
		for(BorderTag tag : map.keySet()){
			b.append("\t"+tag+":\n");
			List<RuleSet> l = map.get(tag);
			for(RuleSet r : l){
				b.append("\t"+r.toString()+"\n");
			}
		}
		
		return b.toString();
	}
	
	/*
	 * Static methods to create the default rulesets for a given NucleusType
	 */
	
	public static RuleSetCollection createDefaultRuleSet(NucleusType type){
		
		switch(type){
			case PIG_SPERM:
				return createPigSpermRuleSets();
			case RODENT_SPERM:
				return createMouseSpermRuleSets();
			default:
				return createRoundRuleSets();

		}
		
	}
	
	/**
	 * Create a RuleSetCollection for mouse sperm nuclei
	 * @return
	 */
	private static RuleSetCollection createMouseSpermRuleSets(){
		RuleSetCollection r = new RuleSetCollection();
		
		r.addRuleSet(BorderTag.REFERENCE_POINT,   RuleSet.mouseSpermRPRuleSet());
		r.addRuleSet(BorderTag.ORIENTATION_POINT, RuleSet.mouseSpermOPRuleSet());
		r.addRuleSet(BorderTag.TOP_VERTICAL,      RuleSet.mouseSpermTVRuleSet());
		r.addRuleSet(BorderTag.BOTTOM_VERTICAL,   RuleSet.mouseSpermBVRuleSet());
		return r;
	}
	
	/**
	 * Create a RuleSetCollection for pig sperm nuclei
	 * @return
	 */
	private static RuleSetCollection createPigSpermRuleSets(){
		RuleSetCollection r = new RuleSetCollection();
		
		r.addRuleSet(BorderTag.REFERENCE_POINT, RuleSet.pigSpermRPRuleSet());
		return r;
	}
	
	/**
	 * Create a RuleSetCollection for round nuclei
	 * @return
	 */
	private static RuleSetCollection createRoundRuleSets(){
		RuleSetCollection r = new RuleSetCollection();
		
		r.addRuleSet(BorderTag.REFERENCE_POINT,   RuleSet.roundRPRuleSet());
		r.addRuleSet(BorderTag.ORIENTATION_POINT, RuleSet.roundRPRuleSet());
		return r;
	}

}

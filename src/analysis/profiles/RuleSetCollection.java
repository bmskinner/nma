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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logging.Loggable;
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
public class RuleSetCollection implements Serializable, Loggable {
	
	private static final long serialVersionUID = 1L;
	
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
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		finest("\tReading RulesetCollection");
		in.defaultReadObject();
		finest("\tRead RulesetCollection");
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		finest("\tWriting RulesetCollection");
		out.defaultWriteObject();
		finest("\tWrote RulesetCollection");
	}

}

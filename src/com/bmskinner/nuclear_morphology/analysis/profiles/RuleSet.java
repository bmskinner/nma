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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;

/**
 * A collection of rules that can be followed to
 * identify a feature in a profile. Rules will be applied
 * sequentially: if rule n limits indexes, rule n+1 will only
 * be applied to those remaining indexes
 * @author bms41
 *
 */
public class RuleSet implements Serializable {
	
	private static final long serialVersionUID = 1L;
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
	
	public ProfileType getType(){
		return type;
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
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.ANGLE);
		
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
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.ANGLE);
		
		return builder
			.isLocalMinimum()
			.indexIsMoreThan(0.2) // assumes the profile is indexes on the RP
			.indexIsLessThan(0.6)
			.isMinimum()
			.build();
		
	}
	
	/**
	 * Create a RuleSet that describes how to find the top vertical in 
	 * mouse sperm nuclear profiles
	 * @return
	 */
	public static RuleSet mouseSpermTVRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.ANGLE);
		
		return builder
			.isConstantRegionAtValue(180, 10, 2)
			.isFirstIndexInRegion()
			.build();
	}
	
	/**
	 * Create a RuleSet that describes how to find the bottom 
	 * vertical in mouse sperm nuclear profiles
	 * @return
	 */
	public static RuleSet mouseSpermBVRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.ANGLE);
		
		return builder
			.isConstantRegionAtValue(180, 10, 2)
			.isLastIndexInRegion()
			.build();
	}
	
	/**
	 * Create a RuleSet that describes how to find the RP in 
	 * pig sperm nuclear profiles
	 * @return
	 */
	public static RuleSet pigSpermRPBackupRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.ANGLE);
				
		return builder
				.valueIsMoreThan(180)
				.isMaximum()
				.build();
	}
	
	/**
	 * Create a RuleSet that describes how to find the RP in 
	 * pig sperm nuclear profiles with poorly identifiable tails
	 * @return
	 */
	public static RuleSet pigSpermRPRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.ANGLE);
		
		return builder
				.isMinimum()                   // This will find one of the tail dimples
				.indexIsWithinFractionOf(0.07) // Expand to include indexes around the dimple
				.isLocalMaximum()              // Select the first local max point to avoid shoulders
				.build();
	}
	
	
	/**
	 * Create a RuleSet that describes how to find the RP in 
	 * round nucleus profiles
	 * @return
	 */
	public static RuleSet roundRPRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.DIAMETER);
		
		return builder
			.isMaximum()
			.build();
	}
	
	/**
	 * Create a RuleSet that describes how to find the RP in 
	 * round nucleus profiles
	 * @return
	 */
	public static RuleSet roundOPRuleSet(){
		
		RuleSetBuilder builder = new RuleSetBuilder(ProfileType.DIAMETER);
		
		return builder
			.isMinimum()
			.build();
	}
	
	
}
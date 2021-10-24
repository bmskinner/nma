package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.samples.IndividualNuclei;


public class RuleSetTester {
	
	@Test
	public void testMouseRPRuleSet() throws Exception{
	    IProfile median = IndividualNuclei.rodentSpermMedianProfile();

	    RuleSet r = RuleSet.mouseSpermRPRuleSet();
	    int hits = ProfileIndexFinder.countMatchingIndexes(median, r);
	    assertEquals(1, hits);

	    int rpIndex = ProfileIndexFinder.identifyIndex(median, r);
	    assertEquals(197, rpIndex);
	}
	
	@Test
	public void testMouseOPRuleSet() throws Exception{
	    IProfile median = IndividualNuclei.rodentSpermMedianProfileFromRP();

	    RuleSet r = RuleSet.mouseSpermOPRuleSet();

	    int hits = ProfileIndexFinder.countMatchingIndexes(median, r);
	    assertEquals(1, hits);

	    int index = ProfileIndexFinder.identifyIndex(median, r);
	    assertEquals(147, index);
	}

}

package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.samples.IndividualNuclei;


public class RuleSetTester {
	
	@Test
	public void testMouseRPRuleSet() throws Exception{
	    IProfile median = IndividualNuclei.rodentSpermMedianProfile();

	    ProfileIndexFinder finder = new ProfileIndexFinder();

	    RuleSet r = RuleSet.mouseSpermRPRuleSet();
	    int hits = finder.countMatchingIndexes(median, r);
	    assertEquals(1, hits);

	    int rpIndex = finder.identifyIndex(median, r);
	    assertEquals(197, rpIndex);
	}
	
	@Test
	public void testMouseOPRuleSet() throws Exception{
	    IProfile median = IndividualNuclei.rodentSpermMedianProfileFromRP();
	    for(int i=0; i<median.size(); i++){
            System.out.println(i+" : " +median.get(i));
        }
	    ProfileIndexFinder finder = new ProfileIndexFinder();

	    RuleSet r = RuleSet.mouseSpermOPRuleSet();

	    System.out.println(r.toString());

	    int hits = finder.countMatchingIndexes(median, r);
	    assertEquals(1, hits);

	    int index = finder.identifyIndex(median, r);
	    assertEquals(147, index);
	}

}

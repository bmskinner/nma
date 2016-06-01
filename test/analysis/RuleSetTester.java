package analysis;

import org.junit.Test;

import analysis.ProfileRulesetInderxer.RuleSet;
import Samples.IndividualNuclei;
import components.generic.Profile;
import components.generic.SegmentedProfile;

public class RuleSetTester {
	
	@Test
	public void testRuleSetsWork(){
		

		try {
			SegmentedProfile median = IndividualNuclei.rodentSpermMedianProfile();


			ProfileRulesetInderxer finder = new ProfileRulesetInderxer();

			RuleSet r = finder.createMouseSpermRPRuleSet();

			System.out.println(r.toString());

			int rpIndex = finder.identifyIndex(median, r);

			System.out.println("Found RP: "+rpIndex+" in profile of length "+median.size());
			
			System.out.println(median.valueString());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

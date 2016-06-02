package analysis;

import org.junit.Test;

import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.RuleSet;
import Samples.IndividualNuclei;
import components.generic.Profile;
import components.generic.SegmentedProfile;

public class RuleSetTester {
	
	@Test
	public void testMouseRPRuleSet(){
		
		System.out.println("\nTesting mouse RP rules\n");
		try {
			SegmentedProfile median = IndividualNuclei.rodentSpermMedianProfile();


			ProfileIndexFinder finder = new ProfileIndexFinder();

			RuleSet r = RuleSet.mouseSpermRPRuleSet();

			System.out.println(r.toString());
			
			int hits = finder.countMatchingIndexes(median, r);
			System.out.println("Total hits: "+hits);

			int rpIndex = finder.identifyIndex(median, r);

			System.out.println("Found RP: "+rpIndex+" in profile of length "+median.size());
			
//			System.out.println(median.valueString());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testMouseOPRuleSet(){
		System.out.println("\nTesting mouse OP rules\n");

		try {
			SegmentedProfile median = IndividualNuclei.rodentSpermMedianProfile();


			ProfileIndexFinder finder = new ProfileIndexFinder();

			RuleSet r = RuleSet.mouseSpermOPRuleSet();

			System.out.println(r.toString());
			
			int hits = finder.countMatchingIndexes(median, r);
			System.out.println("Total hits: "+hits);

			int rpIndex = finder.identifyIndex(median, r);

			System.out.println("Found OP: "+rpIndex+" in profile of length "+median.size());
			
//			System.out.println(median.valueString());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

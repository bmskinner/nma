package test.no.components;

import static org.junit.Assert.*;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.SegmentedProfile;
import no.nuclei.Nucleus;

import org.junit.Test;

import test.NucleusTest;
import test.SegmentFitterTest;

public class SegmentedProfileTest {


	public static SegmentedProfile createMedianProfile() throws Exception{
				
		SegmentedProfile profile = SegmentFitterTest.createRodentSpermMedianProfile();	
		profile.setSegments(SegmentFitterTest.getMedianRodentSpermSegments());
		return profile;
	}
	
	public static SegmentedProfile createNucleusProfile() throws Exception{
		
		Nucleus n = NucleusTest.createTestRodentSpermNucleus();
		SegmentedProfile profile = n.getAngleProfile();	
		return profile;
	}
	
	@Test
	public void profileCanBeReversed(){

		try {
			SegmentedProfile tester = createMedianProfile();

			SegmentedProfile result = createMedianProfile();
			result.reverse();
			result.reverse();

			for(String name : tester.getSegmentNames()){
				NucleusBorderSegment testerSeg = tester.getSegment(name);
				NucleusBorderSegment resultSeg = result.getSegment(name);
				
				assertEquals("Values should be identical", testerSeg.length(), resultSeg.length());
				assertEquals("Values should be identical", testerSeg.toString(), resultSeg.toString());
				System.out.println(testerSeg.toString());
				System.out.println(resultSeg.toString());
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}

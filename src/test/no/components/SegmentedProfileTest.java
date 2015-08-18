package test.no.components;

import static org.junit.Assert.*;
import no.components.SegmentedProfile;
import no.nuclei.Nucleus;

import org.junit.Test;

import test.NucleusTest;
import test.SegmentFitterTest;

public class SegmentedProfileTest {


	public static SegmentedProfile createMedianProfile(){
				
		SegmentedProfile profile = SegmentFitterTest.createRodentSpermMedianProfile();	
		profile.setSegments(SegmentFitterTest.getMedianRodentSpermSegments());
		return profile;
	}
	
	public static SegmentedProfile createNucleusProfile(){
		
		Nucleus n = NucleusTest.createTestRodentSpermNucleus();
		SegmentedProfile profile = n.getAngleProfile();	
		return profile;
	}
	
	

}

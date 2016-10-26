/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package components;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import analysis.SegmentFitterTest;
import components.generic.ISegmentedProfile;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

public class SegmentedProfileTest {


//	public static SegmentedProfile createMedianProfile() throws Exception{
//				
//		SegmentedProfile profile = SegmentFitterTest.createRodentSpermMedianProfile();	
//		profile.setSegments(SegmentFitterTest.getMedianRodentSpermSegments());
//		return profile;
//	}
	
	public static ISegmentedProfile createNucleusProfile() throws Exception{
		
		Nucleus n = NucleusTest.createTestRodentSpermNucleus();
		ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE);	
		return profile;
	}
	
//	@Test
//	public void profileCanBeReversed(){
//
//		try {
//			SegmentedProfile tester = createMedianProfile();
//
//			SegmentedProfile result = createMedianProfile();
//			result.reverse();
//			result.reverse();
//
//			for(String name : tester.getSegmentNames()){
//				NucleusBorderSegment testerSeg = tester.getSegment(name);
//				NucleusBorderSegment resultSeg = result.getSegment(name);
//				
//				assertEquals("Values should be identical", testerSeg.length(), resultSeg.length());
//				assertEquals("Values should be identical", testerSeg.toString(), resultSeg.toString());
//				System.out.println(testerSeg.toString());
//				System.out.println(resultSeg.toString());
//			}
//
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	

}

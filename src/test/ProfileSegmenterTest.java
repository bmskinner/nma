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
package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import analysis.nucleus.ProfileSegmenter;

import components.generic.Profile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;


public class ProfileSegmenterTest {

	/**
	 * Test the segmentation of a simulated rodent sperm nucleus
	 * median profile (values taken from actual exported median)
	 */
	@Test
	public void segmentMedianProfile() {
		System.out.println("Beginning median segmentation");
		Profile median = SegmentFitterTest.createRodentSpermMedianProfile();	
		File log = SegmentFitterTest.makeLogFile();
		ProfileSegmenter segmenter = new ProfileSegmenter(median, log);
		
		List<NucleusBorderSegment> list  = segmenter.segment();
				
		int length = 0;
		for(NucleusBorderSegment seg : list){
			assertEquals("Endpoints should be linked", seg.getEndIndex(), seg.nextSegment().getStartIndex());
			assertTrue(seg.hasNextSegment());
			assertTrue(seg.hasPrevSegment());
			
			length += seg.length();
			seg.print();
		}
		
		assertEquals("Lengths should match", median.size(), length);
		
	}
	
	/**
	 * Test the segmentation of a simulated rodent sperm nucleus
	 * angle profile (values taken from actual nucleus)
	 */
	@Test
	public void segmentNucleusProfile(){
		
		try{
		System.out.println("Beginning nucleus segmentation");
		Nucleus n = NucleusTest.createTestRodentSpermNucleus();
		File log = SegmentFitterTest.makeLogFile();
		ProfileSegmenter segmenter = new ProfileSegmenter(n.getAngleProfile(), log);
				
		List<NucleusBorderSegment> list  = segmenter.segment();
		
		
			
		int length = 0;
		for(NucleusBorderSegment seg : list){
			assertEquals("Endpoints should be linked", seg.getEndIndex(), seg.nextSegment().getStartIndex());
			assertTrue(seg.hasNextSegment());
			assertTrue(seg.hasPrevSegment());
			
			length += seg.length();
			seg.print();
		}
		
		assertEquals("Lengths should match", n.getAngleProfile().size(), length);
		}  catch(Exception e){
			System.out.println("Error");
		}
	}
	


}

package test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import no.analysis.ProfileSegmenter;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.nuclei.Nucleus;

import org.junit.Test;


public class ProfileSegmenterTest {

	/**
	 * Test the segmentation of a simulated rodent sperm nucleus
	 * median profile (values taken from actual exported median)
	 */
	@Test
	public void segmentMedianProfile() {
		System.out.println("Beginning median segmentation");
		Profile median = SegmentFitterTest.createRodentSpermMedianProfile();				
		ProfileSegmenter segmenter = new ProfileSegmenter(median);
		
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
		System.out.println("Beginning nucleus segmentation");
		Nucleus n = NucleusTest.createTestRodentSpermNucleus();
		ProfileSegmenter segmenter = new ProfileSegmenter(n.getAngleProfile());
				
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
	}
	


}

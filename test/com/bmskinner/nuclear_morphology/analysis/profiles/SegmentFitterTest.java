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
package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultSegmentedProfile;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class SegmentFitterTest {
	
	@Rule
	public final ExpectedException expectedException = ExpectedException.none();
	
	private SegmentFitter fitter;
	
	@Before
	public void setUp(){
	}
	
	@Test
	public void testFittingOnUnprofiledDatasetThrowsException() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.baseHeight(40)
				.baseWidth(40)
				.build();
		expectedException.expect(MissingProfileException.class);
		fitter = new SegmentFitter(d.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN));
		
	}
	
	/**
	 * A fitting may take a multi-segment source profile and fit it to a single-segment
	 * target. If this happens, the target should get the same segmentation pattern as 
	 * the source
	 * @throws Exception
	 */
	@Test
	public void testFittingCanApplyProfilesFromTemplate() throws Exception {
		
		// Make a segmented dataset
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.baseHeight(40)
				.baseWidth(40)
				.segmented()
				.build();
		assertTrue("Tempalte should have multiple segments", d.getCollection().getProfileManager().getSegmentCount()>1);
		
		ISegmentedProfile source = d.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
		
		// Create the fitter
		fitter = new SegmentFitter(source);
		
		// Create a single segment target profile of appropriate length
		IProfile targetVals = new DefaultProfile(100, d.getCollection().getMedianArrayLength());
		ISegmentedProfile target = new DefaultSegmentedProfile(targetVals);
		assertEquals("Target should have single segment", 1, target.getSegmentCount());
		
		ISegmentedProfile result = fitter.fit(target);
		assertEquals("Fitted profile should have multiple segments", source.getSegmentCount(), result.getSegmentCount());
	}
}

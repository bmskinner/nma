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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.Tag;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
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
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1).ofType(NucleusType.ROUND)
				.baseHeight(40).baseWidth(40).build();
		expectedException.expect(UnavailableProfileTypeException.class);
		fitter = new SegmentFitter(d.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN));
		
	}
	

	
//	@Test
//	public void testFittingOnSingleCellDatasetHasNoEffect() throws Exception {
//		IAnalysisDataset dataset = TestDatasetFactory.profileDataset(TestDatasetFactory.squareDataset(1));
//		expectedException.expect(UnavailableProfileTypeException.class);
//		fitter = new SegmentFitter(dataset.getCollection().getProfileCollection()
//				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN));
//		
//		fitter.fit(dataset.getCollection().getNuclei().stream().findFirst().get(), dataset.getCollection().getProfileCollection());
//		
//	}
	


}

/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
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

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileOffsetter.ProfileOffsetException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;


public class ProfileOffsetterTest {
    
    private static IAnalysisDataset d;
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    @Before
    public void init() {
        try {
            d = SampleDatasetReader.openTestRodentDataset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testProfileOffsetter() {
        ProfileOffsetter p = new ProfileOffsetter(d.getCollection());
    }
   
    @Test
    public void testAssignBorderTagToNucleiViaFrankenProfile() throws ProfileOffsetException {
        ProfileOffsetter p = new ProfileOffsetter(d.getCollection());
        p.assignLandmarkViaFrankenProfile(Landmark.ORIENTATION_POINT);
    }

    @Test
    public void testReCalculateVerticals() throws ProfileOffsetException {
        ProfileOffsetter p = new ProfileOffsetter(d.getCollection());
        p.reCalculateVerticals();

    }

}

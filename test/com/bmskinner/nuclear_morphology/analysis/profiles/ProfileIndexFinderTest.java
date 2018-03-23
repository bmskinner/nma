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

import static org.junit.Assert.*;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;

public class ProfileIndexFinderTest {

    @Test
    public void testGetMatchingIndexesIProfileRuleSet() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMatchingIndexesIProfileRule() {
        fail("Not yet implemented");
    }

    @Test
    public void testCountMatchingIndexes() {
        fail("Not yet implemented");
    }

    @Test
    public void testIdentifyIndexIProfileRuleSet() {
        fail("Not yet implemented");
    }

    @Test
    public void testIdentifyIndexIProfileListOfRuleSet() {
        fail("Not yet implemented");
    }

    @Test
    public void testIdentifyIndexICellCollectionTag() {
        fail("Not yet implemented");
    }

    @Test
    public void testIdentifyIndexICellCollectionListOfRuleSet() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMatchingProfile() {
        fail("Not yet implemented");
    }
    
    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getConsistentRegionBounds(double, double, int)}.
     */
    @Test
    public void testGetConsistentRegionBounds() {
        
        float[] arr = {  1, 2, 3, 4, 5, 5, 5, 5, 5, 6, 7, 8, 9 };
        
        IProfile p = new FloatProfile(arr);
        
        double value = 5;
        double tolerance = 0.1;
        int points = 5;
        
        int[] bounds = p.getConsistentRegionBounds(value, tolerance, points);
        
        assertEquals( 4, bounds[0] );
        assertEquals( 8, bounds[1] );
    }
    
    @Test
    public void testGetConsistentRegionBoundsFailsWhenTooShort() {
        
        float[] arr = {  1, 2, 3, 4, 5, 5, 5, 5, 6, 7, 8, 9 };
        
        IProfile p = new FloatProfile(arr);
        
        double value = 5;
        double tolerance = 0.1;
        int points = 5;
        
        int[] bounds = p.getConsistentRegionBounds(value, tolerance, points);
        
        assertEquals( -1, bounds[0] );
        assertEquals( -1, bounds[1] );
    }
    
    @Test
    public void testGetConsistentRegionBoundsSucceedssWhenToleranceRaised() {
        
        float[] arr = {  1, 2, 3, 4, 5, 5, 5, 5, 6, 7, 8, 9 };
        
        IProfile p = new FloatProfile(arr);
        
        double value = 5.5;
        double tolerance = 0.6;
        int points = 5;
        
        int[] bounds = p.getConsistentRegionBounds(value, tolerance, points);
        
        assertEquals( 4, bounds[0] );
        assertEquals( 8, bounds[1] );
    }

}

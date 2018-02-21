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

package com.bmskinner.nuclear_morphology.components.nuclei;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.samples.dummy.DummyRodentSpermNucleus;


public class DefaultRodentSpermNucleusTest {
    
    private Nucleus testNucleus;
    
    @Before
    public void setUp() throws ComponentCreationException{
        testNucleus = new DummyRodentSpermNucleus();
    }

    @Test
    public void testCalculateStatistic() {
        fail("Not yet implemented");
    }

    @Test
    public void testRotate() {
        fail("Not yet implemented");
    }

    @Test
    public void testFindPointsAroundBorder() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetBorderTagTagInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testDuplicate() {
        fail("Not yet implemented");
    }

    @Test
    public void testCalculateSignalAnglesFromPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testDumpInfo() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetVerticallyRotatedNucleus() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultRodentSpermNucleusRoiIPointFileIntIntArrayInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultRodentSpermNucleusNucleus() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetHookRoi() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsHookSide() {
        fail("Not yet implemented");
    }

    @Test
    public void testFindTailPointFromMinima() {
        fail("Not yet implemented");
    }

    @Test
    public void testFindTailByNarrowestWidthMethod() {
        fail("Not yet implemented");
    }

    @Test
    public void testSplitNucleusToHeadAndHump() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsClockwiseRP() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetEstimatedTailPoints() {
        fail("Not yet implemented");
    }

    @Test
    public void testAddTailEstimatePosition() {
        fail("Not yet implemented");
    }

    @Test
    public void testHashCode() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetScale() {
        fail("Not yet implemented");
    }

    @Test
    public void testFlipXAroundPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testMoveCentreOfMass() {
        fail("Not yet implemented");
    }

    @Test
    public void testOffset() {
        fail("Not yet implemented");
    }

    @Test
    public void testAlignVertically() {
        fail("Not yet implemented");
    }

    @Test
    public void testInitialise() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsProfileOrientationOK() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultNucleusRoiIPointFileIntIntArrayInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultNucleusNucleus() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetNucleusNumber() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetNameAndNumber() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPathAndNumber() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetSignals() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetSignalCollection() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdateSignalAngle() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdateVerticallyRotatedNucleus() {
        fail("Not yet implemented");
    }

    @Test
    public void testToString() {
        fail("Not yet implemented");
    }

    @Test
    public void testCompareTo() {
        fail("Not yet implemented");
    }

    @Test
    public void testEqualsObject() {
        fail("Not yet implemented");
    }

    @Test
    public void testReverse() {
        fail("Not yet implemented");
    }

    @Test
    public void testProfileableCellularComponentRoiIPointFileIntIntArray() {
        fail("Not yet implemented");
    }

    @Test
    public void testProfileableCellularComponentCellularComponent() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetProfileProfileTypeTagISegmentedProfile() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBorderTagTag() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBorderPointTag() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBorderTags() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetBorderTags() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBorderIndexTag() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetBorderTagTagTagInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testReplaceBorderTags() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasBorderTagTag() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasBorderTagInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasBorderTagTagInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetOffsetBorderIndex() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBorderTagTagInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBorderTagInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsLocked() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetLocked() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetWindowSize() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetWindowProportion() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetWindowProportion() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetProfileProfileType() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasProfile() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetProfileProfileTypeTag() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetProfileProfileTypeISegmentedProfile() {
        fail("Not yet implemented");
    }

    @Test
    public void testCalculateProfiles() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetSegmentStartLock() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetNarrowestDiameterPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetNarrowestDiameter() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetDistanceFromCoMToBorderAtAngle() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultCellularComponentRoiIPointFileIntIntArray() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultCellularComponentCellularComponent() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsSmoothByDefault() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetID() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPosition() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetOriginalBase() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBase() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBounds() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetSourceFolder() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdateSourceFolder() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetSourceFile() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetSourceFileName() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetSourceFileNameWithoutExtension() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetImage() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetRGBImage() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetComponentImage() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetComponentRGBImage() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetSourceFolder() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetSourceFile() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetChannel() {
        int expected = 0;
        assertThat(testNucleus.getChannel(), is(expected));
    }

    @Test
    public void testSetChannel() {
        int expected = 2;
        testNucleus.setChannel(expected);
        assertThat(testNucleus.getChannel(), is(expected));
    }

    @Test
    public void testGetScale() {
        double expected = 1;
        assertThat(testNucleus.getScale(), is(expected));
    }

    @Test
    public void testEqualsCellularComponent() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasStatistic() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetStatisticPlottableStatistic() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetStatisticPlottableStatisticMeasurementScale() {
        double scale = 5;
        
        // Get and save the values with default scale 1
        Map<PlottableStatistic, Double> map = new HashMap<>();
        for(PlottableStatistic stat : PlottableStatistic.getNucleusStats(NucleusType.RODENT_SPERM)){
            map.put(stat, testNucleus.getStatistic(stat));
        }
        
        // Update scale
        testNucleus.setScale(scale);
        
        // Get the actual values for microns and pixels
        for(PlottableStatistic stat : PlottableStatistic.getNucleusStats(NucleusType.RODENT_SPERM)){
            double m = testNucleus.getStatistic(stat, MeasurementScale.MICRONS);
            
            double expected = PlottableStatistic.convert(map.get(stat), scale, MeasurementScale.MICRONS, stat.getDimension());
            assertEquals(stat.toString(), expected, m, 0);
            
            double d = testNucleus.getStatistic(stat, MeasurementScale.PIXELS);
            assertEquals(stat.toString(), map.get(stat), d, 0);
        }        
    }

    @Test
    public void testSetStatistic() {
        double epsilon = 0; // the amount of difference permitted 
        double expected = 25;
        
        for(PlottableStatistic stat : PlottableStatistic.getNucleusStats(NucleusType.RODENT_SPERM)){
            testNucleus.setStatistic(stat, expected);
            double d = testNucleus.getStatistic(stat);
            assertEquals(stat.toString(), expected, d, epsilon);
        }
    }

    @Test
    public void testGetStatistics() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdateDependentStats() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetCentreOfMass() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetOriginalCentreOfMass() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBorderLength() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBorderPointInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetOriginalBorderPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBorderIndexIBorderPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdateBorderPointIntIPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdateBorderPointIntDoubleDouble() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBorderList() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetOriginalBorderList() {
        fail("Not yet implemented");
    }

    @Test
    public void testContainsPointIPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testContainsPointIntInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testContainsOriginalPointIPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testContainsOriginalPointIntInt() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMaxX() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMinX() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMaxY() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMinY() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMedianDistanceBetweenPoints() {
        fail("Not yet implemented");
    }

    @Test
    public void testToPolygon() {
        fail("Not yet implemented");
    }

    @Test
    public void testToOriginalPolygon() {
        fail("Not yet implemented");
    }

    @Test
    public void testToShape() {
        fail("Not yet implemented");
    }

    @Test
    public void testToShapeMeasurementScale() {
        fail("Not yet implemented");
    }

    @Test
    public void testToOriginalShape() {
        fail("Not yet implemented");
    }

    @Test
    public void testToRoi() {
        fail("Not yet implemented");
    }

    @Test
    public void testToOriginalRoi() {
        fail("Not yet implemented");
    }

    @Test
    public void testWrapIndex() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetBooleanMask() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetSourceBooleanMask() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPositionBetween() {
        fail("Not yet implemented");
    }

    @Test
    public void testFindOppositeBorder() {
        fail("Not yet implemented");
    }

    @Test
    public void testFindOrthogonalBorderPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testFindClosestBorderPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testRotatePointToBottom() {
        fail("Not yet implemented");
    }

}

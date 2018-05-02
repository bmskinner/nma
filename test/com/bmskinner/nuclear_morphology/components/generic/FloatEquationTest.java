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

package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.generic.DoubleEquation;
import com.bmskinner.nuclear_morphology.components.generic.FloatEquation;
import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.LineEquation;

/**
 * Tests for the floating point line equations
 * @author bms41
 *
 */
public class FloatEquationTest {
    
    private static final IPoint ORIGIN = new FloatPoint(0, 0);
    private static final FloatEquation X_EQ_Y = new FloatEquation(1, 0);

    @Test
    public void testFloatEquationFloatFloat() {
        FloatEquation d = new FloatEquation(1, 0);
    }
    
    @Test
    public void testEquationCanHandleVerticalLines() {
        Point2D p1 = new Point2D.Double(0, 0);
        Point2D p2 = new Point2D.Double(0, 1);
        FloatEquation d = new FloatEquation(p1, p2);
        
        LineEquation eq = d.getPerpendicular(IPoint.makeNew(p1));
        assertEquals(eq.getM(), 0, 0.0000001);
        assertEquals(eq.getC(), 0, 0.0000001);
        
    }

    @Test
    public void testFloatEquationIPointIPoint() {
    	
    	IPoint p0 = IPoint.makeNew(0d, 0d);
    	
    	for(int degree=0; degree<360; degree++) {
    		double x = Math.cos(Math.toRadians(degree));
    		double y = Math.sin(Math.toRadians(degree));
    		IPoint p1 = IPoint.makeNew(x, y);
    		
    		LineEquation l = new FloatEquation(p0, p1);    		
    		assertEquals(p1, l.getPointOnLine(p0, 1));
    	}
    	
        fail("Not yet implemented");
    }

    @Test
    public void testGetX() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetY() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetM() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetC() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsOnLine() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPointOnLine() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPerpendicular() {
        fail("Not yet implemented");
    }

    @Test
    public void testTranslate() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetIntercept() {
        fail("Not yet implemented");
    }

    @Test
    public void testIntersects() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetProportionalDistance() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetClosestDistanceToPoint() {
        fail("Not yet implemented");
    }

    @Test
    public void testCalculateBestFitLine() {
        fail("Not yet implemented");
    }

}

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

/**
 * 
 */
package com.bmskinner.nuclear_morphology.components.measure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * @author bms41
 *
 */
public class DoubleEquationTest {
    
    private static final IPoint ORIGIN = new FloatPoint(0, 0);
    private static final DoubleEquation X_EQ_Y = new DoubleEquation(1, 0);

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#DoubleEquation(double, double)}.
     */
    @Test
    public void testDoubleEquationDoubleDouble() {
        DoubleEquation d = new DoubleEquation(1, 0);
    }   
    
    @Test
    public void testDoubleEquationCanHandleVerticalLines() {
        Point2D p1 = new Point2D.Double(0, 0);
        Point2D p2 = new Point2D.Double(0, 1);
        DoubleEquation d = new DoubleEquation(p1, p2);
        
        LineEquation eq = d.getPerpendicular(IPoint.makeNew(p1));
        assertEquals(eq.getM(), 0, 0.0000001);
        assertEquals(eq.getC(), 0, 0.0000001);
        
    }
    

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#DoubleEquation(com.bmskinner.nuclear_morphology.components.generic.IPoint, com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
     */
    @Test
    public void testDoubleEquationIPointIPoint() {
        IPoint p2 = new FloatPoint(1, 1);
        DoubleEquation d1 = new DoubleEquation(ORIGIN, p2);
        assertEquals(X_EQ_Y, d1);
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#DoubleEquation(java.awt.geom.Point2D, java.awt.geom.Point2D)}.
     */
    @Test
    public void testDoubleEquationPoint2DPoint2D() {
        
        Point2D p1 = new Point2D.Double(0, 0);
        Point2D p2 = new Point2D.Double(1, 1);
        DoubleEquation d1 = new DoubleEquation(p1, p2);  
        assertEquals(X_EQ_Y, d1);
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#getX(double)}.
     */
    @Test
    public void testGetX() {
        double x = X_EQ_Y.getX(1);
        assertEquals(1, x, 0.000001);
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#getY(double)}.
     */
    @Test
    public void testGetY() {
        double y = X_EQ_Y.getY(1);
        assertEquals(1, y, 0.000001);
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#getM()}.
     */
    @Test
    public void testGetM() {
        double m = X_EQ_Y.getM();
        assertEquals(1, m, 0.000001);
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#getC()}.
     */
    @Test
    public void testGetC() {
        double c = X_EQ_Y.getC();
        assertEquals(0, c, 0.000001);
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#isOnLine(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
     */
    @Test
    public void testIsOnLine() {
        IPoint offLine = new FloatPoint(1, 0);
        
        assertTrue(X_EQ_Y.isOnLine(ORIGIN));
        assertFalse(X_EQ_Y.isOnLine(offLine));
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#getPointOnLine(com.bmskinner.nuclear_morphology.components.generic.IPoint, double)}.
     */
    @Test
    public void testGetPointOnLine() {
        IPoint pPos = X_EQ_Y.getPointOnLine(ORIGIN, 1);
        IPoint pNeg = X_EQ_Y.getPointOnLine(ORIGIN, -1);
        
        IPoint p1 = IPoint.makeNew(Math.sqrt(0.5),Math.sqrt(0.5)); 
        assertEquals("x", p1.getX(), pPos.getX(), 0.000001); 
        assertEquals("y", p1.getY(), pPos.getY(), 0.000001);
        
        IPoint p2 = IPoint.makeNew(-Math.sqrt(0.5),-Math.sqrt(0.5)); 
        assertEquals("x", p2.getX(), pNeg.getX(), 0.000001); 
        assertEquals("y", p2.getY(), pNeg.getY(), 0.000001);
        
        
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#getPerpendicular(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
     */
    @Test
    public void testGetPerpendicular() {
        LineEquation l = X_EQ_Y.getPerpendicular(ORIGIN);
        LineEquation exp = new DoubleEquation(-1, 0);
        assertEquals(exp, l);    
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#translate(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
     */
    @Test
    public void testTranslate() {
        IPoint above = new FloatPoint(0, 1);
        LineEquation l = X_EQ_Y.translate(above); 
        LineEquation exp = new DoubleEquation(1, 1);
        assertEquals(exp, l);    
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#getIntercept(com.bmskinner.nuclear_morphology.components.measure.LineEquation)}.
     */
    @Test
    public void testGetIntercept() {
        LineEquation l = new DoubleEquation(-1, 0);
        IPoint p = X_EQ_Y.getIntercept(l); 
        assertEquals(p, ORIGIN);    
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.measure.DoubleEquation#intersects(com.bmskinner.nuclear_morphology.components.measure.DoubleEquation)}.
     */
    @Test
    public void testIntersects() {
        LineEquation parallel = new DoubleEquation(1, 1);
        LineEquation intersects = new DoubleEquation(2, 1);
        assertFalse(parallel.intersects(X_EQ_Y));
        assertTrue(intersects.intersects(X_EQ_Y));
       
    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DoubleEquation#getProportionalDistance(com.bmskinner.nuclear_morphology.components.generic.IPoint, com.bmskinner.nuclear_morphology.components.generic.IPoint, double)}.
     */
//    @Test
//    public void testGetProportionalDistance() {
//        LineEquation y_eq_1 = new DoubleEquation(0, 1); // y=1
//        IPoint x_zero = new FloatPoint(0, 1);
//        IPoint x_ten  = new FloatPoint(10, 1);
//        
//        y_eq_1.getPr
//        fail("Not yet implemented");
//    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DoubleEquation#getClosestDistanceToPoint(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
     */
//    @Test
//    public void testGetClosestDistanceToPoint() {
//        fail("Not yet implemented");
//    }

    /**
     * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DoubleEquation#calculateBestFitLine(java.util.List)}.
     */
//    @Test
//    public void testCalculateBestFitLine() {
//        fail("Not yet implemented");
//    }
}

/**
 * 
 */
package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * @author ben
 *
 */
public class FloatPointTest {
    
    private static final IPoint CENTRE_POINT = new FloatPoint(0, 0);
    private static final IPoint POINT_10x_10y = new FloatPoint(10, 10);
    private static final IPoint POINT_n10x_n10y = new FloatPoint(-10, -10);
    private static final IPoint POINT_0x_10y = new FloatPoint(0, 10);
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#FloatPoint(float, float)}.
	 */
	@Test
	public void testFloatPointFloatFloat() {
		FloatPoint p = new FloatPoint(10f, 10f);
		assertEquals(POINT_10x_10y, p);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#FloatPoint(double, double)}.
	 */
	@Test
	public void testFloatPointDoubleDouble() {
	    
	    IPoint test = new FloatPoint(10d, 10d);
	    assertEquals(POINT_10x_10y, test);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#FloatPoint(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testFloatPointIPoint() {
	    IPoint test = new FloatPoint(POINT_10x_10y);
	    assertEquals(POINT_10x_10y, test);
	}
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#getXAsInt()}.
	 */
	@Test
	public void testGetXAsIntWhenBelowHalf() {
	    IPoint test = new FloatPoint(10.1, 10.1);
		assertEquals(10, test.getXAsInt());
	}
	
	@Test
    public void testGetXAsIntWhenEqualHalf() {
        IPoint test = new FloatPoint(10.5, 10.5);
        assertEquals(11, test.getXAsInt());
    }

	@Test
	public void testGetXAsIntWhenAboveHalf() {
	    IPoint test = new FloatPoint(10.7, 10.7);
	    assertEquals(11, test.getXAsInt());
	}

	@Test
	public void testGetYAsIntWhenBelowHalf() {
	    IPoint test = new FloatPoint(10.1, 10.1);
	    assertEquals(10, test.getYAsInt());
	}

	@Test
	public void testGetYAsIntWhenEqualHalf() {
	    IPoint test = new FloatPoint(10.5, 10.5);
	    assertEquals(11, test.getYAsInt());
	}

	@Test
	public void testGetYAsIntWhenAboveHalf() {
	    IPoint test = new FloatPoint(10.7, 10.7);
	    assertEquals(11, test.getYAsInt());
	}
	

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#set(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testSet() {
	    FloatPoint f = new FloatPoint(CENTRE_POINT);
	    f.set(POINT_10x_10y);
		assertEquals(POINT_10x_10y, f);
	}

	@Test
	public void testSetExceptsOnNull() {
	    FloatPoint f = new FloatPoint(CENTRE_POINT);
	    exception.expect(IllegalArgumentException.class);
	    f.set(null);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#getLengthTo(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testGetLengthTo() {
	    FloatPoint f = new FloatPoint(0, 10);
	    assertEquals(10, CENTRE_POINT.getLengthTo(f), 0.000001);
	}
	
	@Test
	public void testGetLengthToExceptsOnNull() {
	    exception.expect(IllegalArgumentException.class);
	    CENTRE_POINT.getLengthTo(null);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#overlaps(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testOverlaps() {
	    FloatPoint f1 = new FloatPoint(10.77777, 11.888888);
	    FloatPoint f2 = new FloatPoint(10.77777, 11.888888);
	    assertTrue(f1.overlaps(f2));
	}
	
	@Test
	public void testOverlapsExceptsOnNull() {
	    exception.expect(IllegalArgumentException.class);
	    CENTRE_POINT.overlaps(null);
	}
	
	@Test
    public void testOverlapsReturnsTrueOnIntX() {
        FloatPoint f1 = new FloatPoint(11.33333, 11.888888);
        FloatPoint f2 = new FloatPoint(10.77776, 11.888888);
        assertTrue(f1.overlaps(f2));
    }
	    
	@Test
	public void testOverlapsReturnsFalseOnDifferentX() {
	    FloatPoint f1 = new FloatPoint(10.77777, 11.888888);
	    FloatPoint f2 = new FloatPoint(11.77777, 11.888888);
	    assertFalse(f1.overlaps(f2));
	}
	    
	@Test
	public void testOverlapsReturnsTrueOnIntY() {
	    FloatPoint f1 = new FloatPoint(11.888888, 11.33333);
	    FloatPoint f2 = new FloatPoint(11.888888, 10.77776);
	    assertTrue(f1.overlaps(f2));
	}
	
	@Test
    public void testOverlapsReturnsFalseOnDifferentY() {
        FloatPoint f1 = new FloatPoint(11.888888, 10.77777);
        FloatPoint f2 = new FloatPoint(11.888888, 11.77777);
        assertFalse(f1.overlaps(f2));
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#isAbove(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testIsAbove() {
	    FloatPoint f = new FloatPoint(0, 10);
        assertTrue(f.isAbove(CENTRE_POINT));
	}
	
	@Test
	public void testIsAboveReturnsFalseOnSelf() {
	    assertFalse(CENTRE_POINT.isAbove(CENTRE_POINT));
	}

	@Test
	public void testIsAboveExceptsOnNull() {
	    exception.expect(IllegalArgumentException.class);
	    CENTRE_POINT.isAbove(null);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#isBelow(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
    @Test
    public void testIsBelow() {
        FloatPoint f = new FloatPoint(0, -10);
        assertTrue(f.isBelow(CENTRE_POINT));
    }
    
    @Test
    public void testIsBelowReturnsFalseOnSelf() {
        assertFalse(CENTRE_POINT.isBelow(CENTRE_POINT));
    }

    @Test
    public void testIsBelowExceptsOnNull() {
        exception.expect(IllegalArgumentException.class);
        CENTRE_POINT.isBelow(null);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#isLeftOf(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
    @Test
    public void testIsLeftOf() {
        FloatPoint f = new FloatPoint(-10, 0);
        assertTrue(f.isLeftOf(CENTRE_POINT));
    }
    
    @Test
    public void testIsLeftOfReturnsFalseOnSelf() {
        assertFalse(CENTRE_POINT.isLeftOf(CENTRE_POINT));
    }

    @Test
    public void testIsLeftOfExceptsOnNull() {
        exception.expect(IllegalArgumentException.class);
        CENTRE_POINT.isLeftOf(null);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#isRightOf(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
    @Test
    public void testIsRightOf() {
        FloatPoint f = new FloatPoint(10, 0);
        assertTrue(f.isRightOf(CENTRE_POINT));
    }
    
    @Test
    public void testIsRightOfReturnsFalseOnSelf() {
        assertFalse(CENTRE_POINT.isRightOf(CENTRE_POINT));
    }

    @Test
    public void testIsRightOfExceptsOnNull() {
        exception.expect(IllegalArgumentException.class);
        CENTRE_POINT.isRightOf(null);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#offset(double, double)}.
	 */
	@Test
	public void testOffset() {
	    FloatPoint f = new FloatPoint(0, 0);
	    f.offset(10, 10);
	    assertEquals(POINT_10x_10y, f);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#overlapsPerfectly(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testOverlapsPerfectly() {
	    FloatPoint f1 = new FloatPoint(10.77777, 11.888888);
	    FloatPoint f2 = new FloatPoint(10.77777, 11.888888);
	    assertTrue(f1.overlapsPerfectly(f2));
	}
	
	@Test
	public void testOverlapsPerfectlyReturnsFalseOnDifferentX() {
	    FloatPoint f1 = new FloatPoint(10.77777, 11.888888);
	    FloatPoint f2 = new FloatPoint(10.77776, 11.888888);
	    assertFalse(f1.overlapsPerfectly(f2));
	}
	
	@Test
    public void testOverlapsPerfectlyReturnsFalseOnDifferentY() {
        FloatPoint f1 = new FloatPoint(10.77777, 11.888888);
        FloatPoint f2 = new FloatPoint(10.77777, 11.888887);
        assertFalse(f1.overlapsPerfectly(f2));
    }
	
	@Test
	public void testOverlapsPerfectlyExceptsOnNull() {
	    exception.expect(IllegalArgumentException.class);
	    CENTRE_POINT.overlapsPerfectly(null);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#toPoint2D()}.
	 */
	@Test
	public void testToPoint2D() {
	    Point2D exp = new Point2D.Float(10, 10);  
	    assertEquals(exp, POINT_10x_10y.toPoint2D());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#findAngle(com.bmskinner.nuclear_morphology.components.generic.IPoint, com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testFindAngle() {
		
		/*
		 *                     |
		 *                     b       c
		 *                     |
		 *                     |
		 *                     |
		 *      ______d________a_______e________
		 *                     |
		 *                     |
		 *                     |
		 *                     |       f
		 *                     |
		 * 
		 * 
		 */
		
		IPoint d = new FloatPoint(-10, 0);
		IPoint e = new FloatPoint(10, 0);
		IPoint f = new FloatPoint(10, -10);
		
		// 90 degrees
		
		double exp = 90;
		
		double angle = POINT_0x_10y.findAngle(CENTRE_POINT, POINT_10x_10y);
		
		assertEquals(exp, angle, 0);
		
		// 90 degrees
		
		exp = 90;
		angle = CENTRE_POINT.findAngle(POINT_10x_10y, f);
		assertEquals(exp, angle, 0);
		
		// 0 degrees
		
		exp=0;
		angle = POINT_0x_10y.findAngle(CENTRE_POINT, CENTRE_POINT);
		assertEquals(exp, angle, 0);
		
		// 180 degrees horiz
		exp=180;
		
		angle = CENTRE_POINT.findAngle(d, e);
		assertEquals(exp, angle, 0);
		
		// 180 degrees vert
		exp=180;
		
		angle = e.findAngle(POINT_10x_10y, f);
		assertEquals(exp, angle, 0);
		
		// 135 degrees in -y axis
		exp=135;
		
		angle = CENTRE_POINT.findAngle(d, f);
		assertEquals(exp, angle, 0);
	}
	
	@Test
	public void testFindAngleExceptsOnNullPoint1(){
	    IPoint f = new FloatPoint(10, -10);
	    exception.expect(IllegalArgumentException.class);
	    CENTRE_POINT.findAngle(null, f);
	}
	
	@Test
    public void testFindAngleExceptsOnNullPoint2(){
        IPoint f = new FloatPoint(10, -10);
        exception.expect(IllegalArgumentException.class);
        CENTRE_POINT.findAngle(f, null);
    }
	
	@Test
	public void testMinusIPoint(){
	    IPoint test = POINT_10x_10y.minus(new FloatPoint(5, 5));
	    IPoint exp = new FloatPoint(5, 5);
	    assertEquals(exp, test);
	}
	
	@Test
    public void testMinusIPointExceptsOnNull(){
	    exception.expect(IllegalArgumentException.class);
        POINT_10x_10y.minus(null);
    }
	
	@Test
    public void testPlusIPoint(){
        IPoint test = POINT_10x_10y.plus(new FloatPoint(5, 5));
        IPoint exp = new FloatPoint(15, 15);
        assertEquals(exp, test);
    }
    
    @Test
    public void testPlusIPointExceptsOnNull(){
        exception.expect(IllegalArgumentException.class);
        POINT_10x_10y.plus(null);
    }
    
    @Test
    public void testMinusDouble(){
        IPoint test = POINT_10x_10y.minus(5);
        IPoint exp = new FloatPoint(5, 5);
        assertEquals(exp, test);
    }
    
    @Test
    public void testPlusDouble(){
        IPoint test = POINT_10x_10y.plus(5);
        IPoint exp = new FloatPoint(15, 15);
        assertEquals(exp, test);
    }

}

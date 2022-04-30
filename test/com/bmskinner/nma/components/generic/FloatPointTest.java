/**
 * 
 */
package com.bmskinner.nma.components.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;

/**
 * @author ben
 *
 */
public class FloatPointTest {
    
    private static final @NonNull IPoint CENTRE_POINT = new FloatPoint(0, 0);
    private static final @NonNull IPoint POINT_10x_10y = new FloatPoint(10, 10);
    private static final @NonNull IPoint POINT_n10x_n10y = new FloatPoint(-10, -10);
    private static final @NonNull IPoint POINT_0x_10y = new FloatPoint(0, 10);
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();

	
	@Test
	public void testDuplicateCopiesDefensively() {
		FloatPoint p = new FloatPoint(10f, 10f);
		IPoint q = p.duplicate();
		assertEquals("Equality test", p, q);
		q = q.plus(10);
		assertNotEquals("Defensive test", p, q);
	}
	
	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#FloatPoint(float, float)}.
	 */
	@Test
	public void testFloatPointFloatFloat() {
		FloatPoint p = new FloatPoint(10f, 10f);
		assertEquals(POINT_10x_10y, p);
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#FloatPoint(double, double)}.
	 */
	@Test
	public void testFloatPointDoubleDouble() {
	    
	    IPoint test = new FloatPoint(10d, 10d);
	    assertEquals(POINT_10x_10y, test);
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#FloatPoint(com.bmskinner.nma.components.generic.IPoint)}.
	 */
	@Test
	public void testFloatPointIPoint() {
	    IPoint test = new FloatPoint(POINT_10x_10y);
	    assertEquals(POINT_10x_10y, test);
	}
	
	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#getXAsInt()}.
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
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#set(com.bmskinner.nma.components.generic.IPoint)}.
	 */
	@Test
	public void testSet() {
	    FloatPoint f = new FloatPoint(CENTRE_POINT);
	    f.set(POINT_10x_10y);
		assertEquals(POINT_10x_10y, f);
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#getLengthTo(com.bmskinner.nma.components.generic.IPoint)}.
	 */
	@Test
	public void testGetLengthTo() {
	    FloatPoint f = new FloatPoint(0, 10);
	    assertEquals(10, CENTRE_POINT.getLengthTo(f), 0.000001);
	}
	

	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#overlaps(com.bmskinner.nma.components.generic.IPoint)}.
	 */
	@Test
	public void testOverlaps() {
	    FloatPoint f1 = new FloatPoint(10.77777, 11.888888);
	    FloatPoint f2 = new FloatPoint(10.77777, 11.888888);
	    assertTrue(f1.overlaps(f2));
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
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#isAbove(com.bmskinner.nma.components.generic.IPoint)}.
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


	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#isBelow(com.bmskinner.nma.components.generic.IPoint)}.
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

	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#isLeftOf(com.bmskinner.nma.components.generic.IPoint)}.
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

	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#isRightOf(com.bmskinner.nma.components.generic.IPoint)}.
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

	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#offset(double, double)}.
	 */
	@Test
	public void testOffset() {
	    FloatPoint f = new FloatPoint(0, 0);
	    f.offset(10, 10);
	    assertEquals(POINT_10x_10y, f);
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#overlapsPerfectly(com.bmskinner.nma.components.generic.IPoint)}.
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
	
	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#toPoint2D()}.
	 */
	@Test
	public void testToPoint2D() {
	    Point2D exp = new Point2D.Float(10, 10);  
	    assertEquals(exp, POINT_10x_10y.toPoint2D());
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.generic.FloatPoint#findSmallestAngle(com.bmskinner.nma.components.generic.IPoint, com.bmskinner.nma.components.generic.IPoint)}.
	 */
	@Test
	public void testFindSmallestAngle() {
		
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
		
		double angle = POINT_0x_10y.findSmallestAngle(CENTRE_POINT, POINT_10x_10y);
		
		assertEquals(exp, angle, 0);
		
		// 90 degrees
		
		exp = 90;
		angle = CENTRE_POINT.findSmallestAngle(POINT_10x_10y, f);
		assertEquals(exp, angle, 0);
		
		// 0 degrees
		
		exp=0;
		angle = POINT_0x_10y.findSmallestAngle(CENTRE_POINT, CENTRE_POINT);
		assertEquals(exp, angle, 0);
		
		// 180 degrees horiz
		exp=180;
		
		angle = CENTRE_POINT.findSmallestAngle(d, e);
		assertEquals(exp, angle, 0);
		
		// 180 degrees vert
		exp=180;
		
		angle = e.findSmallestAngle(POINT_10x_10y, f);
		assertEquals(exp, angle, 0);
		
		// 135 degrees in -y axis
		exp=135;
		
		angle = CENTRE_POINT.findSmallestAngle(d, f);
		assertEquals(exp, angle, 0);
	}
	
	@Test
	public void testFindAbsoluteAngle() {
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
		
		IPoint a = CENTRE_POINT;
		IPoint b = POINT_0x_10y;
		IPoint c = POINT_10x_10y;
		IPoint d = new FloatPoint(-10, 0);
		IPoint e = new FloatPoint(10, 0);
		IPoint f = new FloatPoint(10, -10);
		
		// 90 degrees clockwise
		assertEquals(90, a.findAbsoluteAngle(b, e), 0);
		assertEquals(270, a.findAbsoluteAngle(e, b), 0);
		
		// 270 degrees clockwise
		assertEquals(270, a.findAbsoluteAngle(b, d), 0);
		assertEquals(90, a.findAbsoluteAngle(d, b), 0);
		
		// 0 degrees 
		assertEquals(0, a.findAbsoluteAngle(b, b), 0);
		
		// 180 degrees
		assertEquals(180, a.findAbsoluteAngle(d, e), 0);
		assertEquals(180, a.findAbsoluteAngle(e, d), 0);
	}
			
	@Test
	public void testMinusIPoint(){
	    IPoint test = POINT_10x_10y.minus(new FloatPoint(5, 5));
	    IPoint exp = new FloatPoint(5, 5);
	    assertEquals(exp, test);
	}
	
	@Test
    public void testPlusIPoint(){
        IPoint test = POINT_10x_10y.plus(new FloatPoint(5, 5));
        IPoint exp = new FloatPoint(15, 15);
        assertEquals(exp, test);
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

/**
 * 
 */
package components.generic;

import static org.junit.Assert.*;

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
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#FloatPoint(float, float)}.
	 */
	@Test
	public void testFloatPointFloatFloat() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#FloatPoint(double, double)}.
	 */
	@Test
	public void testFloatPointDoubleDouble() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#FloatPoint(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testFloatPointIPoint() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#getXAsInt()}.
	 */
	@Test
	public void testGetXAsInt() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#getYAsInt()}.
	 */
	@Test
	public void testGetYAsInt() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#set(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testSet() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#getLengthTo(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testGetLengthTo() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#overlaps(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testOverlaps() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#isAbove(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testIsAbove() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#isBelow(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testIsBelow() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#isLeftOf(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testIsLeftOf() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#isRightOf(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testIsRightOf() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#offset(double, double)}.
	 */
	@Test
	public void testOffset() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#overlapsPerfectly(com.bmskinner.nuclear_morphology.components.generic.IPoint)}.
	 */
	@Test
	public void testOverlapsPerfectly() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatPoint#toPoint2D()}.
	 */
	@Test
	public void testToPoint2D() {
		fail("Not yet implemented");
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
		
		
		IPoint a = new FloatPoint(0, 0);
		IPoint b = new FloatPoint(0, 10);
		IPoint c = new FloatPoint(10, 10);
		IPoint d = new FloatPoint(-10, 0);
		IPoint e = new FloatPoint(10, 0);
		IPoint f = new FloatPoint(10, -10);
		
		// 90 degrees
		
		double exp = 90;
		
		double angle = b.findAngle(a, c);
		
		assertEquals(exp, angle, 0);
		
		// 90 degrees
		
		exp = 90;
		angle = a.findAngle(c, f);
		assertEquals(exp, angle, 0);
		
		// 0 degrees
		
		exp=0;
		angle = b.findAngle(a, a);
		assertEquals(exp, angle, 0);
		
		// 180 degrees horiz
		exp=180;
		
		angle = a.findAngle(d, e);
		assertEquals(exp, angle, 0);
		
		// 180 degrees vert
		exp=180;
		
		angle = e.findAngle(c, f);
		assertEquals(exp, angle, 0);
		
		// 135 degrees in -y axis
		exp=135;
		
		angle = a.findAngle(d, f);
		assertEquals(exp, angle, 0);
		
		// Null input
		exception.expect(IllegalArgumentException.class);
		angle = a.findAngle(null, f);
		angle = a.findAngle(null, null);
		angle = a.findAngle(f, null);
		
				
		
	}

}

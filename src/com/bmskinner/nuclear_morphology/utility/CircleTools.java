/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.utility;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * Utility functions involving circles
 * @author bms41
 * @since 1.13.5
 *
 */
public class CircleTools {
	
	/**
	 * Find if the given circles overlap
	 * @param com1 the centre of the first circle
	 * @param r1 the radius of the first circle
	 * @param com2 the centre of the second circle
	 * @param r2 the radius of the second circle
	 * @return
	 */
	public static boolean circlesOverlap(IPoint com1, double r1, IPoint com2, double r2){
		double d = com1.getLengthTo(com2); 
		double r1r2 = Math.abs(r1+r2);
		return d < r1r2;
	}
	
	
	/**
	 * Test if either circle is entirely contained within the other
	 * @param com1 the centre of the first circle
	 * @param r1 the radius of the first circle
	 * @param com2 the centre of the second circle
	 * @param r2 the radius of the second circle
	 * @return true if either circle is contained within the other
	 */
	public static boolean circleIsEnclosed(IPoint com1, double r1, IPoint com2, double r2){
		double d = com1.getLengthTo(com2); 
		return d < Math.abs(r1 - r2);
	}
	
	/**
	 * Find the intersection points of two circles. Based on:
	 * http://paulbourke.net/geometry/circlesphere/
	 * @param com1 the centre of the first circle
	 * @param r1 the radius of the first circle
	 * @param com2 the centre of the second circle
	 * @param r2 the radius of the second circle
	 * @return
	 */
	public static IPoint[] findIntersections(IPoint com0, double r0, IPoint com1, double r1){
		
		double a, dx, dy, d, h, rx, ry;

		double x0 = com0.getX();
		double x1 = com1.getX();

		double y0 = com0.getY();
		double y1 = com1.getY();

		double x2, y2;

		/* dx and dy are the vertical and horizontal distances between
		 * the circle centers.
		 */
		dx = x1 - x0;
		dy = y1 - y0;

		/* Determine the straight-line distance between the centers. */
		d = Math.sqrt((dy*dy) + (dx*dx));

		//		d = com0.getLengthTo(com1); 



		/* Check for solvability. */

		// If d > r0 + r1 then there are no solutions, the circles are separate.

		if(!circlesOverlap(com0, r0, com1, r1)){
			throw new IllegalArgumentException("Circles do not overlap" );
		}

		//  If d < |r0 - r1| then there are no solutions because one circle is contained within the other.
		if(circleIsEnclosed(com0, r0, com1, r1)){
			throw new IllegalArgumentException("One circle is contained within the other");
		}


		// If d = 0 and r0 = r1 then the circles are coincident and there are an infinite number of solutions.
		if(d==0 && r0==r1){
			throw new IllegalArgumentException("Circles overlap perfectly");
		}

		double xi, yi, xi_prime, yi_prime;

		  /* 'point 2' is the point where the line through the circle
		   * intersection points crosses the line between the circle
		   * centers.  
		   */

		  /* Determine the distance from point 0 to point 2. */
		  a = ((r0*r0) - (r1*r1) + (d*d)) / (2.0 * d) ;

		  /* Determine the coordinates of point 2. */
		  x2 = x0 + (dx * a/d);
		  y2 = y0 + (dy * a/d);

		  /* Determine the distance from point 2 to either of the
		   * intersection points.
		   */
		  h = Math.sqrt((r0*r0) - (a*a));

		  /* Now determine the offsets of the intersection points from
		   * point 2.
		   */
		  rx = -dy * (h/d);
		  ry = dx * (h/d);

		  /* Determine the absolute intersection points. */
		  xi = x2 + rx;
		  xi_prime = x2 - rx;
		  yi = y2 + ry;
		  yi_prime = y2 - ry;

			IPoint first  = IPoint.makeNew(xi, yi);
			IPoint second = IPoint.makeNew(xi_prime,yi_prime);
			
			IPoint[] result = { first, second };
			return result;

//		// Considering the two triangles P0P2P3 and P1P2P3 we can write
//
//		// a2 + h2 = r02 and b2 + h2 = r12
//		
//		
//
//		// Using d = a + b we can solve for a,
//
//		// a = (r02 - r12 + d2 ) / (2 d)
//		
//		double a = ((r1*r1) - (r2*r2) + (d*d)) / (2*d);
//		
//		double x1 = com1.getX();
//		double x2 = com2.getX();
//		double y1 = com1.getY();
//		double y2 = com2.getY();
//		
//		double dx = x1-x2;
//		double dy = y1-y2;
//
//		// It can be readily shown that this reduces to r0 when the two circles touch at one point, ie: d = r0 + r1
//
//		// Solve for h by substituting a into the first equation, h2 = r02 - a2
//
//		// So:
//
//		//P2 = P0 + a ( P1 - P0 ) / d
//
//
//		//And finally, P3 = (x3,y3) in terms of P0 = (x0,y0), P1 = (x1,y1) and P2 = (x2,y2), is
//
//		//x3 = x2 +- h ( y1 - y0 ) / d
//
//		//y3 = y2 -+ h ( x1 - x0 ) / d
//				
//
//		double h = Math.sqrt( r1*r1 - a*a);
//		double xm = x1 + (a*dx)/d;
//		double ym = y1 + (a*dy)/d;
//		double xs1 = xm + (h*dy)/d;
//		double xs2 = xm - (h*dy)/d;
//		double ys1 = ym - (h*dx)/d;
//		double ys2 = ym + (h*dx)/d;
//		
//		IPoint first  = IPoint.makeNew(xs1, ys1);
//		IPoint second = IPoint.makeNew(xs2,ys2);
//		
//		IPoint[] result = { first, second };
//		return result;
	}

}

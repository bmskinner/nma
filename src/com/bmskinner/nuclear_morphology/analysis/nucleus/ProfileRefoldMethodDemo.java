package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.stats.Quartile;

/**
 * Testing new ways to refold profiles
 * @author ben
 *
 */
public class ProfileRefoldMethodDemo extends AbstractAnalysisMethod {
	
	public ProfileRefoldMethodDemo(IAnalysisDataset dataset) {
		super(dataset);
	}


	
	@Override
	public IAnalysisResult call() throws Exception {

		run();		
		IAnalysisResult r = new DefaultAnalysisResult(dataset);
		return r;
	}
	
	private void run() {
		
		try {
		
		IProfile angleProfile  = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);
		
		IProfile radiusProfile = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.RADIUS, Tag.REFERENCE_POINT, Quartile.MEDIAN);
		
		
		// Go windowsize points around the profile
		
		int points = angleProfile.size();
		
		int window = (int) (dataset.getAnalysisOptions().getProfileWindowProportion() * points);
		// Get the distance between points
		int distance = window;
		
		List<IPoint> list = new ArrayList<>();
		
		IPoint com = IPoint.makeNew(0, 0);
		
		// Make the first point on the x axis, radius from zero
		IPoint first = IPoint.makeNew(radiusProfile.get(0), 0);
		
		list.add(first);
		log(first.toString());
		
		// Now calculate the position of the next point, which is 
		// radius away from CoM, and can be in any direction from 
		// the first point
		
		/*
		 * Looking for the intersection of the circles
		 * with radii described
		 */
		IPoint[] interesctions = null;
		boolean loop = true;
		while( loop && distance > 1 && distance < 1000){
			try {
				interesctions = intersectionPoints(first, distance, com, radiusProfile.get(window) );
				loop=false;
			} catch(IllegalArgumentException e){
				log(e.getMessage());
				distance++;
				log("Increasing distance to "+distance);
				
			}
		}
		
		
		IPoint second = interesctions[0];
		list.add(second);
		log(second.toString());
		
		// We can now use the angle from the previous point with the radius
		// to find a position
		
		for(int i=window,  j=1; i<points; i+=window, j++){
			
			double r = radiusProfile.get(i+1);
			double a = angleProfile.get(i);
			distance = window;
			
			// In the  first iteration of the loop
			// a is the angle from first to third via second
			// r is the distance from com to third
			
			// The next point must be one of the intersections between
			// point two circle and the com circle
			
			// Find which intersection is closest to the desired angle
			IPoint prev    = list.get(j-1);
			IPoint current = list.get(j);
			
			
			
			IPoint[] inters = null;
			loop = true;
			while( loop && distance < 1000){
				try {
					inters = intersectionPoints(com, r, current, distance);
					loop=false;
				} catch(IllegalArgumentException e){
					log(e.getMessage());
					distance++;
					log("Increasing distance to "+distance);
					
				}
			}
			
			// We have the two intersections of the circles. One of these will be closer 
			// to the desired angle

			double a1 = current.findAngle(prev, inters[0]);
			double a2 = current.findAngle(prev, inters[1]);
			
			// The angle should be the interior angle of the object being drawn
			// If the desired angle is >180 degrees, correct
			
			if(a>180){
				a1+=180;
				a2+=180;
			}
			
			// get the differences to the desired angle
			
			double d1 = Math.abs(a1-a);
			double d2 = Math.abs(a2-a);
			

			
			IPoint next = d1 < d2 ? inters[0] : inters[1];
			
			if(next.getLengthTo(current) > distance){
				log("Error getting points");
			}

			list.add(next);
			
		}
		
		log("##################");
		for(IPoint p : list){
			log("\t"+p.getX()+"\t"+p.getY());
		}
		
		} catch(Exception e){
			error("Profile refold error", e);
		}
	}
	
	/**
	 * Find the intersection points of two circles. Based on:
	 * http://stackoverflow.com/questions/3349125/circle-circle-intersection-points
	 * @param com1 the centre of the first circle
	 * @param r1 the radius of the first circle
	 * @param com2 the centre of the second circle
	 * @param r2 the radius of the second circle
	 * @return
	 */
	private IPoint[] intersectionPoints(IPoint com1, double r1, IPoint com2, double r2){
		
		 //First calculate the distance d between the center of the circles. d = ||P1 - P0||.
		
		double d = com1.getLengthTo(com2); 
		double r1r2 = r1+r2;
		
		log("P1: "+com1.toString()+" r = "+r1);
		log("P2: "+com2.toString()+" r = "+r2);

		// If d > r0 + r1 then there are no solutions, the circles are separate.
		
		if(Math.abs(d) > r1r2){
			throw new IllegalArgumentException("Circles do not overlap: "+d+" > "+ r1r2 );
		}

		//  If d < |r0 - r1| then there are no solutions because one circle is contained within the other.
		if(Math.abs(d) < r1 - r2){
			throw new IllegalArgumentException("One circle is contained within the other: "+d+" < "+ r1r2);
		}
		

		// If d = 0 and r0 = r1 then the circles are coincident and there are an infinite number of solutions.
		if(d==0 && r1==r2){
			throw new IllegalArgumentException("Circles overlap perfectly");
		}
		

		// Considering the two triangles P0P2P3 and P1P2P3 we can write

		// a2 + h2 = r02 and b2 + h2 = r12
		
		

		// Using d = a + b we can solve for a,

		// a = (r02 - r12 + d2 ) / (2 d)
		
		double a = ((r1*r1) - (r2*r2) + (d*d)) / (2*d);
		
		double x1 = com1.getX();
		double x2 = com2.getX();
		double y1 = com1.getY();
		double y2 = com2.getY();
		
		double dx = x1-x2;
		double dy = y1-y2;

		// It can be readily shown that this reduces to r0 when the two circles touch at one point, ie: d = r0 + r1

		// Solve for h by substituting a into the first equation, h2 = r02 - a2

		// So:

		//P2 = P0 + a ( P1 - P0 ) / d


		//And finally, P3 = (x3,y3) in terms of P0 = (x0,y0), P1 = (x1,y1) and P2 = (x2,y2), is

		//x3 = x2 +- h ( y1 - y0 ) / d

		//y3 = y2 -+ h ( x1 - x0 ) / d
				

		double h = Math.sqrt( r1*r1 - a*a);
		double xm = x1 + (a*dx)/d;
		double ym = y1 + (a*dy)/d;
		double xs1 = xm + (h*dy)/d;
		double xs2 = xm - (h*dy)/d;
		double ys1 = ym - (h*dx)/d;
		double ys2 = ym + (h*dx)/d;
		
		IPoint first = IPoint.makeNew(xs1, ys1);
		IPoint second = IPoint.makeNew(xs2,ys2);

		IPoint[] result = { first, second };
		return result;
	}

}

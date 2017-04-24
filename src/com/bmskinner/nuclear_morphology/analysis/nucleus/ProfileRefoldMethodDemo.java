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
import com.bmskinner.nuclear_morphology.utility.CircleTools;

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
		
		IProfile diameterProfile = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.DIAMETER, Tag.REFERENCE_POINT, Quartile.MEDIAN);
		
		
		List<IPoint> list = new ArrayList<>();
		
		IPoint com = IPoint.makeNew(0, 0);
		
		// Make the first point on the x axis, radius from zero.
		// This is the reference point
		log("First point radius is "+radiusProfile.get(0));
		IPoint first = IPoint.makeNew(radiusProfile.get(0), 0);
		
		/*
		 * The angle profile measures points at every windowsize intervals.
		 * Therefore we need to measure the angles between points spaced
		 * windowsize apart when building a skeleton outline
		 * Move windowsize points along the profile and get the angle.
		 * The interpolation of nucleus periperies has put points every ~1 pixel.
		 * The rough distance to move to the next point is one pixel * the window size
		 */

		int totalPoints = angleProfile.size();
		int window = (int) (dataset.getAnalysisOptions().getProfileWindowProportion() * totalPoints);
		// Get the distance between points
		int distance = window;
		
		log("Setting distance between points to "+distance);
		
		list.add(first);
		log(first.toString());
		
		// Now calculate the position of the next point, which is 
		// radius away from CoM, and can be in any direction from 
		// the first point
		
		/*
		 * Looking for the intersection of the circles
		 * with radii described
		 */
		log("Second point radius is "+radiusProfile.get(window));
		IPoint second = calculateSecondPoint(first, distance, com, radiusProfile.get(window));
		log("Second point distance from CoM: "+second.getLengthTo(com));
		list.add(second);
		log("Second: "+second.toString());
		
		/*
		 * We can now use the angle from the previous point with the radius
		 * to find a position.
		 */

		
		for(int i=window, k=totalPoints,  j=1; i<totalPoints; i+=window, j++, k--){
			
			double r = radiusProfile.get(i+1);
			double a = angleProfile.get(i);
			distance = window;
			
			// In the  first iteration of the loop
			// a is the angle from first to third via second
			// r is the distance from com to third
			
			// The next point must be one of the intersections between
			// point two circle and the com circle
			
			// Get the current and previous points to work from
			IPoint prev    = list.get(j-1);
			IPoint current = list.get(j);
			
			// Find which circle intersection is closest to the desired angle
			
			IPoint[] inters = null;
			boolean loop = true;
			while( loop && distance < 1000){
				try {
					inters = CircleTools.findIntersections(com, r, current, distance);
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
			
			/*
			 * If the angle is near 90 degrees, the wrong choice may be made.
			 * We need to direct the point further.
			 */
			

//			if(next.getLengthTo(current) > distance){
//				log("Error getting points");
//			}

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
	 * Find the points at the intersection of two circles, and return one of them
	 * @param first
	 * @param r1
	 * @param com
	 * @param r2
	 * @return
	 */
	private IPoint calculateSecondPoint(IPoint first, double r1, IPoint com, double r2){
		/*
		 * Looking for the intersection of the circles
		 * with radii described
		 */
		IPoint[] interesctions = null;
		boolean loop = true;
		while( loop && r1 > 1 && r1 < 1000){
			try {
				interesctions = CircleTools.findIntersections(first, r1, com, r2 );
				loop=false;
			} catch(IllegalArgumentException e){
				log(e.getMessage());
				r1++;
				log("Increasing distance to "+r1);
				
			}
		}
		
		log("Second point possible values:");
		log(interesctions[0].toString());
		log(interesctions[1].toString());
		IPoint second = interesctions[0];
		return second;
	}

}
